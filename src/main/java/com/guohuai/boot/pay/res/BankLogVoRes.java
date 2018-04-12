package com.guohuai.boot.pay.res;

import java.util.List;

import com.guohuai.boot.pay.vo.BankLogVo;

import lombok.Data;

@Data
public class BankLogVoRes {

	private List<BankLogVo> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;
}
