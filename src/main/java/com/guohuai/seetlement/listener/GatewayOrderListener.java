package com.guohuai.seetlement.listener;

import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.PayEnum;
import com.guohuai.boot.pay.dao.BankLogDao;
import com.guohuai.boot.pay.dao.PaymentDao;
import com.guohuai.boot.pay.service.ComOrderService;
import com.guohuai.boot.pay.vo.BankLogVo;
import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.boot.pay.vo.PaymentVo;
import com.guohuai.component.util.TradeTypeEnum;
import com.guohuai.seetlement.listener.event.GatewayOrderEvent;
import com.guohuai.settlement.api.request.OrderRequest;
import com.guohuai.settlement.api.response.OrderResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 网关支付请求适配器
 */
@Slf4j
@Component
public class GatewayOrderListener {
	@Autowired
	private PaymentDao paymentDao;
	@Autowired
	private BankLogDao bankLogDao;
	@Autowired
	private ComOrderService comOrderService;
	@Autowired
	private SeqGenerator seqGenerator;

	@EventListener
	public void acceptOrderEvent(GatewayOrderEvent event) {
		log.info("接收网关支付请求,event:{}", JSONObject.toJSONString(event));
		OrderVo orderVo = event.getOrderVo();
		OrderResponse orderResponse = event.getOrderResponse();
		OrderRequest req = event.getOrderRequest();
		String payNo = seqGenerator.next(orderVo.getType());
		if (!StringUtil.isEmpty(orderVo.getPayNo())) {
			payNo = orderVo.getPayNo();
		}else{
			orderVo.setPayNo(payNo);
		}
		
		long time_test = System.currentTimeMillis();
		Timestamp time = new Timestamp(System.currentTimeMillis());
		
		BankLogVo bankLogVo =null;
		PaymentVo paymentVo =null;
		// 创建交互日志
		bankLogVo = new BankLogVo();
		bankLogVo.setOrderNo(orderVo.getOrderNo());
		bankLogVo.setUserOid(orderVo.getUserOid());
		bankLogVo.setPayNo(payNo);
		bankLogVo.setOperatorTime(time);
		bankLogVo.setCreateTime(time);
		bankLogVo.setUpdateTime(time);
		bankLogVo.setTradStatus(PayEnum.PAY0.getCode());
		bankLogVo.setAmount(orderVo.getAmount());
		bankLogVo.setType(orderVo.getType());
		
		// 创建支付记录
		paymentVo = new PaymentVo();
		paymentVo.setOrderNo(orderVo.getOrderNo());
		paymentVo.setCommandStatus(PayEnum.PAY0.getCode());
		paymentVo.setAmount(orderVo.getAmount());
		paymentVo.setCardNo(orderVo.getCardNo());
		paymentVo.setPlatformAccount(orderVo.getCardNo());
		paymentVo.setType(orderVo.getType());
		paymentVo.setCreateTime(time);
		paymentVo.setUpdateTime(time);
		paymentVo.setUserOid(orderVo.getUserOid());
		paymentVo.setRealName(orderVo.getRealName());
		paymentVo.setPayNo(bankLogVo.getPayNo());
		paymentVo.setAccountCity(req.getInAcctCityName());
		paymentVo.setAccountProvince(req.getInAcctProvinceCode());
		paymentVo.setChannelNo(orderVo.getChannel());
		paymentVo.setUpTime(time);//存放对账日期
		paymentVo.setUserType(orderVo.getUserType());
		paymentDao.saveAndFlush(paymentVo);

		bankLogVo.setSheetId(paymentVo.getOid());
		bankLogDao.saveAndFlush(bankLogVo);
		
		log.info("同步定单保存时间：{}", (System.currentTimeMillis() - time_test));
		if (TradeTypeEnum.trade_pay.getCode().equals(orderVo.getType())) {//充值
			comOrderService.callPublishGatewayPayEvent(orderVo, paymentVo, bankLogVo, orderResponse, req);
		} 
	}
}
