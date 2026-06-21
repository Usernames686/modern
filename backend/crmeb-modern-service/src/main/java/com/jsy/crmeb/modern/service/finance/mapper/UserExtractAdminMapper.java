package com.jsy.crmeb.modern.service.finance.mapper;

import com.jsy.crmeb.modern.service.finance.dto.UserExtractResponse;
import com.jsy.crmeb.modern.service.finance.dto.UserExtractUpdateRequest;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserExtractAdminMapper {
    @Select("""
            <script>
            select count(*)
            from eb_user_extract e
            <where>
              <if test="keywords != null and keywords != ''">
                and (
                  e.wechat like concat('%', #{keywords}, '%')
                  or e.real_name like concat('%', #{keywords}, '%')
                  or e.bank_code like concat('%', #{keywords}, '%')
                  or e.bank_address like concat('%', #{keywords}, '%')
                  or e.alipay_code like concat('%', #{keywords}, '%')
                  or e.fail_msg like concat('%', #{keywords}, '%')
                )
              </if>
              <if test="extractType != null and extractType != ''">
                and e.extract_type = #{extractType}
              </if>
              <if test="status != null">
                and e.status = #{status}
              </if>
              <if test="startTime != null and endTime != null">
                and e.create_time between #{startTime} and #{endTime}
              </if>
            </where>
            </script>
            """)
    long countExtracts(
            @Param("keywords") String keywords,
            @Param("extractType") String extractType,
            @Param("status") Integer status,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    @Select("""
            <script>
            select e.id,
                   e.uid,
                   e.real_name as realName,
                   e.extract_type as extractType,
                   e.bank_code as bankCode,
                   e.bank_address as bankAddress,
                   e.alipay_code as alipayCode,
                   e.extract_price as extractPrice,
                   e.mark,
                   e.balance,
                   e.fail_msg as failMsg,
                   e.status,
                   e.wechat,
                   e.create_time as createTime,
                   e.update_time as updateTime,
                   e.fail_time as failTime,
                   e.bank_name as bankName,
                   e.qrcode_url as qrcodeUrl,
                   coalesce(u.nickname, '') as nickName
            from eb_user_extract e
            left join eb_user u on u.uid = e.uid
            <where>
              <if test="keywords != null and keywords != ''">
                and (
                  e.wechat like concat('%', #{keywords}, '%')
                  or e.real_name like concat('%', #{keywords}, '%')
                  or e.bank_code like concat('%', #{keywords}, '%')
                  or e.bank_address like concat('%', #{keywords}, '%')
                  or e.alipay_code like concat('%', #{keywords}, '%')
                  or e.fail_msg like concat('%', #{keywords}, '%')
                )
              </if>
              <if test="extractType != null and extractType != ''">
                and e.extract_type = #{extractType}
              </if>
              <if test="status != null">
                and e.status = #{status}
              </if>
              <if test="startTime != null and endTime != null">
                and e.create_time between #{startTime} and #{endTime}
              </if>
            </where>
            order by e.create_time desc, e.id desc
            limit #{offset}, #{limit}
            </script>
            """)
    List<UserExtractResponse> selectExtracts(
            @Param("keywords") String keywords,
            @Param("extractType") String extractType,
            @Param("status") Integer status,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Select("""
            select coalesce(sum(extract_price), 0)
            from eb_user_extract
            where status = #{status}
              and (#{startTime} is null or create_time between #{startTime} and #{endTime})
            """)
    BigDecimal sumExtractByStatus(@Param("status") Integer status, @Param("startTime") String startTime, @Param("endTime") String endTime);

    @Select("""
            select coalesce(sum(price), 0)
            from eb_user_brokerage_record
            where link_type = 'order'
              and type = 1
              and status = 3
              and (#{startTime} is null or update_time between #{startTime} and #{endTime})
            """)
    BigDecimal sumCompletedOrderBrokerage(@Param("startTime") String startTime, @Param("endTime") String endTime);

    @Select("""
            select coalesce(sum(price), 0)
            from eb_user_brokerage_record
            where type = 2
              and status = 3
              and (#{startTime} is null or update_time between #{startTime} and #{endTime})
            """)
    BigDecimal sumCompletedSubBrokerage(@Param("startTime") String startTime, @Param("endTime") String endTime);

    @Select("""
            select e.id,
                   e.uid,
                   e.real_name as realName,
                   e.extract_type as extractType,
                   e.bank_code as bankCode,
                   e.bank_address as bankAddress,
                   e.alipay_code as alipayCode,
                   e.extract_price as extractPrice,
                   e.mark,
                   e.balance,
                   e.fail_msg as failMsg,
                   e.status,
                   e.wechat,
                   e.create_time as createTime,
                   e.update_time as updateTime,
                   e.fail_time as failTime,
                   e.bank_name as bankName,
                   e.qrcode_url as qrcodeUrl
            from eb_user_extract e
            where e.id = #{id}
            for update
            """)
    UserExtractResponse selectExtractForUpdate(@Param("id") Integer id);

    @Update("""
            update eb_user_extract
            set real_name = #{request.realName},
                extract_type = #{request.extractType},
                bank_code = #{request.bankCode},
                bank_address = #{request.bankAddress},
                alipay_code = #{request.alipayCode},
                extract_price = #{request.extractPrice},
                mark = #{request.mark},
                wechat = #{request.wechat},
                bank_name = #{request.bankName},
                qrcode_url = #{request.qrcodeUrl},
                update_time = current_timestamp
            where id = #{request.id}
            """)
    int updateExtract(@Param("request") UserExtractUpdateRequest request);

    @Update("""
            update eb_user_extract
            set status = -1,
                fail_msg = #{backMessage},
                fail_time = current_timestamp,
                update_time = current_timestamp
            where id = #{id} and status = 0
            """)
    int rejectExtract(@Param("id") Integer id, @Param("backMessage") String backMessage);

    @Update("""
            update eb_user_extract
            set status = 1,
                update_time = current_timestamp
            where id = #{id} and status = 0
            """)
    int passExtract(@Param("id") Integer id);

    @Select("select id from eb_user_brokerage_record where link_id = #{linkId} and link_type = 'withdraw' for update")
    Integer selectWithdrawBrokerageIdForUpdate(@Param("linkId") String linkId);

    @Update("""
            update eb_user_brokerage_record
            set status = 3,
                update_time = current_timestamp
            where id = #{id}
            """)
    int markBrokerageComplete(@Param("id") Integer id);
}
