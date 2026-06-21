package com.jsy.crmeb.modern.service.front.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class FrontRechargeRequest {
    private BigDecimal price;
    @JsonProperty("rechar_id")
    private Integer groupDataId;
    private String payType = "weixin";
    @JsonProperty("from")
    private String fromType;

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getGroupDataId() { return groupDataId; }
    public void setGroupDataId(Integer groupDataId) { this.groupDataId = groupDataId; }
    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }
    public String getFromType() { return fromType; }
    public void setFromType(String fromType) { this.fromType = fromType; }
}
