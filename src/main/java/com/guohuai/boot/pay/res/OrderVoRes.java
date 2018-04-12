package com.guohuai.boot.pay.res;

import java.math.BigDecimal;
import java.util.List;

import com.guohuai.boot.pay.vo.OrderVo;

import lombok.Data;

@Data
public class OrderVoRes {

	private List<OrderVo> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;
	private BigDecimal totalAmount;//总金额
	private long totalAccount;//总人数
}
