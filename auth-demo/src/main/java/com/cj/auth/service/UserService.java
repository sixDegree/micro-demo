package com.cj.auth.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cj.auth.entity.User;
import com.cj.auth.repository.UserRepository;

@Service
public class UserService {
	@Autowired
	private UserRepository userRepository;

	public User findByNameAndPassword (User user){
		Optional<User> result= this.userRepository.findByNameAndPassword(user.getName(),user.getPassword());
		if(result.isPresent())
			return result.get();
		return null;
	}

	public boolean save(User user){
		return this.userRepository.save(user);
	}
}
