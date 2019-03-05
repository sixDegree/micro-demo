package com.cj.rabbit.demo2;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Demo2App {
	public static void main( String[] args ) throws IOException{
		ConfigurableApplicationContext ctx = SpringApplication.run(Demo2App.class, args);
		NotifyHelloSender sender=ctx.getBean(NotifyHelloSender.class);
		sender.send("Hello world "+System.currentTimeMillis());
    }
}
