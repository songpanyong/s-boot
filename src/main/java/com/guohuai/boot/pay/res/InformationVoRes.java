package com.guohuai.boot.pay.res;

import java.util.List;

import com.guohuai.boot.pay.vo.InformationVo;

import lombok.Data;

@Data
public class InformationVoRes {

	private List< InformationVo> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;
}
