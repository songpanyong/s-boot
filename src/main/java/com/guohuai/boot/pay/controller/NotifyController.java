package com.guohuai.boot.pay.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.component.ext.web.BaseController;
import com.guohuai.boot.pay.service.PaymentService;
import com.guohuai.component.util.Constant;
import com.guohuai.payadapter.listener.event.TradeEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 网关支付交易回调
 * 
 * @author xyl
 *
 */
@Slf4j
@RestController
@RequestMapping(value = "/settlement/notify")
public class NotifyController extends BaseController {

	@Autowired
	private PaymentService paymentService;
	
	@Autowired
	private ApplicationEventPublisher event;

	/**
	 * 宝付网关支付接收通知
	 */
	@RequestMapping(value = "/baofoo/netpay", method = { RequestMethod.POST, RequestMethod.GET })
	public String baoofooNetpay(HttpServletRequest request, HttpServletResponse response) {
		// 转换请求参数为MAP
		Map<String, String[]> reqParams = request.getParameterMap();
		log.info("宝付网关支付接收通知:{}", reqParams);

		String TransID = request.getParameter("TransID");// 商户流水号
		String Result = request.getParameter("Result");// 支付结果
		String ResultDesc = request.getParameter("ResultDesc");// 支付结果描述
		String FactMoney = request.getParameter("FactMoney");// 实际成功金额
		String AdditionalInfo = request.getParameter("AdditionalInfo");// 订单附加消息
		String SuccTime = request.getParameter("SuccTime");// 支付完成时间
		String Md5Sign = request.getParameter("Md5Sign");//MD5签名宝付返回的
		TradeEvent tradeEvent = new TradeEvent();
		tradeEvent.setPayNo(TransID);
		tradeEvent.setAmount(FactMoney);
		tradeEvent.setReturnCode(Result);
		tradeEvent.setErrorDesc(ResultDesc);// 商品描述
		tradeEvent.setEmergencyMark(AdditionalInfo);
		tradeEvent.setSuccTime(SuccTime);
		tradeEvent.setSignature(Md5Sign);
		
		tradeEvent.setChannel("15");
		tradeEvent.setTradeType("01");

		log.info("宝付网关支付通知参数,{}", JSONObject.toJSONString(tradeEvent));
		event.publishEvent(tradeEvent);
		if (!Constant.SUCCESS.equals(tradeEvent.getReturnCode())) {
			log.info("验签失败");
			return "验签失败";
		}
		Map<String, String> map = new HashMap<String, String>();// 保存参与验签字段
		map.put("tradeNo", "");
		map.put("merchantNo", TransID);
		map.put("resMessage", ResultDesc);
		String status, resCode;
		if ("1".equals(Result)) {
			status = "S";// 支付成功
			resCode = Constant.SUCCESS;
		} else {
			status = "F";// 支付失败
			resCode = Constant.FAIL;
		}
		map.put("status", status);
		map.put("resCode", resCode);
		
		log.info("通知订单信息{}", JSONObject.toJSONString(map));
		
		if (paymentService.noticUrl(map)) {
			log.info("宝付网关支付回调返回成功！");
			return "OK";
		}
		return "FAIL";
	}

	@RequestMapping(value = "/baofoo/returnpage", method = { RequestMethod.POST, RequestMethod.GET })
	public String returnpage(HttpServletRequest request, HttpServletResponse response) {
		return "回调page";
	}
}
