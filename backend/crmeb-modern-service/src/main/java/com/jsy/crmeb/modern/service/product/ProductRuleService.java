package com.jsy.crmeb.modern.service.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.product.dto.StoreProductRuleRequest;
import com.jsy.crmeb.modern.service.product.entity.StoreProductRule;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductRuleMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductRuleService {
    private final StoreProductRuleMapper ruleMapper;

    public ProductRuleService(StoreProductRuleMapper ruleMapper) {
        this.ruleMapper = ruleMapper;
    }

    public PageResponse<StoreProductRule> list(int page, int limit, String keywords) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        Page<StoreProductRule> rulePage = ruleMapper.selectPage(new Page<>(safePage, safeLimit), buildListQuery(keywords));
        return new PageResponse<>(safePage, safeLimit, rulePage.getTotal(), rulePage.getRecords());
    }

    public StoreProductRule info(Integer id) {
        validateId(id);
        StoreProductRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new IllegalArgumentException("商品规格不存在");
        }
        return rule;
    }

    @Transactional
    public boolean save(StoreProductRuleRequest request) {
        validateRequest(request, false);
        if (existsByRuleName(request.getRuleName(), null)) {
            throw new IllegalArgumentException("此规格值已经存在");
        }
        StoreProductRule rule = fromRequest(request);
        return ruleMapper.insert(rule) > 0;
    }

    @Transactional
    public boolean update(StoreProductRuleRequest request) {
        validateRequest(request, true);
        info(request.getId());
        StoreProductRule rule = fromRequest(request);
        rule.setId(request.getId());
        return ruleMapper.updateById(rule) > 0;
    }

    @Transactional
    public boolean delete(String ids) {
        List<Integer> idList = idsFromString(ids);
        if (idList.isEmpty()) {
            throw new IllegalArgumentException("请选择商品规格");
        }
        return ruleMapper.deleteBatchIds(idList) > 0;
    }

    private QueryWrapper<StoreProductRule> buildListQuery(String keywords) {
        QueryWrapper<StoreProductRule> query = new QueryWrapper<>();
        if (StringUtils.hasText(keywords)) {
            String trimmed = keywords.trim();
            query.and(item -> item.like("rule_name", trimmed).or().like("rule_value", trimmed));
        }
        query.orderByDesc("id");
        return query;
    }

    private StoreProductRule fromRequest(StoreProductRuleRequest request) {
        StoreProductRule rule = new StoreProductRule();
        rule.setRuleName(request.getRuleName().trim());
        rule.setRuleValue(request.getRuleValue().trim());
        return rule;
    }

    private void validateRequest(StoreProductRuleRequest request, boolean update) {
        if (request == null) {
            throw new IllegalArgumentException("参数错误");
        }
        if (update) {
            validateId(request.getId());
        }
        if (!StringUtils.hasText(request.getRuleName())) {
            throw new IllegalArgumentException("规格名称不能为空");
        }
        if (request.getRuleName().trim().length() > 32) {
            throw new IllegalArgumentException("规格名称长度不能超过32个字符");
        }
        if (!StringUtils.hasText(request.getRuleValue())) {
            throw new IllegalArgumentException("规格值不能为空");
        }
    }

    private boolean existsByRuleName(String ruleName, Integer ignoreId) {
        QueryWrapper<StoreProductRule> query = new QueryWrapper<>();
        query.eq("rule_name", ruleName.trim());
        if (ignoreId != null) {
            query.ne("id", ignoreId);
        }
        return ruleMapper.selectCount(query) > 0;
    }

    private void validateId(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id 参数不合法");
        }
    }

    private List<Integer> idsFromString(String ids) {
        List<Integer> idList = new ArrayList<>();
        if (!StringUtils.hasText(ids)) {
            return idList;
        }
        for (String item : ids.split(",")) {
            if (StringUtils.hasText(item)) {
                idList.add(Integer.valueOf(item.trim()));
            }
        }
        return idList;
    }
}
