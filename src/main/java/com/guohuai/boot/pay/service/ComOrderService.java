package com.guohuai.boot.pay.service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountTransRequest;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.ErrorDesEnum;
import com.guohuai.boot.PayEnum;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.service.AccountInfoService;
import com.guohuai.boot.account.service.AccountWithdrawalsService;
import com.guohuai.boot.account.service.TransService;
import com.guohuai.boot.pay.dao.BankLogDao;
import com.guohuai.boot.pay.dao.ComOrderDao;
import com.guohuai.boot.pay.dao.PaymentDao;
import com.guohuai.boot.pay.dao.ProtocolDao;
import com.guohuai.boot.pay.vo.BankLogVo;
import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.boot.pay.vo.PaymentVo;
import com.guohuai.boot.pay.vo.ProtocolVo;
import com.guohuai.component.util.*;
import com.guohuai.payadapter.component.TradeChannel;
import com.guohuai.payadapter.component.TradeEventCodeEnum;
import com.guohuai.payadapter.control.ChannelDao;
import com.guohuai.payadapter.listener.event.OrderEvent;
import com.guohuai.payadapter.listener.event.TradeEvent;
import com.guohuai.payadapter.redeem.CallBackDao;
import com.guohuai.seetlement.listener.event.GatewayOrderEvent;
import com.guohuai.seetlement.listener.event.OrderPayOrPayeeEvent;
import com.guohuai.settlement.api.SettlementSdk;
import com.guohuai.settlement.api.request.OrderRequest;
import com.guohuai.settlement.api.response.BaseResponse;
import com.guohuai.settlement.api.response.OrderResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单交易支付
 * 
 * @author star.zhu 2016年11月14日
 * @modify xueyunlong 2016年11月15日 合并出款、入款
 */
@Service
@Slf4j
public class ComOrderService {
	@Autowired
	private ComOrderDao comOrderDao;
	@Autowired
	private PaymentDao paymentDao;
	@Autowired
	private BankLogDao bankLogDao;
	@Autowired
	private ApplicationEventPublisher event;
	@Autowired
	private ProtocolDao protocolDao;
	@Autowired
	private CallBackDao callbackDao;
	@Autowired
	private SeqGenerator seqGenerator;
	@Autowired
	SettlementSdk settlementSdk;
	@Autowired
	PaymentService paymentService;
	
	@Autowired
	AccountInfoService accountInfoService;
	@Autowired
	TransService transService;
	@Autowired
	ChannelDao channelDao;
	@Autowired
	ComChannelBankService channelBankService;
	
	@Autowired
	UserInfoDao userInfoDao;
	@Autowired
	private PayTwoRedisUtil payTwoRedisUtil;
	
	@Autowired
	private AccountWithdrawalsService accountWithdrawalsService;
	
	@Autowired
	private AccOrderDao accOrderDao;
	@Value("${withOutThirdParty:no}")
	private String withOutThirdParty;
	
	@Autowired
	private PayPushService payPushService;

	@Autowired
	AccountInfoDao accountInfoDao;
	/**
	 * 确认支付
	 * 
	 * @param req
	 * @return
	 */
	@Transactional
	public OrderResponse trade(OrderRequest req) {
		log.info("OrderNo{},接收订单,{}", req.getOrderNo(), JSONObject.toJSONString(req));
		OrderResponse orderResponse = new OrderResponse();
		if (StringUtil.isEmpty(TradeTypeEnum.getEnumName(req.getType()))) {
			orderResponse.setReturnCode(TradeEventCodeEnum.trade_1005.getCode());
			orderResponse.setErrorMessage(TradeEventCodeEnum.getEnumName(orderResponse.getReturnCode()));
			return orderResponse;
		}
		
		Timestamp time = new Timestamp(System.currentTimeMillis());
		// 是否四要素验证成功
//		ProtocolVo protocolVo = protocolDao.findOneByUserOidAndStatus(req.getUserOid(), ErrorDesEnum.ElELOCK.getCode());
		//20180115支持用户绑定多张卡
		if(StringUtil.isEmpty(req.getBankCard())){
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("银行卡号不能为空");
			return orderResponse;
		}
		String cardNo = DesPlus.encrypt(req.getBankCard());
		ProtocolVo protocolVo = protocolDao.findOneByUserOidAndStatusAndCarNo(
				req.getUserOid(), ErrorDesEnum.ElELOCK.getCode(), cardNo);
		try {
			// 设置返回信息
			BeanUtils.copyProperties(req, orderResponse);

			if (null != protocolVo) {
				// 创建订单记录
				OrderVo orderVo = null;
				boolean isTwo = false;// 是否是确认支付
				if (req.getType().equals(TradeTypeEnum.trade_pay.getCode())) {
					BankLogVo bankL = bankLogDao.findBySheetId(req.getPayNo());
					if (bankL == null) {
						orderResponse.setReturnCode(TradeEventCodeEnum.trade_1011.getCode());
						orderResponse.setErrorMessage(TradeEventCodeEnum.trade_1011.getName());
						log.info("支付未发送短信验证：{}", JSONObject.toJSONString(orderResponse));
						return orderResponse;
					}

					// 是否原来存在
					orderVo = comOrderDao.findByPayNo(req.getPayNo());
					orderResponse.setSmsCode(req.getSmsCode());
					if (orderVo != null) {
						isTwo = true;
						orderVo.setReceiveTime(time);
						orderVo.setUpdateTime(time);
						comOrderDao.saveAndFlush(orderVo);
					}
				}

				if (orderVo == null) {
					orderVo = new OrderVo();
					BeanUtils.copyProperties(req, orderVo);
					orderVo.setPayNo(StringUtil.isEmpty(req.getPayNo()) ? "" : req.getPayNo());
					orderVo.setStatus(PayEnum.PAY0.getCode());
					orderVo.setCardNo(protocolVo.getCardNo());
					orderVo.setRealName(protocolVo.getRealName());
					orderVo.setReceiveTime(time);
					orderVo.setCreateTime(time);
					orderVo.setUpdateTime(time);
					orderVo.setBankCode(protocolVo.getBankName());
					UserInfoEntity user = userInfoDao.findByUserOid(orderVo.getUserOid());
					if (null != user) {
						orderVo.setPhone(user.getPhone());
					}

					comOrderDao.saveAndFlush(orderVo);
				}

				// 如果要提现，判断是否超提现额度
				if (PayEnum.PAYTYPE02.getCode().equals(req.getType())) {
					boolean isOrderExcess = accountInfoService.isOrderExcess(req.getUserOid(), req.getAmount());
					log.info("提现判断是否超出可提现余额：{}", isOrderExcess);
					if (isOrderExcess) {
						orderResponse.setStatus(PayEnum.PAY2.getCode());
						orderResponse.setAmount(orderVo.getAmount());
						orderResponse.setReturnCode(TradeEventCodeEnum.trade_1012.getCode());
						orderResponse.setErrorMessage(TradeEventCodeEnum.trade_1012.getName());
						log.error("定单处理异常orderNo:{},error:{}", req.getOrderNo(), orderResponse.getErrorMessage());
						return orderResponse;
					} else {// 未超限，开始记账
							// 20170410记账
							// 创建支付记录仅用于记账，不保存订单
						PaymentVo paymentVo = new PaymentVo();
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
						paymentVo.setAccountCity(req.getInAcctCityName());
						paymentVo.setAccountProvince(req.getInAcctProvinceCode());
						paymentVo.setUserType(orderVo.getUserType());

						// 记账
						Map<String, Object> accountMap = accounting(req, orderVo.getOrderNo(), orderVo.getUserOid(),
								paymentVo);
						// 记账失败处理
						if (!Constant.SUCCESS.equals(accountMap.get("returnCode"))) {
							orderResponse.setStatus(PayEnum.PAY2.getCode());
							orderResponse.setAmount(orderVo.getAmount());
							orderResponse.setReturnCode((String) accountMap.get("returnCode"));
							orderResponse.setErrorMessage((String) accountMap.get("returnMsg"));
							log.error("定单处理异常orderNo:{},error:{}", req.getOrderNo(), orderResponse.getErrorMessage());
							return orderResponse;
						}
					}
				}

				orderResponse.setStatus(PayEnum.PAY1.getCode());
				orderResponse.setAmount(orderVo.getAmount());
				orderResponse.setReturnCode(Constant.SUCCESS);

				// 异步支付
				OrderPayOrPayeeEvent orderPayOrPayeeEvent = new OrderPayOrPayeeEvent();
				orderPayOrPayeeEvent.setOrderVo(orderVo);
				orderPayOrPayeeEvent.setOrderRequest(req);
				orderPayOrPayeeEvent.setOrderResponse(orderResponse);
				orderPayOrPayeeEvent.setProtocolVo(protocolVo);
				orderPayOrPayeeEvent.setReqTwo(isTwo);
				// 推送异步支付
				event.publishEvent(orderPayOrPayeeEvent);
			} else {
				orderResponse.setReturnCode(TradeEventCodeEnum.trade_1006.getCode());
				orderResponse.setErrorMessage(TradeEventCodeEnum.trade_1006.getName());
			}

		} catch (Exception e) {
			log.error("定单处理异常orderNo:{},error:{}", req.getOrderNo(), e);
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("系统错误！");
		}
		log.info("确认支付返回：{}", JSONObject.toJSONString(orderResponse));
		return orderResponse;

	}

