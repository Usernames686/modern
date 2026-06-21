package com.jsy.crmeb.modern.service.schedule.task;

import com.jsy.crmeb.modern.service.user.entity.UserBrokerageRecord;
import com.jsy.crmeb.modern.service.user.mapper.UserBrokerageRecordMapper;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component("BrokerageFrozenTask")
public class BrokerageFrozenTask {
    private static final Logger logger = LoggerFactory.getLogger(BrokerageFrozenTask.class);

    private final UserBrokerageRecordMapper brokerageRecordMapper;
    private final TransactionTemplate transactionTemplate;

    public BrokerageFrozenTask(UserBrokerageRecordMapper brokerageRecordMapper, TransactionTemplate transactionTemplate) {
        this.brokerageRecordMapper = brokerageRecordMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public void brokerageFrozen() {
        long nowMillis = System.currentTimeMillis();
        List<UserBrokerageRecord> records = brokerageRecordMapper.selectDueFrozenOrderAddRecords(nowMillis);
        for (UserBrokerageRecord record : records) {
            try {
                Boolean thawed = transactionTemplate.execute(status -> thawRecord(record.getId(), nowMillis));
                if (!Boolean.TRUE.equals(thawed)) {
                    logger.warn("佣金解冻处理跳过，记录id = {}", record.getId());
                }
            } catch (Exception exception) {
                logger.error("佣金解冻处理失败，记录id = {}", record.getId(), exception);
            }
        }
    }

    private Boolean thawRecord(Integer recordId, long nowMillis) {
        UserBrokerageRecord record = brokerageRecordMapper.selectDueFrozenRecordForUpdate(recordId, nowMillis);
        if (record == null) {
            return Boolean.FALSE;
        }
        BigDecimal currentBrokeragePrice = brokerageRecordMapper.selectUserBrokeragePriceForUpdate(record.getUid());
        if (currentBrokeragePrice == null) {
            return Boolean.FALSE;
        }
        BigDecimal addPrice = record.getPrice() == null ? BigDecimal.ZERO : record.getPrice();
        BigDecimal balance = currentBrokeragePrice.add(addPrice);
        if (brokerageRecordMapper.markComplete(record.getId(), balance, nowMillis) != 1) {
            throw new IllegalStateException("佣金解冻记录状态更新失败，记录id = " + record.getId());
        }
        if (brokerageRecordMapper.updateUserBrokeragePrice(record.getUid(), balance) != 1) {
            throw new IllegalStateException("佣金解冻用户佣金更新失败，用户uid = " + record.getUid());
        }
        return Boolean.TRUE;
    }
}
