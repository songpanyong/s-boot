package com.guohuai.boot.pay.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.ErrorDesEnum;
import com.guohuai.boot.PayEnum;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.service.UserInfoService;
import com.guohuai.boot.pay.CertificatesTypeEnum;
import com.guohuai.boot.pay.dao.ComOrderDao;
import com.guohuai.boot.pay.dao.ProtocolDao;
import com.guohuai.boot.pay.vo.ChannelBankVo;
import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.boot.pay.vo.ProtocolVo;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.DesPlus;
import com.guohuai.component.util.TradeTypeEnum;
import com.guohuai.payadapter.component.TradeChannel;
import com.guohuai.payadapter.component.TradeEventCodeEnum;
import com.guohuai.payadapter.control.Channel;
import com.guohuai.payadapter.control.ChannelDao;
import com.guohuai.payadapter.listener.event.AuthenticationEvent;
import com.guohuai.payadapter.listener.event.TradeEvent;
import com.guohuai.seetlement.listener.event.OrderPayOrPayeeEvent;
import com.guohuai.settlement.api.request.ElementValidationRequest;
import com.guohuai.settlement.api.request.FindBindRequest;
import com.guohuai.settlement.api.request.OrderRequest;
import com.guohuai.settlement.api.response.ElementValidaResponse;
import com.guohuai.settlement.api.response.FindBindResponse;
import com.guohuai.settlement.api.response.FindBindResponse.BankCard;
import com.guohuai.settlement.api.response.OrderResponse;

@Service
public class EnterpriseWithholdingService {

	private final static Logger log = LoggerFactory.getLogger(EnterpriseWithholdingService.class);
	
	@Autowired
	private ComOrderDao comOrderDao;
	
	@Autowired
	private ApplicationEventPublisher event;
	
	@Autowired
	private ProtocolDao protocolDao;
	
	@Autowired
	private UserInfoService userInfoService;

	@Autowired
	private SeqGenerator seqGenerator;
	
	@Autowired
	ChannelDao channelDao;
	
	@Autowired
	private UserInfoDao userInfoDao;
	
	@Autowired
	ComChannelBankService channelBankService;
	
	public String genSn() {
		String sn = this.seqGenerator.next("ELEM");
		return sn;
	}
	
	/**
	 * 企业绑卡
	 * @param req
	 * @return
	 */
	public ElementValidaResponse bindCard(ElementValidationRequest req) {

		Timestamp time = new Timestamp(System.currentTimeMillis());
		ElementValidaResponse resp = new ElementValidaResponse();
		resp.setReturnCode(Constant.FAIL);
		resp.setRequestNo(req.getRequestNo());
		UserInfoEntity userInfo = userInfoService.getAccountUserByUserOid(req.getUserOid());
		if(userInfo == null){
			resp.setErrorMessage("用户不存在");
			return resp;
		}
		String encryptCardNo = DesPlus.encrypt(req.getCardNo().trim());
		ProtocolVo protocol = protocolDao.findOneByUserOid(req.getUserOid(), encryptCardNo,
				ErrorDesEnum.ElELOCK.getCode());
		if (null != protocol) {
			resp.setErrorMessage("该银行卡您已经绑定，请更换银行卡");
			return resp;
		}
		Channel channel = channelDao.queryChannel(TradeChannel.baofoopayee.getValue());
		if(null==channel) {
			resp.setErrorMessage("支付渠道为空，请配置支付渠道");
			return resp;
		}
		List<ChannelBankVo> channelBankList = channelBankService.findChannelBank(req.getCardNo().trim(),
				channel.getChannelNo());
		if (CollectionUtils.isEmpty(channelBankList)) {
			resp.setErrorMessage("不支持银行卡或未把银行加入到通道配置");
			return resp;
		}
		log.info("渠道银行信息,{}", JSONObject.toJSONString(channelBankList.get(0)));
		try {
			ProtocolVo protocolVo = new ProtocolVo();
			protocolVo.setOid(StringUtil.uuid());
			protocolVo.setCardNo(encryptCardNo);
			if(!StringUtil.isEmpty(req.getCertificateNo())) {
				protocolVo.setCertificateNo(DesPlus.encrypt(req.getCertificateNo().trim()));
			}
			protocolVo.setUserOid(req.getUserOid());
			protocolVo.setUpdateTime(time);
			protocolVo.setCreateTime(time);
			protocolVo.setProvince(req.getProvince());
			protocolVo.setCity(req.getCity());
			protocolVo.setCounty(req.getCounty());
			protocolVo.setBranch(req.getBranch());
			protocolVo.setCardType(req.getCardType());
			protocolVo.setBankName(req.getBankName());
			protocolVo.setRealName(req.getRealName());
			if (StringUtil.isEmpty(req.getBankName())) {
				protocolVo.setBankName(channelBankList.get(0).getChannelbankName());
			}
			protocolVo.setAccountBankType(PayEnum.ELEMENT_ENTERPRISE.getCode());
			protocolVo.setStatus(ErrorDesEnum.ElELOCK.getCode());
			protocolVo.setCertificates(CertificatesTypeEnum.IDCARD.getCode());
			protocolVo.setCardType("企业");
			protocolVo.setUserType(userInfo.getUserType());
			protocolDao.save(protocolVo);

			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("绑卡成功");
		} catch (Exception e) {
			log.error("绑卡失败，cardNo:{},e:{}", req.getCardNo(), e);
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("绑卡失败");
		}
		
		if(Constant.SUCCESS.equals(resp.getReturnCode())){
			userInfo.setName(req.getRealName());
			userInfoDao.saveAndFlush(userInfo);
		}
		resp.setCardNo(req.getCardNo());
		resp.setCertificateNo(req.getCertificateNo());
		resp.setRequestNo(req.getRequestNo());
		resp.setCertificates(req.getCertificates());
		return resp;
	}
	
