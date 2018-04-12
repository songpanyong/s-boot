package com.guohuai.boot.account.res;

import java.util.List;

import lombok.Data;

import com.guohuai.boot.account.entity.AccountBindCardAuditEntity;

@Data
public class AccountBindCardAuditPageResponse {

	private List<AccountBindCardAuditEntity> rows; 
	private int page;
	private int row;
	private int totalPage;
	private long total;
	
}
