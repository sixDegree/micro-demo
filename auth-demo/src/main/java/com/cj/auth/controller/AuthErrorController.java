package com.cj.auth.controller;

import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cj.auth.util.MicroResponse;

@RestController
public class AuthErrorController implements ErrorController{

	@Override
	public String getErrorPath() {
		return "/error";
	}

	@RequestMapping(value="/error")
    public Object onError(HttpServletResponse rs,Exception ex){
	   HttpStatus status=HttpStatus.resolve(rs.getStatus());
	   if(status!=null)
		   return new MicroResponse(false,rs.getStatus(),status.getReasonPhrase());
	   else
		   return MicroResponse.fail(ex.getMessage());
    }
}
