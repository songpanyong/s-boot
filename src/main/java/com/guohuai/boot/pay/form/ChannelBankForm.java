package com.guohuai.boot.pay.form;

import java.math.BigDecimal;
import java.sql.Timestamp;

import lombok.Data;

@Data
public class ChannelBankForm{
	   private String oid;
	   private String userOid;
	   private String channelNo;//渠道N
	   private String channelName;//渠道名
	   private String channelbankCode;//渠道银行代码
	   private String channelbankName;//渠道银行名称
	   private BigDecimal singleQuota;//单笔限额
	   private BigDecimal dailyLimit;//日限额
	   private BigDecimal monthlyLimit;//月限额
	   private String standardCode;//人行代码
	   private Timestamp createTime;
	   private Timestamp updateTime;
	   private int page=1;
	   private int rows=10;

}
