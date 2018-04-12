package com.guohuai.boot.account.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.guohuai.basic.component.ext.hibernate.UUID;


@Entity
@Table(name = "t_account_platform_change_records")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class PlatformChangeRecordsEntity extends UUID implements Serializable{
	
	private static final long serialVersionUID = -7406533480884213701L;
	
	private String userOid;// 用户id    
	private String auditOid; // 审核记录oid
	private String changeType; // 变更类型
	private String accountNo; // 变更账户号
	private String oldName; // 变更前名称
	private String newName; // 变更后名称
	
	private BigDecimal oldBalance; // 变更前额度
	private BigDecimal newBalance; // 变更后额度
	private String oldOutputAccountName; // 变更前出账账户名称
	private String oldOutputAccountNo; // 变更前出账账户号
	private String newOutputAccountName; // 变更后出账账户名称
	
	private String newOutputAccountNo; // 变更后出账账户号
	private String oldIntputAccountName; // 变更前入账账户名称
	private String oldInputAccountNo; // 变更前入账账户号
	private String newInputAccountName; // 变更后入账账户名称
	private String newInputAccountNo; // 变更后入账账户号
	
	private String effectiveTimeType; //生效时间类型
	private String eventName; // 登账事件名称
	private String eventOid; // 登账事件Oid
	private String eventChildOid; //登账子事件Oid
	
	private Timestamp updateTime; // 更新时间
	private Timestamp createTime; // 创建时间
}
