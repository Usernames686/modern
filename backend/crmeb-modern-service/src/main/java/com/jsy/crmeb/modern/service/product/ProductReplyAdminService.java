package com.jsy.crmeb.modern.service.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.product.dto.StoreProductReplyCommentRequest;
import com.jsy.crmeb.modern.service.product.dto.StoreProductReplyRequest;
import com.jsy.crmeb.modern.service.product.dto.StoreProductReplyResponse;
import com.jsy.crmeb.modern.service.product.entity.StoreProduct;
import com.jsy.crmeb.modern.service.product.entity.StoreProductReply;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductMapper;
import com.jsy.crmeb.modern.service.product.mapper.StoreProductReplyMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductReplyAdminService {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final StoreProductReplyMapper replyMapper;
    private final StoreProductMapper productMapper;
    private final ObjectMapper objectMapper;

    public ProductReplyAdminService(
            StoreProductReplyMapper replyMapper,
            StoreProductMapper productMapper,
            ObjectMapper objectMapper) {
        this.replyMapper = replyMapper;
        this.productMapper = productMapper;
        this.objectMapper = objectMapper;
    }

    public PageResponse<StoreProductReplyResponse> list(
            int page,
            int limit,
            String isReply,
            String productSearch,
            String nickname,
            String dateLimit) {
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        Page<StoreProductReply> replyPage = replyMapper.selectPage(new Page<>(safePage, safeLimit),
                buildListQuery(isReply, productSearch, nickname, dateLimit));
        List<StoreProductReplyResponse> records = replyPage.getRecords().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<>(safePage, safeLimit, replyPage.getTotal(), records);
    }

    public StoreProductReply info(Integer id) {
        validateId(id);
        StoreProductReply reply = replyMapper.selectById(id);
        if (reply == null) {
            throw new IllegalArgumentException("商品评论不存在");
        }
        return reply;
    }

    @Transactional
    public boolean virtualCreate(StoreProductReplyRequest request) {
        validateReplyRequest(request);
        StoreProduct product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("请选择商品");
        }
        StoreProductReply reply = new StoreProductReply();
        reply.setUid(0);
        reply.setOid(0);
        reply.setProductId(request.getProductId());
        reply.setUnique(StringUtils.hasText(request.getUnique()) ? request.getUnique().trim() : String.valueOf(randomUnique()));
        reply.setReplyType("product");
        reply.setProductScore(request.getProductScore());
        reply.setServiceScore(request.getServiceScore());
        reply.setComment(request.getComment().trim());
        reply.setPics(normalizePics(request.getPics()));
        reply.setMerchantReplyContent(null);
        reply.setMerchantReplyTime(null);
        reply.setIsDel(0);
        reply.setIsReply(0);
        reply.setNickname(request.getNickname().trim());
        reply.setAvatar(clearImagePrefix(request.getAvatar()));
        reply.setSku(valueOrEmpty(request.getSku()));
        reply.setCreateTime(LocalDateTime.now(SHANGHAI));
        reply.setUpdateTime(LocalDateTime.now(SHANGHAI));
        return replyMapper.insert(reply) > 0;
    }

    @Transactional
    public boolean delete(Integer id) {
        info(id);
        StoreProductReply reply = new StoreProductReply();
        reply.setId(id);
        reply.setIsDel(1);
        reply.setUpdateTime(LocalDateTime.now(SHANGHAI));
        return replyMapper.updateById(reply) > 0;
    }

    @Transactional
    public boolean comment(StoreProductReplyCommentRequest request) {
        if (request == null || request.getIds() == null || request.getIds() <= 0) {
            throw new IllegalArgumentException("评论id不能为空");
        }
        if (!StringUtils.hasText(request.getMerchantReplyContent())) {
            throw new IllegalArgumentException("请填写评论内容");
        }
        info(request.getIds());
        StoreProductReply reply = new StoreProductReply();
        reply.setId(request.getIds());
        reply.setMerchantReplyContent(request.getMerchantReplyContent().trim());
        reply.setMerchantReplyTime((int) (System.currentTimeMillis() / 1000));
        reply.setIsReply(1);
        reply.setUpdateTime(LocalDateTime.now(SHANGHAI));
        return replyMapper.updateById(reply) > 0;
    }

    private QueryWrapper<StoreProductReply> buildListQuery(
            String isReply,
            String productSearch,
            String nickname,
            String dateLimit) {
        QueryWrapper<StoreProductReply> query = new QueryWrapper<>();
        query.eq("is_del", 0);
        Integer isReplyValue = intValue(isReply);
        if (isReplyValue != null) {
            query.eq("is_reply", isReplyValue);
        }
        if (StringUtils.hasText(productSearch)) {
            QueryWrapper<StoreProduct> productQuery = new QueryWrapper<>();
            productQuery.select("id");
            String search = productSearch.trim();
            productQuery.like("store_name", search);
            Integer id = intValue(search);
            if (id != null) {
                productQuery.or().eq("id", id);
            }
            List<Integer> ids = productMapper.selectList(productQuery).stream().map(StoreProduct::getId).toList();
            if (ids.isEmpty()) {
                query.eq("product_id", -1);
            } else {
                query.in("product_id", ids);
            }
        }
        if (StringUtils.hasText(nickname)) {
            query.like("nickname", nickname.trim());
        }
        applyDateLimit(query, dateLimit);
        query.orderByDesc("id");
        return query;
    }

    private void applyDateLimit(QueryWrapper<StoreProductReply> query, String dateLimit) {
        if (!StringUtils.hasText(dateLimit)) {
            return;
        }
        String[] parts = dateLimit.split(",");
        if (parts.length < 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
            return;
        }
        LocalDateTime start = LocalDate.parse(parts[0].trim()).atStartOfDay();
        LocalDateTime end = LocalDate.parse(parts[1].trim()).atTime(LocalTime.MAX);
        query.between("create_time", start, end);
    }

    private StoreProductReplyResponse toResponse(StoreProductReply reply) {
        StoreProductReplyResponse response = new StoreProductReplyResponse();
        response.setId(reply.getId());
        response.setUid(reply.getUid());
        response.setOid(reply.getOid());
        response.setProductId(reply.getProductId());
        response.setReplyType(reply.getReplyType());
        response.setProductScore(reply.getProductScore());
        response.setServiceScore(reply.getServiceScore());
        response.setComment(reply.getComment());
        response.setPics(splitPics(reply.getPics()));
        response.setMerchantReplyContent(reply.getMerchantReplyContent());
        response.setMerchantReplyTime(reply.getMerchantReplyTime());
        response.setIsDel(Integer.valueOf(1).equals(reply.getIsDel()));
        response.setIsReply(Integer.valueOf(1).equals(reply.getIsReply()));
        response.setNickname(reply.getNickname());
        response.setAvatar(reply.getAvatar());
        response.setCreateTime(reply.getCreateTime());
        response.setUpdateTime(reply.getUpdateTime());
        response.setSku(reply.getSku());
        response.setStoreProduct(productMapper.selectById(reply.getProductId()));
        return response;
    }

    private void validateReplyRequest(StoreProductReplyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("参数错误");
        }
        if (request.getProductId() == null || request.getProductId() <= 0) {
            throw new IllegalArgumentException("请选择商品");
        }
        if (request.getProductScore() == null || request.getProductScore() < 1 || request.getProductScore() > 5) {
            throw new IllegalArgumentException("商品分数为1-5");
        }
        if (request.getServiceScore() == null || request.getServiceScore() < 1 || request.getServiceScore() > 5) {
            throw new IllegalArgumentException("服务分数为1-5");
        }
        if (!StringUtils.hasText(request.getComment())) {
            throw new IllegalArgumentException("请填写评论内容");
        }
        if (request.getComment().trim().length() > 512) {
            throw new IllegalArgumentException("评论内容长度不能超过512个字符");
        }
        if (!StringUtils.hasText(request.getAvatar())) {
            throw new IllegalArgumentException("请选择用户头像");
        }
        if (!StringUtils.hasText(request.getNickname())) {
            throw new IllegalArgumentException("请填写用户名称");
        }
    }

    private List<String> splitPics(String pics) {
        if (!StringUtils.hasText(pics)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String pic : pics.split(",")) {
            if (StringUtils.hasText(pic)) {
                values.add(pic.trim());
            }
        }
        return values;
    }

    private String normalizePics(String pics) {
        if (!StringUtils.hasText(pics)) {
            return "";
        }
        try {
            List<String> list = objectMapper.readValue(pics, new TypeReference<>() {});
            return list.stream().map(this::clearImagePrefix).filter(StringUtils::hasText).collect(Collectors.joining(","));
        } catch (Exception ignored) {
            return splitPics(pics).stream().map(this::clearImagePrefix).collect(Collectors.joining(","));
        }
    }

    private String clearImagePrefix(String value) {
        String text = valueOrEmpty(value);
        if (!StringUtils.hasText(text)) {
            return "";
        }
        if (text.startsWith("http://") || text.startsWith("https://")) {
            int index = text.indexOf("/crmebimage/");
            return index >= 0 ? text.substring(index + 1) : text;
        }
        return text.startsWith("/") ? text.substring(1) : text;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer intValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int randomUnique() {
        return new Random().nextInt(88889) + 11111;
    }

    private void validateId(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("商品评论不存在");
        }
    }
}
