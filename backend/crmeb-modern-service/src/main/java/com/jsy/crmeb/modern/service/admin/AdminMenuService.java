package com.jsy.crmeb.modern.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jsy.crmeb.modern.service.admin.dto.MenuResponse;
import com.jsy.crmeb.modern.service.admin.entity.SystemAdmin;
import com.jsy.crmeb.modern.service.admin.entity.SystemMenu;
import com.jsy.crmeb.modern.service.admin.mapper.SystemAdminMapper;
import com.jsy.crmeb.modern.service.admin.mapper.SystemMenuMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AdminMenuService {
    private final AdminAuthService adminAuthService;
    private final SystemAdminMapper systemAdminMapper;
    private final SystemMenuMapper systemMenuMapper;

    public AdminMenuService(AdminAuthService adminAuthService, SystemAdminMapper systemAdminMapper, SystemMenuMapper systemMenuMapper) {
        this.adminAuthService = adminAuthService;
        this.systemAdminMapper = systemAdminMapper;
        this.systemMenuMapper = systemMenuMapper;
    }

    public List<MenuResponse> getMenus(String token) {
        Integer adminId = adminAuthService.requireAdminIdByToken(token);
        SystemAdmin admin = systemAdminMapper.selectById(adminId);
        List<SystemMenu> menus = isSuperAdmin(admin)
                ? systemMenuMapper.selectList(new LambdaQueryWrapper<SystemMenu>()
                        .eq(SystemMenu::getIsDelte, 0)
                        .eq(SystemMenu::getIsShow, 1)
                        .ne(SystemMenu::getMenuType, "A"))
                : systemMenuMapper.selectMenusByUserId(adminId);
        return buildTree(menus.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    private boolean isSuperAdmin(SystemAdmin admin) {
        return admin != null && admin.getRoles() != null && List.of(admin.getRoles().split(",")).contains("1");
    }

    private MenuResponse toResponse(SystemMenu menu) {
        MenuResponse response = new MenuResponse();
        response.setId(menu.getId());
        response.setPid(menu.getPid());
        response.setName(menu.getName());
        response.setIcon(menu.getIcon());
        response.setPerms(menu.getPerms());
        response.setComponent(menu.getComponent());
        response.setMenuType(menu.getMenuType());
        response.setSort(menu.getSort());
        return response;
    }

    private List<MenuResponse> buildTree(List<MenuResponse> menus) {
        Map<Integer, List<MenuResponse>> childrenByPid = menus.stream()
                .collect(Collectors.groupingBy(MenuResponse::getPid));
        List<MenuResponse> roots = new ArrayList<>(childrenByPid.getOrDefault(0, List.of()));
        roots.forEach(root -> attachChildren(root, childrenByPid));
        return sortMenus(roots);
    }

    private void attachChildren(MenuResponse parent, Map<Integer, List<MenuResponse>> childrenByPid) {
        List<MenuResponse> children = new ArrayList<>(childrenByPid.getOrDefault(parent.getId(), List.of()));
        children.forEach(child -> attachChildren(child, childrenByPid));
        parent.setChildList(sortMenus(children));
    }

    private List<MenuResponse> sortMenus(List<MenuResponse> menus) {
        return menus.stream()
                .sorted(Comparator
                        .comparing((MenuResponse menu) -> Objects.requireNonNullElse(menu.getSort(), 0)).reversed()
                        .thenComparing(MenuResponse::getId))
                .collect(Collectors.toList());
    }
}
