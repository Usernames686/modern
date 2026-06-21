package com.jsy.crmeb.modern.service.finance.dto;

import java.math.BigDecimal;

public class ExtractBalanceResponse {
    private BigDecimal withdrawn;
    private BigDecimal unDrawn;
    private BigDecimal commissionTotal;
    private BigDecimal toBeWithdrawn;

    public ExtractBalanceResponse(BigDecimal withdrawn, BigDecimal unDrawn, BigDecimal commissionTotal, BigDecimal toBeWithdrawn) {
        this.withdrawn = withdrawn;
        this.unDrawn = unDrawn;
        this.commissionTotal = commissionTotal;
        this.toBeWithdrawn = toBeWithdrawn;
    }

    public BigDecimal getWithdrawn() { return withdrawn; }
    public void setWithdrawn(BigDecimal withdrawn) { this.withdrawn = withdrawn; }
    public BigDecimal getUnDrawn() { return unDrawn; }
    public void setUnDrawn(BigDecimal unDrawn) { this.unDrawn = unDrawn; }
    public BigDecimal getCommissionTotal() { return commissionTotal; }
    public void setCommissionTotal(BigDecimal commissionTotal) { this.commissionTotal = commissionTotal; }
    public BigDecimal getToBeWithdrawn() { return toBeWithdrawn; }
    public void setToBeWithdrawn(BigDecimal toBeWithdrawn) { this.toBeWithdrawn = toBeWithdrawn; }
}
