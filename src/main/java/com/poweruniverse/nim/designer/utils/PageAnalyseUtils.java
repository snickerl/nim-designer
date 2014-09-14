package com.poweruniverse.nim.designer.utils;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.dom4j.Element;

import com.poweruniverse.nim.designer.service.PageService;
import com.poweruniverse.nim.interfaces.entity.ShiTiLeiI;
import com.poweruniverse.nim.interfaces.entity.ZiDuanI;

import freemarker.template.Configuration;
import freemarker.template.Template;

public class PageAnalyseUtils {
	
	public static JSONArray getFieldsDefByEL(Element el,ShiTiLeiI stl,boolean allowPropertyNotExists) throws Exception{
		JSONArray fieldArray  = new JSONArray();
		
		List<Element> subEls = el.elements("property");
		for(Element subEl :subEls){
			JSONObject fieldObj  = new JSONObject();
			fieldObj.put("name", subEl.attributeValue("name"));
			fieldObj.put("fieldType", subEl.attributeValue("fieldType"));
			
			Element subFieldsEl = subEl.element("properties");
			if(subFieldsEl!=null){
				ShiTiLeiI subStl = null;
				if(stl!=null && stl.hasZiDuan(subEl.attributeValue("name"))){
					ZiDuanI zd = stl.getZiDuan(subEl.attributeValue("name"));
					subStl = zd.getGuanLianSTL();
					
					JSONObject metaObj  = new JSONObject();
					metaObj.put("primaryFieldName", subStl.getZhuJianLie());
					fieldObj.put("meta",metaObj);
				}else if(stl!=null && !stl.hasZiDuan(subEl.attributeValue("name")) && !allowPropertyNotExists){
					throw new Exception("实体类("+stl.getShiTiLeiMC()+")中不存在此字段("+subEl.attributeValue("name")+")!");
				}
				fieldObj.put("fields",getFieldsDefByEL(subFieldsEl,subStl,allowPropertyNotExists));
			}
			fieldArray.add(fieldObj);
		}
		return fieldArray;
	}
	
	
	public static JSONArray getFiltersFromEl(Element dataEl,Map<String, Object> root,JSONObject params ) throws Exception{
		JSONArray filterJsonArray = new JSONArray();
		Element filtersEl = dataEl.element("filters");
		if(filtersEl!=null){
			List<Element> filterEls = filtersEl.elements("filter");
			for(Element filterEl : filterEls){
				if("propertyFilter".equals(filterEl.attributeValue("component"))){
					String property= filterEl.attributeValue("property"); 
					String assist= filterEl.attributeValue("assist"); 
					String operator= filterEl.attributeValue("operator"); 
					String value= filterEl.attributeValue("value"); 
					//是否动态value
					if(value!=null && value.indexOf("$")>=0 || value.indexOf("<#") >=0){
						String parseString = value;
						if(params!=null){
							parseString = "<#assign _paramsString>"+params.toString()+"</#assign><#assign params = _paramsString?eval />"+parseString;
						}
						value = processTemplate(parseString, root,null);
					}
					
					JSONObject filterObj = new JSONObject();
					filterObj.put("property", property);
					filterObj.put("assist", assist);
					filterObj.put("operator", operator);
					filterObj.put("value", value);
					
					filterJsonArray.add(filterObj);
				}else if("sqlFilter".equals(filterEl.attributeValue("component"))){
					String sql= filterEl.attributeValue("sql"); 
					
					JSONObject filterObj = new JSONObject();
					filterObj.put("operator", "sql");
					filterObj.put("sql", sql);
					
					filterJsonArray.add(filterObj);
				}
			}
		}
		return filterJsonArray;
	}
	
	
	public static JSONArray getSortsFromEl(Element dataEl,Map<String, Object> root,JSONObject params ) throws Exception{
		JSONArray sortJsonArray = new JSONArray();
		Element sortsEl = dataEl.element("sorts");
		if(sortsEl!=null){
			List<Element> sortEls = sortsEl.elements("sort");
			for(Element sortEl : sortEls){
				String property= sortEl.attributeValue("property"); 
				String dir= sortEl.attributeValue("dir"); 
				
				JSONObject sortObj = new JSONObject();
				sortObj.put("property", property);
				sortObj.put("dir", dir);
				
				sortJsonArray.add(sortObj);
			}
		}
		return sortJsonArray;
	}
	
	public static JSONArray getParametersFromEl(Element dataEl,Map<String, Object> root,JSONObject params ) throws Exception{
		JSONArray parameterJsonArray = new JSONArray();
		Element parametersEl = dataEl.element("parameters");
		if(parametersEl!=null){
			List<Element> parameterEls = parametersEl.elements("parameter");
			for(Element parameterEl : parameterEls){
				String parameterType= parameterEl.attributeValue("parameterType"); 
				String name= parameterEl.attributeValue("name"); 
				
				
				JSONObject parameterObj = new JSONObject();
				parameterObj.put("parameterType", parameterType);
				parameterObj.put("name", name);
				
				
				String value= parameterEl.attributeValue("value"); 
				//是否动态value
				if(value!=null && value.indexOf("$")>=0 || value.indexOf("<#") >=0){
					String parseString = value;
					if(params!=null){
						parseString = "<#assign _paramsString>"+params.toString()+"</#assign><#assign params = _paramsString?eval />"+parseString;
					}
					value = processTemplate(parseString, root,null);
				}
				parameterObj.put("value", value);
				
				parameterJsonArray.add(parameterObj);
			}
		}
		return parameterJsonArray;
	}

	
	public static String processTemplate(String templateString,Map<String, Object> root,File basePath ) throws Exception{
		Configuration cfg = new Configuration();
		cfg.setEncoding(Locale.CHINA, "UTF-8");
		if(basePath!=null){
			cfg.setDirectoryForTemplateLoading(basePath);
		}
		
		Properties p = new Properties();
		p.load(PageService.class.getResourceAsStream("/freemarker.properties"));
		cfg.setSettings(p);
		
		Template t = new Template("name", new StringReader(templateString),cfg);
		StringWriter writer = new StringWriter();
		t.process(root, writer);
		return writer.toString();
	}
	
}
