package com.guohuai.boot.pay;

import com.guohuai.boot.ErrorDesEnum;

public enum CertificatesTypeEnum {
	IDCARD("1","身份证"),
	UNIFORM_CREDIT_SOCIAL_CODE("2","统一信用社会代码"),
	ORGANIZATION_CODE_CERTIFICATE("3","组织机构代码证"),
	BUSINESS_LICENSE("4","营业执照"),
	REGISTRATION_CERTIFICATE("5","登记证书"),
	NATIONAL_TAX_REGISTRATION_NUMBER("6","国税登记证号码"),
	LOCAL_TAX_REGISTRATION_NUMBER("7","地税登记证号码"),
	OPENING_PERMIT("8","开户许可证"),
	INSTITUTION_NUMBER("9","事业单位编号"),
	FINANCIAL_LICENSE_NUMBER("10","金融许可证编号"),
	
	OTHER_DOCUMENTS("11","其它证件");
	
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
	private CertificatesTypeEnum(String code, String name) {
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
