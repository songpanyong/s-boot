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
@Table(name = "t_account_order")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class AccOrderEntity extends UUID implements Serializable {
	
	//订单状态1-成功，0-未处理，2-失败, 3-撤单,4-处理中
	public static final String ORDERSTATUS_INIT = "0"; 
	public static final String ORDERSTATUS_SUCCESS = "1"; 
	public static final String ORDERSTATUS_FAIL = "2"; 
	public static final String ORDERSTATUS_KILL = "3";
	public static final String ORDERSTATUS_DEAL = "4";
	
	/**
	* @Fields serialVersionUID : TODO
	*/
	private static final long serialVersionUID = 1060337252245115641L;
	
	private String requestNo;
	private String systemSource; //来源系统类型
	private String orderNo; //来源系统单据号
	private String userOid; //用户ID
	private String orderType; //申购:01、赎回:02、派息:03、赠送体验金:04、体验金到期:05
	private String relationProductNo; //关联产品编号
	private String relationProductName; //关联产品名称
	/**
	 * 转出产品编号
	 */
	private String outputRelationProductNo;
	
	/**
	 * 转出产品名称
	 */
	private String outputRelationProductName;
	private BigDecimal balance;	//单据金额
	/**
	 * 代金券
	 */
	private BigDecimal voucher;
	
	/**
	 * 加息券收益
	 */
	private BigDecimal rateBalance;
	
	/**
	 * 手续费
	 */
	private BigDecimal fee=BigDecimal.ZERO;

	/**
	 * 冻结金额
	 */
	private BigDecimal frozenBalance=BigDecimal.ZERO;
	
	/**
	 * 续投解冻金额，申购续投时使用
	 */
	private BigDecimal continUnfreezBalance = BigDecimal.ZERO;

	private String orderStatus; //订单状态
	private String orderDesc; //订单描述
	
	private String failDetail;//失败详情
	
	private String inputAccountNo; //入账账户，根据单据类型，做转账时用
	private String outpuptAccountNo; //出账账户，根据单据类型，做转账时用
	private Timestamp submitTime; //单据时间
	private Timestamp receiveTime; //系统接收时间
	private String businessStatus; //业务系统对账状态
	private String financeStatus; //账务系统对账状态
	private String remark; //订单描述
	private Timestamp createTime;
	private Timestamp updateTime;
	
	/**
	 * 订单关联产品类型20170410新增
	 */
	private String productType;
	/**
	 * 发行人用户Id
	 */
	private String publisherUserOid;
	private String userType;//用户类型，T1 投资人 T2发行人
	private String phone; // 手机号 
	
	private String origOrderNo;//原定单号（用于退款）
	
}
