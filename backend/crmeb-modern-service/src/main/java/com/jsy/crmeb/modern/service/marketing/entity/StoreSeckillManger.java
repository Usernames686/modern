package com.jsy.crmeb.modern.service.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("eb_store_seckill_manger")
public class StoreSeckillManger {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private Integer startTime;
    private Integer endTime;
    private String img;
    private String silderImgs;
    private Integer sort;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean isDel;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getStartTime() { return startTime; }
    public void setStartTime(Integer startTime) { this.startTime = startTime; }
    public Integer getEndTime() { return endTime; }
    public void setEndTime(Integer endTime) { this.endTime = endTime; }
    public String getImg() { return img; }
    public void setImg(String img) { this.img = img; }
    public String getSilderImgs() { return silderImgs; }
    public void setSilderImgs(String silderImgs) { this.silderImgs = silderImgs; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Boolean getIsDel() { return isDel; }
    public void setIsDel(Boolean isDel) { this.isDel = isDel; }
}
