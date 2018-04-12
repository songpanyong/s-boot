package com.guohuai.component.util;

public enum AccountTypeEnum {
	
	CURRENT("01","活期户"),
	CURRENTINTEREST("02","活期利息户"),
	EXPERIENCE("03","体验金"),
	ONWAY("04","在途户"),
	FROZEN("05","提现冻结户"),
	REGULAR("06","定期户"),
	PRODUCT("07","产品户"),
//	STOCKFUND("08","备付金户"),
	RESERVE("08","备付金户"),
	SUPERFAMILY("09","超级户"),
	BASICER("10","基本户"),
	OPERATE("11","运营户"),
	COLLECTION_SETTLEMENT("12", "归集清算户"),
	AVAILABLE_AMOUNT("13", "可用金户"),
	RECHARGEFROZEN("14","充值冻结户"),
	REDEEMFROZEN("15","冻结资金户"),
	REGULARINTEREST("16","定期利息户"),
	CONTINUED_INVESTMENT_FROZEN("17","续投冻结户");
	
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
	
	private AccountTypeEnum(String code,String name){
		this.code = code;
		this.name = name;
	}
	
	public static String getEnumName(final String value) {
		for (AccountTypeEnum accountTpyeEnum : AccountTypeEnum.values()) {
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
