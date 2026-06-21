package com.jsy.crmeb.modern.service.schedule.task;

import com.jsy.crmeb.modern.service.wechat.WechatTemplateMessageQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("AsyncWeChatPublicTempMessage")
public class AsyncWeChatPublicTempMessage {
    private static final Logger logger = LoggerFactory.getLogger(AsyncWeChatPublicTempMessage.class);
    private final WechatTemplateMessageQueueService wechatTemplateMessageQueueService;

    public AsyncWeChatPublicTempMessage(WechatTemplateMessageQueueService wechatTemplateMessageQueueService) {
        this.wechatTemplateMessageQueueService = wechatTemplateMessageQueueService;
    }

    public void init() {
        try {
            wechatTemplateMessageQueueService.consumePublic();
        } catch (Exception exception) {
            logger.error("AsyncWeChatPublicTempMessage.init failed", exception);
        }
    }
}
