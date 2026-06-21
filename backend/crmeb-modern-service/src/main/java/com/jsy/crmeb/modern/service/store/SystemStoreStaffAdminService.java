package com.jsy.crmeb.modern.service.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.admin.dto.SystemAdminListResponse;
import com.jsy.crmeb.modern.service.admin.entity.SystemAdmin;
import com.jsy.crmeb.modern.service.admin.mapper.SystemAdminMapper;
import com.jsy.crmeb.modern.service.store.dto.SystemStoreStaffRequest;
import com.jsy.crmeb.modern.service.store.dto.SystemStoreStaffResponse;
import com.jsy.crmeb.modern.service.store.entity.SystemStore;
import com.jsy.crmeb.modern.service.store.entity.SystemStoreStaff;
import com.jsy.crmeb.modern.service.store.mapper.SystemStoreMapper;
import com.jsy.crmeb.modern.service.store.mapper.SystemStoreStaffMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SystemStoreStaffAdminService {
    private final SystemStoreStaffMapper staffMapper;
    private final SystemStoreMapper storeMapper;
    private final SystemAdminMapper adminMapper;

    public SystemStoreStaffAdminService(SystemStoreStaffMapper staffMapper, SystemStoreMapper storeMapper, SystemAdminMapper adminMapper) {
        this.staffMapper = staffMapper;
        this.storeMapper = storeMapper;
        this.adminMapper = adminMapper;
    }

    public PageResponse<SystemStoreStaffResponse> list(Integer storeId, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        LambdaQueryWrapper<SystemStoreStaff> query = new LambdaQueryWrapper<>();
        if (storeId != null && storeId > 0) {
            query.eq(SystemStoreStaff::getStoreId, storeId);
        }
        query.orderByDesc(SystemStoreStaff::getId);
        Page<SystemStoreStaff> staffPage = staffMapper.selectPage(new Page<>(safePage, safeLimit), query);
        List<SystemStoreStaff> records = staffPage.getRecords();
        Map<Integer, SystemAdmin> admins = loadAdmins(records);
        Map<Integer, SystemStore> stores = loadStores(records);
        List<SystemStoreStaffResponse> list = records.stream()
                .map(staff -> toResponse(staff, admins.get(staff.getUid()), stores.get(staff.getStoreId())))
                .toList();
        return new PageResponse<>(safePage, safeLimit, staffPage.getTotal(), list);
    }

    @Transactional
    public boolean save(SystemStoreStaffRequest request) {
        validate(request, null);
        Long exists = staffMapper.selectCount(new LambdaQueryWrapper<SystemStoreStaff>().eq(SystemStoreStaff::getUid, request.getUid()));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("该管理员已经是核销员");
        }
        SystemStoreStaff staff = fromRequest(request);
        staff.setVerifyStatus(request.getVerifyStatus() == null ? 0 : request.getVerifyStatus());
        staff.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        staff.setCreateTime(LocalDateTime.now());
        staff.setUpdateTime(LocalDateTime.now());
        return staffMapper.insert(staff) > 0;
    }

    @Transactional
    public boolean update(Integer id, SystemStoreStaffRequest request) {
        validate(request, id);
        requiredStaff(id);
        Long exists = staffMapper.selectCount(new LambdaQueryWrapper<SystemStoreStaff>()
                .eq(SystemStoreStaff::getUid, request.getUid())
                .ne(SystemStoreStaff::getId, id));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("该管理员已经是核销员");
        }
        SystemStoreStaff staff = fromRequest(request);
        staff.setId(id);
        staff.setUpdateTime(LocalDateTime.now());
        return staffMapper.updateById(staff) > 0;
    }

    public boolean delete(Integer id) {
        requiredStaff(id);
        return staffMapper.deleteById(id) > 0;
    }

    public boolean updateStatus(Integer id, Integer status) {
        SystemStoreStaff staff = requiredStaff(id);
        Integer nextStatus = status == null ? 0 : status;
        if (nextStatus.equals(staff.getStatus())) {
            return true;
        }
        SystemStoreStaff update = new SystemStoreStaff();
        update.setId(id);
        update.setStatus(nextStatus);
        update.setUpdateTime(LocalDateTime.now());
        return staffMapper.updateById(update) > 0;
    }

    public SystemStoreStaff info(Integer id) {
        return requiredStaff(id);
    }

    private void validate(SystemStoreStaffRequest request, Integer id) {
        if (request == null) {
            throw new IllegalArgumentException("参数错误");
        }
        if (request.getUid() == null || request.getUid() <= 0) {
            throw new IllegalArgumentException("请选择管理员");
        }
        SystemAdmin admin = adminMapper.selectById(request.getUid());
        if (admin == null || Integer.valueOf(1).equals(admin.getIsDel())) {
            throw new IllegalArgumentException("管理员不存在");
        }
        if (request.getStoreId() == null || request.getStoreId() <= 0) {
            throw new IllegalArgumentException("请选择提货点");
        }
        SystemStore store = storeMapper.selectById(request.getStoreId());
        if (store == null || Boolean.TRUE.equals(store.getIsDel())) {
            throw new IllegalArgumentException("提货点不存在");
        }
        if (!StringUtils.hasText(request.getStaffName())) {
            throw new IllegalArgumentException("核销员名称不能为空");
        }
        if (request.getStaffName().trim().length() > 64) {
            throw new IllegalArgumentException("核销员名称不能超过64个字符");
        }
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().trim().matches("^1[3456789]\\d{9}$")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("核销员不存在");
        }
    }

    private SystemStoreStaff fromRequest(SystemStoreStaffRequest request) {
        SystemStoreStaff staff = new SystemStoreStaff();
        staff.setUid(request.getUid());
        staff.setAvatar(valueOrEmpty(request.getAvatar()));
        staff.setStoreId(request.getStoreId());
        staff.setStaffName(request.getStaffName().trim());
        staff.setPhone(valueOrEmpty(request.getPhone()));
        staff.setVerifyStatus(request.getVerifyStatus());
        staff.setStatus(request.getStatus());
        return staff;
    }

    private SystemStoreStaff requiredStaff(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("核销员不存在");
        }
        SystemStoreStaff staff = staffMapper.selectById(id);
        if (staff == null) {
            throw new IllegalArgumentException("核销员不存在");
        }
        return staff;
    }

    private Map<Integer, SystemAdmin> loadAdmins(List<SystemStoreStaff> records) {
        List<Integer> ids = records.stream().map(SystemStoreStaff::getUid).filter(id -> id != null && id > 0).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return adminMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(SystemAdmin::getId, Function.identity()));
    }

    private Map<Integer, SystemStore> loadStores(List<SystemStoreStaff> records) {
        List<Integer> ids = records.stream().map(SystemStoreStaff::getStoreId).filter(id -> id != null && id > 0).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return storeMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(SystemStore::getId, Function.identity()));
    }

    private SystemStoreStaffResponse toResponse(SystemStoreStaff staff, SystemAdmin admin, SystemStore store) {
        SystemStoreStaffResponse response = new SystemStoreStaffResponse();
        response.setId(staff.getId());
        response.setUid(staff.getUid());
        response.setAvatar(StringUtils.hasText(staff.getAvatar()) ? staff.getAvatar() : admin == null ? "" : admin.getAccount());
        response.setUser(toAdminSummary(admin));
        response.setStoreId(staff.getStoreId());
        response.setSystemStore(store);
        response.setStaffName(staff.getStaffName());
        response.setPhone(staff.getPhone());
        response.setVerifyStatus(staff.getVerifyStatus());
        response.setStatus(staff.getStatus());
        response.setCreateTime(staff.getCreateTime());
        response.setUpdateTime(staff.getUpdateTime());
        return response;
    }

    private String valueOrEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private SystemAdminListResponse toAdminSummary(SystemAdmin admin) {
        if (admin == null) {
            return null;
        }
        SystemAdminListResponse response = new SystemAdminListResponse();
        response.setId(admin.getId());
        response.setAccount(admin.getAccount());
        response.setRealName(admin.getRealName());
        response.setRoles(admin.getRoles());
        response.setLastIp(admin.getLastIp());
        response.setLastTime(admin.getUpdateTime());
        response.setLoginCount(admin.getLoginCount());
        response.setLevel(admin.getLevel());
        response.setStatus(Integer.valueOf(1).equals(admin.getStatus()));
        response.setPhone(admin.getPhone());
        response.setIsSms(Integer.valueOf(1).equals(admin.getIsSms()));
        response.setIsDel(Integer.valueOf(1).equals(admin.getIsDel()));
        return response;
    }
}
