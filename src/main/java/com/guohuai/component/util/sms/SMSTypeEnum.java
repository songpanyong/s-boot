package com.guohuai.component.util.sms;

public enum SMSTypeEnum {

	BINDCARD("bindCard", "绑卡短信模板");

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

	private SMSTypeEnum(String code, String name) {
		this.code = code;
		this.name = name;
	}

	public static String getEnumName(final String value) {
		for (SMSTypeEnum userTpyeEnum : SMSTypeEnum.values()) {
			if (userTpyeEnum.getCode().equals(value)) {
				return userTpyeEnum.getName();
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return this.code;
	}
}
