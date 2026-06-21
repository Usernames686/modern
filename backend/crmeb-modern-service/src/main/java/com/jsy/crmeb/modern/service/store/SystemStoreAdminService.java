package com.jsy.crmeb.modern.service.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.store.dto.SystemStoreRequest;
import com.jsy.crmeb.modern.service.store.dto.SystemStoreSearchRequest;
import com.jsy.crmeb.modern.service.store.entity.SystemStore;
import com.jsy.crmeb.modern.service.store.mapper.SystemStoreMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SystemStoreAdminService {
    private final SystemStoreMapper systemStoreMapper;

    public SystemStoreAdminService(SystemStoreMapper systemStoreMapper) {
        this.systemStoreMapper = systemStoreMapper;
    }

    public PageResponse<SystemStore> list(SystemStoreSearchRequest request) {
        int page = request.getPage() <= 0 ? 1 : request.getPage();
        int limit = request.getLimit() <= 0 ? 20 : Math.min(request.getLimit(), 100);
        Page<SystemStore> storePage = systemStoreMapper.selectPage(new Page<>(page, limit), buildListQuery(request.getStatus(), request.getKeywords()));
        return new PageResponse<>(page, limit, storePage.getTotal(), storePage.getRecords());
    }

    public Map<String, Integer> getCount(String keywords) {
        Map<String, Integer> count = new HashMap<>();
        count.put("show", Math.toIntExact(systemStoreMapper.selectCount(buildListQuery(1, keywords))));
        count.put("hide", Math.toIntExact(systemStoreMapper.selectCount(buildListQuery(0, keywords))));
        count.put("recycle", Math.toIntExact(systemStoreMapper.selectCount(buildListQuery(2, keywords))));
        return count;
    }

    @Transactional
    public boolean save(SystemStoreRequest request) {
        validate(request);
        SystemStore store = fromRequest(request);
        store.setIsShow(true);
        store.setIsDel(false);
        store.setCreateTime(LocalDateTime.now());
        store.setUpdateTime(LocalDateTime.now());
        return systemStoreMapper.insert(store) > 0;
    }

    @Transactional
    public boolean update(Integer id, SystemStoreRequest request) {
        validateId(id);
        validate(request);
        if (systemStoreMapper.selectById(id) == null) {
            throw new IllegalArgumentException("门店自提点不存在");
        }
        SystemStore store = fromRequest(request);
        store.setId(id);
        store.setUpdateTime(LocalDateTime.now());
        return systemStoreMapper.updateById(store) > 0;
    }

    public boolean updateStatus(Integer id, Boolean status) {
        SystemStore store = requiredStore(id);
        Boolean nextStatus = Boolean.TRUE.equals(status);
        if (nextStatus.equals(store.getIsShow())) {
            return true;
        }
        SystemStore update = new SystemStore();
        update.setId(id);
        update.setIsShow(nextStatus);
        update.setUpdateTime(LocalDateTime.now());
        return systemStoreMapper.updateById(update) > 0;
    }

    public boolean delete(Integer id) {
        requiredStore(id);
        SystemStore update = new SystemStore();
        update.setId(id);
        update.setIsDel(true);
        update.setUpdateTime(LocalDateTime.now());
        return systemStoreMapper.updateById(update) > 0;
    }

    public boolean completeDelete(Integer id) {
        requiredStore(id);
        return systemStoreMapper.deleteById(id) > 0;
    }

    public boolean recovery(Integer id) {
        SystemStore store = requiredStore(id);
        if (!Boolean.TRUE.equals(store.getIsDel())) {
            return true;
        }
        SystemStore update = new SystemStore();
        update.setId(id);
        update.setIsDel(false);
        update.setUpdateTime(LocalDateTime.now());
        return systemStoreMapper.updateById(update) > 0;
    }

    public SystemStore info(Integer id) {
        SystemStore store = requiredStore(id);
        if (StringUtils.hasText(store.getLatitude()) && StringUtils.hasText(store.getLongitude())) {
            store.setLatitude(store.getLatitude() + "," + store.getLongitude());
        }
        return store;
    }

    private LambdaQueryWrapper<SystemStore> buildListQuery(Integer status, String keywords) {
        LambdaQueryWrapper<SystemStore> query = new LambdaQueryWrapper<>();
        if (Integer.valueOf(2).equals(status)) {
            query.eq(SystemStore::getIsDel, true);
        } else if (Integer.valueOf(0).equals(status)) {
            query.eq(SystemStore::getIsShow, false).eq(SystemStore::getIsDel, false);
        } else {
            query.eq(SystemStore::getIsShow, true).eq(SystemStore::getIsDel, false);
        }
        if (StringUtils.hasText(keywords)) {
            String trimmed = keywords.trim();
            query.and(item -> item.like(SystemStore::getName, trimmed).or().like(SystemStore::getPhone, trimmed));
        }
        query.orderByDesc(SystemStore::getUpdateTime).orderByDesc(SystemStore::getId);
        return query;
    }

    private SystemStore fromRequest(SystemStoreRequest request) {
        SystemStore store = new SystemStore();
        store.setName(trim(request.getName()));
        store.setIntroduction(valueOrEmpty(request.getIntroduction()));
        store.setPhone(trim(request.getPhone()));
        store.setAddress(trim(request.getAddress()));
        store.setDetailedAddress(trim(request.getDetailedAddress()));
        store.setDayTime(valueOrEmpty(request.getDayTime()));
        store.setImage(clearImagePrefix(request.getImage()));
        store.setValidTime(valueOrEmpty(request.getValidTime()));
        applyLngLat(store, request);
        return store;
    }

    private void applyLngLat(SystemStore store, SystemStoreRequest request) {
        String latitude = trim(request.getLatitude());
        String longitude = trim(request.getLongitude());
        if (StringUtils.hasText(latitude) && latitude.contains(",")) {
            String[] parts = latitude.split(",");
            if (parts.length >= 2) {
                longitude = parts[0].trim();
                latitude = parts[1].trim();
            }
        }
        store.setLatitude(valueOrEmpty(latitude));
        store.setLongitude(valueOrEmpty(longitude));
    }

    private SystemStore requiredStore(Integer id) {
        validateId(id);
        SystemStore store = systemStoreMapper.selectById(id);
        if (store == null) {
            throw new IllegalArgumentException("门店自提点不存在");
        }
        return store;
    }

    private void validate(SystemStoreRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("参数错误");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("请填写门店名称");
        }
        if (!StringUtils.hasText(request.getPhone())) {
            throw new IllegalArgumentException("请填写手机号");
        }
        if (!StringUtils.hasText(request.getAddress())) {
            throw new IllegalArgumentException("提货点地址");
        }
        if (!StringUtils.hasText(request.getDetailedAddress())) {
            throw new IllegalArgumentException("请填写详细地址");
        }
        if (!StringUtils.hasText(request.getImage())) {
            throw new IllegalArgumentException("请上传门店logo");
        }
        if (!StringUtils.hasText(request.getLatitude())) {
            throw new IllegalArgumentException("请选择经纬度");
        }
    }

    private void validateId(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("提货点不存在!");
        }
    }

    private String clearImagePrefix(String image) {
        String value = trim(image);
        if (!StringUtils.hasText(value)) {
            return "";
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            int index = value.indexOf("/crmebimage/");
            return index >= 0 ? value.substring(index + 1) : value;
        }
        return value.startsWith("/") ? value.substring(1) : value;
    }

    private String valueOrEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
