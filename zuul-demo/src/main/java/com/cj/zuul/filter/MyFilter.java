package com.cj.zuul.filter;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

public class MyFilter extends ZuulFilter {

	private static Logger log = LoggerFactory.getLogger(MyFilter.class);
	
	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() throws ZuulException {
		
		RequestContext ctx = RequestContext.getCurrentContext(); 
		HttpServletRequest request = ctx.getRequest(); 
		log.info(String.format("%s -- %s", request.getMethod(), request.getRequestURL().toString())); 

		String token = request.getParameter("token");// 获取请求的参数 
		if (token!=null && token.length()>0) { 
			ctx.setSendZuulResponse(true); //对请求进行路由 
			ctx.setResponseStatusCode(200); 
			ctx.set("success", true); 
		} else { 
			ctx.setSendZuulResponse(false); //不对其进行路由 
			ctx.setResponseStatusCode(400); 
			ctx.setResponseBody("token is empty"); 
			ctx.set("success", false); 
		}
		
		return null;
	}

	@Override
	public String filterType() {
		//Zuul内置的filter类型有四种，pre, route，post，error，分别代表请求处理前，处理时，处理后和出错后
		return "pre";
	}

	@Override
	public int filterOrder() {
		//指定了该过滤器执行的顺序
		return 1;
	}

}
