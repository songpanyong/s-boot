package com.guohuai.boot.account.form;

import lombok.Data;

@Data
public class AccountBindCardAuditForm {
	
	private String oid; // 平台oid
	private String userOid; // 平台oid
	private String platformName; // 平台名称
	private String accountBankType; // 账户类型
	private String realName; // 账户名称
	private String cardNo; // 银行账号
	private String bankName; // 开户行名称
	private String bankBranch; // 开户支行
	private String bankAddress; // 开户地区
	private String province; // 开户行所属省份
	private String city; //开户行所属城市
	private String certificateNo; // 身份证号
	private String phone; // 手机号
	private String applicantId; // 申请人id
	private String applicantName; // 申请人姓名
	private String auditStatus; // 审核状态
	private String auditMark; // 审核意见
	private String operatorId; // 审核人id
	private String operatorName; // 审核人姓名
	private String beginTime; // 开始时间
	private String endTime; // 结束时间
    private int page = 1;
    private int rows = 10;
    /**绑卡状态
     * 0未绑卡，
     * 1已绑卡，
     * 2未绑卡绑卡申请中，
     * 3绑卡审核通过（个人卡），
     * 4已绑卡换绑申请中，
     * 5已绑卡换绑审核通过（个人卡）
     * 6审核通过待解绑（个人卡）
     */
    private String bindCardStatus;

}
