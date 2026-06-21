package com.jsy.crmeb.modern.service.dashboard.dto;

import java.math.BigDecimal;

public class DashboardChartPoint {
    private String label;
    private BigDecimal price = BigDecimal.ZERO;
    private Integer countValue = 0;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getCountValue() { return countValue; }
    public void setCountValue(Integer countValue) { this.countValue = countValue; }
}
