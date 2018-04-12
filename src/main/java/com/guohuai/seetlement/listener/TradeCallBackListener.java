package com.guohuai.seetlement.listener;

import java.sql.Timestamp;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.boot.pay.dao.BankLogDao;
import com.guohuai.boot.pay.dao.ComOrderDao;
import com.guohuai.boot.pay.dao.PaymentDao;
import com.guohuai.boot.pay.service.ComOrderService;
import com.guohuai.boot.pay.vo.BankLogVo;
import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.boot.pay.vo.PaymentVo;
import com.guohuai.payadapter.listener.event.CallBackEvent;
import com.guohuai.payadapter.redeem.CallBackDao;
import com.guohuai.settlement.api.response.OrderResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @ClassName: TradeCallBackListener
 * @Description: 交易回调
 * @author xueyunlong
 * @date 2016年11月15日 下午5:29:32
 *
 */
@Slf4j
@Component
@SuppressWarnings("unused")
public class TradeCallBackListener {

	@Autowired
	private ComOrderDao comOrderDao;
	@Autowired
	private PaymentDao paymentDao;
	@Autowired
	private BankLogDao bankLogDao;

	@Autowired
	CallBackDao callbackDao;
	@Autowired
	ComOrderService comOrderService;
	
	private final int minute = 1;// 回调间隔时间;
	private final int totalCount = 20;// 最大回调次数;

	@Transactional
	@EventListener
	public void acceptOrderEvent(CallBackEvent event) {
		log.info("接收银行交易回调信息,event:{}", JSONObject.toJSONString(event));
		Timestamp time = new Timestamp(System.currentTimeMillis());
		BankLogVo bankLogVo = bankLogDao.findByPayNo(event.getPayNo());
		if (null != bankLogVo) {
			bankLogVo.setTradStatus(event.getStatus());
			bankLogVo.setBankReturnContent(event.getErrorDesc());
			bankLogVo.setErrorCode(event.getReturnCode());
			bankLogVo.setUpdateTime(time);
			bankLogDao.save(bankLogVo);

			PaymentVo paymentVo = paymentDao.findByOrderNo(bankLogVo.getOrderNo());
			paymentVo.setCommandStatus(event.getStatus());
			paymentVo.setUpdateTime(time);
			paymentVo.setFailDetail(event.getErrorDesc());
			paymentDao.save(paymentVo);

			OrderVo orderVo = comOrderDao.findByorderNo(paymentVo.getOrderNo());
			orderVo.setStatus(event.getStatus());
			orderVo.setUpdateTime(time);
			orderVo.setFailDetail(event.getErrorDesc());
			comOrderDao.save(orderVo);

			OrderResponse orderResponse = new OrderResponse();
			orderResponse.setStatus(event.getStatus());
			orderResponse.setPayNo(orderVo.getPayNo());
			orderResponse.setOrderNo(orderVo.getOrderNo());
			orderResponse.setUserOid(orderVo.getUserOid());
			orderResponse.setErrorMessage(event.getErrorDesc());
			orderResponse.setReturnCode(event.getReturnCode());
			orderResponse.setAmount(orderVo.getAmount());
			orderResponse.setUserType(orderVo.getUserType());
			log.info("推送orderResponse{}",JSONObject.toJSONString(orderResponse));
			comOrderService.pushResult(orderResponse);
		} else {
			log.info("定单号不存在，{}", event.getOrderNo());
		}

	}
}
