package com.jsy.crmeb.modern.service.marketing;

import com.jsy.crmeb.modern.common.config.CrmebRuntimeProperties;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.marketing.dto.BargainProductRequest;
import com.jsy.crmeb.modern.service.marketing.mapper.BargainAdminMapper;
import com.jsy.crmeb.modern.service.product.dto.ProductAttrResponse;
import com.jsy.crmeb.modern.service.product.dto.ProductAttrValueResponse;
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
public class BargainAdminService {
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PRODUCT_TYPE_BARGAIN = 2;

    private final BargainAdminMapper bargainMapper;
    private final StoreProductAttrMapper attrMapper;
    private final StoreProductAttrValueMapper attrValueMapper;
    private final StoreProductDescriptionMapper descriptionMapper;
    private final CrmebRuntimeProperties runtimeProperties;

    public BargainAdminService(
            BargainAdminMapper bargainMapper,
            StoreProductAttrMapper attrMapper,
            StoreProductAttrValueMapper attrValueMapper,
            StoreProductDescriptionMapper descriptionMapper,
            CrmebRuntimeProperties runtimeProperties) {
        this.bargainMapper = bargainMapper;
        this.attrMapper = attrMapper;
        this.attrValueMapper = attrValueMapper;
        this.descriptionMapper = descriptionMapper;
        this.runtimeProperties = runtimeProperties;
    }

    public PageResponse<Map<String, Object>> list(Integer page, Integer limit, Integer status, String keywords) {
        int safePage = safePage(page);
        int safeLimit = safeLimit(limit);
        String safeKeywords = StringUtils.hasText(keywords) ? keywords.trim() : null;
        long total = bargainMapper.countProducts(status, safeKeywords);
        List<Map<String, Object>> rows = bargainMapper.selectProducts(status, safeKeywords, safeLimit, offset(safePage, safeLimit)).stream()
                .map(this::productRow)
                .toList();
        return new PageResponse<>(safePage, safeLimit, total, rows);
    }

    public Map<String, Object> info(Integer id) {
        Map<String, Object> row = requiredProduct(id);
        Map<String, Object> result = productRow(row);
        addProductFormData(id, result);
        return result;
    }

    @Transactional
    public boolean save(BargainProductRequest request) {
        applyProductRequest(request, false);
        bargainMapper.insertProduct(request);
        replaceAttrs(request.getId(), request);
        replaceAttrValues(request.getId(), request);
        saveDescription(request.getId(), request.getContent());
        return true;
    }

    @Transactional
    public boolean update(Integer id, BargainProductRequest request) {
        requiredProduct(id);
        request.setId(id);
        applyProductRequest(request, true);
        int updated = bargainMapper.updateProduct(id, request);
        replaceAttrs(id, request);
        replaceAttrValues(id, request);
        saveDescription(id, request.getContent());
        return updated > 0;
    }

    @Transactional
    public boolean updateStatus(Integer id, Object status) {
        requiredProduct(id);
        return bargainMapper.updateProductStatus(id, truthy(status) ? 1 : 0) > 0;
    }

    @Transactional
    public boolean delete(Integer id) {
        Map<String, Object> row = requiredProduct(id);
        long now = System.currentTimeMillis();
        if (truthy(row.get("status")) && longValue(row.get("startTime")) <= now && now <= longValue(row.get("stopTime"))) {
            throw new IllegalArgumentException("活动开启中，商品不支持删除");
        }
        return bargainMapper.softDeleteProduct(id) > 0;
    }

    public PageResponse<Map<String, Object>> userList(Integer page, Integer limit, Integer status, String dateLimit) {
        int safePage = safePage(page);
        int safeLimit = safeLimit(limit);
        long[] range = parseDateLimit(dateLimit);
        Long start = range == null ? null : range[0];
        Long end = range == null ? null : range[1];
        long total = bargainMapper.countBargainUsers(status, start, end);
        List<Map<String, Object>> rows = bargainMapper.selectBargainUsers(status, start, end, safeLimit, offset(safePage, safeLimit)).stream()
                .map(this::userRow)
                .toList();
        return new PageResponse<>(safePage, safeLimit, total, rows);
    }

    public List<Map<String, Object>> userHelpList(Integer bargainUserId) {
        if (bargainUserId == null || bargainUserId <= 0) {
            throw new IllegalArgumentException("砍价参与记录id不能为空");
        }
        return bargainMapper.selectBargainUserHelp(bargainUserId).stream()
                .map(this::helpRow)
                .toList();
    }

