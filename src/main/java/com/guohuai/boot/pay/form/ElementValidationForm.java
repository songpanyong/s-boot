package com.guohuai.boot.pay.form;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class ElementValidationForm{
	  private String oid;
      private String userOid;
      private String reuqestNo;
      private String systemSource;
      private Timestamp receivingTime;
      private Timestamp feedbackTime;
      private String merchantId;
      private String productId;
      private String orderNo;
      private String bankCode;//客服银行卡行别',
      private String realName;//客户姓名',
      private String cardNo;//客户银行账号',
      private String certificatetype;//客户借记卡类型',
      private String certificateNo;//客户证件号码',
      private String phone;//预留手机号',
      private String status;
      private String failDetail;//失败详情',
      private String errorCode;//错误码',
      private Timestamp channelReturnTime;//返回时间',
      private Timestamp createTime;
      private Timestamp updateTime;
      private int page=1;
      private int rows=10;
}
