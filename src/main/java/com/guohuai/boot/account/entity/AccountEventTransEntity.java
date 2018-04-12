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
@Table(name = "t_account_event_trans")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class AccountEventTransEntity extends UUID implements Serializable {
	
	private static final long serialVersionUID = -2889108156950429018L;
	
	// 状态0:创建 1：成功 2：失败
	public static final String STATUS_INIT = "0"; 
	public static final String STATUS_SUCCESS = "1"; 
	public static final String STATUS_FAIL = "2"; 
	
	private String orderNo; // 订单号
	private String requestNo; //请求流水号
	private String orderType; // 订单类别
	private String transNo; // 事件交易号
	private String eventChildOid; // 子事件Oid
	private String childEventName; // 子事件名称
	private String childEventType; // 子事件类型
	private BigDecimal balance; // 交易金额
	private String status; // 状态 0:创建 1：成功 2：失败
	private String inputUserType; // 入账用户类型
	private String inputAccountType; // 入账账户类型
	private String inputAccountNo; // 入账账户编号
	private String inputAccountName; // 入账账户名称
	private String outputUserType; // 出账用户类型
	private String outputAccountType; // 出账账户类型
	private String outputAccountNo; // 出账账户编号
	private String outputAccountName; // 出账账户名称
	private String remark; // 备注
	private Timestamp createTime; // 创建时间
	private Timestamp updateTime; // 修改时间
	
}
