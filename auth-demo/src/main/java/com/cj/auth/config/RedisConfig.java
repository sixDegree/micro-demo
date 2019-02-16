package com.cj.auth.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Configuration
//@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 180,
//redisFlushMode=RedisFlushMode.ON_SAVE,
//redisNamespace="sps")
public class RedisConfig {
	
	@Bean
	public RedisTemplate<Object,Object> jsonRedisTemplate(RedisConnectionFactory redisConnectionFactory){
		RedisTemplate<Object, Object> template = new RedisTemplate<Object,Object>();
		template.setConnectionFactory(redisConnectionFactory);
		
		RedisSerializer<String> stringSerializer = new StringRedisSerializer();
		template.setKeySerializer(stringSerializer);
		template.setHashKeySerializer(stringSerializer);
		
		Jackson2JsonRedisSerializer<Object>	jsonSerializer = initJsonSerializer();
		template.setValueSerializer(jsonSerializer);
		template.setHashValueSerializer(jsonSerializer);
		
		template.afterPropertiesSet();
		
		System.out.println("Create Customer JsonRedisTemplate-----");
		return template;
	}
	
	private Jackson2JsonRedisSerializer<Object> initJsonSerializer(){
		Jackson2JsonRedisSerializer<Object>	jsonSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
		ObjectMapper objectMapper = initObjectMapper();
		jsonSerializer.setObjectMapper(objectMapper);
		return jsonSerializer;
	}
	private ObjectMapper initObjectMapper(){
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); 
		objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		//objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
		return objectMapper;
	}
	
	/* SessionRepository: 
	 * 定义了创建、保存、删除以及检索session的方法
	 * (将Session实例真正保存到数据存储的逻辑是在这个接口的实现中编码完成的)
	 * 
	 * RedisOperationsSessionRepository implements SessionRepository:
	 * 会在Redis中创建、存储和删除session
	 * 
	 * {@link RedisHttpSessionConfiguration} 
	*/
	
//	@Bean
//	public SessionRepository<?> sessionRepository(RedisConnectionFactory factory){
//	    RedisOperationsSessionRepository sessionRepository =  new RedisOperationsSessionRepository(jsonRedisTemplate(factory));
//	//    FastJsonRedisSerializer<Object> fastJsonRedisSerializer = new FastJsonRedisSerializer<>(Object.class);
//	//    sessionRepository.setDefaultSerializer(fastJsonRedisSerializer);
//		sessionRepository.setDefaultMaxInactiveInterval(180);
//		sessionRepository.setRedisKeyNamespace("sps");
//		sessionRepository.setRedisFlushMode(RedisFlushMode.ON_SAVE);
//		System.out.println("Create Customer RedisOperationsSessionRepository --- ");
//	    sessionRepository.setDefaultSerializer(initJsonSerializer());
//	    return sessionRepository;
//	}
	
//	@SuppressWarnings("unchecked")
//	@Bean
//	public SessionRepository<?> sessionRepository( @Qualifier("jsonRedisTemplate") RedisOperations<Object, Object> redisTemplate){
//		RedisOperationsSessionRepository sessionRepository =  new RedisOperationsSessionRepository(redisTemplate);
//		// sessionRepository.setDefaultSerializer(initJsonSerializer());
//		sessionRepository.setDefaultSerializer((RedisSerializer<Object>) redisTemplate.getValueSerializer());
//		sessionRepository.setDefaultMaxInactiveInterval(180);
//		sessionRepository.setRedisKeyNamespace("sps");
//		sessionRepository.setRedisFlushMode(RedisFlushMode.ON_SAVE);
//		System.out.println("Create Customer RedisOperationsSessionRepository --- ");
//		return sessionRepository;
//	}
	
	@Bean
	public RedisSerializer<Object> springSessionDefaultRedisSerializer(@Qualifier("jsonRedisTemplate") RedisOperations<Object, Object> redisTemplate){
		return (RedisSerializer<Object>)redisTemplate.getValueSerializer();
	}
}
