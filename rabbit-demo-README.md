
# rabbit-demo

## rabbitmq

[Quick Start](http://www.rabbitmq.com/getstarted.html)

`Producer -> Exchange -> routingKey/headers/all -> Queue -> Consumer`


- Message: 
	+ 消息(Properties+Body)，Server和应用程序之间传送的数据
	+ Properties: 可对消息进行修饰，比如消息的优先级，延迟等高级特性
	+ Body: 消息体内容
	
- 队列 Queue: 
	+ Message Queue 消息队列，保存消息并将它们转发给消费者
	
- 交换机 Exchange: 
	+ 接收消息并且转发到绑定的队列
	+ 根据路由键 Routing key(一个路由规则,用`.`分隔)将消息转发到绑定的队列
	+ 注: 
		* 交换机不存储消息(如果没有 Queue Binding to Exchange, 它会直接丢弃掉 Producer 发送过来的消息) 
		* 在启用ack模式后，交换机找不到队列会返回错误
	+ 交换机有四种类型:
		* Direct(默认)：根据routingKey全文匹配寻找匹配的队列
		* Topic：与direct类似, 只是routingKey匹配上支持通配符：`*` 只能向后多匹配一层路径 ; `#` 可以向后匹配多层路径
		* Headers：根据请求消息中设置的header attribute参数类型（一组键值对）匹配队列（和routingKey没有关系）
		* Fanout：广播模式，转发消息到所有绑定队列（和routingKey没有关系）
	
- 绑定 Binding: 
	+ Exchange和Queue之间的虚拟连接（多对多）
	+ binding中可以包含Routing key

- 虚拟主机 Virtual host: 
	+ 用于进行逻辑隔离，最上层的消息路由
	+ 一个Virtual host里面可以有若干个Exchange和Queue，但名称需不同
	+ 每一个RabbitMQ服务器都有一个默认的虚拟主机`/`
	+ 用户只能在虚拟主机的粒度进行权限控制（eg: 要禁止A组访问B组的交换机/队列/绑定，须为A和B分别创建一个虚拟主机）

- 服务端 Server: 
	+ 又称Broker，接受Client的连接，实现AMQP实体服务
	
- 连接 Connection: 
	+ 应用程序与Broker的网络连接
	
- Channel: 
	+ 网络信道，消息读写的通道
	+ Client可建立多个Channel，每个Channel代表一个会话任务
	+ 几乎所有的操作都在Channel中进行
	

## demo

### pom.xml

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-test</artifactId>
	<scope>test</scope>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.56</version>
</dependency>
```

### application.yml

```
server:
  port: 8080
  servlet:
    context-path: /rabbit-demo
    
spring:
  rabbitmq:
    addresses: localhost:5672
    username: admin
    password: admin
    virtual-host: my_vhost
    connection-timeout: 15000
    # for confirmCallback:
    publisher-confirms: true
    # add below for consumer:
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 1
        concurrency: 5
        max-concurrency: 10
```

### main

1. DirectExchangeDemoApp
2. TopicExchangeDemoApp
3. HeadersExchangeDemoApp
4. FanoutExchangeDemoApp
5. ExchangeObjectDemoApp
6. SendConfirmDemoApp