    public Map<String, String> export(Integer status, String keywords) {
        String safeKeywords = StringUtils.hasText(keywords) ? keywords.trim() : null;
        List<Map<String, Object>> rows = bargainMapper.selectProducts(status, safeKeywords, 10000, 0).stream()
                .map(this::productRow)
                .toList();
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("没有可导出的数据");
        }
        List<String> lines = new ArrayList<>();
        lines.add(csv("ID", "砍价名称", "砍价价格", "最低价", "参与人数", "帮忙砍价人数", "砍价成功人数", "限量", "限量剩余", "活动开始", "活动结束", "状态"));
        for (Map<String, Object> row : rows) {
            lines.add(csv(
                    row.get("id"),
                    row.get("title"),
                    row.get("price"),
                    row.get("minPrice"),
                    row.get("countPeopleAll"),
                    row.get("countPeopleHelp"),
                    row.get("countPeopleSuccess"),
                    row.get("quotaShow"),
                    row.get("surplusQuota"),
                    row.get("startTime"),
                    row.get("stopTime"),
                    Boolean.TRUE.equals(row.get("status")) ? "开启" : "关闭"));
        }
        String fileName = "砍价商品导出_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + ".csv";
        Path root = Path.of(runtimeProperties.getImagePath() == null || runtimeProperties.getImagePath().isBlank()
                ? "./upload/"
                : runtimeProperties.getImagePath()).toAbsolutePath().normalize();
        Path exportDir = root.resolve("crmebimage/export").normalize();
        try {
            Files.createDirectories(exportDir);
            Files.writeString(exportDir.resolve(fileName), "\uFEFF" + String.join("\n", lines), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("导出砍价商品失败：" + exception.getMessage());
        }
        return Map.of("fileName", "/crmebimage/export/" + fileName);
    }

    private Map<String, Object> productRow(Map<String, Object> source) {
        Map<String, Object> row = copy(source);
        row.put("status", truthy(source.get("status")));
        row.put("isHot", truthy(source.get("isHot")));
        row.put("isDel", truthy(source.get("isDel")));
        row.put("isPostage", truthy(source.get("isPostage")));
        String startText = dateText(source.get("startTime"));
        String stopText = dateText(source.get("stopTime"));
        row.put("startTime", startText);
        row.put("stopTime", stopText);
        row.put("startTimeStr", startText);
        row.put("stopTimeStr", stopText);
        row.put("addTime", dateTimeText(source.get("addTime")));
        row.put("surplusQuota", intValue(source.get("quota")));
        row.putIfAbsent("countPeopleAll", 0L);
        row.putIfAbsent("countPeopleHelp", 0L);
        row.putIfAbsent("countPeopleSuccess", 0L);
        return row;
    }

    private void addProductFormData(Integer id, Map<String, Object> result) {
        result.put("sliderImage", result.getOrDefault("images", "[]"));
        result.put("content", nvlText(descriptionMapper.selectDescription(id, PRODUCT_TYPE_BARGAIN)));
        List<Map<String, Object>> attrValues = attrValueMapper.selectByProductIdAndType(id, PRODUCT_TYPE_BARGAIN).stream()
                .map(this::attrValueRow)
                .toList();
        attrValues.forEach(row -> row.put("minPrice", result.get("minPrice")));
        result.put("attrValue", attrValues);
        List<Map<String, Object>> attrs = attrMapper.selectByProductIdAndType(id, PRODUCT_TYPE_BARGAIN).stream()
                .map(this::attrRow)
                .toList();
        if (attrs.isEmpty()) {
            attrs = attrMapper.selectByProductIdAndType(integer(result.get("productId"), 0), 0).stream()
                    .map(this::attrRow)
                    .toList();
        }
        result.put("attr", attrs);
        result.put("specType", isMultiSpec(attrs, attrValues));
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
        row.put("minPrice", source.getPrice());
        row.put("image", source.getImage());
        row.put("cost", source.getCost());
        row.put("otPrice", source.getOtPrice());
        row.put("weight", source.getWeight());
        row.put("volume", source.getVolume());
        row.put("attrValue", normalizeAttrValueText(source.getAttrValue()));
        row.put("barCode", nvlText(source.getBarCode()));
        return row;
    }

    private boolean isMultiSpec(List<Map<String, Object>> attrs, List<Map<String, Object>> attrValues) {
        if (attrs != null && attrs.stream().anyMatch(row -> !"规格".equals(string(row.get("attrName"), "规格")))) {
            return true;
        }
        if (attrValues != null && attrValues.size() > 1) {
            return true;
        }
        if (attrValues != null && !attrValues.isEmpty()) {
            String attrValue = string(attrValues.get(0).get("attrValue"), "");
            return StringUtils.hasText(attrValue) && !attrValue.contains("\"规格\"");
        }
        return false;
    }

