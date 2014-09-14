package com.poweruniverse.nim.designer.service.parser;

import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.dom4j.Element;

public class PageElParser {

	/**
	 * 集合类型数据源的解析
	 */
	public static JSONObject parsePageEl(Element pageEl) throws Exception{
		JSONObject ret = new JSONObject();
		
		String needsLogin = pageEl.attributeValue("needsLogin");
		ret.put("needsLogin", "true".equals(needsLogin));
		
		String pageTitle = pageEl.attributeValue("title");
		//临时代码 将页面标题字段名 从title改为label
		if(pageEl.attributeValue("label")!=null){
			pageTitle = pageEl.attributeValue("label");
		}
		ret.put("title", pageTitle);
		
		//page events
		
		String onLoad = pageEl.attributeValue("onLoad");
		String onClose = pageEl.attributeValue("onClose");
		ret.put("onLoad", onLoad);
		ret.put("onClose", onClose);
		
		
		JSONObject listenersObj = new JSONObject();
		listenersObj.put("onLoad",pageEl.attributeValue("onLoad"));
		listenersObj.put("onClose",pageEl.attributeValue("onClose"));
		
		String pageCmpType = pageEl.attributeValue("component");
		if("page".equals(pageCmpType)){
			ret.put("pageScriptContent", "//当前页面\n" +
					"var _page_widget = LUI.Page.createNew({\n" +
					"title:'" + pageTitle +"',\n" +
					"needsLogin:" + "true".equals(needsLogin) +",\n" +
					"listenerDefs:" +listenersObj.toString()+"\n"+
				"});\n\n");
			
		}else if("subpage".equals(pageCmpType)){
			
			String name = pageEl.attributeValue("name");
			String width = pageEl.attributeValue("width");
			String height = pageEl.attributeValue("height");
			
			ret.put("name", name);
			ret.put("width", width);
			ret.put("height", height);
			
			ret.put("pageScriptContent", "//当前页面\n" +
					"var _page_widget = LUI.Subpage.createNew({\n" +
					"name:'" + name +"',\n" +
					"title:'" + pageTitle +"',\n" +
					"needsLogin:" + "true".equals(needsLogin) +",\n" +
					"width:'" + width +"',\n" +
					"height:'" + height +"',\n" +
					"listenerDefs:" +listenersObj.toString()+"\n"+
				"});\n\n");
		}
		return ret;
	}


}
