package com.jsy.crmeb.modern.service.schedule.task;

import com.jsy.crmeb.modern.service.schedule.mapper.ScheduleJobLogMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component("AutoDeleteLogTask")
public class AutoDeleteLogTask {
    private final ScheduleJobLogMapper scheduleJobLogMapper;

    public AutoDeleteLogTask(ScheduleJobLogMapper scheduleJobLogMapper) {
        this.scheduleJobLogMapper = scheduleJobLogMapper;
    }

    public void autoDeleteLog() {
        LocalDateTime beforeTime = LocalDate.now().minusDays(9).atStartOfDay();
        scheduleJobLogMapper.deleteScheduleLogsBefore(beforeTime);
        scheduleJobLogMapper.deleteWechatExceptionLogsBefore(beforeTime);
    }
}
