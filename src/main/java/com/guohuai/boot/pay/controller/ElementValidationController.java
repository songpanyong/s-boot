package com.guohuai.boot.pay.controller;

import java.util.List;

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
import com.guohuai.boot.pay.dao.ElementValidationDao;
import com.guohuai.boot.pay.form.ElementValidationForm;
import com.guohuai.boot.pay.res.ElementValidationRes;
import com.guohuai.boot.pay.service.ElementValidationService;
import com.guohuai.boot.pay.vo.ElementValidationVo;
import com.guohuai.component.common.TemplateQueryController;
import com.guohuai.settlement.api.request.ElementValidationRequest;
import com.guohuai.settlement.api.request.UserProtocolRequest;
import com.guohuai.settlement.api.response.ElementValidaResponse;
import com.guohuai.settlement.api.response.ElementValidaRulesResponse;
import com.guohuai.settlement.api.response.UserProtocolResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName: ElementValidationController
 * @Description: 四要素验证（鉴权）
 * @author xueyunlong
 * @date 2016年11月28日 下午3:22:05
 *
 */
@Slf4j
@RestController
@RequestMapping(value = "/settlement/element")
public class ElementValidationController extends TemplateQueryController<ElementValidationVo,ElementValidationDao>{

	@Autowired
	private ElementValidationService elementValidationService;
	
	@RequestMapping(value = "/find", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<List<ElementValidationVo>> findByUserOid(@RequestParam String userOid) {
		List<ElementValidationVo> rows=elementValidationService.findByUserOid(userOid);
		return new ResponseEntity<List<ElementValidationVo>>(rows, HttpStatus.OK);
	}

	
	@RequestMapping(value = "/page", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<ElementValidationRes> page(@RequestBody ElementValidationForm req) {
		ElementValidationRes rows=elementValidationService.page(req);
		return new ResponseEntity<ElementValidationRes>(rows, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/bindApply",method = RequestMethod.POST)
	public ElementValidaResponse  bindApply(@RequestBody ElementValidationRequest req) {
		ElementValidaResponse elementValidaResponse=new ElementValidaResponse();
//		elementValidaResponse=elementValidationService.elementWithSms(req);
		elementValidaResponse=elementValidationService.bindApply(req);
		return elementValidaResponse;
	}

	@RequestMapping(value = "/bindConfirmWithoutSms",method = RequestMethod.POST)
	public ElementValidaResponse  bindConfirmWithoutSms(@RequestBody ElementValidationRequest req) {
		return elementValidationService.elementWithoutSms(req);
	}
	
	
	@RequestMapping(value = "/bindConfrim",method = RequestMethod.POST)
	public ElementValidaResponse  bindConfrim(@RequestBody ElementValidationRequest req) {
		ElementValidaResponse elementValidaResponse=new ElementValidaResponse();
//		elementValidaResponse=elementValidationService.smsChekc(req);
		elementValidaResponse=elementValidationService.bindConfrim(req); 
		return elementValidaResponse;
	}
	
	@RequestMapping(value = "/bindBaofoo",method = RequestMethod.POST)
	public ElementValidaResponse  bindBaofoo(@RequestBody ElementValidationRequest req) {
		log.info("宝付直接绑卡：req={}",JSONObject.toJSON(req));
		ElementValidaResponse elementValidaResponse=new ElementValidaResponse();
		elementValidaResponse=elementValidationService.bindingBaofoo(req); 
		log.info("宝付直接绑卡返回：req={}",JSONObject.toJSON(elementValidaResponse));
		return elementValidaResponse;
	}
	
	@RequestMapping(value = "/unlock",method = RequestMethod.POST)
	public ElementValidaResponse  unlock(@RequestBody ElementValidationRequest req) {
//		ElementValidaResponse elementValidaResponse=elementValidationService.unLock(req); 
		ElementValidaResponse elementValidaResponse=elementValidationService.unbundling(req);
		return elementValidaResponse;
	}
	
	/**
	 * 绑卡规则获取
	 * @return
	 */
	@RequestMapping(value = "/bindingRules",method = RequestMethod.POST)
	public ElementValidaRulesResponse bindingRules() {
		ElementValidaRulesResponse elementValidaRulesResponse=elementValidationService.bindingRules();
		return elementValidaRulesResponse;
	}
	
	/**
	 * 用户绑卡信息查询
	 * @return
	 */
	@RequestMapping(value = "/bindCardInfo",method = RequestMethod.POST)
	public UserProtocolResponse bindCardInfo(@RequestBody UserProtocolRequest req){
		UserProtocolResponse userProtocolResponse=elementValidationService.bindCardInfo(req);
		return userProtocolResponse;
	}
}
