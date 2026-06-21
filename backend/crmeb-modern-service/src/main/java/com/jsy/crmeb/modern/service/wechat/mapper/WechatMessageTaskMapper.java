package com.jsy.crmeb.modern.service.wechat.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WechatMessageTaskMapper {
    @Select("select value from eb_system_config where name = #{name} limit 1")
    String selectConfigValue(@Param("name") String name);

    @Insert("""
            insert into eb_wechat_exceptions(errcode, errmsg, data, remark, create_time, update_time)
            values(#{errcode}, #{errmsg}, #{data}, #{remark}, current_timestamp, current_timestamp)
            """)
    int insertException(
            @Param("errcode") String errcode,
            @Param("errmsg") String errmsg,
            @Param("data") String data,
            @Param("remark") String remark);
}
