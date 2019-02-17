package com.cj.zoo.entity;

public class Image {
	private String id;
	private String originalName;
	private String type;
	private String status;
	
	public Image(){}
	
	public Image(String id,String originalName,String status){
		this.id=id;
		this.originalName=originalName;
		this.status=status;
		int pos=originalName.lastIndexOf(".");
		this.type=(pos!=-1?originalName.substring(pos+1):"Unknow");
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
