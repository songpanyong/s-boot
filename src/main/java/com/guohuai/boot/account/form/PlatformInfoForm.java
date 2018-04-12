package com.guohuai.boot.account.form;

import lombok.Data;

@Data
public class PlatformInfoForm {
	
	private String userOid; // 平台ID
	private String platformName; // 平台名称
	private String platformStatus; // 平台状态
	private String bindCardStatus; // 平台绑卡状态
	private int page = 1;
    private int rows = 10;
	
}