	/**
	 * 
	 * 代扣
	 * @param req
	 * @return
	 */
	public OrderResponse witholding(OrderRequest req) {

		Timestamp time = new Timestamp(System.currentTimeMillis());

		// 设置返回信息
		OrderResponse orderResponse = new OrderResponse();
		BeanUtils.copyProperties(req, orderResponse);

		// 验证定单类别
		if (!TradeTypeEnum.trade_pay.getCode().equals(req.getType())) {
			orderResponse.setReturnCode(TradeEventCodeEnum.trade_1005.getCode());
			orderResponse.setErrorMessage(TradeEventCodeEnum.getEnumName(orderResponse.getReturnCode()));
			return orderResponse;
		}

		// 是否四要素验证成功
//		ProtocolVo protocolVo = protocolDao.findOneByUserOidAndStatus(req.getUserOid(), ErrorDesEnum.ElELOCK.getCode());
		//20180115支持用户绑定多张卡
		if(StringUtil.isEmpty(req.getBankCard())){
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("银行卡号不能为空");
			return orderResponse;
		}
		String cardNo = DesPlus.encrypt(req.getBankCard());
		ProtocolVo protocolVo = protocolDao.findOneByUserOidAndStatusAndCarNo(
				req.getUserOid(), ErrorDesEnum.ElELOCK.getCode(), cardNo);
		if (null == protocolVo) {
			orderResponse.setReturnCode(TradeEventCodeEnum.trade_1006.getCode());
			orderResponse.setErrorMessage(TradeEventCodeEnum.trade_1006.getName());
			return orderResponse;
		}

		OrderVo orderVo = new OrderVo();
		try {
			// 创建订单记录
			BeanUtils.copyProperties(req, orderVo);
			orderVo.setPayNo(StringUtil.isEmpty(req.getPayNo()) ? "" : req.getPayNo());
			orderVo.setStatus(PayEnum.PAY0.getCode());
			orderVo.setCardNo(protocolVo.getCardNo());
			orderVo.setRealName(protocolVo.getRealName());
			orderVo.setBankCode(protocolVo.getBankName());
			orderVo.setReceiveTime(time);
			orderVo.setCreateTime(time);
			orderVo.setUpdateTime(time);
//			orderVo.setUserType(req.getUserType());
			log.info("创建定单 orderVo = [{}]", JSONObject.toJSON(orderVo));
			comOrderDao.save(orderVo);
		} catch (Exception e) {
			log.error("定单处理异常orderNo:{},error:{}", req.getOrderNo(), e);
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("创建定单失败，系统繁忙！");
			return orderResponse;
		}
			//定单保存成功
			orderResponse.setStatus(PayEnum.PAY1.getCode());
			orderResponse.setReturnCode(Constant.SUCCESS);

			try {
				// 异步代扣
				OrderPayOrPayeeEvent orderPayOrPayeeEvent = new OrderPayOrPayeeEvent();
				orderPayOrPayeeEvent.setOrderVo(orderVo);
				orderPayOrPayeeEvent.setOrderRequest(req);
				orderPayOrPayeeEvent.setOrderResponse(orderResponse);
				orderPayOrPayeeEvent.setProtocolVo(protocolVo);
				orderPayOrPayeeEvent.setReqTwo(false);
				event.publishEvent(orderPayOrPayeeEvent);
			} catch (Exception e) {
				log.error("推送异步支付异常 orderNo:{},error:{}", req.getOrderNo(), e);
				orderResponse.setReturnCode(Constant.FAIL);
				orderResponse.setErrorMessage("推送异步支付异常，系统繁忙！");
			}
		return orderResponse;

	}
	/**
	 * 网关支付
	 * 
	 * @param req
	 * @return
	 */
	public OrderResponse gatewayRrade(OrderRequest req) {
		log.info("OrderNo{},网关支付接收订单,{}", req.getOrderNo(), JSONObject.toJSONString(req));
		OrderResponse orderResponse = new OrderResponse();
		if (StringUtil.isEmpty(TradeTypeEnum.getEnumName(req.getType()))) {
			orderResponse.setReturnCode(TradeEventCodeEnum.trade_1005.getCode());
			orderResponse.setErrorMessage(TradeEventCodeEnum.getEnumName(orderResponse.getReturnCode()));
			return orderResponse;
		}
		if(StringUtil.isEmpty(req.getUserOid())){
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("用户id不能为空");
			return orderResponse;
		}
		UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(req.getUserOid());
		if(userInfoEntity == null){
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("用户不存在");
			return orderResponse;
		}
		long time_test = System.currentTimeMillis();
		Timestamp time = new Timestamp(System.currentTimeMillis());
		try {
			// 设置返回信息
			BeanUtils.copyProperties(req, orderResponse);
			// 创建订单记录
			OrderVo orderVo = null;
			if (!StringUtil.isEmpty(req.getOrderNo())) {
				// 判断订单是否已存在存在
				orderVo = comOrderDao.findByPayNo(req.getPayNo());
				if (orderVo == null) {
					orderVo = new OrderVo();
					BeanUtils.copyProperties(req, orderVo);
					orderVo.setPayNo(StringUtil.isEmpty(req.getPayNo()) ? "" : req.getPayNo());
					orderVo.setStatus(PayEnum.PAY0.getCode());// 未处理
					orderVo.setUserType(userInfoEntity.getUserType());
					orderVo.setReceiveTime(time);
					orderVo.setCreateTime(time);
					orderVo.setUpdateTime(time);
					comOrderDao.saveAndFlush(orderVo);
				} else {
					orderResponse.setReturnCode("1012");
					orderResponse.setErrorMessage("订单号已存在");
					return orderResponse;
				}
			} else {
				orderResponse.setReturnCode(TradeEventCodeEnum.trade_1005.getCode());
				orderResponse.setErrorMessage(TradeEventCodeEnum.getEnumName(orderResponse.getReturnCode()));
				return orderResponse;
			}

			// 同步网关支付
			GatewayOrderEvent gatewayOrderEvent = new GatewayOrderEvent();
			gatewayOrderEvent.setOrderVo(orderVo);
			gatewayOrderEvent.setOrderRequest(req);
			gatewayOrderEvent.setOrderResponse(orderResponse);
			log.info("推送网关支付{}",JSONObject.toJSON(gatewayOrderEvent));
			event.publishEvent(gatewayOrderEvent);

			if ("0000".equals(gatewayOrderEvent.getOrderResponse().getReturnCode())) {
				orderResponse.setStatus(PayEnum.PAY1.getCode());// 交易成功
				orderResponse.setAmount(orderVo.getAmount());
				orderResponse.setReturnCode(Constant.SUCCESS);
			} else {
				orderResponse.setStatus(PayEnum.PAY2.getCode());// 交易失败
				orderResponse.setAmount(orderVo.getAmount());
				orderResponse.setReturnCode(Constant.FAIL);
			}
			log.info("定单类别：{}，支付时间：{}", orderVo.getType(), System.currentTimeMillis() - time_test);

		} catch (Exception e) {
			log.error("定单处理异常orderNo:{},error:{}", req.getOrderNo(), e);
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("系统错误！");
		}
		log.info("网关支付返回：{}", JSONObject.toJSONString(orderResponse));
		return orderResponse;

	}

