package com.jsy.crmeb.modern.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.common.util.LegacyPasswordUtil;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.admin.dto.SystemAdminListResponse;
import com.jsy.crmeb.modern.service.admin.dto.SystemAdminRequest;
import com.jsy.crmeb.modern.service.admin.dto.SystemAdminSearchRequest;
import com.jsy.crmeb.modern.service.admin.entity.SystemAdmin;
import com.jsy.crmeb.modern.service.admin.mapper.SystemAdminMapper;
import java.util.Arrays;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminSystemAdminService {
    private final SystemAdminMapper systemAdminMapper;

    public AdminSystemAdminService(SystemAdminMapper systemAdminMapper) {
        this.systemAdminMapper = systemAdminMapper;
    }

    public PageResponse<SystemAdminListResponse> list(SystemAdminSearchRequest request) {
        int page = request.getPage() <= 0 ? 1 : request.getPage();
        int limit = request.getLimit() <= 0 ? 20 : Math.min(request.getLimit(), 100);
        Page<SystemAdmin> adminPage = systemAdminMapper.selectPage(new Page<>(page, limit), buildQuery(request));
        Map<Integer, String> roleMap = systemAdminMapper.selectRoleRows().stream()
                .collect(Collectors.toMap(SystemAdminMapper.RoleRow::getId, SystemAdminMapper.RoleRow::getRoleName));
        return new PageResponse<>(
                page,
                limit,
                adminPage.getTotal(),
                adminPage.getRecords().stream().map(admin -> toResponse(admin, roleMap)).toList());
    }

    public SystemAdminListResponse info(Integer id) {
        SystemAdmin admin = requiredAdmin(id);
        Map<Integer, String> roleMap = systemAdminMapper.selectRoleRows().stream()
                .collect(Collectors.toMap(SystemAdminMapper.RoleRow::getId, SystemAdminMapper.RoleRow::getRoleName));
        return toResponse(admin, roleMap);
    }

    public boolean save(SystemAdminRequest request) {
        validateRequest(request, false);
        if (existsAccount(request.getAccount(), null)) {
            throw new IllegalArgumentException("管理员已存在");
        }
        SystemAdmin admin = new SystemAdmin();
        admin.setAccount(request.getAccount().trim());
        admin.setPwd(LegacyPasswordUtil.encryptPassword(request.getPwd(), admin.getAccount()));
        admin.setRealName(request.getRealName().trim());
        admin.setRoles(request.getRoles().trim());
        admin.setStatus(Boolean.TRUE.equals(request.getStatus()) ? 1 : 0);
        admin.setPhone(request.getPhone().trim());
        admin.setIsSms(0);
        admin.setIsDel(0);
        admin.setLevel(0);
        admin.setLoginCount(0);
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());
        return systemAdminMapper.insert(admin) > 0;
    }

    public boolean update(SystemAdminRequest request) {
        validateRequest(request, true);
        SystemAdmin exists = requiredAdmin(request.getId());
        if (!Objects.equals(exists.getAccount(), request.getAccount().trim()) && existsAccount(request.getAccount(), request.getId())) {
            throw new IllegalArgumentException("账号已存在");
        }
        SystemAdmin admin = new SystemAdmin();
        admin.setId(request.getId());
        admin.setAccount(request.getAccount().trim());
        if (StringUtils.hasText(request.getPwd())) {
            admin.setPwd(LegacyPasswordUtil.encryptPassword(request.getPwd(), admin.getAccount()));
        }
        admin.setRealName(request.getRealName().trim());
        admin.setRoles(request.getRoles().trim());
        admin.setStatus(Boolean.TRUE.equals(request.getStatus()) ? 1 : 0);
        admin.setPhone(request.getPhone().trim());
        if (request.getIsDel() != null) {
            admin.setIsDel(Boolean.TRUE.equals(request.getIsDel()) ? 1 : 0);
        }
        admin.setUpdateTime(LocalDateTime.now());
        return systemAdminMapper.updateById(admin) > 0;
    }

    public boolean delete(Integer id) {
        requiredAdmin(id);
        return systemAdminMapper.deleteById(id) > 0;
    }

    public boolean updateStatus(Integer id, Boolean status) {
        requiredAdmin(id);
        SystemAdmin admin = new SystemAdmin();
        admin.setId(id);
        admin.setStatus(Boolean.TRUE.equals(status) ? 1 : 0);
        admin.setUpdateTime(LocalDateTime.now());
        return systemAdminMapper.updateById(admin) > 0;
    }

    public boolean updateIsSms(Integer id) {
        SystemAdmin exists = requiredAdmin(id);
        if (!StringUtils.hasText(exists.getPhone())) {
            throw new IllegalArgumentException("请先为管理员添加手机号!");
        }
        SystemAdmin admin = new SystemAdmin();
        admin.setId(id);
        admin.setIsSms(Integer.valueOf(1).equals(exists.getIsSms()) ? 0 : 1);
        admin.setUpdateTime(LocalDateTime.now());
        return systemAdminMapper.updateById(admin) > 0;
    }

    private LambdaQueryWrapper<SystemAdmin> buildQuery(SystemAdminSearchRequest request) {
        LambdaQueryWrapper<SystemAdmin> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getRoles())) {
            query.eq(SystemAdmin::getRoles, request.getRoles().trim());
        }
        if (request.getStatus() != null) {
            query.eq(SystemAdmin::getStatus, Boolean.TRUE.equals(request.getStatus()) ? 1 : 0);
        }
        if (StringUtils.hasText(request.getRealName())) {
            String keyword = request.getRealName().trim();
            query.and(item -> item.like(SystemAdmin::getRealName, keyword).or().like(SystemAdmin::getAccount, keyword));
        }
        query.orderByDesc(SystemAdmin::getId);
        return query;
    }

    private void validateRequest(SystemAdminRequest request, boolean requireId) {
        if (request == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (requireId && (request.getId() == null || request.getId() <= 0)) {
            throw new IllegalArgumentException("管理员id不能为空");
        }
        if (!StringUtils.hasText(request.getAccount())) {
            throw new IllegalArgumentException("请填管理员账号");
        }
        if (request.getAccount().trim().length() > 32) {
            throw new IllegalArgumentException("账号长度不能超过32个字符");
        }
        if (!requireId && !StringUtils.hasText(request.getPwd())) {
            throw new IllegalArgumentException("请填管理员密码");
        }
        if (StringUtils.hasText(request.getPwd()) && (request.getPwd().length() < 6 || request.getPwd().length() > 32)) {
            throw new IllegalArgumentException("密码长度6-32个字符");
        }
        if (!StringUtils.hasText(request.getRealName())) {
            throw new IllegalArgumentException("请填写管理员姓名");
        }
        if (request.getRealName().trim().length() > 16) {
            throw new IllegalArgumentException("姓名长度不能超过16个字符");
        }
        if (!StringUtils.hasText(request.getRoles())) {
            throw new IllegalArgumentException("管理员身份不能为空");
        }
        if (request.getRoles().trim().length() > 128) {
            throw new IllegalArgumentException("角色组合长度不能超过128个字符");
        }
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("状态不能为空");
        }
        if (!StringUtils.hasText(request.getPhone())) {
            throw new IllegalArgumentException("请填写手机号");
        }
        if (!request.getPhone().trim().matches("^1[3-9]\\d{9}$")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
    }

    private boolean existsAccount(String account, Integer ignoreId) {
        LambdaQueryWrapper<SystemAdmin> query = new LambdaQueryWrapper<SystemAdmin>()
                .eq(SystemAdmin::getAccount, account.trim());
        if (ignoreId != null) {
            query.ne(SystemAdmin::getId, ignoreId);
        }
        query.last("limit 1");
        return systemAdminMapper.selectCount(query) > 0;
    }

    private SystemAdmin requiredAdmin(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("管理员id不能为空");
        }
        SystemAdmin admin = systemAdminMapper.selectById(id);
        if (admin == null || Integer.valueOf(1).equals(admin.getIsDel())) {
            throw new IllegalArgumentException("管理员不存在");
        }
        return admin;
    }

    private SystemAdminListResponse toResponse(SystemAdmin admin, Map<Integer, String> roleMap) {
        SystemAdminListResponse response = new SystemAdminListResponse();
        response.setId(admin.getId());
        response.setAccount(admin.getAccount());
        response.setRealName(admin.getRealName());
        response.setRoles(admin.getRoles());
        response.setRoleNames(roleNames(admin.getRoles(), roleMap));
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

    private String roleNames(String roles, Map<Integer, String> roleMap) {
        if (!StringUtils.hasText(roles)) {
            return "";
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(item -> {
                    try {
                        return roleMap.getOrDefault(Integer.parseInt(item), item);
                    } catch (NumberFormatException exception) {
                        return item;
                    }
                })
                .collect(Collectors.joining(","));
    }
}
