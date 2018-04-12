package com.guohuai.boot.pay.res;

import java.util.List;

import com.guohuai.boot.pay.vo.ChannelVo;

import lombok.Data;

@Data
public class ChannelVoRes {

	private List<ChannelVo> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;
}
