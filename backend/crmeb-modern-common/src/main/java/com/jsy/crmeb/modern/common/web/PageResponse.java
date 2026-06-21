package com.jsy.crmeb.modern.common.web;

import java.util.List;

public class PageResponse<T> {
    private int page;
    private int limit;
    private int totalPage;
    private long total;
    private List<T> list;

    public PageResponse(int page, int limit, long total, List<T> list) {
        this.page = page;
        this.limit = limit;
        this.total = total;
        this.list = list;
        this.totalPage = limit <= 0 ? 0 : (int) Math.ceil((double) total / limit);
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
