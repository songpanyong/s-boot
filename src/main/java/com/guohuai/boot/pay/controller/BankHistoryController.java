package com.guohuai.boot.pay.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.guohuai.basic.component.ext.web.Response;
import com.guohuai.boot.pay.dao.BankHistoryDao;
import com.guohuai.boot.pay.form.BankHistoryForm;
import com.guohuai.boot.pay.res.BankHistoryVoRes;
import com.guohuai.boot.pay.service.BankHistoryService;
import com.guohuai.boot.pay.vo.BankHistoryVo;
import com.guohuai.component.common.TemplateQueryController;

/**
 * 
 * 银行对账信息查询，忽略对账
 *
 */
@RestController
@RequestMapping(value = "/settlement/bankHistory")
public class BankHistoryController extends TemplateQueryController<BankHistoryVo,BankHistoryDao>{

	@Autowired
	private BankHistoryService bankHistoryService;
	
	@RequestMapping(value = "/page", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<BankHistoryVoRes> page(BankHistoryForm req) {
		BankHistoryVoRes rows = bankHistoryService.page(req);
		return new ResponseEntity<BankHistoryVoRes>(rows, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/ignore",method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> ignore(@RequestParam String[] oids) {
//		String operator=this.getLoginAdmin();
//		form.setUserOid(operator);
		Response resp = new Response();
		bankHistoryService.ignore(oids);
		resp.with("result", "SUCCESS");
		return new ResponseEntity<Response>(resp,HttpStatus.OK);
	}
	
}
