package com.cj.rabbit.demo1.entity;

import java.io.Serializable;

public class Order implements Serializable {

	private static final long serialVersionUID = -698577629696435935L;
	
	private String id;
	private String name;
	private String msgId;
	public String getId() {
		return id;
	}
	public void setId(String id) {
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
	
	
}
