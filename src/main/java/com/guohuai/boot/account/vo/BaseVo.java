package com.guohuai.boot.account.vo;

import java.io.Serializable;

import lombok.Data;

@Data
public abstract class BaseVo implements Serializable {

	private static final long serialVersionUID = 6786755781L;
	private String returnCode;
	private String errorMessage;
	
}
