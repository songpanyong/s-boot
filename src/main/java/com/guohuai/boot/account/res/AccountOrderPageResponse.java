package com.guohuai.boot.account.res;

import java.util.List;

import com.guohuai.boot.account.entity.AccOrderEntity;

import lombok.Data;

/**
 * 订单分页
 * @author mr_gu
 *
 */
@Data
public class AccountOrderPageResponse {

	private List<AccOrderEntity> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;
	
}
