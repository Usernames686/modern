package com.jsy.crmeb.modern.service.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ArticleRequest {
    @NotBlank(message = "请选择分类")
    private String cid;
    @NotBlank(message = "请填写文章标题")
    @Size(max = 200, message = "文章标题最多200个字符")
    private String title;
    @NotBlank(message = "请填写文章作者")
    @Size(max = 50, message = "文章作者最多50个字符")
    private String author;
    @NotBlank(message = "请上传文章图片")
    @Size(max = 255, message = "文章图片名称最多255个字符")
    private String imageInput;
    @NotBlank(message = "请填写文章简介")
    @Size(max = 200, message = "文章简介最多200个字符")
    private String synopsis;
    @NotBlank(message = "请填写文章分享标题")
    @Size(max = 200, message = "文章分享标题最多200个字符")
    private String shareTitle;
    @NotBlank(message = "请填写文章分享简介")
    @Size(max = 200, message = "文章分享简介最多200个字符")
    private String shareSynopsis;
    @NotNull(message = "是否热门(小程序)不能为空")
    private Boolean isHot;
    @NotNull(message = "是否轮播图(小程序)不能为空")
    private Boolean isBanner;
    @NotBlank(message = "请填写文章内容")
    private String content;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getImageInput() {
        return imageInput;
    }

    public void setImageInput(String imageInput) {
        this.imageInput = imageInput;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public String getShareTitle() {
        return shareTitle;
    }

    public void setShareTitle(String shareTitle) {
        this.shareTitle = shareTitle;
    }

    public String getShareSynopsis() {
        return shareSynopsis;
    }

    public void setShareSynopsis(String shareSynopsis) {
        this.shareSynopsis = shareSynopsis;
    }

    public Boolean getIsHot() {
        return isHot;
    }

    public void setIsHot(Boolean isHot) {
        this.isHot = isHot;
    }

    public Boolean getIsBanner() {
        return isBanner;
    }

    public void setIsBanner(Boolean isBanner) {
        this.isBanner = isBanner;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