	/**
	 * 支付短信验证
	 * 
	 * @param req
	 * @return
	 */
	public OrderResponse validPay(OrderRequest req) {
		log.info("validPay{}", JSONObject.toJSON(req));
		OrderResponse orderResponse = new OrderResponse();
		if(StringUtil.isEmpty(req.getBankCard())){
			log.error("充值卡号不能为空,{}",req.getRequestNo());
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("充值卡号不能为空");
			return orderResponse;
		}
		Timestamp time = new Timestamp(System.currentTimeMillis());
		// 是否四要素验证成功
		BankLogVo bankLogEntity = bankLogDao.findOneByRequestNo(req.getRequestNo());
		try {
			// 设置返回信息
			BeanUtils.copyProperties(req, orderResponse);
			if(null!=bankLogEntity){
				log.info("请求流水号重复,{}",req.getRequestNo());
				orderResponse.setReturnCode(Constant.FAIL);
				orderResponse.setErrorMessage("请求流水号重复");
				return orderResponse;
			}
			String cardNo = DesPlus.encrypt(req.getBankCard());
//			ProtocolVo protocolVo = protocolDao.findOneByUserOidAndStatus(req.getUserOid(), ErrorDesEnum.ElELOCK.getCode());
			//20180115支持用户绑定多张卡
			ProtocolVo protocolVo = protocolDao.findOneByUserOidAndStatusAndCarNo(
					req.getUserOid(), ErrorDesEnum.ElELOCK.getCode(), cardNo);
			if (null != protocolVo) {
				String custAccountId =  DesPlus.decrypt(protocolVo.getCertificateNo());
				// 创建订单记录
				BankLogVo bankLogVo = new BankLogVo();
				bankLogVo.setUserOid(req.getUserOid());
				// 为了不改变原来的逻辑，发起验证码的流水号，暂赋值在sheetId上
				bankLogVo.setSheetId(seqGenerator.next(PayEnum.PAYTYPE01.getCode()));
				bankLogVo.setAmount(req.getAmount());
				bankLogVo.setRemark("支付短信验证发起");
				bankLogVo.setCreateTime(time);
				bankLogVo.setType(PayEnum.PAYTYPE01.getCode());
				bankLogVo.setRequestNo(req.getRequestNo());
				bankLogDao.save(bankLogVo);
				
				OrderEvent orderEvent = new OrderEvent();
				orderEvent.setAmount(req.getAmount());
				orderEvent.setUserOid(req.getUserOid());
				orderEvent.setPayNo(bankLogVo.getSheetId());
				orderEvent.setCardNo(req.getBankCard());
				orderEvent.setRealName(protocolVo.getRealName());
				orderEvent.setSourceType(req.getSystemSource());
				orderEvent.setMobile(protocolVo.getPhone());
				orderEvent.setTradeType(PayEnum.PAYTYPE01.getCode());
				orderEvent.setCustAccountId(custAccountId);
				orderEvent.setBindId(protocolVo.getProtocolNo());
//				orderEvent.setProductName(req.getProdInfo());
				event.publishEvent(orderEvent);
				
				
				if (orderEvent.getReturnCode().equals(Constant.SUCCESS)) {
					orderResponse.setStatus(PayEnum.PAY1.getCode());
					bankLogVo.setTradStatus(PayEnum.PAY1.getCode());
				}else {
					orderResponse.setStatus(PayEnum.PAY2.getCode());
					bankLogVo.setTradStatus(PayEnum.PAY2.getCode());
				}
				orderResponse.setErrorMessage(orderEvent.getErrorDesc());
				orderResponse.setSmsCode(req.getSmsCode());
				orderResponse.setPayNo(orderEvent.getPayNo());
				orderResponse.setReturnCode(orderEvent.getReturnCode());
				// 重新获取验证码用,支付的手机号,支付流水号
				orderResponse.setPhone(protocolVo.getPhone());
				orderResponse.setPayNo(orderEvent.getPayNo());
				//先锋参数
				orderResponse.setMemberUserId(orderEvent.getBindId());
				orderResponse.setOutPaymentId(orderEvent.getOutPaymentId());
				orderResponse.setOutTradeNo(orderEvent.getOutTradeNo());
				
				//先锋推进参数
				bankLogVo.setBankReturnSerialId(orderEvent.getBindId());//先锋支付用户id
				bankLogVo.setBankReturnTicket(orderEvent.getOutPaymentId());//先锋支付流水号
				bankLogVo.setBankSerialNumber(orderEvent.getOutTradeNo());//先锋交易流水号
				
				bankLogVo.setRemark(orderEvent.getBusinessNo());// 返回支付推进参数---宝付
				bankLogVo.setSmsCode(req.getSmsCode());
				bankLogVo.setFailDetail(orderEvent.getErrorDesc());
				bankLogDao.save(bankLogVo);
			} else {
				orderResponse.setReturnCode(TradeEventCodeEnum.trade_1006.getCode());
				orderResponse.setErrorMessage(TradeEventCodeEnum.trade_1006.getName());
			}
		} catch (Exception e) {
			log.error("发起短信处理异常validPay:{},error:{}", req.getOrderNo(), e);
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("系统错误！");
		}
		log.info("支付短信验证返回：{}", JSONObject.toJSONString(orderResponse));
		return orderResponse;

	}

	/**
	 * 调用支付接口
	 * 
	 * @param orderVo
	 * @param protocolVo
	 * @param paymentVo
	 * @param bankLogVo
	 * @param orderResponse
	 * @return
	 */
	@Transactional
	public OrderResponse callPublishPayEvent(OrderVo orderVo, ProtocolVo protocolVo, PaymentVo paymentVo,
			BankLogVo bankLogVo, OrderResponse orderResponse) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		// 创建相应事件
		OrderEvent orderEvent = new OrderEvent();
		orderEvent.setOrderNo(orderVo.getOrderNo());
		orderEvent.setPayNo(bankLogVo.getPayNo());
		orderEvent.setAmount(orderVo.getAmount());
		orderEvent.setRealName(orderVo.getRealName());
		orderEvent.setCustAccountId(DesPlus.decrypt(protocolVo.getCertificateNo()));
		orderEvent.setCardNo(DesPlus.decrypt(orderVo.getCardNo()));
		orderEvent.setSourceType(orderVo.getSystemSource());
		orderEvent.setTradeType(orderVo.getType());
		orderEvent.setUserOid(paymentVo.getUserOid());
		orderEvent.setBankTypeCode(protocolVo.getBankTypeCode());
		orderEvent.setVerifyCode(orderResponse.getSmsCode());
		orderEvent.setMobile(protocolVo.getPhone());

		// 查询推进参数
		String remark = bankLogDao.findRemarkByPayNo(orderVo.getPayNo());
		String outPaymentId = bankLogVo.getBankReturnTicket();//先锋支付流水号
		String outTradeNo = bankLogVo.getBankSerialNumber();//先锋交易流水号
		String bindId = bankLogVo.getBankReturnSerialId();//先锋支付用户id
		log.info("支付日志：{}",JSONObject.toJSONString(bankLogVo));
		orderEvent.setBusinessNo(remark);// 宝付支付推进参数
		orderEvent.setOutPaymentId(outPaymentId);
		orderEvent.setOutTradeNo(outTradeNo);
		orderEvent.setBindId(bindId);
		// 同步推送处理
		log.info("支付指令参数,{}", JSONObject.toJSONString(orderEvent));
		event.publishEvent(orderEvent);
		long returnTime = System.currentTimeMillis();
		log.info("支付返回,{}", JSONObject.toJSONString(orderEvent));

		// 重新获取对象
		orderVo = comOrderDao.getOne(orderVo.getOid());
		paymentVo = paymentDao.getOne(paymentVo.getOid());
		bankLogVo = bankLogDao.getOne(bankLogVo.getOid());

		if (!StringUtil.isEmpty(orderEvent.getHostFlowNo())) {
			paymentVo.setHostFlowNo(orderEvent.getHostFlowNo());
		}

		if (orderEvent.getReturnCode().equals(Constant.SUCCESS)) {
			// 交易申请成功 以实时状态为准,1成功，2失败，3处理中
			paymentVo.setCommandStatus(PayEnum.PAY1.getCode());
			paymentVo.setBankReturnSeriNo(orderEvent.getHostFlowNo()); // 回写银行流水号
			bankLogVo.setBankReturnSerialId(orderEvent.getHostFlowNo());
			
		
		} else if (orderEvent.getReturnCode().equals(Constant.INPROCESS)) {
			paymentVo.setCommandStatus(PayEnum.PAY3.getCode());
		} else {
			paymentVo.setCommandStatus(PayEnum.PAY2.getCode());
			orderVo.setFailDetail(orderEvent.getErrorDesc());
			paymentVo.setFailDetail(orderEvent.getErrorDesc());
		}
		// 当回调过快时，回调线程已把定单状态修改为成功，如果有此情况说明已支付成功，这里获取支付成功状态
		if (PayEnum.PAY1.getCode().equals(orderVo.getStatus())) {
			log.info("定单{}优先回调成功", orderVo.getOrderNo());
			paymentVo.setCommandStatus(PayEnum.PAY1.getCode());
		}

		// 修改收单
		orderVo.setStatus(paymentVo.getCommandStatus());
		orderVo.setChannel(orderEvent.getChannel());
		orderVo.setBankReturnSeriNo(orderEvent.getHostFlowNo());
		orderVo.setPayNo(bankLogVo.getPayNo());
		orderVo.setReturnCode(orderEvent.getReturnCode());
		orderVo.setUpdateTime(time);
		orderVo.setBankCode(orderEvent.getBankName());
		comOrderDao.save(orderVo);

		// 修改支付记录
		paymentVo.setMerchantId(orderEvent.getMerchantId());
		paymentVo.setProductId(orderEvent.getProductId());

		// 回写渠道 交易类别
		paymentVo.setChannelNo(orderEvent.getChannel());
		paymentVo.setType(orderVo.getType());
		// 回写平台信息
		paymentVo.setPlatformAccount(orderEvent.getPlatformAccount());
		paymentVo.setPlatformName(orderEvent.getPlatformName());
		paymentVo.setPayAddress(orderEvent.getPayAddress());
		paymentVo.setUpdateTime(time);
		//存放对账时间
		paymentVo.setUpTime(time);
		paymentDao.save(paymentVo);
		log.info("交易指令修改完成，orderNo:{}", orderVo.getOrderNo());

