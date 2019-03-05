package com.cj.rabbit.demo1.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cj.rabbit.demo1.entity.Order;

@Component
public class OrderSender {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	public void send(Order order) throws Exception{
		CorrelationData correlationData=new CorrelationData();
		correlationData.setId(order.getMsgId());

		rabbitTemplate.convertAndSend("order-exchange",	// exchange
			"order.abcd",	 // routingkey
			order,			// 消息内容
			correlationData // 消息唯一Id
			);
	}
}
