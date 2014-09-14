package com.poweruniverse.nim.designer.servlet;

import java.io.IOException;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.ModelData;
import com.poweruniverse.nim.system.service.DesignerService;
import com.poweruniverse.nim.system.service.PageService;
import com.poweruniverse.nim.system.utils.BaseJavaDatasource;
import com.poweruniverse.oim.client.service.ExecResult;
import com.poweruniverse.oim.client.service.MethodResult;
import com.poweruniverse.oim.server.entity.system.CaoZuoMB;
import com.poweruniverse.oim.server.entity.system.GongNeng;
import com.poweruniverse.oim.server.entity.system.GongNengCZ;
import com.poweruniverse.oim.server.entity.system.ShiTiLei;
import com.poweruniverse.oim.server.entity.system.YongHu;
import com.poweruniverse.oim.server.entity.system.ZiDuan;
import com.poweruniverse.oim.server.service.AuthUtils;
import com.poweruniverse.oim.server.service.DataUtils;
import com.poweruniverse.oim.server.service.ServiceUtils;
import com.poweruniverse.oim.server.util.Encrypt;
import com.poweruniverse.oim.server.util.Environment;
import com.poweruniverse.oim.server.util.HibernateSessionFactory;

/**
 * 用于过滤客户端通过servlet方式对webservice的访问请求
 * 将请求转发到适当的服务类中
 * @author Administrator
 *
 */
