package com.jsy.crmeb.modern.service.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsy.crmeb.modern.common.config.CrmebRuntimeProperties;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.admin.mapper.LegacyLoginUiMapper;
import com.jsy.crmeb.modern.service.coupon.CouponAdminService;
import com.jsy.crmeb.modern.service.product.dto.ProductAddStockRequest;
import com.jsy.crmeb.modern.service.product.dto.ProductAttrValueAddStockRequest;
import com.jsy.crmeb.modern.service.product.dto.ProductAttrValueResponse;
import com.jsy.crmeb.modern.service.product.dto.StoreProductBasicUpdateRequest;
import com.jsy.crmeb.modern.service.product.dto.StoreProductCreateRequest;
import com.jsy.crmeb.modern.service.product.dto.StoreProductResponse;
import com.jsy.crmeb.modern.service.product.dto.StoreProductInfoResponse;
import com.jsy.crmeb.modern.service.product.dto.StoreProductSearchRequest;
import com.jsy.crmeb.modern.service.product.dto.StoreProductTabsHeader;
import com.jsy.crmeb.modern.service.product.entity.StoreProduct;
import com.jsy.crmeb.modern.service.product.mapper.ProductActivityMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreCartMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductAttrMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductAttrValueMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductCouponMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductDescriptionMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductRelationMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductAdminService {
    private static final List<String> COPY_PLATFORM_NAMES = List.of("", "淘宝", "京东", "苏宁", "拼多多", "天猫");
    private static final int NORMAL_PRODUCT_ATTR_TYPE = 0;
    private static final String DEFAULT_ATTR_NAME = "规格";
    private static final String DEFAULT_ATTR_VALUE = "默认";
    private static final String DEFAULT_ATTR_VALUE_JSON = "{\"规格\":\"默认\"}";
    private static final int DEFAULT_PRODUCT_SORT = 32760;
    private static final int MAX_PRODUCT_SORT = 32767;
    private static final int DEFAULT_PRODUCT_STOCK = 100;

    private final StoreProductMapper storeProductMapper;
    private final StoreProductRelationMapper relationMapper;
    private final ProductCatalogService catalogService;
    private final LegacyLoginUiMapper configMapper;
    private final StoreCartMapper storeCartMapper;
    private final StoreProductAttrValueMapper attrValueMapper;
    private final ProductActivityMapper activityMapper;
    private final StoreProductAttrMapper attrMapper;
    private final StoreProductDescriptionMapper descriptionMapper;
    private final StoreProductCouponMapper couponMapper;
    private final CouponAdminService couponAdminService;
    private final CrmebRuntimeProperties runtimeProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    public ProductAdminService(
            StoreProductMapper storeProductMapper,
            StoreProductRelationMapper relationMapper,
            ProductCatalogService catalogService,
            LegacyLoginUiMapper configMapper,
            StoreCartMapper storeCartMapper,
            StoreProductAttrValueMapper attrValueMapper,
            ProductActivityMapper activityMapper,
            StoreProductAttrMapper attrMapper,
            StoreProductDescriptionMapper descriptionMapper,
            StoreProductCouponMapper couponMapper,
            CouponAdminService couponAdminService,
            CrmebRuntimeProperties runtimeProperties,
            ObjectMapper objectMapper) {
        this.storeProductMapper = storeProductMapper;
        this.relationMapper = relationMapper;
        this.catalogService = catalogService;
        this.configMapper = configMapper;
        this.storeCartMapper = storeCartMapper;
        this.attrValueMapper = attrValueMapper;
        this.activityMapper = activityMapper;
        this.attrMapper = attrMapper;
        this.descriptionMapper = descriptionMapper;
        this.couponMapper = couponMapper;
        this.couponAdminService = couponAdminService;
        this.runtimeProperties = runtimeProperties;
        this.objectMapper = objectMapper;
    }

    public PageResponse<StoreProductResponse> list(StoreProductSearchRequest request) {
        int page = request.getPage() <= 0 ? 1 : request.getPage();
        int limit = request.getLimit() <= 0 ? 20 : request.getLimit();
        Page<StoreProduct> productPage = storeProductMapper.selectPage(new Page<>(page, limit), buildQuery(request, true));
        List<StoreProductResponse> list = productPage.getRecords().stream().map(this::toResponse).toList();
        return new PageResponse<>(page, limit, productPage.getTotal(), list);
    }

    public List<StoreProductTabsHeader> tabsHeaders(StoreProductSearchRequest request) {
        List<StoreProductTabsHeader> headers = new ArrayList<>();
        headers.add(new StoreProductTabsHeader(0, "出售中商品", 1));
        headers.add(new StoreProductTabsHeader(0, "仓库中商品", 2));
        headers.add(new StoreProductTabsHeader(0, "已经售馨商品", 3));
        headers.add(new StoreProductTabsHeader(0, "警戒库存", 4));
        headers.add(new StoreProductTabsHeader(0, "商品回收站", 5));
        for (StoreProductTabsHeader header : headers) {
            StoreProductSearchRequest copy = copyForType(request, header.getType());
            header.setCount(Math.toIntExact(storeProductMapper.selectCount(buildQuery(copy, false))));
        }
        return headers;
    }

    public StoreProductInfoResponse info(Integer id) {
        StoreProduct product = getExistingProduct(id);
        StoreProductInfoResponse response = new StoreProductInfoResponse();
        fillProductResponse(product, response);
        response.setSliderImage(product.getSliderImage());
        response.setStoreInfo(product.getStoreInfo());
        response.setVideoLink(normalizeAsset(product.getVideoLink()));
        response.setUnitName(product.getUnitName());
        response.setGiveIntegral(product.getGiveIntegral());
        response.setTempId(product.getTempId());
        response.setSpecType(Integer.valueOf(1).equals(product.getSpecType()));
        response.setIsSub(Integer.valueOf(1).equals(product.getIsSub()));
        response.setIsGood(Integer.valueOf(1).equals(product.getIsGood()));
        response.setActivity(activityList(product.getActivity()));
        response.setAttr(attrMapper.selectByProductIdAndType(id, 0));
        List<ProductAttrValueResponse> attrValues = attrValueMapper.selectByProductIdAndType(id, 0);
        attrValues.forEach(value -> {
            value.setImage(normalizeAsset(value.getImage()));
            value.setAddStock(0);
        });
        response.setAttrValue(attrValues);
        response.setContent(descriptionMapper.selectDescription(id, 0));
        List<Integer> couponIds = couponMapper.selectCouponIds(id);
        response.setCouponIds(couponIds);
        response.setCoupons(couponAdminService.byIds(couponIds));
        return response;
    }

    public List<StoreProductResponse> listByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<Integer, StoreProduct> productMap = storeProductMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(StoreProduct::getId, Function.identity(), (left, right) -> left));
        return ids.stream()
                .map(productMap::get)
                .filter(product -> product != null && !Integer.valueOf(1).equals(product.getIsDel()))
                .map(this::toResponse)
                .toList();
    }

    public Map<String, String> exportProduct(StoreProductSearchRequest request) {
        Page<StoreProduct> productPage = storeProductMapper.selectPage(new Page<>(1, 10000), buildQuery(request, true));
        if (productPage.getRecords().isEmpty()) {
            throw new IllegalArgumentException("没有可导出的数据！");
        }
        List<String> lines = new ArrayList<>();
        lines.add(csvLine(List.of("商品名称", "商品分类", "价格", "库存", "销量", "浏览量")));
        for (StoreProduct product : productPage.getRecords()) {
            StoreProductResponse response = toResponse(product);
            lines.add(csvLine(List.of(
                    response.getStoreName(),
                    response.getCateValues(),
                    money(response.getPrice()),
                    String.valueOf(response.getStock()),
                    String.valueOf(response.getSales()),
                    String.valueOf(response.getBrowse()))));
        }
        String fileName = "商品导出_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(111111111, 999999999)
                + ".csv";
        Path exportDir = Path.of(runtimeProperties.getImagePath(), "crmebimage", "export").toAbsolutePath().normalize();
        try {
            Files.createDirectories(exportDir);
            Files.writeString(exportDir.resolve(fileName), "\uFEFF" + String.join("\n", lines), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("导出商品失败：" + exception.getMessage());
        }
        return Map.of("fileName", "/crmebimage/export/" + fileName);
    }

    public Map<String, Object> copyConfig() {
        String copyType = configMapper.selectConfigValue("system_product_copy_type");
        if (!StringUtils.hasText(copyType)) {
            throw new IllegalArgumentException("请先进行采集商品配置");
        }
        return Map.of(
                "copyType", copyType,
                "copyNum", 0);
    }

    public Map<String, Object> copyProduct(String url) {
        StoreProductCreateRequest info = importProductFromUrl(url, detectCopyPlatform(url));
        return Map.of("info", info);
    }

    public StoreProductCreateRequest importProductFromUrl(String url, Integer form) {
        int platform = form == null ? detectCopyPlatform(url) : form;
        validateCopyUrl(url, platform);
        try {
            JsonNode root = requestJson(copyApiUrl(url, platform));
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                throw new IllegalArgumentException("复制商品失败--返回数据格式错误--未找到data");
            }
            JsonNode node = data.has("item") ? data.path("item") : data;
            StoreProductCreateRequest product = new StoreProductCreateRequest();
            product.setStoreName(text(node, "title", text(node, "store_name", "")));
            product.setKeyword(product.getStoreName());
            product.setImage(firstImage(node.path("images"), text(node, "image", "")));
            product.setSliderImages(sliderImagesFromNode(node.path("images"), product.getImage()));
            product.setPrice(decimal(node, "price"));
            product.setOtPrice(decimal(node, "ot_price"));
            product.setCost(decimal(node, "cost"));
            product.setUnitName(text(node, "unit_name", ""));
            product.setContent(text(node, "desc", text(node, "description", "")));
            product.setVipPrice(decimal(node, "vip_price"));
            product.setSort(0);
            product.setIsShow(false);
            product.setIsBenefit(false);
            product.setIsNew(false);
            product.setIsGood(false);
            product.setIsHot(false);
            product.setIsBest(false);
            product.setIsSub(false);
            product.setFicti(0);
            product.setGiveIntegral(0);
            product.setActivity(List.of("默认", "秒杀", "砍价", "拼团"));
            product.setCouponIds(List.of());
            return product;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("确认URL和平台是否正确，以及平台费用是否足额" + exception.getMessage());
        }
    }

    @Transactional
    public Integer create(StoreProductCreateRequest request) {
        validateProductBase(request.getStoreName(), request.getCateId(), request.getImage(), request.getPrice());
        StoreProduct product = new StoreProduct();
        fillProductForSave(product, request);
        int now = (int) (System.currentTimeMillis() / 1000);
        product.setAddTime(now);
        product.setSales(0);
        product.setStock(DEFAULT_PRODUCT_STOCK);
        product.setPostage(BigDecimal.ZERO);
        product.setIsPostage(0);
        product.setIsDel(0);
        product.setIsRecycle(0);
        product.setBarCode("");
        product.setBrowse(0);
        product.setSpecType(0);
        product.setFlatPattern("");
        product.setVersion(0);
        storeProductMapper.insert(product);
        ensureDefaultSku(product);
        upsertProductDescription(product.getId(), request.getContent());
        updateProductCoupons(product.getId(), request.getCouponIds() == null ? List.of() : request.getCouponIds());
        return product.getId();
    }

    @Transactional
    public void updateBasic(StoreProductBasicUpdateRequest request) {
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new IllegalArgumentException("商品id不能为空");
        }
        validateProductBase(request.getStoreName(), request.getCateId(), request.getImage(), request.getPrice());
        StoreProduct product = getExistingProduct(request.getId());
        fillProductForSave(product, request);
        storeProductMapper.updateById(product);
        syncDefaultSku(product);
        if (request.getContent() != null) {
            upsertProductDescription(product.getId(), request.getContent());
        }
        if (request.getCouponIds() != null) {
            updateProductCoupons(product.getId(), request.getCouponIds());
        }
    }

    @Transactional
    public void deleteProduct(Integer id, String type) {
        StoreProduct product = getExistingProduct(id);
        if ("delete".equals(type)) {
            assertNoActiveMarketing(id);
            product.setIsDel(1);
            storeProductMapper.updateById(product);
            storeCartMapper.deleteByProductId(id);
            return;
        }
        if (Integer.valueOf(1).equals(product.getIsRecycle())) {
            throw new IllegalArgumentException("商品已存在回收站");
        }
        product.setIsRecycle(1);
        storeProductMapper.updateById(product);
        storeCartMapper.disableByProductId(id);
    }

    public void restoreProduct(Integer id) {
        StoreProduct product = getExistingProduct(id);
        product.setIsRecycle(0);
        storeProductMapper.updateById(product);
    }

    @Transactional
    public void putOnShelf(Integer id) {
        StoreProduct product = getExistingProduct(id);
        if (Integer.valueOf(1).equals(product.getIsShow())) {
            return;
        }
        product.setIsShow(1);
        storeProductMapper.updateById(product);
        List<Integer> skuIds = attrValueMapper.selectIdsByProductIdAndType(id, 0);
        if (!skuIds.isEmpty()) {
            storeCartMapper.enableBySkuIds(skuIds);
        }
    }

    @Transactional
    public void offShelf(Integer id) {
        StoreProduct product = getExistingProduct(id);
        if (Integer.valueOf(0).equals(product.getIsShow())) {
            return;
        }
        product.setIsShow(0);
        storeProductMapper.updateById(product);
        storeCartMapper.disableByProductId(id);
        relationMapper.deleteByProductId(id);
    }

    @Transactional
    public void quickAddStock(ProductAddStockRequest request) {
        StoreProduct product = getExistingProduct(request.getId());
        List<ProductAttrValueAddStockRequest> requestValues = request.getAttrValueList();
        HashSet<Integer> attrIds = new HashSet<>();
        for (ProductAttrValueAddStockRequest value : requestValues) {
            if (!attrIds.add(value.getId())) {
                throw new IllegalArgumentException("有重复的商品规格属性ID");
            }
        }
        List<Integer> attrIdList = requestValues.stream().map(ProductAttrValueAddStockRequest::getId).toList();
        List<ProductAttrValueResponse> existingValues = attrValueMapper.selectByProductIdAndTypeAndIds(product.getId(), 0, attrIdList);
        if (existingValues.size() != attrIdList.size()) {
            throw new IllegalArgumentException("商品规格属性ID数组数据异常，请刷新后再试");
        }
        Map<Integer, ProductAttrValueResponse> valueMap = existingValues.stream()
                .collect(Collectors.toMap(ProductAttrValueResponse::getId, Function.identity()));
        int totalStock = 0;
        for (ProductAttrValueAddStockRequest value : requestValues) {
            ProductAttrValueResponse existing = valueMap.get(value.getId());
            int addStock = value.getAddStock() == null ? 0 : value.getAddStock();
            totalStock += addStock;
            int updated = attrValueMapper.incrementStock(existing.getId(), 0, addStock, existing.getVersion());
            if (updated <= 0) {
                throw new IllegalArgumentException("更新商品attrValue失败，attrValueId = " + existing.getId());
            }
        }
        int productUpdated = storeProductMapper.incrementStock(product.getId(), totalStock);
        if (productUpdated <= 0) {
            throw new IllegalArgumentException("更新普通商品库存失败,商品id = " + product.getId());
        }
    }

    private StoreProductSearchRequest copyForType(StoreProductSearchRequest source, int type) {
        StoreProductSearchRequest copy = new StoreProductSearchRequest();
        copy.setType(type);
        copy.setCateId(source.getCateId());
        copy.setKeywords(source.getKeywords());
        copy.setPriceOrder(source.getPriceOrder());
        copy.setSalesOrder(source.getSalesOrder());
        return copy;
    }

    private String money(BigDecimal value) {
        return value == null ? "" : "￥" + value.stripTrailingZeros().toPlainString();
    }

    private void validateProductBase(String storeName, String cateId, String image, BigDecimal price) {
        if (!StringUtils.hasText(storeName)) {
            throw new IllegalArgumentException("商品名称不能为空");
        }
        if (!StringUtils.hasText(cateId)) {
            throw new IllegalArgumentException("请选择商品分类");
        }
        if (!StringUtils.hasText(image)) {
            throw new IllegalArgumentException("请选择商品封面图");
        }
        if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("商品售价不能小于0");
        }
    }

    private int detectCopyPlatform(String url) {
        if (!StringUtils.hasText(url)) {
            return 1;
        }
        String lower = url.toLowerCase();
        if (lower.contains("jd.com")) {
            return 2;
        }
        if (lower.contains("suning.com")) {
            return 3;
        }
        if (lower.contains("yangkeduo.com") || lower.contains("pinduoduo.com")) {
            return 4;
        }
        if (lower.contains("tmall.com")) {
            return 5;
        }
        return 1;
    }

    private void validateCopyUrl(String url, int platform) {
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("请输入链接地址！");
        }
        if (platform < 1 || platform > 5) {
            throw new IllegalArgumentException("请选择正确的采集平台");
        }
    }

    private String copyApiUrl(String url, int platform) {
        String token = configMapper.selectConfigValue("copy_product_apikey");
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("请配置复制产品平台的Token -- www.99api.com");
        }
        String baseUrl = configMapper.selectConfigValue(switch (platform) {
            case 2 -> "importProductJD";
            case 3 -> "importProductSN";
            case 4 -> "importProductPDD";
            case 5 -> "importProductTM";
            default -> "importProductTB";
        });
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalArgumentException("请配置复制产品平台的Url-- www.99api.com");
        }
        return baseUrl
                + "?apikey="
                + encode(token.trim())
                + copyApiQuery(url.trim(), platform);
    }

    private String copyApiQuery(String url, int platform) {
        return switch (platform) {
            case 2 -> "&itemid=" + encode(removeSuffix(url.substring(url.lastIndexOf("/") + 1), ".html"));
            case 3 -> {
                int start = url.indexOf(".com/") + 5;
                int end = url.indexOf(".html");
                if (start < 5 || end <= start) {
                    throw new IllegalArgumentException("苏宁商品链接格式不正确");
                }
                String[] shopProduct = url.substring(start, end).split("/");
                if (shopProduct.length < 2) {
                    throw new IllegalArgumentException("苏宁商品链接格式不正确");
                }
                yield "&itemid=" + encode(shopProduct[1]) + "&shopid=" + encode(shopProduct[0]);
            }
            case 4 -> "&itemid=" + encode(queryParam(url, "goods_id"));
            default -> "&itemid=" + encode(queryParam(url, "id"));
        };
    }

    private JsonNode requestJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("复制商品失败，第三方接口状态：" + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new IllegalArgumentException("复制商品失败：" + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("复制商品失败：请求被中断");
        }
    }

    private String queryParam(String url, String key) {
        try {
            URI uri = URI.create(url);
            String query = uri.getRawQuery();
            if (query != null) {
                for (String part : query.split("&")) {
                    int equals = part.indexOf('=');
                    String name = equals >= 0 ? part.substring(0, equals) : part;
                    if (key.equals(name)) {
                        String value = equals >= 0 ? part.substring(equals + 1) : "";
                        if (StringUtils.hasText(value)) {
                            return value;
                        }
                    }
                }
            }
        } catch (RuntimeException ignored) {
        }
        throw new IllegalArgumentException(COPY_PLATFORM_NAMES.get(detectCopyPlatform(url)) + "商品链接缺少 " + key + " 参数");
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String removeSuffix(String value, String suffix) {
        if (value != null && value.endsWith(suffix)) {
            return value.substring(0, value.length() - suffix.length());
        }
        return value;
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node == null ? null : node.path(field);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return fallback;
        }
        return value.asText(fallback);
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.path(field);
        if (value == null || value.isMissingNode() || value.isNull() || !StringUtils.hasText(value.asText())) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.asText());
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private List<String> sliderImagesFromNode(JsonNode node, String fallback) {
        List<String> images = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                if (StringUtils.hasText(item.asText())) {
                    images.add(item.asText());
                }
            });
        } else if (node != null && node.isTextual()) {
            String value = node.asText();
            if (value.trim().startsWith("[")) {
                try {
                    JsonNode parsed = objectMapper.readTree(value);
                    if (parsed.isArray()) {
                        parsed.forEach(item -> {
                            if (StringUtils.hasText(item.asText())) {
                                images.add(item.asText());
                            }
                        });
                    }
                } catch (IOException ignored) {
                }
            }
            if (images.isEmpty()) {
                for (String image : value.split(",")) {
                    String clean = image.trim().replace("[", "").replace("]", "").replace("\"", "");
                    if (StringUtils.hasText(clean)) {
                        images.add(clean);
                    }
                }
            }
        }
        if (images.isEmpty() && StringUtils.hasText(fallback)) {
            images.add(fallback);
        }
        return images.stream().distinct().toList();
    }

    private String firstImage(JsonNode imagesNode, String fallback) {
        List<String> images = sliderImagesFromNode(imagesNode, fallback);
        return images.isEmpty() ? "" : images.get(0);
    }

    private void fillProductForSave(StoreProduct product, StoreProductBasicUpdateRequest request) {
        product.setStoreName(request.getStoreName().trim());
        product.setStoreInfo(request.getStoreInfo() == null ? "" : request.getStoreInfo().trim());
        product.setKeyword(request.getKeyword() == null ? "" : request.getKeyword().trim());
        product.setCateId(request.getCateId() == null ? "" : request.getCateId().trim());
        product.setImage(clearAssetPrefix(request.getImage()));
        product.setVideoLink(clearAssetPrefix(request.getVideoLink()));
        product.setSliderImage(toSliderImageJson(request.getSliderImages()));
        product.setPrice(nonNegative(request.getPrice(), product.getPrice() == null ? BigDecimal.ZERO : product.getPrice()));
        product.setVipPrice(nonNegative(request.getVipPrice(), product.getVipPrice() == null ? BigDecimal.ZERO : product.getVipPrice()));
        product.setOtPrice(nonNegative(request.getOtPrice(), product.getOtPrice() == null ? BigDecimal.ZERO : product.getOtPrice()));
        product.setCost(nonNegative(request.getCost(), product.getCost() == null ? BigDecimal.ZERO : product.getCost()));
        product.setUnitName(defaultText(request.getUnitName(), defaultText(product.getUnitName(), "件")));
        product.setGiveIntegral(request.getGiveIntegral() == null ? product.getGiveIntegral() : Math.max(request.getGiveIntegral(), 0));
        product.setTempId(request.getTempId() == null ? product.getTempId() : Math.max(request.getTempId(), 0));
        product.setSort(normalizeSort(request.getSort(), product.getSort(), product.getId() == null));
        product.setFicti(request.getFicti() == null ? product.getFicti() : Math.max(request.getFicti(), 0));
        product.setIsShow(booleanToInt(request.getIsShow(), product.getIsShow()));
        product.setIsSub(booleanToInt(request.getIsSub(), product.getIsSub()));
        product.setIsHot(booleanToInt(request.getIsHot(), product.getIsHot()));
        product.setIsBenefit(booleanToInt(request.getIsBenefit(), product.getIsBenefit()));
        product.setIsBest(booleanToInt(request.getIsBest(), product.getIsBest()));
        product.setIsNew(booleanToInt(request.getIsNew(), product.getIsNew()));
        product.setIsGood(booleanToInt(request.getIsGood(), product.getIsGood()));
        product.setActivity(toActivityString(request.getActivity(), product.getActivity()));
    }

    private String csvLine(List<String> values) {
        return values.stream().map(this::csvValue).collect(Collectors.joining(","));
    }

    private String csvValue(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private Integer normalizeSort(Integer value, Integer fallback, boolean createMode) {
        if (value == null || (createMode && value <= 0)) {
            return fallback == null || (createMode && fallback <= 0) ? DEFAULT_PRODUCT_SORT : Math.min(fallback, MAX_PRODUCT_SORT);
        }
        return Math.min(Math.max(value, 0), MAX_PRODUCT_SORT);
    }

    private BigDecimal nonNegative(BigDecimal value, BigDecimal fallback) {
        if (value == null) {
            return fallback;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金额不能小于0");
        }
        return value;
    }

    private Integer booleanToInt(Boolean value, Integer fallback) {
        if (value == null) {
            return fallback;
        }
        return value ? 1 : 0;
    }

    private String toSliderImageJson(List<String> sliderImages) {
        if (sliderImages == null || sliderImages.isEmpty()) {
            return "[]";
        }
        return sliderImages.stream()
                .filter(StringUtils::hasText)
                .map(this::clearAssetPrefix)
                .map(value -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String toActivityString(List<String> activity, String fallback) {
        if (activity == null || activity.isEmpty()) {
            return fallback;
        }
        List<String> values = new ArrayList<>();
        for (String item : activity) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            switch (item.trim()) {
                case "默认", "0" -> values.add("0");
                case "秒杀", "1" -> values.add("1");
                case "砍价", "2" -> values.add("2");
                case "拼团", "3" -> values.add("3");
                default -> {
                }
            }
        }
        if (values.isEmpty()) {
            return fallback;
        }
        return values.stream().distinct().collect(Collectors.joining(","));
    }

    private void upsertProductDescription(Integer productId, String content) {
        String description = content == null ? "" : content;
        if (descriptionMapper.countDescription(productId, 0) > 0) {
            descriptionMapper.updateDescription(productId, 0, description);
            return;
        }
        descriptionMapper.insertDescription(productId, 0, description);
    }

    private void updateProductCoupons(Integer productId, List<Integer> couponIds) {
        couponMapper.deleteByProductId(productId);
        int addTime = (int) (System.currentTimeMillis() / 1000);
        couponIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .forEach(id -> couponMapper.insertProductCoupon(productId, id, addTime));
    }

    private void ensureDefaultSku(StoreProduct product) {
        if (product.getId() == null) {
            return;
        }
        if (attrMapper.countByProductIdAndType(product.getId(), NORMAL_PRODUCT_ATTR_TYPE) <= 0) {
            attrMapper.insertAttr(product.getId(), DEFAULT_ATTR_NAME, DEFAULT_ATTR_VALUE, NORMAL_PRODUCT_ATTR_TYPE);
        }
        List<ProductAttrValueResponse> values = attrValueMapper.selectByProductIdAndType(product.getId(), NORMAL_PRODUCT_ATTR_TYPE);
        if (values.isEmpty()) {
            attrValueMapper.insertAttrValue(
                    product.getId(),
                    DEFAULT_ATTR_VALUE,
                    product.getStock() == null ? 0 : Math.max(product.getStock(), 0),
                    product.getPrice() == null ? BigDecimal.ZERO : product.getPrice(),
                    product.getImage(),
                    randomUnique(),
                    product.getCost() == null ? BigDecimal.ZERO : product.getCost(),
                    product.getBarCode() == null ? "" : product.getBarCode(),
                    product.getOtPrice() == null ? BigDecimal.ZERO : product.getOtPrice(),
                    NORMAL_PRODUCT_ATTR_TYPE,
                    DEFAULT_ATTR_VALUE_JSON);
        }
    }

    private void syncDefaultSku(StoreProduct product) {
        if (product.getId() == null || Integer.valueOf(1).equals(product.getSpecType())) {
            return;
        }
        ensureDefaultSku(product);
        List<ProductAttrValueResponse> values = attrValueMapper.selectByProductIdAndType(product.getId(), NORMAL_PRODUCT_ATTR_TYPE);
        if (values.size() != 1) {
            return;
        }
        ProductAttrValueResponse value = values.get(0);
        attrValueMapper.updateBasicFields(
                value.getId(),
                product.getId(),
                NORMAL_PRODUCT_ATTR_TYPE,
                product.getPrice() == null ? BigDecimal.ZERO : product.getPrice(),
                product.getImage(),
                product.getCost() == null ? BigDecimal.ZERO : product.getCost(),
                product.getOtPrice() == null ? BigDecimal.ZERO : product.getOtPrice(),
                product.getBarCode() == null ? "" : product.getBarCode());
    }

    private String randomUnique() {
        long value = ThreadLocalRandom.current().nextLong(0x100000000L);
        return Long.toHexString(value | 0x100000000L).substring(1, 9);
    }

    private String clearAssetPrefix(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("/crmebimage/")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }

    private StoreProduct getExistingProduct(Integer id) {
        StoreProduct product = storeProductMapper.selectById(id);
        if (product == null || Integer.valueOf(1).equals(product.getIsDel())) {
            throw new IllegalArgumentException("商品不存在");
        }
        return product;
    }

    private void assertNoActiveMarketing(Integer productId) {
        if (activityMapper.countActiveSeckill(productId) > 0) {
            throw new IllegalArgumentException("有商品关联的秒杀商品活动开启中，不能删除");
        }
        if (activityMapper.countActiveBargain(productId) > 0) {
            throw new IllegalArgumentException("有商品关联的砍价商品活动开启中，不能删除");
        }
        if (activityMapper.countActiveCombination(productId) > 0) {
            throw new IllegalArgumentException("有商品关联的拼团商品活动开启中，不能删除");
        }
    }

    private List<String> activityList(String activity) {
        if (!StringUtils.hasText(activity)) {
            return List.of("默认", "秒杀", "砍价", "拼团");
        }
        List<String> list = new ArrayList<>();
        for (String value : activity.split(",")) {
            String trimmed = value.trim();
            switch (trimmed) {
                case "0" -> list.add("默认");
                case "1" -> list.add("秒杀");
                case "2" -> list.add("砍价");
                case "3" -> list.add("拼团");
                default -> {
                }
            }
        }
        return list;
    }

    private QueryWrapper<StoreProduct> buildQuery(StoreProductSearchRequest request, boolean includeOrder) {
        QueryWrapper<StoreProduct> query = new QueryWrapper<>();
        switch (request.getType()) {
            case 1 -> query.eq("is_show", 1).eq("is_recycle", 0).eq("is_del", 0);
            case 2 -> query.eq("is_show", 0).eq("is_recycle", 0).eq("is_del", 0);
            case 3 -> query.le("stock", 0).eq("is_recycle", 0).eq("is_del", 0);
            case 4 -> query.le("stock", warningStock()).eq("is_recycle", 0).eq("is_del", 0);
            case 5 -> query.eq("is_recycle", 1).eq("is_del", 0);
            default -> query.eq("is_recycle", 0).eq("is_del", 0);
        }
        if (StringUtils.hasText(request.getKeywords())) {
            String keyword = request.getKeywords().trim();
            query.and(wrapper -> wrapper
                    .eq("id", keyword)
                    .or().like("store_name", keyword)
                    .or().like("keyword", keyword));
        }
        if (StringUtils.hasText(request.getCateId())) {
            String[] cateIds = request.getCateId().split(",");
            query.and(wrapper -> {
                boolean first = true;
                for (String cateId : cateIds) {
                    if (StringUtils.hasText(cateId)) {
                        if (first) {
                            wrapper.apply("FIND_IN_SET({0}, cate_id)", cateId.trim());
                            first = false;
                        } else {
                            wrapper.or().apply("FIND_IN_SET({0}, cate_id)", cateId.trim());
                        }
                    }
                }
            });
        }
        if (includeOrder) {
            if ("desc".equalsIgnoreCase(request.getSalesOrder())) {
                query.last("order by (sales + ficti) desc, sort desc, id desc");
            } else if ("asc".equalsIgnoreCase(request.getSalesOrder())) {
                query.last("order by (sales + ficti) asc, sort asc, id asc");
            } else if ("desc".equalsIgnoreCase(request.getPriceOrder())) {
                query.orderByDesc("price").orderByDesc("sort").orderByDesc("id");
            } else if ("asc".equalsIgnoreCase(request.getPriceOrder())) {
                query.orderByAsc("price").orderByDesc("sort").orderByDesc("id");
            } else {
                query.orderByDesc("sort").orderByDesc("id");
            }
        }
        return query;
    }

    private int warningStock() {
        String value = configMapper.selectConfigValue("store_stock");
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    private StoreProductResponse toResponse(StoreProduct product) {
        StoreProductResponse response = new StoreProductResponse();
        fillProductResponse(product, response);
        return response;
    }

    private void fillProductResponse(StoreProduct product, StoreProductResponse response) {
        response.setId(product.getId());
        response.setImage(normalizeAsset(product.getImage()));
        response.setStoreName(product.getStoreName());
        response.setKeyword(product.getKeyword());
        response.setBarCode(product.getBarCode());
        response.setCateId(product.getCateId());
        response.setCateValues(catalogService.getCategoryNames(product.getCateId()));
        response.setPrice(product.getPrice());
        response.setVipPrice(product.getVipPrice());
        response.setOtPrice(product.getOtPrice());
        response.setSort(product.getSort());
        response.setSales(product.getSales());
        response.setStock(product.getStock());
        response.setIsShow(Integer.valueOf(1).equals(product.getIsShow()));
        response.setIsHot(Integer.valueOf(1).equals(product.getIsHot()));
        response.setIsBenefit(Integer.valueOf(1).equals(product.getIsBenefit()));
        response.setIsBest(Integer.valueOf(1).equals(product.getIsBest()));
        response.setIsNew(Integer.valueOf(1).equals(product.getIsNew()));
        response.setAddTime(product.getAddTime());
        response.setIsPostage(Integer.valueOf(1).equals(product.getIsPostage()));
        response.setIsDel(Integer.valueOf(1).equals(product.getIsDel()));
        response.setCost(product.getCost());
        response.setFicti(product.getFicti());
        response.setBrowse(product.getBrowse());
        response.setCollectCount(relationMapper.countByProductIdAndType(product.getId(), "collect"));
    }

    private String normalizeAsset(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/")) {
            return value;
        }
        return "/" + value;
    }
}
