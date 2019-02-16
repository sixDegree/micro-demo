package com.cj.auth.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cj.auth.entity.User;
import com.cj.auth.service.UserService;
import com.cj.auth.util.MicroResponse;

@RestController
@RequestMapping("/session")
public class AuthSessionController {

	@Autowired
	private UserService userService;
	
	@GetMapping("/")
	public Object index(){
		return ResponseEntity.ok("This is micro-authService using springsession!");
	}
	
	@PostMapping("/login")
	public Object login(@RequestBody User loginUser,HttpServletRequest request){
		if(loginUser==null || StringUtils.isAnyBlank(loginUser.getName(),loginUser.getPassword()))
			return MicroResponse.InvalidRequest;
		
		// check name & password
		User user=userService.findByNameAndPassword(loginUser);
		if(user==null)
			return MicroResponse.AuthenticationFail;
				
		HttpSession session=request.getSession();
		session.setAttribute(session.getId(), user);
		System.out.println(session.getId());
		return MicroResponse.success(user);
	}
	
	@GetMapping("/logout")
	public Object logout(HttpServletRequest request){
		HttpSession session=request.getSession(false);
		if(session!=null){
			session.removeAttribute(session.getId());
			System.out.println(session.getId());
		}else
			System.out.println("logout: session is null");
		return MicroResponse.OK;
	}
	
	@GetMapping("/authentication")
	public Object getAuthentication(HttpServletRequest request /*HttpSession session*/){
		HttpSession session=request.getSession(false);
		if(session!=null){
			System.out.println(session.getId());
			return MicroResponse.success(session.getAttribute(session.getId()));
		}
		System.out.println("getAuthentication: session is null");
		return MicroResponse.success(null);
	}
}
