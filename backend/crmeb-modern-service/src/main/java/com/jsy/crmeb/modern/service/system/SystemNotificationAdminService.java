package com.jsy.crmeb.modern.service.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jsy.crmeb.modern.service.system.dto.NotificationInfoResponse;
import com.jsy.crmeb.modern.service.system.dto.NotificationUpdateRequest;
import com.jsy.crmeb.modern.service.system.entity.SmsTemplate;
import com.jsy.crmeb.modern.service.system.entity.SystemNotification;
import com.jsy.crmeb.modern.service.system.entity.TemplateMessage;
import com.jsy.crmeb.modern.service.system.mapper.SmsTemplateMapper;
import com.jsy.crmeb.modern.service.system.mapper.SystemNotificationMapper;
import com.jsy.crmeb.modern.service.system.mapper.TemplateMessageMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SystemNotificationAdminService {
    private final SystemNotificationMapper notificationMapper;
    private final TemplateMessageMapper templateMessageMapper;
    private final SmsTemplateMapper smsTemplateMapper;

    public SystemNotificationAdminService(
            SystemNotificationMapper notificationMapper,
            TemplateMessageMapper templateMessageMapper,
            SmsTemplateMapper smsTemplateMapper) {
        this.notificationMapper = notificationMapper;
        this.templateMessageMapper = templateMessageMapper;
        this.smsTemplateMapper = smsTemplateMapper;
    }

    public List<SystemNotification> list(Integer sendType) {
        QueryWrapper<SystemNotification> query = new QueryWrapper<>();
        if (sendType != null) {
            if (sendType < 1 || sendType > 2) {
                throw new IllegalArgumentException("未知的发送类型");
            }
            query.eq("send_type", sendType);
        }
        query.orderByAsc("id");
        return notificationMapper.selectList(query);
    }

    @Transactional
    public boolean wechatSwitch(Integer id) {
        SystemNotification notification = requiredNotification(id);
        if (integer(notification.getIsWechat()) == 0) {
            throw new IllegalArgumentException("通知没有配置公众号模板");
        }
        SystemNotification update = new SystemNotification();
        update.setId(id);
        update.setIsWechat(integer(notification.getIsWechat()) == 1 ? 2 : 1);
        return notificationMapper.updateById(update) > 0;
    }

    @Transactional
    public boolean routineSwitch(Integer id) {
        SystemNotification notification = requiredNotification(id);
        if (integer(notification.getIsRoutine()) == 0) {
            throw new IllegalArgumentException("通知没有配置小程序订阅模板");
        }
        SystemNotification update = new SystemNotification();
        update.setId(id);
        update.setIsRoutine(integer(notification.getIsRoutine()) == 1 ? 2 : 1);
        return notificationMapper.updateById(update) > 0;
    }

    @Transactional
    public boolean smsSwitch(Integer id) {
        SystemNotification notification = requiredNotification(id);
        if (integer(notification.getIsSms()) == 0) {
            throw new IllegalArgumentException("通知没有配置短信");
        }
        SystemNotification update = new SystemNotification();
        update.setId(id);
        update.setIsSms(integer(notification.getIsSms()) == 1 ? 2 : 1);
        return notificationMapper.updateById(update) > 0;
    }

    public NotificationInfoResponse detail(Integer id, String detailType) {
        SystemNotification notification = requiredNotification(id);
        if ("wechat".equals(detailType)) {
            if (integer(notification.getIsWechat()) == 0) {
                throw new IllegalArgumentException("请先配置公众号模板消息");
            }
            TemplateMessage template = requiredTemplateMessage(notification.getWechatId());
            return toWechatResponse(template, notification.getIsWechat());
        }
        if ("routine".equals(detailType)) {
            if (integer(notification.getIsRoutine()) == 0) {
                throw new IllegalArgumentException("请先配置小程序订阅消息");
            }
            TemplateMessage template = requiredTemplateMessage(notification.getRoutineId());
            return toWechatResponse(template, notification.getIsRoutine());
        }
        if ("sms".equals(detailType)) {
            if (integer(notification.getIsSms()) == 0) {
                throw new IllegalArgumentException("请先配置短信模板");
            }
            SmsTemplate template = requiredSmsTemplate(notification.getSmsId());
            return toSmsResponse(template, notification.getIsSms());
        }
        throw new IllegalArgumentException("详情类型不能为空");
    }

    @Transactional
    public boolean update(NotificationUpdateRequest request) {
        SystemNotification notification = requiredNotification(request.getId());
        String detailType = request.getDetailType();
        if (!"sms".equals(detailType) && !StringUtils.hasText(request.getTempId())) {
            throw new IllegalArgumentException("模板id不能为空");
        }
        if ("wechat".equals(detailType)) {
            if (integer(notification.getIsWechat()) == 0) {
                throw new IllegalArgumentException("请先为通知配置公众号模板");
            }
            TemplateMessage template = requiredTemplateMessage(notification.getWechatId());
            updateTemplateMessage(template, request.getTempId());
            if (!integer(notification.getIsWechat()).equals(request.getStatus())) {
                SystemNotification update = new SystemNotification();
                update.setId(notification.getId());
                update.setIsWechat(request.getStatus());
                notificationMapper.updateById(update);
            }
            return true;
        }
        if ("routine".equals(detailType)) {
            if (integer(notification.getIsRoutine()) == 0) {
                throw new IllegalArgumentException("请先为通知配置小程序订阅模板");
            }
            TemplateMessage template = requiredTemplateMessage(notification.getRoutineId());
            updateTemplateMessage(template, request.getTempId());
            if (!integer(notification.getIsRoutine()).equals(request.getStatus())) {
                SystemNotification update = new SystemNotification();
                update.setId(notification.getId());
                update.setIsRoutine(request.getStatus());
                notificationMapper.updateById(update);
            }
            return true;
        }
        if ("sms".equals(detailType) && !integer(notification.getIsSms()).equals(request.getStatus())) {
            SystemNotification update = new SystemNotification();
            update.setId(notification.getId());
            update.setIsSms(request.getStatus());
            return notificationMapper.updateById(update) > 0;
        }
        return true;
    }

    private void updateTemplateMessage(TemplateMessage template, String tempId) {
        if (tempId.equals(template.getTempId())) {
            return;
        }
        TemplateMessage update = new TemplateMessage();
        update.setId(template.getId());
        update.setTempId(tempId);
        update.setUpdateTime(LocalDateTime.now());
        templateMessageMapper.updateById(update);
    }

    private NotificationInfoResponse toWechatResponse(TemplateMessage template, Integer status) {
        NotificationInfoResponse response = new NotificationInfoResponse();
        response.setId(template.getId());
        response.setTempId(template.getTempId());
        response.setTempKey(template.getTempKey());
        response.setContent(template.getContent());
        response.setName(template.getName());
        response.setStatus(status);
        return response;
    }

    private NotificationInfoResponse toSmsResponse(SmsTemplate template, Integer status) {
        NotificationInfoResponse response = new NotificationInfoResponse();
        response.setId(template.getId());
        response.setTempId(template.getTempId());
        response.setTempKey(template.getTempKey());
        response.setContent(template.getContent());
        response.setTitle(template.getTitle());
        response.setStatus(status);
        return response;
    }

    private SystemNotification requiredNotification(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("通知id不能为空");
        }
        SystemNotification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new IllegalArgumentException("系统通知不存在");
        }
        return notification;
    }

    private TemplateMessage requiredTemplateMessage(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("模板不存在");
        }
        TemplateMessage template = templateMessageMapper.selectById(id);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在");
        }
        return template;
    }

    private SmsTemplate requiredSmsTemplate(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("短信模板不存在");
        }
        SmsTemplate template = smsTemplateMapper.selectById(id);
        if (template == null) {
            throw new IllegalArgumentException("短信模板不存在");
        }
        return template;
    }

    private Integer integer(Integer value) {
        return value == null ? 0 : value;
    }
}
