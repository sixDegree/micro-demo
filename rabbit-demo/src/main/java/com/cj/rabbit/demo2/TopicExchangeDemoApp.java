package com.cj.rabbit.demo2;

import java.io.IOException;
import java.util.Map;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
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

/* Topic Exchange ：
 * 与direct类似, 只是routing_key匹配上支持通配符：`*` 只能向后多匹配一层路径 ; `#` 可以向后匹配多层路径 
 * 
 * Producer -> Exchange -> (routingKey) -> Queue -> Consumer
 * 
 * case:
 * binding:
 * 		topicExchange - routingKey: topic.s1.* - queue(topic.s1) 
 * 		topicExchange - routingKey: topic.s2.# - queue(topic.s2)
 * 		defaultExchange - routingKey: 123 - queue(123)
 * send to topicExchange:
 * 		msg with routingKey: 123	-> no consume
 * 		msg with routingKey: topic.s1,topic.s1.user,topic.s1.user.role -> consume queue(topic.s1) routingKey(topic.s1.user)
 * 		msg with routingKey: s2,s2.user,s2.user.role -> consume queue(topic.s2): all
 * */
@SpringBootApplication
public class TopicExchangeDemoApp {
	public static void main( String[] args ) throws IOException, InterruptedException{
		ConfigurableApplicationContext ctx = SpringApplication.run(TopicExchangeDemoApp.class, args);
		
		QueueSender sender=ctx.getBean(QueueSender.class);
		
		String routingKey=TopicExchangeConfig.RoutingKey_S2; // "123",TopicExchangeConfig.RoutingKey_S1,TopicExchangeConfig.RoutingKey_S2
		sender.send(routingKey,"Hello world "+System.currentTimeMillis());
		sender.send(routingKey+".user","Hello World "+System.currentTimeMillis());
		sender.send(routingKey+".user.role","Hello World "+System.currentTimeMillis());
    }
	
	// config
	@Configuration
	public class TopicExchangeConfig{
		public final static String RoutingKey_S1="topic.s1";
		public final static String RoutingKey_S2="topic.s2";
		public final static String Exchange_Name="topicExchange";
		
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
		public TopicExchange exchange(){
			return new TopicExchange(Exchange_Name);
		}
		
		// Binding
		@Bean
		public Binding bindingS1QueueAndExchange(Queue s1Queue, TopicExchange exchange){
			return BindingBuilder.bind(s1Queue).to(exchange).with(RoutingKey_S1+".*");
		}
		@Bean
		public Binding bingS2QueueAndExchange(Queue s2Queue,TopicExchange exchange){
			return BindingBuilder.bind(s2Queue).to(exchange).with(RoutingKey_S2+".#");
		}
	}
	
	// sender
	@Component
	public class QueueSender{
		@Autowired
	    private AmqpTemplate rabbitTemplate;
		
		public void send(String routingKey,String msg){
			System.out.println("send routingKey("+routingKey+") :"+msg);
			rabbitTemplate.convertAndSend(TopicExchangeConfig.Exchange_Name, routingKey, msg);
		}
	}
	
	// receiver
	@Component
	public class QueueReceiver{
		@RabbitHandler
	    @RabbitListener(queues = {TopicExchangeConfig.RoutingKey_S1,TopicExchangeConfig.RoutingKey_S2,"123"})
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
