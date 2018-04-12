package com.guohuai;

import com.guohuai.settlement.api.SettlementSdk;
import com.guohuai.settlement.api.request.ElementValidationRequest;
import com.guohuai.settlement.api.response.ElementValidaResponse;

public class OrderTest {
@SuppressWarnings("unused")
public static void main(String[] args) {
//	OrderSdk orderSdk=new OrderSdk("http://127.0.0.1:8080");
//	OrderRequest orderRequest=new OrderRequest();
//	orderRequest.setMoney("1");
//	orderRequest.setOrderOid("111");
//	orderSdk.apply(orderRequest);
	SettlementSdk sdk =new SettlementSdk("http://127.0.0.1:8080");
	ElementValidationRequest elementValidationRequest =new ElementValidationRequest();
	elementValidationRequest.setBankCode("bankName");
	ElementValidaResponse elementValidaResponse=sdk.elementValid(elementValidationRequest);
}
}
