package com.guohuai.boot.pay.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.guohuai.basic.component.ext.web.Response;
import com.guohuai.boot.pay.dao.AuthenticationDao;
import com.guohuai.boot.pay.service.OrderReconciliationService;
import com.guohuai.boot.pay.service.OutsideReconciliationService;
import com.guohuai.boot.pay.vo.AuthenticationVo;
import com.guohuai.component.common.TemplateQueryController;

/**
 * @ClassName: OrderReconciliationController
 * @Description: 对账
 * @author xueyunlong
 * @date 2016年11月28日 下午3:23:39
 *
 */
@RestController
@RequestMapping(value = "/settlement/orderReconciliation")
public class OrderReconciliationController extends TemplateQueryController<AuthenticationVo,AuthenticationDao>{

	@Autowired
	OrderReconciliationService orderReconciliationService;
	
	@Autowired
	OutsideReconciliationService outsideReconciliationService;

	@RequestMapping(value = "/test",method = RequestMethod.GET)
	public Map<String,Object>  apply() {
		Map<String,Object> returnMap = new HashMap<String,Object>();
		String dateString = "20161122";
		String channel = "01";
		returnMap = outsideReconciliationService.orderReconciliation(dateString, channel);
		return returnMap;
	}
	
	/**
	 * 对账
	 * @param dateString 对账日期YYYYMMDD
	 * @param channel 渠道 7金运通代扣，8金运通代付，9金运通网关支付
	 * @return
	 */
	@RequestMapping(value = "/check", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> page(@RequestParam String dateString,@RequestParam String channel) {
		Map<String,Object> returnMap = new HashMap<String,Object>();
		Response r = new Response();
		returnMap = outsideReconciliationService.orderReconciliation(dateString, channel);
		if("0000".equals(returnMap.get("responseCode"))){
			r.with("result","SUCCESS");
		}else{
			r.with("result","FAIL");
			r.with("resultDetial", returnMap.get("responseMsg"));
		}
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
}
