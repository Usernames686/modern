package com.jsy.crmeb.modern.service.front;

import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.front.mapper.FrontArticleMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FrontArticleService {
    private final FrontArticleMapper articleMapper;

    public FrontArticleService(FrontArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    public PageResponse<Map<String, Object>> categories() {
        List<Map<String, Object>> list = articleMapper.selectCategories();
        return new PageResponse<>(1, Math.max(list.size(), 1), list.size(), list);
    }

    public PageResponse<Map<String, Object>> list(String cid, int page, int limit) {
        if (cid == null || cid.isBlank()) {
            throw new IllegalArgumentException("文章分类不能为空");
        }
        int safePage = page <= 0 ? 1 : page;
        int safeLimit = limit <= 0 ? 8 : Math.min(limit, 50);
        int offset = (safePage - 1) * safeLimit;
        return new PageResponse<>(
                safePage,
                safeLimit,
                articleMapper.countByCategory(cid),
                normalizeArticles(articleMapper.selectByCategory(cid, offset, safeLimit)));
    }

    public PageResponse<Map<String, Object>> hotList() {
        List<Map<String, Object>> list = normalizeArticles(articleMapper.selectHot());
        return new PageResponse<>(1, Math.max(list.size(), 1), list.size(), list);
    }

    public PageResponse<Map<String, Object>> bannerList() {
        int limit = intConfig("news_slides_limit", 5);
        List<Map<String, Object>> list = normalizeArticles(articleMapper.selectBanner(Math.max(1, Math.min(limit, 20))));
        return new PageResponse<>(1, Math.max(list.size(), 1), list.size(), list);
    }

    @Transactional
    public Map<String, Object> info(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("文章ID不能为空");
        }
        Map<String, Object> article = articleMapper.selectInfo(id);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        articleMapper.increaseVisit(id);
        article.put("imageInput", normalizeAsset(string(article.get("imageInput"))));
        article.put("content", normalizeContent(string(article.get("content"))));
        return article;
    }

    private List<Map<String, Object>> normalizeArticles(List<Map<String, Object>> articles) {
        for (Map<String, Object> article : articles) {
            article.put("imageInput", normalizeAsset(string(article.get("imageInput"))));
        }
        return articles;
    }

    private String normalizeContent(String content) {
        if (content.isBlank()) {
            return "";
        }
        return content.replace("src=\"crmebimage/", "src=\"/crmebimage/")
                .replace("src='crmebimage/", "src='/crmebimage/");
    }

    private String normalizeAsset(String value) {
        if (value.isBlank() || value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/")) {
            return value;
        }
        return "/" + value;
    }

    private int intConfig(String name, int fallback) {
        try {
            String value = articleMapper.configValue(name);
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
