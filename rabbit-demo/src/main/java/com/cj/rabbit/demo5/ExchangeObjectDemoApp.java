package com.cj.rabbit.demo5;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

@SpringBootApplication
public class ExchangeObjectDemoApp {
	public static void main( String[] args ) throws IOException, InterruptedException{
		ConfigurableApplicationContext ctx = SpringApplication.run(ExchangeObjectDemoApp.class, args);
		
		OrderQueueSender sender=ctx.getBean(OrderQueueSender.class);
		
		String routingKey="order.123";
		Order order = new Order();
		order.setId(System.currentTimeMillis());
		order.setMsgId(order.getId()+"$"+UUID.randomUUID().toString());
		order.setName("Hello");
		sender.send(routingKey,order);
    }
	
	public static final String Order_Exchange="order-exchange";
	public static final String Order_Queue="order-queue";
	public static final String Order_RoutingKey="order.#";
	
	// sender
	@Component
	public class OrderQueueSender{
		@Autowired
	    private RabbitTemplate rabbitTemplate;
		
		public void send(String routingKey,Order order){
			System.out.println("send order :"+order);
			rabbitTemplate.convertAndSend(Order_Exchange,routingKey,order,new CorrelationData(order.getMsgId()));
		}
	}
	
	// receiver
	@Component
	public class OrderQueueReceiver {
		@RabbitHandler
		@RabbitListener(
			// Queue,Exchange,Binding 不存在的会自动创建
			bindings=@QueueBinding(
				value=@Queue(name=Order_Queue,durable="true"),
				exchange=@Exchange(name=Order_Exchange,durable="true",type="topic"),
				key=Order_RoutingKey
			)
		)
//		@RabbitListener(queues = {Order_Queue})
		public void onOrderMessage(@Payload Order order,@Headers Map<String,Object> headers,Channel channel) 
			throws Exception{
			System.out.println("consume queue("+headers.get(AmqpHeaders.CONSUMER_QUEUE)+")"
					+" routingKey("+headers.get(AmqpHeaders.RECEIVED_ROUTING_KEY)+")"
					+" order:"+order);
			;

			// 手工签收：
			Long deliveryTag=(Long)headers.get(AmqpHeaders.DELIVERY_TAG);
			channel.basicAck(deliveryTag,false); // 给MQ主动回送一个信息，说明已签收
		}
	}
	
	// entity: Order
	public static class Order implements Serializable {
		private static final long serialVersionUID = -698577629696435935L;
		private Long id;
		private String name;
		private String msgId;
		public Long getId() {
			return id;
		}
		public void setId(Long id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getMsgId() {
			return msgId;
		}
		public void setMsgId(String msgId) {
			this.msgId = msgId;
		}
		
		@Override
		public String toString() {
			return "Order [id=" + id + ", name=" + name + ", msgId=" + msgId + "]";
		}
	}
}
