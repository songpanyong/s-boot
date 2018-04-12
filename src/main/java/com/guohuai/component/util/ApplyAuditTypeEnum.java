package com.guohuai.component.util;

public enum ApplyAuditTypeEnum {
	//申请原因包含
	CHANGE_NAME1("01","平台名称名称更改"),
	DISABLE_PLATFORM("02","平台停用"),
	ENABLE_PLATFORM("03","平台启用"),
	BUILD_PROVISION_ACCOUNT("04","新建备付金账户"),
	ENABLE_PROVISION_ACCOUNT("05","启用备付金账户"),
	DISABLE_PROVISION_ACCOUNT("06","停用备付金账户"),
	CHANGE_CREDIT("07","调整授信额度"),
	CHANGE_EVENT("08","设置平台登账事件"),
	CHANGE_NAME2("09","账户名称更改"),
	
	//申请原因类型包含
	PLATFORM_INFO_CHANGE("1","平台基本信息变更"),
	PLATFORM_CREDIT_CHANGE("2","平台账户额度调整"),
	PLATFORM_EVENT_CHANGE("3","平台登账设置更新");
	
	//审核中
	public static final String AUDIT_YES = "1";
	//无审核
	public static final String AUDIT_NO = "0";
	
	private String code;
	private String name;
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	private ApplyAuditTypeEnum(String code,String name){
		this.code = code;
		this.name = name;
	}
	
	public static String getEnumName(final String value) {
		for (ApplyAuditTypeEnum accountTpyeEnum : ApplyAuditTypeEnum.values()) {
			if (accountTpyeEnum.getCode().equals(value)) {
				return accountTpyeEnum.getName();
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		return this.code;
	}
}