		// 修改交互日志
		bankLogVo.setTradStatus(paymentVo.getCommandStatus());
		bankLogVo.setErrorCode(orderEvent.getReturnCode());
		bankLogVo.setFailDetail(orderEvent.getErrorDesc());
		bankLogVo.setUpdateTime(time);
		bankLogVo.setSmsCode(orderEvent.getVerifyCode());
		bankLogDao.save(bankLogVo);
		log.info("修改状态完成time:{}", System.currentTimeMillis() - returnTime);
		
		orderResponse.setStatus(paymentVo.getCommandStatus());
		orderResponse.setAmount(paymentVo.getAmount());
		orderResponse.setPayNo(paymentVo.getPayNo());
		orderResponse.setOrderNo(paymentVo.getOrderNo());
		orderResponse.setReturnCode(orderEvent.getReturnCode());
		orderResponse.setErrorMessage(orderEvent.getErrorDesc());
		// 实时推送业务系统(暂金运通渠道)
			//异步处理，如果不是成功和超时，（所有同步调用银行、者适配器处理错误、找不到渠道等）都回调业务系统
	
		if(!StringUtil.in(orderResponse.getReturnCode(),Constant.INPROCESS)){
			pushResult(orderResponse);
		}
		log.info("支付交易返回orderResponse:{}", JSONObject.toJSONString(orderResponse));
		return orderResponse;
	}

	/**
	 * 赎回
	 * 
	 * @param orderVo
	 * @param protocolVo
	 * @param paymentVo
	 * @param bankLogVo
	 * @param orderResponse
	 * @return
	 */
	@Transactional
	public OrderResponse callPublishPayeeEvent(OrderVo orderVo, ProtocolVo protocolVo, PaymentVo paymentVo,
			BankLogVo bankLogVo, OrderResponse orderResponse, OrderRequest req) {
		// 创建相应事件
		OrderEvent orderEvent = new OrderEvent();
		
		orderEvent.setOrderNo(orderVo.getOrderNo());
		orderEvent.setPayNo(bankLogVo.getPayNo());
		orderEvent.setAmount(orderVo.getAmount());
		orderEvent.setRealName(orderVo.getRealName());
		orderEvent.setCustAccountId(DesPlus.decrypt(protocolVo.getCertificateNo()));
		orderEvent.setLaunchplatform(paymentVo.getLaunchplatform());
		orderEvent.setSourceType(orderVo.getSystemSource());
		orderEvent.setTradeType(orderVo.getType());
		orderEvent.setUserOid(paymentVo.getUserOid());
		// 添加省 市
		orderEvent.setInAcctProvinceCode(paymentVo.getAccountProvince());
		orderEvent.setInAcctCityName(paymentVo.getAccountCity());
		orderEvent.setCardNo(DesPlus.decrypt(orderVo.getCardNo()));

		//账户记账
		Map<String,Object> accountMap = accounting(req, orderVo.getOrderNo(), orderVo.getUserOid(), paymentVo);
		
		if(!Constant.SUCCESS.equals(accountMap.get("returnCode"))){
			orderEvent.setReturnCode((String) accountMap.get("returnCode"));
			orderEvent.setErrorDesc((String) accountMap.get("returnMsg"));
		}else if((boolean) accountMap.get("isNeedPublish")){//判断是否需要代付
			log.info("赎回指令参数,{}", JSONObject.toJSONString(orderEvent));
			event.publishEvent(orderEvent);
			log.info("赎回返回,{}", JSONObject.toJSONString(orderEvent));
		}
		
		// 重新获取对象
		orderVo = comOrderDao.getOne(orderVo.getOid());
		paymentVo = paymentDao.getOne(paymentVo.getOid());
		bankLogVo = bankLogDao.getOne(bankLogVo.getOid());

		if (!StringUtil.isEmpty(orderEvent.getHostFlowNo())) {
			paymentVo.setHostFlowNo(orderEvent.getHostFlowNo());
		}
		if (orderEvent.getReturnCode().equals(Constant.SUCCESS)) {
			// 设置为处理中
			paymentVo.setCommandStatus(PayEnum.PAY3.getCode());
			paymentVo.setBankReturnSeriNo(orderEvent.getHostFlowNo()); // 回写银行流水号
			bankLogVo.setBankReturnSerialId(orderEvent.getHostFlowNo());
	
		} else if (orderEvent.getReturnCode().equals(Constant.INPROCESS)) {
			paymentVo.setCommandStatus(PayEnum.PAY4.getCode());
		} else if (orderEvent.getReturnCode().equals(TradeEventCodeEnum.trade_1007.getCode())) {
			log.info("人工处理，设置为未处理状态，orderNo:{},money:{}", orderVo.getOrderNo(), orderVo.getAmount());
			// 交易设置为处理中，返回业务系统为成功，等人工处理后回调
			paymentVo.setCommandStatus(PayEnum.PAY0.getCode());
			paymentVo.setLaunchplatform(PayEnum.PAYMETHOD2.getCode());// 20170209修改为后台发起订单，走申请提现审核
			orderEvent.setErrorDesc(TradeEventCodeEnum.trade_1007.getName());
		} else {
			paymentVo.setCommandStatus(PayEnum.PAY2.getCode());
		}
		
		// 当回调过快时，回调线程已把定单状态修改为成功，如果有此情况说明已支付成功，这里获取支付成功状态
		if (PayEnum.PAY1.getCode().equals(orderVo.getStatus())) {
			log.info("定单{}优先回调成功", orderVo.getOrderNo());
			paymentVo.setCommandStatus(PayEnum.PAY1.getCode());
		}

		// 修改收单
		orderVo.setStatus(paymentVo.getCommandStatus());
		orderVo.setFailDetail(orderEvent.getErrorDesc());
		orderVo.setChannel(orderEvent.getChannel());
		orderVo.setBankReturnSeriNo(orderEvent.getHostFlowNo());
		orderVo.setPayNo(bankLogVo.getPayNo());
		orderVo.setReturnCode(orderEvent.getReturnCode());
		comOrderDao.save(orderVo);

		// 修改支付记录
		paymentVo.setFailDetail(orderEvent.getErrorDesc());
		paymentVo.setChannelNo(orderEvent.getChannel());
		paymentVo.setType(orderVo.getType());
		paymentVo.setEmergencyMark(orderEvent.getEmergencyMark());
		paymentVo.setCrossFlag(orderEvent.getCrossFlag());
		paymentVo.setPlatformAccount(orderEvent.getPlatformAccount());
		paymentVo.setPlatformName(orderEvent.getPlatformName());
		paymentVo.setPayAddress(orderEvent.getPayAddress());
		paymentDao.save(paymentVo);

		// 修改交互日志
		bankLogVo.setTradStatus(paymentVo.getCommandStatus());
		bankLogVo.setErrorCode(orderEvent.getReturnCode());
		bankLogVo.setFailDetail(orderEvent.getErrorDesc());
		bankLogDao.save(bankLogVo);
		callbackDao.updateByPayNo(orderEvent.getChannel(), bankLogVo.getPayNo());

		orderResponse.setStatus(paymentVo.getCommandStatus());
		orderResponse.setAmount(paymentVo.getAmount());
		orderResponse.setPayNo(paymentVo.getPayNo());
		orderResponse.setOrderNo(paymentVo.getOrderNo());
		orderResponse.setErrorMessage(orderEvent.getErrorDesc());
		orderResponse.setReturnCode(orderEvent.getReturnCode());
		if (orderEvent.getReturnCode().equals(TradeEventCodeEnum.trade_1007.getCode())) {
			orderResponse.setReturnCode(Constant.SUCCESS);
		}
		//赎回走人工处理
		log.info("赎回交易返回orderResponse:{}", JSONObject.toJSONString(orderResponse));
		return orderResponse;
	}

	/**
	 * 查询支付状态
	 * 
	 * @param orderRequest
	 * @return
	 */
	public OrderResponse queryPay(OrderRequest orderRequest) {
		// ----------修改指令状态---------
		PaymentVo paymentVo = paymentDao.findByOrderNo(orderRequest.getOrderNo());

		// ---------调用-----
		OrderEvent orderEvent = new OrderEvent();
		orderEvent.setOrderNo(orderRequest.getOrderNo());
		orderEvent.setProductId(paymentVo.getProductId());
		orderEvent.setMerchantId(paymentVo.getMerchantId());
		orderEvent.setTradeType("queryPayment");
		orderEvent.setChannel(paymentVo.getChannelNo());
		log.info("{},查询支付状态请求,{}", JSONObject.toJSONString(orderEvent));
		event.publishEvent(orderEvent);
		log.info("{},查询支付状态返回,{}", JSONObject.toJSONString(orderEvent));

		OrderResponse orderResponse = new OrderResponse();
		orderResponse.setUserOid(orderRequest.getUserOid());
		orderResponse.setOrderNo(orderEvent.getOrderNo());
		orderResponse.setType(paymentVo.getType());
		orderResponse.setAmount(orderEvent.getAmount());
		orderResponse.setStatus(orderEvent.getStatus());
		return orderResponse;
	}

	/**
	 * 网关支付
	 * 
	 * @param orderVo
	 * @param paymentVo
	 * @param bankLogVo
	 * @param orderResponse
	 * @return
	 */
	public OrderResponse callPublishGatewayPayEvent(OrderVo orderVo, PaymentVo paymentVo, BankLogVo bankLogVo,
			OrderResponse orderResponse, OrderRequest req) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		// 创建相应事件
		TradeEvent tradeEvent = new TradeEvent();
		tradeEvent.setOrderNo(orderVo.getOrderNo());
		tradeEvent.setPayNo(orderVo.getPayNo());
		tradeEvent.setAmount(orderVo.getAmount().toString());
		tradeEvent.setOrderDesc(req.getDescribe());// 商品描述
		tradeEvent.setProdInfo(req.getProdInfo());// 商品信息
		tradeEvent.setProdDetailUrl(req.getProdDetailUrl());// 商品地址
		tradeEvent.setUserOid(paymentVo.getUserOid());
		tradeEvent.setTradeType(orderVo.getType());
		
		tradeEvent.setChannel("14");
		tradeEvent.setTradeTime(time);
		log.info("支付指令参数,{}", JSONObject.toJSONString(tradeEvent));
		event.publishEvent(tradeEvent);
		// log.info("支付返回,{}", JSONObject.toJSONString(tradeEvent));

		// 重新获取对象
		orderVo = comOrderDao.getOne(orderVo.getOid());
		paymentVo = paymentDao.getOne(paymentVo.getOid());

		// 修改收单
		orderVo.setStatus(paymentVo.getCommandStatus());
		orderVo.setFailDetail(tradeEvent.getErrorDesc());
		orderVo.setChannel(tradeEvent.getChannel());
		orderVo.setBankReturnSeriNo(tradeEvent.getHostFlowNo());
		orderVo.setPayNo(bankLogVo.getPayNo());
		orderVo.setReturnCode(tradeEvent.getReturnCode());
		orderVo.setUpdateTime(time);
		comOrderDao.save(orderVo);

		// 修改支付记录
		paymentVo.setFailDetail(tradeEvent.getErrorDesc());
		paymentVo.setMerchantId(tradeEvent.getMerchantId());
		paymentVo.setProductId(tradeEvent.getProductId());
		// 回写渠道 交易类别
		paymentVo.setChannelNo(tradeEvent.getChannel());
		// 回写平台信息
		paymentVo.setUpdateTime(time);
		paymentDao.save(paymentVo);

		orderResponse.setStatus(orderVo.getStatus());// 网关支付状态未知，暂定未处理
		orderResponse.setAmount(orderVo.getAmount());
		orderResponse.setReturnCode(tradeEvent.getReturnCode());
		orderResponse.setErrorMessage(tradeEvent.getErrorDesc());
		orderResponse.setRespHtml(tradeEvent.getRespHTML());
		return orderResponse;

	}

	/**
	 * 重新获取短信验证码
	 * 
	 * @param req
	 * @return
	 */
	public OrderResponse reValidPay(OrderRequest req) {// 首次发送验证码需返回手机号moblie和支付流水号payNo
		log.info("OrderNo{},接收重新获取验证码订单,{}", req.getOrderNo(), JSONObject.toJSONString(req));
		OrderResponse orderResponse = new OrderResponse();
		TradeEvent tradeEvent = new TradeEvent();
		try {
			// 重新获取验证码需要字段，手机号，待支付订单号
			tradeEvent.setChannel(TradeChannel.ucfPayCertPay.getValue());
			tradeEvent.setTradeType("reVerifiCode");// 重新获取验证码
			tradeEvent.setMobile(req.getPhone());
			tradeEvent.setPayNo(req.getPayNo());
			event.publishEvent(tradeEvent);
			if (tradeEvent.getReturnCode().equals(Constant.SUCCESS)) {
				orderResponse.setStatus(PayEnum.PAY1.getCode());
			
			} else if (tradeEvent.getReturnCode().equals(Constant.INPROCESS)) {
				orderResponse.setStatus(PayEnum.PAY4.getCode());
			} else {
				orderResponse.setStatus(PayEnum.PAY2.getCode());
			}
			orderResponse.setPayNo(tradeEvent.getPayNo());
			orderResponse.setReturnCode(tradeEvent.getReturnCode());
			orderResponse.setErrorMessage(tradeEvent.getErrorDesc());
		} catch (Exception e) {
			log.error("定单处理异常orderNo:{},error:{}", req.getOrderNo(), e);
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("系统错误！");
		}
		orderResponse.setPayNo(tradeEvent.getPayNo());
		log.info("重新获取验证码返回：{}", JSONObject.toJSONString(orderResponse));
		return orderResponse;
	}

	/**
	 * 最终实时返回的确定交易结果
	 */
	public void pushResult(OrderResponse orderResponse) {
		Map<String,String> map = new HashMap<String,String>();
//		map.put("settlement", "settlement");
		map.put("tradeNo", orderResponse.getOrderNo());
		if(Constant.SUCCESS.equals(orderResponse.getReturnCode())){
			orderResponse.setStatus("S");
		}else{
			orderResponse.setStatus("F");
		}
		map.put("status", orderResponse.getStatus());
		map.put("merchantNo", orderResponse.getPayNo());
		map.put("resCode", orderResponse.getReturnCode());
		map.put("resMessage", orderResponse.getErrorMessage());
		//实时返回成功和失败交易状态进行通知，处理中用查询接口查询最交易终状态再推送
		
		if(!StringUtil.in(orderResponse.getReturnCode(), Constant.INPROCESS)){
			log.info("交易实时回调……,支付订单号{},map:{}",orderResponse.getPayNo(),JSONObject.toJSONString(map));
			paymentService.noticUrl(map);
		}
	}
	
	/**
	 * 账户记账
	 * @param req
	 * @param orderNo
	 * @param userOid
	 * @param paymentVo
	 * @return
	 */
	public Map<String,Object> accounting(OrderRequest req, String orderNo, String userOid,PaymentVo paymentVo){
		Map<String, Object> accountMap = new HashMap<String,Object>();
		//默认不记账
		boolean isNeedAccounting = false;
		//默认不推送
		boolean isNeedPublish = false;
		String returnCode = Constant.SUCCESS;
		String returnMsg = "成功";
		//查询记账状态
		String accountingTimes = transService.getWithdrawalsAccountStatus(userOid, orderNo);
		log.info("获取记账状态，已记账{}次",accountingTimes);
		//调用trade赎回接口订单，需首次记账：增加提现冻结户余额，基本户不变，调用feed赎回接口订单，二次记账：提现冻结户与余额减少，基本户余额减少
		if(req != null&&"0".equals(accountingTimes)){//trade接口调用，并且是未记账
			accountingTimes = "1";//首次记账
			isNeedAccounting = true;
			isNeedPublish = true;
		}else if(req != null&&"1".equals(accountingTimes)){//trade接口调用，并且已记账提现冻结户
			isNeedAccounting = false;
			isNeedPublish = true;
		}else if(req != null&&"2".equals(accountingTimes)){//trade接口调用，并且已记账基本户
			isNeedAccounting = false;
			isNeedPublish = false;
			returnCode = TradeEventCodeEnum.trade_1014.getCode();//订单不能重复记账
			returnMsg = TradeEventCodeEnum.trade_1014.getName();
		}else if("0".equals(accountingTimes)){//调用feed接口，但尚未记账,历史数据或第一次未记账成功的订单
			try {
				log.info("接收提现订单，第1记账，开始记账...");
				AccountTransResponse accountTransResponse = paymentService.accounting(paymentVo, null);
				if (Constant.SUCCESS.equals(accountTransResponse.getReturnCode())) {
					log.info("记账成功");
					isNeedAccounting = true;
					isNeedPublish = true;
					accountingTimes = "2";//二次记账
					//记账完成,查询余额
					if(UserTypeEnum.INVESTOR.getCode().equals(paymentVo.getUserType())){
						 //查询投资人余额
						accountInfoService.getAccountBalanceByUserOid(userOid);
					}else if(UserTypeEnum.PUBLISHER.getCode().equals(paymentVo.getUserType())){
						 //查询发行人余额
						accountInfoService.getPublisherAccountBalanceByUserOid(userOid,"");
					}
				} else {
					log.info("记账失败，accountTransResponse ={}",JSONObject.toJSON(accountTransResponse));
					returnCode = TradeEventCodeEnum.trade_1013.getCode();//订单记账失败
					returnMsg = TradeEventCodeEnum.trade_1013.getName();
				}
			} catch (Exception e) {
				log.error("记账异常,定单号：{},错误信息：{}", paymentVo.getOrderNo(), e);
				returnCode = TradeEventCodeEnum.trade_1013.getCode();//订单记账失败
				returnMsg = TradeEventCodeEnum.trade_1013.getName();
			}
		}else if("1".equals(accountingTimes)){//调用feed接口，已完成提现冻结户记账，基本户需记账
			isNeedAccounting = true;
			isNeedPublish = true;
			accountingTimes = "2";//二次记账
		}else if("2".equals(accountingTimes)){//调用feed接口，已完成基本户记账，无需再次记账
			isNeedPublish = true;
		}
		//是否需要记账
		if(isNeedAccounting){
			try {
				log.info("接收提现订单，第{}记账，开始记账...",accountingTimes);
				AccountTransResponse accountTransResponse = paymentService.accounting(paymentVo, null);
				if (Constant.SUCCESS.equals(accountTransResponse.getReturnCode())) {
					log.info("记账成功");
					isNeedPublish = true;
					//记账完成,查询余额
					if(UserTypeEnum.INVESTOR.getCode().equals(paymentVo.getUserType())){
						 //查询投资人余额
						accountInfoService.getAccountBalanceByUserOid(userOid);
					}else if(UserTypeEnum.PUBLISHER.getCode().equals(paymentVo.getUserType())){
						 //查询发行人余额
						accountInfoService.getPublisherAccountBalanceByUserOid(userOid,"");
					}
//					accountInfoService.getAccountBalanceByUserOid(userOid);
				} else {
					log.info("记账失败，accountTransResponse ={}",JSONObject.toJSON(accountTransResponse));
					returnCode = TradeEventCodeEnum.trade_1013.getCode();
					returnCode = TradeEventCodeEnum.trade_1013.getName();
				}
			} catch (Exception e) {
				log.error("记账异常,定单号：{},错误信息：{}", paymentVo.getOrderNo(), e);
				returnCode = TradeEventCodeEnum.trade_1013.getCode();
				returnCode = TradeEventCodeEnum.trade_1013.getName();
			}
		}
		accountMap.put("isNeedPublish", isNeedPublish);
		accountMap.put("returnCode", returnCode);
		accountMap.put("returnMsg", returnMsg);
		return accountMap;
		
	}
	
	/**
	 * 商户余额查询
	 */
	public OrderResponse getMemBalanceQuery(){
		OrderResponse orderResponse = new OrderResponse();
		TradeEvent tradeEvent = new TradeEvent();
		tradeEvent.setTradeType("memBalanceQry");
		tradeEvent.setAccountType("1");
		event.publishEvent(tradeEvent);
		String returnCode = tradeEvent.getReturnCode();
		String returnMsg = tradeEvent.getErrorDesc();
		String balance = "";
		if(Constant.SUCCESS.equals(returnCode)){
			balance = tradeEvent.getAccBalance().get("1");
			if(StringUtil.isEmpty(balance)){
				balance = "0.00";
				returnCode = Constant.FAIL;
				returnMsg = "账户余额为空";
				orderResponse.setAmount(new BigDecimal(balance));
				orderResponse.setReturnCode(returnCode);
				orderResponse.setErrorMessage(returnMsg);
				return orderResponse;
			}
			log.info("商户基本户余额:"+balance);
		}else{
			balance = "0.00";
		}
		orderResponse.setAmount(new BigDecimal(balance));
		orderResponse.setReturnCode(returnCode);
		orderResponse.setErrorMessage(returnMsg);
		return orderResponse;
	}
	
	
	/**
	 * 提现申请
	 * 
	 * @param req
	 * @return
	 */
	public OrderResponse applyWthdrawal(OrderRequest req) {

		log.info("OrderNo{},提现申请接收订单,{}", req.getOrderNo(), JSONObject.toJSONString(req));
		OrderResponse orderResponse = new OrderResponse();
		BeanUtils.copyProperties(req, orderResponse);
		OrderVo order = saveReqOrder(req);
		if (null == order) {
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("保存提现申请异常");
			return orderResponse;
		}
		if (!TradeTypeEnum.trade_payee.getCode().equals(req.getType())) {
			orderResponse.setReturnCode(TradeEventCodeEnum.trade_1005.getCode());
			orderResponse.setErrorMessage(TradeEventCodeEnum.getEnumName(orderResponse.getReturnCode()));
			order.setStatus(PayEnum.PAY2.getCode());
			order.setReturnCode(orderResponse.getReturnCode());
			order.setFailDetail(orderResponse.getErrorMessage());
			comOrderDao.saveAndFlush(order);
			return orderResponse;
		}
		if (PayEnum.PAY1.getCode().equals(order.getStatus())) {
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("已提现成功，不允许重发 ");
			log.info("OrderNo{},提现申请返回,{}", req.getOrderNo(), JSONObject.toJSONString(orderResponse));
			return orderResponse;
		}
		log.info("验证提现定单");
		AccOrderEntity accOrderEntity = accOrderDao.findByOrderNo(order.getOrderNo());
		if (null != accOrderEntity) {
			if (AccOrderEntity.ORDERSTATUS_INIT.equals(accOrderEntity.getOrderStatus())) {
				orderResponse.setReturnCode(Constant.FAIL);
				orderResponse.setErrorMessage("提现申请失败，提现金额已冻结，不能在次发起申请 ");
				log.info("OrderNo{},提现申请返回,{}", req.getOrderNo(), JSONObject.toJSONString(orderResponse));
				return orderResponse;
			}
		}
		if (StringUtil.isEmpty(req.getBankCard())) {
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("银行卡号不能为空");
			order.setStatus(PayEnum.PAY2.getCode());
			order.setReturnCode(orderResponse.getReturnCode());
			order.setFailDetail(orderResponse.getErrorMessage());
			comOrderDao.saveAndFlush(order);
			return orderResponse;
		}
		String cardNo = DesPlus.encrypt(req.getBankCard().trim());
		ProtocolVo protocolVo = protocolDao.findOneByUserOidAndStatusAndCarNo(
				req.getUserOid(), ErrorDesEnum.ElELOCK.getCode(), cardNo);
		if (null == protocolVo) {
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("用户未绑定该银行卡");
			order.setStatus(PayEnum.PAY2.getCode());
			order.setReturnCode(orderResponse.getReturnCode());
			order.setFailDetail(orderResponse.getErrorMessage());
			comOrderDao.saveAndFlush(order);
			return orderResponse;
		}

		// 构建定单信息
		order.setRealName(protocolVo.getRealName());
		order.setBankCode(protocolVo.getBankName());
		order.setCardNo(protocolVo.getCardNo());
		orderResponse.setOrderNo(order.getOrderNo());
		try {
			AccountTransRequest accountTransRequest = new AccountTransRequest();
			accountTransRequest.setOrderNo(order.getOrderNo());
			accountTransRequest.setBalance(order.getAmount().add(order.getFee()));
			accountTransRequest.setSystemSource(order.getSystemSource());
			accountTransRequest.setUserOid(order.getUserOid());
			accountTransRequest.setOrderType(OrderTypeEnum.WITHDRAWALS.getCode());
			accountTransRequest.setUserType(order.getUserType());
			accountTransRequest.setFee(order.getFee());

			BaseResponse baseResponse = accountWithdrawalsService.withdrawals("FROZEN", accountTransRequest);
			if (!Constant.SUCCESS.equals(baseResponse.getReturnCode())) {
				log.info("提现申请，冻结账户失败，orderNo：{}",order.getOrderNo());
				order.setStatus(PayEnum.PAY2.getCode());
			}
			comOrderDao.saveAndFlush(order);
			orderResponse.setErrorMessage(baseResponse.getErrorMessage());
			orderResponse.setReturnCode(baseResponse.getReturnCode());
		} catch (Exception e) {
			log.error("定单处理异常orderNo:{},error:{}", req.getOrderNo(), e);
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("系统错误！");

			order.setStatus(PayEnum.PAY2.getCode());
			order.setReturnCode(orderResponse.getReturnCode());
			order.setFailDetail(orderResponse.getErrorMessage());
			comOrderDao.saveAndFlush(order);
		}
		log.info("提现申请返回：{}", JSONObject.toJSONString(orderResponse));
		return orderResponse;

	}
	/**
	 * 提现确认
	 * 
	 * @param req
	 * @return
	 */
	public OrderResponse confirmWthdrawal(OrderRequest req) {

		log.info("OrderNo{},提现确认接收订单,{}", req.getOrderNo(), JSONObject.toJSONString(req));
		OrderResponse orderResponse = new OrderResponse();
		orderResponse.setReturnCode(Constant.SUCCESS);
		BeanUtils.copyProperties(req, orderResponse);
		OrderVo order = comOrderDao.findByorderNo(req.getOrderNo());
		if (null == order) {
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("提现申请定单号不存在");
			log.info("OrderNo{},提现确认返回,{}", req.getOrderNo(), JSONObject.toJSONString(orderResponse));
			return orderResponse;
		}

		if (PayEnum.PAY1.getCode().equals(order.getStatus())) {
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("已提现成功，不允许重发 ");
			log.info("OrderNo{},提现确认返回,{}", req.getOrderNo(), JSONObject.toJSONString(orderResponse));
			return orderResponse;
		}
		
		if (PayEnum.PAY3.getCode().equals(order.getStatus())) {
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("订单处理中，不允许重发 ");
			log.info("OrderNo{},提现确认返回,{}", req.getOrderNo(), JSONObject.toJSONString(orderResponse));
			return orderResponse;
		}

		if (!order.getUserOid().equals(req.getUserOid().trim())) {
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("用户不是同一个，提现失败 ");
			log.info("OrderNo{},提现确认返回,{}", req.getOrderNo(), JSONObject.toJSONString(orderResponse));
			return orderResponse;
		}

		AccOrderEntity accOrderEntity = accOrderDao.findByOrderNo(order.getOrderNo());
		if (null == accOrderEntity) {
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("提现确认失败，提现申请无冻结记录 ");
			log.info("OrderNo{},提现确认返回,{}", req.getOrderNo(), JSONObject.toJSONString(orderResponse));
			return orderResponse;
		}
		
		if (!AccOrderEntity.ORDERSTATUS_INIT.equals(accOrderEntity.getOrderStatus())) {
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("提现确认失败， 冻结状态错误");
			log.info("OrderNo{},提现确认返回,{}", req.getOrderNo(), JSONObject.toJSONString(orderResponse));
			return orderResponse;
		}

		if (!TradeTypeEnum.trade_payee.getCode().equals(req.getType())) {
			orderResponse.setReturnCode(TradeEventCodeEnum.trade_1005.getCode());
			orderResponse.setErrorMessage(TradeEventCodeEnum.getEnumName(orderResponse.getReturnCode()));
			log.info("OrderNo{},提现确认返回,{}", req.getOrderNo(), JSONObject.toJSONString(orderResponse));
			return orderResponse;
		}
		//获取用户提现申请时绑定银行卡信息
		ProtocolVo protocolVo = protocolDao.findProtocolByUserOidAndCard(req.getUserOid(), order.getCardNo());
		if (null == protocolVo) {
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("用户绑卡信息不存在");
			log.info("OrderNo{},提现确认返回,{}", req.getOrderNo(), JSONObject.toJSONString(orderResponse));
			return orderResponse;
		}

		// 将提现审核订单放入redis中
		log.info("提现审核通过，增加redis缓存，防止重复提现，订单号:{}", order.getOrderNo());
		Long check = payTwoRedisUtil.setRedisByTime("wthdrawal_order_redis_tag" + order.getOrderNo(), order.getOrderNo());
		if (check.intValue() == 0) {
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("订单已提现确认，不能再次确认");
			return orderResponse;
		}

		log.info("验证用户余额..");
		AccountInfoEntity basicAccount = null;//用户基本户
		BigDecimal basicBalance = BigDecimal.ZERO;//用户基本户金额
		basicAccount = accountInfoDao.findAccountByAccountTypeAndUserOid(AccountTypeEnum.BASICER.getCode(), req.getUserOid().trim());
		if(null == basicAccount){
			log.error("用户{}基本账户不存在!", req.getUserOid());
			orderResponse.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			orderResponse.setErrorMessage("用户基本账户不存在");
			return orderResponse;
		}
		basicBalance=basicAccount.getBalance();
		if(basicBalance.compareTo(order.getAmount()) < 0){
			orderResponse.setReturnCode(Constant.BALANCEERROR);
			orderResponse.setErrorMessage("用户提现基本户余额不足");
			log.info("用户提现基本户余额不足!");
			return orderResponse;
		}
		log.info("用户余额：{}，提现金额{}",basicBalance,order.getAmount());
		order.setStatus(PayEnum.PAY3.getCode());
		try {
			accOrderEntity.setSubmitTime(new Timestamp(System.currentTimeMillis()));
			accOrderEntity.setUpdateTime(accOrderEntity.getSubmitTime());
			accOrderDao.saveAndFlush(accOrderEntity);
			payPushService.callPublishPayeeEvent(req, order, protocolVo);
		} catch (Exception e) {
			log.error("定单处理异常orderNo:{},error:{}", req.getOrderNo(), e);
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("系统错误！");

			order.setStatus(PayEnum.PAY2.getCode());
			order.setReturnCode(orderResponse.getReturnCode());
			order.setFailDetail(orderResponse.getErrorMessage());
		}
		comOrderDao.saveAndFlush(order);
		log.info("提现确认返回：{}", JSONObject.toJSONString(orderResponse));
		return orderResponse;

	}
	
	/**
	 * 提现解冻
	 * 
	 * @param req
	 * @return
	 */
	public OrderResponse unforzenUserWithdrawals(OrderRequest req) {

		log.info("OrderNo{},提现解冻接收订单,{}", req.getOrderNo(), JSONObject.toJSONString(req));
		OrderResponse orderResponse = new OrderResponse();
		BeanUtils.copyProperties(req, orderResponse);
		OrderVo order = comOrderDao.findByorderNo(req.getOrderNo());
		if(null == order ){
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("提现申请定单号不存在");
			log.info("OrderNo{},提现解冻返回,{}", req.getOrderNo(), JSONObject.toJSONString(orderResponse));
			return orderResponse;
		}
		
		if(PayEnum.PAY1.getCode().equals(order.getStatus())){
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("已提现成功，不允许解冻 ");
			log.info("OrderNo{},提现解冻返回,{}", req.getOrderNo(), JSONObject.toJSONString(orderResponse));
			return orderResponse;
		}
		if(!order.getUserOid().equals(req.getUserOid().trim())){
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("用户不是同一个，解冻失败 ");
			log.info("OrderNo{},提现解冻返回,{}", req.getOrderNo(), JSONObject.toJSONString(orderResponse));
			return orderResponse;
		}
		
		AccOrderEntity accOrderEntity = accOrderDao.findByOrderNo(order.getOrderNo());
		if (null == accOrderEntity) {
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("提现解冻失败，无冻结记录 ");
			log.info("OrderNo{},提现解冻返回,{}", req.getOrderNo(), JSONObject.toJSONString(orderResponse));
			return orderResponse;
		}
		
		if (!AccOrderEntity.ORDERSTATUS_INIT.equals(accOrderEntity.getOrderStatus())) {
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("提现解冻失败， 冻结状态错误");
			log.info("OrderNo{},提现解冻返回,{}", req.getOrderNo(), JSONObject.toJSONString(orderResponse));
			return orderResponse;
		}
		
		if(PayEnum.PAY3.getCode().equals(order.getStatus())){
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("交易处理中，不能解冻 ");
			log.info("OrderNo{},提现解冻返回,{}", req.getOrderNo(), JSONObject.toJSONString(orderResponse));
			return orderResponse;
		}
		
		try {
			AccountTransRequest accountTransRequest=new AccountTransRequest();
			accountTransRequest.setOrderNo(order.getOrderNo());
			accountTransRequest.setBalance(order.getAmount().add(order.getFee()));
			accountTransRequest.setSystemSource(order.getSystemSource());
			accountTransRequest.setUserOid(order.getUserOid());
			accountTransRequest.setOrderType(OrderTypeEnum.WITHDRAWALS.getCode());
			accountTransRequest.setUserType(order.getUserType());
			accountTransRequest.setFee(order.getFee());
			accountTransRequest.setOrderCreatTime(DateUtil.format(new Date(), DateUtil.datetimePattern));
			
			BaseResponse baseResponse =accountWithdrawalsService.withdrawals("UNFROZEN", accountTransRequest);
			if(Constant.SUCCESS.equals(baseResponse.getReturnCode())){
				order.setFailDetail("提现解冻成功");
			}else{
				order.setFailDetail("提现解冻失败");
			}
			//解冻成功或者失败 提现定单状态为失败
			order.setStatus(PayEnum.PAY2.getCode());
			comOrderDao.saveAndFlush(order);
			
			orderResponse.setErrorMessage(baseResponse.getErrorMessage());
			orderResponse.setReturnCode(baseResponse.getReturnCode());
		} catch (Exception e) {
			log.error("定单解冻处理异常orderNo:{},error:{}", req.getOrderNo(), e);
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("提现解冻系统错误！");
			order.setReturnCode(orderResponse.getReturnCode());
			order.setFailDetail(orderResponse.getErrorMessage());
			comOrderDao.saveAndFlush(order);
		}
		log.info("提现解冻返回：{}", JSONObject.toJSONString(orderResponse));
		return orderResponse;

	}
	/**
	 * 调用三方付款
	 * @param
	 */
	@SuppressWarnings("unused")
	private OrderResponse callPublishPayeeEvent(OrderRequest req, OrderVo orderVo, ProtocolVo protocolVo,
			OrderResponse orderResponse) {
		log.info("提现交易 OrderRequest:{}", JSONObject.toJSONString(req));
		Timestamp time = new Timestamp(System.currentTimeMillis());
		// 创建交互日志
		String orderNo = orderVo.getOrderNo();
		// 创建支付记录
		PaymentVo paymentVo= paymentDao.findByOrderNo(orderNo);
		if (null == paymentVo) {
			paymentVo=new PaymentVo();
			paymentVo.setCommandStatus(PayEnum.PAY0.getCode());
			paymentVo.setOrderNo(orderNo);
			paymentVo.setAmount(orderVo.getAmount());
			paymentVo.setCardNo(orderVo.getCardNo());
			paymentVo.setPlatformAccount(orderVo.getCardNo());
			paymentVo.setType(orderVo.getType());
			paymentVo.setCreateTime(time);
			paymentVo.setUpdateTime(time);
			paymentVo.setUserOid(orderVo.getUserOid());
			paymentVo.setRealName(orderVo.getRealName());
			paymentVo.setUserType(orderVo.getUserType());
			paymentVo.setPhone(orderVo.getPhone());
			paymentVo.setUpTime(orderVo.getCreateTime());
			//设置后台直接支付
			paymentVo.setLaunchplatform(PayEnum.PAYMETHOD2.getCode());
			/**
			 * 添加省 市
			 */
			paymentVo.setAccountCity(req.getInAcctCityName());
			paymentVo.setAccountProvince(req.getInAcctProvinceCode());
			paymentVo = paymentDao.saveAndFlush(paymentVo);
		}else{
			if(PayEnum.PAY1.getCode().equals(paymentVo.getCommandStatus())){
				orderResponse.setReturnCode(Constant.WITHHOLD_SUCCESSED);
				orderResponse.setErrorMessage("已提现成功，不能重复提现");
				return orderResponse;
			}
		}

		String payNo = seqGenerator.next(PayEnum.PAYTYPE01.getCode());
		if("yes".equals(withOutThirdParty)){
			int length = orderVo.getOrderNo().length();
			payNo=payNo+orderVo.getOrderNo().substring(length-1, length);
		}
		BankLogVo bankLogVo = new BankLogVo();
		bankLogVo.setOrderNo(orderVo.getOrderNo());
		bankLogVo.setUserOid(orderVo.getUserOid());
		bankLogVo.setPayNo(payNo);
		bankLogVo.setOperatorTime(time);
		bankLogVo.setCreateTime(time);
		bankLogVo.setUpdateTime(time);
		bankLogVo.setTradStatus(PayEnum.PAY0.getCode());
		bankLogVo.setAmount(orderVo.getAmount());
		bankLogVo.setType(orderVo.getType());
		bankLogVo.setSheetId(paymentVo.getOid());
		bankLogDao.saveAndFlush(bankLogVo);
		// 重新赋值
		paymentVo.setPayNo(payNo);

		OrderEvent orderEvent = new OrderEvent();
		orderEvent.setOrderNo(orderVo.getOrderNo());
		orderEvent.setPayNo(bankLogVo.getPayNo());
		orderEvent.setAmount(orderVo.getAmount());
		orderEvent.setRealName(orderVo.getRealName());
		orderEvent.setCustAccountId(DesPlus.decrypt(protocolVo.getCertificateNo()));
		orderEvent.setLaunchplatform(paymentVo.getLaunchplatform());
		orderEvent.setSourceType(orderVo.getSystemSource());
		orderEvent.setTradeType(orderVo.getType());
		orderEvent.setUserOid(orderVo.getUserOid());
		// 添加省 市
		orderEvent.setInAcctProvinceCode(paymentVo.getAccountProvince());
		orderEvent.setInAcctCityName(paymentVo.getAccountCity());
		orderEvent.setCardNo(DesPlus.decrypt(orderVo.getCardNo()));
		
		log.info("提现交易推送三方orderEvent:{}", JSONObject.toJSONString(orderEvent));
		event.publishEvent(orderEvent);
		log.info("提现交易推送三方返回orderEvent:{}", JSONObject.toJSONString(orderEvent));
		if (!StringUtil.isEmpty(orderEvent.getHostFlowNo())) {
			paymentVo.setHostFlowNo(orderEvent.getHostFlowNo());
		}
		if (orderEvent.getReturnCode().equals(Constant.SUCCESS)) {
			// 设置为处理中
			paymentVo.setCommandStatus(PayEnum.PAY3.getCode());
			paymentVo.setBankReturnSeriNo(orderEvent.getHostFlowNo()); // 回写银行流水号
			bankLogVo.setBankReturnSerialId(orderEvent.getHostFlowNo());
		
		} else if (orderEvent.getReturnCode().equals(Constant.INPROCESS)) {
			paymentVo.setCommandStatus(PayEnum.PAY4.getCode());
		} else if (orderEvent.getReturnCode().equals(TradeEventCodeEnum.trade_1007.getCode())) {
			log.info("人工处理，设置为未处理状态，orderNo:{},money:{}", orderVo.getOrderNo(), orderVo.getAmount());
			// 交易设置为处理中，返回业务系统为成功，等人工处理后回调
			paymentVo.setCommandStatus(PayEnum.PAY0.getCode());
			paymentVo.setLaunchplatform(PayEnum.PAYMETHOD2.getCode());// 20170209修改为后台发起订单，走申请提现审核
			orderEvent.setErrorDesc(TradeEventCodeEnum.trade_1007.getName());
		} else {
			paymentVo.setCommandStatus(PayEnum.PAY2.getCode());
		}

		// 当回调过快时，回调线程已把定单状态修改为成功，如果有此情况说明已支付成功，这里获取支付成功状态
		if (PayEnum.PAY1.getCode().equals(orderVo.getStatus())) {
			log.info("定单{}优先回调成功", orderVo.getOrderNo());
			paymentVo.setCommandStatus(PayEnum.PAY1.getCode());
		}

		// 修改收单
		orderVo.setStatus(paymentVo.getCommandStatus());
		orderVo.setFailDetail(orderEvent.getErrorDesc());
		orderVo.setChannel(orderEvent.getChannel());
		orderVo.setBankReturnSeriNo(orderEvent.getHostFlowNo());
		orderVo.setPayNo(bankLogVo.getPayNo());
		orderVo.setReturnCode(orderEvent.getReturnCode());
		comOrderDao.save(orderVo);

		// 修改支付记录
		paymentVo.setFailDetail(orderEvent.getErrorDesc());
		paymentVo.setChannelNo(orderEvent.getChannel());
		paymentVo.setType(orderVo.getType());
		paymentVo.setEmergencyMark(orderEvent.getEmergencyMark());
		paymentVo.setCrossFlag(orderEvent.getCrossFlag());
		paymentVo.setPlatformAccount(orderEvent.getPlatformAccount());
		paymentVo.setPlatformName(orderEvent.getPlatformName());
		paymentVo.setPayAddress(orderEvent.getPayAddress());
		paymentDao.save(paymentVo);

		// 修改交互日志
		bankLogVo.setTradStatus(paymentVo.getCommandStatus());
		bankLogVo.setErrorCode(orderEvent.getReturnCode());
		bankLogVo.setFailDetail(orderEvent.getErrorDesc());
		bankLogDao.save(bankLogVo);
		callbackDao.updateByPayNo(orderEvent.getChannel(), bankLogVo.getPayNo());

		orderResponse.setStatus(paymentVo.getCommandStatus());
		orderResponse.setAmount(paymentVo.getAmount());
		orderResponse.setPayNo(paymentVo.getPayNo());
		orderResponse.setOrderNo(paymentVo.getOrderNo());
		orderResponse.setErrorMessage(orderEvent.getErrorDesc());
		orderResponse.setReturnCode(orderEvent.getReturnCode());
		
		log.info("提现交易返回orderResponse:{}", JSONObject.toJSONString(orderResponse));
		return orderResponse;
	}

