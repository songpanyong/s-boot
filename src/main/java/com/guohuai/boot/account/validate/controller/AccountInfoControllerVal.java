package com.guohuai.boot.account.validate.controller;

import org.apache.commons.lang3.StringUtils;

import com.guohuai.account.api.request.CreateAccountRequest;
import com.guohuai.account.api.response.CreateAccountResponse;
import com.guohuai.basic.config.ErrorDefineConfig;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.UserTypeEnum;

/**   
 * @Description: 账户信息控制器验证 
 * @author ZJ   
 * @date 2018年1月22日 下午6:12:35 
 * @version V1.0   
 */
public class AccountInfoControllerVal {
	/**
	 * 验证创建产品户入参
	 * @param req
	 * @return
	 */
	public static CreateAccountResponse valCreateProductAccount(CreateAccountRequest req) {
		CreateAccountResponse result = new CreateAccountResponse();
		
    	if (null == req) {
    		result.setReturnCode("9065");// 创建产品户入参对象为空
    		result.setErrorMessage(ErrorDefineConfig.define.get(Integer.valueOf(result.getReturnCode())));
			return result;
        }
    	if (StringUtils.isEmpty(req.getUserOid())) {
    		result.setReturnCode("9066");// 发行人为空
    		result.setErrorMessage(ErrorDefineConfig.define.get(Integer.valueOf(result.getReturnCode())));
            return result;
        }
    	result.setReturnCode(Constant.SUCCESS);
		result.setErrorMessage(ErrorDefineConfig.define.get(Integer.valueOf(result.getReturnCode())));
		return result;
	}
}