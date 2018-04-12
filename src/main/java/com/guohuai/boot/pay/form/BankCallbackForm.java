package com.guohuai.boot.pay.form;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class BankCallbackForm{
	private String oid;
	private String orderNO;
	/**
	 * 渠道
	 */
	private String channelNo;
	/**
	 * 交易类别
	 */
	private String tradeType;
	/**
	 * 分钟
	 */
	private String minute;
	/**
	 * 回调发起时间
	 */
	private String callbackDate;
	/**
	 * 回调次数
	 */
	private Integer count;
	/**
	 * 设置回调最大次数
	 */
	private Integer totalCount;
	/**
	 * 
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
	private String payNo;
	private String type;
	private int page=1;
	private int rows=10;
	private String startTime;
	private String endTime;
}
