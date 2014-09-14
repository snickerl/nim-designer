package com.poweruniverse.nim.designer.servlet;

import net.sf.json.JSONObject;


public class Message implements java.io.Serializable {
	private static final long serialVersionUID = -7111514299949856465L;
	
	private Boolean success = true;
	private String errorMsg = null;
	private JSONObject data = null;
	
	public Message(){
	}
	
	public Message(JSONObject dataParam){
		this.success = true;
		if(dataParam!=null){
			for(Object key:dataParam.keySet()){
				put(key, dataParam.get(key));
			}
		}
	}
	
	public Message(String key,Object value){
		this.success = true;
		put(key, value);
	}
	
	public void put(Object key,Object value){
		if(this.data == null){
			this.data = new JSONObject();
		}
		this.data.put(key, value);
	}
	
	public Object get(Object key){
		return this.data.get(key);
	}
	
	public Message(String errorMsg ){
		this.success = false;
		this.errorMsg = errorMsg;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getErrorMsg() {
		return errorMsg;
	}


	public void setSuccess(boolean success) {
		this.success = success;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}


	@Override
	public String toString() {
		JSONObject ret = new JSONObject();
		ret.put("success", success);
		if(!success){
			ret.put("errorMsg", errorMsg);
		}
		if(data!=null){
			for(Object key : data.keySet()){
				ret.put(key, data.get(key));
			}
		}
		return ret.toString();
	}

}
