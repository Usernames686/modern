package com.jsy.crmeb.modern.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jsy.crmeb.modern.service.admin.dto.SystemMenuRequest;
import com.jsy.crmeb.modern.service.admin.dto.SystemMenuSearchRequest;
import com.jsy.crmeb.modern.service.admin.entity.SystemMenu;
import com.jsy.crmeb.modern.service.admin.mapper.SystemMenuMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminSystemMenuService {
    private final SystemMenuMapper systemMenuMapper;

    public AdminSystemMenuService(SystemMenuMapper systemMenuMapper) {
        this.systemMenuMapper = systemMenuMapper;
    }

    public List<SystemMenu> list(SystemMenuSearchRequest request) {
        LambdaQueryWrapper<SystemMenu> query = new LambdaQueryWrapper<SystemMenu>()
                .eq(SystemMenu::getIsDelte, 0);
        if (request != null && StringUtils.hasText(request.getName())) {
            query.like(SystemMenu::getName, request.getName().trim());
        }
        if (request != null && StringUtils.hasText(request.getMenuType())) {
            query.eq(SystemMenu::getMenuType, request.getMenuType().trim());
        }
        query.orderByDesc(SystemMenu::getSort).orderByAsc(SystemMenu::getId);
        return systemMenuMapper.selectList(query);
    }

    public boolean add(SystemMenuRequest request) {
        validateRequest(request, false);
        SystemMenu menu = new SystemMenu();
        copyRequest(request, menu);
        menu.setId(null);
        menu.setIsDelte(0);
        menu.setCreateTime(LocalDateTime.now());
        menu.setUpdateTime(LocalDateTime.now());
        return systemMenuMapper.insert(menu) > 0;
    }

    @Transactional
    public boolean delete(Integer id) {
        SystemMenu menu = requiredMenu(id);
        List<SystemMenu> updates = new ArrayList<>();
        markDeleted(menu, updates);
        collectChildren(id, updates);
        for (SystemMenu update : updates) {
            systemMenuMapper.updateById(update);
        }
        return true;
    }

    public boolean update(SystemMenuRequest request) {
        validateRequest(request, true);
        requiredMenu(request.getId());
        SystemMenu menu = new SystemMenu();
        copyRequest(request, menu);
        menu.setId(request.getId());
        menu.setUpdateTime(LocalDateTime.now());
        return systemMenuMapper.updateById(menu) > 0;
    }

    public SystemMenu info(Integer id) {
        SystemMenu menu = requiredMenu(id);
        menu.setIsDelte(null);
        return menu;
    }

    public boolean updateShowStatus(Integer id) {
        SystemMenu menu = requiredMenu(id);
        SystemMenu update = new SystemMenu();
        update.setId(id);
        update.setIsShow(Integer.valueOf(1).equals(menu.getIsShow()) ? 0 : 1);
        update.setUpdateTime(LocalDateTime.now());
        return systemMenuMapper.updateById(update) > 0;
    }

    private void validateRequest(SystemMenuRequest request, boolean requireId) {
        if (request == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (requireId && (request.getId() == null || request.getId() <= 0)) {
            throw new IllegalArgumentException("系统菜单id不能为空");
        }
        if (request.getPid() == null) {
            request.setPid(0);
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("菜单名称不能为空");
        }
        if (request.getName().trim().length() > 100) {
            throw new IllegalArgumentException("菜单名称不能超过100个字符");
        }
        if (!List.of("M", "C", "A").contains(request.getMenuType())) {
            throw new IllegalArgumentException("未知的菜单类型");
        }
        if ("C".equals(request.getMenuType()) && !StringUtils.hasText(request.getComponent())) {
            throw new IllegalArgumentException("菜单类型的组件路径不能为空");
        }
        if ("A".equals(request.getMenuType()) && !StringUtils.hasText(request.getPerms())) {
            throw new IllegalArgumentException("按钮类型的权限标识不能为空");
        }
        if (request.getSort() == null || request.getSort() < 0) {
            throw new IllegalArgumentException("排序最小为0");
        }
        if (request.getIsShow() == null) {
            throw new IllegalArgumentException("显示状态不能为空");
        }
    }

    private void copyRequest(SystemMenuRequest request, SystemMenu menu) {
        menu.setPid(request.getPid());
        menu.setName(request.getName().trim());
        menu.setIcon(defaultString(request.getIcon()));
        menu.setPerms(defaultString(request.getPerms()));
        menu.setComponent(defaultString(request.getComponent()));
        menu.setMenuType(request.getMenuType());
        menu.setSort(request.getSort());
        menu.setIsShow(Boolean.TRUE.equals(request.getIsShow()) ? 1 : 0);
    }

    private SystemMenu requiredMenu(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("系统菜单id不能为空");
        }
        SystemMenu menu = systemMenuMapper.selectById(id);
        if (menu == null || Integer.valueOf(1).equals(menu.getIsDelte())) {
            throw new IllegalArgumentException("系统菜单不存在");
        }
        return menu;
    }

    private void collectChildren(Integer pid, List<SystemMenu> updates) {
        List<SystemMenu> children = systemMenuMapper.selectList(new LambdaQueryWrapper<SystemMenu>()
                .eq(SystemMenu::getPid, pid)
                .eq(SystemMenu::getIsDelte, 0));
        for (SystemMenu child : children) {
            markDeleted(child, updates);
            collectChildren(child.getId(), updates);
        }
    }

    private void markDeleted(SystemMenu source, List<SystemMenu> updates) {
        SystemMenu update = new SystemMenu();
        update.setId(source.getId());
        update.setIsDelte(1);
        update.setUpdateTime(LocalDateTime.now());
        updates.add(update);
    }

    private String defaultString(String value) {
        return value == null ? "" : value.trim();
    }
}
