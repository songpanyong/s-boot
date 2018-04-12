package com.guohuai.boot.account.dto;

import java.io.Serializable;
import java.util.List;

import com.guohuai.basic.config.ErrorDefineConfig;
import com.guohuai.boot.account.entity.AccountEventChildEntity;
import com.guohuai.boot.account.entity.AccountEventEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**   
 * @Description: 登账事件响应对象 
 * @author ZJ   
 * @date 2018年1月18日 上午10:56:56 
 * @version V1.0   
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AccountEventResDTO implements Serializable {
	private static final long serialVersionUID = -1491119308328181434L;
	private String returnCode;// 结果码
	private String errorMessage;// 结果描述
	private AccountEventEntity accountEventEntity; // 账户事件
	private List<AccountEventChildEntity> accountEventChildEntitys;// 账户子事件结果集

	public void setError(String errorCode) {
		this.returnCode = errorCode;
		this.errorMessage = ErrorDefineConfig.define.get(Integer.valueOf(errorCode));
	}
}