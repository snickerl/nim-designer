package com.poweruniverse.nim.designer.utils;

import net.sf.json.JSONObject;

public abstract class BaseJavaDatasource {
	/**
	 * 返回值格式：{
	 * 	totalCount:0,
	 * 	start:0,
	 * 	limit:0,
	 * 	rows:[{},{},...]
	 * }
	 */
	public abstract JSONObject getData(JSONObject params,int start,int limit);

}
