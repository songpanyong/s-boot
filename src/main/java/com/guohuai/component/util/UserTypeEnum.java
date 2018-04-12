package com.guohuai.component.util;

public enum UserTypeEnum {

	INVESTOR("T1", "投资人账户"), PUBLISHER("T2", "发行人账户"), PLATFORMER("T3", "平台账户"),;

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

	private UserTypeEnum(String code, String name) {
		this.code = code;
		this.name = name;
	}

	public static String getEnumName(final String value) {
		for (UserTypeEnum userTpyeEnum : UserTypeEnum.values()) {
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
