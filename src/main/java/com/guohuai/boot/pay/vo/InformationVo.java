package com.guohuai.boot.pay.vo;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(
    name = "t_bank_information"
)
public class InformationVo implements Serializable{
	  private static final long serialVersionUID = -3286518029485346965L;
	  @Id
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
}
