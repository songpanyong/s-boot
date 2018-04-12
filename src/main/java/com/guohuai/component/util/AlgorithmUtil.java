package com.guohuai.component.util;

import java.math.BigDecimal;

public class AlgorithmUtil {

	/**
	 * 去零
	 * @param number
	 * @return
	 */
	public static String getPrettyNumber(String number) {  
	    return BigDecimal.valueOf(Double.parseDouble(number))  
	            .stripTrailingZeros().toPlainString();  
	}

}
