package com.jsy.crmeb.modern.service.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.user.entity.UserBrokerageRecord;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserBrokerageRecordMapper extends BaseMapper<UserBrokerageRecord> {
    @Select("""
            select id,
                   uid,
                   link_id as linkId,
                   link_type as linkType,
                   type,
                   title,
                   price,
                   balance,
                   mark,
                   status,
                   frozen_time as frozenTime,
                   thaw_time as thawTime,
                   create_time as createTime,
                   update_time as updateTime,
                   brokerage_level as brokerageLevel
            from eb_user_brokerage_record
            where thaw_time <= #{nowMillis}
              and link_type = 'order'
              and type = 1
              and status = 2
            order by id asc
            """)
    List<UserBrokerageRecord> selectDueFrozenOrderAddRecords(@Param("nowMillis") long nowMillis);

    @Select("""
            select id,
                   uid,
                   link_id as linkId,
                   link_type as linkType,
                   type,
                   title,
                   price,
                   balance,
                   mark,
                   status,
                   frozen_time as frozenTime,
                   thaw_time as thawTime,
                   create_time as createTime,
                   update_time as updateTime,
                   brokerage_level as brokerageLevel
            from eb_user_brokerage_record
            where id = #{id}
              and thaw_time <= #{nowMillis}
              and link_type = 'order'
              and type = 1
              and status = 2
            for update
            """)
    UserBrokerageRecord selectDueFrozenRecordForUpdate(@Param("id") Integer id, @Param("nowMillis") long nowMillis);

    @Select("select brokerage_price from eb_user where uid = #{uid} for update")
    BigDecimal selectUserBrokeragePriceForUpdate(@Param("uid") Integer uid);

    @Update("""
            update eb_user_brokerage_record
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
            @Param("balance") BigDecimal balance,
            @Param("nowMillis") long nowMillis);

    @Update("""
            update eb_user
            set brokerage_price = #{balance},
                update_time = current_timestamp
            where uid = #{uid}
            """)
    int updateUserBrokeragePrice(@Param("uid") Integer uid, @Param("balance") BigDecimal balance);
}
