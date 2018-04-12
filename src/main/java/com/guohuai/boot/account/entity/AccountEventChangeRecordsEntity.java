package com.guohuai.boot.account.entity;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.guohuai.basic.component.ext.hibernate.UUID;


@Entity
@Table(name = "t_account_event_change_records")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class AccountEventChangeRecordsEntity extends UUID implements Serializable{
	
	private static final long serialVersionUID = -8237819596289520826L;
	//状态  0-未生效 1-已生效 2-撤销
	public static final String STATUS_NOT = "0";
	public static final String STATUS_YES = "1";
	public static final String STATUS_REVOKE = "2";
	
	private String eventOid; // 登账事件oid
	private String auditOid; // 审核oid
	private String eventChildOid; // 登账子事件oid
	private String oldOutputAccountNo; // 变更前出账账户号
	private String newOutputAccountNo; // 变更后出账账户号
	private String oldIntputAccountNo; // 变更前入账账户号
	private String newIntputAccountNo; // 变更后入账账户号
	private String effevtiveStatus; // 生效状态
	private Timestamp effectiveTime; // 生效时间
	private Timestamp updateTime; // 更新时间
	private Timestamp createTime; // 创建时间
}
