package com.guohuai.boot.pay.form;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class InformationForm{
	  private String oid;
	  private String userOid;
	  private String bankAccontClass;
	  private String bankAccount;
	  private String bankAccountName;
	  private String accountFullName;
	  private String bankAddress;
	  private String openAccountProvince;
	  private String openAccountCity;
	  private String accountType;//基本户、一般户、收入户、支出户等
	  private String accountStatus;//可用、禁用
	  private Timestamp createTime;
	  private Timestamp updateTime;
	  private int page=1;
	  private int rows=10;

}
