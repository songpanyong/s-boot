package com.guohuai.boot.pay.form;

import java.sql.Date;
import java.sql.Timestamp;

import lombok.Data;

@Data
public class AuthenticationForm{
	private String oid;
	private String userOid;//用户ID
	private String smsCode;
	private String requestNo;//请求流水号
	private String orderNo;//订单号
	private String merchantId;//商户身份
	private String protocolType;//协议类型11：借记卡扣款 12：信用卡扣款 13：余额扣款 14：余额+借记卡 扣款15： 余额+信用卡 扣款 目前只支持借记 卡扣款
	private String note;//说明
	private Date startDate;//协议生效日期
	private Date endDate;//协议失效日期
	private String custAccountId;//账户id
	private String holderName;//真名
	private String certificateNo;//证件号
	private String realName;//客户姓名
	private String type;//鉴权类别 申请：01 确认 02
	private String bankCode;//银行行别
	private String cardType;//银行卡类型
	private String cardNo;//银行卡号
	private String phone;//预留手机号
	private String certificatetype;//证件类型 身份证：0
	private Timestamp requestTime;
	private String smsNo;//（在响应时返回）结算系统记录
	private String status;//开通协议请求返回的状态
	private String failDetail;
	private String errorCode;
	private Timestamp returnTime;//返回信息时间
	private String protocolNo;//代扣协议号，后续进行代扣时需要这个信息
	private Timestamp createTime;
	private Timestamp updateTime;
	private int queryType=0;//[0:查询状态;1:查询代扣申请流程]
    private int page=1;
    private int rows=10;

}
