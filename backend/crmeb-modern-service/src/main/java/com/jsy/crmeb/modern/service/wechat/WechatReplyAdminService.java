package com.jsy.crmeb.modern.service.wechat;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.wechat.dto.WechatReplyRequest;
import com.jsy.crmeb.modern.service.wechat.dto.WechatReplySearchRequest;
import com.jsy.crmeb.modern.service.wechat.entity.WechatReply;
import com.jsy.crmeb.modern.service.wechat.mapper.WechatReplyMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WechatReplyAdminService {
    private final WechatReplyMapper wechatReplyMapper;

    public WechatReplyAdminService(WechatReplyMapper wechatReplyMapper) {
        this.wechatReplyMapper = wechatReplyMapper;
    }

    public PageResponse<WechatReply> list(WechatReplySearchRequest request) {
        int page = request.getPage() <= 0 ? 1 : request.getPage();
        int limit = request.getLimit() <= 0 ? 20 : request.getLimit();
        Page<WechatReply> replyPage = wechatReplyMapper.selectPage(new Page<>(page, limit), buildQuery(request));
        return new PageResponse<>(page, limit, replyPage.getTotal(), replyPage.getRecords());
    }

    public WechatReply info(Integer id) {
        return requiredReply(id);
    }

    public WechatReply infoByKeywords(String keywords) {
        if (!StringUtils.hasText(keywords)) {
            throw new IllegalArgumentException("关键字不能为空");
        }
        QueryWrapper<WechatReply> query = new QueryWrapper<>();
        query.eq("keywords", keywords.trim());
        return wechatReplyMapper.selectOne(query);
    }

    @Transactional
    public boolean create(WechatReplyRequest request) {
        assertKeywordUnique(request.getKeywords(), null);
        WechatReply reply = fromRequest(request, new WechatReply());
        reply.setCreateTime(LocalDateTime.now());
        reply.setUpdateTime(LocalDateTime.now());
        return wechatReplyMapper.insert(reply) > 0;
    }

    @Transactional
    public boolean update(WechatReplyRequest request) {
        WechatReply reply = requiredReply(request.getId());
        assertKeywordUnique(request.getKeywords(), request.getId());
        fromRequest(request, reply);
        reply.setUpdateTime(LocalDateTime.now());
        return wechatReplyMapper.updateById(reply) > 0;
    }

    @Transactional
    public boolean updateStatus(Integer id, Boolean status) {
        WechatReply reply = requiredReply(id);
        reply.setStatus(Boolean.TRUE.equals(status));
        reply.setUpdateTime(LocalDateTime.now());
        return wechatReplyMapper.updateById(reply) > 0;
    }

    @Transactional
    public boolean delete(Integer id) {
        WechatReply reply = requiredReply(id);
        if ("subscribe".equals(reply.getKeywords()) || "default".equals(reply.getKeywords())) {
            throw new IllegalArgumentException("关注回复和无匹配回复不能删除");
        }
        return wechatReplyMapper.deleteById(id) > 0;
    }

    private QueryWrapper<WechatReply> buildQuery(WechatReplySearchRequest request) {
        QueryWrapper<WechatReply> query = new QueryWrapper<>();
        if (StringUtils.hasText(request.getType())) {
            query.eq("type", request.getType().trim());
        }
        if (StringUtils.hasText(request.getKeywords())) {
            query.like("keywords", request.getKeywords().trim());
        }
        query.orderByDesc("id");
        return query;
    }

    private WechatReply fromRequest(WechatReplyRequest request, WechatReply reply) {
        reply.setKeywords(request.getKeywords().trim());
        reply.setType(request.getType().trim());
        reply.setData(clearPrefix(request.getData()));
        reply.setStatus(Boolean.TRUE.equals(request.getStatus()));
        return reply;
    }

    private void assertKeywordUnique(String keywords, Integer id) {
        if (!StringUtils.hasText(keywords)) {
            throw new IllegalArgumentException("关键字不能为空");
        }
        WechatReply existed = infoByKeywords(keywords);
        if (existed != null && (id == null || !id.equals(existed.getId()))) {
            throw new IllegalArgumentException(keywords.trim() + "关键字已经存在");
        }
    }

    private WechatReply requiredReply(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("回复id不能为空");
        }
        WechatReply reply = wechatReplyMapper.selectById(id);
        if (reply == null) {
            throw new IllegalArgumentException("没有找到相关数据");
        }
        return reply;
    }

    private String clearPrefix(String value) {
        String text = StringUtils.hasText(value) ? value.trim() : "";
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replaceAll("(https?://[^/]+)?/?crmebimage/", "crmebimage/");
    }
}
