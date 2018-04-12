package com.guohuai.component.util;

public enum ErrorEnum {
	USERTYPENOTEXISTS("9001","用户类型不存在"),
	ORDERTYPENOTEXISTS("9002","交易类型不存在"),
	USERNOTEXISTS("9003","用户不存在"),
	USEREXISTS("9004","用户已存在"),
	ACCOUNTTYPENOTEXISTS("9005","账户类型不存在"),
	ACCOUNTNOTEXISTS("9006","账户不存在"),
	BALANCELESS("9007","账户余额不足"),
	ORDEREXISTS("9008","订单已经存在"),
	RELATIONPRODUCTNOTNULL("9009","关系产品不能为空"),
	BALANCEERROR("9010","金额不能为负数"),
	
	SUCCESS("0000","成功"),
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
	private ErrorEnum(String code, String name) {
		this.code = code;
		this.name = name;
	}

	@Override
	public String toString() {
		return this.code;
	}

	/**
	 * 通过code取得类型
	 * 
	 * @param code
	 * @return
	 */
	public static String getName(String code) {
		for (ErrorEnum type : ErrorEnum.values()) {
			if (type.getCode().equals(code)) {
				return type.getName();
			}
		}
		return null;
	}

}
