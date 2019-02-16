package com.cj.dubbo.service;

public class DemoServiceImpl implements DemoService {

	@Override
	public String sayHello(String name) {
		return "Hello "+name;
	}
	
}
