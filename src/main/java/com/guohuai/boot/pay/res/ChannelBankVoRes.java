package com.guohuai.boot.pay.res;

import java.util.List;

import com.guohuai.boot.pay.vo.ChannelBankVo;

import lombok.Data;

@Data
public class ChannelBankVoRes {

	private List<ChannelBankVo> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;
}
