package com.jsy.crmeb.modern.service.schedule;

import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.schedule.dto.ScheduleJobRequest;
import com.jsy.crmeb.modern.service.schedule.entity.ScheduleJob;
import com.jsy.crmeb.modern.service.schedule.entity.ScheduleJobLog;
import com.jsy.crmeb.modern.service.schedule.mapper.ScheduleJobLogMapper;
import com.jsy.crmeb.modern.service.schedule.mapper.ScheduleJobMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ScheduleAdminService {
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_PAUSE = 1;

    private final ScheduleJobMapper scheduleJobMapper;
    private final ScheduleJobLogMapper scheduleJobLogMapper;
    private final ScheduleTaskExecutor scheduleTaskExecutor;

    public ScheduleAdminService(
            ScheduleJobMapper scheduleJobMapper,
            ScheduleJobLogMapper scheduleJobLogMapper,
            ScheduleTaskExecutor scheduleTaskExecutor) {
        this.scheduleJobMapper = scheduleJobMapper;
        this.scheduleJobLogMapper = scheduleJobLogMapper;
        this.scheduleTaskExecutor = scheduleTaskExecutor;
    }

    public List<ScheduleJob> list() {
        return scheduleJobMapper.selectActive();
    }

    public PageResponse<ScheduleJobLog> logList(
            Integer jobId,
            String beanName,
            String methodName,
            Integer status,
            Integer page,
            Integer limit) {
        int safePage = page == null || page <= 0 ? 1 : page;
        int safeLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 200);
        String cleanBeanName = clean(beanName);
        String cleanMethodName = clean(methodName);
        long total = scheduleJobLogMapper.countAll(jobId, cleanBeanName, cleanMethodName, status);
        List<ScheduleJobLog> list = scheduleJobLogMapper.selectPage(
                jobId,
                cleanBeanName,
                cleanMethodName,
                status,
                (safePage - 1) * safeLimit,
                safeLimit);
        return new PageResponse<>(safePage, safeLimit, total, list);
    }

    public Boolean add(ScheduleJobRequest request) {
        validateRequest(request, false);
        ScheduleJob job = new ScheduleJob();
        applyRequest(job, request);
        job.setStatus(STATUS_PAUSE);
        job.setIsDelte(false);
        job.setCreateTime(LocalDateTime.now());
        return scheduleJobMapper.insert(job) > 0;
    }

    public Boolean update(ScheduleJobRequest request) {
        validateRequest(request, true);
        ScheduleJob job = getExisting(request.getJobId());
        if (Integer.valueOf(STATUS_NORMAL).equals(job.getStatus())) {
            throw new IllegalArgumentException("请先暂停定时任务");
        }
        applyRequest(job, request);
        return scheduleJobMapper.updateById(job) > 0;
    }

    public Boolean suspend(Integer jobId) {
        ScheduleJob job = getExisting(jobId);
        if (Integer.valueOf(STATUS_PAUSE).equals(job.getStatus())) {
            throw new IllegalArgumentException("定时任务已暂停，请勿重复操作");
        }
        job.setStatus(STATUS_PAUSE);
        return scheduleJobMapper.updateById(job) > 0;
    }

    public Boolean start(Integer jobId) {
        ScheduleJob job = getExisting(jobId);
        if (Integer.valueOf(STATUS_NORMAL).equals(job.getStatus())) {
            throw new IllegalArgumentException("定时任务已启动，请勿重复操作");
        }
        job.setStatus(STATUS_NORMAL);
        return scheduleJobMapper.updateById(job) > 0;
    }

    public Boolean delete(Integer jobId) {
        ScheduleJob job = getExisting(jobId);
        if (Integer.valueOf(STATUS_NORMAL).equals(job.getStatus())) {
            throw new IllegalArgumentException("请先暂停定时任务");
        }
        job.setIsDelte(true);
        return scheduleJobMapper.updateById(job) > 0;
    }

    @Transactional
    public Boolean trig(Integer jobId) {
        ScheduleJob job = getExisting(jobId);
        ScheduleJobLog log = new ScheduleJobLog();
        log.setJobId(job.getJobId());
        log.setBeanName(job.getBeanName());
        log.setMethodName(job.getMethodName());
        log.setParams(job.getParams());
        long start = System.currentTimeMillis();
        try {
            scheduleTaskExecutor.execute(job);
            log.setStatus(1);
            log.setError(null);
        } catch (Exception exception) {
            log.setStatus(0);
            log.setError(truncate(exception.toString(), 2000));
        }
        log.setTimes(Math.toIntExact(Math.min(System.currentTimeMillis() - start, Integer.MAX_VALUE)));
        log.setCreateTime(LocalDateTime.now());
        scheduleJobLogMapper.insert(log);
        return true;
    }

    private ScheduleJob getExisting(Integer jobId) {
        if (jobId == null || jobId <= 0) {
            throw new IllegalArgumentException("定时任务ID不能为空");
        }
        ScheduleJob job = scheduleJobMapper.selectById(jobId);
        if (job == null || Boolean.TRUE.equals(job.getIsDelte())) {
            throw new IllegalArgumentException("定时任务不存在");
        }
        return job;
    }

    private void validateRequest(ScheduleJobRequest request, boolean requireId) {
        if (request == null) {
            throw new IllegalArgumentException("参数错误");
        }
        if (requireId && (request.getJobId() == null || request.getJobId() <= 0)) {
            throw new IllegalArgumentException("定时任务ID不能为空");
        }
        if (!StringUtils.hasText(request.getBeanName())) {
            throw new IllegalArgumentException("spring bean名称不能为空");
        }
        if (!StringUtils.hasText(request.getMethodName())) {
            throw new IllegalArgumentException("方法名不能为空");
        }
        if (!StringUtils.hasText(request.getCronExpression())) {
            throw new IllegalArgumentException("cron表达式不能为空");
        }
    }

    private void applyRequest(ScheduleJob job, ScheduleJobRequest request) {
        job.setBeanName(request.getBeanName().trim());
        job.setMethodName(request.getMethodName().trim());
        job.setCronExpression(request.getCronExpression().trim());
        job.setParams(clean(request.getParams()));
        job.setRemark(clean(request.getRemark()));
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String truncate(String value, int limit) {
        String text = value == null ? "" : value;
        return text.length() <= limit ? text : text.substring(0, limit);
    }
}
