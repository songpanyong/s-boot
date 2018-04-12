//package com.guohuai.component.util;
//
//import java.lang.reflect.InvocationTargetException;
//
//import org.apache.commons.beanutils.BeanUtils;
//
//import com.guohuai.component.exception.SETException;
//
//
//public class BeanUtil {
//	/**
//	 * copy bean's properties
//	 * @param dest
//	 * @param orig
//	 */
//	public static void copy(Object dest,Object orig){
//		try {
//			BeanUtils.copyProperties(dest, orig);
//		} catch (IllegalAccessException | InvocationTargetException e) {
//			throw new SETException(e.getMessage());
//		}
//	}
//}
//
