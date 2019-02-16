package com.cj.auth.util;

public class MicroResponse {

	public static final MicroResponse OK=new MicroResponse(true,1,null);
	public static final MicroResponse AuthenticationFail=new MicroResponse(false,2, "Authentication Fail");
	public static final MicroResponse UnAuthorized=new MicroResponse(false,3, "Not Authorized");
	public static final MicroResponse InvalidRequest=new MicroResponse(false,4,"Invalid Request");
	public static final MicroResponse Existed=new MicroResponse(false,5,"Already Existed");
	public static final MicroResponse NotExist=new MicroResponse(false,6,"Not Exist");
	
	public static MicroResponse success(Object data){
		return new MicroResponse(true,1,data);
	}
	
	public static MicroResponse fail(Object data){
		return new MicroResponse(false,0,data);
	}
	
	
	private boolean success;
	private Integer code;
	private Object data;
	
	public MicroResponse(boolean success, Integer code, Object data) {
		super();
		this.success = success;
		this.code = code;
		this.data = data;
	}
	
	public boolean isSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
	public Integer getCode() {
		return code;
	}
	public void setCode(Integer code) {
		this.code = code;
	}
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
}

