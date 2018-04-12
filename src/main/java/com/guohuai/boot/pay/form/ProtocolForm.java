package com.guohuai.boot.pay.form;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class ProtocolForm{
	private String oid;
	private String userOid;
	private String accountBankType;//银行卡开户行类型',
	private String cardNo;//银行卡号',
	private String protocolNo;//代扣协议编号',
	private Timestamp createTime;
	private Timestamp updateTime;
	private int page=1;
	private int rows=10;

}
