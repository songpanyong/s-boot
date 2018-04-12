package com.guohuai.component.util;

public enum BindBankCardEnum {

	ENTERPRISE("01", "企业"),
	PERSONAL("02", "个人"),
	//0未绑卡，1已绑卡，2未绑卡绑卡申请中，3绑卡审核通过（个人卡），4已绑卡换绑申请中，5已绑卡换绑审核通过（个人卡）6已绑卡换绑解绑失败（个人卡）
	BIND_STATUS_0("0","未绑卡"),//两个都不调，显示绑定银行卡按钮
	BIND_STATUS_1("1","已绑卡"),//调用query，显示更换银行卡按钮
	BIND_STATUS_2("2","未绑卡绑卡申请中"),//不调，无按钮
	BIND_STATUS_3("3","绑卡审核通过（个人卡）"),//调用info，显示更换银行卡按钮，当点击提交时，提示已存在绑卡审核信息，显示获取验证码
	BIND_STATUS_4("4","已绑卡换绑申请中"),//调用query，无按钮
	BIND_STATUS_5("5","已绑卡换绑审核通过（个人卡）"),//全调，显示更换银行卡按钮，当点击提交时，提示已存在绑卡审核信息，显示获取验证码
	BIND_STATUS_6("6","已绑卡换绑解绑失败（个人卡）");//全调，显示更换银行卡按钮，当点击提交时，提示已存在绑卡审核信息，显示解绑按钮

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

	private BindBankCardEnum(String code, String name) {
		this.code = code;
		this.name = name;
	}

	public static String getEnumName(final String value) {
		for (BindBankCardEnum bindBankCardEnum : BindBankCardEnum.values()) {
			if (bindBankCardEnum.getCode().equals(value)) {
				return bindBankCardEnum.getName();
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return this.code;
	}
}
