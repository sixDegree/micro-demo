package com.cj.rabbit.demo1;

import java.io.IOException;
import java.util.Map;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;



/* Direct Exchange (默认)：
 * 根据routing_key全文匹配寻找匹配的队列 
 * 
 * Producer -> Exchange -> (routingKey) -> Queue -> Consumer
 * 
 * case:
 * 	queue(direct.s1)
 * 	queue(direct.s2)
 * 	=>
 * 		defaultExchange -> routingKey:direct.s1 -> queue(direct.s1)
 * 		defaultExchange -> routingKey:direct.s2 -> queue(direct.s2)
 * send to defaultExchange:
 * 	msg with routingKey: direct.s1,direct.s1.user -> consume queue(direct.s1): direct.s1
 * 	msg with routingKey: direct.s2,direct.s2.user.role -> consume queue(direct.s2): direct.s2
 * 
 * */
@SpringBootApplication
@Configuration
public class DirectExchangeDemoApp {
	
	public static final String RoutingKey_S1="direct.s1";
	public static final String RoutingKey_S2="direct.s2";
	
	public static void main( String[] args ) throws IOException{
		ConfigurableApplicationContext ctx = SpringApplication.run(DirectExchangeDemoApp.class, args);
		QueueSender sender=ctx.getBean(QueueSender.class);
		
		sender.send(DirectExchangeDemoApp.RoutingKey_S1,"Hello world "+System.currentTimeMillis());
		sender.send(DirectExchangeDemoApp.RoutingKey_S1+".user","Hello world "+System.currentTimeMillis());
		sender.send(DirectExchangeDemoApp.RoutingKey_S2,"Hello world "+System.currentTimeMillis());
		sender.send(DirectExchangeDemoApp.RoutingKey_S2+".user.role","Hello world "+System.currentTimeMillis());
		
    }
	
	// config
	@Bean
    public Queue s1Queue() {
        return new Queue(DirectExchangeDemoApp.RoutingKey_S1);		//配置一个routingKey为direct.s1的消息队列
    }
	@Bean
	public Queue s2Queue() {
		return new Queue(DirectExchangeDemoApp.RoutingKey_S2);
	}
	
	// sender
	@Component
	public class QueueSender {
		@Autowired
	    private AmqpTemplate rabbitTemplate;
	     
	    public void send(String routingKey,String msg){
	    	System.out.println("send "+routingKey+" : "+msg);
	        rabbitTemplate.convertAndSend(routingKey, msg);
	    }
	}
	
	// receiver
	@Component
	@RabbitListener(queues = {DirectExchangeDemoApp.RoutingKey_S1,DirectExchangeDemoApp.RoutingKey_S2})
	public class QueueReceiver {
		
		@RabbitHandler
	    public void receive(@Payload String msg,@Headers Map<String,Object> headers,Channel channel) 
	    		throws IOException {
			System.out.println("consume queue("+headers.get(AmqpHeaders.CONSUMER_QUEUE)+")"
					+" routingKey("+headers.get(AmqpHeaders.RECEIVED_ROUTING_KEY)+")"
					+" msg:"+msg);
			;
			Long deliveryTag=(Long)headers.get(AmqpHeaders.DELIVERY_TAG);
			channel.basicAck(deliveryTag, false);
	    }
	}
}
