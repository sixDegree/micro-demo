package com.cj.auth.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Repository;

import com.cj.auth.entity.User;
import com.cj.auth.util.MD5Utils;

@Repository
public class UserRepository {
	
	//private final ConcurrentMap<Integer,User> users = new ConcurrentHashMap<Integer,User>();
	private final ConcurrentMap<String,User> users=new ConcurrentHashMap<String,User>();
    private final static AtomicInteger idGenerator = new AtomicInteger();
    
    @PostConstruct
    public void init(){
    	users.put("admin", new User(idGenerator.incrementAndGet(),"admin",MD5Utils.getMD5Str("admin123")));
    }
    
    public boolean save(User user){
        Integer id = idGenerator.incrementAndGet();
        user.setId(id);
		user.setPassword(MD5Utils.getMD5Str(user.getPassword()));
        return users.put(user.getName(),user)==null;
    }
    
    public Collection<User> list(){
        return users.values();
    }
    
    public Optional<User> findByNameAndPassword(String name,String password){
    	User user=users.get(name);
    	if(user!=null && user.getPassword().equals(MD5Utils.getMD5Str(password)))
    		return Optional.of(user);
    	return Optional.empty();
    }
    
    public boolean existsByName(String name){
    	return users.containsKey(name);
    }
}
