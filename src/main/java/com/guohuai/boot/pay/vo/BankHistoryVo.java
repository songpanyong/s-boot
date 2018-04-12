package com.guohuai.boot.pay.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(
    name = "t_bank_history"
)
public class BankHistoryVo implements Serializable{
	  private static final long serialVersionUID = -3286518029485346965L;
	  @Id
	  private String oid;
	  private String bankType;//用于以后扩展多行时用
	  private String accountNo;
	  private String currency;
	  private Timestamp tradTime;
	  private String hostSerial;//可能用于对账
	  private String transactionFlow;
	  private String paymentPartyNetwork;
	  private String paymentallianceCode;
	  private String paymentName;
	  private String paymentPartyAccount;
	  private String paymentAccount;
	  private String settlementCurrency;
	  private BigDecimal tradAmount;
	  private String receivParty;
	  private String payeeContact;
	  private String beneficiaryBankName;
	  private String beneficiaryAccount;
	  private String payeeName;
	  private String lendMark;
	  private String stract;
	  private String voucher;//可能用于对账
	  private BigDecimal fee;
	  private BigDecimal postFee;
	  private BigDecimal accountBalance;
	  private String postscript;
	  private String chineseAbstract;
	  private String customerCustom;//用于对账，支付时上传的凭证号
	  private String reconciliationMark;
	  private String userOid;  
	  private Timestamp createTime;
	  private Timestamp updateTime;
	  private Integer reconStatus;//对账结果，默认为未对账:0，对账结果有对账成功:1;和对账失败:2
	  private String tradStatus;
}
