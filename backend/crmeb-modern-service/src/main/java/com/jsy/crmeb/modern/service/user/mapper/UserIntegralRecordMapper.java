package com.jsy.crmeb.modern.service.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.user.dto.UserIntegralRecordResponse;
import com.jsy.crmeb.modern.service.user.entity.UserIntegralRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserIntegralRecordMapper extends BaseMapper<UserIntegralRecord> {
    @Select("""
            <script>
            select count(*)
            from eb_user_integral_record r
            left join eb_user u on u.uid = r.uid
            where r.status = 3
              <if test="uid != null">
                and r.uid = #{uid}
              </if>
              <if test="keywords != null and keywords != ''">
                and u.nickname like concat('%', #{keywords}, '%')
              </if>
              <if test="startTime != null and startTime != ''">
                and r.update_time &gt;= #{startTime}
              </if>
              <if test="endTime != null and endTime != ''">
                and r.update_time &lt;= #{endTime}
              </if>
            </script>
            """)
    long countAdminRecords(
            @Param("uid") Integer uid,
            @Param("keywords") String keywords,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    @Select("""
            <script>
            select r.id,
                   r.title,
                   r.balance,
                   r.integral,
                   r.mark,
                   r.uid,
                   coalesce(u.nickname, '') as nickName,
                   r.update_time as updateTime
            from eb_user_integral_record r
            left join eb_user u on u.uid = r.uid
            where r.status = 3
              <if test="uid != null">
                and r.uid = #{uid}
              </if>
              <if test="keywords != null and keywords != ''">
                and u.nickname like concat('%', #{keywords}, '%')
              </if>
              <if test="startTime != null and startTime != ''">
                and r.update_time &gt;= #{startTime}
              </if>
              <if test="endTime != null and endTime != ''">
                and r.update_time &lt;= #{endTime}
              </if>
            order by r.update_time desc, r.id desc
            limit #{offset}, #{limit}
            </script>
            """)
    List<UserIntegralRecordResponse> selectAdminRecords(
            @Param("uid") Integer uid,
            @Param("keywords") String keywords,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            select id,
                   uid,
                   link_id as linkId,
                   link_type as linkType,
                   type,
                   title,
                   integral,
                   balance,
                   mark,
                   status,
                   frozen_time as frozenTime,
                   thaw_time as thawTime,
                   create_time as createTime,
                   update_time as updateTime
            from eb_user_integral_record
            where thaw_time <= #{nowMillis}
              and link_type = 'order'
              and type = 1
              and status = 2
            order by id asc
            """)
    List<UserIntegralRecord> selectDueFrozenOrderAddRecords(@Param("nowMillis") long nowMillis);

    @Select("""
            select id,
                   uid,
                   link_id as linkId,
                   link_type as linkType,
                   type,
                   title,
                   integral,
                   balance,
                   mark,
                   status,
                   frozen_time as frozenTime,
                   thaw_time as thawTime,
                   create_time as createTime,
                   update_time as updateTime
            from eb_user_integral_record
            where id = #{id}
              and thaw_time <= #{nowMillis}
              and link_type = 'order'
              and type = 1
              and status = 2
            for update
            """)
    UserIntegralRecord selectDueFrozenRecordForUpdate(@Param("id") Integer id, @Param("nowMillis") long nowMillis);

    @Select("select integral from eb_user where uid = #{uid} for update")
    Integer selectUserIntegralForUpdate(@Param("uid") Integer uid);

    @Update("""
            update eb_user_integral_record
            set status = 3,
                balance = #{balance},
                update_time = current_timestamp
            where id = #{id}
              and thaw_time <= #{nowMillis}
              and link_type = 'order'
              and type = 1
              and status = 2
            """)
    int markComplete(
            @Param("id") Integer id,
            @Param("balance") Integer balance,
            @Param("nowMillis") long nowMillis);

    @Update("""
            update eb_user
            set integral = #{balance},
                update_time = current_timestamp
            where uid = #{uid}
            """)
    int updateUserIntegral(@Param("uid") Integer uid, @Param("balance") Integer balance);
}
