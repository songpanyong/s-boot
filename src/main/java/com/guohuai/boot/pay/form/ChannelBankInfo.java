package com.guohuai.boot.pay.form;

import java.io.Serializable;

import lombok.Data;

@Data
public class ChannelBankInfo implements Serializable {
	private static final long serialVersionUID = -3966810509224769756L;
	String standardCode, channelbankName, channelbankCode, bankCode, singleQuota, dailyLimit, monthlyLimit;
}
