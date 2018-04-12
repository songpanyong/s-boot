package com.guohuai.boot.account.res;

import java.util.List;

import lombok.Data;

import com.guohuai.boot.account.entity.PlatformInfoAuditEntity;

@Data
public class PlatformInfoAuditResponse {

	private List<PlatformInfoAuditEntity> rows;
	private int page;
	private int row;
	private int totalPage;
	private long total;
}
