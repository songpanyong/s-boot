package com.guohuai.component.util;

public enum TransferOrderTypeEnum {
	//基本户到发行人账户：11，发行人到基本户账户：12 备付金到超级户:13，超级户到备付金：14，备付金到基本户:15，基本户到备付金：16
	BASETOPUB("11","基本户到发行人账户"),
	PUBTOBASE("12","发行人到基本户账户"),
	PRETOSUP("13","备付金到超级户"),
	SUPTOPRE("14","超级户到备付金"),
	PRETOBASE("15","备付金到基本户"),
	BASETOPRE("16","基本户到备付金"),
	;
	
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
	
	private TransferOrderTypeEnum(String code,String name){
		this.code = code;
		this.name = name;
	}
	
	public static String getEnumName(final String value) {
		for (TransferOrderTypeEnum accountTpyeEnum : TransferOrderTypeEnum.values()) {
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
