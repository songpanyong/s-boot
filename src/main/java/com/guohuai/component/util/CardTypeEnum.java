package com.guohuai.component.util;

public enum CardTypeEnum {

	DEBITCARD("01", "借记卡");

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

	private CardTypeEnum(String code, String name) {
		this.code = code;
		this.name = name;
	}

	public static String getEnumName(final String value) {
		for (CardTypeEnum userTpyeEnum : CardTypeEnum.values()) {
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
