package com.guohuai.boot.pay.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.guohuai.basic.component.ext.hibernate.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "t_bank_order")
@EqualsAndHashCode(callSuper=false)
public class OrderVo extends UUID implements Serializable {
	private static final long serialVersionUID = -8067578394143480757L;
	private String userOid;// 用户ID
	private String orderNo;// 订单号
	private String status;// 单据状态
	private Timestamp receiveTime;
	private String type;// 扣:01 付:02
	private BigDecimal amount;
	private String bankCode;
	private String cardNo;
	private String realName;
	private String describ;
	private String remark;
	private String channel;
	private String failDetail;
	private String settlementStatus;// 成功，失败，进行中（冗余）
	private String businessStatus;// 跟业务系统对账状态
	private int reconStatus=0;// 跟银行流水对账状态（冗余）
	private Timestamp createTime;
	private Timestamp updateTime;
	private String requestNo;
	private String systemSource;
	private BigDecimal fee = BigDecimal.ZERO;
	private String payNo;//支付流水号 
	private String bankReturnSeriNo;//银行返回流水号
	private String returnCode;
	private String userType;//用户类型，T1 投资人 T2发行人
	private String phone; // 手机号
	private String memberId;//商户号
	private String auditStatus="0";//审核状态，0无需审核、1待审核、2审核通过、3驳回
	private String operatorId;//操作员ID
	private String operatorName;//操作员名称
	private String auditRemark;//审核备注
	
}
