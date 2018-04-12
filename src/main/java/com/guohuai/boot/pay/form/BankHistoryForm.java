package com.guohuai.boot.pay.form;

import java.math.BigDecimal;
import java.sql.Timestamp;

import lombok.Data;

@Data
public class BankHistoryForm{
	  private String oid;
	  private String bankType;//用于以后扩展多行时用
	  private String accountNo;
	  private String currency;
	  private String tradTime;
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
	  private int page=1;
	  private int rows=10;
	  private int reconStatus;
	  private String tradStatus;

}
