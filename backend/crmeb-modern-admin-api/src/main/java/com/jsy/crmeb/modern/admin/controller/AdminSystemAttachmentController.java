package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.upload.SystemAttachmentService;
import com.jsy.crmeb.modern.service.upload.dto.AttachmentMoveRequest;
import com.jsy.crmeb.modern.service.upload.dto.AttachmentRequest;
import com.jsy.crmeb.modern.service.upload.entity.SystemAttachment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminSystemAttachmentController {
    private final SystemAttachmentService systemAttachmentService;

    public AdminSystemAttachmentController(SystemAttachmentService systemAttachmentService) {
        this.systemAttachmentService = systemAttachmentService;
    }

    @GetMapping("/api/admin/system/attachment/list")
    public ApiResponse<PageResponse<SystemAttachment>> list(
            @RequestParam(name = "pid", required = false, defaultValue = "0") Integer pid,
            @RequestParam(name = "attType", required = false) String attType,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit) {
        return ApiResponse.ok(systemAttachmentService.list(pid, attType, page, limit));
    }

    @PostMapping("/api/admin/system/attachment/save")
    public ApiResponse<Void> save(@RequestBody AttachmentRequest request) {
        systemAttachmentService.save(request);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/system/attachment/delete/{ids}")
    public ApiResponse<Void> delete(@PathVariable(name = "ids") String ids) {
        systemAttachmentService.delete(ids);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/system/attachment/update")
    public ApiResponse<Void> update(
            @RequestParam(name = "id") Integer id,
            @RequestBody AttachmentRequest request) {
        systemAttachmentService.update(id, request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/system/attachment/move")
    public ApiResponse<Void> move(@RequestBody AttachmentMoveRequest request) {
        systemAttachmentService.move(request);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/system/attachment/info/{id}")
    public ApiResponse<SystemAttachment> info(@PathVariable(name = "id") Integer id) {
        return ApiResponse.ok(systemAttachmentService.info(id));
    }
}
