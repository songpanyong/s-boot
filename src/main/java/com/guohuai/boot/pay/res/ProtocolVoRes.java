package com.guohuai.boot.pay.res;

import java.util.List;

import com.guohuai.boot.pay.vo.ProtocolVo;

import lombok.Data;

@Data
public class ProtocolVoRes {

	private List<ProtocolVo> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;
}
