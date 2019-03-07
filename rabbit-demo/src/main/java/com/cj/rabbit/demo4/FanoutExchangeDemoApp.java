package com.cj.rabbit.demo4;

import java.io.IOException;
import java.util.Map;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
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

/* Fanout Exchange
 * 广播模式，转发消息到所有绑定队列（和routingKey没有关系）
 * 
 * Producer -> Exchange -> (all) -> Queue -> Consumer
 * 
 * case:
 * binding: 
 * 		FanoutExchange - all - queue(fanout.s1) & queue(fanout.s2)
 * 		defaultExchange - routingKey: 123 - queue(123)
 * send to FanoutExchange with routingKey:123,fanout.s1,fanout.s1.user,... 
 * 		-> consume queue(fanout.s1) & queue(fanout.s2)
 * 
 *  */
@SpringBootApplication
public class FanoutExchangeDemoApp {
	
	public static void main( String[] args ) throws IOException, InterruptedException{
		ConfigurableApplicationContext ctx = SpringApplication.run(FanoutExchangeDemoApp.class, args);
		
		QueueSender sender=ctx.getBean(QueueSender.class);
		
//		String routingKey=FanoutExchangeConfig.RoutingKey_S1;
		String routingKey="123";
		sender.send(routingKey,"Hello world S1 "+System.currentTimeMillis());
		sender.send(routingKey+".user","Hello World S1.User "+System.currentTimeMillis());
		sender.send(routingKey+".user.role","Hello World S1.User.Role "+System.currentTimeMillis());
    }
	
	// config
	@Configuration
	public class FanoutExchangeConfig{
		public final static String RoutingKey_S1="fanout.s1";
		public final static String RoutingKey_S2="fanout.s2";
		public final static String Exchange_Name="fanoutExchange";
		
		// Queue
		@Bean
		public Queue s1Queue(){
			return new Queue(RoutingKey_S1);
		}
		@Bean
		public Queue s2Queue(){
			return new Queue(RoutingKey_S2);
		}
		@Bean
		public Queue testQueue() {
			return new Queue("123");
		}
		
		// Exchange
		@Bean
		public FanoutExchange exchange(){
			return new FanoutExchange(Exchange_Name);
		}
		
		// Binding
		@Bean
		public Binding bindingS1QueueAndExchange(Queue s1Queue, FanoutExchange exchange){
			return BindingBuilder.bind(s1Queue).to(exchange);
		}
		@Bean
		public Binding bingS2QueueAndExchange(Queue s2Queue,FanoutExchange exchange){
			return BindingBuilder.bind(s2Queue).to(exchange);
		}
	}
	
	// sender
	@Component
	public class QueueSender{
		@Autowired
	    private AmqpTemplate rabbitTemplate;
		
		public void send(String routingKey,String msg){
			System.out.println("send routingKey("+routingKey+") :"+msg);
			rabbitTemplate.convertAndSend(FanoutExchangeConfig.Exchange_Name, routingKey, msg);
		}
	}
	
	// receiver
	@Component
	public class QueueReceiver{
		@RabbitHandler
	    @RabbitListener(queues = {FanoutExchangeConfig.RoutingKey_S1,FanoutExchangeConfig.RoutingKey_S2,"123"})
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
