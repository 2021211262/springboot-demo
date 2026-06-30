package com.example.springbootdemo.mq;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class OrderMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageProducer.class);

    private static final String TOPIC = "order-topic";

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    public void sendOrderCreatedMessage(Long orderId) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQ not available, skip sending order message: orderId={}", orderId);
            return;
        }
        String payload = "ORDER_CREATED:" + orderId;
        Message<String> message = MessageBuilder.withPayload(payload).build();
        rocketMQTemplate.send(TOPIC, message);
        log.info("Order message sent successfully: orderId={}", orderId);
    }
}
