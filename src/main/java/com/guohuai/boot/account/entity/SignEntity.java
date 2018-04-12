package com.guohuai.boot.account.entity;


import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.guohuai.basic.component.ext.hibernate.UUID;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "t_account_sign")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class SignEntity extends UUID implements Serializable{
	
	//签约状态  0-已签约 1-已解约
	public static final String STATUS_SIGN = "0";
	public static final String STATUS_SURRENDER = "1";
	
	/**
	* @Fields serialVersionUID : TODO
	*/
	private static final long serialVersionUID = -9142925618360157430L;
	private String realName; // 用户名
	private String phone; // 手机号
	private String identityNo; // 身份证号
	private String protocolNo; // 协议号
	private String status; // 签约状态 0-已签约 1-已解约
	private String userOid; // 用户编号
	private String bankCard; // 银行卡号
	private String bankGroup; // 银行组号
	private String cardType;
	private Timestamp createTime; // 创建时间
	private Timestamp updateTime; // 修改时间
	private String busiTag; // 业务标签
}
