package com.guohuai.boot.pay.form;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

import lombok.Data;

@Data
public class ReconciliationMeteForm{
	  private String oid;
	  private String sourceSystem;
	  private Date date;
	  private String orderId;
	  private BigDecimal amount;
	  private String status;
	  private String reconciliationStatus;
	  private String userOid;
	  private Timestamp createTime;
	  private Timestamp updateTime;
}
