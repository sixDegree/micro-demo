package com.cj.rabbit.demo6;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ConfirmCallback;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;

@SpringBootApplication
public class SendConfirmDemoApp {
	public static void main( String[] args ) throws IOException, InterruptedException{
		ConfigurableApplicationContext ctx = SpringApplication.run(SendConfirmDemoApp.class, args);
		
		OrderQueueSender sender=ctx.getBean(OrderQueueSender.class);
		TransportLogService transportLogService=ctx.getBean(TransportLogService.class);
		
		// prepare testing transportLog records
		Order order = null;
		for(int i=0;i<=4;i++) {
			order=new Order();
			order.setId(System.currentTimeMillis());
			order.setMsgId("0"+i+"-"+order.getId()+"$"+UUID.randomUUID().toString());
			order.setName("TestSending0"+i);
			transportLogService.create(order.getMsgId(), JSON.toJSONString(order),"Sending",i,10);
		}
		for(int i=5;i<=8;i++) {
			order=new Order();
			order.setId(System.currentTimeMillis());
			order.setMsgId("0"+i+"-"+order.getId()+"$"+UUID.randomUUID().toString());
			order.setName("TestSuccess0"+i);
			transportLogService.create(order.getMsgId(), JSON.toJSONString(order),"Success",i-5,10);
		}
		order=new Order();
		order.setId(System.currentTimeMillis());
		order.setMsgId("09"+"-"+order.getId()+"$"+UUID.randomUUID().toString());
		order.setName("TestFail09");
		transportLogService.create(order.getMsgId(), JSON.toJSONString(order),"Fail",0,10);

		// send
		String routingKey="order.123";
		order = new Order();
		order.setId(System.currentTimeMillis());
		order.setMsgId(order.getId()+"$"+UUID.randomUUID().toString());
		order.setName("Hello");
		transportLogService.create(order.getMsgId(), JSON.toJSONString(order));
		sender.send(routingKey,order);
    }
	
	public static final String Order_Exchange="order-exchange";
	public static final String Order_Queue="order-queue";
	public static final String Order_RoutingKey="order.#";
	public static final String Order_RoutingKey_Prefix="order.";
	
	// sender
	@Component
	public class OrderQueueSender{
		@Autowired
	    private RabbitTemplate rabbitTemplate;
		
		/*public void send(String routingKey,Order order){
			System.out.println("send order :"+order);
			rabbitTemplate.convertAndSend(Order_Exchange,routingKey,order,new CorrelationData(order.getMsgId()));
		}*/
		
		@Autowired
		private TransportLogService transportLogService;
		private final ConfirmCallback confirmCallback=new RabbitTemplate.ConfirmCallback(){
			@Override
			public void confirm(CorrelationData correlationData,boolean ack,String cause){
				System.out.println("confirm correlationData:"+correlationData+", ack:"+ack+", cause:"+cause);
				if(ack)
					transportLogService.changeStatus(correlationData.getId(),"Success");
				else
					System.err.println("confirm error:"+cause);
			}
		};
		
		public void send(String routingKey,Order order){
			System.out.println("send order :"+order);
			rabbitTemplate.setConfirmCallback(confirmCallback);
			rabbitTemplate.convertAndSend(Order_Exchange,routingKey,order,new CorrelationData(order.getMsgId()));
		}
	}
	
	// receiver
	@Component
	public class OrderQueueReceiver {
		@RabbitHandler
		@RabbitListener(
			bindings=@QueueBinding(
				value=@Queue(name=Order_Queue,durable="true"),
				exchange=@Exchange(name=Order_Exchange,durable="true",type="topic"),
				key=Order_RoutingKey
			)
		)
//		@RabbitListener(queues = {Order_Queue})
		public void onOrderMessage(@Payload Order order,@Headers Map<String,Object> headers,Channel channel) 
			throws Exception{
			System.out.println("consume queue("+headers.get(AmqpHeaders.CONSUMER_QUEUE)+")"
					+" routingKey("+headers.get(AmqpHeaders.RECEIVED_ROUTING_KEY)+")"
					+" order:"+order);
			;

			// 手工签收：
			Long deliveryTag=(Long)headers.get(AmqpHeaders.DELIVERY_TAG);
			channel.basicAck(deliveryTag,false); // 给MQ主动回送一个信息，说明已签收
		}
	}
	
	// schedule Task
	@Component
	public class RetryMessageTasker {
		@Autowired
		private OrderQueueSender orderQueueSender;
		@Autowired
		private TransportLogService transportLogService;

		@Scheduled(initialDelay=3000,fixedDelay=1000)
		public void reSend(){
			System.out.println("trigger reSend()");
			List<TransportLog> list=transportLogService.listRetry();	// status='Sending' and nextTime<=sysdate()
			
			list.forEach(transportLog->{
				System.out.println(transportLog);
				String msgId= transportLog.getMsgId();
				Integer retryCount=transportLog.getRetryCount();
				
				if(transportLog.getRetryCount()>=3){
					System.out.println("Fail "+retryCount+" times:"+msgId);
					transportLogService.changeStatus(msgId,"Fail");		// stop retry sending
				}else{
					System.out.println("Retry "+(retryCount+1)+" times:"+msgId);
					transportLogService.addRetryCount(msgId);
					Order reSendOrder=JSON.parseObject(transportLog.getContent(),Order.class);
					orderQueueSender.send(Order_RoutingKey_Prefix+retryCount,reSendOrder);
				}
			});
		}
		
