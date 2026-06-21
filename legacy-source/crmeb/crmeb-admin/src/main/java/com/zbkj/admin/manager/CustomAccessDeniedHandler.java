package com.zbkj.admin.manager;

import com.alibaba.fastjson.JSONObject;

import com.zbkj.common.result.CommonResult;
import com.zbkj.common.result.CommonResultCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;

/**
 * 自定义权限不足处理程序
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler, Serializable {

    private static final long serialVersionUID = -8970718410437077606L;

    @Override
    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException e) {
        httpServletResponse.setStatus(200);
        httpServletResponse.setContentType("application/json");
        httpServletResponse.setCharacterEncoding("utf-8");
        try {
            httpServletResponse.getWriter().print(JSONObject.toJSONString(CommonResult.failed(CommonResultCode.FORBIDDEN)));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