public class ServiceRequestDispatcher extends HttpServlet{
	private static final long serialVersionUID = 1L;
	private static String contextPath = null;
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Message ret = dispatch(req, resp);
		if(ret!=null){
			resp.setCharacterEncoding("utf-8");         
			resp.setContentType("text/html; charset=utf-8"); 
			resp.getWriter().write(ret.toString());
		}
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Message ret = dispatch(req, resp);
		if(ret!=null){
			resp.setCharacterEncoding("utf-8");         
			resp.setContentType("text/html; charset=utf-8"); 
			resp.getWriter().write(ret.toString());
		}
		resp.flushBuffer();
	}
	
	/**
	 * 用户向servlet发出的请求 转发到正确的webservice服务
	 * @param req
	 * @param resp
	 * @return
	 */
	private Message dispatch(HttpServletRequest req, HttpServletResponse resp){
		if(contextPath==null){
			contextPath = getServletContext().getRealPath("/");
		}
		String appName = req.getParameter("application");
		String wsName = req.getParameter("service");
		String methodName = req.getParameter("method");
		Message ret = null;
		try {
			JSONObject paramterJsonObj = null;
			String parameters = req.getParameter("parameters");
			if(parameters!=null){
				paramterJsonObj = JSONObject.fromObject(parameters);
			}
			//
			Integer yongHuDM = null;
			//当前登录用户的信息
			Environment env = (Environment)req.getSession().getAttribute(Environment.ENV);
			if(env!=null && env.getUser() !=null){
				yongHuDM = env.getUser().getYongHuDM();
			}
			//
			if(wsName.equals("designer")){
				if(methodName.equals("readCmpDef")){
					//读取设计器基础定义
					ret = DesignerService.readCmpDef(contextPath);
				}else if(methodName.equals("readFile")){
					//读取页面html,css,js文件
					ret = DesignerService.readFile(contextPath, paramterJsonObj.getString("pageUrl"));
					resp.setCharacterEncoding("utf-8");         
					resp.setContentType("text/html; charset=utf-8"); 
					resp.getWriter().write((String)ret.get("content"));
					ret = null;
				}else if(methodName.equals("readPageDef")){
					//读取当前页面定义
					ret = DesignerService.readPageDef(contextPath,paramterJsonObj.getString("pageUrl"));
				}else if(methodName.equals("saveFile")){
					//读取当前页面定义
					ret = DesignerService.saveFile(contextPath,paramterJsonObj.getString("pageUrl"),paramterJsonObj.getString("content"));
				}else if(methodName.equals("savePageDef")){
					//保存当前页面定义
					ret = DesignerService.savePageDef(contextPath,paramterJsonObj.getString("pageUrl"),paramterJsonObj.getJSONObject("data"));
				}else if(methodName.equals("loadSTL")){
					//保存当前页面定义
					String propertyString = null;
					if(paramterJsonObj.containsKey("property")){
						propertyString = paramterJsonObj.getString("property");
					}
					String gongNengDH = null;
					if(paramterJsonObj.containsKey("gongNengDH")){
						gongNengDH = paramterJsonObj.getString("gongNengDH");
					}
					String shiTiLeiDH = null;
					if(paramterJsonObj.containsKey("shiTiLeiDH")){
						shiTiLeiDH = paramterJsonObj.getString("shiTiLeiDH");
					}
					
					ret = DesignerService.loadSTL(paramterJsonObj.getString("xiTongDH"),gongNengDH,shiTiLeiDH,propertyString,paramterJsonObj.getString("fields"));
				}else if(methodName.equals("loadSQLMeta")){
					//从sql取得列名称以及列类型
					String xiTongDH = paramterJsonObj.getString("xiTongDH");
					String sql = paramterJsonObj.getString("sql");
					
					JSONArray columnsDef = com.poweruniverse.nim.system.utils.DataUtils.loadSQLMeta(sql);
					ret = new Message("data",columnsDef);
				}else{
					ret = new Message("未定义处理方式的方法（"+wsName+"."+methodName+"）！");
				}
			}else if (wsName.equals("auth")){
				if(methodName.equals("hasGNCZAuth")){
					JSONObject params = paramterJsonObj.getJSONObject("params");
					String xiTongDH = params.getString("xiTongDH");
					String gongNengDH = params.getString("gongNengDH");
					String caoZuoDH = params.getString("caoZuoDH");
					
					boolean authRsult = false;
					if(!env.isSuperUser()){
						authRsult = ServiceUtils.checkGNCZAuth(gongNengDH,caoZuoDH,yongHuDM==null?null:env.getUser());
					}else{
						authRsult = true;
					}
					ret = new Message("data",authRsult);
				}else if(methodName.equals("hasAuth")){
					JSONObject params = paramterJsonObj.getJSONObject("params");
					String xiTongDH = params.getString("xiTongDH");
					String gongNengDH = params.getString("gongNengDH");
					String caoZuoDH = params.getString("caoZuoDH");
					Integer id = null;
					if(params.has("id")){
						Object idObj = params.get("id");
						if(!(idObj instanceof JSONNull)){
							id = params.getInt("id");
						}
					}
					
					boolean authRsult = false;
					if(!env.isSuperUser()){
						authRsult = AuthUtils.checkAuth(gongNengDH,caoZuoDH,id,yongHuDM==null?null:env.getUser());
					}else{
						authRsult = true;
					}
					ret = new Message("data",authRsult);
				}else if(methodName.equals("getCaoZuoMB")){
					JSONObject params = paramterJsonObj.getJSONObject("params");
					String xiTongDH = params.getString("xiTongDH");
					String gongNengDH = params.getString("gongNengDH");
					String caoZuoDH = params.getString("caoZuoDH");
					Integer id = null;
					if(params.has("id")){
						Object idObj = params.get("id");
						if(!(idObj instanceof JSONNull)){
							id = params.getInt("id");
						}
					}
					
					CaoZuoMB caoZuoMB = ServiceUtils.getCaoZuoMB(gongNengDH, caoZuoDH, yongHuDM, id);
					
					ret = new Message();
					ret.put("caoZuoMBDH", caoZuoMB.getCaoZuoMBDH());
					ret.put("jiChuYM", caoZuoMB.getJiChuYM());
					ret.put("caoZuoMBLJ", caoZuoMB.getCaoZuoMBLJ());
					ret.put("caoZuoMBFtlFileName", caoZuoMB.getCaoZuoMBLJ()+gongNengDH+caoZuoMB.getfirstUpCaoZuoDH()+".ftl");
				}
				
			}else if (wsName.equals("data")){
				if(methodName.equals("login")){
					String userName = paramterJsonObj.getString("userName");
					String userPassword = paramterJsonObj.getString("userPassword");
					boolean saveCookie = paramterJsonObj.getBoolean("saveCookie");
					String validCode = null;
					boolean shiYongYZM = false;
					if(paramterJsonObj.containsKey("validCode")){
						validCode = paramterJsonObj.getString("validCode");
						shiYongYZM = true;
					}
					String xiTongDH = paramterJsonObj.getString("xiTongDH");
					
					ModelData loginInfo = new BaseModelData();
					loginInfo.set("dengluDH", userName);
					loginInfo.set("dengluMM", userPassword);
					loginInfo.set("validCode", validCode);
					loginInfo.set("saveCookie", saveCookie);
					loginInfo.set("checkValidCode", true);
					
					loginInfo.set("shiYongWT", false);
					loginInfo.set("shiYongYZM",shiYongYZM );
					loginInfo.set("xiTongDH",xiTongDH);
					
					MethodResult loginRsult = DataUtils.doLogin(loginInfo, req, resp);
					if(!loginRsult.isSuccess()){
						ret = new Message(loginRsult.getErrorMsg());
					}else{
						ret = new Message();
					}
				}else if(methodName.equals("changePassword")){
					String oldpassword = paramterJsonObj.getString("oldpassword");
					String newpassword1 = paramterJsonObj.getString("newpassword1");
					String newpassword2 = paramterJsonObj.getString("newpassword2");
					
					if(yongHuDM == null ){
						return new Message("未登录或已超时,请重新登录！");
					}else{
						// 检查密码
						if (oldpassword == null || oldpassword.length()==0) {
							return  new Message("请输入原密码！");
						}else if (newpassword1 == null || newpassword1.length()==0) {
							return  new Message("请输入新密码！");
						}else if(!newpassword1.equals(newpassword2)){
							return  new Message("两次输入的新密码不一致！");
						}
						// 从数据库取对应的用户信息
						YongHu user = (YongHu) HibernateSessionFactory.getSession().load(YongHu.class,yongHuDM);
						if (user == null) {
							return  new Message("当前用户不存在！");
						}
						String dengLuDH = user.getDengLuDH();
						// 检查密码 原密码不为空
						// 用来计算密码的种子
						String checkUserCode1 = "0000" + dengLuDH;
						checkUserCode1 = checkUserCode1.substring(
								checkUserCode1.length() - 5, checkUserCode1.length());
						// 加密的密码
						String password1 = Encrypt.encrypt(checkUserCode1, oldpassword);
						// 检查输入的密码(加密前或加密后)与数据库中存储的密码是否一致
						if (password1.equals(user.getDengLuMM()) || oldpassword.equals(user.getDengLuMM())) {
							//新的密码
							String password2 = Encrypt.encrypt(checkUserCode1, newpassword2);
							//修改最后登录时间
							user.setDengLuMM(password2);
							HibernateSessionFactory.getSession().update(user);
							ret = new Message();
						} else {
							ret = new Message("原密码输入错误！");
						}
					}
				}else if(methodName.equals("logout")){
					//检查登录状态 并取得当前用户代码
					if(env!=null && env.getUser()!=null){
						//清除登陆信息
						env.userLogout();
						//清除cookie
				        Cookie cookie = new Cookie("dengLuDH", null);
				        cookie.setPath("/");
				        cookie.setMaxAge(0); // Delete
				        resp.addCookie(cookie);

				        cookie = new Cookie("dengLuMM", null);
				        cookie.setPath("/");
				        cookie.setMaxAge(0); // Delete
				        resp.addCookie(cookie);
						
					}
					//返回
			        ret = new Message();
				}else if(methodName.equals("execute")){
					if(yongHuDM != null){
						JSONObject params = paramterJsonObj.getJSONObject("params");
						String xiTongDH = params.getString("xiTongDH");
						String gongNengDH = params.getString("gongNengDH");
						String caoZuoDH = params.getString("caoZuoDH");
						
						List<Integer> idList = new ArrayList<Integer>();
						
						if(params.containsKey("id")){
							Object idObj = params.get("id");
							if(!(idObj==null || idObj instanceof JSONNull)){
								Integer id = Integer.parseInt(idObj.toString());
								idList.add(id);
							}
						}
						//未登录
						YongHu yh = (YongHu)HibernateSessionFactory.getSession().load(YongHu.class, env.getUser().getYongHuDM());
						ExecResult<ModelData> result = DataUtils.doExec(yh,gongNengDH,caoZuoDH, idList, false, true,req,resp);
						if(result.isSuccess()){
							ret =  new Message();
						}else{
							ret =  new Message(result.getErrorMsg());
						}
					}else{
						ret = new Message("未登陆或已超时");
					}
					
				}else if(methodName.equals("save")){
					String xiTongDH = paramterJsonObj.getString("xiTongDH");
					String gongNengDH = paramterJsonObj.getString("gongNengDH");
					String caoZuoDH = paramterJsonObj.getString("caoZuoDH");
					JSONArray rows = paramterJsonObj.getJSONArray("submitData");
					boolean isCommit = paramterJsonObj.getBoolean("isCommit");
					
					GongNeng gn = GongNeng.getGongNengByDH(gongNengDH);
					String zhuJianLie = gn.getShiTiLei().getZhuJianLie();
					
					List<Integer> idList = new ArrayList<Integer>();
					if(paramterJsonObj.containsKey("id")){
						idList.add(paramterJsonObj.getInt("id"));
					}
					
					if(rows.size() > idList.size()){
						int k = idList.size();
						for(int i=k;i<rows.size();i++){
							Integer id = null;
							JSONObject row = rows.getJSONObject(i);
							if(row.containsKey("id")){
								id = row.getInt("id");
							}else if (row.containsKey(zhuJianLie)){
								id = row.getInt(zhuJianLie);
							}
							idList.add(id);
						}
					}
					
					//数据转换为ModelData格式
					Map<String,Map<String,List<ModelData>>> postDataMap = new HashMap<String,Map<String,List<ModelData>>>();
					Map<String,List<ModelData>> postData = new HashMap<String,List<ModelData>>();
					postData.put("modified", DataUtils.applyJsonArrayToModel(rows));
					postDataMap.put(gongNengDH, postData);
					
					ExecResult<ModelData> saveRet = DataUtils.doSubmit(yongHuDM,gongNengDH,caoZuoDH, idList,caoZuoDH, postDataMap,false,isCommit,isCommit,req,resp);
					ret = new Message();
					if(!saveRet.isSuccess()){
						ret.put("success", false);
						ret.put("errorMsg", saveRet.getErrorMsg());
					}else{
						ret.put("success", true);
						List<ModelData> resRows = saveRet.getData();
						JSONArray resData = DataUtils.applyModelToJsonArray(resRows);
						ret.put("data",resData);
					}
					
				}else if(methodName.equals("getSqlVariable")){
					JSONObject params = paramterJsonObj.getJSONObject("params");
					String xiTongDH = params.getString("xiTongDH");
					String sql = params.getString("sql");
					
					Object qRsult = com.poweruniverse.nim.system.utils.DataUtils.getSqlVariable(sql);
					
	                if (qRsult != null && qRsult instanceof Clob) {
	                	Clob clob = (Clob)qRsult;
	                	qRsult = clob.getSubString(1, (int) clob.length());
	                }
	                if(qRsult != null && qRsult instanceof String){
						String oldString = (String)qRsult;
						oldString = oldString.replaceAll("\\\\", "\\\\");
						oldString = oldString.replaceAll("\r", "");
						oldString = oldString.replaceAll("\n", "");
						oldString = oldString.replaceAll("'", "\\'");
						qRsult = oldString.replaceAll("\"", "\\\"");
					}
					//
					ret = new Message();
					ret.put("data", qRsult);
				}else if(methodName.equals("listTodoData")){
					JSONObject params = paramterJsonObj.getJSONObject("params");
					String xiTongDH = "system";
					String shiTiLeiDH = "SYS_LiuChengJS";
					int start = 0;
					if(params.containsKey("start")){
						start = params.getInt("start");
					}
					int limit = 0;
					if(params.containsKey("limit")){
						limit = params.getInt("limit");
					}
					String fieldString = null;
					if(params.containsKey("fields")){
						fieldString = params.getString("fields");
					}
					String workflowsString = null;
					if(params.containsKey("workflows")){
						workflowsString = params.getString("workflows");
					}
					ShiTiLei dataStl = ShiTiLei.getShiTiLeiByDH(shiTiLeiDH);
					if(dataStl==null){
						return new Message("实体类("+shiTiLeiDH+")不存在！");
					}
					
					JSONObject qRsult = com.poweruniverse.nim.system.utils.DataUtils.getTodoListByHibernate(
							shiTiLeiDH,start,limit,JSONArray.fromObject(fieldString),JSONArray.fromObject(workflowsString),yongHuDM);
					JSONArray jsonData = qRsult.getJSONArray("data");
					int totalCount = qRsult.getInt("totalCount");
					
					//将数据转换为json格式 添加到页面中
					//
					ret = new Message();
					ret.put("totalCount", totalCount);
					ret.put("start", start);
					ret.put("limit", limit);
					ret.put("rows", jsonData);
					
					JSONObject meta = new JSONObject();
					meta.put("zhuJianLie", dataStl.getZhuJianLie());
					meta.put("xianShiLie", dataStl.getXianShiLie());
					meta.put("paiXuLie", dataStl.getPaiXuLie());
					ret.put("meta", meta);
				}else if(methodName.equals("listSqlData")){
					JSONObject params = paramterJsonObj.getJSONObject("params");
					String xiTongDH = params.getString("xiTongDH");
					String sql = params.getString("sql");
					int start = 0;
					if(params.containsKey("start")){
						start = params.getInt("start");
					}
					int limit = 0;
					if(params.containsKey("limit")){
						limit = params.getInt("limit");
					}
					String fieldString = null;
					if(params.containsKey("fields")){
						fieldString = params.getString("fields");
					}
					
					JSONArray fieldJsonArray = null;
					if(fieldString==null ||  fieldString.length() < 4){
						fieldJsonArray = JSONArray.fromObject("[]");
					}else{
						fieldJsonArray = JSONArray.fromObject(fieldString);
					}
					JSONObject qRsult = com.poweruniverse.nim.system.utils.DataUtils.getSqlData(sql, start,limit,fieldJsonArray);
					//
					ret = new Message();
					ret.put("totalCount", qRsult.get("totalCount"));
					ret.put("start", start);
					ret.put("limit", limit);
					ret.put("rows", qRsult.get("rows"));
				
				}else if(methodName.equals("listJavaData")){
					String className = paramterJsonObj.getString("className");
					
					JSONObject pa = new JSONObject();
					if(paramterJsonObj.containsKey("parameters")){
						JSONArray ps = paramterJsonObj.getJSONArray("parameters");
						for(int i=0;i<ps.size();i++){
							JSONObject p = ps.getJSONObject(i);
							pa.put(p.get("name"), p.get("value"));
						}
					}
					
					int start = 0;
					if(paramterJsonObj.containsKey("start")){
						start = paramterJsonObj.getInt("start");
					}
					
					int limit = 0;
					if(paramterJsonObj.containsKey("limit")){
						limit = paramterJsonObj.getInt("limit");
					}
//					
//					String fieldString = null;
//					if(params.containsKey("fields")){
//						fieldString = params.getString("fields");
//					}
//					JSONArray fieldJsonArray = null;
//					if(fieldString==null ||  fieldString.length() < 4){
//						fieldJsonArray = JSONArray.fromObject("[]");
//					}else{
//						fieldJsonArray = JSONArray.fromObject(fieldString);
//					}
					
					//取得数据对象(用老的程序方法)
					JSONObject jsonData = null;
					try {
						BaseJavaDatasource javaDS = (BaseJavaDatasource)Class.forName(className).newInstance();
						jsonData = javaDS.getData(pa,start,limit);
					} catch (Exception e) {
						jsonData = new JSONObject();
						jsonData.put("totalCount", 0);
						jsonData.put("start", start);
						jsonData.put("limit", limit);
						jsonData.put("rows", new JSONArray());
						
						e.printStackTrace();
					}
					//
					ret = new Message();
					ret.put("totalCount", jsonData.getInt("totalCount"));
					ret.put("start", start);
					ret.put("limit", limit);
					ret.put("rows", jsonData.getJSONArray("rows"));
				
				}else if(methodName.equals("listStlData")){
					JSONObject params = paramterJsonObj.getJSONObject("params");
					String xiTongDH = params.getString("xiTongDH");
					String shiTiLeiDH = params.getString("shiTiLeiDH");
					int start = 0;
					if(params.containsKey("start")){
						start = params.getInt("start");
					}
					int limit = 0;
					if(params.containsKey("limit")){
						limit = params.getInt("limit");
					}
					String fieldString = null;
					if(params.containsKey("fields")){
						fieldString = params.getString("fields");
					}
					String filterString = null;
					if(params.containsKey("filters")){
						filterString = params.getString("filters");
					}
					String sortString = null;
					if(params.containsKey("sorts")){
						sortString = params.getString("sorts");
					};
					
					ShiTiLei dataStl = ShiTiLei.getShiTiLeiByDH(shiTiLeiDH);
					if(dataStl==null){
						return new Message("实体类("+shiTiLeiDH+")不存在！");
					}
					
					ModelData qRsult = com.poweruniverse.nim.system.utils.DataUtils.getObjListByHibernate(shiTiLeiDH,start,limit,filterString,sortString,yongHuDM);
					List<?> objs = qRsult.get("data");
					Integer totalCount = qRsult.get("totalCount");
					//创建返回对象
					JSONObject meta = new JSONObject();
					meta.put("zhuJianLie", dataStl.getZhuJianLie());
					meta.put("xianShiLie", dataStl.getXianShiLie());
					meta.put("paiXuLie", dataStl.getPaiXuLie());
					
					JSONArray fieldJsonArray = null;
					if(fieldString==null ||  fieldString.length() < 4){
						fieldJsonArray = JSONArray.fromObject("[{" +
								"name:'"+dataStl.getXianShiLie()+"'" +
							"},{" +
								"name:'"+dataStl.getPaiXuLie()+"'" +
							"}]");
					}else if( fieldString.equals("['...']") ||  fieldString.equals("[\"...\"]")){
						//全部的字段  当前级别以及对象，集合字段的显示列、排序列
						fieldJsonArray = new JSONArray();
						for(ZiDuan zd:dataStl.getZds()){
							JSONObject zdObj = new JSONObject();
							zdObj.put("name", zd.getZiDuanDH());
							
							if(zd.getZiDuanLX().getZiDuanLXDH().equals("set") || zd.getZiDuanLX().getZiDuanLXDH().equals("object") ){
								JSONArray subZdArray = new JSONArray();
								
								JSONObject zjlObj = new JSONObject();
								zjlObj.put("name", zd.getGuanLianSTL().getZhuJianLie());
								subZdArray.add(zjlObj);
								
								JSONObject xslObj = new JSONObject();
								xslObj.put("name", zd.getGuanLianSTL().getXianShiLie());
								subZdArray.add(xslObj);
								
								JSONObject pxlObj = new JSONObject();
								pxlObj.put("name", zd.getGuanLianSTL().getPaiXuLie());
								subZdArray.add(pxlObj);
								
								zdObj.put("fields", subZdArray);
								
								JSONObject subMeta = new JSONObject();
								subMeta.put("zhuJianLie", zd.getGuanLianSTL().getZhuJianLie());
								subMeta.put("xianShiLie", zd.getGuanLianSTL().getXianShiLie());
								subMeta.put("paiXuLie", zd.getGuanLianSTL().getPaiXuLie());
								meta.put(zd.getZiDuanDH(), subMeta);
							}
							
							fieldJsonArray.add(zdObj);
						}
					}else{
						fieldJsonArray = JSONArray.fromObject(fieldString);
					}
					
					//将数据转换为json格式 添加到页面中
					JSONArray jsonData = com.poweruniverse.nim.system.utils.DataUtils.applyList2JSONArray(dataStl,objs,fieldJsonArray);
					//
					ret = new Message();
					ret.put("totalCount", totalCount);
					ret.put("start", start);
					ret.put("limit", limit);
					ret.put("rows", jsonData);
					ret.put("meta", meta);
				
				}else if(methodName.equals("loadGnData")){
					JSONObject params = paramterJsonObj.getJSONObject("params");
					String xiTongDH = params.getString("xiTongDH");
					String gongNengDH = params.getString("gongNengDH");
					String caoZuoDH = params.getString("caoZuoDH");
					
					Integer id = null;
					if(params.containsKey("id")){
						Object idObj = params.get("id");
						if(!(idObj==null || idObj instanceof JSONNull)){
							id = Integer.parseInt(idObj.toString());
						}
					}
					String fieldString = null;
					if(params.containsKey("fields")){
						fieldString = params.getString("fields");
					}
					
					//取得数据
					GongNengCZ gncz = GongNeng.getGongNengByDH(gongNengDH).getCaoZuoByDH(caoZuoDH);
					if(gncz==null){
						throw new Exception("功能操作:"+gongNengDH+"."+caoZuoDH+"不存在！");
					}
					
					if(gncz.getDuiXiangXG() && id==null){
						throw new Exception("功能操作:"+gongNengDH+"."+caoZuoDH+"为对象相关的操作，必须提供id参数！");
					}
					
					//用这个openGNDH、openCZDH确定和检查权限
					if(!AuthUtils.checkAuth(gongNengDH,caoZuoDH, id, yongHuDM==null?null:(YongHu)HibernateSessionFactory.getSession().load(YongHu.class, yongHuDM))){
						throw new Exception("记录("+gongNengDH+"."+caoZuoDH+"."+id+")不存在或用户没有权限！");
					}
					Object obj = com.poweruniverse.oim.server.service.DataUtils.getData(gncz,id,yongHuDM,false);
					
					//创建返回对象
					JSONObject meta = new JSONObject();
					meta.put("zhuJianLie", gncz.getGongNeng().getShiTiLei().getZhuJianLie());
					meta.put("xianShiLie", gncz.getGongNeng().getShiTiLei().getXianShiLie());
					meta.put("paiXuLie", gncz.getGongNeng().getShiTiLei().getPaiXuLie());
					
					JSONArray fieldJsonArray = null;
					if(fieldString==null ||  fieldString.length() < 4){
						fieldJsonArray = JSONArray.fromObject("[{" +
								"name:'"+gncz.getGongNeng().getShiTiLei().getXianShiLie()+"'" +
							"},{" +
								"name:'"+gncz.getGongNeng().getShiTiLei().getPaiXuLie()+"'" +
							"}]");
					}else if( fieldString.equals("['...']") ||  fieldString.equals("[\"...\"]")){
						//全部的字段  当前级别以及对象，集合字段的显示列、排序列
						fieldJsonArray = new JSONArray();
						for(ZiDuan zd:gncz.getGongNeng().getShiTiLei().getZds()){
							JSONObject zdObj = new JSONObject();
							zdObj.put("name", zd.getZiDuanDH());
							
							if(zd.getZiDuanLX().getZiDuanLXDH().equals("set") || zd.getZiDuanLX().getZiDuanLXDH().equals("object") ){
								JSONArray subZdArray = new JSONArray();
								
								JSONObject zjlObj = new JSONObject();
								zjlObj.put("name", zd.getGuanLianSTL().getZhuJianLie());
								subZdArray.add(zjlObj);
								
								JSONObject xslObj = new JSONObject();
								xslObj.put("name", zd.getGuanLianSTL().getXianShiLie());
								subZdArray.add(xslObj);
								
								JSONObject pxlObj = new JSONObject();
								pxlObj.put("name", zd.getGuanLianSTL().getPaiXuLie());
								subZdArray.add(pxlObj);
								
								zdObj.put("fields", subZdArray);
								
								JSONObject subMeta = new JSONObject();
								subMeta.put("zhuJianLie", zd.getGuanLianSTL().getZhuJianLie());
								subMeta.put("xianShiLie", zd.getGuanLianSTL().getXianShiLie());
								subMeta.put("paiXuLie", zd.getGuanLianSTL().getPaiXuLie());
								meta.put(zd.getZiDuanDH(), subMeta);
							}
							
							fieldJsonArray.add(zdObj);
						}
					}else{
						fieldJsonArray = JSONArray.fromObject(fieldString);
					}
					
					//将数据转换为json格式 添加到页面中
					JSONObject jsonData = com.poweruniverse.nim.system.utils.DataUtils.applyEntity2JSONObject(gncz.getGongNeng().getShiTiLei(),obj,fieldJsonArray);
					JSONArray jsonArray= new JSONArray();
					jsonArray.add(jsonData);
					//
					ret = new Message();
					ret.put("totalCount", 1);
					ret.put("start", 0);
					ret.put("limit", 0);
					ret.put("rows", jsonArray);
					ret.put("meta", meta);
				}else if(methodName.equals("listGnData") || methodName.equals("list")){
					JSONObject params = paramterJsonObj.getJSONObject("params");
					String xiTongDH = params.getString("xiTongDH");
					String gongNengDH = params.getString("gongNengDH");
					String caoZuoDH = params.getString("caoZuoDH");
					int start = 0;
					if(params.containsKey("start")){
						start = params.getInt("start");
					}
					int limit = 0;
					if(params.containsKey("limit")){
						limit = params.getInt("limit");
					}
					String fieldString = null;
					if(params.containsKey("fields")){
						fieldString = params.getString("fields");
					}
					String filterString = null;
					if(params.containsKey("filters")){
						filterString = params.getString("filters");
					}
					String sortString = null;
					if(params.containsKey("sorts")){
						sortString = params.getString("sorts");
					};
					
					
					GongNengCZ gncz = GongNeng.getGongNengByDH(gongNengDH).getCaoZuoByDH(caoZuoDH);
					
					ModelData qRsult = com.poweruniverse.nim.system.utils.DataUtils.getObjListByHibernate(gongNengDH,caoZuoDH,start,limit,filterString,sortString,yongHuDM);
					List<?> objs = qRsult.get("data");
					Integer totalCount = qRsult.get("totalCount");
					//创建返回对象
					JSONObject meta = new JSONObject();
					meta.put("zhuJianLie", gncz.getGongNeng().getShiTiLei().getZhuJianLie());
					meta.put("xianShiLie", gncz.getGongNeng().getShiTiLei().getXianShiLie());
					meta.put("paiXuLie", gncz.getGongNeng().getShiTiLei().getPaiXuLie());
					
					JSONArray fieldJsonArray = null;
					if(fieldString==null ||  fieldString.length() < 4){
						fieldJsonArray = JSONArray.fromObject("[{" +
								"name:'"+gncz.getGongNeng().getShiTiLei().getXianShiLie()+"'" +
							"},{" +
								"name:'"+gncz.getGongNeng().getShiTiLei().getPaiXuLie()+"'" +
							"}]");
					}else if( fieldString.equals("['...']") ||  fieldString.equals("[\"...\"]")){
						//全部的字段  当前级别以及对象，集合字段的显示列、排序列
						fieldJsonArray = new JSONArray();
						for(ZiDuan zd:gncz.getGongNeng().getShiTiLei().getZds()){
							JSONObject zdObj = new JSONObject();
							zdObj.put("name", zd.getZiDuanDH());
							
							if(zd.getZiDuanLX().getZiDuanLXDH().equals("set") || zd.getZiDuanLX().getZiDuanLXDH().equals("object") ){
								JSONArray subZdArray = new JSONArray();
								
								JSONObject zjlObj = new JSONObject();
								zjlObj.put("name", zd.getGuanLianSTL().getZhuJianLie());
								subZdArray.add(zjlObj);
								
								JSONObject xslObj = new JSONObject();
								xslObj.put("name", zd.getGuanLianSTL().getXianShiLie());
								subZdArray.add(xslObj);
								
								JSONObject pxlObj = new JSONObject();
								pxlObj.put("name", zd.getGuanLianSTL().getPaiXuLie());
								subZdArray.add(pxlObj);
								
								zdObj.put("fields", subZdArray);
								
								JSONObject subMeta = new JSONObject();
								subMeta.put("zhuJianLie", zd.getGuanLianSTL().getZhuJianLie());
								subMeta.put("xianShiLie", zd.getGuanLianSTL().getXianShiLie());
								subMeta.put("paiXuLie", zd.getGuanLianSTL().getPaiXuLie());
								meta.put(zd.getZiDuanDH(), subMeta);
							}
							
							fieldJsonArray.add(zdObj);
						}
					}else{
						fieldJsonArray = JSONArray.fromObject(fieldString);
					}
					
					//将数据转换为json格式 添加到页面中
					JSONArray jsonData = com.poweruniverse.nim.system.utils.DataUtils.applyList2JSONArray(gncz.getGongNeng().getShiTiLei(),objs,fieldJsonArray);
					//
					ret = new Message();
					ret.put("totalCount", totalCount);
					ret.put("start", start);
					ret.put("limit", limit);
					ret.put("rows", jsonData);
					ret.put("meta", meta);
				}else{
					ret = new Message("未定义处理方式的方法（"+wsName+"."+methodName+"）！");
				}
				
			}else if (wsName.equals("page")){
				if(methodName.equals("css")){
					//读取页面html,css,js文件
					ret = PageService.css(contextPath, paramterJsonObj.getString("pageUrl"));
					resp.setCharacterEncoding("utf-8");         
					resp.setContentType("text/html; charset=utf-8"); 
					resp.getWriter().write((String)ret.get("content"));
					ret = null;
				}else if(methodName.equals("js")){
					//读取页面html,css,js文件
					ret = PageService.js(contextPath, paramterJsonObj.getString("pageUrl"));
					resp.setCharacterEncoding("utf-8");         
					resp.setContentType("text/html; charset=utf-8"); 
					resp.getWriter().write((String)ret.get("content"));
					ret = null;
				}else if(methodName.equals("html")){
					//读取页面html文件
					JSONObject params = null;
					if(paramterJsonObj.has("params")){
						params = paramterJsonObj.getJSONObject("params");
					}
					
					ret = PageService.html(contextPath, paramterJsonObj.getString("pageUrl"), params,yongHuDM);
				}else{
					ret = new Message("未定义处理方式的方法（"+wsName+"."+methodName+"）！");
				}
			}else{
				ret = new Message("未定义处理方式的服务（"+wsName+"）！");
			}
			
//			YongHu yongHu = (YongHu)req.getSession().getAttribute(ApplicationConstants.flag_user_in_session);
//			String ip = getServletClientIp(req);
//			
//			ApplicationInfo currentApp =  ApplicationConstants.AppInfoMap.get(ApplicationConstants.current_APP_name);
//			if(ServiceRouter.hasIPPermission(ip, yongHu.getYongHuDM())){
//				//无论session中 是否有已登录用户信息 均以UserClientInfo形式调用 
//				//是否需要登录 由webservice方法自己判断
//				InvokeEnvelope newEnvelope = new InvokeEnvelope(appName,wsName,methodName,paramterJsonObj,yongHu);
//				ret = ServiceRouter.invokeService(newEnvelope);
//			}else{
//				ret = new Message("当前用户无权限（"+yongHu.getDengLuDH()+":"+ip+"）进行此操作！");
//			}
			
		} catch (Exception e) {
			e.printStackTrace();
			ret = new Message(e.getMessage());
			HibernateSessionFactory.closeSession(false);
		}finally{
			HibernateSessionFactory.closeSession(true);
		}
		return ret;
	}
	
	
	private static String getServletClientIp(HttpServletRequest req) {

	    String ip = req.getHeader("X-Forwarded-For");
	    if(ip != null) {
	        if(ip.indexOf(",") == -1) {
	            return ip;
	        }
	        return ip.split(",")[0];
	    }

	    if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
	        ip = req.getHeader("Proxy-Client-IP");
	    }
	    if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
	        ip = req.getHeader("WL-Proxy-Client-IP");
	    }
	    if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
	        ip = req.getRemoteAddr();
	    }

	    return ip;
	}

}
