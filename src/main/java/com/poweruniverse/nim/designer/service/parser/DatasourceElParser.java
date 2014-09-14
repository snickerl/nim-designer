package com.poweruniverse.nim.designer.service.parser;

import java.sql.Clob;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.dom4j.Element;

import com.poweruniverse.nim.bean.Components;
import com.poweruniverse.nim.bean.UserInfo;
import com.poweruniverse.nim.designer.utils.BaseJavaDatasource;
import com.poweruniverse.nim.designer.utils.PageAnalyseUtils;
import com.poweruniverse.nim.esb.message.InvokeEnvelope;
import com.poweruniverse.nim.esb.translator.DataRequestTranslator;
import com.poweruniverse.nim.esb.utils.ServiceRouter;
import com.poweruniverse.nim.interfaces.entity.ShiTiLeiI;
import com.poweruniverse.nim.interfaces.message.ReturnI;

public class DatasourceElParser {

	/**
	 * 集合类型数据源的解析
	 */
	public static JSONObject parseDatasetEl(Element datasetEl,JSONObject params,Map<String, Object> root,UserInfo user,String realDsName) throws Exception{
		String dataScriptContent = "";
		String dataLoadContent = "";
		//数据源名称
		String label= datasetEl.attributeValue("label"); 
		String name= datasetEl.attributeValue("name"); 
		if(realDsName!=null){
			name = realDsName;
		}
		String autoLoad= datasetEl.attributeValue("autoLoad"); 
		if("javaDataset".equals(datasetEl.attributeValue("component"))){
			//java类型的数据源
			String className = datasetEl.attributeValue("className"); 
			
			int start = 0;
			String startString = datasetEl.attributeValue("start");
			if(startString!=null){
				start = Integer.parseInt(startString);
			}
			
			int limit = 0;
			String limitString = datasetEl.attributeValue("limit");
			if(limitString!=null){
				limit = Integer.parseInt(limitString);
			}
			
			//toDoDataset events
			JSONObject listenersObj = new JSONObject();
			listenersObj.put("onLoad",datasetEl.attributeValue("onLoad"));
			
			JSONArray parameters =  PageAnalyseUtils.getParametersFromEl(datasetEl,root,params);
			
			Element fieldsEl = datasetEl.element("properties");
			JSONArray fieldJsonArray = new JSONArray();
			if(fieldsEl!=null){
				fieldJsonArray = PageAnalyseUtils.getFieldsDefByEL(fieldsEl,null,true);
			}
			
			//将数据转换为json格式 添加到页面中
			dataScriptContent += "\n//实体类 数据集（"+label+":"+name+"）\n";
			dataScriptContent += "var _dataset_"+name+" = LUI.Datasource.javaDataset.createNew({\n" +
					"name:'" +name+"',\n"+
					"label:'" +label+"',\n"+
					"className:'" +className+"',\n"+
					"start:" +start+",\n"+
					"limit:" +limit+",\n"+
					"autoLoad:" +autoLoad+",\n"+
					"listenerDefs:" +listenersObj.toString()+",\n"+
					"fields:" +fieldJsonArray+",\n"+
					"parameters:" +parameters+"\n"+
			"});\n";
			if("true".equals(autoLoad)){
				int totalCount = 0;
				//取得数据对象(用老的程序方法)
				JSONObject jsonData = null;
				try {
					BaseJavaDatasource javaDS = (BaseJavaDatasource)Class.forName(className).newInstance();
					JSONObject pa = new JSONObject();
					for(int i=0;i<parameters.size();i++){
						JSONObject p = parameters.getJSONObject(i);
						pa.put(p.get("name"), p.get("value"));
					}
					jsonData = javaDS.getData(pa,start,limit);
				} catch (Exception e) {
					jsonData = new JSONObject();
					jsonData.put("totalCount", 0);
					jsonData.put("start", start);
					jsonData.put("limit", limit);
					jsonData.put("rows", new JSONArray());
					e.printStackTrace();
				}
				
				dataScriptContent += "_dataset_"+name+"_init_data = \n" +
						jsonData.toString()+"\n"+
				";\n";
				dataLoadContent += "_dataset_"+name+".loadData(_dataset_"+name+"_init_data);\n\n";
				
				//
				root.put(name+"_totalCount", totalCount);
				root.put(name, jsonData);
			}
			
			//注册
			dataScriptContent += "_page_widget.register(_dataset_"+name+")\n";
		}else if("sqlDataset".equals(datasetEl.attributeValue("component"))){
			String xiTongDH= datasetEl.attributeValue("xiTongDH"); 
			
			String sqlString= datasetEl.attributeValue("sql"); 
			if(sqlString.indexOf("<#")>=0 || sqlString.indexOf("${")>=0 ){
				if(params!=null){
					sqlString = "<#assign _paramsString>"+params.toString()+"</#assign><#assign params = _paramsString?eval />"+sqlString;
				}
				sqlString = PageAnalyseUtils.processTemplate(sqlString, root,null);
			}
			sqlString = sqlString.replaceAll("\\\\n", " ");
			
			int start = 0;
			String startString = datasetEl.attributeValue("start");
			if(startString!=null){
				start = Integer.parseInt(startString);
			}
			
			int limit = 0;
			String limitString = datasetEl.attributeValue("limit");
			if(limitString!=null){
				limit = Integer.parseInt(limitString);
			}
			
			//onLoad events
			JSONObject listenersObj = new JSONObject();
			listenersObj.put("onLoad",datasetEl.attributeValue("onLoad"));
			

			JSONArray fieldJsonArray = null;
			Element fieldsEl = datasetEl.element("properties");
			if(fieldsEl==null){
				fieldJsonArray = JSONArray.fromObject("[]");
			}else{
				fieldJsonArray = PageAnalyseUtils.getFieldsDefByEL(fieldsEl,null,true);
			}
			
			//将数据转换为json格式 添加到页面中
			dataScriptContent += "\n//SQL 数据集（"+label+":"+name+"）\n";
			dataScriptContent += "var _dataset_"+name+" = LUI.Datasource.sqlDataset.createNew({\n" +
					"name:'" +name+"',\n"+
					"label:'" +label+"',\n"+
					"xiTongDH:'" +xiTongDH+"',\n"+
					"sql:\"" +sqlString+"\",\n"+
					"autoLoad:" +autoLoad+",\n"+
					"start:" +start+",\n"+
					"limit:" +limit+",\n"+
					"listenerDefs:" +listenersObj.toString()+",\n"+
					"fields:" +fieldJsonArray.toString()+"\n"+
			"});\n";
			if("true".equals(autoLoad)){
				InvokeEnvelope invokeEnvelope = DataRequestTranslator.getSqlInvokeEnvelope(Components.getComponent("nim-designer"),user,sqlString,start,limit,fieldJsonArray);
				
				ReturnI result = ServiceRouter.invokeService(invokeEnvelope);
				
				Integer totalCount = (Integer)result.get("totalCount");
				JSONArray rows = (JSONArray)result.get("data");
				
				dataScriptContent += "_dataset_"+name+"_init_data = {\n" +
						"start:" +start+",\n"+
						"limit:" +limit+",\n"+
						"totalCount:" +totalCount+",\n"+
						"rows:" +rows+"\n"+
				"};\n";
				dataLoadContent += "_dataset_"+name+".loadData(_dataset_"+name+"_init_data);\n\n";
				
				root.put(name, rows);
			}
			//注册
			dataScriptContent += "_page_widget.register(_dataset_"+name+")\n";
		}else if("todoDataset".equals(datasetEl.attributeValue("component"))){
			//待办的数据集
			String xiTongDH= datasetEl.attributeValue("xiTongDH"); 
			String shiTiLeiDH= datasetEl.attributeValue("shiTiLeiDH"); 
			
			//toDoDataset events
			JSONObject listenersObj = new JSONObject();
			listenersObj.put("onLoad",datasetEl.attributeValue("onLoad"));
			
			JSONObject result = com.poweruniverse.nim.system.utils.DataUtils.getToDoDataset(datasetEl, params,yongHuDM);
			if(result.getBoolean("success")){
				JSONArray objJsonArray = result.getJSONArray("data");
				int totalCount = result.getInt("totalCount");
				int start = result.getInt("start");
				int limit = result.getInt("limit");
				String fieldJsonString = result.getString("fields");
				String workflowJsonString = result.getString("workflows");
				//送到客户端
				dataScriptContent += "\n//待办 数据集（"+label+":"+name+"）\n";
				dataScriptContent += "var _dataset_"+name+" = LUI.Datasource.TodoDataset.createNew({\n" +
						"name:'" +name+"',\n"+
						"label:'" +label+"',\n"+
						"xiTongDH:'" +xiTongDH+"',\n"+
						"shiTiLeiDH:'" +shiTiLeiDH+"',\n"+
						"autoLoad:" +autoLoad+",\n"+
						"fields:" +fieldJsonString.toString()+",\n"+
						"workflows:" +workflowJsonString.toString()+",\n"+
						"listenerDefs:" +listenersObj.toString()+",\n"+
						"start:" +start+",\n"+
						"limit:" +limit+"\n"+
				"});\n";
				if("true".equals(autoLoad)){
					dataScriptContent += "_dataset_"+name+"_init_data = {\n" +
							"start:" +start+",\n"+
							"limit:" +limit+",\n"+
							"totalCount:" +totalCount+",\n"+
							"rows:" +objJsonArray.toString()+"\n"+
					"};\n";
					dataLoadContent += "_dataset_"+name+".loadData(_dataset_"+name+"_init_data);\n\n";
				}
			}else{
				throw new Exception(result.getString("errorMsg"));
			}
			//注册
			dataScriptContent += "_page_widget.register(_dataset_"+name+")\n";
		}else if("stlDataset".equals(datasetEl.attributeValue("component"))){

			String xiTongDH= datasetEl.attributeValue("xiTongDH"); 
			String shiTiLeiDH= datasetEl.attributeValue("shiTiLeiDH"); 
			
			int start = 0;
			String startString = datasetEl.attributeValue("start");
			if(startString!=null){
				start = Integer.parseInt(startString);
			}
			
			int limit = 0;
			String limitString = datasetEl.attributeValue("limit");
			if(limitString!=null){
				limit = Integer.parseInt(limitString);
			}
			
			JSONArray filters = PageAnalyseUtils.getFiltersFromEl(datasetEl,root,params);
			JSONArray sorts = PageAnalyseUtils.getSortsFromEl(datasetEl,root,params);
			
			//toDoDataset events
			JSONObject listenersObj = new JSONObject();
			listenersObj.put("onLoad",datasetEl.attributeValue("onLoad"));
			
			//取得json格式的数据
			ShiTiLeiI dataStl = ShiTiLei.getShiTiLeiByDH(shiTiLeiDH);
			if(dataStl==null){
				throw new Exception("实体类("+shiTiLeiDH+")不存在！");
			}

			JSONArray fieldJsonArray = null;
			Element fieldsEl = datasetEl.element("properties");
			if(fieldsEl==null){
				fieldJsonArray = JSONArray.fromObject("[{" +
					"name:'"+dataStl.getXianShiLie()+"',fieldType:'string'" +
				"},{" +
					"name:'"+dataStl.getPaiXuLie()+"',fieldType:'string'" +
				"}]");
			}else{
				fieldJsonArray = PageAnalyseUtils.getFieldsDefByEL(fieldsEl,dataStl,false);
			}
			
			//将数据转换为json格式 添加到页面中
			dataScriptContent += "\n//实体类 数据集（"+label+":"+name+"）\n";
			dataScriptContent += "var _dataset_"+name+" = LUI.Datasource.stlDataset.createNew({\n" +
					"name:'" +name+"',\n"+
					"label:'" +label+"',\n"+
					"xiTongDH:'" +xiTongDH+"',\n"+
					"shiTiLeiDH:'" +shiTiLeiDH+"',\n"+
					"primaryFieldName:'" +dataStl.getZhuJianLie()+"',\n"+
					"autoLoad:" +autoLoad+",\n"+
					"start:" +start+",\n"+
					"limit:" +limit+",\n"+
					"listenerDefs:" +listenersObj.toString()+",\n"+
					"fields:" +fieldJsonArray.toString()+",\n"+
					"filters:" +filters+",\n"+
					"sorts:" +sorts+"\n"+
			"});\n";
			if("true".equals(autoLoad)){
				//取得数据对象(用老的程序方法)
				ModelData result = com.poweruniverse.nim.system.utils.DataUtils.getObjListByHibernate(shiTiLeiDH, start, limit, filters.toString(), sorts.toString(), yongHuDM);
				
//				ModelData result = com.poweruniverse.nim.system.utils.DataUtils.getStlDataset(datasetEl, params, root,yongHuDM);
				List<?> objs = result.get("data");
				int totalCount = result.get("totalCount");

				JSONArray jsonData = com.poweruniverse.nim.system.utils.DataUtils.applyList2JSONArray(dataStl,objs,fieldJsonArray);
				dataScriptContent += "_dataset_"+name+"_init_data = {\n" +
						"start:" +start+",\n"+
						"limit:" +limit+",\n"+
						"totalCount:" +totalCount+",\n"+
						"rows:" +jsonData.toString()+"\n"+
				"};\n";
				dataLoadContent += "_dataset_"+name+".loadData(_dataset_"+name+"_init_data);\n\n";
				
				//
				root.put(name+"_totalCount", totalCount);
				root.put(name+"_start", start);
				root.put(name+"_limit", limit);
				root.put(name, objs);
			}
			//注册
			dataScriptContent += "_page_widget.register(_dataset_"+name+")\n";
		}else if("gnDataset".equals(datasetEl.attributeValue("component"))){
			String xiTongDH= datasetEl.attributeValue("xiTongDH"); 
			String gongNengDH= datasetEl.attributeValue("gongNengDH"); 
			String caoZuoDH= datasetEl.attributeValue("caoZuoDH"); 
			
			//gnDataset events
			JSONObject listenersObj = new JSONObject();
			listenersObj.put("onLoad",datasetEl.attributeValue("onLoad"));
			
			int start = 0;
			String startString = datasetEl.attributeValue("start");
			if(startString!=null){
				start = Integer.parseInt(startString);
			}
			
			int limit = 0;
			String limitString = datasetEl.attributeValue("limit");
			if(limitString!=null){
				limit = Integer.parseInt(limitString);
			}
			
			JSONArray filters = PageAnalyseUtils.getFiltersFromEl(datasetEl,root,params);
			JSONArray sorts = PageAnalyseUtils.getSortsFromEl(datasetEl,root,params);
			
			//取得json格式的数据
			GongNeng dataGn = GongNeng.getGongNengByDH(gongNengDH);
			if(dataGn==null){
				throw new Exception("功能("+gongNengDH+")不存在！");
			}

			JSONArray fieldJsonArray = null;
			Element fieldsEl = datasetEl.element("properties");
			if(fieldsEl==null){
				fieldJsonArray = JSONArray.fromObject("[{" +
					"name:'"+dataGn.getShiTiLei().getXianShiLie()+"',fieldType:'string'" +
				"},{" +
					"name:'"+dataGn.getShiTiLei().getPaiXuLie()+"',fieldType:'string'" +
				"}]");
			}else{
				fieldJsonArray = PageAnalyseUtils.getFieldsDefByEL(fieldsEl,dataGn.getShiTiLei(),false);
			}
			
			//将数据转换为json格式 添加到页面中
			dataScriptContent += "\n//功能 数据集（"+label+":"+name+"）\n";
			dataScriptContent += "var _dataset_"+name+" = LUI.Datasource.gnDataset.createNew({\n" +
					"name:'" +name+"',\n"+
					"label:'" +label+"',\n"+
					"xiTongDH:'" +xiTongDH+"',\n"+
					"gongNengDH:'" +gongNengDH+"',\n"+
					"caoZuoDH:'" +caoZuoDH+"',\n"+
					"primaryFieldName:'" +dataGn.getShiTiLei().getZhuJianLie()+"',\n"+
					"autoLoad:" +autoLoad+",\n"+
					"start:" +start+",\n"+
					"limit:" +limit+",\n"+
					"listenerDefs:" +listenersObj.toString()+",\n"+
					"fields:" +fieldJsonArray.toString()+",\n"+
					"filters:" +filters+",\n"+
					"sorts:" +sorts+"\n"+
			"});\n";
			if("true".equals(autoLoad)){
				ModelData result = com.poweruniverse.nim.system.utils.DataUtils.getObjListByHibernate(gongNengDH, caoZuoDH, start, limit, filters.toString(), sorts.toString(), yongHuDM);
//				ModelData result = com.poweruniverse.nim.system.utils.DataUtils.getGnDataset(datasetEl, params, root,yongHuDM);
				List<?> objs = result.get("data");
				int totalCount = result.get("totalCount");
				
				JSONArray jsonData = com.poweruniverse.nim.system.utils.DataUtils.applyList2JSONArray(dataGn.getShiTiLei(),objs,fieldJsonArray);
				dataScriptContent += "var _dataset_"+name+"_init_data = {\n" +
						"start:" +start+",\n"+
						"limit:" +limit+",\n"+
						"totalCount:" +totalCount+",\n"+
						"rows:" +jsonData.toString()+"\n"+
				"};\n";
				dataLoadContent += "_dataset_"+name+".loadData(_dataset_"+name+"_init_data);\n\n";
				
				//
				root.put(name+"_totalCount", totalCount);
				root.put(name+"_start", start);
				root.put(name+"_limit", limit);
				root.put(name, objs);
			}
			//注册
			dataScriptContent += "_page_widget.register(_dataset_"+name+")\n";
		}
	
		JSONObject ret = new JSONObject();
		ret.put("dataScriptContent", dataScriptContent);
		ret.put("dataLoadContent", dataLoadContent);
		return ret;
	}
	
