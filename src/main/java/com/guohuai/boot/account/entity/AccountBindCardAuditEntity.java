package com.guohuai.boot.account.entity;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.guohuai.basic.component.ext.hibernate.UUID;

@Entity
@Table(name = "t_account_bind_card_audit")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class AccountBindCardAuditEntity extends UUID implements Serializable {

	private static final long serialVersionUID = -2688587865946879517L;

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
	private Timestamp updateTime; // 更新时间
	private Timestamp createTime; // 创建时间
}
