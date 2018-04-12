package com.guohuai.boot.pay.vo;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(
    name = "t_bank_authentication"
)
public class AuthenticationVo implements Serializable{
	private static final long serialVersionUID = -3286518029485346965L;
	@Id
	private String oid;
	private String userOid;//用户ID
	private String type;//鉴权类别 申请：01 确认 02
	private String smsCode;
	private String orderNo;//订单号//请求流水号
	private String merchantId;//商户身份
	private String protocolType;//协议类型11：借记卡扣款 12：信用卡扣款 13：余额扣款 14：余额+借记卡 扣款15： 余额+信用卡 扣款 目前只支持借记 卡扣款
	private String note;//说明
	private Date startDate;//协议生效日期
	private Date endDate;//协议失效日期
	private String certificatetype;//证件类型 身份证：0
	private String certificateNo;//证件号
	private String realName;//客户姓名
	private String cardNo;//银行卡号
	private String bankCode;//银行行别
	private String cardType;//银行卡类型
	private String bankName;//银行名称
	private String phone;//预留手机号
	private Timestamp requestTime;
	private String smsNo;//（在响应时返回）结算系统记录
	private String status;//开通协议请求返回的状态
	private String failDetail;
	private String errorCode;
	private Timestamp returnTime;//返回信息时间
	private String protocolNo;//代扣协议号，后续进行代扣时需要这个信息
	private Timestamp createTime;
	private Timestamp updateTime;
	private String requestNo;
}
