package com.poweruniverse.nim.designer.service.parser;

import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.dom4j.Element;

import com.poweruniverse.nim.bean.UserInfo;

public class GridElParser {

	/**
	 * 集合类型数据源的解析
	 */
	public static JSONObject parseGridEl(Element gridEl,JSONObject params,Map<String, Object> root,UserInfo user) throws Exception{
		String gridScriptContent = "";
		String dataLoadContent = "";
		
		String name = gridEl.attributeValue("name");
		String label = gridEl.attributeValue("label");
		String autoLoad= gridEl.attributeValue("autoLoad"); 
		
		//gnDataset events
		JSONObject listenersObj = new JSONObject();
		
		if(gridEl.attributeValue("onRender")!=null){
			listenersObj.put("onGridRendered",gridEl.attributeValue("onRender"));
		}else{
			listenersObj.put("onGridRendered",gridEl.attributeValue("onGridRendered"));
		}
		listenersObj.put("onRowRendered",gridEl.attributeValue("onRowRendered"));
		listenersObj.put("onPagiRendered",gridEl.attributeValue("onPagiRendered"));
		
		//检查datasource设置
		String datasourceType = gridEl.attributeValue("datasourceType");
		String datasourceName = gridEl.attributeValue("datasourceName");
		
		Element datasourceEl = gridEl.element("dataset");
		if(datasourceEl!=null){
			datasourceName = name+"_datasource_";
			
			JSONObject fieldDatasetResult = DatasourceElParser.parseDatasetEl(datasourceEl, params, root, user,datasourceName);
			gridScriptContent += fieldDatasetResult.getString("dataScriptContent");
			dataLoadContent += fieldDatasetResult.getString("dataLoadContent");
		}
		
		if("displayGrid".equals(gridEl.attributeValue("component"))){
			//检查 renderto参数
			
			String renderto = gridEl.attributeValue("renderto");
			String pagiTarget = gridEl.attributeValue("pagiTarget");//分页栏生成目标
			String headerLineString = gridEl.attributeValue("headerLines");
			String footerLineString = gridEl.attributeValue("footerLines");
			
			int headerLines = 0;
			if(headerLineString!=null){
				headerLines = Integer.parseInt(headerLineString);
			}
			int footerLines = 0;
			if(footerLineString!=null){
				footerLines = Integer.parseInt(footerLineString);
			}
			String renderTemplate = gridEl.attributeValue("renderTemplate");
			if(renderto!=null && renderTemplate!=null && renderto.length()>0 && renderTemplate.length()>0){
//				templateScriptContent +="\n<script id='grid-template-"+name+"' type='text/x-handlebars-template'>\n";
//				templateScriptContent+="{{#each data.rows}}\n";
				
				
				//检查colums设置
				JSONArray colArray = new JSONArray();
				Element columnsEl = gridEl.element("columns");
				if(columnsEl!=null){
					List<Element> columnEls = columnsEl.elements("column");
					if(columnEls!=null){
						for(Element columnEl : columnEls){
							String colName = columnEl.attributeValue("name");
							String colLabel = columnEl.attributeValue("label");
							String colRenderto = columnEl.attributeValue("renderto");
							String colRenderTemplate = columnEl.attributeValue("renderTemplate");
							if(colRenderTemplate==null || colRenderTemplate.length()==0){
								colRenderTemplate = "{{"+colName+"}}";
							}
							
							String colShowTips = columnEl.attributeValue("showTips");
							String colTipsTemplate = columnEl.attributeValue("tipsTemplate");
							
							String colShowThousand= columnEl.attributeValue("showThousand");
							
//							if(colName!=null && colRenderto!=null && colName.length()>0 && colRenderto.length()>0){
//								int pos1 = renderTemplate.indexOf(">", renderTemplate.indexOf(colRenderto.substring(1)));
//								int pos2 = renderTemplate.indexOf("<", pos1);
//								if(colRenderTemplate==null || colRenderTemplate.length()==0){
//									renderTemplate = renderTemplate.substring(0,pos1+1)+"{{"+colName+"}}"+renderTemplate.substring(pos2);
//								}else{
//									renderTemplate = renderTemplate.substring(0,pos1+1)+colRenderTemplate+renderTemplate.substring(pos2);
//								}
//							}
							
							JSONObject colObj = new JSONObject();
							colObj.put("name", colName);
							colObj.put("label", colLabel);
							colObj.put("renderto", colRenderto);
							colObj.put("renderTemplate", colRenderTemplate);
							colObj.put("showTips", colShowTips);
							colObj.put("showThousand", colShowThousand);
							colObj.put("tipsTemplate", colTipsTemplate);
							
							colArray.add(colObj);
						}
					}
				}
//				templateScriptContent+=renderTemplate+"\n";
//				templateScriptContent+="{{/each}}\n";
//				templateScriptContent +="</script>\n";
//				
				gridScriptContent+="\n//生成表格"+label+":"+name+"的显示\n";
													
//				dataScriptContent+="$('#grid-template-"+name+" li').remove();\n";
				gridScriptContent+="var _griddisplay_"+name+" = LUI.Grid.createNew({\n" +
						"name:'" +name+"',\n"+
						"datasourceType:'" +datasourceType+"',\n"+
						"datasourceName:'" +datasourceName+"',\n"+
						"autoLoad:" +autoLoad+",\n"+
						"renderto:'" +renderto+"',\n"+
						"columns:" +colArray+",\n"+
						"pagiTarget:" +(pagiTarget==null?"null":("'"+pagiTarget+"'"))+",\n"+
						"listenerDefs:" +listenersObj.toString()+",\n"+
						"headerLines:" +headerLines+",\n"+
						"footerLines:" +footerLines+"\n"+
				"});\n";
			}
			//注册
			gridScriptContent += "_page_widget.register(_griddisplay_"+name+")\n";
		}else if("editGrid".equals(gridEl.attributeValue("component"))){
			
			//注册
			gridScriptContent += "_page_widget.register(_griddisplay_"+name+")\n";
		}else if("operateGrid".equals(gridEl.attributeValue("component"))){
			
			//注册
			gridScriptContent += "_page_widget.register(_griddisplay_"+name+")\n";
		}
	
		JSONObject ret = new JSONObject();
		ret.put("gridScriptContent", gridScriptContent);
		ret.put("dataLoadContent", dataLoadContent);
		return ret;
	}

}
