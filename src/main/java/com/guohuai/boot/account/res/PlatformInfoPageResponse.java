package com.guohuai.boot.account.res;

import java.util.List;

import lombok.Data;

import com.guohuai.boot.account.entity.PlatformInfoEntity;

@Data
public class PlatformInfoPageResponse {

	private List<PlatformInfoEntity> rows; 
	private int page;
	private int row;
	private int totalPage;
	private long total;
	
}
