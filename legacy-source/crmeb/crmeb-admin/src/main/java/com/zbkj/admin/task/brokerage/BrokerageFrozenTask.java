package com.zbkj.admin.task.brokerage;


import com.zbkj.admin.task.order.OrderReceiptTask;
import com.zbkj.common.utils.CrmebDateUtil;
import com.zbkj.service.service.UserBrokerageRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 佣金冻结期解冻task
 */
@Component("BrokerageFrozenTask")
public class BrokerageFrozenTask {

    //日志
    private static final Logger logger = LoggerFactory.getLogger(OrderReceiptTask.class);

    @Autowired
    private UserBrokerageRecordService userBrokerageRecordService;

    /**
     * 1小时同步一次数据
     */
    public void brokerageFrozen() {
        // cron : 0 0 */1 * * ?
        logger.info("---BrokerageFrozenTask task------produce Data with fixed rate task: Execution Time - {}", CrmebDateUtil.nowDateTime());
        try {
            userBrokerageRecordService.brokerageThaw();

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("BrokerageFrozenTask.task" + " | msg : " + e.getMessage());
        }
    }
}
