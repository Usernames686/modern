package com.jsy.crmeb.modern.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.admin.dto.MenuCheckResponse;
import com.jsy.crmeb.modern.service.admin.dto.SystemRoleInfoResponse;
import com.jsy.crmeb.modern.service.admin.dto.SystemRoleRequest;
import com.jsy.crmeb.modern.service.admin.dto.SystemRoleSearchRequest;
import com.jsy.crmeb.modern.service.admin.entity.SystemMenu;
import com.jsy.crmeb.modern.service.admin.entity.SystemRole;
import com.jsy.crmeb.modern.service.admin.entity.SystemRoleMenu;
import com.jsy.crmeb.modern.service.admin.mapper.SystemMenuMapper;
import com.jsy.crmeb.modern.service.admin.mapper.SystemRoleMapper;
import com.jsy.crmeb.modern.service.admin.mapper.SystemRoleMenuMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminSystemRoleService {
    private final SystemRoleMapper systemRoleMapper;
    private final SystemRoleMenuMapper systemRoleMenuMapper;
    private final SystemMenuMapper systemMenuMapper;

    public AdminSystemRoleService(
            SystemRoleMapper systemRoleMapper,
            SystemRoleMenuMapper systemRoleMenuMapper,
            SystemMenuMapper systemMenuMapper) {
        this.systemRoleMapper = systemRoleMapper;
        this.systemRoleMenuMapper = systemRoleMenuMapper;
        this.systemMenuMapper = systemMenuMapper;
    }

    public PageResponse<SystemRole> list(SystemRoleSearchRequest request) {
        int page = request.getPage() <= 0 ? 1 : request.getPage();
        int limit = request.getLimit() <= 0 ? 20 : Math.min(request.getLimit(), 100);
        Page<SystemRole> rolePage = systemRoleMapper.selectPage(new Page<>(page, limit), buildQuery(request));
        return new PageResponse<>(page, limit, rolePage.getTotal(), rolePage.getRecords());
    }

    @Transactional
    public boolean save(SystemRoleRequest request) {
        validateRequest(request, false);
        if (existsRoleName(request.getRoleName(), null)) {
            throw new IllegalArgumentException("角色名称重复");
        }
        SystemRole role = new SystemRole();
        role.setRoleName(request.getRoleName().trim());
        role.setRules("");
        role.setStatus(Boolean.TRUE.equals(request.getStatus()));
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        if (systemRoleMapper.insert(role) <= 0) {
            return false;
        }
        saveRoleMenus(role.getId(), parseRuleIds(request.getRules()));
        return true;
    }

    @Transactional
    public boolean update(SystemRoleRequest request) {
        validateRequest(request, true);
        SystemRole exists = requiredRole(request.getId());
        if (!Objects.equals(exists.getRoleName(), request.getRoleName()) && existsRoleName(request.getRoleName(), request.getId())) {
            throw new IllegalArgumentException("角色名称重复");
        }
        SystemRole role = new SystemRole();
        role.setId(request.getId());
        role.setRoleName(request.getRoleName().trim());
        role.setRules("");
        role.setStatus(Boolean.TRUE.equals(request.getStatus()));
        role.setUpdateTime(LocalDateTime.now());
        systemRoleMapper.updateById(role);
        systemRoleMenuMapper.deleteByRid(request.getId());
        saveRoleMenus(request.getId(), parseRuleIds(request.getRules()));
        return true;
    }

    @Transactional
    public boolean delete(Integer id) {
        requiredRole(id);
        systemRoleMapper.deleteById(id);
        systemRoleMenuMapper.deleteByRid(id);
        return true;
    }

    public SystemRoleInfoResponse info(Integer id) {
        SystemRole role = requiredRole(id);
        Set<Integer> checked = new HashSet<>(systemRoleMenuMapper.selectMenuIdsByRid(id));
        SystemRoleInfoResponse response = new SystemRoleInfoResponse();
        response.setId(role.getId());
        response.setRoleName(role.getRoleName());
        response.setStatus(role.getStatus());
        response.setCreateTime(role.getCreateTime());
        response.setUpdateTime(role.getUpdateTime());
        response.setMenuList(menuTree(checked));
        return response;
    }

    public boolean updateStatus(Integer id, Boolean status) {
        SystemRole role = requiredRole(id);
        if (Objects.equals(role.getStatus(), status)) {
            return true;
        }
        SystemRole update = new SystemRole();
        update.setId(id);
        update.setStatus(Boolean.TRUE.equals(status));
        update.setUpdateTime(LocalDateTime.now());
        return systemRoleMapper.updateById(update) > 0;
    }

    public List<MenuCheckResponse> menuCacheTree() {
        return menuTree(Set.of());
    }

    private LambdaQueryWrapper<SystemRole> buildQuery(SystemRoleSearchRequest request) {
        LambdaQueryWrapper<SystemRole> query = new LambdaQueryWrapper<>();
        if (request.getStatus() != null) {
            query.eq(SystemRole::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getRoleName())) {
            query.like(SystemRole::getRoleName, request.getRoleName().trim());
        }
        query.orderByAsc(SystemRole::getId);
        return query;
    }

    private void validateRequest(SystemRoleRequest request, boolean requireId) {
        if (request == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (requireId && (request.getId() == null || request.getId() <= 0)) {
            throw new IllegalArgumentException("角色id不能为空");
        }
        if (!StringUtils.hasText(request.getRoleName())) {
            throw new IllegalArgumentException("请填写角色名称");
        }
        if (request.getRoleName().trim().length() > 32) {
            throw new IllegalArgumentException("角色名称不能超过32个字符");
        }
        if (!StringUtils.hasText(request.getRules())) {
            throw new IllegalArgumentException("权限不能为空");
        }
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("状态不能为空");
        }
    }

    private boolean existsRoleName(String roleName, Integer ignoreId) {
        LambdaQueryWrapper<SystemRole> query = new LambdaQueryWrapper<SystemRole>()
                .eq(SystemRole::getRoleName, roleName.trim());
        if (ignoreId != null) {
            query.ne(SystemRole::getId, ignoreId);
        }
        query.last("limit 1");
        return systemRoleMapper.selectCount(query) > 0;
    }

    private SystemRole requiredRole(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("角色id不能为空");
        }
        SystemRole role = systemRoleMapper.selectById(id);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        return role;
    }

    private List<Integer> parseRuleIds(String rules) {
        return Arrays.stream(rules.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(item -> {
                    try {
                        return Integer.parseInt(item);
                    } catch (NumberFormatException exception) {
                        throw new IllegalArgumentException("权限格式错误");
                    }
                })
                .distinct()
                .toList();
    }

    private void saveRoleMenus(Integer roleId, List<Integer> ruleIds) {
        for (Integer ruleId : ruleIds) {
            SystemRoleMenu roleMenu = new SystemRoleMenu();
            roleMenu.setRid(roleId);
            roleMenu.setMenuId(ruleId);
            systemRoleMenuMapper.insert(roleMenu);
        }
    }

    private List<MenuCheckResponse> menuTree(Set<Integer> checked) {
        List<SystemMenu> menus = systemMenuMapper.selectList(new LambdaQueryWrapper<SystemMenu>()
                .eq(SystemMenu::getIsDelte, 0)
                .orderByDesc(SystemMenu::getSort)
                .orderByAsc(SystemMenu::getId));
        List<MenuCheckResponse> responses = menus.stream()
                .map(menu -> toMenuCheck(menu, checked))
                .toList();
        Map<Integer, List<MenuCheckResponse>> childrenByPid = responses.stream()
                .collect(Collectors.groupingBy(MenuCheckResponse::getPid));
        List<MenuCheckResponse> roots = new ArrayList<>(childrenByPid.getOrDefault(0, List.of()));
        roots.forEach(root -> attachChildren(root, childrenByPid));
        return sortMenus(roots);
    }

    private MenuCheckResponse toMenuCheck(SystemMenu menu, Set<Integer> checked) {
        MenuCheckResponse response = new MenuCheckResponse();
        response.setId(menu.getId());
        response.setPid(menu.getPid());
        response.setName(menu.getName());
        response.setIcon(menu.getIcon());
        response.setSort(menu.getSort());
        response.setChecked(checked.contains(menu.getId()));
        response.setDisabled(false);
        return response;
    }

    private void attachChildren(MenuCheckResponse parent, Map<Integer, List<MenuCheckResponse>> childrenByPid) {
        List<MenuCheckResponse> children = new ArrayList<>(childrenByPid.getOrDefault(parent.getId(), List.of()));
        children.forEach(child -> attachChildren(child, childrenByPid));
        parent.setChildList(sortMenus(children));
    }

    private List<MenuCheckResponse> sortMenus(List<MenuCheckResponse> menus) {
        return menus.stream()
                .sorted(Comparator
                        .comparing((MenuCheckResponse menu) -> Objects.requireNonNullElse(menu.getSort(), 0)).reversed()
                        .thenComparing(MenuCheckResponse::getId))
                .toList();
    }
}
