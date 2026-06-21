package com.jsy.crmeb.modern.service.finance.mapper;

import com.jsy.crmeb.modern.service.finance.dto.BrokerageRecordResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserBrokerageAdminMapper {
    @Select("""
            <script>
            select count(*)
            from eb_user_brokerage_record br
            left join eb_user u on u.uid = br.uid
            <where>
              <choose>
                <when test="filterType == 1">
                  br.link_type = 'order' and br.status = 3 and br.type = 1
                </when>
                <when test="filterType == 2">
                  br.link_type = 'withdraw' and br.status = 5 and br.type = 2
                </when>
                <when test="filterType == 3">
                  br.link_type = 'withdraw' and br.status = 3 and br.type = 1
                </when>
                <when test="filterType == 4">
                  br.link_type = 'withdraw' and br.status = 3 and br.type = 2
                </when>
                <when test="filterType == 5">
                  br.link_type = 'yue' and br.status = 3 and br.type = 2
                </when>
                <otherwise>
                  br.status in (3, 5)
                </otherwise>
              </choose>
            </where>
            </script>
            """)
    long countBrokerage(@Param("filterType") Integer filterType);

    @Select("""
            <script>
            select br.id,
                   br.uid,
                   br.link_id as linkId,
                   br.link_type as linkType,
                   br.type,
                   case
                     when br.link_type = 'withdraw' and br.status = 3 and br.type = 2 then '提现成功'
                     else br.title
                   end as title,
                   br.price,
                   br.balance,
                   br.mark,
                   br.status,
                   br.frozen_time as frozenTime,
                   br.thaw_time as thawTime,
                   br.create_time as createTime,
                   br.update_time as updateTime,
                   br.brokerage_level as brokerageLevel,
                   coalesce(u.nickname, '-') as userName
            from eb_user_brokerage_record br
            left join eb_user u on u.uid = br.uid
            <where>
              <choose>
                <when test="filterType == 1">
                  br.link_type = 'order' and br.status = 3 and br.type = 1
                </when>
                <when test="filterType == 2">
                  br.link_type = 'withdraw' and br.status = 5 and br.type = 2
                </when>
                <when test="filterType == 3">
                  br.link_type = 'withdraw' and br.status = 3 and br.type = 1
                </when>
                <when test="filterType == 4">
                  br.link_type = 'withdraw' and br.status = 3 and br.type = 2
                </when>
                <when test="filterType == 5">
                  br.link_type = 'yue' and br.status = 3 and br.type = 2
                </when>
                <otherwise>
                  br.status in (3, 5)
                </otherwise>
              </choose>
            </where>
            order by br.update_time desc, br.id desc
            limit #{offset}, #{limit}
            </script>
            """)
    List<BrokerageRecordResponse> selectBrokerage(
            @Param("filterType") Integer filterType,
            @Param("offset") int offset,
            @Param("limit") int limit);
}
