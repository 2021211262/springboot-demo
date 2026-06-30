package com.example.springbootdemo.service;

import com.example.springbootdemo.common.BusinessException;
import com.example.springbootdemo.common.ResultCode;
import com.example.springbootdemo.entity.Order;
import com.example.springbootdemo.mapper.OrderMapper;
import com.example.springbootdemo.mq.OrderMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final String LOCK_KEY_PREFIX = "lock:order:productId:";
    private static final long LOCK_EXPIRE_SECONDS = 30;
    private static final int LOCK_RETRY_COUNT = 3;
    private static final long LOCK_RETRY_INTERVAL_MS = 200;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderCreateService orderCreateService;

    @Autowired
    private DistributedLockService distributedLockService;

    @Autowired
    private OrderMessageProducer orderMessageProducer;

    public Order createOrder(Long productId, Integer quantity) {
        String lockKey = LOCK_KEY_PREFIX + productId;
        String lockValue = distributedLockService.tryLockWithRetry(lockKey, LOCK_EXPIRE_SECONDS, LOCK_RETRY_COUNT, LOCK_RETRY_INTERVAL_MS);
        if (lockValue == null) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS.getCode(), "系统繁忙，请稍后重试");
        }

        try {
            Order order = orderCreateService.doCreateOrder(productId, quantity);

            try {
                orderMessageProducer.sendOrderCreatedMessage(order.getId());
            } catch (Exception e) {
                log.error("Failed to send order message after order created: orderId={}", order.getId(), e);
            }

            return order;
        } finally {
            distributedLockService.unlock(lockKey, lockValue);
        }
    }

    public Order getOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
        }
        return order;
    }

    public List<Order> getAllOrders() {
        return orderMapper.selectAll();
    }

    public int getOrderCountByProductId(Long productId) {
        return orderMapper.selectCountByProductId(productId);
    }
}
