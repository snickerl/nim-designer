package com.poweruniverse.nim.designer.service.parser;

import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.dom4j.Element;

import com.poweruniverse.nim.designer.utils.PageAnalyseUtils;

public class ImportElParser {

	/**
	 * 导入子页面的解析
	 */
	public static String parseImportEl(Element importEl,JSONObject params,Map<String, Object> root,Integer yongHuDM) throws Exception{
		String importScriptContent = "";
		
		String name = importEl.attributeValue("name");
		String label = importEl.attributeValue("label");
		String renderto = importEl.attributeValue("renderto");
		String pageURL = importEl.attributeValue("pageURL");
		String autoLoad = importEl.attributeValue("autoLoad");
		if(autoLoad==null){
			autoLoad = "true";
		}
		
		//page events
		String onLoad = importEl.attributeValue("onLoad");
		JSONObject listenersObj = new JSONObject();
		listenersObj.put("onLoad",onLoad);
		
		JSONArray parameters =  PageAnalyseUtils.getParametersFromEl(importEl,root,params);
		
		String importCmpType = importEl.attributeValue("component");
		if("importSubpage".equals(importCmpType)){
			importScriptContent = "//当前页面\n" +
				"LUI.ImportPage.createNew({\n" +
					"name:'" + name +"',\n" +
					"label:'" + label +"',\n" +
					"renderto:'" + renderto +"',\n" +
					"pageURL:'" + pageURL +"',\n" +
					"autoLoad:" + autoLoad +",\n" +
					"parameters:" +parameters+",\n"+
					"listenerDefs:" +listenersObj.toString()+"\n"+
				"});\n\n";
		}
		return importScriptContent;
	}


}
