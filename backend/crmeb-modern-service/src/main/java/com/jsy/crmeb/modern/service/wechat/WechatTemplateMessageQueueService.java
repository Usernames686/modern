package com.jsy.crmeb.modern.service.wechat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsy.crmeb.modern.service.wechat.mapper.WechatMessageTaskMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatTemplateMessageQueueService {
    private static final Logger logger = LoggerFactory.getLogger(WechatTemplateMessageQueueService.class);
    public static final String WE_CHAT_MESSAGE_KEY_PUBLIC = "we_chat_public_message_list";
    public static final String WE_CHAT_MESSAGE_KEY_PROGRAM = "we_chat_program_message_list";
    private static final String PUBLIC_ACCESS_TOKEN_KEY = "wechat_public_accessToken";
    private static final String MINI_ACCESS_TOKEN_KEY = "wechat_mini_accessToken";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
    private static final String PUBLIC_SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s";
    private static final String MINI_SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=%s";

    private final StringRedisTemplate redisTemplate;
    private final WechatMessageTaskMapper wechatMessageTaskMapper;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public WechatTemplateMessageQueueService(
            StringRedisTemplate redisTemplate,
            WechatMessageTaskMapper wechatMessageTaskMapper,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.wechatMessageTaskMapper = wechatMessageTaskMapper;
        this.objectMapper = objectMapper;
    }

    public void consumePublic() {
        consume(WE_CHAT_MESSAGE_KEY_PUBLIC, false);
    }

    public void consumeProgram() {
        consume(WE_CHAT_MESSAGE_KEY_PROGRAM, true);
    }

    private void consume(String redisKey, boolean mini) {
        Long size = redisTemplate.opsForList().size(redisKey);
        logger.info("WechatTemplateMessageQueueService.consume {} size: {}", redisKey, size);
        if (size == null || size < 1) {
            return;
        }
        for (int i = 0; i < size; i++) {
            String data = redisTemplate.opsForList().rightPop(redisKey);
            if (!StringUtils.hasText(data)) {
                continue;
            }
            try {
                boolean success = mini ? sendMiniSubscribeMessage(data) : sendPublicTemplateMessage(data);
                if (!success) {
                    redisTemplate.opsForList().leftPush(redisKey, data);
                }
            } catch (Exception exception) {
                redisTemplate.opsForList().leftPush(redisKey, data);
                logger.error("微信模板消息消费失败，已重新入队，key = {}", redisKey, exception);
            }
        }
    }

    private boolean sendPublicTemplateMessage(String payload) throws IOException, InterruptedException {
        String accessToken = accessToken(false);
        JsonNode response = postJson(String.format(PUBLIC_SEND_URL, url(accessToken)), payload);
        if (invalidAccessToken(response)) {
            redisTemplate.delete(PUBLIC_ACCESS_TOKEN_KEY);
            accessToken = accessToken(false);
            response = postJson(String.format(PUBLIC_SEND_URL, url(accessToken)), payload);
        }
        return checkWechatResponse(response, payload, "微信公众号发送模板消息异常");
    }

    private boolean sendMiniSubscribeMessage(String payload) throws IOException, InterruptedException {
        String accessToken = accessToken(true);
        JsonNode response = postJson(String.format(MINI_SEND_URL, url(accessToken)), payload);
        if (invalidAccessToken(response)) {
            redisTemplate.delete(MINI_ACCESS_TOKEN_KEY);
            accessToken = accessToken(true);
            response = postJson(String.format(MINI_SEND_URL, url(accessToken)), payload);
        }
        return checkWechatResponse(response, payload, "微信小程序发送订阅消息异常");
    }

    private String accessToken(boolean mini) throws IOException, InterruptedException {
        String redisKey = mini ? MINI_ACCESS_TOKEN_KEY : PUBLIC_ACCESS_TOKEN_KEY;
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (StringUtils.hasText(cached)) {
            return cached;
        }
        String appId = config(mini ? "routine_appid" : "wechat_appid", mini ? "微信小程序appId未设置" : "微信公众号appId未设置");
        String secret = config(mini ? "routine_appsecret" : "wechat_appsecret", mini ? "微信小程序secret未设置" : "微信公众号secret未设置");
        JsonNode response = getJson(String.format(ACCESS_TOKEN_URL, url(appId), url(secret)));
        if (response == null || !StringUtils.hasText(text(response, "access_token"))) {
            saveWechatException(response, "微信获取access_token异常");
            throw new IllegalStateException("微信获取access_token异常: " + (response == null ? "empty response" : response.toString()));
        }
        String token = text(response, "access_token");
        long expires = Math.max(60L, response.path("expires_in").asLong(7200L) - 1800L);
        redisTemplate.opsForValue().set(redisKey, token, expires, TimeUnit.SECONDS);
        return token;
    }

    private String config(String key, String message) {
        String value = wechatMessageTaskMapper.selectConfigValue(key);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }

    private JsonNode getJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return objectMapper.readTree(response.body());
    }

    private JsonNode postJson(String url, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return objectMapper.readTree(response.body());
    }

    private boolean checkWechatResponse(JsonNode response, String payload, String remark) {
        if (response == null) {
            throw new IllegalStateException("微信平台接口异常，没任何数据返回");
        }
        int errcode = response.path("errcode").asInt(0);
        if (errcode != 0) {
            saveWechatException(response, remark + " payload=" + payload);
            throw new IllegalStateException("微信接口调用失败：" + errcode + text(response, "errmsg"));
        }
        return true;
    }

    private boolean invalidAccessToken(JsonNode response) {
        int errcode = response == null ? 0 : response.path("errcode").asInt(0);
        return errcode == 40001 || errcode == 40014 || errcode == 42001;
    }

    private void saveWechatException(JsonNode response, String remark) {
        String errcode = response == null ? "" : text(response, "errcode");
        String errmsg = response == null ? "" : text(response, "errmsg");
        String data = response == null ? "" : response.toString();
        wechatMessageTaskMapper.insertException(errcode, errmsg, data, remark);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
