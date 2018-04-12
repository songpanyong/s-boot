package com.guohuai.boot.pay.res;

import java.util.List;

import com.guohuai.boot.pay.vo.ElementValidationVo;

import lombok.Data;

@Data
public class ElementValidationRes {

	private List<ElementValidationVo> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;
}
