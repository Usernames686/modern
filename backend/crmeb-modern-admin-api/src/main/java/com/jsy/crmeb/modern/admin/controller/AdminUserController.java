package com.jsy.crmeb.modern.admin.controller;

import com.jsy.crmeb.modern.common.web.ApiResponse;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.user.UserAdminService;
import com.jsy.crmeb.modern.service.user.dto.SystemUserLevelResponse;
import com.jsy.crmeb.modern.service.user.dto.SystemUserLevelRequest;
import com.jsy.crmeb.modern.service.user.dto.UpdateUserLevelRequest;
import com.jsy.crmeb.modern.service.user.dto.UserGroupRequest;
import com.jsy.crmeb.modern.service.user.dto.UserGroupResponse;
import com.jsy.crmeb.modern.service.user.dto.UserIntegralRecordResponse;
import com.jsy.crmeb.modern.service.user.dto.UserIntegralSearchRequest;
import com.jsy.crmeb.modern.service.user.dto.UserOperateIntegralMoneyRequest;
import com.jsy.crmeb.modern.service.user.dto.UserResponse;
import com.jsy.crmeb.modern.service.user.dto.UserSearchRequest;
import com.jsy.crmeb.modern.service.user.dto.UserTagRequest;
import com.jsy.crmeb.modern.service.user.dto.UserTagResponse;
import com.jsy.crmeb.modern.service.user.dto.UserUpdateRequest;
import com.jsy.crmeb.modern.service.user.dto.UserUpdateSpreadRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminUserController {
    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping("/api/admin/user/list")
    public ApiResponse<PageResponse<UserResponse>> userList(@ModelAttribute UserSearchRequest request) {
        return ApiResponse.ok(userAdminService.list(request));
    }

    @PostMapping("/api/admin/user/integral/list")
    public ApiResponse<PageResponse<UserIntegralRecordResponse>> integralList(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestBody(required = false) UserIntegralSearchRequest request) {
        UserIntegralSearchRequest safeRequest = request == null ? new UserIntegralSearchRequest() : request;
        if (page != null) {
            safeRequest.setPage(page);
        }
        if (limit != null) {
            safeRequest.setLimit(limit);
        }
        return ApiResponse.ok(userAdminService.integralRecords(safeRequest));
    }

    @GetMapping("/api/admin/user/info")
    public ApiResponse<UserResponse> info(@RequestParam("id") Integer id) {
        return ApiResponse.ok(userAdminService.info(id));
    }

    @PostMapping("/api/admin/user/update")
    public ApiResponse<Void> update(
            @RequestParam(name = "id", required = false) Integer id,
            @RequestBody UserUpdateRequest request) {
        userAdminService.update(request, id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/user/group/list")
    public ApiResponse<PageResponse<UserGroupResponse>> groupList(
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit) {
        return ApiResponse.ok(userAdminService.groups(page, limit));
    }

    @GetMapping("/api/admin/user/tag/list")
    public ApiResponse<PageResponse<UserTagResponse>> tagList(
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit) {
        return ApiResponse.ok(userAdminService.tags(page, limit));
    }

    @PostMapping("/api/admin/user/group/save")
    public ApiResponse<Void> groupSave(@RequestBody UserGroupRequest request) {
        userAdminService.saveGroup(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/user/tag/save")
    public ApiResponse<Void> tagSave(@RequestBody UserTagRequest request) {
        userAdminService.saveTag(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/user/group/update")
    public ApiResponse<Void> groupUpdate(@RequestParam("id") Integer id, @RequestBody UserGroupRequest request) {
        userAdminService.updateGroup(id, request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/user/tag/update")
    public ApiResponse<Void> tagUpdate(@RequestParam("id") Integer id, @RequestBody UserTagRequest request) {
        userAdminService.updateTag(id, request);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/user/group/delete")
    public ApiResponse<Void> groupDelete(@RequestParam("id") Integer id) {
        userAdminService.deleteGroup(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/user/tag/delete")
    public ApiResponse<Void> tagDelete(@RequestParam("id") Integer id) {
        userAdminService.deleteTag(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/user/group/info")
    public ApiResponse<UserGroupResponse> groupInfo(@RequestParam("id") Integer id) {
        return ApiResponse.ok(userAdminService.groupInfo(id));
    }

    @GetMapping("/api/admin/user/tag/info")
    public ApiResponse<UserTagResponse> tagInfo(@RequestParam("id") Integer id) {
        return ApiResponse.ok(userAdminService.tagInfo(id));
    }

    @GetMapping("/api/admin/system/user/level/list")
    public ApiResponse<List<SystemUserLevelResponse>> levelList() {
        return ApiResponse.ok(userAdminService.levels());
    }

    @PostMapping("/api/admin/system/user/level/save")
    public ApiResponse<Void> levelSave(@RequestBody SystemUserLevelRequest request) {
        userAdminService.saveLevel(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/system/user/level/update/{id}")
    public ApiResponse<Void> levelUpdate(@PathVariable("id") Integer id, @RequestBody SystemUserLevelRequest request) {
        userAdminService.updateLevel(id, request);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/system/user/level/info")
    public ApiResponse<SystemUserLevelResponse> levelInfo(@RequestParam("id") Integer id) {
        return ApiResponse.ok(userAdminService.levelInfo(id));
    }

    @PostMapping("/api/admin/system/user/level/delete/{id}")
    public ApiResponse<Void> levelDelete(@PathVariable("id") Integer id) {
        userAdminService.deleteLevel(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/system/user/level/use")
    public ApiResponse<Void> levelUse(@RequestBody SystemUserLevelRequest request) {
        userAdminService.updateLevelShow(request.getId(), request.getIsShow());
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/user/update/phone")
    public ApiResponse<Void> updatePhone(
            @RequestParam(name = "id") Integer id,
            @RequestParam(name = "phone") String phone) {
        userAdminService.updatePhone(id, phone);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/user/group")
    public ApiResponse<Void> group(
            @RequestParam(name = "id") String id,
            @RequestParam(name = "groupId") String groupId) {
        userAdminService.group(id, groupId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/user/tag")
    public ApiResponse<Void> tag(
            @RequestParam(name = "id") String id,
            @RequestParam(name = "tagId") String tagId) {
        userAdminService.tag(id, tagId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/user/operate/founds")
    public ApiResponse<Void> operateFunds(@ModelAttribute UserOperateIntegralMoneyRequest request) {
        userAdminService.operateFunds(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/user/update/level")
    public ApiResponse<Void> updateUserLevel(@RequestBody UpdateUserLevelRequest request) {
        userAdminService.updateUserLevel(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/user/update/spread")
    public ApiResponse<Void> updateSpread(@RequestBody UserUpdateSpreadRequest request) {
        userAdminService.updateSpread(request);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/store/retail/spread/clean/{id}")
    public ApiResponse<Void> clearSpread(@PathVariable("id") Integer id) {
        userAdminService.clearSpread(id);
        return ApiResponse.ok(null);
    }
}
