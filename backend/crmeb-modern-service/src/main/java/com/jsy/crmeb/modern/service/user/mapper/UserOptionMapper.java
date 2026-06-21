package com.jsy.crmeb.modern.service.user.mapper;

import com.jsy.crmeb.modern.service.user.dto.SystemUserLevelResponse;
import com.jsy.crmeb.modern.service.user.dto.UserGroupResponse;
import com.jsy.crmeb.modern.service.user.dto.UserTagResponse;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserOptionMapper {
    @Select("select id, group_name as groupName from eb_user_group order by id asc")
    List<UserGroupResponse> selectGroups();

    @Select("select id, name from eb_user_tag order by id asc")
    List<UserTagResponse> selectTags();

    @Select("select id, group_name as groupName from eb_user_group where id = #{id}")
    UserGroupResponse selectGroupById(Integer id);

    @Select("select id, name from eb_user_tag where id = #{id}")
    UserTagResponse selectTagById(Integer id);

    @Select("""
            select count(1)
            from eb_user_group
            where group_name = #{groupName}
              and (#{ignoreId} is null or id <> #{ignoreId})
            """)
    long countGroupName(@Param("groupName") String groupName, @Param("ignoreId") Integer ignoreId);

    @Select("""
            select count(1)
            from eb_user_tag
            where name = #{name}
              and (#{ignoreId} is null or id <> #{ignoreId})
            """)
    long countTagName(@Param("name") String name, @Param("ignoreId") Integer ignoreId);

    @Insert("insert into eb_user_group(group_name) values(#{groupName})")
    int insertGroup(String groupName);

    @Insert("insert into eb_user_tag(name) values(#{name})")
    int insertTag(String name);

    @Update("update eb_user_group set group_name = #{groupName} where id = #{id}")
    int updateGroup(@Param("id") Integer id, @Param("groupName") String groupName);

    @Update("update eb_user_tag set name = #{name} where id = #{id}")
    int updateTag(@Param("id") Integer id, @Param("name") String name);

    @Delete("delete from eb_user_group where id = #{id}")
    int deleteGroup(Integer id);

    @Delete("delete from eb_user_tag where id = #{id}")
    int deleteTag(Integer id);

    @Select("""
            select id,
                   name,
                   experience,
                   is_show as isShow,
                   grade,
                   discount,
                   icon
            from eb_system_user_level
            where is_del = 0
            order by grade asc, id asc
            """)
    List<SystemUserLevelResponse> selectLevels();

    @Select("""
            select id,
                   name,
                   experience,
                   is_show as isShow,
                   grade,
                   discount,
                   icon
            from eb_system_user_level
            where is_del = 0
              and id = #{id}
            """)
    SystemUserLevelResponse selectLevel(@Param("id") Integer id);

    @Select("""
            select count(1)
            from eb_system_user_level
            where is_del = 0
              and name = #{name}
              and (#{ignoreId} is null or id <> #{ignoreId})
            """)
    long countLevelName(@Param("name") String name, @Param("ignoreId") Integer ignoreId);

    @Select("""
            select count(1)
            from eb_system_user_level
            where is_del = 0
              and grade = #{grade}
              and (#{ignoreId} is null or id <> #{ignoreId})
            """)
    long countLevelGrade(@Param("grade") Integer grade, @Param("ignoreId") Integer ignoreId);

    @Insert("""
            insert into eb_system_user_level(name, experience, is_show, grade, discount, icon, is_del, create_time, update_time)
            values(#{name}, #{experience}, #{isShow}, #{grade}, #{discount}, #{icon}, 0, current_timestamp, current_timestamp)
            """)
    int insertLevel(
            @Param("name") String name,
            @Param("experience") Integer experience,
            @Param("isShow") Boolean isShow,
            @Param("grade") Integer grade,
            @Param("discount") Integer discount,
            @Param("icon") String icon);

    @Update("""
            update eb_system_user_level
            set name = #{name},
                experience = #{experience},
                is_show = #{isShow},
                grade = #{grade},
                discount = #{discount},
                icon = #{icon},
                update_time = current_timestamp
            where id = #{id}
            """)
    int updateLevel(
            @Param("id") Integer id,
            @Param("name") String name,
            @Param("experience") Integer experience,
            @Param("isShow") Boolean isShow,
            @Param("grade") Integer grade,
            @Param("discount") Integer discount,
            @Param("icon") String icon);

    @Update("update eb_system_user_level set is_del = 1, update_time = current_timestamp where id = #{id}")
    int deleteLevel(Integer id);

    @Update("update eb_system_user_level set is_show = #{isShow}, update_time = current_timestamp where id = #{id}")
    int updateLevelShow(@Param("id") Integer id, @Param("isShow") Boolean isShow);

    @Update("update eb_user_level set status = 0 where level_id = #{levelId}")
    int disableUserLevelRecords(Integer levelId);

    @Delete("delete from eb_user_level where level_id = #{levelId}")
    int deleteUserLevelRecords(Integer levelId);

    @Select("""
            select id,
                   name,
                   experience,
                   is_show as isShow,
                   grade,
                   discount,
                   icon
            from eb_system_user_level
            where is_del = 0
              and id = #{id}
            """)
    SystemUserLevelResponse selectLevelById(Integer id);
}
