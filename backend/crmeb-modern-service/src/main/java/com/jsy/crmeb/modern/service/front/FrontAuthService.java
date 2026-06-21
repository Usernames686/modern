package com.jsy.crmeb.modern.service.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jsy.crmeb.modern.common.util.LegacyPasswordUtil;
import com.jsy.crmeb.modern.service.user.entity.User;
import com.jsy.crmeb.modern.service.user.mapper.UserMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FrontAuthService {
    public static final String TOKEN_HEADER = "Authori-zation";
    public static final String UNAUTHORIZED_MESSAGE = "请登录后再操作";

    private static final String TOKEN_PREFIX = "crmeb-modern:front:token:";
    private static final Duration TOKEN_TTL = Duration.ofDays(7);

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    public FrontAuthService(UserMapper userMapper, StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
    }

    public Map<String, Object> login(Map<String, Object> body, String ip) {
        String account = stringValue(body, "account", "phone", "username");
        String password = stringValue(body, "password", "pwd");
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            throw new FrontAuthException(400, "账号和密码不能为空");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .and(wrapper -> wrapper.eq(User::getAccount, account).or().eq(User::getPhone, account))
                .last("limit 1"));
        if (user == null || Integer.valueOf(1).equals(user.getIsLogoff())) {
            throw new FrontAuthException(400, "用户不存在或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new FrontAuthException(400, "账号已被禁用");
        }
        String encryptedPassword = LegacyPasswordUtil.encryptPassword(password, user.getAccount());
        if (!encryptedPassword.equals(user.getPwd())) {
            throw new FrontAuthException(400, "用户不存在或密码错误");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(tokenKey(token), String.valueOf(user.getUid()), TOKEN_TTL);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getUid, user.getUid())
                .set(User::getLastIp, trimIp(ip))
                .set(User::getLastLoginTime, LocalDateTime.now())
                .set(User::getUpdateTime, LocalDateTime.now()));

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "login");
        response.put("key", "");
        response.put("uid", user.getUid());
        response.put("nikeName", user.getNickname());
        response.put("phone", user.getPhone());
        return response;
    }

    public Map<String, Object> userInfo(String token) {
        return userMap(requireUserByToken(token));
    }

    public Map<String, Object> updateProfile(String token, Map<String, Object> body) {
        User user = requireUserByToken(token);
        String nickname = stringValue(body, "nickname", "nickName");
        String avatar = stringValue(body, "avatar", "headimgurl");
        if (!StringUtils.hasText(nickname)) {
            throw new FrontAuthException(400, "昵称不能为空");
        }
        if (nickname.length() > 20) {
            throw new FrontAuthException(400, "昵称不能超过20个字符");
        }
        LambdaUpdateWrapper<User> update = new LambdaUpdateWrapper<User>()
                .eq(User::getUid, user.getUid())
                .set(User::getNickname, nickname)
                .set(User::getUpdateTime, LocalDateTime.now());
        if (StringUtils.hasText(avatar)) {
            update.set(User::getAvatar, avatar);
        }
        userMapper.update(null, update);
        return userInfo(token);
    }

    public void verifyCurrentPhone(String token, Map<String, Object> body) {
        User user = requireUserByToken(token);
        String phone = stringValue(body, "phone", "account");
        String captcha = stringValue(body, "captcha", "code");
        if (!StringUtils.hasText(user.getPhone())) {
            throw new FrontAuthException(400, "当前账号未绑定手机号");
        }
        if (!user.getPhone().equals(phone)) {
            throw new FrontAuthException(400, "手机号与当前账号不一致");
        }
        requireLocalCaptcha(captcha);
    }

    public Map<String, Object> updatePhone(String token, Map<String, Object> body) {
        User user = requireUserByToken(token);
        String phone = stringValue(body, "phone", "account");
        String captcha = stringValue(body, "captcha", "code");
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new FrontAuthException(400, "请输入正确的手机号码");
        }
        requireLocalCaptcha(captcha);
        User exists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .ne(User::getUid, user.getUid())
                .last("limit 1"));
        if (exists != null) {
            throw new FrontAuthException(400, "手机号已被其他账号绑定");
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getUid, user.getUid())
                .set(User::getPhone, phone)
                .set(User::getUpdateTime, LocalDateTime.now()));
        return userInfo(token);
    }

    public void updatePassword(String token, Map<String, Object> body) {
        User user = requireUserByToken(token);
        String account = stringValue(body, "account", "phone");
        String password = stringValue(body, "password", "pwd");
        String captcha = stringValue(body, "captcha", "code");
        if (StringUtils.hasText(account) && !account.equals(user.getPhone()) && !account.equals(user.getAccount())) {
            throw new FrontAuthException(400, "账号与当前登录用户不一致");
        }
        if (!password.matches("^[a-zA-Z]\\w{5,17}$")) {
            throw new FrontAuthException(400, "密码必须以字母开头，长度6-18位，只能包含字母、数字和下划线");
        }
        requireLocalCaptcha(captcha);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getUid, user.getUid())
                .set(User::getPwd, LegacyPasswordUtil.encryptPassword(password, user.getAccount()))
                .set(User::getUpdateTime, LocalDateTime.now()));
    }

    public boolean tokenIsExist(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        Boolean exists = redisTemplate.hasKey(tokenKey(token));
        return Boolean.TRUE.equals(exists);
    }

    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            redisTemplate.delete(tokenKey(token));
        }
    }

    public Integer requireUidByToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new FrontAuthException(401, UNAUTHORIZED_MESSAGE);
        }
        String uid = redisTemplate.opsForValue().get(tokenKey(token));
        if (!StringUtils.hasText(uid)) {
            throw new FrontAuthException(401, UNAUTHORIZED_MESSAGE);
        }
        redisTemplate.expire(tokenKey(token), TOKEN_TTL);
        return Integer.valueOf(uid);
    }

    public User requireUserByToken(String token) {
        User user = userMapper.selectById(requireUidByToken(token));
        if (user == null || !Integer.valueOf(1).equals(user.getStatus()) || Integer.valueOf(1).equals(user.getIsLogoff())) {
            throw new FrontAuthException(401, UNAUTHORIZED_MESSAGE);
        }
        return user;
    }

    private Map<String, Object> userMap(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("uid", user.getUid());
        response.put("account", user.getAccount());
        response.put("nickname", user.getNickname());
        response.put("avatar", normalizeAsset(user.getAvatar()));
        response.put("phone", user.getPhone());
        response.put("nowMoney", user.getNowMoney());
        response.put("brokeragePrice", user.getBrokeragePrice());
        response.put("integral", user.getIntegral());
        response.put("experience", user.getExperience());
        response.put("level", user.getLevel());
        response.put("isPromoter", Integer.valueOf(1).equals(user.getIsPromoter()));
        response.put("payCount", user.getPayCount());
        response.put("subscribe", Integer.valueOf(1).equals(user.getSubscribe()));
        return response;
    }

    private String stringValue(Map<String, Object> body, String... names) {
        if (body == null) {
            return "";
        }
        for (String name : names) {
            Object value = body.get(name);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return "";
    }

    private void requireLocalCaptcha(String captcha) {
        if (!StringUtils.hasText(captcha)) {
            throw new FrontAuthException(400, "请填写验证码");
        }
    }

    private String normalizeAsset(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/")) {
            return value;
        }
        return "/" + value;
    }

    private String tokenKey(String token) {
        return TOKEN_PREFIX + token;
    }

    private String trimIp(String ip) {
        if (ip == null || ip.length() <= 16) {
            return ip;
        }
        return ip.substring(0, 16);
    }
}
