package com.guohuai.boot.account.entity;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "t_account_platform_info_audit")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class PlatformInfoAuditEntity implements Serializable{
	
	private static final long serialVersionUID = -8738947796014377129L;
	
	@Id
	private String oid;
	private String userOid; // 平台id
	private String userName; // 平台名称
	private String userType; // 账户类型
	private String userStatus; // 平台状态
	private String applyType; // 申请原因类型
	private String applyTypeName; // 申请原因类型名称
	private String applyReason; // 申请原因
	private String applicantId; // 申请人id
	private String applicantName; // 申请人姓名
	private String auditStatus; // 审核状态
	private String auditReason; // 审核原因
	private String operatorId; // 审核人id
	private String operatorName; // 审核人姓名
	private Timestamp updateTime; // 更新时间
	private Timestamp createTime; // 创建时间
	private String phone; //用户账号
}
