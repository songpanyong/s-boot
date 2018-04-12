package com.guohuai.boot.pay.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.component.ext.web.BaseController;
import com.guohuai.boot.pay.service.PaymentService;
import com.guohuai.component.util.Constant;
import com.guohuai.payadapter.component.TradeChannel;
import com.guohuai.payadapter.component.TradeType;
import com.guohuai.payadapter.listener.event.DecryptEvent;

@Slf4j
@RestController
@RequestMapping(value = "/settlement/noticeUrl"/*, produces = "application/json;charset=utf-8"*/)
public class ListenerNotfriyController extends BaseController{
	
	@Autowired
	ApplicationEventPublisher eventPublisher;
	@Autowired
	private PaymentService paymentService;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@RequestMapping(value = "/withdrawing", method = {RequestMethod.POST,RequestMethod.GET})
	public String  withdrawingCallback(HttpServletRequest request, HttpServletResponse response) {
	    Map parameters = request.getParameterMap();//保存request请求参数的临时变量
	    log.info("服务器端通知-接收到先锋支付代付返回报文,{}",JSONObject.toJSON(parameters));
	    JSONObject obj = (JSONObject) JSONObject.toJSON(parameters);
	    DecryptEvent event = new DecryptEvent();
	    event.setChannel(TradeChannel.ucfPayWithdraw.getValue());
	    event.setData(obj.getString("data"));
	    event.setVersion("3");
	    event.setTradeType(TradeType.ucfPayDecrypt.getValue());
	    //推送处理
	    eventPublisher.publishEvent(event);
	    if(!Constant.SUCCESS.endsWith(event.getReturnCode())){
	    	log.info("sign verify FAIL:验签失败");
        	return "FAIL";
	    }
        log.info("sign verify SUCCESS:验签通过");
    	parameters = installNoticParam(event);
    	if(paymentService.noticUrl(parameters)){
    		//验签成功需返回先锋支付“SUCCESS”
    		log.info("先锋支付代付回调返回成功");
        	return "SUCCESS";
    	}else{
    		log.info("先锋支付代付回调处理失败");
    		return "FAIL";
    	}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@RequestMapping(value = "/validPay", method = {RequestMethod.POST,RequestMethod.GET})
	public String  validPayCallback(HttpServletRequest request, HttpServletResponse response) {
		Map parameters = request.getParameterMap();//保存request请求参数的临时变量
	    log.info("服务器端通知-接收到先锋支付认证支付返回报文,{}",JSONObject.toJSON(parameters));
	    JSONObject obj = (JSONObject) JSONObject.toJSON(parameters);
	    DecryptEvent event = new DecryptEvent();
	    event.setChannel(TradeChannel.ucfPayWithdraw.getValue());
	    event.setData(obj.getString("data"));
	    event.setVersion("4");
	    event.setTradeType(TradeType.ucfPayDecrypt.getValue());
	    //推送处理
	    eventPublisher.publishEvent(event);
	    if(!Constant.SUCCESS.endsWith(event.getReturnCode())&&event.getStatus()==null){
	    	log.info("sign verify FAIL:验签失败");
        	return "FAIL";
	    }
        log.info("sign verify SUCCESS:验签通过");
    	parameters = installNoticParam(event);
    	if(paymentService.noticUrl(parameters)){
    		//验签成功需返回先锋支付“SUCCESS”
    		log.info("先锋支付认证支付回调返回成功");
        	return "SUCCESS";
    	}else{
    		log.info("先锋支付认证支付回调处理失败");
    		return "FAIL";
    	}
	}
	
	/**
	 * 组装notic参数
	 * @param event
	 * @return
	 */
	public Map<String,String> installNoticParam(DecryptEvent event){
		Map<String,String> param = new HashMap<String,String>();
		param.put("tradeNo",event.getTradeNo());//返回流水号
		param.put("status",event.getStatus());//订单状态S成功F失败
		if("F".endsWith(event.getStatus())){
			event.setReturnCode(Constant.FAIL);
		}
		param.put("merchantNo",event.getPayNo());//支付流水号
		param.put("resMessage",event.getErrorDesc());//失败详情
		param.put("resCode",event.getReturnCode());//返回错误码
		return param;
	}
}
