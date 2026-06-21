package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.service.system.entity.TemplateMessage;
import com.jsy.crmeb.modern.service.wechat.WechatTemplateAdminService;
import com.jsy.crmeb.modern.service.wechat.dto.WechatProgramMyTempRequest;
import com.jsy.crmeb.modern.service.wechat.dto.WechatTemplateRequest;
import com.jsy.crmeb.modern.service.wechat.entity.WechatProgramMyTemp;
import com.jsy.crmeb.modern.service.wechat.entity.WechatProgramPublicTemp;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminWechatTemplateController {
    private final WechatTemplateAdminService templateAdminService;

    public AdminWechatTemplateController(WechatTemplateAdminService templateAdminService) {
        this.templateAdminService = templateAdminService;
    }

    @GetMapping("/api/admin/wechat/template/list")
    public ApiResponse<Map<String, Object>> templateList(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "keywords", required = false) String keywords,
            @RequestParam(name = "status", required = false) Integer status) {
        return ApiResponse.ok(templateAdminService.templateList(page, limit, type, keywords, status));
    }

    @PostMapping("/api/admin/wechat/template/save")
    public ApiResponse<Boolean> templateSave(@RequestBody WechatTemplateRequest request) {
        return ApiResponse.ok(templateAdminService.templateSave(request));
    }

    @PostMapping("/api/admin/wechat/template/update/{id}")
    public ApiResponse<Boolean> templateUpdate(@PathVariable("id") Integer id, @RequestBody WechatTemplateRequest request) {
        return ApiResponse.ok(templateAdminService.templateUpdate(id, request));
    }

    @GetMapping("/api/admin/wechat/template/info/{id}")
    public ApiResponse<TemplateMessage> templateInfo(@PathVariable("id") Integer id) {
        return ApiResponse.ok(templateAdminService.templateInfo(id));
    }

    @PostMapping("/api/admin/wechat/template/update/status/{id}")
    public ApiResponse<Boolean> templateStatus(
            @PathVariable("id") Integer id,
            @RequestParam(name = "status", required = false) Integer status) {
        return ApiResponse.ok(templateAdminService.templateStatus(id, status));
    }

    @GetMapping("/api/admin/wechat/template/delete/{id}")
    public ApiResponse<Boolean> templateDelete(@PathVariable("id") Integer id) {
        return ApiResponse.ok(templateAdminService.templateDelete(id));
    }

    @PostMapping("/api/admin/wechat/template/whcbqhn/sync")
    public ApiResponse<Map<String, Object>> wechatSync() {
        return ApiResponse.ok(templateAdminService.safeSync("公众号模板消息同步"));
    }

    @PostMapping("/api/admin/wechat/template/routine/sync")
    public ApiResponse<Map<String, Object>> routineSync() {
        return ApiResponse.ok(templateAdminService.safeSync("小程序订阅消息同步"));
    }

    @GetMapping("/api/admin/wechat/program/public/temp/list")
    public ApiResponse<Map<String, Object>> publicTempList(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "tid", required = false) Integer tid,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "categoryId", required = false) Integer categoryId) {
        return ApiResponse.ok(templateAdminService.publicTempList(page, limit, tid, title, type, categoryId));
    }

    @GetMapping("/api/admin/wechat/program/category")
    public ApiResponse<List<Map<String, Object>>> categoryList() {
        return ApiResponse.ok(templateAdminService.categoryList());
    }

    @GetMapping("/api/admin/wechat/program/getWeChatKeywordsByTid")
    public ApiResponse<Map<String, Object>> keywordsByTid(@RequestParam(name = "tid", required = false) Integer tid) {
        return ApiResponse.ok(templateAdminService.keywordsByTid(tid));
    }

    @GetMapping("/api/admin/wechat/program/public/temp/info")
    public ApiResponse<WechatProgramPublicTemp> publicTempInfo(
            @RequestParam(name = "id", required = false) Integer id,
            @RequestParam(name = "tid", required = false) Integer tid) {
        return ApiResponse.ok(templateAdminService.publicTempInfo(id, tid));
    }

    @GetMapping("/api/admin/wechat/program/my/temp/list")
    public ApiResponse<Map<String, Object>> myTempList(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "id", required = false) Integer id,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "tempId", required = false) String tempId,
            @RequestParam(name = "status", required = false) Boolean status,
            @RequestParam(name = "type", required = false) String type) {
        return ApiResponse.ok(templateAdminService.myTempList(page, limit, id, title, tempId, status, type));
    }

    @GetMapping("/api/admin/wechat/program/my/temp/info")
    public ApiResponse<WechatProgramMyTemp> myTempInfo(@RequestParam("id") Integer id) {
        return ApiResponse.ok(templateAdminService.myTempInfo(id));
    }

    @PostMapping("/api/admin/wechat/program/my/temp/save")
    public ApiResponse<Boolean> myTempSave(@RequestBody WechatProgramMyTempRequest request) {
        return ApiResponse.ok(templateAdminService.myTempSave(request));
    }

    @PostMapping("/api/admin/wechat/program/my/temp/update")
    public ApiResponse<Boolean> myTempUpdate(
            @RequestParam("id") Integer id,
            @RequestBody WechatProgramMyTempRequest request) {
        return ApiResponse.ok(templateAdminService.myTempUpdate(id, request));
    }

    @GetMapping("/api/admin/wechat/program/my/temp/update/status")
    public ApiResponse<Boolean> myTempStatus(
            @RequestParam("id") Integer id,
            @RequestParam(name = "status", required = false) Boolean status) {
        return ApiResponse.ok(templateAdminService.myTempStatus(id, status));
    }

    @GetMapping("/api/admin/wechat/program/my/temp/update/type")
    public ApiResponse<Boolean> myTempType(
            @RequestParam("id") Integer id,
            @RequestParam(name = "type", required = false) String type) {
        return ApiResponse.ok(templateAdminService.myTempType(id, type));
    }

    @GetMapping("/api/admin/wechat/program/my/temp/delete")
    public ApiResponse<Boolean> myTempDelete(@RequestParam("id") Integer id) {
        return ApiResponse.ok(templateAdminService.myTempDelete(id));
    }

    @GetMapping("/api/admin/wechat/program/my/temp/async")
    public ApiResponse<Map<String, Object>> myTempAsync() {
        return ApiResponse.ok(templateAdminService.safeSync("一键同步我的模板到小程序"));
    }
}
