package com.guohuai.component.common;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.guohuai.basic.config.ErrorDefineConfig;

/**
 * 
* @ClassName: AccountEerrorDefineConfig 
* @Description: 根据code获取错误信息
* @author chendonghui  
* @date 2018年3月21日 下午3:22:10
 */
@Configuration
@ConfigurationProperties(prefix = "error")
@PropertySource({"classpath:errormessage.properties"})
@Component
public class AccountEerrorDefineConfig {

	public static Map<String, String> define = new HashMap<String, String>();
	 
    public Map<String, String> getDefine() {
       return define;
    }
 
    public void setDefine(Map<String, String> define) {
    	AccountEerrorDefineConfig.define = define;
    }
    
}
