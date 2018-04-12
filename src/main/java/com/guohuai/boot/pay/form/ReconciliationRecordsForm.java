package com.guohuai.boot.pay.form;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class ReconciliationRecordsForm{
	   private String oid;
	   private String userOid;
	   private String reconDate;//对账日期YYYYMMDD
	   private String reconStatus;//'0：对账失败 1：已对帐'
	   private String failDetial;//失败详情
	   private Timestamp createTime;
	   private Timestamp updateTime;
	   private String channelId;//渠道

}
