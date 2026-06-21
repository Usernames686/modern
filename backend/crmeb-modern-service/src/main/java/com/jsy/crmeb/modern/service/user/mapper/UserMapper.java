package com.jsy.crmeb.modern.service.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jsy.crmeb.modern.service.user.entity.User;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Update("update eb_user set group_id = #{groupId}, update_time = current_timestamp where uid = #{uid}")
    int updateGroupId(@Param("uid") Integer uid, @Param("groupId") String groupId);

    @Update("update eb_user set tag_id = #{tagId}, update_time = current_timestamp where uid = #{uid}")
    int updateTagId(@Param("uid") Integer uid, @Param("tagId") String tagId);

    @Update("update eb_user set group_id = '', update_time = current_timestamp where group_id = #{groupId}")
    int clearGroupId(String groupId);

    @Update("update eb_user set tag_id = trim(both ',' from replace(concat(',', tag_id, ','), concat(',', #{tagId}, ','), ',')), update_time = current_timestamp where find_in_set(#{tagId}, tag_id)")
    int clearTagId(String tagId);

    @Update("update eb_user set phone = #{phone}, account = #{phone}, update_time = current_timestamp where uid = #{uid}")
    int updatePhone(@Param("uid") Integer uid, @Param("phone") String phone);

    @Update("update eb_user set level = #{levelId}, update_time = current_timestamp where uid = #{uid}")
    int updateLevel(@Param("uid") Integer uid, @Param("levelId") Integer levelId);

    @Update("update eb_user set level = 0, update_time = current_timestamp where level = #{levelId}")
    int clearLevelByLevelId(Integer levelId);

    @Update("""
            update eb_user
            set real_name = #{realName},
                birthday = #{birthday},
                card_id = #{cardId},
                mark = #{mark},
                status = #{status},
                addres = #{addres},
                group_id = #{groupId},
                tag_id = #{tagId},
                is_promoter = #{isPromoter},
                update_time = current_timestamp
            where uid = #{uid}
            """)
    int updateBasic(
            @Param("uid") Integer uid,
            @Param("realName") String realName,
            @Param("birthday") String birthday,
            @Param("cardId") String cardId,
            @Param("mark") String mark,
            @Param("status") Integer status,
            @Param("addres") String addres,
            @Param("groupId") String groupId,
            @Param("tagId") String tagId,
            @Param("isPromoter") Integer isPromoter);

    @Update("update eb_user set spread_uid = #{spreadUid}, spread_time = #{spreadTime}, update_time = current_timestamp where uid = #{uid}")
    int updateSpread(@Param("uid") Integer uid, @Param("spreadUid") Integer spreadUid, @Param("spreadTime") LocalDateTime spreadTime);

    @Update("update eb_user set spread_uid = 0, spread_time = null, update_time = current_timestamp where uid = #{uid}")
    int clearSpread(@Param("uid") Integer uid);

    @Update("update eb_user set spread_count = greatest(coalesce(spread_count, 0) + #{delta}, 0), update_time = current_timestamp where uid = #{uid}")
    int updateSpreadCount(@Param("uid") Integer uid, @Param("delta") Integer delta);

    @Update("update eb_user set now_money = #{nowMoney}, integral = #{integral}, update_time = current_timestamp where uid = #{uid}")
    int updateMoneyAndIntegral(
            @Param("uid") Integer uid,
            @Param("nowMoney") java.math.BigDecimal nowMoney,
            @Param("integral") Integer integral);

    @Update("""
            update eb_user
            set now_money = now_money - #{amount},
                update_time = current_timestamp
            where uid = #{uid}
              and now_money >= #{amount}
            """)
    int deductBalanceForPayment(@Param("uid") Integer uid, @Param("amount") java.math.BigDecimal amount);

    @Update("""
            update eb_user
            set now_money = now_money - #{amount},
                integral = integral - #{integral},
                update_time = current_timestamp
            where uid = #{uid}
              and now_money >= #{amount}
              and integral >= #{integral}
            """)
    int deductBalanceAndIntegralForPayment(
            @Param("uid") Integer uid,
            @Param("amount") java.math.BigDecimal amount,
            @Param("integral") Integer integral);
}
