package com.cj.dubbo.consumer;

import com.cj.dubbo.service.HelloService;

public class HelloConsumerService {

	private HelloService helloService;
	
	public HelloService getHelloService() {
		return helloService;
	}

	public void setHelloService(HelloService helloService) {
		this.helloService = helloService;
	}

	public void printSay(String name) {
		if(helloService==null)
			System.out.println("Can't get helloService!");
		System.out.println(helloService.sayHello(name));
	}
}
