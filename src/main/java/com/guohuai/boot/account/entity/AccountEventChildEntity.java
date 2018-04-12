package com.guohuai.boot.account.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.guohuai.basic.component.ext.hibernate.UUID;


@Entity
@Table(name = "t_account_event_child")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class AccountEventChildEntity extends UUID implements Serializable{
	
	private static final long serialVersionUID = 6538835913628295136L;
	
	private String childEventName; // 子事件名称
	private String childEventType; // 子事件类型
	private String eventOid; // 子事件id
	private String inputUserType; // 入账用户类型
	private String inputAccountType; // 入账账户类型
	private String inputAccountNo; // 入账账户号
	private String inputAccountName; // 入账账户名称
	private String outputUserType; // 出账用户类型
	private String outputAccountType; // 出账账户类型
	private String outputAccountNo; // 出账账户号
	private String outputAccountName; // 出账账户名称
}