//	
//	private void saveAccountOrder(OrderRequest req, OrderVo order) {
//		AccOrderEntity orderEntity = new AccOrderEntity();
//		orderEntity.setBalance(order.getAmount().add(req.getFee()));
//		orderEntity.setOrderNo(order.getOrderNo());
//		orderEntity.setFee(req.getFee());
//		orderEntity.setOrderStatus(order.getStatus());
//		orderEntity.setUserOid(order.getUserOid());
//		orderEntity.setPhone(order.getPhone());
//		orderEntity.setOrderType(OrderTypeEnum.WITHDRAWALS.getCode());
//		orderEntity.setSystemSource(order.getSystemSource());
//		orderEntity.setUserType(UserTypeEnum.INVESTOR.getCode());
//		orderEntity.setSystemSource("mimosa");
//		accOrderDao.save(orderEntity);
//		
//	}

	private OrderVo saveReqOrder(OrderRequest req) {
		OrderVo orderVo = comOrderDao.findByorderNo(req.getOrderNo());
		if (null == orderVo) {
			try {
				orderVo=new OrderVo();
				Timestamp time = new Timestamp(System.currentTimeMillis());
				BeanUtils.copyProperties(req, orderVo);
				orderVo.setPayNo(StringUtil.isEmpty(req.getPayNo()) ? "" : req.getPayNo());
				orderVo.setStatus(PayEnum.PAY0.getCode());
				orderVo.setReceiveTime(time);
				orderVo.setCreateTime(time);
				orderVo.setUpdateTime(time);
				UserInfoEntity user = userInfoDao.findByUserOid(orderVo.getUserOid());
				if (null != user) {
					orderVo.setPhone(user.getPhone());
				}
				orderVo = comOrderDao.saveAndFlush(orderVo);
			} catch (Exception e) {
				log.error("保存提现申请定单异常，定单号:{} 异常信息 :{}", req.getOrderNo(), e);
				return null;
			}
		}
		return orderVo;
	}

	public static void main(String[] args) {
		String returnCode = Constant.FAIL;
		
		if(!StringUtil.in(returnCode, Constant.SUCCESS,Constant.INPROCESS)){
			System.out.println("SUCCESS");
		}
		System.out.println("FAIL");
	}
}