package com.guohuai.component.util;

public enum RedeemTypeEnum {
	//T+0：T0，T+1：T1
	REDEEMT0("T0","T+0赎回"),
	REDEEMT1("T1","T+1赎回"),
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
	
	private RedeemTypeEnum(String code,String name){
		this.code = code;
		this.name = name;
	}
	
	public static String getEnumName(final String value) {
		for (RedeemTypeEnum redEnum : RedeemTypeEnum.values()) {
			if (redEnum.getCode().equals(value)) {
				return redEnum.getName();
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		return this.code;
	}
}
