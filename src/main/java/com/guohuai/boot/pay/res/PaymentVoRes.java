package com.guohuai.boot.pay.res;

import java.util.List;

import com.guohuai.boot.pay.vo.PaymentVo;

import lombok.Data;

@Data
public class PaymentVoRes {

	private List<PaymentVo> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;
}
