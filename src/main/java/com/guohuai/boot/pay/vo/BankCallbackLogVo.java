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
    name = "t_bank_callback_log"
)
public class BankCallbackLogVo implements Serializable{
	
	private static final long serialVersionUID = 5695866689530750466L;
	@Id
	private String oid;
	private String callBackOid;
	/**
	 * 支付流水号
	 */
	private String payNo;
	/**
	 * 状态
	 */
	private String status;
	/**
	 * 银行返回代码
	 */
	private String returnCode;
	/**
	 * 银行返回错误信息
	 */
	private String returnMsg;
	private Timestamp createTime;
	private Timestamp updateTime;
	private String bankReturnSerialId;
}
