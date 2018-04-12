package com.guohuai.boot.pay.form;

import lombok.Data;

@Data
public class ReconciliationStatisticsForm {
	
	private String channelNo;//渠道编码
	private String outsideDate;//三方支付单清算日

}
