package com.guohuai.boot.account.validate.service;

import org.apache.commons.lang3.StringUtils;

import com.guohuai.boot.account.dto.AccountEventReqDTO;
import com.guohuai.boot.account.dto.AccountEventResDTO;
import com.guohuai.component.util.ErrorEnum;

/**   
 * @Description: 登账事件服务验证 
 * @author ZJ   
 * @date 2018年1月18日 上午11:17:13 
 * @version V1.0   
 */
public class AccountEventServiceVal {
	/**
	 * 验证查询登账事件信息入参
	 * @param req
	 * @return
	 */
	public static AccountEventResDTO valQueryAccountEventInfo(AccountEventReqDTO req) {
		AccountEventResDTO result = new AccountEventResDTO();

		if (null == req) {
			result.setError("9054");// 查询登账事件信息请求对象为空
			return result;
		}
		if (StringUtils.isEmpty(req.getUserOid())) {
			result.setError("9055");// 平台id为空
			return result;
		}
		if (StringUtils.isEmpty(req.getTransType())) {
			result.setError("9056");// 交易类型为空
			return result;
		}
		if (StringUtils.isEmpty(req.getEventType())) {
			result.setError("9057");// 事件类型为空
			return result;
		}

		result.setError(ErrorEnum.SUCCESS.getCode());// 成功
		return result;
	}
}