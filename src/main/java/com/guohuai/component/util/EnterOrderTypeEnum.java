package com.guohuai.component.util;

public enum EnterOrderTypeEnum {
	//调增：20，调减：30
	ENTERADD("20","调增"),
	ENTERSUB("30","调减"),
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
	
	private EnterOrderTypeEnum(String code,String name){
		this.code = code;
		this.name = name;
	}
	
	public static String getEnumName(final String value) {
		for (EnterOrderTypeEnum accountTpyeEnum : EnterOrderTypeEnum.values()) {
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
