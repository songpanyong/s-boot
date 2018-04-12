package com.guohuai.boot.pay.controller;

import java.sql.Timestamp;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.guohuai.basic.component.ext.web.Response;
import com.guohuai.boot.pay.dao.ReconciliationStatisticsDao;
import com.guohuai.boot.pay.form.ReconciliationStatisticsForm;
import com.guohuai.boot.pay.res.ReconciliationStatisticsVoRes;
import com.guohuai.boot.pay.service.ReconciliationStatisticsService;
import com.guohuai.boot.pay.vo.ReconciliationStatisticsVo;
import com.guohuai.component.common.TemplateQueryController;
import com.guohuai.settlement.api.response.BaseResponse;


@RestController
@RequestMapping(value = "/settlement/statistics"/*, produces = "application/json;charset=utf-8"*/)
public class ReconciliationStatisticsController extends TemplateQueryController<ReconciliationStatisticsVo,ReconciliationStatisticsDao>{

	@Autowired
	private ReconciliationStatisticsService reconciliationStatisticsService;
	
	@RequestMapping(value = "/getData", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> getData(@Valid ReconciliationStatisticsForm form) {
		String date = form.getOutsideDate();
		Timestamp outsideDate = Timestamp.valueOf(date+" 00:00:00");
		ReconciliationStatisticsVoRes res = reconciliationStatisticsService
				.findReconciliationStatisticsByChannleAndDate(form.getChannelNo(), outsideDate);
		Response r = new Response();
		if("SUCCESS".equals(res.getReturnCode())){
			r.with("result","SUCCESS");     
			r.with("res", res);
		}else{
			r.with("result","FAIL");
			r.with("resultDetial", "系统异常");
		}
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
	/**
	 * 确认完成对账
	 * @param oids
	 * @return
	 */
	@RequestMapping(value = "/confirmComplete",method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> confirmComplete(@Valid ReconciliationStatisticsForm form) {
		Response r = new Response();
		String date = form.getOutsideDate();
		Timestamp outsideDate = Timestamp.valueOf(date+" 00:00:00");
		BaseResponse resp = reconciliationStatisticsService.confirmCompleteReconciliation(form.getChannelNo(), outsideDate);
		r.with("result", resp.getReturnCode());
		r.with("resultDetial", resp.getErrorMessage());
		return new ResponseEntity<Response>(r,HttpStatus.OK);
	}
	
}
