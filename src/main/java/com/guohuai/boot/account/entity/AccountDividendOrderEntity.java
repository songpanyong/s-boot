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
@Table(name = "t_account_dividend_order")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class AccountDividendOrderEntity extends UUID implements Serializable {

	// 订单、派息状态1-处理成功，0-未处理，2-处理失败，3-受理失败
	public static final String ORDERSTATUS_INIT = "0";
	public static final String ORDERSTATUS_SUCCESS = "1";
	public static final String ORDERSTATUS_FAIL = "2";
	public static final String ORDERSTATUS_SAVE_FAIL = "3";

	/**
	 * @Fields serialVersionUID : TODO
	 */
	private static final long serialVersionUID = 1060337252322451141L;

	private String requestNo;
	private String systemSource; // 来源系统类型
	private String orderNo; // 来源系统单据号
	private String userOid; // 用户ID
	private String userType; // 用户类型  投资人账户:T1、发行人账户:T2、平台账户:T3  
	private String orderType; // 申购:01、赎回:02、派息:03、赠送体验金:04、体验金到期:05
	private String productType; // 产品类型
	private String relationProductNo; // 关联产品编号
	private String relationProductName; // 关联产品名称
	/**
	 * 业务系统订单创建时间（用于业务和账户对账）
	 */
	private String orderCreatTime;
	/**
	 * 转出产品编号
	 */
	private String outputRelationProductNo;

	/**
	 * 转出产品名称
	 */
	private String outputRelationProductName;
	private BigDecimal balance; // 单据金额
	/**
	 * 代金券
	 */
	private BigDecimal voucher;

	private String orderStatus; // 订单状态
	private String dividendStatus; // 派息状态
	private String orderDesc; // 订单描述
	private String inputAccountNo; // 入账账户，根据单据类型，做转账时用
	private String outpuptAccountNo; // 出账账户，根据单据类型，做转账时用
	private Timestamp submitTime; // 单据时间
	private Timestamp receiveTime; // 系统接收时间
	private String businessStatus; // 业务系统对账状态
	private String financeStatus; // 账务系统对账状态
	private String remark; // 订单描述
	private Timestamp createTime;
	private Timestamp updateTime;
}
