package com.jsy.crmeb.modern.service.marketing;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.common.config.CrmebRuntimeProperties;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.marketing.dto.SeckillManagerRequest;
import com.jsy.crmeb.modern.service.marketing.dto.SeckillProductRequest;
import com.jsy.crmeb.modern.service.marketing.entity.StoreSeckill;
import com.jsy.crmeb.modern.service.marketing.entity.StoreSeckillManger;
import com.jsy.crmeb.modern.service.marketing.mapper.StoreSeckillMangerMapper;
import com.jsy.crmeb.modern.service.marketing.mapper.StoreSeckillMapper;
import com.jsy.crmeb.modern.service.product.ProductAdminService;
import com.jsy.crmeb.modern.service.product.dto.ProductAttrResponse;
import com.jsy.crmeb.modern.service.product.dto.ProductAttrValueResponse;
import com.jsy.crmeb.modern.service.product.dto.StoreProductInfoResponse;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductAttrMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductAttrValueMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductDescriptionMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SeckillAdminService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int PRODUCT_TYPE_SECKILL = 1;

    private final StoreSeckillMapper seckillMapper;
    private final StoreSeckillMangerMapper mangerMapper;
    private final StoreProductAttrMapper attrMapper;
    private final StoreProductAttrValueMapper attrValueMapper;
    private final StoreProductDescriptionMapper descriptionMapper;
    private final ProductAdminService productAdminService;
    private final CrmebRuntimeProperties runtimeProperties;

    public SeckillAdminService(
            StoreSeckillMapper seckillMapper,
            StoreSeckillMangerMapper mangerMapper,
            StoreProductAttrMapper attrMapper,
            StoreProductAttrValueMapper attrValueMapper,
            StoreProductDescriptionMapper descriptionMapper,
            ProductAdminService productAdminService,
            CrmebRuntimeProperties runtimeProperties) {
        this.seckillMapper = seckillMapper;
        this.mangerMapper = mangerMapper;
        this.attrMapper = attrMapper;
        this.attrValueMapper = attrValueMapper;
        this.descriptionMapper = descriptionMapper;
        this.productAdminService = productAdminService;
        this.runtimeProperties = runtimeProperties;
    }

    public PageResponse<Map<String, Object>> list(Integer page, Integer limit, Integer timeId, Integer status, String keywords) {
        int safePage = page == null || page <= 0 ? 1 : page;
        int safeLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 200);
        Page<StoreSeckill> seckillPage = seckillMapper.selectPage(new Page<>(safePage, safeLimit), buildQuery(timeId, status, keywords));
        List<StoreSeckillManger> managers = allManagers();
        Map<Integer, StoreSeckillManger> managerMap = managers.stream()
                .collect(Collectors.toMap(StoreSeckillManger::getId, Function.identity(), (left, right) -> left));
        Integer currentTimeId = currentTimeId();
        List<Map<String, Object>> records = seckillPage.getRecords().stream()
                .map(item -> toSeckillRow(item, managerMap.get(item.getTimeId()), currentTimeId))
                .toList();
        return new PageResponse<>(safePage, safeLimit, seckillPage.getTotal(), records);
    }

    public Map<String, Object> info(Integer id) {
        StoreSeckill seckill = requiredSeckill(id);
        StoreSeckillManger manager = seckill.getTimeId() == null ? null : mangerMapper.selectById(seckill.getTimeId());
        return toSeckillRow(seckill, manager, currentTimeId());
    }

    public Map<String, Object> productDraft(Integer productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("商品id不能为空");
        }
        StoreProductInfoResponse product = productAdminService.info(productId);
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("productId", product.getId());
        draft.put("id", product.getId());
        draft.put("title", text(product.getStoreName()));
        draft.put("storeName", text(product.getStoreName()));
        draft.put("image", text(product.getImage()));
        draft.put("images", text(product.getSliderImage()));
        draft.put("sliderImage", text(product.getSliderImage()));
        draft.put("unitName", text(product.getUnitName(), "件"));
        draft.put("tempId", product.getTempId() == null ? 0 : product.getTempId());
        draft.put("info", text(product.getStoreInfo()));
        draft.put("content", text(product.getContent()));
        draft.put("price", nvl(product.getPrice()));
        draft.put("otPrice", nvl(product.getOtPrice()));
        draft.put("cost", nvl(product.getCost()));
        draft.put("stock", product.getStock() == null ? 0 : product.getStock());
        draft.put("quota", product.getStock() == null ? 0 : product.getStock());
        draft.put("quotaShow", product.getStock() == null ? 0 : product.getStock());
        draft.put("sort", product.getSort() == null ? 0 : product.getSort());
        draft.put("specType", Boolean.TRUE.equals(product.getSpecType()));
        draft.put("attr", product.getAttr() == null ? List.of() : product.getAttr().stream()
                .map(this::attrRow)
                .toList());
        draft.put("attrValue", product.getAttrValue() == null ? List.of() : product.getAttrValue().stream()
                .map(value -> {
                    Map<String, Object> row = attrValueRow(value);
                    row.put("id", null);
                    row.put("productId", product.getId());
                    int stock = integer(row.get("stock"), 0);
                    row.put("quota", stock);
                    row.put("quotaShow", stock);
                    return row;
                })
                .toList());
        return draft;
    }

    @Transactional
    public boolean save(SeckillProductRequest request) {
        StoreSeckill seckill = new StoreSeckill();
        applyProductRequest(seckill, request, false);
        seckill.setCreateTime(LocalDateTime.now());
        seckill.setSales(0);
        seckill.setIsDel(false);
        seckill.setIsShow(true);
        if (seckillMapper.insert(seckill) <= 0) {
            return false;
        }
        replaceAttrs(seckill.getId(), request);
        replaceAttrValues(seckill.getId(), seckill, request);
        saveDescription(seckill.getId(), request == null ? "" : request.getContent());
        return true;
    }

    @Transactional
    public boolean update(Integer id, SeckillProductRequest request) {
        StoreSeckill old = requiredSeckill(id);
        if (Integer.valueOf(1).equals(old.getStatus())) {
            throw new IllegalArgumentException("请先关闭秒杀商品，再修改商品信息");
        }
        StoreSeckill seckill = new StoreSeckill();
        seckill.setId(id);
        applyProductRequest(seckill, request, true);
        if (seckillMapper.updateById(seckill) <= 0) {
            return false;
        }
        replaceAttrs(id, request);
        replaceAttrValues(id, seckill, request);
        saveDescription(id, request == null ? "" : request.getContent());
        return true;
    }

    public PageResponse<Map<String, Object>> managerList(Integer page, Integer limit, String name, String status) {
        int safePage = page == null || page <= 0 ? 1 : page;
        int safeLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 200);
        QueryWrapper<StoreSeckillManger> query = new QueryWrapper<>();
        query.eq("is_del", 0);
        if (StringUtils.hasText(name)) {
            query.like("name", name.trim());
        }
        if (StringUtils.hasText(status)) {
            query.eq("status", normalizeManagerStatus(status));
        }
        query.orderByAsc("sort").orderByAsc("id");
        Page<StoreSeckillManger> result = mangerMapper.selectPage(new Page<>(safePage, safeLimit), query);
        List<Map<String, Object>> list = result.getRecords().stream().map(this::toManagerRow).toList();
        return new PageResponse<>(safePage, safeLimit, result.getTotal(), list);
    }

    public Map<String, Object> managerInfo(Integer id) {
        return toManagerRow(requiredManager(id));
    }

    @Transactional
    public boolean managerSave(SeckillManagerRequest request) {
        int[] range = parseManagerTime(request == null ? null : request.getTime());
        ensureManagerTimeUnique(null, range[0], range[1]);
        StoreSeckillManger manager = new StoreSeckillManger();
        applyManagerRequest(manager, request, range);
        manager.setCreateTime(LocalDateTime.now());
        manager.setUpdateTime(LocalDateTime.now());
        manager.setIsDel(false);
        return mangerMapper.insert(manager) > 0;
    }

    @Transactional
    public boolean managerUpdate(Integer id, SeckillManagerRequest request) {
        requiredManager(id);
        int[] range = parseManagerTime(request == null ? null : request.getTime());
        ensureManagerTimeUnique(id, range[0], range[1]);
        StoreSeckillManger manager = new StoreSeckillManger();
        manager.setId(id);
        applyManagerRequest(manager, request, range);
        manager.setUpdateTime(LocalDateTime.now());
        return mangerMapper.updateById(manager) > 0;
    }

    @Transactional
    public boolean managerDelete(Integer id) {
        requiredManager(id);
        StoreSeckillManger manager = new StoreSeckillManger();
        manager.setId(id);
        manager.setIsDel(true);
        manager.setUpdateTime(LocalDateTime.now());
        return mangerMapper.updateById(manager) > 0;
    }

    @Transactional
    public boolean managerUpdateStatus(Integer id, String status) {
        requiredManager(id);
        StoreSeckillManger manager = new StoreSeckillManger();
        manager.setId(id);
        manager.setStatus(normalizeManagerStatus(status));
        manager.setUpdateTime(LocalDateTime.now());
        return mangerMapper.updateById(manager) > 0;
    }

    @Transactional
    public boolean updateStatus(Integer id, Integer status) {
        requiredSeckill(id);
        StoreSeckill update = new StoreSeckill();
        update.setId(id);
        update.setStatus(status == null || status <= 0 ? 0 : 1);
        return seckillMapper.updateById(update) > 0;
    }

    @Transactional
    public boolean delete(Integer id) {
        requiredSeckill(id);
        StoreSeckill update = new StoreSeckill();
        update.setId(id);
        update.setIsDel(true);
        return seckillMapper.updateById(update) > 0;
    }

    public Map<String, String> export(Integer timeId, Integer status, String keywords) {
        List<StoreSeckill> records = seckillMapper.selectList(buildQuery(timeId, status, keywords));
        Map<Integer, StoreSeckillManger> managerMap = allManagers().stream()
                .collect(Collectors.toMap(StoreSeckillManger::getId, Function.identity(), (left, right) -> left));
        Integer current = currentTimeId();
        List<String> lines = new ArrayList<>();
        lines.add(csv("ID", "配置", "秒杀时段", "商品标题", "原价", "秒杀价", "限量", "限量剩余", "秒杀状态", "创建时间", "状态"));
        for (StoreSeckill item : records) {
            Map<String, Object> row = toSeckillRow(item, managerMap.get(item.getTimeId()), current);
            Map<?, ?> manager = row.get("storeSeckillManagerResponse") instanceof Map<?, ?> map ? map : Map.of();
            lines.add(csv(
                    row.get("id"),
                    managerValue(manager, "name"),
                    managerValue(manager, "time"),
                    row.get("title"),
                    row.get("otPrice"),
                    row.get("price"),
                    row.get("quotaShow"),
                    row.get("quota"),
                    row.get("statusName"),
                    row.get("createTime"),
                    Integer.valueOf(1).equals(row.get("status")) ? "开启" : "关闭"));
        }
        String fileName = "秒杀商品导出_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + ".csv";
        Path root = Path.of(runtimeProperties.getImagePath() == null || runtimeProperties.getImagePath().isBlank()
                ? "./upload/"
                : runtimeProperties.getImagePath()).toAbsolutePath().normalize();
        Path exportDir = root.resolve("crmebimage/export").normalize();
        try {
            Files.createDirectories(exportDir);
            Files.writeString(exportDir.resolve(fileName), "\uFEFF" + String.join("\n", lines), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("导出秒杀商品失败：" + exception.getMessage());
        }
        return Map.of("fileName", "/crmebimage/export/" + fileName);
    }

    private QueryWrapper<StoreSeckill> buildQuery(Integer timeId, Integer status, String keywords) {
        QueryWrapper<StoreSeckill> query = new QueryWrapper<>();
        query.eq("is_del", 0);
        if (timeId != null && timeId > 0) {
            query.eq("time_id", timeId);
        }
        if (status != null) {
            query.eq("status", status);
        }
        if (StringUtils.hasText(keywords)) {
            String value = keywords.trim();
            query.and(wrapper -> {
                wrapper.like("title", value);
                if (isInteger(value)) {
                    wrapper.or().eq("id", Integer.parseInt(value));
                }
            });
        }
        query.orderByDesc("sort").orderByDesc("id");
        return query;
    }

    private Map<String, Object> toSeckillRow(StoreSeckill item, StoreSeckillManger manager, Integer currentTimeId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", item.getId());
        row.put("productId", item.getProductId());
        row.put("image", item.getImage());
        row.put("images", item.getImages());
        row.put("sliderImage", item.getImages());
        row.put("title", item.getTitle());
        row.put("storeName", item.getTitle());
        row.put("info", item.getInfo());
        row.put("price", nvl(item.getPrice()));
        row.put("cost", nvl(item.getCost()));
        row.put("otPrice", nvl(item.getOtPrice()));
        row.put("giveIntegral", nvl(item.getGiveIntegral()));
        row.put("sort", item.getSort());
        row.put("stock", item.getStock());
        row.put("sales", item.getSales());
        row.put("unitName", item.getUnitName());
        row.put("postage", nvl(item.getPostage()));
        row.put("description", item.getDescription());
        row.put("content", normalizeDescription(item));
        row.put("startTime", dateText(item.getStartTime()));
        row.put("stopTime", dateText(item.getStopTime()));
        row.put("startTimeStr", dateOnly(item.getStartTime()));
        row.put("stopTimeStr", dateOnly(item.getStopTime()));
        row.put("createTime", dateText(item.getCreateTime()));
        row.put("status", item.getStatus());
        row.put("statusName", statusName(item, currentTimeId));
        row.put("killStatus", killStatus(item, currentTimeId));
        row.put("isPostage", item.getIsPostage());
        row.put("isDel", item.getIsDel());
        row.put("num", item.getNum());
        row.put("isShow", item.getIsShow());
        row.put("timeId", item.getTimeId());
        row.put("tempId", item.getTempId());
        row.put("weight", nvl(item.getWeight()));
        row.put("volume", nvl(item.getVolume()));
        row.put("quota", item.getQuota());
        row.put("quotaShow", item.getQuotaShow());
        List<Map<String, Object>> attrs = attrMapper.selectByProductIdAndType(item.getId(), PRODUCT_TYPE_SECKILL).stream()
                .map(this::attrRow)
                .toList();
        List<ProductAttrValueResponse> activityValues = attrValueMapper.selectByProductIdAndType(item.getId(), PRODUCT_TYPE_SECKILL);
        List<ProductAttrValueResponse> masterValues = item.getProductId() == null || item.getProductId() <= 0
                ? List.of()
                : attrValueMapper.selectByProductIdAndType(item.getProductId(), 0);
        List<Map<String, Object>> attrValues = mergeAttrValues(masterValues, activityValues);
        row.put("specType", Boolean.TRUE.equals(item.getSpecType()) || isMultipleSpec(attrs, attrValues));
        row.put("attr", attrs);
        row.put("attrValue", attrValues);
        row.put("storeSeckillManagerResponse", manager == null ? null : toManagerRow(manager));
        row.put("currentTimeId", currentTimeId);
        row.put("currentTime", manager == null ? "" : managerTime(manager));
        return row;
    }

    private Map<String, Object> attrRow(ProductAttrResponse source) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", source.getId());
        row.put("productId", source.getProductId());
        row.put("attrName", text(source.getAttrName()));
        row.put("attrValues", text(source.getAttrValues()));
        row.put("type", source.getType());
        return row;
    }

    private Map<String, Object> attrValueRow(ProductAttrValueResponse source) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", source.getId());
        row.put("productId", source.getProductId());
        row.put("suk", source.getSuk());
        row.put("stock", source.getStock());
        row.put("quota", source.getQuota());
        row.put("quotaShow", source.getQuotaShow());
        row.put("sales", source.getSales());
        row.put("price", source.getPrice());
        row.put("image", source.getImage());
        row.put("cost", source.getCost());
        row.put("otPrice", source.getOtPrice());
        row.put("weight", source.getWeight());
        row.put("volume", source.getVolume());
        row.put("attrValue", text(source.getAttrValue(), "{}"));
        row.put("barCode", text(source.getBarCode()));
        return row;
    }

    private List<Map<String, Object>> mergeAttrValues(
            List<ProductAttrValueResponse> masterValues,
            List<ProductAttrValueResponse> activityValues) {
        if (masterValues == null || masterValues.isEmpty()) {
            return activityValues == null ? List.of() : activityValues.stream()
                    .map(this::attrValueRow)
                    .toList();
        }
        Map<String, ProductAttrValueResponse> activityBySuk = (activityValues == null ? List.<ProductAttrValueResponse>of() : activityValues).stream()
                .collect(Collectors.toMap(
                        value -> string(value.getSuk(), "默认"),
                        value -> value,
                        (left, right) -> left,
                        LinkedHashMap::new));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ProductAttrValueResponse masterValue : masterValues) {
            ProductAttrValueResponse activityValue = activityBySuk.remove(string(masterValue.getSuk(), "默认"));
            if (activityValue == null) {
                Map<String, Object> row = attrValueRow(masterValue);
                row.put("id", null);
                row.put("quota", 0);
                row.put("quotaShow", 0);
                rows.add(row);
            } else {
                rows.add(attrValueRow(activityValue));
            }
        }
        activityBySuk.values().stream()
                .map(this::attrValueRow)
                .forEach(rows::add);
        return rows;
    }

    private boolean isMultipleSpec(List<Map<String, Object>> attrs, List<Map<String, Object>> attrValues) {
        if (attrs != null && attrs.size() == 1 && "默认".equals(string(attrs.get(0).get("attrValues"), ""))) {
            return false;
        }
        return attrValues != null && attrValues.size() > 1;
    }

    private Map<String, Object> toManagerRow(StoreSeckillManger manager) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", manager.getId());
        row.put("name", manager.getName());
        row.put("startTime", manager.getStartTime());
        row.put("endTime", manager.getEndTime());
        row.put("time", managerTime(manager));
        row.put("img", manager.getImg());
        row.put("silderImgs", manager.getSilderImgs());
        row.put("sort", manager.getSort());
        row.put("status", manager.getStatus());
        row.put("statusName", managerStatusName(manager));
        row.put("killStatus", managerKillStatus(manager));
        row.put("createTime", dateText(manager.getCreateTime()));
        row.put("updateTime", dateText(manager.getUpdateTime()));
        row.put("isDel", manager.getIsDel());
        return row;
    }

    private StoreSeckill requiredSeckill(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("秒杀商品id不能为空");
        }
        StoreSeckill seckill = seckillMapper.selectById(id);
        if (seckill == null || Boolean.TRUE.equals(seckill.getIsDel())) {
            throw new IllegalArgumentException("秒杀商品不存在");
        }
        return seckill;
    }

    private StoreSeckillManger requiredManager(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("秒杀配置id不能为空");
        }
        StoreSeckillManger manager = mangerMapper.selectById(id);
        if (manager == null || Boolean.TRUE.equals(manager.getIsDel())) {
            throw new IllegalArgumentException("秒杀配置不存在");
        }
        return manager;
    }

    private void applyManagerRequest(StoreSeckillManger manager, SeckillManagerRequest request, int[] range) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("秒杀名称不能为空");
        }
        manager.setName(request.getName().trim());
        manager.setStartTime(range[0]);
        manager.setEndTime(range[1]);
        manager.setImg(cleanPath(request.getImg()));
        manager.setSilderImgs(StringUtils.hasText(request.getSilderImgs()) ? cleanPath(request.getSilderImgs()) : "[]");
        manager.setSort(request.getSort());
        manager.setStatus(normalizeManagerStatus(request.getStatus()));
    }

    private void applyProductRequest(StoreSeckill seckill, SeckillProductRequest request, boolean update) {
        if (request == null) {
            throw new IllegalArgumentException("秒杀商品参数不能为空");
        }
        if (!update && (request.getProductId() == null || request.getProductId() <= 0)) {
            throw new IllegalArgumentException("主商品id不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("活动标题不能为空");
        }
        if (!StringUtils.hasText(request.getImage())) {
            throw new IllegalArgumentException("商品主图不能为空");
        }
        if (request.getTimeId() == null || request.getTimeId() <= 0) {
            throw new IllegalArgumentException("请选择秒杀时间段");
        }
        requiredManager(request.getTimeId());
        List<Map<String, Object>> attrValues = normalizedAttrValues(request);
        BigDecimal minPrice = attrValues.stream()
                .map(row -> decimal(row.get("price"), request.getPrice()))
                .min(BigDecimal::compareTo)
                .orElse(decimal(null, request.getPrice()));
        BigDecimal minCost = attrValues.stream()
                .map(row -> decimal(row.get("cost"), request.getCost()))
                .min(BigDecimal::compareTo)
                .orElse(decimal(null, request.getCost()));
        BigDecimal minOtPrice = attrValues.stream()
                .map(row -> decimal(row.get("otPrice"), request.getOtPrice()))
                .min(BigDecimal::compareTo)
                .orElse(decimal(null, request.getOtPrice()));
        int quota = attrValues.stream().mapToInt(row -> integer(row.get("quota"), request.getQuota())).sum();
        int stock = attrValues.stream().mapToInt(row -> integer(row.get("stock"), request.getStock())).sum();
        if (quota <= 0) {
            quota = positive(request.getQuota(), positive(request.getStock(), 1));
        }
        if (stock <= 0) {
            stock = quota;
        }
        validateAttrValues(attrValues, quota);
        if (!update) {
            seckill.setProductId(request.getProductId());
        }
        seckill.setImage(cleanPath(request.getImage()));
        seckill.setImages(cleanPath(imagesJson(request)));
        seckill.setTitle(request.getTitle().trim());
        seckill.setInfo(StringUtils.hasText(request.getInfo()) ? request.getInfo().trim() : "");
        seckill.setUnitName(StringUtils.hasText(request.getUnitName()) ? request.getUnitName().trim() : "件");
        seckill.setStartTime(parseDate(request.getStartTime(), "开始时间不能为空"));
        seckill.setStopTime(parseDate(request.getStopTime(), "结束时间不能为空"));
        if (seckill.getStopTime().isBefore(seckill.getStartTime())) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }
        seckill.setStatus(request.getStatus() == null || request.getStatus() <= 0 ? 0 : 1);
        seckill.setNum(positive(request.getNum(), 1));
        seckill.setTimeId(request.getTimeId());
        seckill.setTempId(positive(request.getTempId(), 0));
        seckill.setSort(request.getSort() == null ? 0 : request.getSort());
        seckill.setPrice(nvl(minPrice));
        seckill.setCost(nvl(minCost));
        seckill.setOtPrice(nvl(minOtPrice));
        seckill.setGiveIntegral(nvl(request.getGiveIntegral()));
        seckill.setStock(stock);
        seckill.setQuota(quota);
        seckill.setQuotaShow(positive(request.getQuotaShow(), quota));
        seckill.setPostage(nvl(request.getPostage()));
        seckill.setWeight(nvl(request.getWeight()));
        seckill.setVolume(nvl(request.getVolume()));
        seckill.setSpecType(Boolean.TRUE.equals(request.getSpecType()) || attrValues.size() > 1);
        seckill.setIsPostage(BigDecimal.ZERO.compareTo(seckill.getPostage()) == 0);
    }

    private void replaceAttrs(Integer seckillId, SeckillProductRequest request) {
        seckillMapper.softDeleteAttrs(seckillId);
        for (Map<String, Object> row : normalizedAttrs(request)) {
            String attrName = string(row.get("attrName"), "");
            String attrValues = string(row.get("attrValues"), "");
            if (!StringUtils.hasText(attrName) || !StringUtils.hasText(attrValues)) {
                continue;
            }
            seckillMapper.insertAttr(seckillId, attrName.trim(), attrValues.trim());
        }
    }

    private void replaceAttrValues(Integer seckillId, StoreSeckill seckill, SeckillProductRequest request) {
        seckillMapper.softDeleteAttrValues(seckillId);
        for (Map<String, Object> row : normalizedAttrValues(request)) {
            int quota = integer(row.get("quota"), seckill.getQuota());
            int stock = integer(row.get("stock"), seckill.getStock());
            if (stock <= 0) {
                stock = quota;
            }
            String image = cleanPath(string(row.get("image"), seckill.getImage()));
            String attrValue = attrValueText(row.get("attrValue"));
            String suk = string(row.get("suk"), "默认");
            seckillMapper.insertAttrValue(
                    seckillId,
                    suk,
                    stock,
                    decimal(row.get("price"), seckill.getPrice()),
                    image,
                    UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                    decimal(row.get("cost"), seckill.getCost()),
                    string(row.get("barCode"), ""),
                    decimal(row.get("otPrice"), seckill.getOtPrice()),
                    decimal(row.get("weight"), seckill.getWeight()),
                    decimal(row.get("volume"), seckill.getVolume()),
                    quota,
                    integer(row.get("quotaShow"), quota),
                    attrValue);
        }
    }

    private void validateAttrValues(List<Map<String, Object>> attrValues, int totalQuota) {
        for (Map<String, Object> row : attrValues) {
            String suk = string(row.get("suk"), "默认");
            int quota = integer(row.get("quota"), totalQuota);
            int stock = integer(row.get("stock"), quota);
            if (quota <= 0) {
                throw new IllegalArgumentException("规格「" + suk + "」活动限量必须大于0");
            }
            if (stock > 0 && quota > stock) {
                throw new IllegalArgumentException("规格「" + suk + "」活动限量不能大于商品库存");
            }
            if (decimal(row.get("price"), BigDecimal.ZERO).compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("规格「" + suk + "」秒杀价格必须大于0");
            }
        }
    }

    private List<Map<String, Object>> normalizedAttrs(SeckillProductRequest request) {
        if (request != null && request.getAttr() != null && !request.getAttr().isEmpty()) {
            return request.getAttr();
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("attrName", "规格");
        row.put("attrValues", "默认");
        return List.of(row);
    }

    private List<Map<String, Object>> normalizedAttrValues(SeckillProductRequest request) {
        if (request != null && request.getAttrValue() != null && !request.getAttrValue().isEmpty()) {
            return request.getAttrValue();
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("suk", "默认");
        row.put("price", request == null ? BigDecimal.ZERO : request.getPrice());
        row.put("cost", request == null ? BigDecimal.ZERO : request.getCost());
        row.put("otPrice", request == null ? BigDecimal.ZERO : request.getOtPrice());
        row.put("quota", request == null ? 1 : request.getQuota());
        row.put("quotaShow", request == null ? 1 : request.getQuotaShow());
        row.put("stock", request == null ? 1 : request.getStock());
        row.put("image", request == null ? "" : request.getImage());
        row.put("weight", request == null ? BigDecimal.ZERO : request.getWeight());
        row.put("volume", request == null ? BigDecimal.ZERO : request.getVolume());
        row.put("attrValue", "{\"规格\":\"默认\"}");
        return List.of(row);
    }

    private void saveDescription(Integer productId, String content) {
        String description = cleanPath(content == null ? "" : content);
        if (descriptionMapper.countDescription(productId, PRODUCT_TYPE_SECKILL) > 0) {
            descriptionMapper.updateDescription(productId, PRODUCT_TYPE_SECKILL, description);
            return;
        }
        descriptionMapper.insertDescription(productId, PRODUCT_TYPE_SECKILL, description);
    }

    private String normalizeDescription(StoreSeckill item) {
        String description = descriptionMapper.selectDescription(item.getId(), PRODUCT_TYPE_SECKILL);
        if (StringUtils.hasText(description)) {
            return description;
        }
        return item.getDescription() == null ? "" : item.getDescription();
    }

    private String imagesJson(SeckillProductRequest request) {
        if (StringUtils.hasText(request.getImages())) {
            return request.getImages();
        }
        List<String> images = request.getImagess();
        if (images == null || images.isEmpty()) {
            images = List.of(request.getImage());
        }
        return "[" + images.stream()
                .filter(StringUtils::hasText)
                .map(this::jsonString)
                .collect(Collectors.joining(",")) + "]";
    }

    private String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String attrValueText(Object value) {
        if (value instanceof Map<?, ?> map) {
            return "{" + map.entrySet().stream()
                    .map(entry -> jsonString(String.valueOf(entry.getKey())) + ":" + jsonString(String.valueOf(entry.getValue())))
                    .collect(Collectors.joining(",")) + "}";
        }
        String text = string(value, "{\"规格\":\"默认\"}");
        return StringUtils.hasText(text) ? text : "{\"规格\":\"默认\"}";
    }

    private LocalDateTime parseDate(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        String text = value.trim();
        if (text.length() >= 19) {
            return LocalDateTime.parse(text.substring(0, 19), DATE_TIME);
        }
        return LocalDate.parse(text.substring(0, Math.min(10, text.length())), DATE).atStartOfDay();
    }

    private int[] parseManagerTime(String time) {
        if (!StringUtils.hasText(time) || !time.contains(",")) {
            throw new IllegalArgumentException("秒杀时间段不能为空");
        }
        String[] parts = time.split(",", 2);
        int start = parseHour(parts[0]);
        int end = parseHour(parts[1]);
        if (start < 0 || start > 23 || end < 1 || end > 24 || start >= end) {
            throw new IllegalArgumentException("请填写正确的时间范围");
        }
        return new int[] { start, end };
    }

    private int parseHour(String value) {
        String text = value == null ? "" : value.trim();
        if (text.contains(":")) {
            text = text.substring(0, text.indexOf(':'));
        }
        return Integer.parseInt(text);
    }

    private void ensureManagerTimeUnique(Integer id, int start, int end) {
        QueryWrapper<StoreSeckillManger> query = new QueryWrapper<>();
        query.eq("is_del", 0).lt("start_time", end).gt("end_time", start);
        if (id != null) {
            query.ne("id", id);
        }
        if (mangerMapper.selectCount(query) > 0) {
            throw new IllegalArgumentException("当前时间段的秒杀配置已存在");
        }
    }

    private String cleanPath(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.replace("http://127.0.0.1:19527/", "")
                .replace("http://127.0.0.1:18080/", "")
                .replaceFirst("^/+", "");
    }

    private List<StoreSeckillManger> allManagers() {
        QueryWrapper<StoreSeckillManger> query = new QueryWrapper<>();
        query.eq("is_del", 0).orderByAsc("sort").orderByAsc("id");
        return mangerMapper.selectList(query);
    }

    private Integer currentTimeId() {
        int hour = LocalDateTime.now().getHour();
        QueryWrapper<StoreSeckillManger> query = new QueryWrapper<>();
        query.eq("is_del", 0).le("start_time", hour).gt("end_time", hour).last("limit 1");
        StoreSeckillManger manager = mangerMapper.selectOne(query);
        return manager == null ? 0 : manager.getId();
    }

    private String statusName(StoreSeckill seckill, Integer currentTimeId) {
        if (seckill.getStatus() == null || seckill.getStatus() == 0) {
            return "已关闭";
        }
        LocalDate today = LocalDate.now();
        LocalDate start = seckill.getStartTime() == null ? null : seckill.getStartTime().toLocalDate();
        LocalDate stop = seckill.getStopTime() == null ? null : seckill.getStopTime().toLocalDate();
        if (start != null && today.isBefore(start)) {
            return "未开始";
        }
        if (start != null && stop != null && !today.isBefore(start) && !today.isAfter(stop)) {
            return seckill.getTimeId() != null && seckill.getTimeId().equals(currentTimeId) ? "进行中" : "未开始";
        }
        return "已结束";
    }

    private Integer killStatus(StoreSeckill seckill, Integer currentTimeId) {
        String name = statusName(seckill, currentTimeId);
        return switch (name) {
            case "已关闭" -> 0;
            case "进行中" -> 2;
            case "已结束" -> -1;
            default -> 1;
        };
    }

    private String managerStatusName(StoreSeckillManger manager) {
        int status = managerStatusValue(manager.getStatus());
        int hour = LocalDateTime.now().getHour();
        if (status == 0) return "关闭";
        if (manager.getStartTime() != null && hour < manager.getStartTime()) return "即将开始";
        if (manager.getEndTime() != null && hour < manager.getEndTime()) return "进行中";
        return "已结束";
    }

    private Integer managerKillStatus(StoreSeckillManger manager) {
        return switch (managerStatusName(manager)) {
            case "关闭" -> 0;
            case "进行中" -> 2;
            case "已结束" -> -1;
            default -> 1;
        };
    }

    private String managerTime(StoreSeckillManger manager) {
        return hourText(manager.getStartTime()) + "," + hourText(manager.getEndTime());
    }

    private String hourText(Integer value) {
        int hour = value == null ? 0 : value;
        return (hour < 10 ? "0" + hour : Integer.toString(hour)) + ":00";
    }

    private String normalizeManagerStatus(String value) {
        if (!StringUtils.hasText(value)) return "'1'";
        String trimmed = value.trim();
        if ("0".equals(trimmed) || "'0'".equals(trimmed) || "false".equalsIgnoreCase(trimmed)) return "'0'";
        return "'1'";
    }

    private int managerStatusValue(String value) {
        return "'0'".equals(value) || "0".equals(value) ? 0 : 1;
    }

    private String dateText(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME);
    }

    private String dateOnly(LocalDateTime value) {
        return value == null ? "" : value.format(DATE);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal decimal(Object value, BigDecimal fallback) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            return new BigDecimal(String.valueOf(value));
        }
        return nvl(fallback);
    }

    private int integer(Object value, Integer fallback) {
        if (value instanceof Number number) return number.intValue();
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            return Integer.parseInt(String.valueOf(value));
        }
        return fallback == null ? 0 : fallback;
    }

    private int positive(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private String string(Object value, String fallback) {
        return value == null || !StringUtils.hasText(String.valueOf(value)) ? fallback : String.valueOf(value);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String text(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private Object managerValue(Map<?, ?> manager, String key) {
        Object value = manager.get(key);
        return value == null ? "-" : value;
    }

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String csv(Object... values) {
        List<String> cells = new ArrayList<>();
        for (Object value : values) {
            String text = value == null ? "" : String.valueOf(value);
            cells.add("\"" + text.replace("\"", "\"\"") + "\"");
        }
        return String.join(",", cells);
    }
}
