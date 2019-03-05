package com.cj.rabbit.demo3;

import java.io.IOException;
import java.util.Map;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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
public class Demo3App {
	public static void main( String[] args ) throws IOException, InterruptedException{
		ConfigurableApplicationContext ctx = SpringApplication.run(Demo3App.class, args);
		
		QueueSender sender=ctx.getBean(QueueSender.class);
		
		sender.send(RabbitTopicConfig.RouteKey_S1,"Hello world S1 "+System.currentTimeMillis());
		sender.send(RabbitTopicConfig.RouteKey_S1+".user","Hello World S1.User "+System.currentTimeMillis());
		sender.send(RabbitTopicConfig.RouteKey_S1+".user.role","Hello World S1.User.Role "+System.currentTimeMillis());
		
		Thread.sleep(3000);
		
		sender.send(RabbitTopicConfig.RouteKey_S2,"Hello world S2 "+System.currentTimeMillis());
		sender.send(RabbitTopicConfig.RouteKey_S2+".user","Hello world S2.User "+System.currentTimeMillis());
		sender.send(RabbitTopicConfig.RouteKey_S2+".user.role","Hello world S2.User.Role "+System.currentTimeMillis());
    }
	
	@Component
	public class QueueSender{
		@Autowired
	    private AmqpTemplate rabbitTemplate;
		
		public void send(String routingKey,String msg){
			System.out.println("send "+routingKey+" message:"+msg);
			rabbitTemplate.convertAndSend(RabbitTopicConfig.Exchange_Name, routingKey, msg);
		}
	}
	
	@Component
	public class QueueReceiver{
		@RabbitHandler
	    @RabbitListener(queues = {"api.s1","api.s2"})
		public void receive(@Payload String msg,@Headers Map<String,Object> headers,Channel channel) 
				throws IOException {
			System.out.println("receive "+headers.get(AmqpHeaders.CONSUMER_QUEUE)+" message: "+msg);
			
			Long deliveryTag=(Long)headers.get(AmqpHeaders.DELIVERY_TAG);
			channel.basicAck(deliveryTag, false);
	    }
	}
}
