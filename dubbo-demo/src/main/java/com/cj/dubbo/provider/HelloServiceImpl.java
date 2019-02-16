package com.cj.dubbo.provider;

import org.springframework.stereotype.Component;

import com.cj.dubbo.service.HelloService;

@Component("helloService")
public class HelloServiceImpl implements HelloService {

	@Override
	public String sayHello(String name) {
		return "Hello, " + name + " (from Spring Boot)";
	}

}
