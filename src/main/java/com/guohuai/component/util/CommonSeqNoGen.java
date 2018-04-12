package com.guohuai.component.util;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonSeqNoGen {
	
	private final static SimpleDateFormat sf = new SimpleDateFormat("yyMMddHHmmss");
	private static volatile BigDecimal postFix = new BigDecimal(0); // 后缀自增值
	private final static BigDecimal bentchMark = new BigDecimal(9999); // 最大9999 可以并发9999
		
	final public synchronized static String generate(String prefix)  {
		
		String dateString = sf.format(new Date());
		
		postFix = postFix.add(new BigDecimal(1));
		
		String strPostFix = "";
		
		if(postFix.compareTo(bentchMark) < 0) { // 小于9999
			strPostFix += postFix.toString();
			while(strPostFix.length() < 4) {
				strPostFix = "0" + strPostFix;
			}
		} else if(postFix.compareTo(bentchMark) == 0) { // =9999 - 返回9999
			strPostFix = "9999";
			postFix = new BigDecimal(0);
		} else {
			// 不会出现
		}

		return  prefix + dateString + strPostFix;
	}
		
	public static void main(String[] args) {
//		
//		for(){
//			
//		}
		String s = CommonSeqNoGen.generate("108");
		System.out.println(s);
	}
	
}
