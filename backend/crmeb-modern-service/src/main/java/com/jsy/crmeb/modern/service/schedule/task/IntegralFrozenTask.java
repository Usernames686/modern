package com.jsy.crmeb.modern.service.schedule.task;

import com.jsy.crmeb.modern.service.user.entity.UserIntegralRecord;
import com.jsy.crmeb.modern.service.user.mapper.UserIntegralRecordMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component("IntegralFrozenTask")
public class IntegralFrozenTask {
    private static final Logger logger = LoggerFactory.getLogger(IntegralFrozenTask.class);

    private final UserIntegralRecordMapper integralRecordMapper;
    private final TransactionTemplate transactionTemplate;

    public IntegralFrozenTask(UserIntegralRecordMapper integralRecordMapper, TransactionTemplate transactionTemplate) {
        this.integralRecordMapper = integralRecordMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public void integralFrozen() {
        long nowMillis = System.currentTimeMillis();
        List<UserIntegralRecord> records = integralRecordMapper.selectDueFrozenOrderAddRecords(nowMillis);
        for (UserIntegralRecord record : records) {
            try {
                Boolean thawed = transactionTemplate.execute(status -> thawRecord(record.getId(), nowMillis));
                if (!Boolean.TRUE.equals(thawed)) {
                    logger.warn("积分解冻处理跳过，记录id = {}", record.getId());
                }
            } catch (Exception exception) {
                logger.error("积分解冻处理失败，记录id = {}", record.getId(), exception);
            }
        }
    }

    private Boolean thawRecord(Integer recordId, long nowMillis) {
        UserIntegralRecord record = integralRecordMapper.selectDueFrozenRecordForUpdate(recordId, nowMillis);
        if (record == null) {
            return Boolean.FALSE;
        }
        Integer currentIntegral = integralRecordMapper.selectUserIntegralForUpdate(record.getUid());
        if (currentIntegral == null) {
            return Boolean.FALSE;
        }
        int addIntegral = record.getIntegral() == null ? 0 : record.getIntegral();
        int balance = currentIntegral + addIntegral;
        if (integralRecordMapper.markComplete(record.getId(), balance, nowMillis) != 1) {
            throw new IllegalStateException("积分解冻记录状态更新失败，记录id = " + record.getId());
        }
        if (integralRecordMapper.updateUserIntegral(record.getUid(), balance) != 1) {
            throw new IllegalStateException("积分解冻用户积分更新失败，用户uid = " + record.getUid());
        }
        return Boolean.TRUE;
    }
}
