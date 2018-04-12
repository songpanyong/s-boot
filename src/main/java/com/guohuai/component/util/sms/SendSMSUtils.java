package com.guohuai.component.util.sms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.basic.component.ext.web.BaseResp;
import com.guohuai.basic.component.sms.ContentSms;
import com.guohuai.basic.component.sms.SmsAware;
import com.guohuai.component.exception.AMPException;

@Slf4j
@Service
public class SendSMSUtils {
	
	@Value("${sms.yimei.contentTypes:#{null}}")
	private String contentTypes;
	
	@Value("${sms.ronglian.tempTypes:#{null}}")
	private String tempTypes;
	
	//短信发送类型：content内容/temp模板
	@Value("${sms.sendTypes:content}")
	private String smsSendTypes;
	
	@Autowired
	private RedisTemplate<String, String> redis;
	
	/**短信验证码过期时间*/
	public static final int EXPIRE_SECONDS = 120;
	/** 短信发送类型--内容 */
	public static final String SMS_SendType_Content = "content";
	/** 短信发送类型--模板 */
	public static final String SMS_SendType_Temp = "temp";
	/**短信模板*/
	public static final Map<String, String > smsContentsMap = new HashMap<String, String>();
	
	@PostConstruct
	public void initSMS() {
		// 初始化模板
		if (!StringUtil.isEmpty(this.contentTypes)) {
			List<SMSTypeEntity> list = JSON.parseArray(this.contentTypes, SMSTypeEntity.class);
			if (smsContentsMap.size() == 0) {
				for (SMSTypeEntity en : list) {
					smsContentsMap.put(en.getSmsType(), en.getContent());
				}
			}
		}
	}
	
	/**
	 * 根据发送类型发送短信
	 * @param phone 手机号
	 * @param smsType 短信类型
	 * @param values 短信模板值
	 */
	public BaseResp sendSMSBySendTypes(String phone, String smsType, String[] values) {
		BaseResp resp = new BaseResp();
		try{
			this.generateVeriCode(phone, smsType, values);
			// 发送短信内容
			String notifyContent = "";
			if (SendSMSUtils.SMS_SendType_Content.equals(this.smsSendTypes)) {
				String content = this.replaceComStrArr(SendSMSUtils.smsContentsMap.get(smsType), values);
				
				if (!StringUtil.isEmpty(content)) {
					ContentSms contentSms = this.setContentSms(phone, content);
					notifyContent = JSON.toJSONString(contentSms);
					log.info("发送短信内容：{}", notifyContent);
					resp = SmsAware.send(contentSms);
					log.info("发送短信返回：code:{}，msg:{}", resp.getErrorCode(), resp.getErrorMessage());
				} else {
					log.error("短信模版不存在");
					throw AMPException.getException("短信模版不存在");
				}
			} else {
				log.error("仅支持contents模板");
				throw AMPException.getException("仅支持contents模板");
			}
		} catch (Exception e) {
			log.error("短信发送失败。原因：{}", AMPException.getStacktrace(e));;
			resp.setErrorCode(-1);
			resp.setErrorMessage(AMPException.getStacktrace(e));
		}
		return resp;
	}
	
	/**
	 * 校验验证码
	 * @param phone 手机号
	 * @param smsType 短信类型
	 * @param veriCode 短信码
	 * @return 校验结果
	 */
	public Boolean checkVeriCode(String phone, String smsType, String veriCode) {
		Boolean result = false;
		String vericode = "";
		try{
			vericode = StrRedisUtil.get(redis, StrRedisUtil.VERI_CODE_REDIS_KEY + phone + "_" + smsType);
		}catch(Exception e){
			log.error("获取验证码异常");
			return false;
		}
		result = veriCode.equals(vericode);
		log.info("验证码校验:{}VS{},校验结果：{}",veriCode, vericode , result);
		return result;
	}
	
	/**
	 * 获取验证码
	 * @param phone 手机号
	 * @param smsType 短信类型
	 * @return 验证码
	 */
	public String getVeriCode(String phone, String smsType) {
		String vericode = null;
		try{
			vericode = StrRedisUtil.get(redis, StrRedisUtil.VERI_CODE_REDIS_KEY + phone + "_" + smsType);
		}catch(Exception e){
			log.error("获取验证码异常");
			return null;
		}
		return vericode;
	}
	
	/**
	 * 生成短信验证码
	 */
	public String[] generateVeriCode(String phone, String smsType, String[] values) {
		// 验证码
		String randonNum = NumberUtil.randomNumb();
		log.info("随机数:{}", randonNum);
		if(SMSTypeEnum.BINDCARD.getCode().equals(smsType)){
			values[0] = randonNum;
			values[1] = String.valueOf(SendSMSUtils.EXPIRE_SECONDS);
			String veriCode = values[0];
			String key = StrRedisUtil.VERI_CODE_REDIS_KEY + phone + "_" + smsType;
			log.info("key={},手机号：{}短信类型为：{}的验证码是：{}", key, phone, smsType, veriCode);
			boolean exist = StrRedisUtil.exists(redis, key);
			if (exist) {
				// 验证码已生成！
				throw AMPException.getException("验证码已生成");
			}
			boolean result = StrRedisUtil.setSMSEx(redis, key, SendSMSUtils.EXPIRE_SECONDS, veriCode);
			if (!result) {
				// error.define[120000]=生成验证码失败！(CODE:120000)
				throw AMPException.getException(120000);
			}
		}
		return values;
	}
	
	/**
	 * 替换掉短信模板中{1}格式的字样
	 * @param target 目标
	 * @param repArr 替换的值
	 * @return
	 */
	public String replaceComStrArr(String target, String[] repArr){
		if (null == target) {
			return StringUtil.EMPTY;
		}
		for (int i = 1; i <= repArr.length; i++) {
			target = target.replace("{" + i +"}" , repArr[i - 1]);
		}
		return target;
	}
	
	/**
	 * 组装参数
	 * @param phone 手机号
	 * @param content 短信内容
	 * @return
	 */
	public ContentSms setContentSms(String phone, String content) {
		ContentSms contentSms = new ContentSms();
		contentSms.setPhone(phone);
		contentSms.setContent(content);
		return contentSms;
	}
}