    private Map<String, Object> userRow(Map<String, Object> source) {
        Map<String, Object> row = copy(source);
        row.put("addTime", dateTimeText(source.get("addTime")));
        row.put("dataTime", dateTimeText(source.get("dataTime")));
        row.put("nowPrice", decimal(source.get("nowPrice")));
        row.putIfAbsent("avatar", "");
        row.putIfAbsent("nickname", "-");
        return row;
    }

    private Map<String, Object> helpRow(Map<String, Object> source) {
        Map<String, Object> row = copy(source);
        row.put("addTime", dateTimeText(source.get("addTime")));
        row.putIfAbsent("avatar", "");
        row.putIfAbsent("nickname", "-");
        return row;
    }

    private Map<String, Object> requiredProduct(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("砍价商品id不能为空");
        }
        Map<String, Object> row = bargainMapper.selectProductById(id);
        if (row == null || truthy(row.get("isDel"))) {
            throw new IllegalArgumentException("砍价商品不存在");
        }
        return row;
    }

    private void applyProductRequest(BargainProductRequest request, boolean update) {
        if (request == null) {
            throw new IllegalArgumentException("砍价商品参数不能为空");
        }
        if (!update && (request.getProductId() == null || request.getProductId() <= 0)) {
            throw new IllegalArgumentException("主商品id不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("砍价活动名称不能为空");
        }
        if (!StringUtils.hasText(request.getImage())) {
            throw new IllegalArgumentException("砍价图片不能为空");
        }
        long start = parseDateStart(request.getStartTime(), "开始时间不能为空");
        long stop = parseDateEnd(request.getStopTime(), "结束时间不能为空");
        if (stop < start) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }
        List<Map<String, Object>> attrValues = normalizedAttrValues(request);
        BigDecimal minPrice = attrValues.stream()
                .map(row -> decimal(row.get("minPrice"), request.getMinPrice()))
                .min(BigDecimal::compareTo)
                .orElse(decimal(null, request.getMinPrice()));
        BigDecimal price = attrValues.stream()
                .map(row -> decimal(row.get("price"), request.getPrice()))
                .min(BigDecimal::compareTo)
                .orElse(decimal(null, request.getPrice()));
        BigDecimal cost = attrValues.stream()
                .map(row -> decimal(row.get("cost"), request.getCost()))
                .min(BigDecimal::compareTo)
                .orElse(decimal(null, request.getCost()));
        int quota = attrValues.stream().mapToInt(row -> integer(row.get("quota"), request.getQuota())).sum();
        int stock = attrValues.stream().mapToInt(row -> integer(row.get("stock"), request.getStock())).sum();
        if (quota <= 0) {
            quota = positive(request.getQuota(), positive(request.getStock(), 1));
        }
        if (stock <= 0) {
            stock = quota;
        }
        validateAttrValues(attrValues, request.getPeopleNum(), quota);
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("砍价起始金额必须大于0");
        }
        if (minPrice.compareTo(price) > 0) {
            throw new IllegalArgumentException("砍价最低价不能大于起始金额");
        }
        String title = request.getTitle().trim();
        String storeName = StringUtils.hasText(request.getStoreName()) ? request.getStoreName().trim() : title;
        String images = imagesJson(request);
        request.setTitle(title);
        request.setStoreName(storeName);
        request.setImage(cleanPath(request.getImage()));
        request.setImages(cleanPath(images));
        request.setUnitName(StringUtils.hasText(request.getUnitName()) ? request.getUnitName().trim() : "件");
        request.setStartTime(String.valueOf(start));
        request.setStopTime(String.valueOf(stop));
        request.setStatus(request.getStatus() == null || request.getStatus() <= 0 ? 0 : 1);
        request.setNum(positive(request.getNum(), 1));
        request.setBargainNum(positive(request.getBargainNum(), 1));
        request.setPeopleNum(positive(request.getPeopleNum(), 1));
        request.setTempId(positive(request.getTempId(), 0));
        request.setSort(request.getSort() == null ? 0 : request.getSort());
        request.setPrice(nvl(price));
        request.setMinPrice(nvl(minPrice));
        request.setCost(nvl(cost));
        request.setGiveIntegral(nvl(request.getGiveIntegral()));
        request.setStock(stock);
        request.setQuota(quota);
        request.setQuotaShow(positive(request.getQuotaShow(), quota));
        request.setPostage(nvl(request.getPostage()));
        request.setIsPostage(BigDecimal.ZERO.compareTo(request.getPostage()) == 0 ? 1 : 0);
        request.setWeight(nvl(request.getWeight()));
        request.setVolume(nvl(request.getVolume()));
        request.setInfo("");
        request.setRule(cleanPath(request.getRule() == null ? "" : request.getRule()));
        request.setContent(cleanPath(request.getContent() == null ? "" : request.getContent()));
        if (!update) {
            request.setAddTime(System.currentTimeMillis());
        }
    }

    private void replaceAttrs(Integer bargainId, BargainProductRequest request) {
        bargainMapper.softDeleteAttrs(bargainId);
        for (Map<String, Object> row : normalizedAttrs(request)) {
            String attrName = string(row.get("attrName"), "");
            String attrValues = string(row.get("attrValues"), "");
            if (!StringUtils.hasText(attrName) || !StringUtils.hasText(attrValues)) {
                continue;
            }
            bargainMapper.insertAttr(bargainId, attrName.trim(), attrValues.trim());
        }
    }

    private void replaceAttrValues(Integer bargainId, BargainProductRequest request) {
        bargainMapper.softDeleteAttrValues(bargainId);
        for (Map<String, Object> row : normalizedAttrValues(request)) {
            int quota = integer(row.get("quota"), request.getQuota());
            int stock = integer(row.get("stock"), request.getStock());
            if (stock <= 0) {
                stock = quota;
            }
            String image = cleanPath(string(row.get("image"), request.getImage()));
            BigDecimal price = decimal(row.get("price"), request.getPrice());
            BigDecimal minPrice = decimal(row.get("minPrice"), request.getMinPrice());
            bargainMapper.insertAttrValue(
                    bargainId,
                    string(row.get("suk"), "默认"),
                    stock,
                    price,
                    image,
                    UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                    decimal(row.get("cost"), request.getCost()),
                    string(row.get("barCode"), ""),
                    decimal(row.get("otPrice"), request.getOtPrice()),
                    decimal(row.get("weight"), request.getWeight()),
                    decimal(row.get("volume"), request.getVolume()),
                    quota,
                    integer(row.get("quotaShow"), quota),
                    attrValueText(row.get("attrValue")));
            request.setMinPrice(minPrice);
        }
    }

    private void validateAttrValues(List<Map<String, Object>> attrValues, Integer peopleNum, int totalQuota) {
        int safePeopleNum = positive(peopleNum, 1);
        for (Map<String, Object> row : attrValues) {
            String suk = string(row.get("suk"), "默认");
            int quota = integer(row.get("quota"), totalQuota);
            int stock = integer(row.get("stock"), quota);
            BigDecimal price = decimal(row.get("price"), BigDecimal.ZERO);
            BigDecimal minPrice = decimal(row.get("minPrice"), BigDecimal.ZERO);
            if (quota <= 0) {
                throw new IllegalArgumentException("规格「" + suk + "」活动限量必须大于0");
            }
            if (stock > 0 && quota > stock) {
                throw new IllegalArgumentException("规格「" + suk + "」活动限量不能大于商品库存");
            }
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("规格「" + suk + "」砍价起始金额必须大于0");
            }
            if (minPrice.compareTo(price) > 0) {
                throw new IllegalArgumentException("规格「" + suk + "」砍价最低价不能大于起始金额");
            }
            BigDecimal minRequired = BigDecimal.valueOf(safePeopleNum).multiply(new BigDecimal("0.01")).add(minPrice);
            if (price.compareTo(minRequired) < 0) {
                throw new IllegalArgumentException("规格「" + suk + "」砍价起始金额不能小于" + minRequired);
            }
        }
    }

    private List<Map<String, Object>> normalizedAttrs(BargainProductRequest request) {
        if (request != null && request.getAttr() != null && !request.getAttr().isEmpty()) {
            return request.getAttr();
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("attrName", "规格");
        row.put("attrValues", "默认");
        return List.of(row);
    }

    private List<Map<String, Object>> normalizedAttrValues(BargainProductRequest request) {
        if (request != null && request.getAttrValue() != null && !request.getAttrValue().isEmpty()) {
            return request.getAttrValue();
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("suk", "默认");
        row.put("price", request == null ? BigDecimal.ZERO : request.getPrice());
        row.put("minPrice", request == null ? BigDecimal.ZERO : request.getMinPrice());
        row.put("cost", request == null ? BigDecimal.ZERO : request.getCost());
        row.put("otPrice", request == null ? BigDecimal.ZERO : request.getPrice());
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
        if (descriptionMapper.countDescription(productId, PRODUCT_TYPE_BARGAIN) > 0) {
            descriptionMapper.updateDescription(productId, PRODUCT_TYPE_BARGAIN, description);
            return;
        }
        descriptionMapper.insertDescription(productId, PRODUCT_TYPE_BARGAIN, description);
    }

    private String imagesJson(BargainProductRequest request) {
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

    private String normalizeAttrValueText(String value) {
        String text = nvlText(value, "{\"规格\":\"默认\"}").trim();
        if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
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

    private BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(String.valueOf(value));
    }

    private BigDecimal decimal(Object value, BigDecimal fallback) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return nvl(fallback);
        }
        return decimal(value);
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
