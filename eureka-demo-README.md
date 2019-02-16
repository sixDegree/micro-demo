## 服务治理 Eureka

Eureka：
- Spring Cloud Netflix项目下的服务治理模块，可以与Springboot构建的微服务很容易的整合起来
- 包含了服务器端和客户端组件：
	+ 服务器端(服务注册中心)
		* 提供服务的发现和注册
		* 服务实例清单: 根据服务实例发送来的信息（例如主机，端口，版本，通信协议等）组织维护一个服务实例清单
		* 失效剔除: 以心跳的方式监测服务实例清单中的服务，剔除没有续约的服务(默认：间隔60s，服务失效时间90s)
		* 自我保护: 保护心跳失败比例在15分钟内低于85%的服务实例的注册信息不过期,所以服务调用者要有容错机处理实例调用失败(`eureka.server.enable-self-preservation`默认为true，启动)
		* 高可用: 可将自己作为服务向其他服务注册中心注册自己,形成了一组互相注册的服务注册中心，服务注册中心之间通过异步模式互相复制各自的状态（服务清单的互相同步），达到高可用
	+ 客户端(包含服务的消费者和生产者,一般一个应用既是服务提供者也是服务消费者)
		* 服务注册: 向服务注册中心注册自身提供的服务（`eureka.client.registerWithEureka`为false，则不会进行注册；`eureka.client.registry-fetch-interval-seconds`:缓存清单的刷新时间，默认30s）
		* 服务续约: 向服务注册中心周期性地发送心跳来更新它的服务租约（`eureka.instance.lease-renewal-interval-in-seconds & lease-expiration-duration-in-seconds`）
		* 获取服务: 向服务注册中心请求获取服务实例清单(包含了服务实例的元数据信息)
		* 服务调用: 根据从注册中心获取的服务实例清单，根据自己的策略选择具体的服务实例进行访问
		* 服务下线: 服务正常关闭,向注册中心取消租约
- 角色：
	+ 服务注册中心：Eureka服务端，提供服务注册和发现的功能（包括失效剔除，自我保护）
	+ 服务提供者：提供服务的应用，将自己提供的服务注册到Eureka，供其他应用发现（任务：服务注册，同步，续约）
	+ 服务消费者：消费者从注册中心发现服务列表，然后调用对应的服务（任务：服务获取，调用，下线）
	

** 示例：**
	- 服务注册中心(提供服务注册功能) ：eureka-server 
	- 服务提供方(注册服务到服务注册中心)：eureka-client


### 实现一个服务注册中心: eureka-server

Key:
- 在项目的启动类上使用`@EnableEurekaServer`注解
- `eureka.client.registerWithEureka` 配置禁用它的客户端注册行为(在默认设置下，Eureka服务注册中心也会将自己作为客户端来尝试注册它自己，会导致报错）
- `eureka.client.serviceUrl.defaultZone`配置服务注册中心的位置(默认是`http://localhost:8761/eureka/`，也可加入安全校验信息`http://<username>:<password>@localhost:8761/eureka/`)
- `eureka.server.enable-self-preservation` 自我保护模式：默认未true打开的，则已关停的Service会一直列在首页；关闭后，检测到的关停的Service就会从列表中自动剔除
- `eureka.server.eviction-interval-timer-in-ms` 心跳周期参数，可调快以便及时发现关停Service


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
		<artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
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
  port: 8761
  servlet:
    context-path: /eureka-server

eureka:
  server:
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 10000
  client:
    registerWithEureka: false   # 是否向服务注册中心注册自己
    fetchRegistry: false        # 是否检索服务
    service-url:
      defaultZone: ${EUREKA_URI:http://localhost:8761/eureka-server/eureka}
```

3. main
```java
package com.cj.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApp 
{
    public static void main( String[] args ){
    	SpringApplication.run(EurekaServerApp.class, args);
    }
}
```

4. Visit: `http://localhost:8761/eureka-server`


### 注册一个服务 eureka-client

Key:
- 在项目的启动类上使用`@EnableEurekaClient`/`EnableDiscoveryClient`注解
	+ spring cloud中discovery service有许多种实现（eureka、consul、zookeeper等等）
	+ 以上两个注解都可实现服务发现的功能
	+ `@EnableEurekaClient`: 基于spring-cloud-netflix,服务采用eureka作为注册中心，使用场景较为单一
	+ `@EnableDiscoveryClient`: 基于spring-cloud-commons, 使用场景更多元
- `spring.application.name` 注册的服务名会使用这个
- `eureka.client.serviceUrl.defaultZone` 配置服务注册中心的位置


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
		<artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
	</dependency>
	<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-web</artifactId>
	</dependency>
	<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-actuator</artifactId>
	</dependency>
</dependencies>
```

2. resources/application.yml
```
server:
  port: 8080     # 0 : 随机
  servlet:
    context-path: /eureka-client
  
spring:
  application:
    name: eureka-client
    
eureka:
  client:
    serviceUrl:
      defaultZone: ${EUREKA_URI:http://localhost:8761/eureka-server/eureka}
  instance:
    preferIpAddress: true
#    secure-port-enabled: true
    status-page-url-path: /eureka-client/actuator
    health-check-url-path: /eureka-client/actuator/health
    home-page-url-path: /eureka-client/
```

3. main
```java
package com.cj.eureka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.discovery.EurekaClient;

@SpringBootApplication
@EnableEurekaClient				// or @EnableDiscoveryClient
@RestController
public class EurekaClientApp{
    public static void main( String[] args ){
        SpringApplication.run(EurekaClientApp.class, args);
    }
    
    @RequestMapping("/")
    public String index() {
    	return "Hello World";
    }
    
    @RequestMapping("/say")
    public String say(){
    	return "Say Hello world";
    }
    
//    @Autowired
//    @Lazy
//    private EurekaClient eurekaClient;
// 
//    @Value("${spring.application.name}")
//    private String appName;
//    
//    @RequestMapping("/greeting")
//    public String greeting() {
//        return String.format("Hello from '%s'!", eurekaClient.getApplication(appName).getName());
//    }

}
```

4. Visit: `http://localhost:8080/eureka-client`
	- `/`,`/say`
	- `/actuator`,`/actuator/info`,`/actuator/health`


## Reference

[spring-cloud-eureka服务治理](https://www.jianshu.com/p/920d2bcda3a7)

[Eureka服务发现的常见问题](https://blog.csdn.net/qq_27529917/article/details/80939962)

[Spring Cloud Netflix](https://cloud.spring.io/spring-cloud-netflix/2.0.x/single/spring-cloud-netflix.html)

