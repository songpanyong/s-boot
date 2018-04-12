package com.guohuai.boot.account.form;

import lombok.Data;

@Data
public class AccountEventForm {
	
	private String oid;
	private String eventName; // 事件名称
	private String eventType; // 事件类型01充值02提现03转账
	private String userOid; // 平台Oid
	private String setUpStatus; // 设置状态 0已生效1生效中
	private int page = 1;
    private int rows = 10;
	
}
