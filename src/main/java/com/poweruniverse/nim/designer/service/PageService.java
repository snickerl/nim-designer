package com.poweruniverse.nim.designer.service;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.login.Configuration;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.poweruniverse.nim.designer.service.parser.ActionElParser;
import com.poweruniverse.nim.designer.service.parser.DatasourceElParser;
import com.poweruniverse.nim.designer.service.parser.FormElParser;
import com.poweruniverse.nim.designer.service.parser.GridElParser;
import com.poweruniverse.nim.designer.service.parser.ImportElParser;
import com.poweruniverse.nim.designer.service.parser.PageElParser;
import com.poweruniverse.nim.designer.service.parser.TabpageElParser;
import com.poweruniverse.nim.interfaces.message.ReturnI;
import com.poweruniverse.nim.message.JsonReturn;

/**
 * 
 * @author Administrator
 *
 */
public class PageService {

	/**
	 * 读取页面内容 （经过freemarker处理）
	 * 结果包括两个部分的内容：html、json
	 * @param xiTongDH
	 * @param fileName
	 * @param params
	 * @return
	 */
	public static ReturnI html(String contextPath, String pageUrl,JSONObject params,Integer yongHuDM){
		ReturnI msg = new JsonReturn();
		msg.put("isLogged", !(yongHuDM==null));
		try {
			//检查pageUrl 是否合法(无.. js后缀)
			int hasDD = pageUrl.indexOf("..");
			if(hasDD ==-1 && (pageUrl.endsWith("html") || pageUrl.endsWith("ftl"))){
				Map<String, Object> root = new HashMap<String, Object>();
				//加入用户信息
				if(yongHuDM!=null){
					root.put("yongHu", com.poweruniverse.nim.system.utils.DataUtils.getYongHu(yongHuDM));
				}
				//加入当前用户的加密信息
				String datetimeKey = ""+Calendar.getInstance().getTimeInMillis();
				String userIdentifier = Encrypt.desEncrypt(""+yongHuDM, datetimeKey);
//				root.put("report_user", userIdentifier);
//				root.put("report_key",datetimeKey );
				
				String dataScriptContent = null;
				String pageName = null;
				//读取页面xml文件定义
				String cfgFileName = pageUrl.substring(0,pageUrl.indexOf("."))+".xml";
				File cfgFile = new File(contextPath+"module/"+cfgFileName);
				if(cfgFile.exists()){
					SAXReader reader = new SAXReader();
					Document doc = reader.read(cfgFile);
					Element cfgEl = doc.getRootElement();
					
					//将数据添加到页面的javascript脚本
					dataScriptContent = "//当前用户的加密信息\n" +
							"var report_user = '"+ userIdentifier+"';\n" +
							"var report_key = '"+ datetimeKey+"';\n\n" ;
					
					//处理page元素
					JSONObject pageResult = PageElParser.parsePageEl(cfgEl);
					//确定此页面是否需要登陆后才能访问
					if(pageResult.getBoolean("needsLogin") && yongHuDM==null){
						msg.setSuccess(false);
						msg.put("needsLogin", true);//此值已在上面的循环中设置
						msg.put("loginPage", "");
						return msg;
					}else{
						Iterator<String> keysIt = pageResult.keys();
						while(keysIt.hasNext()){
							String key = keysIt.next(); 
							if(key.equals("pageScriptContent")){
								dataScriptContent += pageResult.getString("pageScriptContent");
							}else{
								msg.put(key, pageResult.get(key));
							}
						}
					}

					
					String dataLoadContent = "//为自动加载的数据源 load数据\n";
					//处理variable数据源
					List<Element> variableEls = cfgEl.elements("variable");
					for(Element variableEl : variableEls){
						JSONObject datasetResult = DatasourceElParser.parseDataVariableEl(variableEl, params, root, yongHuDM);
						dataScriptContent += datasetResult.getString("dataScriptContent");
						dataLoadContent += datasetResult.getString("dataLoadContent");
					}
					//处理record数据源
					List<Element> recordEls = cfgEl.elements("record");
					for(Element recordEl : recordEls){
						JSONObject datasetResult = DatasourceElParser.parseDataRecordEl(recordEl, params, root, yongHuDM,null);
						dataScriptContent += datasetResult.getString("dataScriptContent");
						dataLoadContent += datasetResult.getString("dataLoadContent");
					}
					//处理dataset数据源
					List<Element> datasetEls = cfgEl.elements("dataset");
					for(Element datasetEl : datasetEls){
						JSONObject datasetResult = DatasourceElParser.parseDatasetEl(datasetEl, params, root, yongHuDM,null);
						dataScriptContent += datasetResult.getString("dataScriptContent");
						dataLoadContent += datasetResult.getString("dataLoadContent");
					}
					
					//处理 grid
					List<Element> gridEls = cfgEl.elements("grid");
					for(Element gridEl : gridEls){
						JSONObject datasetResult = GridElParser.parseGridEl(gridEl, params, root, yongHuDM);
						dataScriptContent += datasetResult.getString("gridScriptContent");
						dataLoadContent += datasetResult.getString("dataLoadContent");
					}
					
					//处理 form
					List<Element> formEls = cfgEl.elements("form");
					for(Element formEl : formEls){
						JSONObject datasetResult = FormElParser.parseFormEl(formEl, params, root, yongHuDM);
						dataScriptContent += datasetResult.getString("formScriptContent");
						dataLoadContent += datasetResult.getString("dataLoadContent");
					}
					//处理 tab
					List<Element> tabEls = cfgEl.elements("tab");
					for(Element tabEl : tabEls){
						dataScriptContent += TabpageElParser.parseTabpageEl(tabEl, params, root, yongHuDM);
					}
								
					
					//处理 action
					List<Element> actionEls = cfgEl.elements("action");
					for(Element actionEl : actionEls){
						dataScriptContent += ActionElParser.parseActionEl(actionEl, params, root, yongHuDM);
					}
					
					//处理import
					List<Element> importEls = cfgEl.elements("import");
					for(Element importEl : importEls){
						dataScriptContent += ImportElParser.parseImportEl(importEl, params, root, yongHuDM);
					}

					//所有数据源 自动加载数据的代码 添加到程序定义的结尾
					dataScriptContent += dataLoadContent;
					
					//页面加载完成
//					if(cfgEl.attributeValue("onLoad")!=null){
//						dataScriptContent+="\n//页面全部加载完成\n";
//						dataScriptContent+="LUI.Page.instance.();\n";
//					}
					//关闭mask的代码
					dataScriptContent+="\n//关闭mask\n";
					dataScriptContent+="$('#_pageContent').unmask();\n";
				
				}else{
					//不存在xml文件 此页面不需要登录
					msg.put("needsLogin", false);
				}
				//原始文本信息
				File tempFile = new File(contextPath+"module/"+pageUrl);
				String fileContent = FileUtils.readFileToString(tempFile,"utf-8");
				if(fileContent.indexOf("$")>=0 || fileContent.indexOf("<#") >=0){
					String parseString = fileContent;
					if(params!=null){
						parseString = "<#assign _paramsString>"+params.toString()+"</#assign><#assign params = _paramsString?eval />"+parseString;
					}
					fileContent = processTemplate(parseString,root,new File(contextPath+"module/"));
				}
				//可能存在的 根据配置文件 生成的脚本
//				msg = new JsonReturn("content", fileContent);
				msg.put("content", fileContent);
				if(dataScriptContent!=null){
					msg.put("script", dataScriptContent);
				}
				
				msg.put("subPageName", pageName);
			}else{
				msg.setSuccess(false);
				msg.put("needsLogin", false);
				msg.setErrorMsg("错误的地址");
			}
		}catch (Exception e) {
			e.printStackTrace();
			msg.setSuccess(false);
			msg.put("needsLogin", false);
			msg.setErrorMsg("Exception："+e.getMessage());
		}
		return msg;
	}
	
	

