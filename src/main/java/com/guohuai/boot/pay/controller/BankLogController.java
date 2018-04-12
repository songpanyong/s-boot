package com.guohuai.boot.pay.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.guohuai.boot.pay.dao.BankLogDao;
import com.guohuai.boot.pay.form.BankLogForm;
import com.guohuai.boot.pay.res.BankLogVoRes;
import com.guohuai.boot.pay.service.BankLogService;
import com.guohuai.boot.pay.vo.BankLogVo;
import com.guohuai.component.common.TemplateQueryController;

/**
 * 银行交互日志
 */
@RestController
@RequestMapping(value = "/settlement/bankLog"/*, produces = "application/json;charset=utf-8"*/)
public class BankLogController extends TemplateQueryController<BankLogVo,BankLogDao>{

	@Autowired
	private BankLogService bankLogService;

	@RequestMapping(value = "/page", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<BankLogVoRes> page(BankLogForm form) {
		BankLogVoRes rows=bankLogService.page(form);	
		return new ResponseEntity<BankLogVoRes>(rows, HttpStatus.OK);
	}

	@RequestMapping(value = "/findByOrderNo", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<BankLogVoRes> page(@RequestParam String orderNo) {
		BankLogVoRes rows=bankLogService.findByOrderNo(orderNo);
		return new ResponseEntity<BankLogVoRes>(rows, HttpStatus.OK);
	}
	
}
