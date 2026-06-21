package com.jsy.crmeb.modern.service.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.schedule.entity.ScheduleJobLog;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ScheduleJobLogMapper extends BaseMapper<ScheduleJobLog> {
    @Select("""
            <script>
            select log_id as logId,
                   job_id as jobId,
                   bean_name as beanName,
                   method_name as methodName,
                   params,
                   status,
                   error,
                   times,
                   create_time as createTime
            from eb_schedule_job_log
            <where>
              <if test='jobId != null'>and job_id = #{jobId}</if>
              <if test='beanName != null and beanName != ""'>and bean_name like concat('%', #{beanName}, '%')</if>
              <if test='methodName != null and methodName != ""'>and method_name like concat('%', #{methodName}, '%')</if>
              <if test='status != null'>and status = #{status}</if>
            </where>
            order by log_id desc
            limit #{offset}, #{limit}
            </script>
            """)
    List<ScheduleJobLog> selectPage(
            @Param("jobId") Integer jobId,
            @Param("beanName") String beanName,
            @Param("methodName") String methodName,
            @Param("status") Integer status,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            <script>
            select count(1)
            from eb_schedule_job_log
            <where>
              <if test='jobId != null'>and job_id = #{jobId}</if>
              <if test='beanName != null and beanName != ""'>and bean_name like concat('%', #{beanName}, '%')</if>
              <if test='methodName != null and methodName != ""'>and method_name like concat('%', #{methodName}, '%')</if>
              <if test='status != null'>and status = #{status}</if>
            </where>
            </script>
            """)
    long countAll(
            @Param("jobId") Integer jobId,
            @Param("beanName") String beanName,
            @Param("methodName") String methodName,
            @Param("status") Integer status);

    @Delete("delete from eb_schedule_job_log where create_time < #{beforeTime}")
    int deleteScheduleLogsBefore(@Param("beforeTime") LocalDateTime beforeTime);

    @Delete("delete from eb_wechat_exceptions where create_time < #{beforeTime}")
    int deleteWechatExceptionLogsBefore(@Param("beforeTime") LocalDateTime beforeTime);
}
