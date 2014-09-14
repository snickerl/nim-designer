package com.poweruniverse.nim.designer.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.DefaultAttribute;

import com.poweruniverse.nim.designer.servlet.Message;
import com.poweruniverse.nim.designer.utils.DataUtils;
import com.poweruniverse.nim.designer.utils.NativeSQLOrder;
import com.poweruniverse.nim.interfaces.message.ReturnI;
import com.poweruniverse.nim.message.JsonReturn;

/**
 * 与设计器有关的服务接口
 * 1、读取系统组件定义信息
 * 2、读取页面配置信息
 * 3、保存页面配置信息
 *
 */
public class DesignerService{
	private static final String cmpCfgFilePathName = "designer.component.xml";
	
	public static ReturnI savePageDef(String contextPath, String xmlFilePath,JSONObject pageDefineJson){
		ReturnI msg = null;
//		{
//			"type":"page",
//			"component":"page",
//			"children":[{
//				"type":"form",
//				"component":"simpleform",
//				"children":[{
//					"type":"fields",
//					"component":"fields",
//					"children":[{
//						"type":"number",
//						"component":"intfield"
//					}]
//				},{
//					"type":"buttons",
//					"component":"buttons"
//				}]
//			}]
//		}
		
		XMLWriter output = null;
		try {
			Document doc = DocumentHelper.createDocument();
			Element cfgEl = doc.addElement("page");
			
			applyJson2XML(cfgEl,pageDefineJson);
			
			OutputFormat format = OutputFormat.createPrettyPrint(); //设置XML文档输出格式
			format.setEncoding("utf-8"); //设置XML文档的编码类型
			
			output = new XMLWriter(new FileOutputStream(new File(contextPath+"module/"+xmlFilePath)),format);
			output.write(doc);
			output.close();
			output =null;
			
			msg = new JsonReturn();
		}catch(Exception e){
			msg = new JsonReturn(e.getMessage());
		}finally{
			if(output!=null){
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				output =null;
			}
		}
		return msg;
	}
	
	/**
	 * 读取module目录下的页面文件 解析页面的定义信息
	 * @return
	 */
	public static ReturnI readPageDef(String contextPath, String xmlFilePath){
		ReturnI msg = null;

		try {
			JSONObject pageDef = new JSONObject();
			
			File cfgFile = new File(contextPath+"module/"+xmlFilePath);
			if(cfgFile.exists()){
				SAXReader reader = new SAXReader();
				Document doc = reader.read(cfgFile);
				Element cfgEl = doc.getRootElement();
				
				pageDef = applyXML2Json(cfgEl);
				
				//临时代码 将页面标题字段名 从title改为label
				if(!pageDef.containsKey("label") && pageDef.containsKey("title")){
					pageDef.put("label", pageDef.getString("title")) ;
				}
			}
			msg = new JsonReturn("pageDef", pageDef);
		} catch (Exception e) {
			e.printStackTrace();
			msg = new JsonReturn("Exception："+e.getMessage());
		}
		return msg;
	}

	public static JSONObject applyXML2Json(Element cfgEl){
		JSONObject cfgJson = new JSONObject();
//		cfgJson.put("component", cfgEl.attributeValue("component"));
		
		for(Object attribute:cfgEl.attributes()){
			DefaultAttribute dAttr = (DefaultAttribute)attribute;
			cfgJson.put(dAttr.getName(), dAttr.getValue());
		}
		//读取type定义
		JSONArray childArray = new JSONArray();
		List<Element> childEls = cfgEl.elements();
		for(Element childEl:childEls){
			childArray.add(applyXML2Json(childEl));
		}
		cfgJson.put("children", childArray);
		return cfgJson;
	}
	
	private static void applyJson2XML(Element cfgEl,JSONObject cfgJson){
		for(Object keyObj:cfgJson.keySet()){
			String keyString = keyObj.toString();
			if(!keyString.equals("children") && !keyString.equals("xmlElName") && !keyString.startsWith("_") && !(cfgJson.get(keyString) instanceof JSONNull)){
				cfgEl.addAttribute(keyString, cfgJson.getString(keyString));
			}
		}
		if(cfgJson.containsKey("children")){
			JSONArray children = cfgJson.getJSONArray("children");
			for(int i=0;i<children.size();i++){
				JSONObject childrenJson = children.getJSONObject(i);
				
				Element childrenEl = cfgEl.addElement(childrenJson.getString("xmlElName"));
				
				applyJson2XML(childrenEl,childrenJson);
			}
		}
	}


