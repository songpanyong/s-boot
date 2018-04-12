package com.guohuai.boot.pay.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.component.ext.web.Response;
import com.guohuai.boot.pay.dao.ComChannelDao;
import com.guohuai.boot.pay.form.ChannelForm;
import com.guohuai.boot.pay.res.ChannelVoRes;
import com.guohuai.boot.pay.service.ComChannelService;
import com.guohuai.boot.pay.vo.ChannelVo;
import com.guohuai.component.common.TemplateQueryController;
import com.guohuai.settlement.api.request.BankChannelRequest;
import com.guohuai.settlement.api.response.BankChannelResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @ClassName: ComChannelController
 * @Description: 渠道配置管理
 * @author xueyunlong
 * @date 2016年11月28日 下午3:21:11
 *
 */
@Slf4j
@RestController
@RequestMapping(value = "/settlement/channel")
public class ComChannelController extends TemplateQueryController<ChannelVo,ComChannelDao>{

	@Autowired
	private ComChannelService comChannelService;

	@RequestMapping(value = "/save", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> save(@Valid ChannelForm req) {
//		String operator=this.getLoginAdmin();
//		req.setUserOid(operator);
		comChannelService.save(req);
		Response r = new Response();
		r.with("result","SUCCESS");
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/update", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> update(@Valid ChannelForm req) {
//		String operator=this.getLoginAdmin();
//		req.setUserOid(operator);
		comChannelService.update(req);
		Response r = new Response();
		r.with("result","SUCCESS");
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}

	@RequestMapping(value = "/page", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<ChannelVoRes> page(ChannelForm req) {
		ChannelVoRes rows=comChannelService.page(req);
		return new ResponseEntity<ChannelVoRes>(rows, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/changeStatus", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> page(@RequestParam String oid,@RequestParam String status) {
//		String operator=this.getLoginAdmin();
		comChannelService.changeStatus(oid,status);
		Response r = new Response();
		r.with("result","SUCCESS");
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
	/**
	 * 支付通道查询
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/queryPaymentChannel",method = RequestMethod.POST)
	public  BankChannelResponse queryPaymentChannel (@RequestBody  BankChannelRequest request) {
		log.info("支付通道查询请求 BankChannelRequest =[{}] ",JSONObject.toJSON(request));
		BankChannelResponse orderResponse = new BankChannelResponse();
		orderResponse = comChannelService.findBankChannelList(request);
		log.info("支付通道查询返回BankChannelResponse =[{}] ",JSONObject.toJSON(orderResponse));
		return orderResponse;
	}
}
