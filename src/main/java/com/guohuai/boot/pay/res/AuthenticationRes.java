package com.guohuai.boot.pay.res;

import java.util.List;

import com.guohuai.boot.pay.vo.AuthenticationVo;

import lombok.Data;

@Data
public class AuthenticationRes {

	private List<AuthenticationVo> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;
}
