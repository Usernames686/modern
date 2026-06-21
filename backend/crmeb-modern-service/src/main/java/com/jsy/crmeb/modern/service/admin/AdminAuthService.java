package com.jsy.crmeb.modern.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jsy.crmeb.modern.common.util.LegacyPasswordUtil;
import com.jsy.crmeb.modern.service.admin.dto.AdminInfoResponse;
import com.jsy.crmeb.modern.service.admin.dto.AdminLoginRequest;
import com.jsy.crmeb.modern.service.admin.dto.AdminLoginResponse;
import com.jsy.crmeb.modern.service.admin.entity.SystemAdmin;
import com.jsy.crmeb.modern.service.admin.mapper.SystemAdminMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminAuthService {
    public static final String TOKEN_HEADER = "Authori-zation";
    public static final String UNAUTHORIZED_MESSAGE = "未登录或token过期，请登录！";

    private static final String TOKEN_PREFIX = "crmeb-modern:admin:token:";
    private static final Duration TOKEN_TTL = Duration.ofHours(12);

    private final SystemAdminMapper systemAdminMapper;
    private final StringRedisTemplate redisTemplate;

    public AdminAuthService(SystemAdminMapper systemAdminMapper, StringRedisTemplate redisTemplate) {
        this.systemAdminMapper = systemAdminMapper;
        this.redisTemplate = redisTemplate;
    }

    public AdminLoginResponse login(AdminLoginRequest request, String ip) {
        if (request == null || !StringUtils.hasText(request.getAccount()) || !StringUtils.hasText(request.getPwd())) {
            throw new AdminAuthException(400, "账号和密码不能为空");
        }

        SystemAdmin admin = findActiveAdmin(request.getAccount());
        String encryptedPassword = LegacyPasswordUtil.encryptPassword(request.getPwd(), admin.getAccount());
        if (!encryptedPassword.equals(admin.getPwd())) {
            accountErrorNumAdd(request.getAccount());
            throw new AdminAuthException(400, "用户不存在或密码错误");
        }
        if (!Integer.valueOf(1).equals(admin.getStatus())) {
            accountErrorNumAdd(request.getAccount());
            throw new AdminAuthException(400, "账号已被禁用");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(tokenKey(token), String.valueOf(admin.getId()), TOKEN_TTL);

        systemAdminMapper.update(null, new LambdaUpdateWrapper<SystemAdmin>()
                .eq(SystemAdmin::getId, admin.getId())
                .set(SystemAdmin::getLastIp, trimIp(ip))
                .set(SystemAdmin::getUpdateTime, LocalDateTime.now())
                .set(SystemAdmin::getLoginCount, safeLoginCount(admin) + 1));
        redisTemplate.delete(accountErrorKey(admin.getAccount()));

        AdminLoginResponse response = new AdminLoginResponse();
        response.setId(admin.getId());
        response.setAccount(admin.getAccount());
        response.setRealName(admin.getRealName());
        response.setToken(token);
        response.setIsSms(Integer.valueOf(1).equals(admin.getIsSms()));
        return response;
    }

    public AdminInfoResponse getInfoByToken(String token) {
        SystemAdmin admin = requireAdminByToken(token);
        AdminInfoResponse response = new AdminInfoResponse();
        response.setId(admin.getId());
        response.setAccount(admin.getAccount());
        response.setRealName(admin.getRealName());
        response.setRoles(resolveRoles(admin.getRoles()));
        response.setRoleIds(admin.getRoles());
        response.setLastIp(admin.getLastIp());
        response.setLoginCount(admin.getLoginCount());
        response.setLevel(admin.getLevel());
        response.setStatus(Integer.valueOf(1).equals(admin.getStatus()));
        response.setPhone(admin.getPhone());
        response.setIsSms(Integer.valueOf(1).equals(admin.getIsSms()));
        response.setPermissionsList(resolvePermissions(admin.getRoles()));
        return response;
    }

    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            redisTemplate.delete(tokenKey(token));
        }
    }

    public Integer accountDetection(String account) {
        if (!StringUtils.hasText(account) || findAdminOrNull(account) == null) {
            return 0;
        }
        String value = redisTemplate.opsForValue().get(accountErrorKey(account));
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        return Integer.valueOf(value);
    }

    public boolean isTokenValid(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        Boolean exists = redisTemplate.hasKey(tokenKey(token));
        return Boolean.TRUE.equals(exists);
    }

    public Integer requireAdminIdByToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new AdminAuthException(401, UNAUTHORIZED_MESSAGE);
        }
        String adminId = redisTemplate.opsForValue().get(tokenKey(token));
        if (!StringUtils.hasText(adminId)) {
            throw new AdminAuthException(401, UNAUTHORIZED_MESSAGE);
        }
        redisTemplate.expire(tokenKey(token), TOKEN_TTL);
        return Integer.valueOf(adminId);
    }

    private SystemAdmin requireAdminByToken(String token) {
        Integer adminId = requireAdminIdByToken(token);
        SystemAdmin admin = systemAdminMapper.selectById(Integer.valueOf(adminId));
        if (admin == null || Integer.valueOf(1).equals(admin.getIsDel()) || !Integer.valueOf(1).equals(admin.getStatus())) {
            throw new AdminAuthException(401, UNAUTHORIZED_MESSAGE);
        }
        return admin;
    }

    private SystemAdmin findActiveAdmin(String account) {
        SystemAdmin admin = findAdminOrNull(account);
        if (admin == null) {
            throw new AdminAuthException(400, "用户不存在或密码错误");
        }
        return admin;
    }

    private SystemAdmin findAdminOrNull(String account) {
        SystemAdmin admin = systemAdminMapper.selectOne(new LambdaQueryWrapper<SystemAdmin>()
                .eq(SystemAdmin::getAccount, account)
                .eq(SystemAdmin::getIsDel, 0)
                .last("limit 1"));
        return admin;
    }

    private List<String> resolveRoles(String roleIds) {
        if (roleIds != null && List.of(roleIds.split(",")).contains("1")) {
            return List.of("admin");
        }
        return List.of("editor");
    }

    private List<String> resolvePermissions(String roleIds) {
        if (roleIds != null && List.of(roleIds.split(",")).contains("1")) {
            return List.of("*:*:*");
        }
        return List.of();
    }

    private int safeLoginCount(SystemAdmin admin) {
        return admin.getLoginCount() == null ? 0 : admin.getLoginCount();
    }

    private String tokenKey(String token) {
        return TOKEN_PREFIX + token;
    }

    private String accountErrorKey(String account) {
        return "admin:account:login:error:" + account;
    }

    private void accountErrorNumAdd(String account) {
        redisTemplate.opsForValue().increment(accountErrorKey(account));
    }

    private String trimIp(String ip) {
        if (ip == null || ip.length() <= 16) {
            return ip;
        }
        return ip.substring(0, 16);
    }
}
