package com.jsy.crmeb.modern.service.schedule.task;

import com.jsy.crmeb.modern.service.bargain.mapper.StoreBargainTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("BargainStopChangeTask")
public class BargainStopChangeTask {
    private static final Logger logger = LoggerFactory.getLogger(BargainStopChangeTask.class);

    private final StoreBargainTaskMapper storeBargainTaskMapper;

    public BargainStopChangeTask(StoreBargainTaskMapper storeBargainTaskMapper) {
        this.storeBargainTaskMapper = storeBargainTaskMapper;
    }

    @Transactional
    public void bargainStopChange() {
        long nowMillis = System.currentTimeMillis();
        try {
            int count = storeBargainTaskMapper.markExpiredParticipatingUsersFailed(nowMillis);
            logger.info("BargainStopChangeTask.bargainStopChange updated {} bargain user records", count);
        } catch (Exception exception) {
            logger.error("砍价活动结束后更新用户状态失败", exception);
        }
    }
}
