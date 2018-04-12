package com.guohuai.seetlement.listener;

import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
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
import com.guohuai.boot.pay.vo.ProtocolVo;
import com.guohuai.component.util.IPUtil;
import com.guohuai.component.util.TradeTypeEnum;
import com.guohuai.seetlement.listener.event.OrderPayOrPayeeEvent;
import com.guohuai.settlement.api.request.OrderRequest;
import com.guohuai.settlement.api.response.OrderResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 支付|赎回异步 请求适配器
 */
@Slf4j
@Component
public class OrderPayOreeListener {
	@Autowired
	private PaymentDao paymentDao;
	@Autowired
	private BankLogDao bankLogDao;
	@Autowired
	private ComOrderService comOrderService;
	@Autowired
	private SeqGenerator seqGenerator;

	@Value("${withOutThirdParty:no}")
	private String withOutThirdParty;
	
	@EventListener
	@Async("payExcutor")
	public void acceptOrderPayOreeEvent(OrderPayOrPayeeEvent event) {
		log.info("异步接收支付|赎回请求,event:{}", JSONObject.toJSONString(event));
		OrderVo orderVo = event.getOrderVo();
		ProtocolVo protocolVo = event.getProtocolVo();
		OrderResponse orderResponse = event.getOrderResponse();
		OrderRequest req = event.getOrderRequest();
		//获取本机IP
		String localip = IPUtil.getHostIP();
		String host = localip.substring(localip.lastIndexOf(".")+1,localip.length());
		String payNo = seqGenerator.next(host+orderVo.getType());//2017.1.20添加本机ip末尾用于区分哪台服务器订单
		//测试对账 修改payNo 尾数 为定单号位数
		if("yes".equals(withOutThirdParty)){
			int length = orderVo.getOrderNo().length();
			payNo=payNo+orderVo.getOrderNo().substring(length-1, length);
		}
		if (!StringUtil.isEmpty(orderVo.getPayNo())) {
			payNo = orderVo.getPayNo();
		}
		Timestamp time = new Timestamp(System.currentTimeMillis());
		
		BankLogVo bankLogVo =null;
		PaymentVo paymentVo =null;
		//是否是二次支付,充值时指输入验证码支付，提现指人工审核支付
		if(event.isReqTwo()){
			bankLogVo=bankLogDao.findByPayNo(req.getPayNo());
			paymentVo=paymentDao.findByPayNo(req.getPayNo());
			
		}else{
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
			BankLogVo bankL = bankLogDao.findBySheetId(req.getPayNo());
			if(bankL != null){//先锋认证支付确认支付获取原推进参数
				bankLogVo.setBankReturnSerialId(bankL.getBankReturnSerialId());//先锋支付用户id
				bankLogVo.setBankReturnTicket(bankL.getBankReturnTicket());//先锋支付流水号
				bankLogVo.setBankSerialNumber(bankL.getBankSerialNumber());//先锋交易流水号
			}
			
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
			paymentVo.setPayNo(payNo);
			paymentVo.setUserType(orderVo.getUserType());
			paymentVo.setPhone(orderVo.getPhone());
			
			/**
			 * 添加省 市
			 */
			paymentVo.setAccountCity(req.getInAcctCityName());
			paymentVo.setAccountProvince(req.getInAcctProvinceCode());
			paymentDao.saveAndFlush(paymentVo);

			bankLogVo.setSheetId(paymentVo.getOid());
			bankLogDao.saveAndFlush(bankLogVo);
		}
		
		OrderResponse orderPayResponse = new OrderResponse();
		if (TradeTypeEnum.trade_pay.getCode().equals(orderVo.getType())) {
			orderPayResponse =  comOrderService.callPublishPayEvent(orderVo, protocolVo, paymentVo, bankLogVo, orderResponse);
			log.info("支付返回 orderPayResponse：{}",JSONObject.toJSON(orderPayResponse));
		} else if (TradeTypeEnum.trade_payee.getCode().equals(orderVo.getType())) {
			paymentVo.setLaunchplatform(PayEnum.PAYMETHOD1.getCode());
			orderPayResponse = comOrderService.callPublishPayeeEvent(orderVo, protocolVo, paymentVo, bankLogVo, orderResponse, req);
			log.info("代付返回 orderPayResponse：{}",JSONObject.toJSON(orderPayResponse));

		}
		
	}

}
