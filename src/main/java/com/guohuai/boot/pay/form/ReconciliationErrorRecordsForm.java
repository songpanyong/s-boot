package com.guohuai.boot.pay.form;

import java.math.BigDecimal;
import java.sql.Timestamp;

import lombok.Data;

@Data
public class ReconciliationErrorRecordsForm{
	   private String oid;
	   private String orderNo;//订单号
	   private String payNo;//支付流水号
	   private String outsideOrderNo;//三方订单号
	   private String orderType;//订单类型
	   private String orderStatus;//订单状态
	   private String outsideOrderStatus;//三方订单状态
	   private BigDecimal amount;//订单金额
	   private BigDecimal outsideAmount;//三方订单金额
	   private String userOid;//用户id
	   private String userName;//用户名
	   private String userPhone;//用户手机号
	   private String channelNo;//渠道编码
	   private String channelName;//渠道名称
	   private String errorType;//异常类型
	   private String errorStatus;//异常处理状态
	   private String errorResult;//异常处理结果
	   private String applyRecord;//是否有申购记录，Y有N无
	   private Timestamp orderTime;//订单时间
	   private Timestamp createTime;//创建时间
	   private Timestamp updateTime;//更新时间
	   public String beginTime;
	   public String endTime;
	   public String remark;//备注
	   private int page=1;
	   private int rows=10;

}
