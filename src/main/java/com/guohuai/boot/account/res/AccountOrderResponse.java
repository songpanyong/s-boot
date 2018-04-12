package com.guohuai.boot.account.res;

import java.math.BigDecimal;
import java.sql.Timestamp;

import com.guohuai.account.api.response.BaseResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class AccountOrderResponse extends BaseResponse {
	/**
	 * 
	 */
	private static final long serialVersionUID = 793382618520726745L;

	private String userOid; //用户ID
	//用户名称	用户账号	用户类型
	private String userName;
	private String phone;
	private String userType;
	
	private String requestNo; //请求流水号
	private String systemSource; //来源系统类型
	private String orderNo; //来源系统单据号
	private String orderType; //申购:01、赎回:02、派息:03、赠送体验金:04、体验金到期:05
	private Timestamp submitTime; //单据时间
	private String remark; //订单描述
	private String failDetail;//失败详情
	private String orderStatus; //订单状态
	
	/**
	 * 赎回时此字段为订单中金额（包括还本付息金额和加息券收益金额）
	 */
	private BigDecimal balance = BigDecimal.ZERO;	//单据金额-这里是用户实付金额
	/**
	 * 申购订单金额（实付金额balance+代金券金额voucher）
	 */
	private BigDecimal investOrderBalance = BigDecimal.ZERO;
	
	//申购、	快申、快赎、赎回、转换-赎回、转换-申购、还本付息、清盘赎回、募集失败退款、现金分红
	private String relationProductNo; //关联产品编号
	private String relationProductName; //关联产品名称
	
	/**
	 * 手续费: 申购、快申、快赎、赎回、转换-赎回、转换-申购、还本付息、清盘赎回
	 */
	private BigDecimal fee=BigDecimal.ZERO;
	/**
	 * 代金券: 申购、快申、募集失败退款
	 */
	private BigDecimal voucher = BigDecimal.ZERO;
	/**
	 * 加息券收益:快赎、赎回、转换-赎回、还本付息、清盘赎回
	 */
	private BigDecimal rateBalance = BigDecimal.ZERO;
	
	/**
	 * 续投解冻金额，申购续投时使用
	 */
	private BigDecimal continUnfreezBalance = BigDecimal.ZERO;
	
	
	
	
	/**
	 * 转出产品编号
	 */
	private String outputRelationProductNo;
	
	/**
	 * 转出产品名称
	 */
	private String outputRelationProductName;
	
	/**
	 * 冻结金额
	 */
	private BigDecimal frozenBalance=BigDecimal.ZERO;
	//	private String orderDesc; //订单描述
	private String inputAccountNo; //入账账户，根据单据类型，做转账时用
	private String outpuptAccountNo; //出账账户，根据单据类型，做转账时用
	private Timestamp receiveTime; //系统接收时间
	private String businessStatus; //业务系统对账状态
	private String financeStatus; //账务系统对账状态
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
	
	private String origOrderNo;//原定单号（用于退款）
	
}
