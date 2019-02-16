
# Dubbo-Demo

[Dubbo Quick Start](http://dubbo.apache.org/en-us/docs/user/quick-start.html) 


### Demo

1. pom.xml
```xml
<!-- Dubbo (include spring) -->
<dependency>
	<groupId>com.alibaba</groupId>
	<artifactId>dubbo</artifactId>
	<version>2.6.2</version>
</dependency>

<!-- Curator (include zookeeper) -->
<!-- Note: need to change zookeeper version,the beta version zookeeper has issues -->
<dependency>
	<groupId>org.apache.curator</groupId>
	<artifactId>curator-recipes</artifactId>
	<version>4.0.1</version>
	<exclusions>
		<exclusion>
			 <groupId>org.apache.zookeeper</groupId>
			 <artifactId>zookeeper</artifactId>
		</exclusion>
	</exclusions>
</dependency>
<dependency>
	<groupId>org.apache.zookeeper</groupId>
	<artifactId>zookeeper</artifactId>
	<version>3.4.6</version>
	<exclusions>
		<exclusion>
			  <groupId>org.slf4j</groupId>
			  <artifactId>slf4j-api</artifactId>
		</exclusion>
		<exclusion>
			 <groupId>org.slf4j</groupId>
			 <artifactId>slf4j-log4j12</artifactId>
		</exclusion>
	</exclusions>
</dependency>

<!-- Junit -->
<dependency>
	<groupId>junit</groupId>
	<artifactId>junit</artifactId>
	<version>4.12</version>
</dependency>
```

2. Provider: resources/demo-provider.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	   xmlns:dubbo="http://dubbo.apache.org/schema/dubbo"
	   xmlns="http://www.springframework.org/schema/beans"
	   xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-4.3.xsd
	   http://dubbo.apache.org/schema/dubbo http://dubbo.apache.org/schema/dubbo/dubbo.xsd">
	
	<dubbo:application name="demoProvider"/>
	<dubbo:registry address="zookeeper://localhost:2181"/>
	<dubbo:protocol name="dubbo" port="20880"/>
	<dubbo:service interface="com.cj.dubbo.service.DemoService" ref="demoService"/>
	<bean id="demoService" class="com.cj.dubbo.service.DemoServiceImpl"/>
	
</beans>
```

3. Provider: Service interface & implement
```java
package com.cj.dubbo.service;
public interface DemoService {
	 String sayHello(String name);
}

package com.cj.dubbo.service;
public class DemoServiceImpl implements DemoService {
	@Override
	public String sayHello(String name) {
		return "Hello "+name;
	}
}
```
		
4. Consumer: resources/demo-consumer.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	   xmlns:dubbo="http://dubbo.apache.org/schema/dubbo"
	   xmlns="http://www.springframework.org/schema/beans"
	   xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-4.3.xsd
	   http://dubbo.apache.org/schema/dubbo http://dubbo.apache.org/schema/dubbo/dubbo.xsd">

	<dubbo:application name="demoConsumer"/>
	<dubbo:registry address="zookeeper://localhost:2181"/>
	<dubbo:reference id="demoService" check="false" interface="com.cj.dubbo.service.DemoService"/>
</beans>
```

5. Consumer: Service interface (same with Provider Service interface)
```java
package com.cj.dubbo.service;
public interface DemoService {
	 String sayHello(String name);
}
```

6. Test
```java
package com.cj.dubbo;
import java.io.IOException;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.cj.dubbo.consumer.HelloConsumerService;
import com.cj.dubbo.service.DemoService;
import com.cj.dubbo.service.HelloService;

public class DemoDubboTest {
	@Test
	public void runDemoProvider() throws IOException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"demo-provider.xml"});
		context.start();
		System.out.println("Provider started.");
		System.in.read(); // press any key to exit
	}
	
	@Test
	public void runDemoConsumer() {
		// System.setProperty("java.net.preferIPv4Stack", "true");
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"demo-consumer.xml"});
		context.start();
		DemoService demoService = (DemoService) context.getBean("demoService"); // obtain proxy object for remote invocation
		String hello = demoService.sayHello("world"); // execute remote invocation
		System.out.println(hello); // show the result
	}
}
```

8. check zookeeper
```bash
[zk: zk01:2181(CONNECTED) 9] ls /dubbo
[com.cj.dubbo.service.DemoService]
[zk: zk01:2181(CONNECTED) 10] ls /dubbo/com.cj.dubbo.service.DemoService
[consumers, configurators, routers, providers]
```

### SpringBoot Dubbo


#### Provider (SpringBoot)

1. pom.xml
```xml
<!-- springboot -->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-test</artifactId>
	<scope>test</scope>
</dependency>

<!-- springboot dubbo -->
<!-- <dependency>
	<groupId>com.alibaba.spring.boot</groupId>
	<artifactId>dubbo-spring-boot-starter</artifactId>
	<version>2.0.0</version>
</dependency> -->

<!-- Dubbo (include spring) -->
<dependency>
	<groupId>com.alibaba</groupId>
	<artifactId>dubbo</artifactId>
	<version>2.6.2</version>
</dependency>

<!-- Curator (include zookeeper) -->
<!-- Note: need to change zookeeper version,the beta version zookeeper has issues -->
<dependency>
	<groupId>org.apache.curator</groupId>
	<artifactId>curator-recipes</artifactId>
	<version>4.0.1</version>
	<exclusions>
		<exclusion>
			 <groupId>org.apache.zookeeper</groupId>
			 <artifactId>zookeeper</artifactId>
		</exclusion>
	</exclusions>
</dependency>
<dependency>
	<groupId>org.apache.zookeeper</groupId>
	<artifactId>zookeeper</artifactId>
	<version>3.4.6</version>
	<exclusions>
		<exclusion>
			  <groupId>org.slf4j</groupId>
			  <artifactId>slf4j-api</artifactId>
		</exclusion>
		<exclusion>
			 <groupId>org.slf4j</groupId>
			 <artifactId>slf4j-log4j12</artifactId>
		</exclusion>
	</exclusions>
</dependency>
```

2. resources/log4j.properties
```
### set log levels ###
log4j.rootLogger=warn, stdout
###output to the console###
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.Target=System.out
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=[%d{dd/MM/yy hh:mm:ss:sss z}] %t %5p %c{2}: %m%n
```

3. resources/provider.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	   xmlns:dubbo="http://dubbo.apache.org/schema/dubbo"
	   xmlns="http://www.springframework.org/schema/beans"
	   xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-4.3.xsd
	   http://dubbo.apache.org/schema/dubbo http://dubbo.apache.org/schema/dubbo/dubbo.xsd">
	
	<dubbo:application name="helloProvider"/>
	<dubbo:registry address="zookeeper://localhost:2181"/>
	<dubbo:protocol name="dubbo" port="20881"/>
	<dubbo:service interface="com.cj.dubbo.service.HelloService"  ref="helloService"/>
	
</beans>
```

4. ProviderConfig (import the `provider.xml`)
```java
package com.cj.dubbo.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource(locations={"classpath:provider.xml"})
public class ProviderConfig {
}
```

5. Provider Service interface & implement
```java
package com.cj.dubbo.service;
public interface HelloService {
	public String sayHello(String name);
}
```
```java
package com.cj.dubbo.provider;
import org.springframework.stereotype.Component;
import com.cj.dubbo.service.HelloService;

@Component("helloService")
public class HelloServiceImpl implements HelloService {
	@Override
	public String sayHello(String name) {
		return "Hello, " + name + " (from Spring Boot)";
	}
}
```

6. main start
```java
package com.cj.dubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HelloProviderApplication {
	public static void main(String[] args) {
		SpringApplication.run(HelloProviderApplication.class,args);
		System.out.println("Start Application");
	}
}
```

#### Consumer (Spring)

1. pom.xml
```xml
<!-- Dubbo (include Spring) -->
<dependency>
	<groupId>com.alibaba</groupId>
	<artifactId>dubbo</artifactId>
	<version>2.6.2</version>
</dependency>

<!-- Curator (include zookeeper) -->
<!-- Note: need to change zookeeper version,the beta version zookeeper has issues -->
<dependency>
	<groupId>org.apache.curator</groupId>
	<artifactId>curator-recipes</artifactId>
	<version>4.0.1</version>
	<exclusions>
		<exclusion>
			 <groupId>org.apache.zookeeper</groupId>
			 <artifactId>zookeeper</artifactId>
		</exclusion>
	</exclusions>
</dependency>
<dependency>
	<groupId>org.apache.zookeeper</groupId>
	<artifactId>zookeeper</artifactId>
	<version>3.4.6</version>
	<exclusions>
		<exclusion>
			  <groupId>org.slf4j</groupId>
			  <artifactId>slf4j-api</artifactId>
		</exclusion>
		<exclusion>
			 <groupId>org.slf4j</groupId>
			 <artifactId>slf4j-log4j12</artifactId>
		</exclusion>
	</exclusions>
</dependency>

<!-- Junit -->
<dependency>
	<groupId>junit</groupId>
	<artifactId>junit</artifactId>
	<version>4.12</version>
</dependency>
```

2. resource/hello-consumer.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	   xmlns:dubbo="http://dubbo.apache.org/schema/dubbo"
	   xmlns="http://www.springframework.org/schema/beans"
	   xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-4.3.xsd
	   http://dubbo.apache.org/schema/dubbo http://dubbo.apache.org/schema/dubbo/dubbo.xsd">

	<dubbo:application name="helloConsumer"/>
	<dubbo:registry address="zookeeper://localhost:2181"/>
	<dubbo:reference id="helloService" check="false"  interface="com.cj.dubbo.service.HelloService"/>
	
	<bean id="helloConsumerService" class="com.cj.dubbo.consumer.HelloConsumerService">
		<property name="helloService" ref="helloService"/>
	</bean>
</beans>
```

3. Service interface (same with Provider Service interface)
```java
package com.cj.dubbo.service;
public interface HelloService {
	public String sayHello(String name);
}
```

4. ConsumerService: Call the provided Service
```java
package com.cj.dubbo.consumer;
import com.cj.dubbo.service.HelloService;

public class HelloConsumerService {
	private HelloService helloService;
	public HelloService getHelloService() {
		return helloService;
	}
	public void setHelloService(HelloService helloService) {
		this.helloService = helloService;
	}
	public void printSay(String name) {
		if(helloService==null)
			System.out.println("Can't get helloService!");
		System.out.println(helloService.sayHello(name));
	}
}
```

5. Test
```java
public class DemoDubboTest {
	@Test
	public void runHelloConsumer() {
		// System.setProperty("java.net.preferIPv4Stack", "true");
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"hello-consumer.xml"});
		context.start();
		HelloService helloService = (HelloService) context.getBean("helloService"); // obtain proxy object for remote invocation
		String hello=helloService.sayHello("Girl"); // execute remote invocation
		System.out.println(hello);					// show the result
		
		System.out.println("---- Consume HelloService ----");
		HelloConsumerService consumer=context.getBean(HelloConsumerService.class);
		consumer.printSay("Boy");
		
	}
}
```

#### Check zookeeper

```bash
[zk: zk01:2181(CONNECTED) 9] ls /dubbo
[com.cj.dubbo.service.DemoService, com.cj.dubbo.service.HelloService]

[zk: zk01:2181(CONNECTED) 18] ls /dubbo/com.cj.dubbo.service.HelloService      
[consumers, configurators, routers, providers]

[zk: zk01:2181(CONNECTED) 19] ls /dubbo/com.cj.dubbo.service.HelloService/providers
[dubbo%3A%2F%2F192.168.31.78%3A20881%2Fcom.cj.dubbo.service.HelloService%3Fanyhost%3Dtrue%26application%3DhelloProvider%26dubbo%3D2.6.2%26generic%3Dfalse%26interface%3Dcom.cj.dubbo.service.HelloService%26methods%3DsayHello%26pid%3D1359%26side%3Dprovider%26timestamp%3D1549812635667]

[zk: zk01:2181(CONNECTED) 20] ls /dubbo/com.cj.dubbo.service.HelloService/consumers
[]
```