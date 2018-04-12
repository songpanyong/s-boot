package com.guohuai.boot.account.res;

import lombok.Data;

@Data
public class AccountEventEffectInfoResponse {

	private String effectiveTimeType; //生效时间类型 01即时,02次日,03次月
	private String setUpStatus; //设置状态：0已生效，1生效中， 2审核中
	private String setUpTime; // 设置时间
	
}
