package com.jsy.crmeb.modern.service.wechat;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jsy.crmeb.modern.service.system.entity.SystemConfig;
import com.jsy.crmeb.modern.service.system.mapper.SystemConfigMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WechatMenuAdminService {
    private static final String CONFIG_KEY = "wechat_public_menu_json";

    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;

    public WechatMenuAdminService(SystemConfigMapper systemConfigMapper, ObjectMapper objectMapper) {
        this.systemConfigMapper = systemConfigMapper;
        this.objectMapper = objectMapper;
    }

    public ObjectNode getMenus() {
        SystemConfig config = menuConfig();
        if (config == null || !StringUtils.hasText(config.getValue())) {
            return wrapMenu(emptyMenu());
        }
        try {
            JsonNode root = objectMapper.readTree(config.getValue());
            ObjectNode menu = normalizeMenu(root);
            return wrapMenu(menu);
        } catch (Exception exception) {
            return wrapMenu(emptyMenu());
        }
    }

    @Transactional
    public boolean saveMenus(JsonNode body) {
        ObjectNode menu = normalizeMenu(body);
        String value;
        try {
            value = objectMapper.writeValueAsString(menu);
        } catch (Exception exception) {
            throw new IllegalArgumentException("菜单内容不是合法 JSON");
        }
        LocalDateTime now = LocalDateTime.now();
        SystemConfig config = menuConfig();
        if (config == null) {
            config = new SystemConfig();
            config.setName(CONFIG_KEY);
            config.setTitle("微信菜单");
            config.setFormId(0);
            config.setValue(value);
            config.setStatus(false);
            config.setCreateTime(now);
            config.setUpdateTime(now);
            return systemConfigMapper.insert(config) > 0;
        }
        config.setValue(value);
        config.setUpdateTime(now);
        return systemConfigMapper.updateById(config) > 0;
    }

    @Transactional
    public boolean deleteMenus() {
        return saveMenus(emptyMenu());
    }

    private ObjectNode normalizeMenu(JsonNode body) {
        JsonNode button = body == null ? null : body.get("button");
        if (button == null && body != null && body.has("menu")) {
            button = body.get("menu").get("button");
        }
        if (button == null || !button.isArray()) {
            throw new IllegalArgumentException("微信菜单必须包含 button 数组");
        }
        if (button.size() > 3) {
            throw new IllegalArgumentException("一级菜单最多 3 个");
        }
        ArrayNode normalized = objectMapper.createArrayNode();
        for (JsonNode item : button) {
            normalized.add(normalizeButton(item, false));
        }
        ObjectNode menu = objectMapper.createObjectNode();
        menu.set("button", normalized);
        return menu;
    }

    private ObjectNode normalizeButton(JsonNode item, boolean child) {
        if (item == null || !item.isObject()) {
            throw new IllegalArgumentException("菜单数据格式错误");
        }
        String name = text(item, "name");
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("菜单名称不能为空");
        }
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name", name.trim());

        JsonNode subButton = item.get("sub_button");
        if (!child && subButton != null && subButton.isArray() && subButton.size() > 0) {
            if (subButton.size() > 5) {
                throw new IllegalArgumentException("二级菜单最多 5 个");
            }
            ArrayNode children = objectMapper.createArrayNode();
            for (JsonNode sub : subButton) {
                children.add(normalizeButton(sub, true));
            }
            node.set("sub_button", children);
            return node;
        }

        String type = text(item, "type");
        if (!StringUtils.hasText(type)) {
            type = "click";
        }
        node.put("type", type);
        if ("click".equals(type)) {
            String key = text(item, "key");
            if (!StringUtils.hasText(key)) {
                throw new IllegalArgumentException("关键字不能为空");
            }
            node.put("key", key.trim());
        } else if ("view".equals(type)) {
            String url = text(item, "url");
            if (!StringUtils.hasText(url)) {
                throw new IllegalArgumentException("跳转地址不能为空");
            }
            node.put("url", url.trim());
        } else if ("miniprogram".equals(type)) {
            for (String field : new String[] {"appid", "url", "pagepath"}) {
                String value = text(item, field);
                if (!StringUtils.hasText(value)) {
                    throw new IllegalArgumentException("小程序菜单配置不完整");
                }
                node.put(field, value.trim());
            }
        } else {
            throw new IllegalArgumentException("未知菜单类型：" + type);
        }
        if (!child) {
            node.set("sub_button", objectMapper.createArrayNode());
        }
        return node;
    }

    private ObjectNode wrapMenu(ObjectNode menu) {
        ObjectNode root = objectMapper.createObjectNode();
        root.set("menu", menu);
        root.put("localMode", true);
        root.put("message", "微信菜单已保存到系统配置，公众号同步未配置。");
        return root;
    }

    private ObjectNode emptyMenu() {
        ObjectNode menu = objectMapper.createObjectNode();
        menu.set("button", objectMapper.createArrayNode());
        return menu;
    }

    private SystemConfig menuConfig() {
        QueryWrapper<SystemConfig> query = new QueryWrapper<>();
        query.eq("name", CONFIG_KEY).orderByDesc("id").last("limit 1");
        return systemConfigMapper.selectOne(query);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }
}