		@Scheduled(initialDelay=1000*10,fixedDelay=5*1000)
		public void listTransportLogs() {
			System.out.println("trigger listTransportLogs()");
			List<TransportLog> list=transportLogService.list();
			for(TransportLog log:list)
				System.out.println(log);
		}
	}
	
	// schedule: spring默认会创建一个单线程池,通过taskRegistrar设置自定义线程池
	@Configuration
	@EnableScheduling
	public class TaskSchedulerConfig implements SchedulingConfigurer{

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar){
			taskRegistrar.setScheduler(taskScheduler());
		}
		@Bean(destroyMethod="shutdown")
		public Executor taskScheduler(){
			return Executors.newScheduledThreadPool(100);
		}
	}
		
	// entity: Order
	public static class Order implements Serializable {
		private static final long serialVersionUID = -698577629696435935L;
		private Long id;
		private String name;
		private String msgId;
		public Long getId() {
			return id;
		}
		public void setId(Long id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getMsgId() {
			return msgId;
		}
		public void setMsgId(String msgId) {
			this.msgId = msgId;
		}
		
		@Override
		public String toString() {
			return "Order [id=" + id + ", name=" + name + ", msgId=" + msgId + "]";
		}
	}

	// entity: TransportLog
	public static class TransportLog implements Serializable{
		private static final long serialVersionUID = 4330091676523447230L;
		private String msgId;
		private String content;
		private String status;
		private Integer retryCount;
		private Date nextTime;
		private Date createTime;
		private Date updateTime;
		public String getMsgId() {
			return msgId;
		}
		public void setMsgId(String msgId) {
			this.msgId = msgId;
		}
		public String getContent() {
			return content;
		}
		public void setContent(String content) {
			this.content = content;
		}
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public Integer getRetryCount() {
			return retryCount;
		}
		public void setRetryCount(Integer retryCount) {
			this.retryCount = retryCount;
		}
		public Date getNextTime() {
			return nextTime;
		}
		public void setNextTime(Date nextTime) {
			this.nextTime = nextTime;
		}
		public Date getCreateTime() {
			return createTime;
		}
		public void setCreateTime(Date createTime) {
			this.createTime = createTime;
		}
		public Date getUpdateTime() {
			return updateTime;
		}
		public void setUpdateTime(Date updateTime) {
			this.updateTime = updateTime;
		}
		@Override
		public String toString() {
			return "TransportLog [msgId=" + msgId + ", content=" + content + ", status=" + status + ", retryCount="
					+ retryCount + ", nextTime=" + nextTime + ", createTime=" + createTime + ", updateTime="
					+ updateTime + "]";
		}
		
	}

	// service: TransportLogService
	@Component
	public class TransportLogService{
		private final ConcurrentMap<String,TransportLog> data = new ConcurrentHashMap<String,TransportLog>();
		
		public List<TransportLog> list(){
			return new ArrayList<TransportLog>(data.values());
		}
		public List<TransportLog> listRetry(){
			Date now = new Date();
			List<TransportLog> list=new ArrayList<TransportLog>();
			for(String key:data.keySet()) {
				TransportLog transportLog=data.get(key);
				if("Sending"==transportLog.getStatus() && transportLog.getNextTime().before(now))
					list.add(transportLog);
			}
			return list;
		}
		
		public TransportLog changeStatus(String msgId,String status){
			TransportLog transportLog=data.get(msgId);
			if(transportLog==null)
				return null;
			transportLog.setStatus(status);
			transportLog.setUpdateTime(new Date());
			data.replace(msgId, transportLog);
			return transportLog;
		}
		
		public TransportLog addRetryCount(String msgId) {
			TransportLog transportLog=data.get(msgId);
			if(transportLog==null)
				return null;
			
			Date now=new Date();
			Calendar c=Calendar.getInstance();
			c.setTime(now);
			c.add(Calendar.SECOND, 10);
			
			Integer retryCount=transportLog.getRetryCount()==null?0:transportLog.getRetryCount();
			transportLog.setRetryCount(retryCount+1);
			transportLog.setNextTime(c.getTime());
			transportLog.setUpdateTime(now);
			data.replace(msgId, transportLog);
			return transportLog;
		}
		
		public TransportLog create(String msgId,String content) {
			return create(msgId,content,"Sending",0,10);
		}
		
		// for test:
		public TransportLog create(String msgId,String content,String initialStatus,Integer intialRetryCount,Integer intervalSeconds){
			Date now=new Date();
			Calendar c=Calendar.getInstance();
			c.setTime(now);
			c.add(Calendar.SECOND, intervalSeconds);
			
			TransportLog transportLog=new TransportLog();
			transportLog.setMsgId(msgId);
			transportLog.setContent(content);
			transportLog.setStatus(initialStatus);
			transportLog.setRetryCount(intialRetryCount);
			transportLog.setNextTime(c.getTime());
			transportLog.setCreateTime(now);
			transportLog.setUpdateTime(now);
			data.putIfAbsent(msgId, transportLog);
			return transportLog;
		}
		
		public void save(TransportLog transportLog) {
			data.putIfAbsent(transportLog.getMsgId(), transportLog);
		}
		
	}
}
