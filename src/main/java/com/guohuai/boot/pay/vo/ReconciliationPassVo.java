package com.guohuai.boot.pay.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.guohuai.basic.component.ext.hibernate.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(
    name = "t_bank_reconciliation_pass"
)
@EqualsAndHashCode(callSuper=false)
public class ReconciliationPassVo extends UUID implements Serializable {
	private static final long serialVersionUID = -3286518029485346965L;
	
	private String channelId;//快付通，为以后扩展预留
	private String productId;//快付通用
	private String orderId;//快付通用（用于和支付订单关联对账）
	private String transactionCurrency;
	private BigDecimal transactionAmount;
	private String paymentBankNo;
	private String beneficiaryBankNo;
	private String tradStatus;
	private String failDetail;
	private String errorCode;
	private Timestamp transactionTime;
	private Date accountDate;
	private String checkMark;//用于记录有没有对账成功
	private String userOid;
	private Timestamp createTime;
	private Timestamp updateTime;
	private int reconStatus=0;//未对账：0；对账成功：1；对账失败：2
	private BigDecimal fee;
	private String outsideOrderNo;//三方订单号
	private String repairStatus;//修复状态Y已修复N未修复
	private String memberId;//商户号

}
