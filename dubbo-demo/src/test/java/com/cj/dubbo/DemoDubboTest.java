package com.cj.dubbo;

import java.io.IOException;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.cj.dubbo.consumer.HelloConsumerService;
import com.cj.dubbo.service.DemoService;
import com.cj.dubbo.service.HelloService;

public class DemoDubboTest {
	
	/*
	 * dependencies:
	 * dubbo ( include spring)
	 * curator (include zookeeper --> need to change zookeeper version,beta version has issues)
	 * 
	 * ref: http://dubbo.apache.org/#!/docs/user/quick-start.md?lang=en-us
	 * 
	 * */
	
	@Test
	public void runDemoProvider() throws IOException {
		System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"demo-provider.xml"});
        context.start();
        System.out.println("Provider started.");
        System.in.read(); // press any key to exit
	}
	
	@Test
	public void runDemoConsumer() {
//		System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"demo-consumer.xml"});
        context.start();
        DemoService demoService = (DemoService) context.getBean("demoService"); // obtain proxy object for remote invocation
        String hello = demoService.sayHello("world"); // execute remote invocation
        System.out.println(hello); // show the result
	}
	
	@Test
	public void runHelloConsumer() {
//		System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"hello-consumer.xml"});
        context.start();
        HelloService helloService = (HelloService) context.getBean("helloService"); // obtain proxy object for remote invocation
        String hello=helloService.sayHello("Girl");
        System.out.println(hello);
        
        System.out.println("----Next----");
        HelloConsumerService consumer=context.getBean(HelloConsumerService.class);
        consumer.printSay("Boy");
        
	}
}
