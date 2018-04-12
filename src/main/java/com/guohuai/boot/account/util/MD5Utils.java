package com.guohuai.boot.account.util;

import java.security.MessageDigest;
import com.thoughtworks.xstream.core.util.Base64Encoder;

public class MD5Utils {
	
	private static Base64Encoder base64en = new Base64Encoder();
	
	 /**
	  * 利用MD5进行加密(将MD5和BASE64结合起来使用)
     * @param encryptStr  待加密的字符串（utf-8编码）
     * @return  加密后的字符串
     * @throws Exception  
     */
    public static String encryptByMd5(String encryptStr) throws Exception {
    	
    	byte[] input = encryptStr.getBytes("utf-8");
    	
    	MessageDigest md = MessageDigest.getInstance("MD5");
    	md.update(input);
    	byte[] digestedValue = md.digest();
    	
    	//加密后的字符串
    	String newstr=base64en.encode(digestedValue);
	    
        return newstr;
    }  
    
//    public static void main(String args[]) {
//    	 String encryptStr = "wlj123thjklpibgdfghkaa4";
//    	 try {
//    		 System.out.println(MD5Utils.encryptByMd5(encryptStr));
//    	 } catch(Exception e) {
//    		 
//    	 }
//    	 
//    }
    

}
