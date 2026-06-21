package com.jsy.crmeb.modern.service.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.schedule.entity.ScheduleJob;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ScheduleJobMapper extends BaseMapper<ScheduleJob> {
    @Select("""
            select job_id as jobId,
                   bean_name as beanName,
                   method_name as methodName,
                   params,
                   cron_expression as cronExpression,
                   status,
                   remark,
                   is_delte as isDelte,
                   create_time as createTime
            from eb_schedule_job
            where is_delte = 0
            order by job_id desc
            """)
    List<ScheduleJob> selectActive();
}
