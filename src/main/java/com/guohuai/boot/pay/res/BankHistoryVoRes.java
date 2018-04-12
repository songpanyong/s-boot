package com.guohuai.boot.pay.res;

import java.util.List;

import com.guohuai.boot.pay.vo.BankHistoryVo;

import lombok.Data;

@Data
public class BankHistoryVoRes {
	
	private List< BankHistoryVo> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;

}
