package com.guohuai.boot.pay.controller;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.component.ext.web.BaseController;
import com.guohuai.boot.pay.service.PaymentService;
import com.guohuai.common.payment.jytpay.api.MockClientMsgBase_Tran;
import com.guohuai.common.payment.jytpay.cmd.CmdTC3002Notify;
import com.guohuai.common.payment.jytpay.cmd.CmdTD4004SecondPayeeResp;
import com.guohuai.component.util.Constant;
import com.thoughtworks.xstream.XStream;

import lombok.extern.slf4j.Slf4j;

/**
 * 金运通回调通知地址
 * 
 * @author hans
 *
 */
@Slf4j
@RestController
@RequestMapping(value = "/settlement/jytNoticeUrl")
public class JytNotifyController extends BaseController {

	@Autowired
	private PaymentService paymentService;

//	@Autowired
//	private JytpayServiceImpl jytPayServiceImpl;

	@Autowired
	MockClientMsgBase_Tran baseTran;
	

	/**
	 * 收款
	 * 实名支付-金运通回调
	 */
	@RequestMapping(value = "/jytPayeeCallback", method = { RequestMethod.POST, RequestMethod.GET })
	public String payeeCallback(HttpServletRequest request, HttpServletResponse response) {
		log.info("金运通实名支付回调……");
		// 转换请求参数为MAP
		Map<String, String[]> reqParams = request.getParameterMap();
//		log.info("接收参数,{}",JSONObject.toJSONString(reqParams));
		Map<String, String> paramMap = new HashMap<String, String>();// 保存参与验签字段
		Iterator<Entry<String, String[]>> it = reqParams.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String[]> ent = it.next();
			if (ent.getValue().length == 1) {
				paramMap.put(ent.getKey(), ent.getValue()[0]);
			} else {
				log.error("获取参数" + ent.getKey() + "的值异常。ent.getValue().length=" + ent.getValue().length);
			}
		}

		String respXml = paramMap.get("xml_enc");
		String respKey = paramMap.get("key_enc");
		String respSign = paramMap.get("sign");
		String xmlStr = "";//保存解析后的报文
		boolean verifyResult = false;
		baseTran.initKey();//初始化证书和密钥
		try {
			// 调用金运通支付类库中验签方法
			verifyResult = baseTran.verifySign(respXml, respSign, respKey);
			// 解密xml报文
			xmlStr = baseTran.decrytXml(respXml, respKey);
			log.info("返回报文解析:{}",xmlStr);
		} catch (Exception e) {
			log.error("sign verify FAIL:验签异常{}", e);
			return "FAIL";
		}
		XStream xs = new XStream();
		CmdTD4004SecondPayeeResp resp = new CmdTD4004SecondPayeeResp();
		try{
			xs.processAnnotations(CmdTD4004SecondPayeeResp.class);
			xs.autodetectAnnotations(true);
			xs.ignoreUnknownElements();
			resp = (CmdTD4004SecondPayeeResp)xs.fromXML(xmlStr);
		}catch(Exception e){
			log.error("XML转化成Object失败",e);
			return "FAIL";
		}
		Map<String, String> map = new HashMap<String, String>();// 保存参与验签字段
		/**
		 * 这里的tradNo指交易流水号，merchantNo订单号
		 */
		map.put("tradeNo", resp.getHead().getTran_flowid());
		map.put("merchantNo", resp.getBody().getOrder_id());
		map.put("resMessage", resp.getHead().getResp_desc());
		String resCode = resp.getHead().getResp_code();//交易返回代码
		String status = resp.getBody().getTran_state();
		if("S0000000".equals(resCode)){
			if("00".equals(status)){
				status = "S";//支付成功
				resCode = Constant.SUCCESS;
			}else{
				status = "F";//支付失败
				resCode = Constant.FAIL;
			}
		}
		map.put("status", status);
		map.put("resCode", resCode);
		log.info("通知订单信息{}",JSONObject.toJSONString(map));
		if (verifyResult) {
			log.info("sign verify SUCCESS:验签通过");
			if (paymentService.noticUrl(map)) {
				log.info("金运通回调返回成功！！");
				return "SUCCESS";
			}
			return "FAIL";

		} else {
			log.info("sign verify FAIL:验签失败");
			return "FAIL";
		}
	}
	
	/**
	 * 资金代付-金运通回调
	 */
	@RequestMapping(value = "/jytPayCallback", method = { RequestMethod.POST, RequestMethod.GET })
	public String payCallback(HttpServletRequest request, HttpServletResponse response) {
		log.info("金运通资金代付回调……");
		// 转换请求参数为MAP
		Map<String, String[]> reqParams = request.getParameterMap();
//		log.info("接收参数,{}",JSONObject.toJSONString(reqParams));
		Map<String, String> paramMap = new HashMap<String, String>();// 保存参与验签字段
		Iterator<Entry<String, String[]>> it = reqParams.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String[]> ent = it.next();
			if (ent.getValue().length == 1) {
				paramMap.put(ent.getKey(), ent.getValue()[0]);
			} else {
				log.error("获取参数" + ent.getKey() + "的值异常。ent.getValue().length=" + ent.getValue().length);
			}
		}
		
