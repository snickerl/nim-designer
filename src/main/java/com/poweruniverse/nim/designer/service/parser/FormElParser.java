package com.poweruniverse.nim.designer.service.parser;

import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.dom4j.Element;

import com.poweruniverse.nim.system.service.DesignerService;

public class FormElParser {

	/**
	 * 集合类型数据源的解析
	 */
	public static JSONObject parseFormEl(Element formEl,JSONObject params,Map<String, Object> root,Integer yongHuDM) throws Exception{
		String formScriptContent = "";
		String dataLoadContent = "";

		String label = formEl.attributeValue("label");
		String name = formEl.attributeValue("name");
		
		if("searchToPage".equals(formEl.attributeValue("component"))){
			//查询 结果显示到新页面
			String autoRender = formEl.attributeValue("autoRender"); 
			if(autoRender==null){
				autoRender = "false";
			}
			String renderto = formEl.attributeValue("renderto");
			String renderType = formEl.attributeValue("renderType");
			String target = formEl.attributeValue("target");
			
			//searchToGrid events
			JSONObject listenersObj = new JSONObject();
			listenersObj.put("beforeSearch",formEl.attributeValue("beforeSearch"));
			
			//检查buttons设置
			JSONArray buttonArray = new JSONArray();
			Element formButtonsEl = formEl.element("buttons");
			if(formButtonsEl!=null){
				List<Element> formButtonEls = formButtonsEl.elements("button");
				if(formButtonEls!=null && formButtonEls.size()>0){
					for(Element formButtonEl : formButtonEls){
						JSONObject buttonObj = DesignerService.applyXML2Json(formButtonEl);
						buttonArray.add(buttonObj);
					}
				}
			}
			
			//检查filters设置
			JSONArray filterArray = new JSONArray();
			Element filtersEl = formEl.element("filters");
			if(filtersEl!=null){
				List<Element> filterEls = filtersEl.elements("filter");
				if(filterEls!=null && filterEls.size()>0){
					for(Element filterEl : filterEls){
						JSONObject filterObj = DesignerService.applyXML2Json(filterEl);
						filterArray.add(filterObj);
					}
				}
			}
			
			formScriptContent+="\n//生成查询表单("+label+":"+name+")\n";
			formScriptContent+="var _searchToPage_"+name+" = LUI.SearchPageForm.createNew({\n" +
					"name:'" +name+"',\n"+
					"label:'" +label+"',\n"+
					"renderto:'" +renderto+"',\n"+
					"autoRender:" +autoRender+",\n"+
					"renderType:'" +renderType+"',\n"+
					"target:'" +target+"',\n"+
					"filters:" +filterArray.toString()+",\n"+
					"buttons:" +buttonArray.toString()+",\n"+
					"listenerDefs:" +listenersObj.toString()+"\n"+
			"});\n";
			//注册
			formScriptContent += "_page_widget.register(_searchToPage_"+name+")\n";
		}else if("dataDisplayForm".equals(formEl.attributeValue("component"))){
			//用于显示数据的表单
			String renderto = formEl.attributeValue("renderto");
			String autoLoad= formEl.attributeValue("autoLoad"); 
			//dataDisplayForm events
			JSONObject listenersObj = new JSONObject();
			listenersObj.put("onRender",formEl.attributeValue("onRender"));
			

			
			//检查colums设置
			JSONArray fields = new JSONArray();
			Element fieldsEl = formEl.element("fields");
			if(fieldsEl!=null){
				List<Element> fieldEls = fieldsEl.elements("field");
				for(Element fieldEl : fieldEls){
					String fieldName = fieldEl.attributeValue("name");
					String fieldLabel = fieldEl.attributeValue("label");
					String fieldType = fieldEl.attributeValue("fieldType");
					
					String fieldRenderto = fieldEl.attributeValue("renderto");
					String fieldRenderTemplate = fieldEl.attributeValue("renderTemplate");
					
					String fieldShowTips = fieldEl.attributeValue("showTips");
					String fieldTipsTemplate = fieldEl.attributeValue("tipsTemplate");
					
					JSONObject field = new JSONObject();
					field.put("name", fieldName);
					field.put("label", fieldLabel);
					field.put("fieldType", fieldType);
					field.put("renderto", fieldRenderto);
					field.put("renderTemplate", fieldRenderTemplate);
					field.put("showTips", fieldShowTips);
					field.put("tipsTemplate", fieldTipsTemplate);
					
					
					
					fields.add(field);
				}
			}
			
			formScriptContent+="\n//生成表单"+label+":"+name+"的显示\n";
			//检查datasource设置
			String datasourceType = formEl.attributeValue("datasourceType");
			String datasourceName = formEl.attributeValue("datasourceName");
			
			Element datasourceEl = formEl.element("record");
			if(datasourceEl!=null){
				datasourceName = name+"_datasource_";
				
				JSONObject _dataRecordResult = DatasourceElParser.parseDataRecordEl(datasourceEl, params, root, yongHuDM,datasourceName);
				formScriptContent += _dataRecordResult.getString("dataScriptContent");
				dataLoadContent += _dataRecordResult.getString("dataLoadContent");
			}
			
//			dataScriptContent+="$('#grid-template-"+name+" li').remove();\n";
			formScriptContent+="var _dataDisplayForm_"+name+" = LUI.DisplayForm.createNew({\n" +
					"name:'" +name+"',\n"+
					"datasourceType:'" +datasourceType+"',\n"+
					"datasourceName:'" +datasourceName+"',\n"+
					"autoLoad:" +autoLoad+",\n"+
					"renderto:'" +renderto+"',\n"+
					"fields:" +fields+",\n"+
					"listenerDefs:" +listenersObj.toString()+"\n"+
			"});\n";
		
			//注册
			formScriptContent += "_page_widget.register(_dataDisplayForm_"+name+")\n";
			///////////////////////////////////////////////////////////
		}else if("searchToGrid".equals(formEl.attributeValue("component"))){
			//查询 结果显示到新页面
			String autoRender = formEl.attributeValue("autoRender"); 
			if(autoRender==null){
				autoRender = "false";
			}
			String renderto = formEl.attributeValue("renderto");
			String renderType = formEl.attributeValue("renderType");
			String datasourceName = formEl.attributeValue("datasourceName");
			String autoSearch= formEl.attributeValue("autoSearch"); 
			if(autoSearch==null){
				autoSearch = "false";
			}
			//searchToGrid events
			JSONObject listenersObj = new JSONObject();
			listenersObj.put("beforeSearch",formEl.attributeValue("beforeSearch"));

			//检查buttons设置
			JSONArray buttonArray = new JSONArray();
			Element formButtonsEl = formEl.element("buttons");
			if(formButtonsEl!=null){
				List<Element> formButtonEls = formButtonsEl.elements("button");
				if(formButtonEls!=null && formButtonEls.size()>0){
					for(Element formButtonEl : formButtonEls){
						JSONObject buttonObj = DesignerService.applyXML2Json(formButtonEl);
						buttonArray.add(buttonObj);
					}
				}
			}
			
			//检查filters设置
			JSONArray filterArray = new JSONArray();
			Element filtersEl = formEl.element("filters");
			if(filtersEl!=null){
				List<Element> filterEls = filtersEl.elements("filter");
				if(filterEls!=null && filterEls.size()>0){
					for(Element filterEl : filterEls){
						JSONObject filterObj = DesignerService.applyXML2Json(filterEl);
						filterArray.add(filterObj);
					}
				}
			}
			
			formScriptContent+="\n//生成查询表单("+label+":"+name+")\n";
			formScriptContent+="var _searchToGrid_"+name+" = LUI.SearchDatasourceForm.createNew({\n" +
					"name:'" +name+"',\n"+
					"label:'" +label+"',\n"+
					"datasourceName:'" +datasourceName+"',\n"+
					"renderto:'" +renderto+"',\n"+
					"renderType:'" +renderType+"',\n"+
					"autoSearch:" +autoSearch+",\n"+
					"autoRender:" +autoRender+",\n"+
					"filters:" +filterArray.toString()+",\n"+
					"buttons:" +buttonArray.toString()+",\n"+
					"listenerDefs:" +listenersObj.toString()+"\n"+
			"});\n";
			if("true".equals(autoSearch)){
				formScriptContent+="_searchToGrid_"+name+".submit();\n";
			}
			//注册
			formScriptContent += "_page_widget.register(_searchToGrid_"+name+")\n";
		}else if("singleEditForm".equals(formEl.attributeValue("component"))){
			
			String renderto = formEl.attributeValue("renderto");
			String renderType = formEl.attributeValue("renderType");
			String autoLoad= formEl.attributeValue("autoLoad"); 
			String autoRender= formEl.attributeValue("autoRender"); 
			
			
			//保存表单时 所使用的参数
			String xiTongDH= formEl.attributeValue("xiTongDH"); 
			String gongNengDH= formEl.attributeValue("gongNengDH"); 
			String caoZuoDH= formEl.attributeValue("caoZuoDH");
			
			//searchToGrid events
			JSONObject listenersObj = new JSONObject();
			listenersObj.put("onLoad",formEl.attributeValue("onLoad"));
			listenersObj.put("onRender",formEl.attributeValue("onRender"));
			listenersObj.put("onSubmit",formEl.attributeValue("onSubmit"));
			
			String fieldsPreScript = "";
			JSONArray fieldArray = new JSONArray();
			Element fieldsEl = formEl.element("fields");
			if(fieldsEl!=null){
				List<Element> fieldEls = fieldsEl.elements("field");
				
				for(Element fieldEl : fieldEls){
					JSONObject fieldObj = DesignerService.applyXML2Json(fieldEl);
					fieldObj.remove("children");
					
					//处理字段的数据源定义
					Element fieldDatasetEl = fieldEl.element("dataset");
					if(fieldDatasetEl!=null){
						JSONObject fieldDatasetResult = DatasourceElParser.parseDatasetEl(fieldDatasetEl, params, root, yongHuDM,fieldObj.getString("name")+"_datasource_");
						fieldsPreScript += fieldDatasetResult.getString("dataScriptContent");
						fieldsPreScript += fieldDatasetResult.getString("dataLoadContent");
						
						fieldObj.put("datasourceName", fieldObj.getString("name")+"_datasource_");
					}
					//处理字段的表格定义
					//...
					//处理字段的树定义
					//...
					fieldArray.add(fieldObj);
				}
			}
			
			JSONArray buttonArray = new JSONArray();
			Element buttonsEl = formEl.element("buttons");
			if(buttonsEl!=null){
				List<Element> buttonEls = buttonsEl.elements("button");
				for(Element buttonEl : buttonEls){
					JSONObject buttonObj = DesignerService.applyXML2Json(buttonEl);
					buttonArray.add(buttonObj);
				}
			}
			
			formScriptContent+="\n//生成单表编辑表单("+label+":"+name+")\n";
			//检查datasource设置
			String datasourceType = formEl.attributeValue("datasourceType");
			String datasourceName = formEl.attributeValue("datasourceName");
			
			Element datasourceEl = formEl.element("record");
			if(datasourceEl!=null){
				datasourceName = name+"_datasource_";
				
				JSONObject _dataRecordResult = DatasourceElParser.parseDataRecordEl(datasourceEl, params, root, yongHuDM,datasourceName);
				formScriptContent += _dataRecordResult.getString("dataScriptContent");
				dataLoadContent += _dataRecordResult.getString("dataLoadContent");
			}
			
			if(fieldsPreScript.length()>0){
				formScriptContent += fieldsPreScript;
			}
			formScriptContent+="var _singleeditform_"+name+" = LUI.Form.createNew({\n" +
					"name:'" +name+"',\n"+
					"label:'" +label+"',\n"+
					(xiTongDH==null?("xiTongDH:'" +xiTongDH+"',\n"):"")+
					(gongNengDH==null?("gongNengDH:'" +gongNengDH+"',\n"):"")+
					(caoZuoDH==null?("caoZuoDH:'" +caoZuoDH+"',\n"):"")+
					"datasourceType:'" +datasourceType+"',\n"+
					"datasourceName:'" +datasourceName+"',\n"+
					"renderto:'" +renderto+"',\n"+
					"renderType:'" +renderType+"',\n"+
					"autoRender:" +autoRender+",\n"+
					"autoLoad:" +autoLoad+",\n"+
					"fields:" +fieldArray.toString()+",\n"+
					"buttons:" +buttonArray.toString()+",\n"+
					"listenerDefs:" +listenersObj.toString()+"\n"+
			"});\n";
			//注册
			formScriptContent += "_page_widget.register(_singleeditform_"+name+")\n";
		}
		JSONObject ret = new JSONObject();
		ret.put("formScriptContent", formScriptContent);
		ret.put("dataLoadContent", dataLoadContent);
		return ret;
	}
	

}
