package com.jsy.crmeb.modern.service.upload.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UploadMapper {
    @Select("select value from eb_system_config where name = #{name} limit 1")
    String findConfigValue(@Param("name") String name);

    @Insert("""
            insert into eb_system_attachment(name, att_dir, satt_dir, att_size, att_type, pid, image_type, create_time, update_time)
            values(#{name}, '', #{sattDir}, #{attSize}, #{attType}, #{pid}, 1, now(), now())
            """)
    int insertAttachment(
            @Param("name") String name,
            @Param("sattDir") String sattDir,
            @Param("attSize") String attSize,
            @Param("attType") String attType,
            @Param("pid") Integer pid);
}
