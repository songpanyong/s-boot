package com.guohuai.boot;

public enum PayXFEnum {
	XF00000("00000","成功"),
	XF00001("00001","已受理"),
	XF00002("00002","订单处理中"),
	XF10000("10000","参数不合法"),
	XF10001("10001","参数值传入错误"),
	XF10003("10003","渠道未开通"),
	XF10004("10004","银行返回失败"),
	XF10005("10005","订单重复提交"),
	XF10007("10007","用户或商户编号不存在"),
	XF10009("10009","交易记录不存在"),
	XF10010("10010","账户余额不足"),
	XF10011("10011","未开通无卡支付"),
	XF10012("10012","退款次数超限"),
	XF10013("10013","累计退款金额超限"),
	XF10024("10024","姓名、身份证、卡号不一致"),    
	XF10025("10025","超银行限额"),  
	XF10026("10026","账户不存在"),  
	XF10027("10027","银行通讯异常"),
	XF11000("11000","订单支付中断"),
	XFS("S","支付成功"),
	XFF("F","支付失败");
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
	private PayXFEnum(String code, String name) {
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
		for (PayXFEnum type : PayXFEnum.values()) {
			if (type.getCode().equals(code)) {
				return type.getName();
			}
		}
		return null;
	}

}
