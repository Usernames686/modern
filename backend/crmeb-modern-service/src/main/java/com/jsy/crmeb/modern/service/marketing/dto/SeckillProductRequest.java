package com.jsy.crmeb.modern.service.marketing.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class SeckillProductRequest {
    private Integer id;
    private Integer productId;
    private String image;
    private String images;
    private List<String> imagess;
    private String title;
    private String info;
    private String unitName;
    private Integer timeId;
    private String startTime;
    private String stopTime;
    private Integer status;
    private Integer num;
    private Integer tempId;
    private Integer sort;
    private BigDecimal price;
    private BigDecimal cost;
    private BigDecimal otPrice;
    private BigDecimal giveIntegral;
    private Integer stock;
    private Integer quota;
    private Integer quotaShow;
    private BigDecimal weight;
    private BigDecimal volume;
    private BigDecimal postage;
    private Boolean specType;
    private String content;
    private List<Map<String, Object>> attr;
    private List<Map<String, Object>> attrValue;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
    public List<String> getImagess() { return imagess; }
    public void setImagess(List<String> imagess) { this.imagess = imagess; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public Integer getTimeId() { return timeId; }
    public void setTimeId(Integer timeId) { this.timeId = timeId; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getStopTime() { return stopTime; }
    public void setStopTime(String stopTime) { this.stopTime = stopTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getNum() { return num; }
    public void setNum(Integer num) { this.num = num; }
    public Integer getTempId() { return tempId; }
    public void setTempId(Integer tempId) { this.tempId = tempId; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public BigDecimal getOtPrice() { return otPrice; }
    public void setOtPrice(BigDecimal otPrice) { this.otPrice = otPrice; }
    public BigDecimal getGiveIntegral() { return giveIntegral; }
    public void setGiveIntegral(BigDecimal giveIntegral) { this.giveIntegral = giveIntegral; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getQuota() { return quota; }
    public void setQuota(Integer quota) { this.quota = quota; }
    public Integer getQuotaShow() { return quotaShow; }
    public void setQuotaShow(Integer quotaShow) { this.quotaShow = quotaShow; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }
    public BigDecimal getPostage() { return postage; }
    public void setPostage(BigDecimal postage) { this.postage = postage; }
    public Boolean getSpecType() { return specType; }
    public void setSpecType(Boolean specType) { this.specType = specType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<Map<String, Object>> getAttr() { return attr; }
    public void setAttr(List<Map<String, Object>> attr) { this.attr = attr; }
    public List<Map<String, Object>> getAttrValue() { return attrValue; }
    public void setAttrValue(List<Map<String, Object>> attrValue) { this.attrValue = attrValue; }
}
