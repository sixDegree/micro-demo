package com.cj.eureka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.discovery.EurekaClient;

@SpringBootApplication
@EnableEurekaClient
@RestController
public class EurekaClientApp{
    public static void main( String[] args ){
        SpringApplication.run(EurekaClientApp.class, args);
    }
    
    @RequestMapping("/")
    public String index() {
    	return "Hello World";
    }
    
    @RequestMapping("/say")
    public String say(){
    	return "Say Hello world";
    }
    
//  @Autowired
//  @Lazy
//  private EurekaClient eurekaClient;
//
//  @Value("${spring.application.name}")
//  private String appName;
//
//    @RequestMapping("/greeting")
//    public String greeting() {
//        return String.format("Hello from '%s'!", eurekaClient.getApplication(appName).getName());
//    }
}
