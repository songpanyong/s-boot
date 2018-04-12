package com.guohuai.component.util;

/**   
 * @Description: 字符串工具 
 * @author ZJ   
 * @date 2018年1月19日 下午5:38:40 
 * @version V1.0   
 */
public class StringUtil {
	/**
	 * null转为字符串
	 * @param str
	 * @return
	 */
	public static String nullToStr(Object str) {
		if (null == str) {
			return "";
		}
		return str.toString();
	}
}