package com.guohuai.boot.account.form;

import lombok.Data;

@Data
public class AccountInfoForm {
	
	private String userOid; // 平台ID
	private String accountNo; // 账户号
	private String accountType; // 账户类型
	private String accountName; // 账户名称
	private String accountStatus; // 账户状态
	
}
