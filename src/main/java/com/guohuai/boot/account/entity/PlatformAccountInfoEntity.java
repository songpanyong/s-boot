package com.guohuai.boot.account.entity;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.guohuai.basic.component.ext.hibernate.UUID;


@Entity
@Table(name = "t_account_platform_account_info")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class PlatformAccountInfoEntity extends UUID implements Serializable{
	
	private static final long serialVersionUID = -6845837628716384362L;
	//状态  0-停用 1-启用
	public static final String STATUS_STOP = "0";
	public static final String STATUS_RUN = "1";
	
	private String userOid; // 平台ID
	private String accountNo; // 账户号
	private String accountType; // 账户类型
	private String userType; // 用户类型
	private String accountName; // 账户名称
	private String accountStatus; // 账户状态
	private String settleStatus; // 设置状态  0不可设置1可设置
	private Timestamp updateTime; // 更新时间
	private Timestamp createTime; // 创建时间
}
