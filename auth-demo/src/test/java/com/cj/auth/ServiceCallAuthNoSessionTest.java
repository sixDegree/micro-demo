package com.cj.auth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServiceCallAuthNoSessionTest {

	@Autowired
	private TestRestTemplate restTemplate;
	private String authURL="http://localhost:8080/micro-auth/session";
	private String cookie="SESSION=ZjFjMWIzYjQtMGE0NC00MWJmLThkODYtZmMxODhmMmJkNGE0; Path=/micro-auth/; HttpOnly";
	
	private String name="admin";
	private String password="admin123";
	
	@Test
	public void callTest(){
		
		//getAuthentication
		callGetAuthentication();
				
		// login
		callLoginTest();
		
		// callGetAuthentication
		callGetAuthentication();
		
		// logout
		callLogoutTest();
		
		// getAuthentication
		callGetAuthentication();
	}
	
	@Test
	public void callLoginTest(){
		System.out.println("call login...");
		
		HttpHeaders headers = new HttpHeaders(); 
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		Map<String,String> userMap=new HashMap<String,String>();
		userMap.put("name", name);
		userMap.put("password", password);
		
		HttpEntity<Map> entity=new HttpEntity<Map>(userMap,headers);
		HttpEntity<Map> response=restTemplate.exchange(authURL+"/login",HttpMethod.POST,entity,Map.class);
		System.out.println(response);
		
		cookie=(String)response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
		System.out.println("cookie:"+cookie);
	}
	
	@Test
	public void callLogoutTest(){
		System.out.println("call logout...");
		HttpHeaders headers = new HttpHeaders(); 
		headers.set(HttpHeaders.COOKIE,cookie); 
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity=new HttpEntity<String>(null,headers);
		HttpEntity<String> response=restTemplate.exchange(authURL+"/logout",HttpMethod.GET,entity,String.class);
		System.out.println(response);
	}
	
	@Test
	public void callGetAuthentication(){
		System.out.println("call getAuthentication...");
		HttpHeaders headers = new HttpHeaders(); 
		headers.set(HttpHeaders.COOKIE,cookie); 
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity=new HttpEntity<String>(null,headers);
		HttpEntity<Map> response=restTemplate.exchange(authURL+"/authentication",HttpMethod.GET,entity,Map.class);
		System.out.println(response);
	}
	
}
