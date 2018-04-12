package com.guohuai.boot.account.res;

import java.util.List;

import lombok.Data;

@Data
public class PlatformInfoResponse {

	private String userOid; // 平台ID
	private String platformName; // 平台名称
	private String platformStatus; // 平台状态
	private String bindCardStatus; // 平台绑卡状态
	private String settleStatus; // 平台设置状态0正常1变更中
	private List<String> openList; // 已启用账户
	private List<String> closeList; //已停用账户
	
}
