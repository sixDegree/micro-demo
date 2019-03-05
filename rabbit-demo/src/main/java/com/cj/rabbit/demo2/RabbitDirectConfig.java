package com.cj.rabbit.demo2;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitDirectConfig {
	@Bean
    public Queue NotifyHelloQueue() {
        return new Queue("notify.hello");		//配置一个routingKey为notify.hello的消息队列
    }
}
