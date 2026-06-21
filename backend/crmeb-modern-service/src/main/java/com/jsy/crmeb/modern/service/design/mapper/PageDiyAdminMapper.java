package com.jsy.crmeb.modern.service.design.mapper;

import com.jsy.crmeb.modern.service.design.dto.PageDiyResponse;
import com.jsy.crmeb.modern.service.design.dto.PageDiySaveRequest;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PageDiyAdminMapper {
    @Select("""
            <script>
            select count(*)
            from eb_page_diy
            where mer_id = 0
              and is_del = 0
              <if test="name != null and name != ''">
                and name like concat('%', #{name}, '%')
              </if>
            </script>
            """)
    long countList(@Param("name") String name);

    @Select("""
            <script>
            select id, version, name, title, cover_image as coverImage, template_name as templateName,
                   add_time as addTime, update_time as updateTime, status, type, is_show as isShow,
                   is_bg_color as isBgColor, is_bg_pic as isBgPic, is_diy as isDiy,
                   color_picker as colorPicker, bg_pic as bgPic, bg_tab_val as bgTabVal,
                   is_del as isDel, return_address as returnAddress, title_bg_color as titleBgColor,
                   title_color as titleColor, service_status as serviceStatus, mer_id as merId,
                   is_default as isDefault, text_position as textPosition
            from eb_page_diy
            where mer_id = 0
              and is_del = 0
              <if test="name != null and name != ''">
                and name like concat('%', #{name}, '%')
              </if>
            order by is_default desc, add_time desc, id desc
            limit #{offset}, #{limit}
            </script>
            """)
    List<PageDiyResponse> selectList(@Param("name") String name, @Param("offset") int offset, @Param("limit") int limit);

    @Select("""
            select id, version, name, title, cover_image as coverImage, template_name as templateName, value,
                   add_time as addTime, update_time as updateTime, status, type, is_show as isShow,
                   is_bg_color as isBgColor, is_bg_pic as isBgPic, is_diy as isDiy,
                   color_picker as colorPicker, bg_pic as bgPic, bg_tab_val as bgTabVal,
                   is_del as isDel, return_address as returnAddress, title_bg_color as titleBgColor,
                   title_color as titleColor, service_status as serviceStatus, mer_id as merId,
                   is_default as isDefault, text_position as textPosition
            from eb_page_diy
            where id = #{id}
            """)
    PageDiyResponse selectInfo(@Param("id") Integer id);

    @Select("""
            select id, version, name, title, cover_image as coverImage, template_name as templateName, value,
                   add_time as addTime, update_time as updateTime, status, type, is_show as isShow,
                   is_bg_color as isBgColor, is_bg_pic as isBgPic, is_diy as isDiy,
                   color_picker as colorPicker, bg_pic as bgPic, bg_tab_val as bgTabVal,
                   is_del as isDel, return_address as returnAddress, title_bg_color as titleBgColor,
                   title_color as titleColor, service_status as serviceStatus, mer_id as merId,
                   is_default as isDefault, text_position as textPosition
            from eb_page_diy
            where is_default = 1 and mer_id = 0 and is_del = 0
            limit 1
            """)
    PageDiyResponse selectDefault();

    @Select("""
            select count(*)
            from eb_page_diy
            where name = #{name}
              and is_del = 0
              and mer_id = 0
              and (#{id} is null or id <> #{id})
            """)
    long countSameName(@Param("name") String name, @Param("id") Integer id);

    @Update("update eb_page_diy set is_default = 0, update_time = current_timestamp where is_default = 1 and mer_id = 0")
    int clearDefault();

    @Update("update eb_page_diy set is_default = 1, update_time = current_timestamp where id = #{id} and is_del = 0 and mer_id = 0")
    int setDefault(@Param("id") Integer id);

    @Update("update eb_page_diy set is_del = 1, update_time = current_timestamp where id = #{id} and is_default <> 1")
    int deleteById(@Param("id") Integer id);

    @Insert("""
            insert into eb_page_diy
            (version, name, title, cover_image, template_name, value, status, type, is_show, is_bg_color,
             is_bg_pic, is_diy, color_picker, bg_pic, bg_tab_val, is_del, return_address, title_bg_color,
             title_color, service_status, mer_id, is_default, text_position)
            values
            (#{request.version}, #{request.name}, #{request.title}, #{request.coverImage}, #{request.templateName},
             #{request.value}, #{request.status}, #{request.type}, #{request.isShow}, #{request.isBgColor},
             #{request.isBgPic}, #{request.isDiy}, #{request.colorPicker}, #{request.bgPic}, #{request.bgTabVal},
             0, #{request.returnAddress}, #{request.titleBgColor}, #{request.titleColor}, #{request.serviceStatus},
             0, 0, #{request.textPosition})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "request.id")
    int insertPage(@Param("request") PageDiyResponse request);

    @Update("""
            update eb_page_diy
            set name = #{request.name},
                title = #{request.title},
                cover_image = #{request.coverImage},
                template_name = #{request.templateName},
                value = #{request.value},
                status = #{request.status},
                type = #{request.type},
                is_show = #{request.isShow},
                is_bg_color = #{request.isBgColor},
                is_bg_pic = #{request.isBgPic},
                is_diy = #{request.isDiy},
                color_picker = #{request.colorPicker},
                bg_pic = #{request.bgPic},
                bg_tab_val = #{request.bgTabVal},
                return_address = #{request.returnAddress},
                title_bg_color = #{request.titleBgColor},
                title_color = #{request.titleColor},
                service_status = #{request.serviceStatus},
                text_position = #{request.textPosition},
                update_time = current_timestamp
            where id = #{request.id}
              and is_del = 0
              and mer_id = 0
            """)
    int updateBase(@Param("request") PageDiySaveRequest request);
}
