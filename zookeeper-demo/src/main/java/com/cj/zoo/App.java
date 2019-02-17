package com.cj.zoo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cj.zoo.service.ZkCuratorService;

@SpringBootApplication
@Configuration
public class App {
    public static void main( String[] args ){
        SpringApplication.run(App.class, args);
    }
    
    @Bean(name="zkService")
    public ZkCuratorService zkService(){
    	return new ZkCuratorService();
    }
    
    @Bean(name="zkClientService")
    public ZkCuratorService zkClientService(){
    	return new ZkCuratorService();
    }
}
