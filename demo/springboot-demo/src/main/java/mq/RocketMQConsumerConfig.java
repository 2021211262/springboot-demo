package com.example.springbootdemo.mq;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

@Configuration
@ConditionalOnProperty(name = "rocketmq.name-server")
public class RocketMQConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(RocketMQConsumerConfig.class);

    private DefaultMQPushConsumer consumer;

    @Bean
    public DefaultMQPushConsumer orderMessageConsumer(
            @Value("${rocketmq.name-server}") String nameServer) throws Exception {
        consumer = new DefaultMQPushConsumer("order-consumer-group");
        consumer.setNamesrvAddr(nameServer);
        consumer.subscribe("order-topic", "*");
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                String body = new String(msg.getBody());
                log.info("Order message received: {}", body);
                if (body.startsWith("ORDER_CREATED:")) {
                    String orderId = body.substring("ORDER_CREATED:".length());
                    log.info("Processing order created event: orderId={}", orderId);
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        log.info("RocketMQ consumer started, nameServer={}", nameServer);
        return consumer;
    }

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            consumer.shutdown();
        }
    }
}
