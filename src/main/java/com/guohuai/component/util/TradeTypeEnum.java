package com.guohuai.component.util;

public enum TradeTypeEnum {
	
	trade_pay("01","充值"),
	trade_payee("02","提现"),
	trade_redeem("03","赎回"),
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
	
	private TradeTypeEnum(String code,String name){
		this.code = code;
		this.name = name;
	}
	
	public static String getEnumName(final String value) {
		for (TradeTypeEnum tradeEnum : TradeTypeEnum.values()) {
			if (tradeEnum.getCode().equals(value)) {
				return tradeEnum.getName();
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		return this.code;
	}
}
