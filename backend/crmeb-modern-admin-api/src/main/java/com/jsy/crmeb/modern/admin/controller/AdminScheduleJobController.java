package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.schedule.ScheduleAdminService;
import com.jsy.crmeb.modern.service.schedule.dto.ScheduleJobRequest;
import com.jsy.crmeb.modern.service.schedule.entity.ScheduleJob;
import com.jsy.crmeb.modern.service.schedule.entity.ScheduleJobLog;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminScheduleJobController {
    private final ScheduleAdminService scheduleAdminService;

    public AdminScheduleJobController(ScheduleAdminService scheduleAdminService) {
        this.scheduleAdminService = scheduleAdminService;
    }

    @GetMapping("/api/admin/schedule/job/list")
    public ApiResponse<List<ScheduleJob>> list() {
        return ApiResponse.ok(scheduleAdminService.list());
    }

    @GetMapping("/api/admin/schedule/job/log/list")
    public ApiResponse<PageResponse<ScheduleJobLog>> logList(
            @RequestParam(name = "jobId", required = false) Integer jobId,
            @RequestParam(name = "beanName", required = false) String beanName,
            @RequestParam(name = "methodName", required = false) String methodName,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit) {
        return ApiResponse.ok(scheduleAdminService.logList(jobId, beanName, methodName, status, page, limit));
    }

    @PostMapping("/api/admin/schedule/job/add")
    public ApiResponse<Boolean> add(@RequestBody ScheduleJobRequest request) {
        return ApiResponse.ok(scheduleAdminService.add(request));
    }

    @PostMapping("/api/admin/schedule/job/update")
    public ApiResponse<Boolean> update(@RequestBody ScheduleJobRequest request) {
        return ApiResponse.ok(scheduleAdminService.update(request));
    }

    @PostMapping("/api/admin/schedule/job/suspend/{jobId}")
    public ApiResponse<Boolean> suspend(@PathVariable("jobId") Integer jobId) {
        return ApiResponse.ok(scheduleAdminService.suspend(jobId));
    }

    @PostMapping("/api/admin/schedule/job/start/{jobId}")
    public ApiResponse<Boolean> start(@PathVariable("jobId") Integer jobId) {
        return ApiResponse.ok(scheduleAdminService.start(jobId));
    }

    @PostMapping("/api/admin/schedule/job/delete/{jobId}")
    public ApiResponse<Boolean> delete(@PathVariable("jobId") Integer jobId) {
        return ApiResponse.ok(scheduleAdminService.delete(jobId));
    }

    @PostMapping("/api/admin/schedule/job/trig/{jobId}")
    public ApiResponse<Boolean> trig(@PathVariable("jobId") Integer jobId) {
        return ApiResponse.ok(scheduleAdminService.trig(jobId));
    }
}
