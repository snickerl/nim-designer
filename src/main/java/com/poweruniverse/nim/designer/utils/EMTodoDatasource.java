package com.poweruniverse.nim.designer.utils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class EMTodoDatasource extends BaseJavaDatasource {

	@Override
	public JSONObject getData(JSONObject params,int start,int limit) {
		JSONArray asa = new JSONArray();
		
		JSONObject row = new JSONObject();
		row.put("name", "张三");
		row.put("age", 23);
		asa.add(row);
		
		row = new JSONObject();
		row.put("name", "李四");
		row.put("age", 22);
		asa.add(row);
		
		JSONObject ret = new JSONObject();
		ret.put("totalCount", asa.size());
		ret.put("start", start);
		ret.put("limit", limit);
		ret.put("rows", asa);
		return ret;
	}


}
