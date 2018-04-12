package com.guohuai.boot.pay.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.guohuai.boot.pay.dao.ComChannelDao;
import com.guohuai.boot.pay.form.BankCallbackLogForm;
import com.guohuai.boot.pay.res.BankCallbackLogVoRes;
import com.guohuai.boot.pay.service.BankCallbackLogService;
import com.guohuai.boot.pay.vo.ChannelVo;
import com.guohuai.component.common.TemplateQueryController;

/**
 * 银行回调日志
* @ClassName: BankCallbackController 
* @Description: TODO
* @author longyunbo
* @date 2016年12月21日 上午11:15:13 
*
 */
@RestController
@RequestMapping(value = "/settlement/bankCallbackLog")
public class BankCallbackLogController extends TemplateQueryController<ChannelVo, ComChannelDao> {
	@Autowired
	private BankCallbackLogService bankCallbackLogService;

	// 查询银行回调日志信息
	@RequestMapping(value = "/listbycboid", method = { RequestMethod.POST, RequestMethod.GET })
	public @ResponseBody ResponseEntity<BankCallbackLogVoRes> getlistByCallbackOid(BankCallbackLogForm form) {
		BankCallbackLogVoRes res = bankCallbackLogService.findBankCallbackLogByCBOid(form);
		return new ResponseEntity<BankCallbackLogVoRes>(res, HttpStatus.OK);
	}

	
}
