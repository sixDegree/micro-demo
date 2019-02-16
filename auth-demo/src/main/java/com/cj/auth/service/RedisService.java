package com.cj.auth.service;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {		
	
	@Autowired
	private RedisTemplate<Object, Object> jsonRedisTemplate;
	
	public Object get(String key) {
		return jsonRedisTemplate.opsForValue().get(key);
	}	
	public void set(String key, Object value) {
		jsonRedisTemplate.opsForValue().set(key, value);
	}
	public void set(String key,Object value,int timeout){
		jsonRedisTemplate.opsForValue().set(key, value,timeout,TimeUnit.SECONDS);
	}
	public Boolean delete(String key){
		return jsonRedisTemplate.delete(key);
	}
	public void expire(String key,int timeout){
		jsonRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
	}
}
