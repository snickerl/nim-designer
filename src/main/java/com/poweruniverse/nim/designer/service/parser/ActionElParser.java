package com.poweruniverse.nim.designer.service.parser;

import java.util.Map;

import net.sf.json.JSONObject;

import org.dom4j.Element;

public class ActionElParser {

	/**
	 * 集合类型数据源的解析
	 */
	public static String parseActionEl(Element actionEl,JSONObject params,Map<String, Object> root,Integer yongHuDM) throws Exception{
		String dataScriptContent = "";

		String name = actionEl.attributeValue("name");
		String label = actionEl.attributeValue("label");
		if("link".equals(actionEl.attributeValue("component"))){
			//检查 renderto参数
			String renderto = actionEl.attributeValue("renderto");
			String href = actionEl.attributeValue("href");
			String target = actionEl.attributeValue("target");
			if(renderto!=null && renderto.length()>0 ){
				dataScriptContent+="\n//action "+label+":"+name+" 的处理(链接)\n";
				dataScriptContent+="if($(\""+renderto+"\").attr(\"href\")){\n" +
						"		$(\""+renderto+"\").attr('href',\"javascript:void(0);\")\n" +
						"	};\n" +
						"	$(\""+renderto+"\").removeAttr(\"onclick\")//删除可能存在的onclick\n" +
						"	.click(function(){\n" +
						("_blank".equals(target)?("		new ForceWindow().open(\""+href+"\");\n"):"")+
						("_parent".equals(target)?("		window.parent.location.href=\""+href+"\";\n"):"")+
						("_top".equals(target)?("		window.top.location=\""+href+"\";\n"):"")+
						("_self".equals(target)?("		window.location.href=\""+href+"\";\n"):"")+
						"	});\n\n" ;
			}
		}else if("operation".equals(actionEl.attributeValue("component"))){
			//检查 renderto参数
			String renderto = actionEl.attributeValue("renderto");
			String xiTongDH= actionEl.attributeValue("xiTongDH"); 
			String gongNengDH= actionEl.attributeValue("gongNengDH"); 
			String caoZuoDH= actionEl.attributeValue("caoZuoDH");
			
			if(renderto!=null && renderto.length()>0 ){
				dataScriptContent+="\n//action "+label+":"+name+" 的处理(操作)\n";
				dataScriptContent+="if($(\""+renderto+"\").attr(\"href\")){\n" +
						"		$(\""+renderto+"\").attr('href',\"javascript:void(0);\")\n" +
						"	};\n" +
						"	$(\""+renderto+"\").removeAttr(\"onclick\")//删除可能存在的onclick\n" +
						"	.click(function(){\n doOpenForm();" +
						"	});\n\n" ;
			}
		}else if("jsFunction".equals(actionEl.attributeValue("component"))){
			//检查 renderto参数
			String renderto = actionEl.attributeValue("renderto");
			String jsFuncName= actionEl.attributeValue("jsFuncName"); 
			
			if(renderto!=null && renderto.length()>0 ){
				dataScriptContent+="\n//action "+label+":"+name+"的处理(函数)\n";
				dataScriptContent+="$(\""+renderto+"\").removeAttr(\"onclick\")//删除可能存在的onclick\n" +
						"	.click("+jsFuncName+");\n\n" ;
			}
		}else if("showHide".equals(actionEl.attributeValue("component"))){
			String renderto = actionEl.attributeValue("renderto");
			String targetEl = actionEl.attributeValue("targetEl");
			String showClass = actionEl.attributeValue("showClass");
			String hiddenClass = actionEl.attributeValue("hiddenClass");
			if(renderto!=null && renderto.length()>0 ){
				dataScriptContent+="\n//action "+label+":"+name+"的处理(显示/隐藏元素)\n";
				dataScriptContent+="if($(\""+renderto+"\").attr(\"href\")){\n" +
						"		$(\""+renderto+"\").attr('href',\"javascript:void(0);\")\n" +
						"	};\n" +
						"	$(\""+renderto+"\").removeAttr(\"onclick\")//删除可能存在的onclick\n" +
						"	.click(function(){\n" +
						"		if($(\""+renderto+"\").hasClass('"+hiddenClass+"')){\n" +
						"			//显示\n" +
						"			$( \""+targetEl+"\" ).slideDown();\n" +
						"		}else{\n" +
						"			//隐藏\n" +
						"			$( \""+targetEl+"\" ).slideUp();\n" +
						"		}\n" +
						"		$(\""+renderto+"\").toggleClass('"+hiddenClass+" "+showClass+" ');\n" +
						"	});\n\n" ;
			}
			;
		}else if("topFixed".equals(actionEl.attributeValue("component"))){
			String renderto = actionEl.attributeValue("renderto");
			if(renderto!=null && renderto.length()>0 ){
				dataScriptContent+="\n//action "+label+":"+name+"的处理(滚动到顶部固定)\n";
				dataScriptContent+="var "+name+"_topPos = $(\""+renderto+"\").offset().top;\n" +
						"_nim_page_container.scroll(function() {\n" +
						"	var p = _nim_page_container.scrollTop();\n" +
						"	$(\""+renderto+"\").css(\"position\",((p) > "+name+"_topPos) ? \"fixed\" : \"static\");\n" +
						"	$(\""+renderto+"\").css(\"top\",((p) > "+name+"_topPos) ? \"0px\" : \"\");\n" +
						"});\n" ;
			}
			;
		}else if("tips".equals(actionEl.attributeValue("component"))){
				
		}
	
		
		return dataScriptContent;
	}
	

}
