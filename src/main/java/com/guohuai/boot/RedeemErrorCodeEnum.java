package com.guohuai.boot;

public enum RedeemErrorCodeEnum {
	
	///------暂定----模拟
	YQ9996("YQ9996","没有满足条件纪录"),
	YQ9993("YQ9993","请求参数数据不对"),
	YQ9990("YQ9990","企业本账户没有开通此交易"),
	YQ9988("YQ9988","银企直连系统未签约的付款账户"),
	YQ9987("YQ9987","非法的账户网点号"),
	YQ9986("YQ9986","企业没有开通银企直联"),
	YQ9985("YQ9985","企业签名的证书不对"),
	YQ9984("YQ9984","交易暂停"),
	YQ9982("YQ9982","收款方户名输入错误"),
	YQ9981("YQ9981","企业银企直联客户未激活,请联系银行处理"),
	YQ9980("YQ9980","企业银企直联客户已暂停,请联系银行处理"),
	YQ9979("YQ9979","付款账户错误,该交易为证券公司专用交易"),
	YQ9978("YQ9978","跨行转账上送同城异地标志不符:上送同城,收方账户异地"),
	YQ9977("YQ9977","XML报文体解析编码或格式异常"),
	YQ9975("YQ9975","跨行转账上送同城异地标志必须为(1,2)"),
	YQ9974("YQ9974","跨行转账上送同城异地标志不符:上送异地,收方账户同城"),
	YQ9972("YQ9972","目标文件不存在"),
	YQ9971("YQ9971","交易请求频率过高,请稍后再试"),
	YQ9970("YQ9970","付款账户位未通企业网银,不能进行制单操作"),
	YQ9969("YQ9969","批量笔数超限,请减少笔数重试"),
	YQ9968("YQ9968","企业网银客户不允许通过银企直连录入操作"),
	YQ9967("YQ9967","企业网银客户没有可用的录入权限的操作员"),
	YQ9966("YQ9966","批量笔数或金额与文件不符"),
	YQ9964("YQ9964","证书已经过期"),
	YQ9963("YQ9963","证书尚未生效"),
	YQ9962("YQ9962","批量流水号重复"),
	YQ0000("YQ0000","无法处理的请求,请核对交易码");

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
	private RedeemErrorCodeEnum(String code, String name) {
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
		for (RedeemErrorCodeEnum type : RedeemErrorCodeEnum.values()) {
			if (type.getCode().equals(code)) {
				return type.getName();
			}
		}
		return null;
	}
	
	/**
	 * 通过name取得类型
	 * 
	 * @param code
	 * @return
	 */
	public static String getCode(String name) {
		for (RedeemErrorCodeEnum type : RedeemErrorCodeEnum.values()) {
			if (type.getName().equals(name)) {
				return type.getCode();
			}
		}
		return null;
	}

}
