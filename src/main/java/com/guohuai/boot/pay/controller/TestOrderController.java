package com.guohuai.boot.pay.controller;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.boot.pay.dao.ComOrderDao;
import com.guohuai.boot.pay.service.ComOrderService;
import com.guohuai.boot.pay.service.ReconciliationRechargeService;
import com.guohuai.boot.pay.service.ReconciliationWithdrawalsService;
import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.component.common.TemplateQueryController;
import com.guohuai.settlement.api.SettlementSdk;
import com.guohuai.settlement.api.request.ElementValidationRequest;
import com.guohuai.settlement.api.request.OrderRequest;
import com.guohuai.settlement.api.response.ElementValidaResponse;
import com.guohuai.settlement.api.response.OrderResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName: ComOrderController
 * @Description: 申购、赎回定单
 * @author xueyunlong
 * @date 2016年11月28日 下午3:21:26
 *
 */
@Slf4j
@RestController
@RequestMapping(value = "/settlement/test")
public class TestOrderController extends TemplateQueryController<OrderVo,ComOrderDao>{

	@Autowired
	private ComOrderService orderService;
	
	@Autowired
	private SeqGenerator seqGenerator;
	
	@Autowired
	private ReconciliationRechargeService reconciliationRechargeService;
	
	@Autowired
	private ReconciliationWithdrawalsService reconciliationWithdrawalsService;
	
	@RequestMapping(value = "/reconciliationRecharge", method = {RequestMethod.POST,RequestMethod.GET})
	public OrderResponse  reconciliationRecharge(String orderNo ,String externalState,String systemState) {
		OrderResponse orderResponse=reconciliationRechargeService.rechargeRecon(orderNo, externalState, systemState);
		log.info("reconciliationRecharge:{}",JSONObject.toJSON(orderResponse));
		return orderResponse;
	}
	
	@RequestMapping(value = "/reconciliationWithd", method = {RequestMethod.POST,RequestMethod.GET})
	public OrderResponse  reconciliationWithd(String orderNo ,String externalState,String systemState) {
		OrderResponse orderResponse=reconciliationWithdrawalsService.withdrawalsRecon(orderNo, externalState, systemState);
		log.info("reconciliationWithd:{}",JSONObject.toJSON(orderResponse));
		return orderResponse;
	}
	
	/**
	 * 付款
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/pay", method = {RequestMethod.POST,RequestMethod.GET})
	public OrderResponse  pay(String userId ,String type) {
		log.info("userId===={}",userId);
		long time=System.currentTimeMillis();
		OrderRequest orderRequest=new OrderRequest();
		orderRequest.setUserOid(userId);
		orderRequest.setOrderNo(seqGenerator.next("y"));
		orderRequest.setAmount(new BigDecimal(1));
		orderRequest.setRemark("压测交易");
		orderRequest.setRequestNo(time+"");
		orderRequest.setSystemSource("mimosa");
		orderRequest.setType(type);
		orderRequest.setDescribe("定单描述");
		OrderResponse orderResponse=orderService.trade(orderRequest); 
		return orderResponse;
	}
	
	@SuppressWarnings("unused")
	@RequestMapping(value = "/elementadd", method = {RequestMethod.POST,RequestMethod.GET})
	public OrderResponse  elementadd(int start ,int end) {
	SettlementSdk sdk = new SettlementSdk("http://127.0.0.1");
	for(int i=start;i<end;i++){
	ElementValidationRequest elementValidationRequest = new ElementValidationRequest();
	elementValidationRequest.setSystemSource("mimosa");
	BigDecimal userid=new BigDecimal("19000000000").add(new BigDecimal(i));
	elementValidationRequest.setUserOid(userid.toString()+"");
	String idCardName = "张三"+i;
	String idCard = "120101190"+i;
	BigDecimal bankCardNum=new BigDecimal("6227002118106218181").add(new BigDecimal(i));
	String mobile = userid+"";
	
	elementValidationRequest.setRealName(idCardName);
	elementValidationRequest.setCardNo(bankCardNum+"");
	elementValidationRequest.setPhone(mobile);
	elementValidationRequest.setCertificateNo(idCard);
	ElementValidaResponse elementValidaResponse= sdk.elementValid(elementValidationRequest);
	}
	return null;
	}

}
