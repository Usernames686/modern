package com.jsy.crmeb.modern.service.marketing;

import com.jsy.crmeb.modern.common.config.CrmebRuntimeProperties;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.marketing.dto.CombinationProductRequest;
import com.jsy.crmeb.modern.service.marketing.mapper.CombinationAdminMapper;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CombinationAdminService {
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PRODUCT_TYPE_COMBINATION = 3;

    private final CombinationAdminMapper combinationMapper;
    private final StoreProductAttrMapper attrMapper;
    private final StoreProductAttrValueMapper attrValueMapper;
    private final StoreProductDescriptionMapper descriptionMapper;
    private final ProductAdminService productAdminService;
    private final CrmebRuntimeProperties runtimeProperties;

    public CombinationAdminService(
            CombinationAdminMapper combinationMapper,
            StoreProductAttrMapper attrMapper,
            StoreProductAttrValueMapper attrValueMapper,
            StoreProductDescriptionMapper descriptionMapper,
            ProductAdminService productAdminService,
            CrmebRuntimeProperties runtimeProperties) {
        this.combinationMapper = combinationMapper;
        this.attrMapper = attrMapper;
        this.attrValueMapper = attrValueMapper;
        this.descriptionMapper = descriptionMapper;
        this.productAdminService = productAdminService;
        this.runtimeProperties = runtimeProperties;
    }

    public PageResponse<Map<String, Object>> list(Integer page, Integer limit, Integer isShow, String keywords) {
        int safePage = safePage(page);
        int safeLimit = safeLimit(limit);
        String safeKeywords = StringUtils.hasText(keywords) ? keywords.trim() : null;
        long total = combinationMapper.countProducts(isShow, safeKeywords);
        List<Map<String, Object>> rows = combinationMapper.selectProducts(isShow, safeKeywords, safeLimit, offset(safePage, safeLimit)).stream()
                .map(this::productRow)
                .toList();
        return new PageResponse<>(safePage, safeLimit, total, rows);
    }

    public Map<String, Object> info(Integer id) {
        Map<String, Object> result = productRow(requiredProduct(id));
        addProductFormData(id, result);
        return result;
    }

    public Map<String, Object> productDraft(Integer productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("商品id不能为空");
        }
        StoreProductInfoResponse product = productAdminService.info(productId);
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("productId", product.getId());
        draft.put("id", product.getId());
        draft.put("title", nvlText(product.getStoreName()));
        draft.put("storeName", nvlText(product.getStoreName()));
        draft.put("image", nvlText(product.getImage()));
        draft.put("images", nvlText(product.getSliderImage()));
        draft.put("sliderImage", nvlText(product.getSliderImage()));
        draft.put("unitName", nvlText(product.getUnitName(), "件"));
        draft.put("tempId", product.getTempId() == null ? 0 : product.getTempId());
        draft.put("info", nvlText(product.getStoreInfo()));
        draft.put("content", nvlText(product.getContent()));
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
    public boolean save(CombinationProductRequest request) {
        applyProductRequest(request, false);
        combinationMapper.insertProduct(request);
        replaceAttrs(request.getId(), request);
        replaceAttrValues(request.getId(), request);
        saveDescription(request.getId(), request.getContent());
        return true;
    }

    @Transactional
    public boolean update(Integer id, CombinationProductRequest request) {
        requiredProduct(id);
        request.setId(id);
        applyProductRequest(request, true);
        int updated = combinationMapper.updateProduct(id, request);
        replaceAttrs(id, request);
        replaceAttrValues(id, request);
        saveDescription(id, request.getContent());
        return updated > 0;
    }

    @Transactional
    public boolean updateStatus(Integer id, Object isShow) {
        requiredProduct(id);
        return combinationMapper.updateProductShow(id, truthy(isShow) ? 1 : 0) > 0;
    }

    @Transactional
    public boolean delete(Integer id) {
        Map<String, Object> row = requiredProduct(id);
        long now = System.currentTimeMillis();
        if (truthy(row.get("isShow")) && longValue(row.get("startTime")) <= now && now <= longValue(row.get("stopTime"))) {
            throw new IllegalArgumentException("活动开启中，商品不支持删除");
        }
        return combinationMapper.softDeleteProduct(id) > 0;
    }

    public Map<String, Object> statistics() {
        return Map.of(
                "countPeople", combinationMapper.countAllPink(),
                "countTeam", combinationMapper.countSuccessTeams());
    }

    public PageResponse<Map<String, Object>> pinkList(Integer page, Integer limit, Integer status, String dateLimit) {
        int safePage = safePage(page);
        int safeLimit = safeLimit(limit);
        long[] range = parseDateLimit(dateLimit);
        Long start = range == null ? null : range[0];
        Long end = range == null ? null : range[1];
        long total = combinationMapper.countPinkHeads(status, start, end);
        List<Map<String, Object>> rows = combinationMapper.selectPinkHeads(status, start, end, safeLimit, offset(safePage, safeLimit)).stream()
                .map(this::pinkRow)
                .toList();
        return new PageResponse<>(safePage, safeLimit, total, rows);
    }

    public List<Map<String, Object>> pinkOrders(Integer pinkId) {
        if (pinkId == null || pinkId <= 0) {
            throw new IllegalArgumentException("拼团id不能为空");
        }
        return combinationMapper.selectPinkOrders(pinkId).stream()
                .map(this::pinkOrderRow)
                .toList();
    }

    public Map<String, String> export(Integer isShow, String keywords) {
        String safeKeywords = StringUtils.hasText(keywords) ? keywords.trim() : null;
        List<Map<String, Object>> rows = combinationMapper.selectProducts(isShow, safeKeywords, 10000, 0).stream()
                .map(this::productRow)
                .toList();
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("没有可导出的数据");
        }
        List<String> lines = new ArrayList<>();
        lines.add(csv("编号", "拼团名称", "原价", "拼团价", "库存", "拼团人数", "参与人数", "成团数量", "销量", "商品状态", "拼团结束时间"));
        for (Map<String, Object> row : rows) {
            lines.add(csv(
                    row.get("id"),
                    row.get("title"),
                    row.get("otPrice"),
                    row.get("price"),
                    row.get("quotaShow"),
                    row.get("countPeople"),
                    row.get("countPeopleAll"),
                    row.get("countPeoplePink"),
                    row.get("sales"),
                    Boolean.TRUE.equals(row.get("isShow")) ? "开启" : "关闭",
                    row.get("stopTimeStr")));
        }
        String fileName = "拼团商品导出_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + ".csv";
        Path root = Path.of(runtimeProperties.getImagePath() == null || runtimeProperties.getImagePath().isBlank()
                ? "./upload/"
                : runtimeProperties.getImagePath()).toAbsolutePath().normalize();
        Path exportDir = root.resolve("crmebimage/export").normalize();
        try {
            Files.createDirectories(exportDir);
            Files.writeString(exportDir.resolve(fileName), "\uFEFF" + String.join("\n", lines), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("导出拼团商品失败：" + exception.getMessage());
        }
        return Map.of("fileName", "/crmebimage/export/" + fileName);
    }

    private Map<String, Object> productRow(Map<String, Object> source) {
        Map<String, Object> row = copy(source);
        row.put("isHost", truthy(source.get("isHost")));
        row.put("isShow", truthy(source.get("isShow")));
        row.put("isDel", truthy(source.get("isDel")));
        row.put("combination", truthy(source.get("combination")));
        row.put("merUse", truthy(source.get("merUse")));
        row.put("isPostage", truthy(source.get("isPostage")));
        row.put("stopTimeStr", dateText(source.get("stopTime")));
        row.put("startTimeStr", dateText(source.get("startTime")));
        row.put("addTimeStr", dateTimeText(source.get("addTime")));
        row.put("remainingQuota", intValue(source.get("quota")));
        row.put("sliderImage", row.getOrDefault("images", "[]"));
        row.put("status", row.get("isShow"));
        row.putIfAbsent("countPeople", 0);
        row.putIfAbsent("countPeopleAll", 0);
        row.putIfAbsent("countPeoplePink", 0);
        return row;
    }

    private void addProductFormData(Integer id, Map<String, Object> result) {
        result.put("content", nvlText(descriptionMapper.selectDescription(id, PRODUCT_TYPE_COMBINATION)));
        Integer productId = intValue(result.get("productId"));
        List<ProductAttrValueResponse> combinationValues = attrValueMapper.selectByProductIdAndType(id, PRODUCT_TYPE_COMBINATION);
        List<ProductAttrValueResponse> masterValues = productId > 0
                ? attrValueMapper.selectByProductIdAndType(productId, 0)
                : List.of();
        List<Map<String, Object>> attrValues = mergeAttrValues(masterValues, combinationValues);
        result.put("attrValue", attrValues);
        List<Map<String, Object>> attrs = attrMapper.selectByProductIdAndType(id, PRODUCT_TYPE_COMBINATION).stream()
                .map(this::attrRow)
                .toList();
        result.put("attr", attrs);
        result.put("specType", isMultipleSpec(attrs, attrValues));
    }

    private List<Map<String, Object>> mergeAttrValues(
            List<ProductAttrValueResponse> masterValues,
            List<ProductAttrValueResponse> combinationValues) {
        if (masterValues == null || masterValues.isEmpty()) {
            return combinationValues == null ? List.of() : combinationValues.stream()
                    .map(this::attrValueRow)
                    .toList();
        }
        Map<String, ProductAttrValueResponse> combinationBySuk = (combinationValues == null ? List.<ProductAttrValueResponse>of() : combinationValues).stream()
                .collect(Collectors.toMap(
                        value -> string(value.getSuk(), "默认"),
                        value -> value,
                        (left, right) -> left,
                        LinkedHashMap::new));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ProductAttrValueResponse masterValue : masterValues) {
            ProductAttrValueResponse combinationValue = combinationBySuk.remove(string(masterValue.getSuk(), "默认"));
            if (combinationValue == null) {
                Map<String, Object> row = attrValueRow(masterValue);
                row.put("id", null);
                row.put("quota", 0);
                row.put("quotaShow", 0);
                rows.add(row);
            } else {
                rows.add(attrValueRow(combinationValue));
            }
        }
        combinationBySuk.values().stream()
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

    private Map<String, Object> attrRow(ProductAttrResponse source) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", source.getId());
        row.put("productId", source.getProductId());
        row.put("attrName", nvlText(source.getAttrName()));
        row.put("attrValues", nvlText(source.getAttrValues()));
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
        row.put("attrValue", nvlText(source.getAttrValue(), "{}"));
        row.put("barCode", nvlText(source.getBarCode()));
        return row;
    }

    private Map<String, Object> pinkRow(Map<String, Object> source) {
        Map<String, Object> row = copy(source);
        row.put("addTime", dateTimeText(source.get("addTime")));
        row.put("stopTime", dateTimeText(source.get("stopTime")));
        row.put("isTpl", truthy(source.get("isTpl")));
        row.put("isRefund", truthy(source.get("isRefund")));
        row.put("isVirtual", truthy(source.get("isVirtual")));
        row.putIfAbsent("avatar", "");
        row.putIfAbsent("nickname", "-");
        return row;
    }

    private Map<String, Object> pinkOrderRow(Map<String, Object> source) {
        Map<String, Object> row = pinkRow(source);
        row.putIfAbsent("refundStatus", 0);
        row.putIfAbsent("orderStatus", 0);
        return row;
    }

    private Map<String, Object> requiredProduct(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("拼团商品id不能为空");
        }
        Map<String, Object> row = combinationMapper.selectProductById(id);
        if (row == null || truthy(row.get("isDel"))) {
            throw new IllegalArgumentException("拼团商品不存在");
        }
        return row;
    }

    private void applyProductRequest(CombinationProductRequest request, boolean update) {
        if (request == null) {
            throw new IllegalArgumentException("拼团商品参数不能为空");
        }
        if (!update && (request.getProductId() == null || request.getProductId() <= 0)) {
            throw new IllegalArgumentException("主商品id不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("拼团名称不能为空");
        }
        if (!StringUtils.hasText(request.getImage())) {
            throw new IllegalArgumentException("推荐图不能为空");
        }
        long start = parseDateStart(request.getStartTime(), "开始时间不能为空");
        long stop = parseDateEnd(request.getStopTime(), "结束时间不能为空");
        if (stop < start) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }
        List<Map<String, Object>> attrValues = normalizedAttrValues(request);
        BigDecimal price = attrValues.stream()
                .map(row -> decimal(row.get("price"), request.getPrice()))
                .min(BigDecimal::compareTo)
                .orElse(decimal(null, request.getPrice()));
        BigDecimal cost = attrValues.stream()
                .map(row -> decimal(row.get("cost"), request.getCost()))
                .min(BigDecimal::compareTo)
                .orElse(decimal(null, request.getCost()));
        BigDecimal otPrice = attrValues.stream()
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
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("拼团价格必须大于0");
        }
        request.setTitle(request.getTitle().trim());
        request.setImage(cleanPath(request.getImage()));
        request.setImages(cleanPath(imagesJson(request)));
        request.setPeople(Math.max(2, positive(request.getPeople(), 2)));
        request.setIsShow(request.getIsShow() == null || request.getIsShow() <= 0 ? 0 : 1);
        request.setStartTime(String.valueOf(start));
        request.setStopTime(String.valueOf(stop));
        request.setEffectiveTime(positive(request.getEffectiveTime(), 1));
        request.setUnitName(StringUtils.hasText(request.getUnitName()) ? request.getUnitName().trim() : "件");
        request.setTempId(positive(request.getTempId(), 0));
        request.setNum(positive(request.getNum(), 1));
        request.setOnceNum(positive(request.getOnceNum(), 1));
        request.setVirtualRation(request.getVirtualRation() == null ? 0 : Math.max(0, request.getVirtualRation()));
        request.setSort(request.getSort() == null ? 0 : request.getSort());
        request.setPrice(nvl(price));
        request.setCost(nvl(cost));
        request.setOtPrice(nvl(otPrice));
        request.setStock(stock);
        request.setQuota(quota);
        request.setQuotaShow(positive(request.getQuotaShow(), quota));
        request.setPostage(nvl(request.getPostage()));
        request.setIsPostage(BigDecimal.ZERO.compareTo(request.getPostage()) == 0 ? 1 : 0);
        request.setWeight(nvl(request.getWeight()));
        request.setVolume(nvl(request.getVolume()));
        request.setInfo(cleanPath(request.getInfo() == null ? "" : request.getInfo()));
        request.setContent(cleanPath(request.getContent() == null ? "" : request.getContent()));
        if (!update) {
            request.setAddTime(System.currentTimeMillis());
        }
    }

    private void replaceAttrs(Integer combinationId, CombinationProductRequest request) {
        combinationMapper.softDeleteAttrs(combinationId);
        for (Map<String, Object> row : normalizedAttrs(request)) {
            String attrName = string(row.get("attrName"), "");
            String attrValues = string(row.get("attrValues"), "");
            if (!StringUtils.hasText(attrName) || !StringUtils.hasText(attrValues)) {
                continue;
            }
            combinationMapper.insertAttr(combinationId, attrName.trim(), attrValues.trim());
        }
    }

    private void replaceAttrValues(Integer combinationId, CombinationProductRequest request) {
        combinationMapper.softDeleteAttrValues(combinationId);
        for (Map<String, Object> row : normalizedAttrValues(request)) {
            int quota = integer(row.get("quota"), request.getQuota());
            int stock = integer(row.get("stock"), request.getStock());
            if (stock <= 0) {
                stock = quota;
            }
            combinationMapper.insertAttrValue(
                    combinationId,
                    string(row.get("suk"), "默认"),
                    stock,
                    decimal(row.get("price"), request.getPrice()),
                    cleanPath(string(row.get("image"), request.getImage())),
                    UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                    decimal(row.get("cost"), request.getCost()),
                    string(row.get("barCode"), ""),
                    decimal(row.get("otPrice"), request.getOtPrice()),
                    decimal(row.get("weight"), request.getWeight()),
                    decimal(row.get("volume"), request.getVolume()),
                    quota,
                    integer(row.get("quotaShow"), quota),
                    attrValueText(row.get("attrValue")));
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
                throw new IllegalArgumentException("规格「" + suk + "」拼团价格必须大于0");
            }
        }
    }

    private List<Map<String, Object>> normalizedAttrs(CombinationProductRequest request) {
        if (request != null && request.getAttr() != null && !request.getAttr().isEmpty()) {
            return request.getAttr();
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("attrName", "规格");
        row.put("attrValues", "默认");
        return List.of(row);
    }

    private List<Map<String, Object>> normalizedAttrValues(CombinationProductRequest request) {
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
        if (descriptionMapper.countDescription(productId, PRODUCT_TYPE_COMBINATION) > 0) {
            descriptionMapper.updateDescription(productId, PRODUCT_TYPE_COMBINATION, description);
            return;
        }
        descriptionMapper.insertDescription(productId, PRODUCT_TYPE_COMBINATION, description);
    }

    private String imagesJson(CombinationProductRequest request) {
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

    private long parseDateStart(String value, String message) {
        LocalDate date = parseDate(value, message);
        return date.atStartOfDay(ZONE).toInstant().toEpochMilli();
    }

    private long parseDateEnd(String value, String message) {
        LocalDate date = parseDate(value, message);
        return date.atTime(LocalTime.MAX).atZone(ZONE).toInstant().toEpochMilli();
    }

    private LocalDate parseDate(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        String text = value.trim();
        if (text.length() >= 10) {
            return LocalDate.parse(text.substring(0, 10), DATE);
        }
        return LocalDate.parse(text, DATE);
    }

    private int safePage(Integer page) {
        return page == null || page <= 0 ? 1 : page;
    }

    private int safeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 20 : Math.min(limit, 200);
    }

    private int offset(int page, int limit) {
        return (page - 1) * limit;
    }

    private Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source == null ? Map.of() : source);
    }

    private String dateText(Object value) {
        long millis = longValue(value);
        if (millis <= 0) return "";
        return Instant.ofEpochMilli(millis).atZone(ZONE).toLocalDate().format(DATE);
    }

    private String dateTimeText(Object value) {
        long millis = longValue(value);
        if (millis <= 0) return "";
        return Instant.ofEpochMilli(millis).atZone(ZONE).toLocalDateTime().format(DATE_TIME);
    }

    private long[] parseDateLimit(String dateLimit) {
        if (!StringUtils.hasText(dateLimit) || !dateLimit.contains(",")) {
            return null;
        }
        String[] parts = dateLimit.split(",", 2);
        LocalDate start = LocalDate.parse(parts[0].trim(), DATE);
        LocalDate end = LocalDate.parse(parts[1].trim(), DATE);
        long startMillis = start.atStartOfDay(ZONE).toInstant().toEpochMilli();
        long endMillis = end.atTime(LocalTime.MAX).atZone(ZONE).toInstant().toEpochMilli();
        return new long[] { startMillis, endMillis };
    }

    private boolean truthy(Object value) {
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.intValue() != 0;
        if (value == null) return false;
        String text = String.valueOf(value);
        return "1".equals(text) || "true".equalsIgnoreCase(text) || "'1'".equals(text);
    }

    private int intValue(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return 0;
        return Integer.parseInt(String.valueOf(value));
    }

    private long longValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return 0L;
        return Long.parseLong(String.valueOf(value));
    }

    private BigDecimal decimal(Object value, BigDecimal fallback) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return nvl(fallback);
        }
        if (value instanceof BigDecimal decimal) return decimal;
        return new BigDecimal(String.valueOf(value));
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int positive(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private int integer(Object value, Integer fallback) {
        if (value instanceof Number number) return number.intValue();
        if (value == null || !StringUtils.hasText(String.valueOf(value))) return positive(fallback, 0);
        return Integer.parseInt(String.valueOf(value));
    }

    private String string(Object value, String fallback) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return fallback;
        }
        return String.valueOf(value);
    }

    private String cleanPath(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.replace("http://127.0.0.1:19527/", "")
                .replace("http://127.0.0.1:18080/", "")
                .replaceFirst("^/+", "");
    }

    private String nvlText(String value) {
        return value == null ? "" : value;
    }

    private String nvlText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
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
