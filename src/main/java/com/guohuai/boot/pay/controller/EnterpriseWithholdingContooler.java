package com.guohuai.boot.pay.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.boot.pay.dao.ElementValidationDao;
import com.guohuai.boot.pay.service.EnterpriseWithholdingService;
import com.guohuai.boot.pay.vo.ElementValidationVo;
import com.guohuai.component.common.TemplateQueryController;
import com.guohuai.settlement.api.request.ElementValidationRequest;
import com.guohuai.settlement.api.request.FindBindRequest;
import com.guohuai.settlement.api.request.OrderRequest;
import com.guohuai.settlement.api.response.ElementValidaResponse;
import com.guohuai.settlement.api.response.FindBindResponse;
import com.guohuai.settlement.api.response.OrderResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 企业代扣
 * @author zby
 *
 */
@Slf4j
@RestController
@RequestMapping(value = "/settlement/enterprise")
public class EnterpriseWithholdingContooler extends TemplateQueryController<ElementValidationVo,ElementValidationDao>{

	@Autowired
	private EnterpriseWithholdingService enterpriseWithholdingService;
	
	/**
	 * 企业绑卡（支持多张）
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/bindcard",method = RequestMethod.POST)
	public ElementValidaResponse  bindCard(@RequestBody ElementValidationRequest req) {
		log.info("企业绑卡：req={}",JSONObject.toJSON(req));
		ElementValidaResponse elementValidaResponse=new ElementValidaResponse();
		elementValidaResponse=enterpriseWithholdingService.bindCard(req); 
		log.info("企业绑卡返回：resp={}",JSONObject.toJSON(elementValidaResponse));
		return elementValidaResponse;
	}
	
	/**
	 * 企业银行卡解绑（宝付）
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/unbundling",method = RequestMethod.POST)
	public ElementValidaResponse  Unbundling(@RequestBody ElementValidationRequest req) {
		ElementValidaResponse elementValidaResponse=enterpriseWithholdingService.unbundling(req); 
		return elementValidaResponse;
	}
	
	/**
	 * 企业代扣
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/baofuWithholding",method = RequestMethod.POST)
	public OrderResponse  withoiding(@RequestBody OrderRequest req) {
		log.info("接收代扣请求 OrderRequest =[{}] ",JSONObject.toJSON(req));
		OrderResponse orderResponse = new OrderResponse();
		orderResponse=enterpriseWithholdingService.baofuWithholding(req);
		log.info("代扣返回orderResponse =[{}] ",JSONObject.toJSON(orderResponse));
		return orderResponse;
	}
	
	/**
	 * 查询已绑定的银行卡列表
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/findBindCard",method = RequestMethod.POST)
	public FindBindResponse findBindCard (@RequestBody FindBindRequest request) {
		log.info("银行卡查询请求 FindBindRequest =[{}] ",JSONObject.toJSON(request));
		FindBindResponse orderResponse = new FindBindResponse();
		orderResponse = enterpriseWithholdingService.findBindCard(request);
		log.info("银行卡查询返回FindBindRequest =[{}] ",JSONObject.toJSON(orderResponse));
		return orderResponse;
	}
}
