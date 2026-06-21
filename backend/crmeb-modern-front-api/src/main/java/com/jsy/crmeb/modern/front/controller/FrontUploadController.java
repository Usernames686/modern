package com.jsy.crmeb.modern.front.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.service.upload.UploadService;
import com.jsy.crmeb.modern.service.upload.dto.FileResultResponse;
import java.io.IOException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
public class FrontUploadController {
    private final UploadService uploadService;

    public FrontUploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/api/front/upload/image")
    public ApiResponse<FileResultResponse> image(
        MultipartHttpServletRequest request,
        @RequestParam(name = "model") String model,
        @RequestParam(name = "pid") Integer pid) {
        MultipartFile file = resolveFile(request);
        return ApiResponse.ok(imageUpload(file, model, pid));
    }

    @PostMapping("/api/front/upload/file")
    public ApiResponse<FileResultResponse> file(
        MultipartHttpServletRequest request,
        @RequestParam(name = "model") String model,
        @RequestParam(name = "pid") Integer pid) {
        MultipartFile file = resolveFile(request);
        return ApiResponse.ok(fileUpload(file, model, pid));
    }

    @PostMapping("/api/front/user/upload/image")
    public ApiResponse<FileResultResponse> userImage(
        MultipartHttpServletRequest request,
        @RequestParam(name = "model") String model,
        @RequestParam(name = "pid") Integer pid) {
        MultipartFile file = resolveFile(request);
        return ApiResponse.ok(imageUpload(file, model, pid));
    }

    private MultipartFile resolveFile(MultipartHttpServletRequest request) {
        MultipartFile multipart = request.getFile("multipart");
        if (multipart != null) {
            return multipart;
        }
        multipart = request.getFile("file");
        if (multipart != null) {
            return multipart;
        }
        return request.getFileMap().values().stream().findFirst().orElse(null);
    }

    private FileResultResponse imageUpload(MultipartFile file, String model, Integer pid) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上载的文件对象不存在...");
        }
        try {
            return uploadService.imageUpload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getInputStream(), model, pid);
        } catch (IOException exception) {
            throw new IllegalArgumentException("文件上传 IO异常");
        }
    }

    private FileResultResponse fileUpload(MultipartFile file, String model, Integer pid) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上载的文件对象不存在...");
        }
        try {
            return uploadService.fileUpload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getInputStream(), model, pid);
        } catch (IOException exception) {
            throw new IllegalArgumentException("文件上传 IO异常");
        }
    }
}
