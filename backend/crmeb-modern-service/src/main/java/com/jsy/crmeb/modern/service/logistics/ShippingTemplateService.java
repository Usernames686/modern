package com.jsy.crmeb.modern.service.logistics;

import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.logistics.dto.ExpressUpdateRequest;
import com.jsy.crmeb.modern.service.logistics.dto.ShippingTemplateFreeRequest;
import com.jsy.crmeb.modern.service.logistics.dto.ShippingTemplateFreeResponse;
import com.jsy.crmeb.modern.service.logistics.dto.ShippingTemplateInfoResponse;
import com.jsy.crmeb.modern.service.logistics.dto.ShippingTemplateRegionRequest;
import com.jsy.crmeb.modern.service.logistics.dto.ShippingTemplateRegionResponse;
import com.jsy.crmeb.modern.service.logistics.dto.ShippingTemplateRequest;
import com.jsy.crmeb.modern.service.logistics.dto.SystemCityTreeResponse;
import com.jsy.crmeb.modern.service.logistics.entity.ShippingTemplate;
import com.jsy.crmeb.modern.service.logistics.mapper.ShippingTemplateMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ShippingTemplateService {
    private final ShippingTemplateMapper shippingTemplateMapper;

    public ShippingTemplateService(ShippingTemplateMapper shippingTemplateMapper) {
        this.shippingTemplateMapper = shippingTemplateMapper;
    }

    public PageResponse<ShippingTemplate> list(Integer page, Integer limit, String keywords) {
        int safePage = page == null || page <= 0 ? 1 : page;
        int safeLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 9999);
        String cleanKeywords = cleanKeywords(keywords);
        long total = shippingTemplateMapper.countAll(cleanKeywords);
        List<ShippingTemplate> list = shippingTemplateMapper.selectPage(cleanKeywords, (safePage - 1) * safeLimit, safeLimit);
        return new PageResponse<>(safePage, safeLimit, total, list);
    }

    public PageResponse<Map<String, Object>> expressList(Integer page, Integer limit, String keywords, Integer isShow) {
        int safePage = page == null || page <= 0 ? 1 : page;
        int safeLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 9999);
        String cleanKeywords = cleanKeywords(keywords);
        long total = shippingTemplateMapper.countExpress(cleanKeywords, isShow);
        List<Map<String, Object>> list = shippingTemplateMapper.selectExpressPage(cleanKeywords, isShow, (safePage - 1) * safeLimit, safeLimit);
        return new PageResponse<>(safePage, safeLimit, total, list);
    }

    public Map<String, Object> expressInfo(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("快递公司id不能为空");
        }
        Map<String, Object> express = shippingTemplateMapper.selectExpressById(id);
        if (express == null) {
            throw new IllegalArgumentException("快递公司不存在");
        }
        return express;
    }

    public Boolean updateExpress(ExpressUpdateRequest request) {
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("快递公司id不能为空");
        }
        Map<String, Object> express = expressInfo(request.getId());
        if (boolValue(express.get("partnerId")) && !StringUtils.hasText(request.getAccount())) {
            throw new IllegalArgumentException("请输入月结账号");
        }
        if (boolValue(express.get("partnerKey")) && !StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("请输入月结密码");
        }
        if (boolValue(express.get("net")) && !StringUtils.hasText(request.getNetName())) {
            throw new IllegalArgumentException("请输入取件网点");
        }
        if (request.getSort() == null || request.getSort() < 0) {
            throw new IllegalArgumentException("排序不能为空");
        }
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("是否可用不能为空");
        }
        return shippingTemplateMapper.updateExpress(
                request.getId(),
                emptyToBlank(request.getAccount()),
                emptyToBlank(request.getPassword()),
                emptyToBlank(request.getNetName()),
                request.getSort(),
                request.getStatus()) > 0;
    }

    public Boolean updateExpressShow(ExpressUpdateRequest request) {
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("快递公司id不能为空");
        }
        if (request.getIsShow() == null) {
            throw new IllegalArgumentException("是否显示不能为空");
        }
        expressInfo(request.getId());
        return shippingTemplateMapper.updateExpressShow(request.getId(), request.getIsShow()) > 0;
    }

    public Map<String, Object> syncExpress() {
        // The old project syncs from OnePass. Keep the entry available locally, but
        // do not contact the third-party platform without real deployment config.
        long total = shippingTemplateMapper.countExpressAll();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("localMode", true);
        result.put("synced", false);
        result.put("total", total);
        result.put("message", "OnePass 同步未配置，当前读取系统快递公司共 " + total + " 条记录。");
        return result;
    }

    public List<SystemCityTreeResponse> cityTree() {
        List<SystemCityTreeResponse> all = shippingTemplateMapper.selectCityTree();
        Map<Integer, SystemCityTreeResponse> map = new LinkedHashMap<>();
        for (SystemCityTreeResponse item : all) {
            item.setChild(new ArrayList<>());
            map.put(item.getCityId(), item);
        }
        List<SystemCityTreeResponse> roots = new ArrayList<>();
        for (SystemCityTreeResponse item : all) {
            SystemCityTreeResponse parent = map.get(item.getParentId());
            if (parent != null) {
                parent.getChild().add(item);
            } else {
                roots.add(item);
            }
        }
        return roots;
    }

    public List<SystemCityTreeResponse> cityList(Integer parentId) {
        Integer safeParentId = parentId == null ? 0 : parentId;
        return shippingTemplateMapper.selectCityList(safeParentId);
    }

    public SystemCityTreeResponse cityInfo(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("城市id不能为空");
        }
        SystemCityTreeResponse city = shippingTemplateMapper.selectCityById(id);
        if (city == null) {
            throw new IllegalArgumentException("城市不存在");
        }
        return city;
    }

    public Boolean updateCity(Integer id, String name) {
        if (id == null) {
            throw new IllegalArgumentException("城市id不能为空");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("城市名称不能为空");
        }
        cityInfo(id);
        return shippingTemplateMapper.updateCityName(id, name.trim()) > 0;
    }

    public ShippingTemplateInfoResponse info(Integer id) {
        ShippingTemplate template = shippingTemplateMapper.selectById(id);
        if (template == null) {
            throw new IllegalArgumentException("运费模板不存在");
        }
        ShippingTemplateInfoResponse response = new ShippingTemplateInfoResponse();
        response.setId(template.getId());
        response.setName(template.getName());
        response.setType(template.getType());
        response.setAppoint(template.getAppoint());
        response.setSort(template.getSort());
        if (Objects.equals(template.getAppoint(), 0)) {
            return response;
        }
        response.setRegionList(shippingTemplateMapper.selectRegionGroup(id));
        if (Objects.equals(template.getAppoint(), 2)) {
            response.setFreeList(shippingTemplateMapper.selectFreeGroup(id));
        }
        return response;
    }

    @Transactional
    public Boolean save(ShippingTemplateRequest request) {
        validateRequest(request, null);
        if (shippingTemplateMapper.existsByName(request.getName(), null)) {
            throw new IllegalArgumentException("模板名称已存在,请更换模板名称!");
        }
        ShippingTemplate template = new ShippingTemplate();
        template.setName(request.getName().trim());
        template.setType(request.getAppoint() != null && request.getAppoint() == 0 ? 0 : request.getType());
        template.setAppoint(request.getAppoint());
        template.setSort(request.getSort());
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        shippingTemplateMapper.insertTemplate(template);
        saveChildren(template.getId(), request);
        return Boolean.TRUE;
    }

    @Transactional
    public Boolean update(Integer id, ShippingTemplateRequest request) {
        ShippingTemplate template = shippingTemplateMapper.selectById(id);
        if (template == null) {
            throw new IllegalArgumentException("运费模板不存在");
        }
        validateRequest(request, id);
        if (shippingTemplateMapper.existsByName(request.getName(), id)) {
            throw new IllegalArgumentException("模板名称已存在,请更换模板名称!");
        }
        shippingTemplateMapper.deleteChildrenByTempId(id);
        template.setName(request.getName().trim());
        template.setType(request.getAppoint() != null && request.getAppoint() == 0 ? 0 : request.getType());
        template.setAppoint(request.getAppoint());
        template.setSort(request.getSort());
        template.setUpdateTime(LocalDateTime.now());
        shippingTemplateMapper.updateTemplate(template);
        saveChildren(id, request);
        return Boolean.TRUE;
    }

    @Transactional
    public Boolean delete(Integer id) {
        if (shippingTemplateMapper.isTemplateUsed(id) > 0) {
            throw new IllegalArgumentException("有商品使用此运费模板，无法删除");
        }
        shippingTemplateMapper.deleteChildrenByTempId(id);
        return shippingTemplateMapper.deleteTemplate(id) > 0;
    }

    public List<ShippingTemplateRegionResponse> regionGroup(Integer tempId) {
        return shippingTemplateMapper.selectRegionGroup(tempId);
    }

    public List<ShippingTemplateFreeResponse> freeGroup(Integer tempId) {
        return shippingTemplateMapper.selectFreeGroup(tempId);
    }

    private void saveChildren(Integer tempId, ShippingTemplateRequest request) {
        Integer appoint = request.getAppoint();
        List<ShippingTemplateRegionRequest> regionRequests = request.getShippingTemplatesRegionRequestList();
        if (!Objects.equals(appoint, 0) && regionRequests != null && !regionRequests.isEmpty()) {
            shippingTemplateMapper.insertRegionBatch(flattenRegions(tempId, request.getType(), regionRequests));
        }
        List<ShippingTemplateFreeRequest> freeRequests = request.getShippingTemplatesFreeRequestList();
        if (Objects.equals(appoint, 2) && freeRequests != null && !freeRequests.isEmpty()) {
            shippingTemplateMapper.insertFreeBatch(flattenFree(tempId, request.getType(), freeRequests));
        }
    }

    private List<Map<String, Object>> flattenRegions(Integer tempId, Integer type, List<ShippingTemplateRegionRequest> requests) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ShippingTemplateRegionRequest request : requests) {
            String uniqid = request.getUniqid() != null && !request.getUniqid().isBlank()
                    ? request.getUniqid()
                    : Integer.toHexString(Objects.hash(request.getCityId(), request.getTitle(), request.getFirst(), request.getFirstPrice(), request.getRenewal(), request.getRenewalPrice()));
            String cityId = request.getCityId();
            String title = request.getTitle();
            List<String> titles = parseTitleList(title);
            if ("all".equals(cityId) || "0".equals(cityId) || cityId == null || cityId.isBlank()) {
                rows.add(regionRow(tempId, type, 0, titles.isEmpty() ? "[]" : titles.get(0), uniqid, request));
            } else {
                List<Integer> cityIds = parseCityIds(cityId);
                Map<Integer, String> titleMap = new LinkedHashMap<>();
                for (String item : titles) {
                    List<Integer> parsed = parseTitlePath(item);
                    if (!parsed.isEmpty()) {
                        titleMap.put(parsed.get(parsed.size() - 1), item);
                    }
                }
                for (Integer city : cityIds) {
                    rows.add(regionRow(tempId, type, city, titleMap.getOrDefault(city, city.toString()), uniqid, request));
                }
            }
        }
        return rows;
    }

    private List<Map<String, Object>> flattenFree(Integer tempId, Integer type, List<ShippingTemplateFreeRequest> requests) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ShippingTemplateFreeRequest request : requests) {
            String uniqid = request.getUniqid() != null && !request.getUniqid().isBlank()
                    ? request.getUniqid()
                    : Integer.toHexString(Objects.hash(request.getCityId(), request.getTitle(), request.getNumber(), request.getPrice()));
            String cityId = request.getCityId();
            String title = request.getTitle();
            List<String> titles = parseTitleList(title);
            if ("all".equals(cityId) || "0".equals(cityId) || cityId == null || cityId.isBlank()) {
                rows.add(freeRow(tempId, type, 0, titles.isEmpty() ? "[]" : titles.get(0), uniqid, request));
            } else {
                List<Integer> cityIds = parseCityIds(cityId);
                Map<Integer, String> titleMap = new LinkedHashMap<>();
                for (String item : titles) {
                    List<Integer> parsed = parseTitlePath(item);
                    if (!parsed.isEmpty()) {
                        titleMap.put(parsed.get(parsed.size() - 1), item);
                    }
                }
                for (Integer city : cityIds) {
                    rows.add(freeRow(tempId, type, city, titleMap.getOrDefault(city, city.toString()), uniqid, request));
                }
            }
        }
        return rows;
    }

    private Map<String, Object> regionRow(Integer tempId, Integer type, Integer cityId, String title, String uniqid, ShippingTemplateRegionRequest request) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tempId", tempId);
        row.put("cityId", cityId);
        row.put("title", title);
        row.put("first", request.getFirst());
        row.put("firstPrice", request.getFirstPrice());
        row.put("renewal", request.getRenewal());
        row.put("renewalPrice", request.getRenewalPrice());
        row.put("type", type);
        row.put("uniqid", uniqid);
        row.put("status", Boolean.TRUE);
        return row;
    }

    private Map<String, Object> freeRow(Integer tempId, Integer type, Integer cityId, String title, String uniqid, ShippingTemplateFreeRequest request) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tempId", tempId);
        row.put("cityId", cityId);
        row.put("title", title);
        row.put("number", request.getNumber());
        row.put("price", request.getPrice());
        row.put("type", type);
        row.put("uniqid", uniqid);
        row.put("status", Boolean.TRUE);
        return row;
    }

    private List<String> parseTitleList(String title) {
        if (!StringUtils.hasText(title)) {
            return List.of();
        }
        String clean = title.trim();
        if (clean.startsWith("[") && clean.endsWith("]")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        if (!StringUtils.hasText(clean)) {
            return List.of();
        }
        String[] items = clean.split("\\],\\[");
        List<String> list = new ArrayList<>();
        for (String item : items) {
            String normalized = item.replace("[", "").replace("]", "").trim();
            if (!normalized.isBlank()) {
                list.add("[" + normalized + "]");
            }
        }
        return list;
    }

    private List<Integer> parseCityIds(String cityId) {
        List<Integer> ids = new ArrayList<>();
        for (String item : cityId.split(",")) {
            if (!item.isBlank()) {
                ids.add(Integer.parseInt(item.trim()));
            }
        }
        return ids;
    }

    private List<Integer> parseTitlePath(String title) {
        String clean = title.replace("[", "").replace("]", "");
        List<Integer> ids = new ArrayList<>();
        for (String item : clean.split(",")) {
            if (!item.isBlank()) {
                ids.add(Integer.parseInt(item.trim()));
            }
        }
        return ids;
    }

    private void validateRequest(ShippingTemplateRequest request, Integer currentId) {
        if (request == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("模板名称必须填写");
        }
        if (request.getType() == null || request.getType() < 0 || request.getType() > 3) {
            throw new IllegalArgumentException("计费方式必须选择");
        }
        if (request.getAppoint() == null || request.getAppoint() < 0 || request.getAppoint() > 2) {
            throw new IllegalArgumentException("指定包邮必须选择");
        }
        if (request.getSort() == null || request.getSort() < 0) {
            throw new IllegalArgumentException("排序数字必须填写");
        }
        if (Objects.equals(request.getAppoint(), 2)) {
            List<ShippingTemplateRegionRequest> regions = request.getShippingTemplatesRegionRequestList();
            if (regions == null || regions.isEmpty()) {
                throw new IllegalArgumentException("不包邮，最少需要一条公共区域运费数据");
            }
        }
    }

    private String cleanKeywords(String keywords) {
        return StringUtils.hasText(keywords) ? keywords.trim() : null;
    }

    private String emptyToBlank(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private boolean boolValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
