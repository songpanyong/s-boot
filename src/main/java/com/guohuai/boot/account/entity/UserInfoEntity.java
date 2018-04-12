package com.guohuai.boot.account.entity;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.guohuai.basic.component.ext.hibernate.UUID;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "t_account_userinfo")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class UserInfoEntity extends UUID implements Serializable{
	
	/**
	* @Fields serialVersionUID : TODO
	*/
	private static final long serialVersionUID = 2717986719900977090L;
	
	/**
	 * T1--投资人账户、T2--发行人账户、T3--平台账户
	 */
	private String userType; 
	private String systemUid; // 用户业务系统ID
	private String userOid; // 用户ID
	private String systemSource; // 来源系统
	private String name; // 姓名
	private String idCard; // 身份证号
	private String bankName; // 开户行
	private String cardNo; // 银行账号
	private String password; // 支付密码
	private String phone; // 手机号
	private String remark; // 备注
	private Timestamp updateTime; // 更新时间
	private Timestamp createTime; // 创建时间
}
