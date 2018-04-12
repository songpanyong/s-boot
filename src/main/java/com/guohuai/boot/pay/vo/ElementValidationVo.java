package com.guohuai.boot.pay.vo;
import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
@Data
@Entity
@Table(
    name = "t_bank_element_validation"
)
public class ElementValidationVo implements Serializable{
      private static final long serialVersionUID = -3286518029485346965L;
	  @Id
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
      private String status;//0:失败;1:成功;2:解绑;3:短信验证
      private String failDetail;//失败详情',
      private String errorCode;//错误码',
      private Timestamp channelReturnTime;//返回时间',
      private Timestamp createTime;
      private Timestamp updateTime;
      private String cardOrderId;
      private String smsCode;
      private String bindChannel;//绑卡渠道
}