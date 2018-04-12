package com.guohuai.boot.pay.form;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

import lombok.Data;

@Data
public class ReconciliationPassForm{
	private String oid;
	private String channelId;//快付通，为以后扩展预留
	private String productId;//快付通用
	private String orderId;//快付通用（用于和支付订单关联对账）
	private String transactionCurrency;
	private BigDecimal transactionAmount;
	private String paymentBankNo;
	private String beneficiaryBankNo;
	private String tradStatus;
	private String failDetail;
	private String errorCode;
	private Timestamp transactionTime;
	private Date accountDate;
	private String checkMark;//用于记录有没有对账成功
	private String userOid;
	private Timestamp createTime;
	private Timestamp updateTime;
	private String checkDate;

}