	/**
	 * 记录类型数据源的解析
	 */
	public static JSONObject parseDataRecordEl(Element recordEl,JSONObject params,Map<String, Object> root,UserInfo user,String realDsName) throws Exception{
		String dataScriptContent = "";
		String dataLoadContent = "";
		
		String name= recordEl.attributeValue("name"); 
		if(realDsName!=null){
			name = realDsName;
		}
		String label= recordEl.attributeValue("label"); 
		String xiTongDH= recordEl.attributeValue("xiTongDH"); 
		String autoLoad= recordEl.attributeValue("autoLoad"); 
		
		//toDoDataset events
		JSONObject listenersObj = new JSONObject();
		listenersObj.put("onLoad",recordEl.attributeValue("onLoad"));
		listenersObj.put("onSave",recordEl.attributeValue("onSave"));
		
		if("sqlRecord".equals(recordEl.attributeValue("component"))){
			
			String sqlString= recordEl.attributeValue("sql"); 
			
			if(sqlString.indexOf("<#")>=0 || sqlString.indexOf("${")>=0 ){
				if(params!=null){
					sqlString = "<#assign _paramsString>"+params.toString()+"</#assign><#assign params = _paramsString?eval />"+sqlString;
				}
				sqlString = PageAnalyseUtils.processTemplate(sqlString, root,null);
			}
			sqlString = sqlString.replaceAll("\\\\n", " ");
			
			JSONArray fieldJsonArray = null;
			Element fieldsEl = recordEl.element("properties");
			if(fieldsEl==null){
				fieldJsonArray = JSONArray.fromObject("[]");
			}else{
				fieldJsonArray = PageAnalyseUtils.getFieldsDefByEL(fieldsEl,null,true);
			}
			
			//将数据转换为json格式 添加到页面中
			dataScriptContent += "\n//SQL 数据对象（"+label+":"+name+"）\n";
			dataScriptContent += "var _dataRecord_"+name+" = LUI.Datasource.sqlDataRecord.createNew({\n" +
					"name:'" +name+"',\n"+
					"label:'" +label+"',\n"+
					"xiTongDH:'" +xiTongDH+"',\n"+
					"sql:\"" +sqlString+"\",\n"+
					"autoLoad:" +autoLoad+",\n"+
					"listenerDefs:" +listenersObj.toString()+",\n"+
					"fields:" +fieldJsonArray.toString()+"\n"+
			"});\n";
			if("true".equals(autoLoad)){
				JSONObject obj = com.poweruniverse.nim.system.utils.DataUtils.getSqlRecord( sqlString,fieldJsonArray);
				dataScriptContent += "_dataRecord_"+name+"_init_data = {\n" +
						"start:0,"+
						"limit:1,"+
						"totalCount:"+(obj.keySet().size()>0?1:0)+","+
						"rows:[" +obj.toString()+"]\n"+
				"};\n";
				dataLoadContent += "_dataRecord_"+name+".loadData(_dataRecord_"+name+"_init_data);\n\n";
				
				root.put(name, obj);
			}
			//注册
			dataScriptContent += "_page_widget.register(_dataRecord_"+name+")\n";
		}else if("gnRecord".equals(recordEl.attributeValue("component"))){
			
			String gongNengDH= recordEl.attributeValue("gongNengDH"); 
			String caoZuoDH= recordEl.attributeValue("caoZuoDH"); 
			String idString= recordEl.attributeValue("id"); 
			
			Integer id = null;
			if(idString!=null){
				if(idString.indexOf("<#")>=0 || idString.indexOf("${")>=0){
					if(params!=null){
						idString = "<#assign _paramsString>"+params.toString()+"</#assign><#assign params = _paramsString?eval />"+idString;
					}
					idString = PageAnalyseUtils.processTemplate(idString, root,null);
				}
				idString = idString.replaceAll("\\\\n", "");
				id = Integer.valueOf(idString);
			}
			
			GongNeng dataGn = GongNeng.getGongNengByDH(gongNengDH);
			if(dataGn==null){
				throw new Exception("功能("+gongNengDH+")不存在！");
			}
			
			JSONArray fieldJsonArray = null;
			Element fieldsEl = recordEl.element("properties");
			if(fieldsEl==null){
				fieldJsonArray = JSONArray.fromObject("[{" +
					"name:'"+dataGn.getShiTiLei().getXianShiLie()+"',fieldType:'string'" +
				"},{" +
					"name:'"+dataGn.getShiTiLei().getPaiXuLie()+"',fieldType:'string'" +
				"}]");
			}else{
				fieldJsonArray = PageAnalyseUtils.getFieldsDefByEL(fieldsEl,dataGn.getShiTiLei(),false);
			}
			
			//将数据转换为json格式 添加到页面中
			dataScriptContent += "\n//功能 数据对象（"+label+":"+name+"）\n";
			dataScriptContent += "var _dataRecord_"+name+" = LUI.Datasource.gnDataRecord.createNew({\n" +
					"name:'" +name+"',\n"+
					"label:'" +label+"',\n"+
					"xiTongDH:'" +xiTongDH+"',\n"+
					"gongNengDH:'" +gongNengDH+"',\n"+
					"caoZuoDH:'" +caoZuoDH+"',\n"+
					"primaryFieldName:'" +dataGn.getShiTiLei().getZhuJianLie()+"',\n"+
					"dataId:" +id+",\n"+
					"autoLoad:" +autoLoad+",\n"+
					"listenerDefs:" +listenersObj.toString()+",\n"+
					"fields:" +fieldJsonArray.toString()+"\n"+
			"});\n";
			if("true".equals(autoLoad)){
				Object obj = com.poweruniverse.nim.system.utils.DataUtils.getGnRecord(recordEl,id,params, root,yongHuDM);
				if(obj!=null){
					JSONObject jsonData = com.poweruniverse.nim.system.utils.DataUtils.applyEntity2JSONObject(dataGn.getShiTiLei(),obj,fieldJsonArray);
					dataScriptContent += "_dataRecord_"+name+"_init_data = {\n" +
							"start:0,"+
							"limit:1,"+
							"totalCount:1,"+
							"rows:[" +jsonData.toString()+"]\n"+
					"};\n";
				}else{
					dataScriptContent += "_dataRecord_"+name+"_init_data = {\n" +
							"start:0,"+
							"limit:1,"+
							"totalCount:0,"+
							"rows:[]\n"+
					"};\n";
				}
				dataLoadContent += "_dataRecord_"+name+".loadData(_dataRecord_"+name+"_init_data);\n\n";
				//
				root.put(name, obj);
			}
			
			//注册
			dataScriptContent += "_page_widget.register(_dataRecord_"+name+")\n";
		}else if("stlRecord".equals(recordEl.attributeValue("component"))){
			String shiTiLeiDH= recordEl.attributeValue("shiTiLeiDH"); 
			String id= recordEl.attributeValue("id"); 
			
			ShiTiLei dataStl = ShiTiLei.getShiTiLeiByDH(shiTiLeiDH);
			if(dataStl==null){
				throw new Exception("实体类("+shiTiLeiDH+")不存在！");
			}
			
			if(id.indexOf("<#")>=0 || id.indexOf("${")>=0 ){
				if(params!=null){
					id = "<#assign _paramsString>"+params.toString()+"</#assign><#assign params = _paramsString?eval />"+id;
				}
				id = PageAnalyseUtils.processTemplate(id, root,null);
			}
			id = id.replaceAll("\\\\n", " ");
			
			JSONArray fieldJsonArray = null;
			Element fieldsEl = recordEl.element("properties");
			if(fieldsEl==null){
				fieldJsonArray = JSONArray.fromObject("[{" +
					"name:'"+dataStl.getXianShiLie()+"',fieldType:'string'" +
				"},{" +
					"name:'"+dataStl.getPaiXuLie()+"',fieldType:'string'" +
				"}]");
			}else{
				fieldJsonArray = PageAnalyseUtils.getFieldsDefByEL(fieldsEl,dataStl,false);
			}
			
			//将数据转换为json格式 添加到页面中
			dataScriptContent += "\n//实体类 数据对象（"+label+":"+name+"）\n";
			dataScriptContent += "var _dataRecord_"+name+" = LUI.Datasource.stlDataRecord.createNew({\n" +
					"name:'" +name+"',\n"+
					"label:'" +label+"',\n"+
					"xiTongDH:'" +xiTongDH+"',\n"+
					"shiTiLeiDH:'" +shiTiLeiDH+"',\n"+
					"primaryFieldName:'" +dataStl.getZhuJianLie()+"',\n"+
					"dataId:'" +id+"',\n"+
					"autoLoad:" +autoLoad+",\n"+
					"listenerDefs:" +listenersObj.toString()+",\n"+
					"fields:" +fieldJsonArray.toString()+"\n"+
			"});\n";
			if("true".equals(autoLoad)){
				Object obj = com.poweruniverse.nim.system.utils.DataUtils.getStlRecord(recordEl,Integer.valueOf(id), params, root);
				if(obj!=null){
					JSONObject jsonData = com.poweruniverse.nim.system.utils.DataUtils.applyEntity2JSONObject(dataStl,obj,fieldJsonArray);
					dataScriptContent += "_dataRecord_"+name+"_init_data = {\n" +
							"start:0,"+
							"limit:1,"+
							"totalCount:1,"+
							"rows:[" +jsonData.toString()+"]\n"+
					"};\n";
				}else{
					dataScriptContent += "_dataRecord_"+name+"_init_data = {\n" +
							"start:0,"+
							"limit:1,"+
							"totalCount:0,"+
							"rows:[]\n"+
					"};\n";
				}
				dataLoadContent += "_dataRecord_"+name+".loadData(_dataRecord_"+name+"_init_data);\n\n";
				root.put(name, obj);
			}
			//
			//注册
			dataScriptContent += "_page_widget.register(_dataRecord_"+name+")\n";
		}
		JSONObject ret = new JSONObject();
		ret.put("dataScriptContent", dataScriptContent);
		ret.put("dataLoadContent", dataLoadContent);
		return ret;
	}
	
	
	/**
	 * 变量类型数据源的解析
	 */
	public static JSONObject parseDataVariableEl(Element variableEl,JSONObject params,Map<String, Object> root,Integer yongHuDM) throws Exception{
		String dataScriptContent = "";
		String dataLoadContent = "";
		

		String name= variableEl.attributeValue("name"); 
		String label= variableEl.attributeValue("label"); 
		String xiTongDH= variableEl.attributeValue("xiTongDH"); 
		String autoLoad= variableEl.attributeValue("autoLoad"); 
		if(autoLoad == null){
			autoLoad = "true";
		}
		String showThousand= variableEl.attributeValue("showThousand");
		
		String renderto = variableEl.attributeValue("renderto");
		String renderTemplate = variableEl.attributeValue("renderTemplate");
		if(renderTemplate == null){
			renderTemplate = "{{"+name+"}}";
		}
		
		//toDoDataset events
		JSONObject listenersObj = new JSONObject();
		listenersObj.put("onLoad",variableEl.attributeValue("onLoad"));
		
		if("sqlVariable".equals(variableEl.attributeValue("component"))){
			String sqlString = variableEl.attributeValue("sql");
			if(sqlString.indexOf("<#")>=0 || sqlString.indexOf("${")>=0 ){
				if(params!=null){
					sqlString = "<#assign _paramsString>"+params.toString()+"</#assign><#assign params = _paramsString?eval />"+sqlString;
				}
				sqlString = PageAnalyseUtils.processTemplate(sqlString, root,null);
			}
			sqlString = sqlString.replaceAll("\\\\n", " ");
			
			//将数据转换为json格式 添加到页面中
			dataScriptContent += "\n//SQL 数据变量（"+label+":"+name+"）\n";
			dataScriptContent += "var _dataVariable_"+name+" = LUI.Datasource.sqlDataVariable.createNew({\n" +
					"name:'" +name+"',\n"+
					"label:'" +label+"',\n"+
					"xiTongDH:'" +xiTongDH+"',\n"+
					"showThousand:'" +showThousand+"',\n"+
					"renderto:'" +renderto+"',\n"+
					"renderTemplate:'" +renderTemplate+"',\n"+
					"sql:\"" +sqlString+"\",\n"+
					"autoLoad:" +autoLoad+",\n"+
					"listenerDefs:" +listenersObj.toString()+",\n"+
			"});\n";
			if("true".equals(autoLoad)){
				Object dataVariable_value = HibernateSessionFactory.getSession().createSQLQuery(sqlString).uniqueResult();
				if (dataVariable_value != null && dataVariable_value instanceof Clob) {
                	Clob clob = (Clob)dataVariable_value;
                	dataVariable_value = clob.getSubString(1, (int) clob.length());
				}
				if(dataVariable_value != null && dataVariable_value instanceof String){
					String oldString = (String)dataVariable_value;
					oldString = oldString.replaceAll("\\\\", "\\\\\\\\");
					oldString = oldString.replaceAll("\r", "");
					oldString = oldString.replaceAll("\n", "");
					oldString = oldString.replaceAll("'", "\\\\'");
					
                	dataVariable_value = oldString.replaceAll("\"", "\\\\\"");
				}
				root.put(name, dataVariable_value);
				
				dataScriptContent += "//自动加载的数据变量值\n";
				if(dataVariable_value==null){
					dataScriptContent += "var "+name+" = null;\n";
				}else{
					dataScriptContent += "var "+name+" = '"+dataVariable_value+"';\n";
				}
				dataLoadContent += "_dataVariable_"+name+".loadData("+name+");\n\n";
			}
			//注册
			dataScriptContent += "_page_widget.register(_dataVariable_"+name+")\n";
		}else if("stlVariable".equals(variableEl.attributeValue("component"))){
			//注册
			dataScriptContent += "_page_widget.register(_dataVariable_"+name+")\n";
		}else if("gnVariable".equals(variableEl.attributeValue("component"))){
			//注册
			dataScriptContent += "_page_widget.register(_dataVariable_"+name+")\n";
		}

		
		JSONObject ret = new JSONObject();
		ret.put("dataScriptContent", dataScriptContent);
		ret.put("dataLoadContent", dataLoadContent);
		return ret;
	}

}
