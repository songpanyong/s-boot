package com.guohuai.boot.pay.vo;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.guohuai.basic.component.ext.hibernate.UUID;

@Data
@Entity
@Table(
    name = "t_bank_reconciliation_statistics"
)
@EqualsAndHashCode(callSuper=false)
public class ReconciliationStatisticsVo extends UUID implements Serializable {
	private static final long serialVersionUID = -3286528029485346945L;
	
	private String channelNo;//渠道编码
	private String channelName;//渠道名称
	private int systemCount;//系统总笔数
	private int outsideCount;//三方总笔数
	private int errorCount;//对账异常总笔数
	private String systemAmount;//系统总金额
	private String outsideAmount;//三方总金额
	private String errorAmount;//对账异常总金额
	private String reconciliationStatus;//对账状态，1：确认完成对账，0：未确认完成对账
	private Timestamp confirmDate;//确认完成对账时间
	private Timestamp reconciliationDate;//对账操作时间
	private Timestamp outsideDate;//三方支付单清算日

}
