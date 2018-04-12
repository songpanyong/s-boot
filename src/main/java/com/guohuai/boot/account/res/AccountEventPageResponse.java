package com.guohuai.boot.account.res;

import java.util.List;

import lombok.Data;

import com.guohuai.boot.account.entity.AccountEventEntity;

@Data
public class AccountEventPageResponse {

	private List<AccountEventEntity> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;
	
}
