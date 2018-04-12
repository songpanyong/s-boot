package com.guohuai.boot.account.entity;

import com.guohuai.basic.component.ext.hibernate.UUID;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;


@Entity
@Table(name = "t_account_trans")
@lombok.Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class TransEntity extends UUID implements Serializable{
	/**
	* @Fields serialVersionUID : TODO
	*/
	
	//转账标记
	public static final String TRANSFER = "TRANSFER";
	//入账标记
	public static final String ENTERACCOUNT = "ENTERACCOUNT";
	
	
	private static final long serialVersionUID = 4140226088022063676L;
	private String accountOid; //账户号
	private String userOid; //用户ID
	private String userType; //T1--投资人账户、T2--发行人账户、T3--平台账户
	private String requestNo; //请求流水号
	private String accountOrderOid; //收单OID
	private String orderType; //申购:01、赎回:02、派息:03、赠送体验金:04、体验金到期:05
	private String systemSource; //来源系统类型\r 如 mimosaweijin
	private String orderNo; //来源系统单据号
	private String relationProductNo; //关联产品编码
	private String relationProductName; //关联产品编码
	private String direction; //金额方向，借+ 贷-
	private BigDecimal orderBalance; //订单金额
	private String ramark; //备注
	private String orderDesc; //定单描述
	private String accountName; //账户名称
	private Timestamp transTime; //交易时间
	private String dataSource; //数据来源
	private BigDecimal balance; //交易后余额
	/**
	 * 代金券
	 */
	private BigDecimal voucher;

	private String isDelete; //删除标记
	private String currency; //币种
	private String inputAccountNo; //入账账户，根据单据类型，做转账时用
	private String outpuptAccountNo; //出败账户，根据单据类型，做转账时用
	private String financeMark; //财务入账标识
	private Timestamp updateTime; //更新时间
	private Timestamp createTime; //创建时间
	private String phone; // 手机号
	private String accountType;
	/**
	 * 事件交易号
	 */
	private String transNo;
}
