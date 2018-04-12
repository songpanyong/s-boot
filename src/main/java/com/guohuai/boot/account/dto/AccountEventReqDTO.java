package com.guohuai.boot.account.dto;

import java.io.Serializable;

import lombok.Data;

/**   
 * @Description: 登账事件请求对象  
 * @author ZJ   
 * @date 2018年1月18日 上午11:09:09 
 * @version V1.0   
 */
@Data
public class AccountEventReqDTO implements Serializable {
	private static final long serialVersionUID = 4285380287856931790L;
	private String userOid; // 平台Oid
	private String eventType; // 事件类型01充值02提现03转账
	private String transType; // 交易类型
}