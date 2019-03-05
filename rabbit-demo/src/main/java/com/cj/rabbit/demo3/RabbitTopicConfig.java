package com.cj.rabbit.demo3;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopicConfig {
	public final static String RouteKey_S1="api.s1";
	public final static String RouteKey_S2="api.s2";
	public final static String Exchange_Name="apiExchange";
	
	// Queue
	@Bean
	public Queue s1Queue(){
		return new Queue(RouteKey_S1);
	}
	@Bean
	public Queue s2Queue(){
		return new Queue(RouteKey_S2);
	}
	
	// Exchange
	@Bean
	public TopicExchange exchange(){
		return new TopicExchange(Exchange_Name);
	}
	
	// Binding
	@Bean
	public Binding bindingS1Exchange(Queue s1Queue, TopicExchange exchange){
		return BindingBuilder.bind(s1Queue).to(exchange).with(RouteKey_S1+".*");
	}
	@Bean
	public Binding bingS2Exchange(Queue s2Queue,TopicExchange exchange){
		return BindingBuilder.bind(s2Queue).to(exchange).with(RouteKey_S2+".#");
	}
}
