package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.service.upload.UploadService;
import com.jsy.crmeb.modern.service.upload.dto.FileResultResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AdminWechatMediaController {
    private final UploadService uploadService;

    public AdminWechatMediaController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/api/admin/wechat/media/upload")
    public ApiResponse<Map<String, Object>> upload(
            @RequestParam("media") MultipartFile file,
            @RequestParam("type") String type) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("待上传素材不能为空");
        }
        String mediaType = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        FileResultResponse result;
        if ("image".equals(mediaType)) {
            result = uploadService.imageUpload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getInputStream(), "wechat", 0);
        } else if ("voice".equals(mediaType)) {
            result = uploadService.fileUpload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getInputStream(), "wechat", 0);
        } else {
            throw new IllegalArgumentException("不支持此类型");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("mediaId", "LOCAL-" + result.url().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT));
        data.put("url", result.url());
        data.put("name", stripExt(result.fileName()));
        data.put("localMode", true);
        data.put("message", "素材已保存到附件库，微信平台上传未配置。");
        return ApiResponse.ok(data);
    }

    private String stripExt(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int dot = value.lastIndexOf('.');
        return dot > 0 ? value.substring(0, dot) : value;
    }
}
