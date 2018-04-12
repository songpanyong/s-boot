package com.guohuai.boot.pay.vo;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(
    name = "t_bank_reconciliation_records"
)
@EqualsAndHashCode(callSuper=false)
public class ReconciliationRecordsVo implements Serializable{
	   private static final long serialVersionUID = -6004709475834256278L;
	   @Id
	   private String oid;
	   private String userOid;
	   private String reconDate;//对账日期YYYYMMDD
	   private String reconStatus;//'0：对账失败 1：已对帐'
	   private String failDetial;//失败详情
	   private Timestamp createTime;
	   private Timestamp updateTime;
	   private String channelId;//渠道id 新增 

}
