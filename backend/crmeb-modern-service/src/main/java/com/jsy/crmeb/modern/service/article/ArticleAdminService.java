package com.jsy.crmeb.modern.service.article;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jsy.crmeb.modern.common.web.PageResponse;
import com.jsy.crmeb.modern.service.article.dto.ArticleRequest;
import com.jsy.crmeb.modern.service.article.dto.ArticleResponse;
import com.jsy.crmeb.modern.service.article.dto.ArticleSearchRequest;
import com.jsy.crmeb.modern.service.article.entity.Article;
import com.jsy.crmeb.modern.service.article.mapper.ArticleMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ArticleAdminService {
    private final ArticleMapper articleMapper;

    public ArticleAdminService(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    public PageResponse<ArticleResponse> list(ArticleSearchRequest request) {
        int page = request.getPage() <= 0 ? 1 : request.getPage();
        int limit = request.getLimit() <= 0 ? 20 : request.getLimit();
        Page<Article> articlePage = articleMapper.selectPage(new Page<>(page, limit), buildQuery(request));
        List<ArticleResponse> list = articlePage.getRecords().stream().map(this::toResponse).toList();
        return new PageResponse<>(page, limit, articlePage.getTotal(), list);
    }

    public Article info(Integer id) {
        return requiredArticle(id);
    }

    @Transactional
    public boolean create(ArticleRequest request) {
        Article article = fromRequest(request);
        article.setVisit("0");
        article.setStatus(false);
        article.setHide(false);
        article.setSort(0);
        article.setAdminId(0);
        article.setMerId(0);
        article.setProductId(0);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        return articleMapper.insert(article) > 0;
    }

    @Transactional
    public boolean update(Integer id, ArticleRequest request) {
        requiredArticle(id);
        Article article = fromRequest(request);
        article.setId(id);
        article.setUpdateTime(LocalDateTime.now());
        return articleMapper.updateById(article) > 0;
    }

    @Transactional
    public boolean delete(Integer id) {
        requiredArticle(id);
        return articleMapper.deleteById(id) > 0;
    }

    private QueryWrapper<Article> buildQuery(ArticleSearchRequest request) {
        QueryWrapper<Article> query = new QueryWrapper<>();
        if (StringUtils.hasText(request.getCid())) {
            query.eq("cid", request.getCid().trim());
        }
        if (StringUtils.hasText(request.getKeywords())) {
            String keywords = request.getKeywords().trim();
            query.and(wrapper -> wrapper.like("title", keywords)
                    .or()
                    .like("author", keywords)
                    .or()
                    .like("synopsis", keywords));
        }
        query.orderByDesc("visit").orderByDesc("id");
        return query;
    }

    private Article fromRequest(ArticleRequest request) {
        Article article = new Article();
        article.setCid(trim(request.getCid()));
        article.setTitle(trim(request.getTitle()));
        article.setAuthor(trim(request.getAuthor()));
        article.setImageInput(clearPrefix(request.getImageInput()));
        article.setSynopsis(trim(request.getSynopsis()));
        article.setShareTitle(trim(request.getShareTitle()));
        article.setShareSynopsis(trim(request.getShareSynopsis()));
        article.setIsHot(Boolean.TRUE.equals(request.getIsHot()));
        article.setIsBanner(Boolean.TRUE.equals(request.getIsBanner()));
        article.setContent(clearPrefix(request.getContent()));
        return article;
    }

    private ArticleResponse toResponse(Article article) {
        ArticleResponse response = new ArticleResponse();
        response.setId(article.getId());
        response.setCid(article.getCid());
        response.setTitle(article.getTitle());
        response.setAuthor(article.getAuthor());
        response.setImageInput(article.getImageInput());
        response.setSynopsis(article.getSynopsis());
        response.setShareTitle(article.getShareTitle());
        response.setShareSynopsis(article.getShareSynopsis());
        response.setVisit(article.getVisit());
        response.setIsHot(article.getIsHot());
        response.setIsBanner(article.getIsBanner());
        response.setContent(article.getContent());
        response.setCreateTime(article.getCreateTime());
        response.setUpdateTime(article.getUpdateTime());
        return response;
    }

    private Article requiredArticle(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("文章id不能为空");
        }
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        return article;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String clearPrefix(String value) {
        String text = trim(value);
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replaceAll("(https?://[^/]+)?/?crmebimage/", "crmebimage/");
    }
}
