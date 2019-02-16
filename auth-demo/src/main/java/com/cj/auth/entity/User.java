package com.cj.auth.entity;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class User implements Serializable {
	private static final long serialVersionUID = -4198480470411674996L;
	
	private Integer id;
	private String name;
	private String password;
	
	public User(){}
	
	public User(Integer id,String name,String password){
		this.id=id;
		this.name=name;
		this.password=password;
	}
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@JsonIgnore
	public String getPassword() {
		return password;
	}
	@JsonProperty
	public void setPassword(String password) {
		this.password = password;
	}
}
