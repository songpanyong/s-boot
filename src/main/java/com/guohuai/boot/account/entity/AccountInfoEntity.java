package com.guohuai.boot.account.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.guohuai.basic.component.ext.hibernate.UUID;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "t_account_info")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class AccountInfoEntity extends UUID implements Serializable {
	
	//冻结状态：NORMAL为正常，FROZEN为冻结，FROZENAU为冻结审批中，THAWAU为解冻审批中
	public static final String FROZENSTATUS_NORMAL = "NORMAL";
	public static final String FROZENSTATUS_FROZEN = "FROZEN";
	public static final String FROZENSTATUS_AUDI = "FROZENAU";
	public static final String FROZENSTATUS_THAW = "THAWAU";
	
	//STATUS枚举：SAVE：保存，SUBMIT：提交，VALID：生效，SEALING：封存中，SEALED：封存，DELETE：删除
	public static final String STATUS_SAVE = "SAVE";
	public static final String STATUS_SUBMIT = "SUBMIT";
	public static final String STATUS_VALID = "VALID";
	public static final String STATUS_SEALING = "SEALING";
	public static final String STATUS_SEALED = "SEALED";
	public static final String STATUS_DELETE = "DELETE";
	
	//auditStatus枚举：NOCOMMIT：未提交，SUBMIT：提交，PASS：通过，REJECT：驳回
	public static final String AUDIT_STATUS_NOCOMMIT = "NOCOMMIT";
	public static final String AUDIT_STATUS_SUBMIT = "SUBMIT";
	public static final String AUDIT_STATUS_PASS = "PASS";// 通过
	public static final String AUDIT_STATUS_REJECT = "REJECT";// 驳回
	
	/**
	* @Fields serialVersionUID : TODO
	*/
	private static final long serialVersionUID = -922483069560508853L;
	/**
	 * 账户号
	 */
	private String accountNo; 
	/**
	 * 用户ID
	 */
	private String userOid;
	/**
	 * T1--投资人账户、T2--发行人账户、T3--平台账户
	 */
	private String userType; 
	/**
	 * 01--活期，02--活期利息，03--体验金，04--在途，05--提现冻结户
	 */
	private String accountType;
	/**
	 * 关联产品
	 */
	private String relationProduct;
	/**
	 * 关联产品名称
	 */
	private String relationProductName;
	/**
	 * 账户名称
	 */
	private String accountName;
	/**
	 * 开户时间
	 */
	private Timestamp openTime;
	/**
	 * 开户人
	 */
	private String openOperator;
	/**
	 * 账户余额
	 */
	private BigDecimal balance;
	/**
	 * SAVE：保存，SUBMIT：提交，VALID：生效，SEALING：封存中，SEALED：封存，DELETE：删除
	 */
	private String status;
	/**
	 * NORMAL为正常，FROZEN为冻结，FROZENAU为冻结审批中，THAWAU为解冻审批中
	 */
	private String frozenStatus;
	/**
	 * 备注
	 */
	private String remark;
	private Timestamp updateTime;
	private Timestamp createTime;
	/**
	 * 审核状态
	 */
	private String auditStatus;
	/**
	 * 授信额度
	 */
	private BigDecimal lineOfCredit;
}
