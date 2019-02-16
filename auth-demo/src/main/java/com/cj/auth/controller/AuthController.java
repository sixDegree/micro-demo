package com.cj.auth.controller;

import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.cj.auth.entity.User;
import com.cj.auth.service.RedisService;
import com.cj.auth.service.UserService;
import com.cj.auth.util.MD5Utils;
import com.cj.auth.util.MicroResponse;

@RestController
public class AuthController {
	
	@Value("${auth.principalHeader}")
	private String principalHeader;
	
	@Value("${auth.expireTime}")
	private int expireTime;
	
	@Autowired
	private RedisService redisService;

	@Autowired
	private UserService userService;
	
	@GetMapping("/")
	public Object index(){
		return ResponseEntity.ok("This is micro-authService!");
	}
	
	@PostMapping("/login")
	public Object login(@RequestBody User loginUser,
			@RequestHeader(name="${auth.usersessionHeader}") String sessionKey, 
			@RequestHeader(name="${auth.principalHeader}",required=false) String principle,
			HttpServletResponse response){
		// db: verify 
		if(loginUser==null || StringUtils.isAnyBlank(loginUser.getName(),loginUser.getPassword()))
			return MicroResponse.InvalidRequest;
		
		// check principle
		if(principle!=null){
			User user=(User)this.redisService.get(sessionKey+":"+principle);
			if(user!=null 
					&& loginUser.getName().equals(user.getName()) 
					&& MD5Utils.getMD5Str(loginUser.getPassword()).equals(user.getPassword())){
				this.redisService.expire(sessionKey+":"+principle, expireTime);
				this.redisService.expire(sessionKey+":"+user.getId(), expireTime);
				response.setHeader(principalHeader, principle);
				return MicroResponse.success("login success");
			}
		}
		
		// check name & password
		User user=userService.findByNameAndPassword(loginUser);
		if(user==null)
			return MicroResponse.AuthenticationFail;

		// delete other clients login
		String oldToken = (String)this.redisService.get(sessionKey+":"+user.getId());
		if(user!=null)
			this.redisService.delete(sessionKey+":"+oldToken);
		this.redisService.delete(sessionKey+":"+user.getId());
		
		// set new
		String token = UUID.randomUUID().toString();
		this.redisService.set(sessionKey+":"+token,user,expireTime);
		this.redisService.set(sessionKey+":"+user.getId(),token,expireTime);
		response.setHeader(principalHeader, token);
		return MicroResponse.success("login success");
	}
	
	@GetMapping("/logout")
	public Object logout(@RequestHeader(name="${auth.principalHeader}") String principle,@RequestHeader(name="${auth.usersessionHeader}") String sessionKey){
		User user=(User)this.redisService.get(sessionKey+":"+principle);
		if(user==null)
			return MicroResponse.success("logout success");

		String token=(String)this.redisService.get(sessionKey+":"+user.getId());
		if(token==null)
			return MicroResponse.success("logout success");
		
		if(principle.equals(token)){
			this.redisService.delete(sessionKey+":"+user.getId());
			this.redisService.delete(sessionKey+":"+token);
			return MicroResponse.success("logout success");
		}
		return MicroResponse.fail("logout fail");
	}
	
	@GetMapping("/authentication")
	public Object getAuthentication(@RequestHeader(name="${auth.principalHeader}") String principle,@RequestHeader(name="${auth.usersessionHeader}") String sessionKey){
		return MicroResponse.success(redisService.get(sessionKey+":"+principle));
	}

	@PostMapping("/regist")
	public Object regist(@RequestBody User user){
		if(user==null || StringUtils.isAnyBlank(user.getName(),user.getPassword()))
			return MicroResponse.InvalidRequest;
		boolean result=userService.save(user);
		return new MicroResponse(result,result?1:0,user);
	}
}
