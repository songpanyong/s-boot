package com.guohuai.boot;

public enum PayEnum {
	OPERATORTYPE0("0","发送"),
	OPERATORTYPE1("1","查询结果"),
	OPERATORTYPE2("2","修改状态"),
	CURRENCY("0","CNY"),
	PAY0("0","未处理"),
	PAY1("1","交易成功"),
	PAY2("2","交易失败"),
	PAY3("3","交易处理中"),
	PAY4("4","超时"),//超时的放到处理中，等人功处理
	PAY5("5","撤销"),
	CERTIFICATETYPE("0","身份证"),
	PROTOCOLTYPE("11","借记卡扣款"),
	ERRORCODE0("0","失败"),
	ERRORCODE1("1","成功"),
	ERRORCODE3("3","绑卡中"),
	ELEMENT_CERTIFICATETYPE("1","个人用户"),
	ELEMENT_ENTERPRISE("2","企业用户"),
	PROXY02("02","确认"),
	PROXY01("01","申请"),
	RECONCILIATION0("0","未对账"),
	RECONCILIATION1("1","对账成功"),
	RECONCILIATION2("2","已匹配对账失败"),
	RECONCILIATION3("3","未匹配对账失败"),
	PAYTYPE01("01","申购"),
	PAYTYPE02("02","赎回"),
	PAYTYPE03("03","派息"),
	PAYTYPE04("04","赠送体验金"),
	PAYTYPE05("05","体验金到期"),
	PAYTYPE07("07","可用金放款"),
	PAYTYPE08("08","可用金收款"),
	PAYTYPE50("50","充值"),
	PAYTYPE51("51","提现"),
	PAYTYPE52("52","活转定"),
	PAYTYPE53("53","定转活"),
	PAYTYPE54("54","冲正"),
	PAYTYPE55("55","冲负"),
	PAYTYPE56("56","红包"),
	PAYTYPE57("57","基本户放款"),
	PAYTYPE58("58","基本户收款"),
	PAYTYPE59("59","返佣冻结"),
	PAYTYPE60("60","返佣"),
	PAYTYPE61("61","解冻"),
	AUDIT0("0","审核不通过"),
	AUDIT1("1","审核通过"),
	OPERATOR01("01","单笔支付"),
	OPERATOR02("02","失败重发"),
	OPERATOR03("03","撤销"),
	OPERATOR04("04","修改状态"),
	OPERATORTYPE01("01","支付/重发"),
	OPERATORTYPE02("02","修改"),
	OPERATORTYPE03("03","撤销"),
	PAYMETHOD1("1","前台"),
	PAYMETHOD2("2","后台");
	
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
	private PayEnum(String code, String name) {
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
		for (PayEnum type : PayEnum.values()) {
			if (type.getCode().equals(code)) {
				return type.getName();
			}
		}
		return null;
	}

}
