package com.jsy.crmeb.modern.service.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("eb_store_seckill")
public class StoreSeckill {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer productId;
    private String image;
    private String images;
    private String title;
    private String info;
    private BigDecimal price;
    private BigDecimal cost;
    private BigDecimal otPrice;
    private BigDecimal giveIntegral;
    private Integer sort;
    private Integer stock;
    private Integer sales;
    private String unitName;
    private BigDecimal postage;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime stopTime;
    private LocalDateTime createTime;
    private Integer status;
    private Boolean isPostage;
    private Boolean isDel;
    private Integer num;
    private Boolean isShow;
    private Integer timeId;
    private Integer tempId;
    private BigDecimal weight;
    private BigDecimal volume;
    private Integer quota;
    private Integer quotaShow;
    private Boolean specType;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public BigDecimal getOtPrice() { return otPrice; }
    public void setOtPrice(BigDecimal otPrice) { this.otPrice = otPrice; }
    public BigDecimal getGiveIntegral() { return giveIntegral; }
    public void setGiveIntegral(BigDecimal giveIntegral) { this.giveIntegral = giveIntegral; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getSales() { return sales; }
    public void setSales(Integer sales) { this.sales = sales; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public BigDecimal getPostage() { return postage; }
    public void setPostage(BigDecimal postage) { this.postage = postage; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getStopTime() { return stopTime; }
    public void setStopTime(LocalDateTime stopTime) { this.stopTime = stopTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Boolean getIsPostage() { return isPostage; }
    public void setIsPostage(Boolean isPostage) { this.isPostage = isPostage; }
    public Boolean getIsDel() { return isDel; }
    public void setIsDel(Boolean isDel) { this.isDel = isDel; }
    public Integer getNum() { return num; }
    public void setNum(Integer num) { this.num = num; }
    public Boolean getIsShow() { return isShow; }
    public void setIsShow(Boolean isShow) { this.isShow = isShow; }
    public Integer getTimeId() { return timeId; }
    public void setTimeId(Integer timeId) { this.timeId = timeId; }
    public Integer getTempId() { return tempId; }
    public void setTempId(Integer tempId) { this.tempId = tempId; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }
    public Integer getQuota() { return quota; }
    public void setQuota(Integer quota) { this.quota = quota; }
    public Integer getQuotaShow() { return quotaShow; }
    public void setQuotaShow(Integer quotaShow) { this.quotaShow = quotaShow; }
    public Boolean getSpecType() { return specType; }
    public void setSpecType(Boolean specType) { this.specType = specType; }
}
