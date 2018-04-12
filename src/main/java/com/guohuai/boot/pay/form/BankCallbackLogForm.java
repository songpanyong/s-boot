package com.guohuai.boot.pay.form;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class BankCallbackLogForm{
	
	private String oid;
	private String callBackOid;
	/**
	 * 支付流水号
	 */
	private String payNo;
	/**
	 * 状态
	 */
	private String status;
	/**
	 * 银行返回代码
	 */
	private String returnCode;
	/**
	 * 银行返回错误信息
	 */
	private String returnMsg;
	private Timestamp createTime;
	private Timestamp updateTime;
	private String bankReturnSerialId;
	private int page=1;
	private int rows=10;
	private String startTime;
	private String endTime;
}
