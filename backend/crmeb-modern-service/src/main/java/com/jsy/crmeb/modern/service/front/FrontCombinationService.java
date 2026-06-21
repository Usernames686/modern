package com.jsy.crmeb.modern.service.front;

import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.front.mapper.FrontCombinationMapper;
import com.jsy.crmeb.modern.service.product.dto.ProductAttrValueResponse;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductAttrValueMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductDescriptionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class FrontCombinationService {
    private static final int PRODUCT_TYPE_COMBINATION = 3;

    private final FrontCombinationMapper combinationMapper;
    private final StoreProductAttrValueMapper attrValueMapper;
    private final StoreProductDescriptionMapper descriptionMapper;
    private final FrontUserCenterService userCenterService;

    public FrontCombinationService(
            FrontCombinationMapper combinationMapper,
            StoreProductAttrValueMapper attrValueMapper,
            StoreProductDescriptionMapper descriptionMapper,
            FrontUserCenterService userCenterService) {
        this.combinationMapper = combinationMapper;
        this.attrValueMapper = attrValueMapper;
        this.descriptionMapper = descriptionMapper;
        this.userCenterService = userCenterService;
    }

    public Map<String, Object> index() {
        long now = System.currentTimeMillis();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("avatarList", combinationMapper.selectRecentPinkAvatars(3));
        response.put("totalPeople", nvl(combinationMapper.countTotalPeople()));
        response.put("productList", combinationMapper.selectVisible(now, null, 6, 0).stream()
                .map(this::toListItem)
                .toList());
        return response;
    }

    public Map<String, Object> header() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("title", "多人拼团更优惠");
        response.put("countPeople", nvl(combinationMapper.countTotalPeople()));
        response.put("countPeopleAll", nvl(combinationMapper.countTotalPeople()));
        response.put("avatarList", combinationMapper.selectRecentPinkAvatars(3));
        return response;
    }

    public PageResponse<Map<String, Object>> list(int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        long now = System.currentTimeMillis();
        List<Map<String, Object>> rows = combinationMapper
                .selectVisible(now, null, safeLimit, (safePage - 1) * safeLimit)
                .stream()
                .map(this::toListItem)
                .toList();
        return new PageResponse<>(safePage, safeLimit, combinationMapper.countVisible(now), rows);
    }

    public Map<String, Object> detail(Integer id) {
        return detail(id, null);
    }

    public Map<String, Object> detail(Integer id, Integer uid) {
        Map<String, Object> row = checkedCombination(id);
        combinationMapper.incrementBrowse(id);
        if (uid != null) {
            userCenterService.recordVisit(uid, 3);
        }
        Map<String, Object> detail = toListItem(row);
        detail.put("content", defaultString(descriptionMapper.selectDescription(id, PRODUCT_TYPE_COMBINATION)));
        detail.put("productAttr", combinationMapper.selectAttrs(id));

        List<ProductAttrValueResponse> values = attrValueMapper.selectByProductIdAndType(id, PRODUCT_TYPE_COMBINATION);
        detail.put("productValue", values.stream()
                .collect(Collectors.toMap(
                        value -> defaultString(value.getSuk(), "默认"),
                        this::attrValueItem,
                        (left, right) -> left,
                        LinkedHashMap::new)));
        if (!values.isEmpty()) {
            detail.put("attrValueId", values.get(0).getId());
            detail.put("sku", values.get(0).getSuk());
        }

        List<Map<String, Object>> pinkHeads = combinationMapper.selectPinkHeads(id, 10).stream()
                .map(this::pinkItem)
                .toList();
        detail.put("pinkList", pinkHeads.stream()
                .filter(item -> numberValue(item.get("status")) == 1 && longValue(item.get("stopTime")) > System.currentTimeMillis())
                .toList());
        detail.put("pinkOkList", pinkHeads.stream()
                .filter(item -> numberValue(item.get("status")) == 2)
                .toList());
        detail.put("pinkOkSum", detail.get("countPeoplePink"));
        detail.put("storeCombination", new LinkedHashMap<>(detail));
        return detail;
    }

    public Map<String, Object> pink(Integer uid, Integer pinkId) {
        Map<String, Object> pink = combinationMapper.selectPinkById(pinkId);
        if (pink == null || numberValue(pink.get("isRefund")) > 0) {
            throw new IllegalArgumentException("对应的拼团不存在");
        }
        Integer combinationId = numberValue(pink.get("cid"));
        Map<String, Object> combination = checkedCombination(combinationId);
        List<Map<String, Object>> members = combinationMapper
                .selectPinkMembers(numberValue(pink.get("kId")) > 0 ? numberValue(pink.get("kId")) : pinkId)
                .stream()
                .map(this::pinkItem)
                .toList();
        int count = Math.max(0, numberValue(pink.get("people")) - members.size());
        boolean userInPink = uid != null && members.stream().anyMatch(item -> uid.equals(numberValue(item.get("uid"))));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count", count);
        response.put("isOk", numberValue(pink.get("status")) == 2 ? 1 : 0);
        response.put("pinkBool", numberValue(pink.get("status")) == 2 ? 1 : 0);
        response.put("userBool", userInPink ? 1 : 0);
        response.put("pinkT", pinkItem(members.stream().filter(item -> numberValue(item.get("kId")) == 0).findFirst().orElse(pink)));
        response.put("pinkAll", members.stream().filter(item -> numberValue(item.get("kId")) > 0).toList());
        response.put("storeCombination", toListItem(combination));
        return response;
    }

    public PageResponse<Map<String, Object>> more(Integer comId, int page, int limit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        long now = System.currentTimeMillis();
        List<Map<String, Object>> rows = combinationMapper
                .selectVisible(now, comId, safeLimit, (safePage - 1) * safeLimit)
                .stream()
                .map(this::toListItem)
                .toList();
        return new PageResponse<>(safePage, safeLimit, rows.size(), rows);
    }

    public boolean remove(Integer uid, Map<String, Object> body) {
        throw new IllegalArgumentException("取消拼团请在订单售后中处理");
    }

    private Map<String, Object> checkedCombination(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("拼团商品id不能为空");
        }
        Map<String, Object> row = combinationMapper.selectVisibleById(id);
        if (row == null) {
            throw new IllegalArgumentException("拼团商品不存在或已下架");
        }
        return row;
    }

    private Map<String, Object> toListItem(Map<String, Object> source) {
        Map<String, Object> row = new LinkedHashMap<>(source);
        row.put("storeName", defaultString(firstPresent(source.get("storeName"), source.get("title"))));
        row.put("title", defaultString(source.get("title")));
        row.put("price", money(source.get("price")));
        row.put("otPrice", money(source.get("otPrice")));
        row.put("cost", money(source.get("cost")));
        row.put("postage", money(source.get("postage")));
        row.put("quotaPercent", percent(numberValue(source.get("quotaShow")), numberValue(source.get("quota"))));
        row.put("percent", row.get("quotaPercent"));
        row.put("sliderImage", defaultString(firstPresent(source.get("images"), source.get("image"))));
        row.put("unitName", defaultString(source.get("unitName"), "件"));
        return row;
    }

    private Map<String, Object> pinkItem(Map<String, Object> source) {
        Map<String, Object> row = new LinkedHashMap<>(source);
        row.put("isLeader", numberValue(source.get("kId")) == 0);
        row.put("count", Math.max(0, numberValue(source.get("people")) - numberValue(source.get("countPeople"))));
        return row;
    }

    private Map<String, Object> attrValueItem(ProductAttrValueResponse value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", value.getId());
        row.put("productId", value.getProductId());
        row.put("suk", value.getSuk());
        row.put("stock", value.getStock());
        row.put("sales", value.getSales());
        row.put("price", money(value.getPrice()));
        row.put("image", value.getImage());
        row.put("cost", money(value.getCost()));
        row.put("otPrice", money(value.getOtPrice()));
        row.put("weight", money(value.getWeight()));
        row.put("volume", money(value.getVolume()));
        row.put("quota", value.getQuota());
        row.put("quotaShow", value.getQuotaShow());
        row.put("attrValue", value.getAttrValue());
        row.put("barCode", value.getBarCode());
        return row;
    }

    private int percent(int total, int left) {
        if (total <= 0) {
            return 0;
        }
        int sold = Math.max(0, total - left);
        return BigDecimal.valueOf(sold)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private String money(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        if (value != null && !String.valueOf(value).isBlank()) {
            return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        return "0.00";
    }

    private int numberValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null && !String.valueOf(value).isBlank()) {
            return Integer.parseInt(String.valueOf(value));
        }
        return 0;
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null && !String.valueOf(value).isBlank()) {
            return Long.parseLong(String.valueOf(value));
        }
        return 0L;
    }

    private Integer nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private Object firstPresent(Object first, Object second) {
        return first == null ? second : first;
    }

    private String defaultString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String defaultString(Object value, String fallback) {
        String text = defaultString(value);
        return text.isBlank() ? fallback : text;
    }
}
