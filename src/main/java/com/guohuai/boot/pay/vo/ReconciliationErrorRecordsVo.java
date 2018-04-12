package com.guohuai.boot.pay.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Data;

import com.guohuai.basic.component.ext.hibernate.UUID;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(
    name = "t_bank_reconciliation_error_records"
)
public class ReconciliationErrorRecordsVo extends UUID implements Serializable {
	private static final long serialVersionUID = -3286528029485346946L;

	private String orderNo;//订单号
	private String payNo;//支付流水号
	private String outsideOrderNo;//三方订单号
	private String orderType;//订单类型
	private String orderStatus;//订单状态
	private String outsideOrderStatus;//三方订单状态
	private BigDecimal amount;//订单金额
	private BigDecimal outsideAmount;//三方订单金额
	private String userOid;//用户id
	private String userName;//用户名
	private String userPhone;//用户手机号
	private String channelNo;//渠道编码
	private String channelName;//渠道名称
	private String errorType;//异常类型
	private String errorStatus;//异常处理状态
	private String errorSort;//异常排序 0等人功处理，1已处理
	private String errorResult;//异常处理结果
	private String applyRecord;//是否有申购记录，Y有N无
	private Timestamp orderTime;//订单时间
	private Timestamp createTime;//创建时间
	private Timestamp updateTime;//更新时间
	private String remark;//备注
	private String memberId;//商户号

}
