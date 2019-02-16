package com.cj.zuul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.discovery.PatternServiceRouteMapper;
import org.springframework.context.annotation.Bean;

import com.cj.zuul.provider.MyFallbackProvider;

@EnableZuulProxy  // include: @EnableDiscoveryClient & @EnableCircuitBreaker
@SpringBootApplication
public class MicroZuulApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroZuulApplication.class, args);
	}
	
//	@Bean 
//	public MyFilter myFilter() { 
//		return new MyFilter(); 
//	}
	
	@Bean
	public MyFallbackProvider myFallbackProvider() {
		return new MyFallbackProvider();
	}

//	@Bean
//	public PatternServiceRouteMapper serviceRouteMapper() {
//		// servicePatterh,routePattern
//	    return new PatternServiceRouteMapper(
//	        "(?<servicename>^.+)/(?<contextPath>v.+$)",
//	        "${contextPath}");
//	}
	
// No need:
//	@Bean
//	@LoadBalanced
//	public RestTemplate restTemplate(){
//		return new RestTemplate();
//	}
}
