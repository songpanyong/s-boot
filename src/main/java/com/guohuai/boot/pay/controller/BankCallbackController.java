package com.guohuai.boot.pay.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.guohuai.basic.component.ext.web.Response;
import com.guohuai.boot.pay.dao.ComChannelDao;
import com.guohuai.boot.pay.form.BankCallbackForm;
import com.guohuai.boot.pay.res.BankCallbackVoRes;
import com.guohuai.boot.pay.service.BankCallbackService;
import com.guohuai.boot.pay.vo.ChannelVo;
import com.guohuai.component.common.TemplateQueryController;

/**
 * 银行回调
* @ClassName: BankCallbackController 
* @Description: 
* @date 2016年12月21日 上午11:15:13 
*
 */
@RestController
@RequestMapping(value = "/settlement/bankCallback")
public class BankCallbackController extends TemplateQueryController<ChannelVo, ComChannelDao> {
	@Autowired
	private BankCallbackService bankCallbackService;

	// 查询银行回调信息
	@RequestMapping(value = "/page", method = { RequestMethod.POST, RequestMethod.GET })
	public @ResponseBody ResponseEntity<BankCallbackVoRes> page(BankCallbackForm req) {
		BankCallbackVoRes rows = bankCallbackService.page(req);
		return new ResponseEntity<BankCallbackVoRes>(rows, HttpStatus.OK);
	}

	//增加回调次数
	@RequestMapping(value = "/addCallBackCount", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> handCallBack(BankCallbackForm req) {
		bankCallbackService.addCallBackCount(req.getOid(), req.getTotalCount());
		Response r = new Response();
		r.with("result","SUCCESS");
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
}
