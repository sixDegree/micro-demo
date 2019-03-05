package com.cj.rabbit.demo1.consumer;

import java.util.Map;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.cj.rabbit.demo1.entity.Order;
import com.rabbitmq.client.Channel;

@Component
public class OrderReceiver {
	@RabbitListener(
			bindings=@QueueBinding(
				value=@Queue(value="order-queue",durable="true"),
				exchange=@Exchange(name="order-exchange",durable="true",type="topic"),
				key="order.#"
			)
		)
		@RabbitHandler
		public void onOrderMessage(@Payload Order order,@Headers Map<String,Object> headers,Channel channel) 
			throws Exception{
			System.err.println("Received Message");
			System.err.println("order Id:"+order.getId());

			// 手工签收：
			Long deliveryTag=(Long)headers.get(AmqpHeaders.DELIVERY_TAG);
			channel.basicAck(deliveryTag,false); // 给MQ主动回送一个信息，说明已签收
		}
}
