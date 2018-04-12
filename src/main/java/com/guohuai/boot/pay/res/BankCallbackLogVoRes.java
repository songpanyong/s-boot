package com.guohuai.boot.pay.res;

import java.util.List;

import lombok.Data;

import com.guohuai.boot.pay.vo.BankCallbackLogVo;

@Data
public class BankCallbackLogVoRes {

	private List<BankCallbackLogVo> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;
}
