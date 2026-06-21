package com.jsy.crmeb.modern.service.schedule.task;

import com.jsy.crmeb.modern.service.wechat.WechatTemplateMessageQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("AsyncWeChatProgramTempMessage")
public class AsyncWeChatProgramTempMessage {
    private static final Logger logger = LoggerFactory.getLogger(AsyncWeChatProgramTempMessage.class);
    private final WechatTemplateMessageQueueService wechatTemplateMessageQueueService;

    public AsyncWeChatProgramTempMessage(WechatTemplateMessageQueueService wechatTemplateMessageQueueService) {
        this.wechatTemplateMessageQueueService = wechatTemplateMessageQueueService;
    }

    public void init() {
        try {
            wechatTemplateMessageQueueService.consumeProgram();
        } catch (Exception exception) {
            logger.error("AsyncWeChatProgramTempMessage.init failed", exception);
        }
    }
}