	/**
	 * 将文件保存到module目录下（html css js文件 ）
	 * @return
	 */
	public static ReturnI saveFile(String contextPath,String pageUrl,String content){
		ReturnI msg = null;
		try {
			String filePathName = pageUrl.replaceAll("\\.\\.", "");
			File cfgFile = new File(contextPath+"module/"+filePathName);
			FileUtils.writeStringToFile(cfgFile, content);
			msg = new JsonReturn();
		} catch (Exception e) {
			e.printStackTrace();
			msg = new JsonReturn("Exception："+e.getMessage());
		}
		return msg;
	}
	
	/**
	 * 读取module目录下的页面html css js文件 
	 * @return
	 */
	public static ReturnI readFile(String contextPath, String pageUrl){
		ReturnI msg = null;
		try {
			String filePathName = pageUrl.replaceAll("\\.\\.", "");
			File cfgFile = new File(contextPath+"module/"+filePathName);
			if(cfgFile.exists()){
				String fileContent = FileUtils.readFileToString(cfgFile,"utf-8");
				msg = new JsonReturn("content", fileContent);
			}else{
				msg = new JsonReturn("content", "");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			msg = new JsonReturn("Exception："+e.getMessage());
		}
		return msg;
	}
	/**
	 * 读取web-inf/compnent.cfg.xml 文件 解析页面控件的定义信息
	 * @return
	 */
	public static ReturnI readCmpDef(String contextPath){
		ReturnI msg = null;
	
		XMLWriter output = null;
		try {
			JSONObject typeMap = new JSONObject();
			JSONObject cmpMap = new JSONObject();
			JSONObject editorTypeMap = new JSONObject();
			JSONObject editorMap = new JSONObject();
			
			File cfgFile = new File(contextPath+"WEB-INF/"+cmpCfgFilePathName);
			SAXReader reader = new SAXReader();
			Document doc = reader.read(cfgFile);
			Element cfgEl = doc.getRootElement();
			
			//读取type定义
			List<Element> typeDefEls = cfgEl.element("type-def").elements("type");
			for(Element typeDefEl:typeDefEls){
				JSONObject typeDefObj = new JSONObject();
	
				String typeName = typeDefEl.attributeValue("name");
				
				for(Object attribute:typeDefEl.attributes()){
					DefaultAttribute dAttr = (DefaultAttribute)attribute;
					typeDefObj.put(dAttr.getName(), dAttr.getValue());
				}
				
				JSONArray propertiesArray = new JSONArray();
				JSONArray extensionsArray = new JSONArray();
				JSONArray eventsArray = new JSONArray();
				JSONArray structureArray = new JSONArray();
				JSONArray dependenciesArray = new JSONArray();
				
				//基础属性
				if(typeDefEl.element("properties")!=null){
					List<Element> propertyDefEls = typeDefEl.element("properties").elements("property");
					for(Element propertyDefEl:propertyDefEls){
						JSONObject propertyDefObj = new JSONObject();
						for(Object attribute:propertyDefEl.attributes()){
							DefaultAttribute dAttr = (DefaultAttribute)attribute;
							propertyDefObj.put(dAttr.getName(), dAttr.getValue());
						}
						propertiesArray.add(propertyDefObj);
					}
					typeDefObj.put("properties", propertiesArray);
				}
				//基础扩展
				if(typeDefEl.element("extensions")!=null){
					List<Element> extensionDefEls = typeDefEl.element("extensions").elements("property");
					for(Element extensionDefEl:extensionDefEls){
						JSONObject extensionDefObj = new JSONObject();
						for(Object attribute:extensionDefEl.attributes()){
							DefaultAttribute dAttr = (DefaultAttribute)attribute;
							extensionDefObj.put(dAttr.getName(), dAttr.getValue());
						}
						extensionsArray.add(extensionDefObj);
					}
					typeDefObj.put("extensions", extensionsArray);
				}
				//基础事件
				if(typeDefEl.element("events")!=null){
					List<Element> eventDefEls = typeDefEl.element("events").elements("event");
					for(Element eventDefEl:eventDefEls){
						JSONObject eventDefObj = new JSONObject();
						//<property code="compnentType" label="控件类型" type="string" allowEmpty="false" info="" default=""/>
						for(Object attribute:eventDefEl.attributes()){
							DefaultAttribute dAttr = (DefaultAttribute)attribute;
							eventDefObj.put(dAttr.getName(), dAttr.getValue());
						}
						eventsArray.add( eventDefObj);
					}
					typeDefObj.put("events", eventsArray);
				}
				//基础结构
				if(typeDefEl.element("structure")!=null){
					List<Element> itemDefEls = typeDefEl.element("structure").elements("item");
					for(Element itemDefEl:itemDefEls){
						JSONObject itemDefObj = new JSONObject();
						//<property code="compnentType" label="控件类型" type="string" allowEmpty="false" info="" default=""/>
						for(Object attribute:itemDefEl.attributes()){
							DefaultAttribute dAttr = (DefaultAttribute)attribute;
							itemDefObj.put(dAttr.getName(), dAttr.getValue());
						}
						structureArray.add(itemDefObj);
					}
					typeDefObj.put("structure", structureArray);
				}
				//依赖的元素
				if(typeDefEl.element("dependencies")!=null){
					List<Element> dependencyEls = typeDefEl.element("dependencies").elements("dependency");
					for(Element dependencyEl:dependencyEls){
						JSONObject dependencyObj = new JSONObject();
						for(Object attribute:dependencyEl.attributes()){
							DefaultAttribute dAttr = (DefaultAttribute)attribute;
							dependencyObj.put(dAttr.getName(), dAttr.getValue());
						}
						dependenciesArray.add(dependencyObj);
					}
					typeDefObj.put("dependencies", dependenciesArray);
				}
				
				typeMap.put(typeName,typeDefObj);
			}
			//读取component定义
			List<Element> cmpDefEls = cfgEl.element("component-def").elements("component");
			for(Element cmpDefEl:cmpDefEls){
				JSONObject cmpDefObj = new JSONObject();
	
				String cmpName = cmpDefEl.attributeValue("name");
				
				for(Object attribute:cmpDefEl.attributes()){
					DefaultAttribute dAttr = (DefaultAttribute)attribute;
					cmpDefObj.put(dAttr.getName(), dAttr.getValue());
				}
				
				JSONArray propertiesArray = new JSONArray();
				JSONArray extensionsArray = new JSONArray();
				JSONArray eventsArray = new JSONArray();
				JSONArray structureArray = new JSONArray();
				JSONArray dependenciesArray = new JSONArray();
	
				//属性
				if(cmpDefEl.element("properties")!=null){
					List<Element> propertyDefEls = cmpDefEl.element("properties").elements("property");
					for(Element propertyDefEl:propertyDefEls){
						JSONObject propertyDefObj = new JSONObject();
	
						for(Object attribute:propertyDefEl.attributes()){
							DefaultAttribute dAttr = (DefaultAttribute)attribute;
							propertyDefObj.put(dAttr.getName(), dAttr.getValue());
						}
						
						propertiesArray.add(propertyDefObj);
					}
					cmpDefObj.put("properties", propertiesArray);
				}
				
				//扩展
				if(cmpDefEl.element("extensions")!=null){
					List<Element> extensionDefEls = cmpDefEl.element("extensions").elements("property");
					for(Element extensionDefEl:extensionDefEls){
						JSONObject extensionDefObj = new JSONObject();
	
						for(Object attribute:extensionDefEl.attributes()){
							DefaultAttribute dAttr = (DefaultAttribute)attribute;
							extensionDefObj.put(dAttr.getName(), dAttr.getValue());
						}
						
						extensionsArray.add(extensionDefObj);
					}
					cmpDefObj.put("extensions", extensionsArray);
				}

				//事件
				if(cmpDefEl.element("events")!=null){
					List<Element> eventDefEls = cmpDefEl.element("events").elements("event");
					for(Element eventDefEl:eventDefEls){
						JSONObject eventDefObj = new JSONObject();
						//<property code="compnentType" label="控件类型" type="string" allowEmpty="false" info="" default=""/>
						String eventCode = eventDefEl.attributeValue("code");
						
						for(Object attribute:eventDefEl.attributes()){
							DefaultAttribute dAttr = (DefaultAttribute)attribute;
							eventDefObj.put(dAttr.getName(), dAttr.getValue());
						}
						
						eventsArray.add(eventDefObj);
					}
					cmpDefObj.put("events", eventsArray);
				}
				//扩展结构
				if(cmpDefEl.element("structure")!=null){
					List<Element> itemDefEls = cmpDefEl.element("structure").elements("item");
					for(Element itemDefEl:itemDefEls){
						JSONObject itemDefObj = new JSONObject();
						//<property code="compnentType" label="控件类型" type="string" allowEmpty="false" info="" default=""/>
						for(Object attribute:itemDefEl.attributes()){
							DefaultAttribute dAttr = (DefaultAttribute)attribute;
							itemDefObj.put(dAttr.getName(), dAttr.getValue());
						}
						structureArray.add(itemDefObj);
					}
					cmpDefObj.put("structure", structureArray);
				}
				//依赖的元素
				if(cmpDefEl.element("dependencies")!=null){
					List<Element> dependencyEls = cmpDefEl.element("dependencies").elements("dependency");
					for(Element dependencyEl:dependencyEls){
						JSONObject dependencyObj = new JSONObject();
						
						for(Object attribute:dependencyEl.attributes()){
							DefaultAttribute dAttr = (DefaultAttribute)attribute;
							dependencyObj.put(dAttr.getName(), dAttr.getValue());
						}
						dependenciesArray.add(dependencyObj);
					}
					cmpDefObj.put("dependencies", dependenciesArray);
				}
	
				cmpMap.put(cmpName,cmpDefObj);
			}
			
			//读取editor type定义
			if(cfgEl.element("editor-def")!=null){
				List<Element> editorTypeDefEls = cfgEl.element("editor-def").elements("type");
				for(Element editorTypeDefEl:editorTypeDefEls){
					JSONObject typeDefObj = new JSONObject();
		
					String typeName = editorTypeDefEl.attributeValue("name");
					
					for(Object attribute:editorTypeDefEl.attributes()){
						DefaultAttribute dAttr = (DefaultAttribute)attribute;
						typeDefObj.put(dAttr.getName(), dAttr.getValue());
					}
					
					JSONArray propertiesArray = new JSONArray();
					JSONArray extensionsArray = new JSONArray();
					JSONArray eventsArray = new JSONArray();
					JSONArray structureArray = new JSONArray();
					JSONArray dependenciesArray = new JSONArray();
					
					//基础属性
					if(editorTypeDefEl.element("properties")!=null){
						List<Element> propertyDefEls = editorTypeDefEl.element("properties").elements("property");
						for(Element propertyDefEl:propertyDefEls){
							JSONObject propertyDefObj = new JSONObject();
							for(Object attribute:propertyDefEl.attributes()){
								DefaultAttribute dAttr = (DefaultAttribute)attribute;
								propertyDefObj.put(dAttr.getName(), dAttr.getValue());
							}
							propertiesArray.add(propertyDefObj);
						}
						typeDefObj.put("properties", propertiesArray);
					}
					//基础扩展
					if(editorTypeDefEl.element("extensions")!=null){
						List<Element> extensionDefEls = editorTypeDefEl.element("extensions").elements("property");
						for(Element extensionDefEl:extensionDefEls){
							JSONObject extensionDefObj = new JSONObject();
							for(Object attribute:extensionDefEl.attributes()){
								DefaultAttribute dAttr = (DefaultAttribute)attribute;
								extensionDefObj.put(dAttr.getName(), dAttr.getValue());
							}
							extensionsArray.add(extensionDefObj);
						}
						typeDefObj.put("extensions", extensionsArray);
					}
					//基础事件
					if(editorTypeDefEl.element("events")!=null){
						List<Element> eventDefEls = editorTypeDefEl.element("events").elements("event");
						for(Element eventDefEl:eventDefEls){
							JSONObject eventDefObj = new JSONObject();
							//<property code="compnentType" label="控件类型" type="string" allowEmpty="false" info="" default=""/>
							for(Object attribute:eventDefEl.attributes()){
								DefaultAttribute dAttr = (DefaultAttribute)attribute;
								eventDefObj.put(dAttr.getName(), dAttr.getValue());
							}
							eventsArray.add( eventDefObj);
						}
						typeDefObj.put("events", eventsArray);
					}
					//基础结构
					if(editorTypeDefEl.element("structure")!=null){
						List<Element> itemDefEls = editorTypeDefEl.element("structure").elements("item");
						for(Element itemDefEl:itemDefEls){
							JSONObject itemDefObj = new JSONObject();
							//<property code="compnentType" label="控件类型" type="string" allowEmpty="false" info="" default=""/>
							for(Object attribute:itemDefEl.attributes()){
								DefaultAttribute dAttr = (DefaultAttribute)attribute;
								itemDefObj.put(dAttr.getName(), dAttr.getValue());
							}
							structureArray.add(itemDefObj);
						}
						typeDefObj.put("structure", structureArray);
					}
					//依赖的元素
					if(editorTypeDefEl.element("dependencies")!=null){
						List<Element> dependencyEls = editorTypeDefEl.element("dependencies").elements("dependency");
						for(Element dependencyEl:dependencyEls){
							JSONObject dependencyObj = new JSONObject();
							for(Object attribute:dependencyEl.attributes()){
								DefaultAttribute dAttr = (DefaultAttribute)attribute;
								dependencyObj.put(dAttr.getName(), dAttr.getValue());
							}
							dependenciesArray.add(dependencyObj);
						}
						typeDefObj.put("dependencies", dependenciesArray);
					}
					
					editorTypeMap.put(typeName,typeDefObj);
				}
			}
			
			//读取editor定义
			if(cfgEl.element("editor-def")!=null){
				List<Element> editorDefEls = cfgEl.element("editor-def").elements("component");
				for(Element editorDefEl:editorDefEls){
					JSONObject cmpDefObj = new JSONObject();
		
					String cmpName = editorDefEl.attributeValue("name");
					
					for(Object attribute:editorDefEl.attributes()){
						DefaultAttribute dAttr = (DefaultAttribute)attribute;
						cmpDefObj.put(dAttr.getName(), dAttr.getValue());
					}
					
					JSONArray propertiesArray = new JSONArray();
					JSONArray extensionsArray = new JSONArray();
					JSONArray eventsArray = new JSONArray();
					JSONArray structureArray = new JSONArray();
					JSONArray dependenciesArray = new JSONArray();
		
					//属性
					if(editorDefEl.element("properties")!=null){
						List<Element> propertyDefEls = editorDefEl.element("properties").elements("property");
						for(Element propertyDefEl:propertyDefEls){
							JSONObject propertyDefObj = new JSONObject();
		
							for(Object attribute:propertyDefEl.attributes()){
								DefaultAttribute dAttr = (DefaultAttribute)attribute;
								propertyDefObj.put(dAttr.getName(), dAttr.getValue());
							}
							
							propertiesArray.add(propertyDefObj);
						}
						cmpDefObj.put("properties", propertiesArray);
					}
					
					//扩展
					if(editorDefEl.element("extensions")!=null){
						List<Element> extensionDefEls = editorDefEl.element("extensions").elements("property");
						for(Element extensionDefEl:extensionDefEls){
							JSONObject extensionDefObj = new JSONObject();
		
							for(Object attribute:extensionDefEl.attributes()){
								DefaultAttribute dAttr = (DefaultAttribute)attribute;
								extensionDefObj.put(dAttr.getName(), dAttr.getValue());
							}
							
							extensionsArray.add(extensionDefObj);
						}
						cmpDefObj.put("extensions", extensionsArray);
					}

					//事件
					if(editorDefEl.element("events")!=null){
						List<Element> eventDefEls = editorDefEl.element("events").elements("event");
						for(Element eventDefEl:eventDefEls){
							JSONObject eventDefObj = new JSONObject();
							//<property code="compnentType" label="控件类型" type="string" allowEmpty="false" info="" default=""/>
							String eventCode = eventDefEl.attributeValue("code");
							
							for(Object attribute:eventDefEl.attributes()){
								DefaultAttribute dAttr = (DefaultAttribute)attribute;
								eventDefObj.put(dAttr.getName(), dAttr.getValue());
							}
							
							eventsArray.add(eventDefObj);
						}
						cmpDefObj.put("events", eventsArray);
					}
					//扩展结构
					if(editorDefEl.element("structure")!=null){
						List<Element> itemDefEls = editorDefEl.element("structure").elements("item");
						for(Element itemDefEl:itemDefEls){
							JSONObject itemDefObj = new JSONObject();
							//<property code="compnentType" label="控件类型" type="string" allowEmpty="false" info="" default=""/>
							for(Object attribute:itemDefEl.attributes()){
								DefaultAttribute dAttr = (DefaultAttribute)attribute;
								itemDefObj.put(dAttr.getName(), dAttr.getValue());
							}
							structureArray.add(itemDefObj);
						}
						cmpDefObj.put("structure", structureArray);
					}
					//依赖的元素
					if(editorDefEl.element("dependencies")!=null){
						List<Element> dependencyEls = editorDefEl.element("dependencies").elements("dependency");
						for(Element dependencyEl:dependencyEls){
							JSONObject dependencyObj = new JSONObject();
							
							for(Object attribute:dependencyEl.attributes()){
								DefaultAttribute dAttr = (DefaultAttribute)attribute;
								dependencyObj.put(dAttr.getName(), dAttr.getValue());
							}
							dependenciesArray.add(dependencyObj);
						}
						cmpDefObj.put("dependencies", dependenciesArray);
					}
		
					editorMap.put(cmpName,cmpDefObj);
				}
			}
			msg = new JsonReturn();
			msg.put("types", typeMap);
			msg.put("components", cmpMap);
			msg.put("editorTypes", editorTypeMap);
			msg.put("editors", editorMap);
		} catch (Exception e) {
			e.printStackTrace();
			msg = new JsonReturn("Exception："+e.getMessage());
		}finally{
			if(output!=null){
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return msg;
	}
	
	
	//取得实体类数据
	public static ReturnI loadSTL(String xiTongDH,String gongNengDH1,String shiTiLeiDH,String property,String fields) {
		ReturnI retMsg = null;
		try{
			if(xiTongDH == null){
				return new JsonReturn("必须提供xiTongDH参数，用于调用此方法(DesignerService.loadSTL)！");
			}
			if(gongNengDH1 == null && shiTiLeiDH==null){
				return new JsonReturn("必须提供gongNengDH或shiTiLeiDH参数，用于调用此方法(DesignerService.loadSTL)！");
			}
			if(fields == null){
				return new JsonReturn("必须提供fields参数，用于调用此方法(DesignerService.loadSTL)！");
			}
			JSONArray fieldJsonArray = JSONArray.fromObject(fields);
			
			ShiTiLei stl = ShiTiLei.getShiTiLeiByDH("SYS_ZiDuan");
			if(stl==null){
				return new Message("实体类SYS_ZiDuan不存在！");
			}
			
			ShiTiLei stlObj = null;
			if(gongNengDH1!=null){
				GongNeng gn = GongNeng.getGongNengByDH(gongNengDH1);
				if(gn==null){
					return new JsonReturn("功能:"+gongNengDH1+"不存在！");
				}
				stlObj = gn.getShiTiLei();
			}else if(shiTiLeiDH!=null){
				stlObj = ShiTiLei.getShiTiLeiByDH(shiTiLeiDH);
			}
			//当前实体类对象
			if(property!=null && property.length()>0){
				ZiDuan zd = stlObj.getZiDuan(property);
				if(zd==null){
					return new JsonReturn("字段("+stlObj.getShiTiLeiMC()+"."+property+")不存在！");
				}else if(zd.getGuanLianSTL()==null){
					return new JsonReturn("字段("+stlObj.getShiTiLeiMC()+"."+property+")未定义关联实体类！");
				}
				stlObj = zd.getGuanLianSTL();
			}
			//查找此实体类下的所有字段
			Session sess = HibernateSessionFactory.getSession();
			@SuppressWarnings("unchecked")
			List<ZiDuan> zds = (List<ZiDuan>)sess.createCriteria(ZiDuan.class)
					.add(Restrictions.eq("shiTiLei.id", stlObj.getShiTiLeiDM()))
					.addOrder(NativeSQLOrder.raw("( case ziduanlxdm when 8 then -20 when 9 then -18 when 1 then -16 when 7 then -14 when 2 then -12 when 10 then -10 else ziduanlxdm end)"))
					.list();
			//
			JSONArray zdsArray = DataUtils.applyList2JSONArray(stl,zds,fieldJsonArray);
			
			retMsg = new JsonReturn("data",zdsArray);
			
		}catch (Exception e){
			retMsg = new JsonReturn(e.getMessage());
			e.printStackTrace();
		}
		return retMsg;
	}

}
