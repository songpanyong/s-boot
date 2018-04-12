package com.guohuai.boot;

public enum ErrorDesEnum {
	APPLYIN("9901","该用户手机号已经申请过代扣协议申请,请继续下步操作！"),
	APPLYMORCONFIRMMOR("9902","该用户手机号已经申请过代扣协议申请和代扣协议确认,无需再次操作！"),
	CONFIRMIN("9903","请先进代扣申请,在进行代扣确认！"),
	SYSTEMMSG("9904","系统异常,请联系管理员处理！"),
	REPEAT("9905","请求流水号重复提交！"),
	ELEMENTVALI("9906","四要素验证失败"),
	ELEMENTVALICHECK("9907","该用户有卡已经验证成功过！"),
	ELEMENTUN("9908","用户银行卡已经解绑"),
	ELEUNLOCK("2","解绑"),
	ElELOCK("1","绑卡");
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
	private ErrorDesEnum(String code, String name) {
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
		for (ErrorDesEnum type : ErrorDesEnum.values()) {
			if (type.getCode().equals(code)) {
				return type.getName();
			}
		}
		return null;
	}
}
