package com.jsy.crmeb.modern.service.finance.dto;

public class BrokerageRecordRequest {
    private Integer type;
    private Integer page = 1;
    private Integer limit = 20;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
