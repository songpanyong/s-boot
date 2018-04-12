package com.guohuai.boot.pay.res;

import java.util.List;

import lombok.Data;

import com.guohuai.boot.pay.vo.BankCallbackVo;

@Data
public class BankCallbackVoRes {

	private List<BankCallbackVo> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;
}
