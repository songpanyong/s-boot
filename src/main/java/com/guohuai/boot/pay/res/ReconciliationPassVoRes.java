package com.guohuai.boot.pay.res;

import java.util.List;

import lombok.Data;

import com.guohuai.boot.pay.vo.ReconciliationPassVo;

@Data
public class ReconciliationPassVoRes {

	private List<ReconciliationPassVo> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;
}
