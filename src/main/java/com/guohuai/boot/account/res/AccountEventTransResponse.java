package com.guohuai.boot.account.res;

import java.math.BigDecimal;
import java.sql.Timestamp;

import com.guohuai.account.api.response.BaseResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class AccountEventTransResponse extends BaseResponse {

	private static final long serialVersionUID = 1972652132338751421L;
	/**
	 * 来源系统
	 */
	private String requestNo;//请求流水号
	private String payNo;//支付流水号
	private String transNo; //登帐事件编号
	private String childEventName;//登帐事件名称
	private BigDecimal balance; //登帐金额（元）
	private String status;//登帐状态
	private String outputAccountNo;//出款方
	private String inputAccountNo;//收款方
	private Timestamp createTime;//登帐时间
	private String remark;//备注
	
	private String orderNo;//订单号
	private String orderType;//订单类型
	
}
