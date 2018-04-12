package com.guohuai.boot.account.res;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.guohuai.account.api.request.entity.TradeEvent;
import com.guohuai.account.api.response.BaseResponse;
import com.guohuai.boot.account.entity.AccountEventChildEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;

@Data
@EqualsAndHashCode(callSuper=false)
public class AccountEventResponse extends BaseResponse {

	private static final long serialVersionUID = 1972652132338751421L;
	
	private String orderNo;//订单号
	private String orderType;//订单类型
	private String requestNo;//请求流水号
	private String remark;//备注
	private AccountEventChildEntity accountEventChildEntity;//触发子事件
	private TradeEvent tradeEvent;//登帐事件
	private AccountInfoEntity outputAccountEntity;//出账账户信息
	private AccountInfoEntity inputAccountEntity;//入账账户信息
	
}
