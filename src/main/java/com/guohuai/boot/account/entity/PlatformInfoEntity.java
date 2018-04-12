package com.guohuai.boot.account.entity;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.guohuai.basic.component.ext.hibernate.UUID;


@Entity
@Table(name = "t_account_platform_info")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class PlatformInfoEntity extends UUID implements Serializable{
	
	private static final long serialVersionUID = -7622345155959646873L;
	//状态  0-停用 1-启用 
	public static final String STATUS_STOP = "0";
	public static final String STATUS_START = "1";
		
	private String userOid; // 平台ID
	private String platformName; // 平台名称
	private String platformStatus; // 平台状态
	private String bindCardStatus; // 平台绑卡状态
	private Timestamp updateTime; // 更新时间
	private Timestamp createTime; // 创建时间
}
