package com.cj.rabbit.demo2;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotifyHelloSender {
	
	@Autowired
    private AmqpTemplate rabbitTemplate;
     
    public void send(String msg){
    	System.out.println("notify.hello send message: "+msg);
        rabbitTemplate.convertAndSend("notify.hello", msg);
    }
}
