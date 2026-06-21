package com.jsy.crmeb.modern.service.schedule.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsy.crmeb.modern.service.product.dto.StoreProductStockRequest;
import com.jsy.crmeb.modern.service.product.mapper.ProductStockTaskMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Component("ProductStockTask")
public class ProductStockTask {
    private static final Logger logger = LoggerFactory.getLogger(ProductStockTask.class);
    private static final String PRODUCT_STOCK_UPDATE = "product_stock_update";
    private static final String PRODUCT_SECKILL_STOCK_UPDATE = "product_seckill_stock_update";
    private static final String PRODUCT_BARGAIN_STOCK_UPDATE = "product_bargain_stock_update";
    private static final String PRODUCT_COMBINATION_STOCK_UPDATE = "product_combination_stock_update";
    private static final int PRODUCT_TYPE_NORMAL = 0;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProductStockTaskMapper stockTaskMapper;
    private final TransactionTemplate transactionTemplate;

    public ProductStockTask(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            ProductStockTaskMapper stockTaskMapper,
            TransactionTemplate transactionTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.stockTaskMapper = stockTaskMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public void stockOperation() {
        consumeQueue(PRODUCT_STOCK_UPDATE, this::doProductStock);
        consumeQueue(PRODUCT_SECKILL_STOCK_UPDATE, this::doSeckillStock);
        consumeQueue(PRODUCT_BARGAIN_STOCK_UPDATE, this::doBargainStock);
        consumeQueue(PRODUCT_COMBINATION_STOCK_UPDATE, this::doCombinationStock);
    }

    private void consumeQueue(String redisKey, StockHandler handler) {
        Long size = redisTemplate.opsForList().size(redisKey);
        logger.info("ProductStockTask.stockOperation queue {} size: {}", redisKey, size);
        if (size == null || size < 1) {
            return;
        }
        for (int i = 0; i < size; i++) {
            String payload = redisTemplate.opsForList().rightPop(redisKey);
            if (!StringUtils.hasText(payload)) {
                continue;
            }
            try {
                StoreProductStockRequest request = objectMapper.readValue(payload, StoreProductStockRequest.class);
                Boolean result = transactionTemplate.execute(status -> handler.apply(request));
                if (!Boolean.TRUE.equals(result)) {
                    redisTemplate.opsForList().leftPush(redisKey, payload);
                }
            } catch (Exception exception) {
                redisTemplate.opsForList().leftPush(redisKey, payload);
                logger.error("库存队列处理失败，已重新入队，key = {}, payload = {}", redisKey, payload, exception);
            }
        }
    }

    private boolean doProductStock(StoreProductStockRequest request) {
        int stockDelta = stockDelta(request);
        int updated = stockTaskMapper.updateProductStock(request.getProductId(), stockDelta, request.getNum());
        if (updated == 0) {
            logger.info("库存修改任务未获取到商品信息 productId = {}", request.getProductId());
            return true;
        }
        if (request.getAttrId() != null) {
            stockTaskMapper.updateProductAttrStock(
                    request.getProductId(),
                    request.getAttrId(),
                    request.getType(),
                    stockDelta,
                    request.getNum());
        }
        return true;
    }

    private boolean doSeckillStock(StoreProductStockRequest request) {
        int stockDelta = stockDelta(request);
        int updated = stockTaskMapper.updateSeckillStock(request.getSeckillId(), stockDelta, request.getNum());
        if (updated == 0) {
            logger.info("库存修改任务未获取到秒杀商品信息 seckillId = {}", request.getSeckillId());
            return true;
        }
        updateActivityAttrAndLinkedProduct(request, request.getSeckillId(), stockDelta);
        return true;
    }

    private boolean doBargainStock(StoreProductStockRequest request) {
        int stockDelta = stockDelta(request);
        int updated = stockTaskMapper.updateBargainStock(request.getBargainId(), stockDelta, request.getNum());
        if (updated == 0) {
            logger.info("库存修改任务未获取到砍价商品信息 bargainId = {}", request.getBargainId());
            return true;
        }
        updateActivityAttrAndLinkedProduct(request, request.getBargainId(), stockDelta);
        return true;
    }

    private boolean doCombinationStock(StoreProductStockRequest request) {
        int stockDelta = stockDelta(request);
        int updated = stockTaskMapper.updateCombinationStock(request.getCombinationId(), stockDelta, request.getNum());
        if (updated == 0) {
            logger.info("库存修改任务未获取到拼团商品信息 combinationId = {}", request.getCombinationId());
            return true;
        }
        updateActivityAttrAndLinkedProduct(request, request.getCombinationId(), stockDelta);
        return true;
    }

    private void updateActivityAttrAndLinkedProduct(StoreProductStockRequest request, Integer activityId, int stockDelta) {
        if (request.getAttrId() != null) {
            stockTaskMapper.updateActivityAttrStock(
                    activityId,
                    request.getAttrId(),
                    request.getType(),
                    stockDelta,
                    request.getNum());
        }
        if (!StringUtils.hasText(request.getSuk())) {
            return;
        }
        List<Integer> normalAttrIds = stockTaskMapper.selectNormalAttrIdsBySuk(request.getProductId(), request.getSuk());
        for (Integer attrId : normalAttrIds) {
            StoreProductStockRequest linkedRequest = new StoreProductStockRequest();
            linkedRequest.setProductId(request.getProductId());
            linkedRequest.setAttrId(attrId);
            linkedRequest.setOperationType("add");
            linkedRequest.setNum(request.getNum());
            linkedRequest.setType(PRODUCT_TYPE_NORMAL);
            linkedRequest.setSuk(request.getSuk());
            doProductStock(linkedRequest);
        }
    }

    private int stockDelta(StoreProductStockRequest request) {
        if (request.getNum() == null || !StringUtils.hasText(request.getOperationType())) {
            throw new IllegalArgumentException("库存队列参数不完整");
        }
        return "add".equals(request.getOperationType()) ? request.getNum() : -request.getNum();
    }

    @FunctionalInterface
    private interface StockHandler {
        boolean apply(StoreProductStockRequest request);
    }
}
