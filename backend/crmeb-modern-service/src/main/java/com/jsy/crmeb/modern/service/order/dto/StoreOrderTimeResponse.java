package com.jsy.crmeb.modern.service.order.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class StoreOrderTimeResponse {
    private List<StoreOrderTimeChartItem> chart = new ArrayList<>();
    private BigDecimal growthRate = BigDecimal.ZERO;
    private BigDecimal increaseTime = BigDecimal.ZERO;
    private int increaseTimeStatus = 1;
    private BigDecimal time = BigDecimal.ZERO;

    public List<StoreOrderTimeChartItem> getChart() {
        return chart;
    }

    public void setChart(List<StoreOrderTimeChartItem> chart) {
        this.chart = chart;
    }

    public BigDecimal getGrowthRate() {
        return growthRate;
    }

    public void setGrowthRate(BigDecimal growthRate) {
        this.growthRate = growthRate;
    }

    public BigDecimal getIncreaseTime() {
        return increaseTime;
    }

    public void setIncreaseTime(BigDecimal increaseTime) {
        this.increaseTime = increaseTime;
    }

    public int getIncreaseTimeStatus() {
        return increaseTimeStatus;
    }

    public void setIncreaseTimeStatus(int increaseTimeStatus) {
        this.increaseTimeStatus = increaseTimeStatus;
    }

    public BigDecimal getTime() {
        return time;
    }

    public void setTime(BigDecimal time) {
        this.time = time;
    }

    public static class StoreOrderTimeChartItem {
        private String time;
        private BigDecimal num = BigDecimal.ZERO;

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public BigDecimal getNum() {
            return num;
        }

        public void setNum(BigDecimal num) {
            this.num = num;
        }
    }
}
