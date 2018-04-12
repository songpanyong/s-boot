package com.guohuai.boot.account.entity;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.guohuai.basic.component.ext.hibernate.UUID;


@Entity
@Table(name = "t_account_fail_order_notify")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class AccFailOrderNotifyEntity extends UUID implements Serializable {
	
	private static final long serialVersionUID = 1060337252245115641L;
	
	private String orderNo; //来源系统单据号
	private String userOid; //用户ID
	private String orderStatus; //订单状态
	private String orderDesc; //订单描述
	private Timestamp receiveTime; //系统接收时间
	private String notified; //是否已通知
	
}
