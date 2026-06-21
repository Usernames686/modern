package com.jsy.crmeb.modern.service.dashboard.dto;

import java.math.BigDecimal;

public class HomeRateResponse {
    private BigDecimal sales = BigDecimal.ZERO;
    private BigDecimal yesterdaySales = BigDecimal.ZERO;
    private Integer pageviews = 0;
    private Integer yesterdayPageviews = 0;
    private Integer orderNum = 0;
    private Integer yesterdayOrderNum = 0;
    private Integer newUserNum = 0;
    private Integer yesterdayNewUserNum = 0;

    public BigDecimal getSales() { return sales; }
    public void setSales(BigDecimal sales) { this.sales = sales; }
    public BigDecimal getYesterdaySales() { return yesterdaySales; }
    public void setYesterdaySales(BigDecimal yesterdaySales) { this.yesterdaySales = yesterdaySales; }
    public Integer getPageviews() { return pageviews; }
    public void setPageviews(Integer pageviews) { this.pageviews = pageviews; }
    public Integer getYesterdayPageviews() { return yesterdayPageviews; }
    public void setYesterdayPageviews(Integer yesterdayPageviews) { this.yesterdayPageviews = yesterdayPageviews; }
    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }
    public Integer getYesterdayOrderNum() { return yesterdayOrderNum; }
    public void setYesterdayOrderNum(Integer yesterdayOrderNum) { this.yesterdayOrderNum = yesterdayOrderNum; }
    public Integer getNewUserNum() { return newUserNum; }
    public void setNewUserNum(Integer newUserNum) { this.newUserNum = newUserNum; }
    public Integer getYesterdayNewUserNum() { return yesterdayNewUserNum; }
    public void setYesterdayNewUserNum(Integer yesterdayNewUserNum) { this.yesterdayNewUserNum = yesterdayNewUserNum; }
}
