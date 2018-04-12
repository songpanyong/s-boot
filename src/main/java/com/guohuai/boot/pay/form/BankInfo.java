package com.guohuai.boot.pay.form;

import java.io.Serializable;

import lombok.Data;

@Data
public class BankInfo implements Serializable {
	private static final long serialVersionUID = -3966810509224769756L;
	String bankName, bankCode;
}
