package com.guohuai.boot.pay.form;

import java.math.BigDecimal;
import java.sql.Timestamp;

import lombok.Data;

@Data
public class BankLogForm{
	  private String oid;
	  private String userOid;
	  private String sheetId;//银行指令流水号
	  private String merchantId;//根据业务系统订单产生一个支付订单号（防止多个业务系统时数据重复）（商户订单号用于快付通对账）平安银行要交流凭证号，业务系统产生，唯一（标示交易唯一性，同一客户上送的不可重复，建议格式：yyyymmddHHSS+8位系列
	  private Timestamp operatorTime;
	  private String operatorType;//发送、查询结果、修改状态
	  private String operatorName;
	  private String operatorId;//系统自动发记录为自动，人工记录为当前登录人
	  private String remark;
	  private String bankReturnContent;//支付或是查询状态时银行返回的内容
	  private String tradStatus;//快付通取交易状态，平安银行取银行处理结果字段，快付通取交易状态字段
	  private String failDetail;//快付通或平安银行返回的失败原因
	  private String errorCode;
	  private String bankSerialNumber;//平安银行，用于记录银行返回报文中的流水号
	  private BigDecimal fee=BigDecimal.ZERO;//平安银行，反回手续费
	  private String bankReturnTicket;
	  private String bankReturnSerialId;
	  private String checkMark;
	  private Timestamp createTime;
	  private Timestamp updateTime;
	  private String requestNo;
	  private int page=1;
	  private int rows=10;
	  private String beginTime;
	  private String endTime;
}
