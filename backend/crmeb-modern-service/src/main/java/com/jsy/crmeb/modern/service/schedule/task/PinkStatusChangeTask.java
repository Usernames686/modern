package com.jsy.crmeb.modern.service.schedule.task;

import com.jsy.crmeb.modern.service.combination.mapper.StorePinkTaskMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("PinkStatusChangeTask")
public class PinkStatusChangeTask {
    private static final Logger logger = LoggerFactory.getLogger(PinkStatusChangeTask.class);
    private static final String REFUND_REASON = "拼团订单取消，申请退款";
    private static final String REFUND_EXPLAIN = "用户取消拼团订单，申请退款";

    private final StorePinkTaskMapper storePinkTaskMapper;

    public PinkStatusChangeTask(StorePinkTaskMapper storePinkTaskMapper) {
        this.storePinkTaskMapper = storePinkTaskMapper;
    }

    @Transactional
    public void pinkStatusChage() {
        long nowMillis = System.currentTimeMillis();
        List<Map<String, Object>> heads = storePinkTaskMapper.selectExpiredActiveHeads(nowMillis);
        for (Map<String, Object> head : heads) {
            try {
                processHead(head);
            } catch (Exception exception) {
                logger.error("拼团状态变化处理失败，团长pinkId = {}", intValue(head.get("id")), exception);
            }
        }
    }

    private void processHead(Map<String, Object> head) {
        Integer headId = intValue(head.get("id"));
        Integer cid = intValue(head.get("cid"));
        int people = intValue(head.get("people"));
        int currentPeople = intValue(head.get("currentPeople"));
        int virtualRation = intValue(head.get("virtualRation"));
        if (headId == null || cid == null) {
            return;
        }
        if (people <= currentPeople) {
            int updated = storePinkTaskMapper.markTeamSuccess(cid, headId, false);
            logger.info("PinkStatusChangeTask.pinkStatusChage marked team {} success, updated {}", headId, updated);
            return;
        }
        if (people <= currentPeople + virtualRation) {
            int updated = storePinkTaskMapper.markTeamSuccess(cid, headId, true);
            logger.info("PinkStatusChangeTask.pinkStatusChage marked team {} virtual success, updated {}", headId, updated);
            return;
        }
        int failed = storePinkTaskMapper.markTeamFail(cid, headId);
        int refunds = storePinkTaskMapper.applyRefundForFailedTeam(cid, headId, REFUND_REASON, REFUND_EXPLAIN);
        logger.info("PinkStatusChangeTask.pinkStatusChage marked team {} fail, pink updated {}, refund applied {}", headId, failed, refunds);
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
