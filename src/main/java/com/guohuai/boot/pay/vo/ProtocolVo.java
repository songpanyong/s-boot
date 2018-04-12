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
    name = "t_bank_protocol"
)
public class ProtocolVo implements Serializable{
	private static final long serialVersionUID = -3286518029485346965L;
	//1：绑卡,2：解绑
	public static final String STATUS_BIND = "1";
	public static final String STATUS_UNBIND = "2";
	@Id
	private String oid;
	private String userOid;
	private String accountBankType;//银行卡开户行类型,1:储蓄卡
	private String cardNo;//银行卡号',
	private String certificateNo;//证件号
	private String realName;//客户姓名	
	private String bankName;//银行名称
	private String protocolNo;//代扣协议编号',
	private String bankTypeCode;//银行类别编码  (快付通内部编码单独使用）
	private String phone;
	private Timestamp createTime;
	private Timestamp updateTime;
	private String status;//1：绑卡,2：解绑
    private String cardOrderId;
    private String smsCode;
    private String orderNo;
    
    //---------------------------------------zby新加字段
    /**
	   * 开户行所属省份
	   */
	  private String province;
	  
	  /**
	   * 开户行所属城市
	   */
	  private String city;
	  
	  /**
	   * 开户行所属省份
	   */
	  private String branch;
	  
	  /**
	   * 开户行所属区、县
	   */
	  private String county;
	  
	  /**
	   * 绑卡类型
	   */
	  private String cardType;
	  
	  /**
	   * 证件类型
	   */
	  private String certificates;
	  
	  /**
	   * 用户类型
	   */
	  private String userType;
	  /**
	   * 是否为实名认证 Y N
	   */
	  private String authenticationStatus;
}
