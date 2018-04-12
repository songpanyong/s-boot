package com.guohuai.boot.pay.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.guohuai.basic.component.ext.web.BaseController;
import com.guohuai.boot.pay.service.TransactionService;
import com.guohuai.settlement.api.request.TransactionRequest;
import com.guohuai.settlement.api.response.TransactionResponse;


@RestController
@RequestMapping(value = "/settlement/reconciliation")
public class TransactionController extends BaseController{

	@Autowired
	private TransactionService reconciliationService;

	@RequestMapping(value = "/queryOrder",method =RequestMethod.POST)
	public TransactionResponse  queryOrder(@RequestBody TransactionRequest req) {
		TransactionResponse reconciliationResponse=reconciliationService.queryOrder(req); 
		return reconciliationResponse;
	}
	
}
