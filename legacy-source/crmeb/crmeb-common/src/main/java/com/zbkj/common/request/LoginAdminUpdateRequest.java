package com.zbkj.common.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

/**
 * 登录用户修改信息请求对象


 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "LoginAdminUpdateRequest", description = "登录用户修改信息请求对象")
public class LoginAdminUpdateRequest {

    @ApiModelProperty(value = "后台管理员昵称", required = true)
    @Size(max = 16, message = "后台管理员昵称长度不能超过16个字符")
    private String realName;

}
