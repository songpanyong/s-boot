package com.guohuai.boot.pay.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.guohuai.boot.pay.dao.ProtocolDao;
import com.guohuai.boot.pay.form.ProtocolForm;
import com.guohuai.boot.pay.res.ProtocolVoRes;
import com.guohuai.boot.pay.service.ProtocolService;
import com.guohuai.boot.pay.vo.ProtocolVo;
import com.guohuai.component.common.TemplateQueryController;


@RestController
@RequestMapping(value = "/settlement/protocol"/*, produces = "application/json;charset=utf-8"*/)
public class ProtocolController extends TemplateQueryController<ProtocolVo,ProtocolDao>{

	@Autowired
	private ProtocolService protocolService;	

	@RequestMapping(value = "/page", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<ProtocolVoRes> save(@RequestBody ProtocolForm req) {
		ProtocolVoRes rows=protocolService.page(req);
		return new ResponseEntity<ProtocolVoRes>(rows, HttpStatus.OK);
	}


}