	/**
	 * 宝付直接绑卡
	 */
	public AuthenticationEvent baofooBind(ElementValidationRequest req) {
		log.info("宝付绑直接卡 baofooElement");
		AuthenticationEvent authenticationEvent = new AuthenticationEvent();
		authenticationEvent.setCardNo(req.getCardNo());
		authenticationEvent.setBankCode(req.getBankCode());
		authenticationEvent.setIdentityNo(req.getCertificateNo());
		authenticationEvent.setMobileNum(req.getPhone());
		authenticationEvent.setUserName(req.getRealName());
		authenticationEvent.setTradeType("baofoobindCard");
		authenticationEvent.setOrderId(seqGenerator.next("BA"));
		event.publishEvent(authenticationEvent);
		return authenticationEvent;
	}
	
	/**
	 * 企业解绑
	 * @param req
	 * @return
	 */
	@SuppressWarnings("unused")
	public ElementValidaResponse unbundling(ElementValidationRequest req) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		log.info("{},解除实名支付银行卡,{}", req.getUserOid(), JSONObject.toJSONString(req));
		ElementValidaResponse elementValidaResponse = new ElementValidaResponse();
		if (StringUtil.isEmpty(req.getCardNo())) {
			elementValidaResponse.setErrorMessage("请输入银行卡号！");
			elementValidaResponse.setReturnCode(Constant.FAIL);
		}
		// 查询用户是否绑过
		ProtocolVo protocol = protocolDao.findOneByUserOid(req.getUserOid(), 
				DesPlus.encrypt(req.getCardNo()),PayEnum.ERRORCODE1.getCode());
		if (protocol != null) {
			
			int a = protocolDao.updateProtocolBytatus(ErrorDesEnum.ELEUNLOCK.getCode(),req.getUserOid(),DesPlus.encrypt(req.getCardNo()));
			elementValidaResponse.setErrorMessage("解绑成功");
			elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
			elementValidaResponse.setReturnCode(Constant.SUCCESS);
			return elementValidaResponse;
		}else {
			ProtocolVo unPro = protocolDao.findOneByUserOid(req.getUserOid(), DesPlus.encrypt(req.getCardNo()),ErrorDesEnum.ELEUNLOCK.getCode());
			if (unPro != null) {
				elementValidaResponse.setErrorMessage("卡号已解绑,不能再解绑！");
				elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
				elementValidaResponse.setReturnCode(Constant.SUCCESS);
				log.info("四要素解绑返回页面端数据：{}", JSONObject.toJSONString(elementValidaResponse));
				return elementValidaResponse;
			}
			elementValidaResponse.setErrorMessage("您未绑卡成功或解绑卡号不正确，解绑失败！");
			elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
			elementValidaResponse.setReturnCode(Constant.FAIL);
			log.info("四要素解绑返回页面端数据：{}", JSONObject.toJSONString(elementValidaResponse));
			return elementValidaResponse;
		}
	}
	
	/**
	 * 宝付解除绑卡
	 *//*
	public Boolean baofooUbindCard(ElementValidationRequest req, String bindId) {
		log.info("宝付解除绑卡 baofooQueryElement");
		if (StringUtil.isEmpty(bindId)) {
			return true;
		} else {
			AuthenticationEvent authenticationEvent = new AuthenticationEvent();
			authenticationEvent.setBindId(bindId);
			authenticationEvent.setTradeType("baofooubindCard");
			authenticationEvent.setOrderId(seqGenerator.next("BA"));
			event.publishEvent(authenticationEvent);
			if ("0000".equals(authenticationEvent.getReturnCode())) {
				return true;
			} else {
				return false;
			}
		}
	}*/
	
	/**
	 * 
	 * 代扣
	 * @param req
	 * @return
	 */
	public OrderResponse baofuWithholding(OrderRequest req) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		log.info("{},企业代扣,{}", req.getUserOid(), JSONObject.toJSONString(req));
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
		ProtocolVo protocolVo = protocolDao.findOneByUserOidAndStatus(req.getUserOid(), ErrorDesEnum.ElELOCK.getCode());
		if (null == protocolVo) {
			orderResponse.setReturnCode(TradeEventCodeEnum.trade_1006.getCode());
			orderResponse.setErrorMessage(TradeEventCodeEnum.trade_1006.getName());
			return orderResponse;
		}
		OrderVo orderVo = new OrderVo();
		// 创建订单记录
		try {
			BeanUtils.copyProperties(req, orderVo);
			orderVo.setPayNo(StringUtil.isEmpty(req.getPayNo()) ? "" : req.getPayNo());
			orderVo.setStatus(PayEnum.PAY0.getCode());
			orderVo.setCardNo(protocolVo.getCardNo());
			orderVo.setRealName(protocolVo.getRealName());
			orderVo.setBankCode(protocolVo.getBankName());
			orderVo.setReceiveTime(time);
			orderVo.setCreateTime(time);
			orderVo.setUpdateTime(time);
//					orderVo.setUserType(req.getUserType());
			log.info("创建定单 orderVo = [{}]", JSONObject.toJSON(orderVo));
			comOrderDao.save(orderVo);
		} catch (Exception e) {
			log.error("定单处理异常orderNo:{},error:{}", req.getOrderNo(), e);
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("创建代扣订单定单失败，系统繁忙！");
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
			withoiding(req,protocolVo);
		} catch (Exception e) {
			log.error("推送异步支付异常 orderNo:{},error:{}", req.getOrderNo(), e);
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("推送异步支付异常，系统繁忙！");
		}
		return orderResponse;
	}
	/**
	 * 宝付dui对公代扣
	 */
	public Boolean  withoiding(OrderRequest req,ProtocolVo protocolVo) {
		log.info("宝付对公代扣withoiding");
		
		TradeEvent authenticationEvent = new TradeEvent();
		authenticationEvent.setPayNo(req.getOrderNo());
		authenticationEvent.setBankCode(req.getPayCode());
		authenticationEvent.setPlatformName(req.getPlatformName());
		authenticationEvent.setCardNo(protocolVo.getCardNo());
		authenticationEvent.setAccountProvince(req.getInAcctProvinceCode());
		authenticationEvent.setAccountCity(req.getInAcctCityName());
		authenticationEvent.setAccountDept(req.getBranch());
		authenticationEvent.setCertificate_no(req.getCertificateNo());
		authenticationEvent.setAmount(req.getAmount().toString());
		event.publishEvent(authenticationEvent);
		if ("0000".equals(authenticationEvent.getReturnCode())) {
			return true;
		} else {
			return false;
		}
	}
	/**
	 * 查询绑定的银行卡列表
	 * @param request
	 * @return
	 */
	@SuppressWarnings("unused")
	public FindBindResponse findBindCard (@RequestBody FindBindRequest request) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		log.info("{},查询实名绑定的银行卡,{}", request.getUserOid(), JSONObject.toJSONString(request));
		// 设置返回信息
		FindBindResponse orderResponse = new FindBindResponse();
		BeanUtils.copyProperties(request, orderResponse);
		List<FindBindResponse.BankCard> list = new ArrayList<>();
		// 是否四要素验证成功
		List<ProtocolVo> protocolVo = protocolDao.findListByUserOidAndStatus(request.getUserOid(), ErrorDesEnum.ElELOCK.getCode());
		if (CollectionUtils.isEmpty(protocolVo)) {
			orderResponse.setReturnCode(TradeEventCodeEnum.trade_1006.getCode());
			orderResponse.setErrorMessage(TradeEventCodeEnum.trade_1006.getName());
			return orderResponse;
		}else {
			for (ProtocolVo protocolVo2 : protocolVo) {
				BankCard card = new FindBindResponse.BankCard();
				card.setBankName(protocolVo2.getBankName());
				card.setBranch(protocolVo2.getBranch());
				card.setCardNo(DesPlus.decrypt(protocolVo2.getCardNo()));
				card.setProtocolNo(protocolVo2.getProtocolNo());
				card.setRealName(protocolVo2.getRealName());
				card.setStatus(protocolVo2.getStatus());
				card.setUserOid(protocolVo2.getUserOid());
				list.add(card);
			}
			
			orderResponse.setReturnCode(Constant.SUCCESS);
			orderResponse.setErrorMessage("查询成功");
			orderResponse.setBankCards(list);
			return orderResponse;
		}
	}
}
