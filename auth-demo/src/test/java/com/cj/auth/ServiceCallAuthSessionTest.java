package com.cj.auth;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServiceCallAuthSessionTest {

	@Autowired
	private TestRestTemplate restTemplate;
	
	private String authURL="http://localhost:8080/micro-auth";
	private String principalHeader="micro-auth";
	private String usersessionHeader="usersession";
	private String usersessionKey="s1";
	
	String principle="a37377ec-dc92-41f8-95af-4d0a494f3eb8";
	
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
		headers.set(usersessionHeader,usersessionKey); 
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		Map<String,String> userMap=new HashMap<String,String>();
		userMap.put("name", "Tom");
		userMap.put("password", "123123");
		
		HttpEntity<Map> entity=new HttpEntity<Map>(userMap,headers);
		HttpEntity<Map> response=restTemplate.exchange(authURL+"/login",HttpMethod.POST,entity,Map.class);
		System.out.println(response);
		
		principle=(String)response.getHeaders().getFirst(principalHeader);
		System.out.println("token:"+principle);
	}
	
	@Test
	public void callLogoutTest(){
		System.out.println("call logout...");
		HttpHeaders headers = new HttpHeaders(); 
		headers.set(principalHeader,principle); 
		headers.set(usersessionHeader,usersessionKey); 
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity=new HttpEntity<String>(null,headers);
		HttpEntity<String> response=restTemplate.exchange(authURL+"/logout",HttpMethod.GET,entity,String.class);
		System.out.println(response);
	}
	
	@Test
	public void callGetAuthentication(){
		System.out.println("call getAuthentication...");
		HttpHeaders headers = new HttpHeaders(); 
		headers.set(principalHeader,principle); 
		headers.set(usersessionHeader,usersessionKey); 
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity=new HttpEntity<String>(null,headers);
		HttpEntity<Map> response=restTemplate.exchange(authURL+"/authentication",HttpMethod.GET,entity,Map.class);
		System.out.println(response);
	}
	
}
