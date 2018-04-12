package com.guohuai.component.util;

import java.math.BigDecimal;

/**   
 * @Description: 计算工具 
 * @author ZJ   
 * @date 2018年1月19日 下午4:58:14 
 * @version V1.0   
 */
public class CalculationUtil {
	/**
	 * BigDecimal的加法运算。
	 * @param b1 被加数
	 * @param b2 加数
	 * @return 两个参数的和
	 */
	public static BigDecimal add(BigDecimal b1, BigDecimal b2) {
		return b1.add(b2);
	}

	/**
	 * BigDecimal的减法运算。
	 * @param b1 被减数
	 * @param b2 减数
	 * @return 两个参数的差
	 */
	public static BigDecimal sub(BigDecimal b1, BigDecimal b2) {
		return b1.subtract(b2);
	}

	/**
	 * 判断是否超额
	 * @param balance1
	 * @param balance2
	 * @return
	 */
	public static boolean isExcess(BigDecimal balance1, BigDecimal balance2) {
		boolean isExcess = false;
		if (balance1.compareTo(balance2) != -1) {// -1 小于 0 等于 1 大于
			isExcess = true;
		}
		return isExcess;
	}
}