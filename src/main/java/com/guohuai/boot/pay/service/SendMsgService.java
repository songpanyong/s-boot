package com.guohuai.boot.pay.service;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.guohuai.component.util.HttpTool;

@Service
public class SendMsgService {
	private final static Logger log = LoggerFactory.getLogger(SendMsgService.class);
	
	@Value("${uc.host}")
	private String ucHost;
	
	//发送监控通知短信
	public boolean sendMonitorNotifyMsg(String mesParam){
		log.info("发送短信通知mesParam="+mesParam);
		String getMsgPhoneUrl = "http://" +ucHost+ "/cms/client/element/find";
    	String sendMsgUrl = "http://" +ucHost+ "/wfduc/boot/sms/sendSms";
		//第一步，获取配置的手机号码
        String codes = "["+"\""+"managerPhone"+"\""+"]";
        String getMsgPhoneParam = "codes="+codes;
        String msgPhoneResult = HttpTool.sendPost(getMsgPhoneUrl,getMsgPhoneParam);
        String phone = getMsgPhone(msgPhoneResult);//解析json获取返还手机号码
        //第二步，发送短息
        if(isMobileNO(phone)){
        	String mesTempCode = "custom";
            mesParam = "["+"\""+mesParam+"\""+"]";
            String sendMsgParam = "phone="+phone+"&mesTempCode="+mesTempCode+"&mesParam="+mesParam;
            String sendMsgResult = HttpTool.sendPost(sendMsgUrl,sendMsgParam);
            boolean result = getSendMsgResult(sendMsgResult);
            return result;
        }else{
        	log.info("发送短信通知手机号获取失败"+phone);
        }
        return false;
	}

	/**
	 * 解析返回参数，获取手机号码
	 * @param msgPhoneResult
	 * @return
	 */
	private String getMsgPhone(String msgPhoneResult) {
    	String errorCode = "";
    	String errorMessage = "";
    	String content = "";
    	try{
    	   //将字符串转换成jsonObject对象
    	   JSONObject myJsonObject = new JSONObject(msgPhoneResult);
    	   //获取对应的值
    	   errorCode = myJsonObject.get("errorCode").toString();
    	   errorMessage = myJsonObject.get("errorMessage").toString();
    	   if("0".equals(errorCode)){
    		   JSONArray jsonArray = myJsonObject.getJSONArray("datas");
               JSONObject datas =jsonArray.getJSONObject(0);
               content = datas.get("content").toString();
    	   }else{
    		   log.info("发送短信通知，获取手机号异常："+errorMessage);
    	   }
    	}catch (JSONException e){
    		log.error("发送短信通知，解析返回参数异常："+e);
    	}
		return content;
	}
	
	private boolean getSendMsgResult(String sendMsgResult){
		String errorCode = "";
    	String errorMessage = "";
		try{
    	   //将字符串转换成jsonObject对象
    	   JSONObject myJsonObject = new JSONObject(sendMsgResult);
    	   //获取对应的值
    	   errorCode = myJsonObject.get("errorCode").toString();
    	   errorMessage = myJsonObject.get("errorMessage").toString();
    	   if("0".equals(errorCode)){
    		   return true;
    	   }else{
    		   log.info("发送短信通知，获取结果异常："+errorMessage);
    	   }
    	}catch (JSONException e){
    		log.error("发送短信通知，解析返回参数异常："+e);
    	}
		return false;
	}

//	public static void main(String[] args) {
//		String mesParam = "短信发送代码测试";
//		boolean result = sendMonitorNotifyMsg(mesParam);
//		System.out.println("发送结果："+result);
//	}
	
	public boolean isMobileNO(String mobiles) {
		Pattern p = Pattern.compile("^((13[0-9])|(14[5|7])|(15([0-3]|[5-9]))|(18[0,5-9]))\\d{8}$");
		Matcher m = p.matcher(mobiles);
		return m.matches();
	}
}