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
    name = "t_bank_callback"
)
public class BankCallbackVo implements Serializable{/**
	* @Fields serialVersionUID : TODO
	*/
	private static final long serialVersionUID = 1165252353189787757L;
	@Id
	private String oid;
	private String orderNO;
	/**
	 * 渠道
	 */
	private String channelNo;
	/**
	 * 交易类别
	 */
	private String tradeType;
	/**
	 * 分钟
	 */
	private String minute;
	/**
	 * 回调发起时间
	 */
	private Timestamp callbackDate;
	/**
	 * 回调次数
	 */
	private Integer count;
	/**
	 * 设置回调最大次数
	 */
	private Integer totalCount;
	/**
	 * 
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
	private String payNo;
	private String type;
}
