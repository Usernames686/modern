package com.jsy.crmeb.modern.service.finance.mapper;

import com.jsy.crmeb.modern.service.finance.dto.MonitorResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserBillAdminMapper {
    @Select("""
            <script>
            select count(*)
            from eb_user_bill ub
            left join eb_user u on ub.uid = u.uid
            where ub.category = 'now_money'
              <if test="keywords != null and keywords != ''">
                and (ub.uid like concat('%', #{keywords}, '%') or u.nickname like concat('%', #{keywords}, '%'))
              </if>
              <if test="title != null and title != ''">
                and ub.title = #{title}
              </if>
              <if test="startTime != null and startTime != '' and endTime != null and endTime != ''">
                and ub.create_time between #{startTime} and #{endTime}
              </if>
            </script>
            """)
    long countFundMonitor(
            @Param("keywords") String keywords,
            @Param("title") String title,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    @Select("""
            <script>
            select ub.id,
                   ub.uid,
                   ub.pm,
                   ub.title,
                   ub.number,
                   ub.mark,
                   ub.create_time as createTime,
                   coalesce(u.nickname, '') as nickName
            from eb_user_bill ub
            left join eb_user u on ub.uid = u.uid
            where ub.category = 'now_money'
              <if test="keywords != null and keywords != ''">
                and (ub.uid like concat('%', #{keywords}, '%') or u.nickname like concat('%', #{keywords}, '%'))
              </if>
              <if test="title != null and title != ''">
                and ub.title = #{title}
              </if>
              <if test="startTime != null and startTime != '' and endTime != null and endTime != ''">
                and ub.create_time between #{startTime} and #{endTime}
              </if>
            order by ub.id desc, ub.create_time desc
            limit #{offset}, #{limit}
            </script>
            """)
    List<MonitorResponse> selectFundMonitor(
            @Param("keywords") String keywords,
            @Param("title") String title,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("offset") int offset,
            @Param("limit") int limit);
}
