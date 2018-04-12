package com.guohuai.boot.pay.form;

import lombok.Data;

@Data
public class NotfriyForm {
	private String resCode;
	private String resMessage;
	private String merchantId;
	private String merchantNo;
	private String tradeNo;
	private String status;
	private String tradeTime;
	private String memo;
	private String sign;
//	{"resCode":"00000","resMessage":"成功","merchantId":"10000009999",
//		"merchantNo":"201501090225014 84","tradeNo":"201501151426021031610000004010",
//		"status":"S","tradeTime":"20150115142602","memo ":"",
//		"sign":"HUBDYBICBSUIHUHDHBD"}
}
