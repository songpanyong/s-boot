package com.guohuai.component.util;

public enum OrderTypeEnum {
	// new enum
	INVEST_T0("investT0","快申"),
	INVEST_T1("investT1","申购"),
	REDEEM_T0("redeemT0","快赎"),
	REDEEM_T1("redeemT1","赎回"),
	CONVERSION_REDEEM("conversionRedeem","转换-赎回"),
	CONVERSION_INVEST("conversionInvest","转换-申购"),
	REPAY_CAPITAL_WITH_INTEREST("repayCapitalWithInterest","还本付息"),
	WIND_UP_REDEEM("windUpRedeem","清盘赎回"),
	DIVIDEND("dividend","现金分红"),
	RECHARGE("recharge","充值"),
	WITHDRAWALS("withdraw","提现"),
	USE_RED_PACKET("useRedPacket","红包"),
	REBATE("rebate","返佣"),
	REFUND("reFund", "退款"),
	RAISE_FAILURE_REFUND("RaiseFailureReFund", "募集失败退款"),
	TRANSFER("transfer", "转账"),
	NETTING("netting", "轧差"),
    UNFREEZE("unfreeze","解冻"),
    
	INVEST("invest","投资"),
	REDEEM("redeem","赎回"),
	NETTING_DEPOSIT("nettingIncome", "轧差-入款"),
    NETTING_OUTCOME("nettingOutcome", "轧差-出款"),
    
	//申购:01、赎回:02、派息:03、赠送体验金:04、体验金到期:05
	APPLY("01","申购"),
//	DIVIDEND("03","派息"),
	GIVEFREEMONEY("04","赠送体验金"),
	EXPIREMONEY("05","体检金到期"),
	PUBLISH("06","增加发行额"),
	ABAPPLY("07","可用金收款"),
	ABREDEEM("08","可用金放款"),
	UNFROZEN("09","解冻充值冻结"),
	
	CURRENTTOREGULAR("52","活转定"),
	REGULARTOCURRENT("53","定转活"),
	OFFSETPOSITIVE("54","冲正"),
	OFFSETNEGATIVE("55","冲负"),
	REDENVELOPE("56" ,"红包"),
	PUBLISHERLOAN("57","发行人放款"),
	PUBLISHERRECE("58","发行人收款"),
	REBATEFROZEN("59","返佣冻结"),
	
	REBATEUNFROZEN("61","解冻"),
	QUOTAADJUSTMENT("62","授信额度调整")
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
	
	private OrderTypeEnum(String code,String name){
		this.code = code;
		this.name = name;
	}
	
	public static String getEnumName(final String value) {
		for (OrderTypeEnum accountTpyeEnum : OrderTypeEnum.values()) {
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
