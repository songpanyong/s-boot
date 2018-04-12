package com.guohuai.boot.pay.res;

import java.util.List;

import lombok.Data;

import com.guohuai.boot.pay.vo.ReconciliationErrorRecordsVo;

@Data
public class ReconciliationErrorRecordsVoRes {

	private List<ReconciliationErrorRecordsVo> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;
}
