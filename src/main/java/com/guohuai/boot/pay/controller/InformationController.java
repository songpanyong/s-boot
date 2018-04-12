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
import com.guohuai.boot.pay.dao.InformationDao;
import com.guohuai.boot.pay.form.InformationForm;
import com.guohuai.boot.pay.res.InformationVoRes;
import com.guohuai.boot.pay.service.InformationService;
import com.guohuai.boot.pay.vo.InformationVo;
import com.guohuai.component.common.TemplateQueryController;

/**
 * @ClassName: InformationController
 * @Description: 银行账户管理
 * @author xueyunlong
 * @date 2016年11月28日 下午3:23:17
 *
 */
@RestController
@RequestMapping(value = "/settlement/information")
public class InformationController extends TemplateQueryController<InformationVo,InformationDao>{

	@Autowired
	private InformationService informationService;

	@RequestMapping(value = "/save",method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> save(InformationForm form) {
//		String operator=this.getLoginAdmin();
//		form.setUserOid(operator);
		informationService.save(form);
		Response r = new Response();
		r.with("result","SUCCESS");
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
	//修改信息
	@RequestMapping(value = "/update", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> update(InformationForm form) {
//		String operator=this.getLoginAdmin();
//		form.setUserOid(operator);
		informationService.update(form);
		Response r = new Response();
		r.with("result","SUCCESS");
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
	//更改状态
	@RequestMapping(value = "/updateStatus", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> updateStatus(@RequestParam(value="accountStatus") String accountStatus,@RequestParam(value="oid") String oid) {
//		String operator=this.getLoginAdmin();
//		form.setUserOid(operator);
		informationService.updateStatus(accountStatus,oid);
		Response r = new Response();
		r.with("result","SUCCESS");
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
//审批查询所有结果	
	@RequestMapping(value = "/approval", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<InformationVoRes> approval(InformationForm req) {
		InformationVoRes rows=informationService.approvalPage(req);
		return new ResponseEntity<InformationVoRes>(rows, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/page", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<InformationVoRes> page(InformationForm req) {
		InformationVoRes rows=informationService.page(req);
		return new ResponseEntity<InformationVoRes>(rows, HttpStatus.OK);
	}


}