//		log.info("服务器端通知-接收到金运通返回报文,{}", JSONObject.toJSON(paramMap));
		String respXml = paramMap.get("xml_enc");
		String respKey = paramMap.get("key_enc");
		String respSign = paramMap.get("sign");
		String xmlStr = "";//保存解析后的报文
		boolean verifyResult = false;
		baseTran.initKey();//初始化证书和密钥
		try {
			// 调用金运通支付类库中验签方法
			verifyResult = baseTran.verifySign(respXml, respSign, respKey);
			// 解密xml报文
			xmlStr = baseTran.decrytXml(respXml, respKey);
			log.info("返回报文解析:{}",xmlStr);
		} catch (Exception e) {
			log.error("sign verify FAIL:验签异常{}", e);
			return "FAIL";
		}
		XStream xs = new XStream();
		CmdTC3002Notify resp = new CmdTC3002Notify();
		try{
			xs.processAnnotations(CmdTC3002Notify.class);
			xs.autodetectAnnotations(true);
			xs.ignoreUnknownElements();
			resp = (CmdTC3002Notify)xs.fromXML(xmlStr);
		}catch(Exception e){
		log.error("XML转化成Object失败",e);
			return "FAIL";
		}
		Map<String, String> map = new HashMap<String, String>();// 保存参与验签字段
		map.put("tradeNo", resp.getHead().getTran_flowid());
		map.put("merchantNo", resp.getBody().getOri_tran_flowid());
		map.put("resMessage", resp.getBody().getTran_resp_desc());
		String resCode = resp.getBody().getTran_resp_code();
		String status = resp.getBody().getTran_state();
		if("S0000000".equals(resCode)){
			if("01".equals(status)){
				status = "S";//支付成功
				resCode = Constant.SUCCESS;
			}else{
				status = "F";//支付失败
				resCode = Constant.FAIL;
			}
		}
		map.put("status", status);
		map.put("resCode", resCode);
		log.info("通知订单信息{}",JSONObject.toJSONString(map));
		if (verifyResult) {
			log.info("sign verify SUCCESS:验签通过");
			if (paymentService.noticUrl(map)) {
				log.info("金运通回调返回成功！！");
				return "SUCCESS";
			}
			return "FAIL";
			
		} else {
			log.info("sign verify FAIL:验签失败");
			return "FAIL";
		}
	}
	
	/**
	 * 金运通网关支付回调地址
	 */
	@RequestMapping(value = "/jytNetPayCallback", method = { RequestMethod.POST, RequestMethod.GET })
	public String jytNetPayCallback(HttpServletRequest request, HttpServletResponse response) {
		log.info("金运通网关支付回调……");
		// 转换请求参数为MAP
		Map<String, String[]> reqParams = request.getParameterMap();
		Map<String, String> paramMap = new HashMap<String, String>();// 保存参与验签字段
		Iterator<Entry<String, String[]>> it = reqParams.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String[]> ent = it.next();
			if (ent.getValue().length == 1) {
				paramMap.put(ent.getKey(), ent.getValue()[0]);
			} else {
				log.error("获取参数" + ent.getKey() + "的值异常。ent.getValue().length=" + ent.getValue().length);
			}
		}

//		log.info("服务器端通知-接收到金运通网关支付返回报文,{}", JSONObject.toJSON(paramMap));
		String oriMerOrderId = paramMap.get("oriMerOrderId");// 原订单号
		String payFlowid = paramMap.get("payFlowid");// 平台支付订单号
		// String oriMerTranTime = paramMap.get("oriMerTranTime");// 原订单发送时间
		// String payFinishTime = paramMap.get("payFinishTime");// 支付完成时间
		String status = paramMap.get("tranState");// 交易状态
		String resCode = paramMap.get("respCode");// 交易响应码
		String resMessage = paramMap.get("respDesc");// 交易响应码

		Map<String, String> map = new HashMap<String, String>();// 保存参与验签字段
		map.put("tradeNo", payFlowid);
		map.put("merchantNo", oriMerOrderId);
		map.put("resMessage", resMessage);
		if ("S0000000".equals(resCode)) {
			if ("02".equals(status)) {//订单交易状态，00-初始 01-支付中，02-支付成功，03-支付失败，04-过期订单  ,05-撤销成功,06-作废订单
				status = "S";// 支付成功
				resCode = Constant.SUCCESS;
			} else {
				status = "F";// 支付失败
				resCode = Constant.FAIL;
			}
		}
		map.put("status", status);
		map.put("resCode", resCode);
		log.info("通知订单信息{}", JSONObject.toJSONString(map));
		if (paymentService.noticUrl(map)) {
			log.info("金运通网关支付回调返回成功！");
			return "S0000000";
		}
		return "FAIL";
	}

}
