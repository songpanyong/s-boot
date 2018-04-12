package com.guohuai.boot.account.entity;


import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.guohuai.basic.component.ext.hibernate.UUID;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "t_account_recon")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class ReconEntity extends UUID implements Serializable {

	/**
	* @Fields serialVersionUID : TODO
	*/
	private static final long serialVersionUID = -8739737306509596475L;
	private String systemSource; //来源系统类型
	private String orderNo; //来源系统订单号
	private String balance; //订单金额
	private String status; //订单状态
	private Timestamp updateTime;
	private Timestamp createTime;
}
