package com.cj.rabbit.demo2;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = "notify.hello")
public class NotifyHelloReceiver {
	
	@RabbitHandler
    public void receive(String msg) {
		System.out.println("notify.hello receive message: "+msg);
    }
}
