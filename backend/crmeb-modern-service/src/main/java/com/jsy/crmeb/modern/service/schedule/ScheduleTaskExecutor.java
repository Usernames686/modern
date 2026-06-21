package com.jsy.crmeb.modern.service.schedule;

import com.jsy.crmeb.modern.service.schedule.entity.ScheduleJob;
import java.lang.reflect.Method;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

@Component
public class ScheduleTaskExecutor {
    private final ApplicationContext applicationContext;

    public ScheduleTaskExecutor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void execute(ScheduleJob job) {
        Object target = applicationContext.getBean(job.getBeanName());
        try {
            Method method;
            if (StringUtils.hasText(job.getParams())) {
                method = ReflectionUtils.findMethod(target.getClass(), job.getMethodName(), String.class);
            } else {
                method = ReflectionUtils.findMethod(target.getClass(), job.getMethodName());
            }
            if (method == null) {
                throw new NoSuchMethodException(job.getMethodName());
            }
            ReflectionUtils.makeAccessible(method);
            if (StringUtils.hasText(job.getParams())) {
                method.invoke(target, job.getParams());
            } else {
                method.invoke(target);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("执行定时任务失败", exception);
        }
    }
}
