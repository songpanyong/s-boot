/**
 * 
 */
package com.guohuai.boot.pay.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.PayEnum;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.service.AccountInfoService;
import com.guohuai.boot.account.service.TransService;
import com.guohuai.boot.pay.dao.BankLogDao;
import com.guohuai.boot.pay.dao.ComOrderDao;
import com.guohuai.boot.pay.dao.PaymentDao;
import com.guohuai.boot.pay.vo.BankLogVo;
import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.boot.pay.vo.PaymentVo;
import com.guohuai.boot.pay.vo.ProtocolVo;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.DesPlus;
import com.guohuai.payadapter.component.TradeEventCodeEnum;
import com.guohuai.payadapter.control.ChannelDao;
import com.guohuai.payadapter.listener.event.OrderEvent;
import com.guohuai.payadapter.redeem.CallBackDao;
import com.guohuai.settlement.api.SettlementSdk;
import com.guohuai.settlement.api.request.OrderRequest;
import com.guohuai.settlement.api.response.OrderResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 支付推送
 * @author xueyunlong 
 *
 */
@Slf4j
@Component
public class PayPushService {


	/**
	 * 调用三方付款
	 * @param req
	 * @param orderVo
	 * @param protocolVo
	 */
	@Async("wthdrawalAsync")
	public void callPublishPayeeEvent(OrderRequest req, OrderVo orderVo, ProtocolVo protocolVo) {
		OrderResponse orderResponse =new OrderResponse();
		log.info("提现交易 OrderRequest:{}", JSONObject.toJSONString(req));
		Timestamp time = new Timestamp(System.currentTimeMillis());
		// 创建交互日志
		String orderNo = orderVo.getOrderNo();
		// 创建支付记录
		PaymentVo paymentVo= paymentDao.findByOrderNo(orderNo);
		
		if(null!=paymentVo){
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("该订单已处理，不能重复提现");
			log.error("该订单{}已处理，不能重复提现",paymentVo.getOrderNo());
			return;
		}else{
			paymentVo=new PaymentVo();
			paymentVo.setCommandStatus(PayEnum.PAY0.getCode());
			paymentVo.setOrderNo(orderNo);
			paymentVo.setAmount(orderVo.getAmount());
			paymentVo.setCardNo(protocolVo.getCardNo());
			paymentVo.setPlatformAccount(protocolVo.getCardNo());
			paymentVo.setType(orderVo.getType());
			paymentVo.setCreateTime(time);
			paymentVo.setUpdateTime(time);
			paymentVo.setUserOid(orderVo.getUserOid());
			paymentVo.setRealName(protocolVo.getRealName());
			paymentVo.setUserType(orderVo.getUserType());
			paymentVo.setPhone(protocolVo.getPhone());
			paymentVo.setUpTime(orderVo.getCreateTime());
			//设置后台直接支付
			paymentVo.setLaunchplatform(PayEnum.PAYMETHOD2.getCode());
			/**
			 * 添加省 市
			 */
			paymentVo.setAccountCity(req.getInAcctCityName());
			paymentVo.setAccountProvince(req.getInAcctProvinceCode());
			paymentVo = paymentDao.saveAndFlush(paymentVo);
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
		paymentDao.save(paymentVo);
		
		OrderEvent orderEvent = new OrderEvent();
		orderEvent.setOrderNo(orderVo.getOrderNo());
		orderEvent.setPayNo(bankLogVo.getPayNo());
		orderEvent.setAmount(orderVo.getAmount());
		orderEvent.setRealName(protocolVo.getRealName());
		if (!StringUtil.isEmpty(protocolVo.getCertificateNo())) {
			orderEvent.setCustAccountId(DesPlus.decrypt(protocolVo.getCertificateNo()));
		}
		orderEvent.setLaunchplatform(paymentVo.getLaunchplatform());
		orderEvent.setSourceType(orderVo.getSystemSource());
		orderEvent.setTradeType(orderVo.getType());
		orderEvent.setUserOid(orderVo.getUserOid());
		// 添加省 市支行  改为从协议表中取 2017/08/19
		orderEvent.setInAcctProvinceCode(protocolVo.getProvince());
		orderEvent.setInAcctCityName(protocolVo.getCity());
		orderEvent.setBranchBankName(protocolVo.getBranch());
		
		orderEvent.setCardNo(DesPlus.decrypt(protocolVo.getCardNo()));
		//先锋支付时，需要区分对公还是对私，用到此字段
		orderEvent.setAccountType(protocolVo.getAccountBankType());
		log.info("提现交易推送三方orderEvent:{}", JSONObject.toJSONString(orderEvent));
		event.publishEvent(orderEvent);
		log.info("提现交易推送三方返回orderEvent:{}", JSONObject.toJSONString(orderEvent));
		if (!StringUtil.isEmpty(orderEvent.getHostFlowNo())) {
			paymentVo.setHostFlowNo(orderEvent.getHostFlowNo());
		}
		if (StringUtil.in(orderEvent.getReturnCode(),Constant.SUCCESS,Constant.INPROCESS)) {
			// 设置为处理中
			paymentVo.setCommandStatus(PayEnum.PAY3.getCode());
			paymentVo.setBankReturnSeriNo(orderEvent.getHostFlowNo()); // 回写银行流水号
			bankLogVo.setBankReturnSerialId(orderEvent.getHostFlowNo());
		}else if (TradeEventCodeEnum.trade_1007.getCode().equals(orderEvent.getReturnCode())) {
			log.info("人工处理，设置为未处理状态，orderNo:{},money:{}", orderVo.getOrderNo(), orderVo.getAmount());
			// 交易设置为处理中，返回业务系统为成功，等人工处理后回调
			paymentVo.setCommandStatus(PayEnum.PAY0.getCode());
			paymentVo.setLaunchplatform(PayEnum.PAYMETHOD2.getCode());// 20170209修改为后台发起订单，走申请提现审核
			orderEvent.setErrorDesc(TradeEventCodeEnum.trade_1007.getName());
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
		comOrderDao.save(orderVo);

		// 修改支付记录
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
		//异步处理，如果不是成功和超时，（所有同步调用银行、者适配器处理错误、找不到渠道等）都回调业务系统
		if(!StringUtil.in(orderResponse.getReturnCode(), Constant.SUCCESS,Constant.INPROCESS)){
			pushResult(orderResponse);
		}
		log.info("提现交易返回orderResponse:{}", JSONObject.toJSONString(orderResponse));
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
		log.info("交易实时回调……,支付订单号{},map:{}",orderResponse.getPayNo(),JSONObject.toJSONString(map));
		paymentService.noticUrl(map);
	}
	
	@Autowired
	private ComOrderDao comOrderDao;
	@Autowired
	private PaymentDao paymentDao;
	@Autowired
	private BankLogDao bankLogDao;
	@Autowired
	private ApplicationEventPublisher event;
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
	private CallBackDao callbackDao;
	
	@Autowired
	UserInfoDao userInfoDao;
	@Value("${withOutThirdParty:no}")
	private String withOutThirdParty;
	
}
