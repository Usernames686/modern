package com.jsy.crmeb.modern.service.order.dto;

public class StoreOrderCountItemResponse {
    private long all;
    private long unPaid;
    private long notShipped;
    private long spike;
    private long bargain;
    private long complete;
    private long toBeWrittenOff;
    private long refunding;
    private long refunded;
    private long refundRefused;
    private long deleted;

    public long getAll() { return all; }
    public void setAll(long all) { this.all = all; }
    public long getUnPaid() { return unPaid; }
    public void setUnPaid(long unPaid) { this.unPaid = unPaid; }
    public long getNotShipped() { return notShipped; }
    public void setNotShipped(long notShipped) { this.notShipped = notShipped; }
    public long getSpike() { return spike; }
    public void setSpike(long spike) { this.spike = spike; }
    public long getBargain() { return bargain; }
    public void setBargain(long bargain) { this.bargain = bargain; }
    public long getComplete() { return complete; }
    public void setComplete(long complete) { this.complete = complete; }
    public long getToBeWrittenOff() { return toBeWrittenOff; }
    public void setToBeWrittenOff(long toBeWrittenOff) { this.toBeWrittenOff = toBeWrittenOff; }
    public long getRefunding() { return refunding; }
    public void setRefunding(long refunding) { this.refunding = refunding; }
    public long getRefunded() { return refunded; }
    public void setRefunded(long refunded) { this.refunded = refunded; }
    public long getRefundRefused() { return refundRefused; }
    public void setRefundRefused(long refundRefused) { this.refundRefused = refundRefused; }
    public long getDeleted() { return deleted; }
    public void setDeleted(long deleted) { this.deleted = deleted; }
}
