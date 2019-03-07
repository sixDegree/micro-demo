package com.cj.rabbit.demo3;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
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

/* 
 * Headers Exchange：
 * 根据请求消息中设置的header attribute参数类型（一组键值对）匹配队列（和routingKey没有关系）
 * 
 * Producer -> Exchange -> (headers) -> Queue -> Consumer
 * 
 * case:
 * binding: 
 * 		headersExchange - headers("from":"Tom" && "to":"Lucy") - queue(headers.s1)
 * 		headersExchange - headers("from":"Tom" || "to":"Lucy") - queue(headers.s2)
 * 		defaultExchange - routingKey:123 - queue(123)
 * send to HeadersExchange with routingKey:123,headers.s1,headers.s1.user
 * 		msg with headers("from":"Tom" && "to":"Lucy") -> consume queue(headers.s1) & queue(headers.s2)
 * 		msg with headers("from":"Tom" ||"to":"Lucy")  -> consume queue(headers.s2)
 * 		msg with headers( !"from":"Tom" && ! "from":"Tom") -> no consume
 * 
 * */
@SpringBootApplication
public class HeadersExchangeDemoApp {
	public static void main( String[] args ) throws IOException, InterruptedException{
		ConfigurableApplicationContext ctx = SpringApplication.run(HeadersExchangeDemoApp.class, args);
		
		QueueSender sender=ctx.getBean(QueueSender.class);
		
		Map<String,Object> headers=new HashMap<String,Object>();
		//case1: send to s1,s1.user,s1.user.role => s1 & s2 receive !
//		headers.put("from", "Tom");
//		headers.put("to", "Lucy");
//		headers.put("cc", "Susan");
//		//case2.1: send to s1,s1.user,s1.user.role => s1 no receive & s2 receive !
		headers.put("from", "Tom");
		headers.put("to", "Lucy2");
//		//case2.2: send to s1,s1.user,s1.user.role => s1 no receive & s2 receive !
//		headers.put("from", "Tom");
		//case3: send to s1,s1.user,s1.user.role => s1 & s2 no receive !
//		headers.put("from", "Tom1");
//		headers.put("to", "Lucy1");
		
		String routingKey="123";	// HeadersExchangeConfig.RoutingKey_S1
		sender.send(routingKey,"Hello world "+System.currentTimeMillis(),headers);
		sender.send(routingKey+".user","Hello World "+System.currentTimeMillis(),headers);
		sender.send(routingKey+".user.role","Hello World "+System.currentTimeMillis(),headers);
	}
	
	// config
	@Configuration
	public class HeadersExchangeConfig{
		public static final String RoutingKey_S1="headers.s1";
		public static final String RoutingKey_S2="headers.s2";
		public static final String Exchange_Name="headersExchange";
		
		// queue
		@Bean
		public Queue s1Queue() {
			return new Queue(RoutingKey_S1);
		}
		@Bean
		public Queue s2Queue() {
			return new Queue(RoutingKey_S2);
		}
		@Bean
		public Queue testQueue() {
			return new Queue("123");
		}
		
		// exchange
		@Bean
		public HeadersExchange exchange() {
			return new HeadersExchange(Exchange_Name);
		} 
		
		// binding
		@Bean
		public Binding bindS1QueueAndExchange(Queue s1Queue,HeadersExchange exchange) {
			Map<String,Object> headerMap=new HashMap<String,Object>();
			headerMap.put("from", "Tom");
			headerMap.put("to", "Lucy");
			return BindingBuilder.bind(s1Queue).to(exchange).whereAll(headerMap).match();
		}
		@Bean
		public Binding bindS2QueueAndExchange(Queue s2Queue,HeadersExchange exchange) {
			Map<String,Object> headerMap=new HashMap<String,Object>();
			headerMap.put("from", "Tom");
			headerMap.put("to", "Lucy");
			return BindingBuilder.bind(s2Queue).to(exchange).whereAny(headerMap).match();
		}
	}
	
	// sender
	@Component
	public class QueueSender{
		 @Autowired
		 private AmqpTemplate rabbitTemplate;
		 
		 public void send(String routingKey,String msg,Map<String,Object> headers){
			Message message = MessageBuilder.withBody(msg.getBytes()).copyHeaders(headers).build();
			System.out.println("send routingKey("+routingKey+") :"+(new String(message.getBody()))+" "+message.getMessageProperties());
			rabbitTemplate.convertAndSend(HeadersExchangeConfig.Exchange_Name, routingKey, message);
		}
	}
	
	// receiver
	@Component
	public class QueueReceiver{
		@RabbitHandler
	    @RabbitListener(queues = {HeadersExchangeConfig.RoutingKey_S1,HeadersExchangeConfig.RoutingKey_S2,"123"})
		public void receive(@Payload byte[] msg,@Headers Map<String,Object> headers,Channel channel) 
				throws IOException {
			System.out.println("consume queue("+headers.get(AmqpHeaders.CONSUMER_QUEUE)+")"
					+" routingKey("+headers.get(AmqpHeaders.RECEIVED_ROUTING_KEY)+")"
					+" msg: "+(new String(msg))
					+", headers:"+headers);
			Long deliveryTag=(Long)headers.get(AmqpHeaders.DELIVERY_TAG);
			channel.basicAck(deliveryTag, false);
	    }
	}
}