	/**
	 * 读取module目录下的页面css文件 
	 * @return
	 */
	public static ReturnI css(String contextPath, String pageUrl){
		ReturnI msg = null;
		try {
			//检查pageUrl 是否合法(无.. js后缀)
			int hasDD = pageUrl.indexOf("..");
			if(hasDD>=0 || !pageUrl.endsWith("css")){
				msg = new JsonReturn("错误的地址");
			}else{
				File cfgFile = new File(contextPath+"module/"+pageUrl);
				String fileContent = FileUtils.readFileToString(cfgFile,"utf-8");
				msg = new JsonReturn("content", fileContent);
			}
		} catch (Exception e) {
			e.printStackTrace();
			msg = new JsonReturn("Exception："+e.getMessage());
		}
		return msg;
	}

	/**
	 * 读取module目录下的页面js文件 
	 * @return
	 */
	public static ReturnI js(String contextPath, String jsFileUrl){
		ReturnI msg = null;
		try {
			//检查pageUrl 是否合法(无.. js后缀)
			int hasDD = jsFileUrl.indexOf("..");
			if(hasDD>=0 || !jsFileUrl.endsWith("js")){
				msg = new JsonReturn("错误的地址");
			}else{
				File cfgFile = new File(contextPath+"module/"+jsFileUrl);
				if(cfgFile.exists()){
					String fileContent = FileUtils.readFileToString(cfgFile,"utf-8");
					
//					fileContent = "try{\n"+fileContent+"}catch(e){\nalert(e.name + ':' + e.message);\n}\n";
					msg = new JsonReturn("content", fileContent);
				}else{
					msg = new JsonReturn("content", "");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			msg = new JsonReturn("Exception："+e.getMessage());
		}
		return msg;
	}


	/**
	 * 根据系统、功能、操作定义，以及绑定的主数据记录、当前用户等信息 
	 * 确定可以使用何页面完成请求
	 * @param xiTongDH
	 * @param gongNengDH
	 * @param caoZuoDH
	 * @param objid
	 * @return
	 */
	public JSONObject getCaoZuoMB(String xiTongDH,String gongNengDH,String caoZuoDH,Integer objid){
		JSONObject ret = null;
		try {
//			Environment env = (Environment)this.getThreadLocalRequest().getSession().getAttribute(Environment.ENV);
//			Integer yongHuDM = null;
//			if(env!=null && env.getUser()!=null){
//				yongHuDM = env.getUser().getYongHuDM();
//			}
//			sess = HibernateSessionFactory.getSession(xiTongDH);
//			ret = TemplateUtils.doGetCaoZuoMB(gongNengDH, caoZuoDH, contextPath, yongHuDM, objid);
		}catch (Exception e) {
			e.printStackTrace();
			ret = new JSONObject();
		}
		return ret;
	}
	

	


}
