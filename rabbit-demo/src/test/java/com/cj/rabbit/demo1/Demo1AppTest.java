package com.cj.rabbit.demo1;

import java.io.IOException;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.cj.rabbit.demo1.entity.Order;
import com.cj.rabbit.demo1.producer.OrderSender;

@RunWith(SpringRunner.class)
@SpringBootTest
public class Demo1AppTest{
	
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
	
//	@Autowired
//	private OrderReceiver orderReceiver;
	
	@Test
	public void testOrderReceive() throws IOException{
		System.in.read();
	}
}