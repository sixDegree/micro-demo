package com.cj.rabbit.demo;

import java.io.IOException;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.cj.rabbit.demo.entity.Order;
import com.cj.rabbit.demo.producer.OrderSender;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DemoAppTest{
	
	@Autowired
	private OrderSender orderSender;

	@Test
	public void testOrderSend() throws Exception{
		Order order=new Order();
		order.setId("201901070002");
		order.setName("Test Order2");
		order.setMsgId(System.currentTimeMillis()+"$"+UUID.randomUUID().toString());
		orderSender.send(order);
	}
	
	@Test
	public void testOrderReceiver() throws IOException{
		System.in.read();
	}
}