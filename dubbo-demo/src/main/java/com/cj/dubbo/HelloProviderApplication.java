package com.cj.dubbo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HelloProviderApplication {
	
	public static void main(String[] args) {
        SpringApplication.run(HelloProviderApplication.class,args);
        System.out.println("Start Application");
    }
}
