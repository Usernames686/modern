package com.jsy.crmeb.modern.service.bargain.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StoreBargainTaskMapper {
    @Update("""
            update eb_store_bargain_user user
            set user.status = 2
            where user.status = 1
              and user.is_del = 0
              and exists (
                  select 1
                  from eb_store_bargain bargain
                  where bargain.id = user.bargain_id
                    and bargain.status = 1
                    and bargain.stop_time < #{nowMillis}
              )
            """)
    int markExpiredParticipatingUsersFailed(@Param("nowMillis") long nowMillis);
}
