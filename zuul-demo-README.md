## 智能路由 Zuul

Zuul
- 是Netflix开源的微服务网关组件(路由转发 + 过滤器)
- 可以和Eureka、Ribbon、Hystrix等组件配合使用，提供动态路由，监控，弹性，安全等的边缘服务，其核心是一些列的Filter
- Zuul可实现对内/对外路由，内部服务互相调用还可通过Eureka进行服务发现和调用
	+ 通过URL映射来实现路由有局限性(例如每增加一个服务就需要配置一条内容；服务本身被调度到其他节点Zuul无法感知）
	+ 利用Eureka注册服务，配置Zuul从Eureka Server获取服务的地址并且基于所有服务的实例进行轮询/熔断/重试更好
	+ 在实现微服务架构时，服务名与服务实例地址的关系在eureka server中已经存在了,只需要将Zuul注册到eureka即可
	+ 注：对于使用`service-id`的路由，Zuul会以均衡负载（Ribbon）的方式访问服务
- 路由规则：
	+ URL(path,url,prefix,strip-prefix)
	+ Service(path,service-id,prefix,strip-prefix)
	+ `strip-prefix`: 是否过滤掉前缀（true则过滤掉）
		* eg: `path: /myusers/**`，默认时转发到服务的请求是`/**`，如果`stripPrefix`为`false`，则转发的请求是`/myusers/**`
	+ `prefix`: 对path增加一个前缀，可加在全局zuul下，也可加在某个路由规则下面
	+ `service-id`: 服务名，服务发现中的服务
	+ `ignored-services`: 注意匹配了忽略的列表, 但却明确的配置在路由列表中的路由不会被忽略
- 路由熔断: 可以通过写自定义的fallback方法，并且将其指定给某个route,来实现该route访问出问题的熔断处理
	```java
	public interface FallbackProvider {
		// 告诉 Zuul 它是负责哪个 route 定义的熔断
		public String getRoute();												
		// 告诉 Zuul 断路出现时，它会提供一个什么返回值来处理请求
		ClientHttpResponse fallbackResponse(String route, Throwable cause);		
	}
	```
- 在Spring Cloud体系中，Spring Cloud Zuul就是提供负载均衡、反向代理、权限认证的一个`API gateway`
- Note: `API Gateway` 是介于客户端和服务器端之间的中间层，所有的外部请求都会先经过这一层，作用
	+ 简化客户端调用复杂度
	+ 数据裁剪以及聚合
	+ 多渠道支持 （针对不同的渠道和客户端提供不同的API Gateway,eg: BFF Backend for front-end）


### Starter

1. pom.xml
```xml
<properties>
	<spring-cloud.version>Finchley.RELEASE</spring-cloud.version>
</properties>
<dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-dependencies</artifactId>
			<version>${spring-cloud.version}</version>
			<type>pom</type>
			<scope>import</scope>
		</dependency>
	</dependencies>
</dependencyManagement>
<dependencies>
	<dependency>
		<groupId>org.springframework.cloud</groupId>
		<artifactId>spring-cloud-starter-netflix-zuul</artifactId>
	</dependency>
	<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-web</artifactId>
	</dependency>
</dependencies>
```

2. resources/application.yml
```
server:
  port: 8888
  servlet:
    context-path: /zuul-demo

spring:
  application:
    name: zuul-demo

zuul:
  routes:
    baidu:
      path: /baidu/**
      url: http://www.baidu.com
    csdn:
      path: /csdn/**
      url: https://www.csdn.net/
```

3. main
```java
@EnableZuulProxy 	// include: @EnableDiscoveryClient, @EnableCircuitBreaker
@SpringBootApplication
public class MicroZuulApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroZuulApplication.class, args);
	}
}
```

4. Visit: `http://localhost:8888/zuul-demo`
	- `/baidu`
	- `/csdn`


### Filter

1. Filter Definition
```java
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
```

2. Inject Filter Bean
```java
@EnableZuulProxy
@SpringBootApplication
public class MicroZuulApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroZuulApplication.class, args);
	}

	// inject Filter
	@Bean 
	public MyFilter myFilter() { 
		return new MyFilter(); 
	}
}
```

3. Visit: `http://localhost:8888/zuul-demo`
	- `/baidu`
	- `/baidu?token=123`


### Service

这里通过Eureka中已经注册的服务名,调用服务

1. Prepare [Eureka](https://github.com/sixDegree/micro-demo)
	- Eureka Server(eureka-server): `http://localhost:8761/eureka-server` 
	- Eureka Client(eureka-client): `http://localhost:8080/eureka-client` (`/`,`/say`,`/actuator`)

2. pom.xml: 添加`spring-cloud-starter-netflix-eureka-client`
```xml
<properties>
	<spring-cloud.version>Finchley.RELEASE</spring-cloud.version>
</properties>
<dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-dependencies</artifactId>
			<version>${spring-cloud.version}</version>
			<type>pom</type>
			<scope>import</scope>
		</dependency>
	</dependencies>
</dependencyManagement>
<dependencies>
	<dependency>
		<groupId>org.springframework.cloud</groupId>
		<artifactId>spring-cloud-starter-netflix-zuul</artifactId>
	</dependency>
	<dependency>
		<groupId>org.springframework.cloud</groupId>
		<artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
	</dependency>
	<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-web</artifactId>
	</dependency>
</dependencies>
```

2. resource/application.xml
```
server:
  port: 8888
  servlet:
    context-path: /zuul-demo

spring:
  application:
    name: zuul-demo

# 注册到eureka服务中心
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka-server/eureka 

zuul:
  routes:
    baidu:
      path: /baidu/**
    csdn:
      path: /csdn/**
      url: https://www.csdn.net/
    eurekaServer:
      path: /eurekaServer/**
      url: http://localhost:8761/eureka-server/
    serviceTest1:	# http://localhost:8888/zuul-demo/eureka-client/say
      path: /eureka-client/**
      service-id: eureka-client
      strip-prefix: false   
    serviceTest2:	# http://localhost:8888/zuul-demo/ec/eureka-client/say
      path: /ec/**
      service-id: eureka-client
```

3. FallbackProvider: 熔断处理，eg： 未发现eureka-client service，则会调用此处理
```java
public class MyFallbackProvider implements FallbackProvider{

	@Override
	public String getRoute() {
		return "*";
	}

	@Override
	public ClientHttpResponse fallbackResponse(String route, Throwable cause) {
		 return new ClientHttpResponse() {
	            @Override
	            public HttpStatus getStatusCode() throws IOException {
	                return HttpStatus.OK;
	            }
	            @Override
	            public int getRawStatusCode() throws IOException {
	                return 200;
	            }
	            @Override
	            public String getStatusText() throws IOException {
	                return "OK";
	            }
	            @Override
	            public void close() {
	            }
	            @Override
	            public InputStream getBody() throws IOException {
	                return new ByteArrayInputStream("This is my fallback response".getBytes());
	            }
	            @Override
	            public HttpHeaders getHeaders() {
	                HttpHeaders headers = new HttpHeaders();
	                headers.setContentType(MediaType.APPLICATION_JSON);
	                return headers;
	            }
	        };
	}
}
```

4. main
```java
@EnableZuulProxy
@SpringBootApplication
public class MicroZuulApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroGatewayApplication.class, args);
	}

	@Bean
	public MyFallbackProvider myFallbackProvider() {
		return new MyFallbackProvider();
	}

/* service route 映射规则修改
   eg： service: erueka-client => route: eureka/client
*/
//	@Bean
//	public PatternServiceRouteMapper serviceRouteMapper() {
//		// servicePatterh,routePattern
//	    return new PatternServiceRouteMapper(
//	        "(?<servicename>^.+)-(?<contextPath>v.+$)",
//	        "${servicename}/${contextPath}");
//	}
}
```

5. Visit: `http://localhost:8888/zuul-demo`
	- `/eureka-client/`,`/eureka-client/say`,`/eureka-client/actuator`
	- `/ec/eureka-client/`, `/ec/eureka-client/say`,`/ec//eureka-client/actuator`
	- `/eurekaServer/`
	- 关闭eureka-client后再访问，会出发路由熔断机制，返回`This is my fallback response`

## Reference

[Spring Cloud Netflix](https://cloud.spring.io/spring-cloud-netflix/2.0.x/single/spring-cloud-netflix.html)
[使用Zuul构建API Gateway](https://blog.csdn.net/zhanglh046/article/details/78651993/)
[Zuul:智能路由和过滤](https://blog.csdn.net/chenqipc/article/details/53322830/)
[路由网关---zuul](https://www.imooc.com/article/44600)
