package com.guohuai.boot.pay.service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.basic.component.ext.web.BaseResp;
import com.guohuai.boot.ErrorDesEnum;
import com.guohuai.boot.PayEnum;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.pay.CertificatesTypeEnum;
import com.guohuai.boot.pay.dao.*;
import com.guohuai.boot.pay.form.ElementValidationForm;
import com.guohuai.boot.pay.res.ElementValidationRes;
import com.guohuai.boot.pay.vo.ChannelBankVo;
import com.guohuai.boot.pay.vo.ChannelVo;
import com.guohuai.boot.pay.vo.ElementValidationVo;
import com.guohuai.boot.pay.vo.ProtocolVo;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.DesPlus;
import com.guohuai.component.util.UserTypeEnum;
import com.guohuai.component.util.sms.SMSTypeEnum;
import com.guohuai.component.util.sms.SendSMSUtils;
import com.guohuai.payadapter.bankutil.BankUtilEntity;
import com.guohuai.payadapter.bankutil.BankUtilService;
import com.guohuai.payadapter.component.TradeChannel;
import com.guohuai.payadapter.component.TradeEventCodeEnum;
import com.guohuai.payadapter.component.TradeType;
import com.guohuai.payadapter.control.Channel;
import com.guohuai.payadapter.control.ChannelDao;
import com.guohuai.payadapter.listener.event.AuthenticationEvent;
import com.guohuai.settlement.api.request.ElementValidationRequest;
import com.guohuai.settlement.api.request.UserProtocolRequest;
import com.guohuai.settlement.api.response.BaseResponse;
import com.guohuai.settlement.api.response.ElementValidaResponse;
import com.guohuai.settlement.api.response.ElementValidaRulesResponse;
import com.guohuai.settlement.api.response.UserProtocolResponse;
import com.guohuai.settlement.api.response.entity.ProtocolDTO;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElementValidationService {
	private final static Logger log = LoggerFactory.getLogger(ElementValidationService.class);
	@Autowired
	private ElementValidationDao elementValidationDao;
	@Autowired
	private ApplicationEventPublisher event;
	
	private String tradeType;
	
	@Value("#jytpay.elementTradeType:test")
	private String testTradeType;
	
	@Autowired
	private ProtocolDao protocolDao;

	@Autowired
	private UserInfoDao userInfoDao;

	@Autowired
	private SeqGenerator seqGenerator;
	@Autowired
	private BankUtilService bankUtilService;

	@Autowired
	ChannelDao channelDao;
	@Autowired
	ComChannelDao comChannelDao;
	@Autowired
	ComChannelBankDao comChannelBankDao;
	
	@Autowired
	ComChannelBankService channelBankService;
	
	@Autowired
	private ElementValidationService elementService;
	
	@Autowired
	private PaymentDao paymentDao;
	
	@Autowired
	private AccOrderDao accOrderDao;
	
	@Autowired
	private SendSMSUtils sendSMSUtils;
	@Value("${user.bindCard.max.number:20}")
	private String userMaxBindNum;//用户最大绑卡数量
	@Value("${card.bindUser.max.number:20}")
	private String cardMaxBindNum;//卡最大绑定用户数量
	@Value("${user.bindOtherUserCard.switch:Y}")
	private String bindOtherUserCard;//用户是否可绑定其他用户卡
	
	public String genSn() {
		String sn = this.seqGenerator.next("ELEM");
		return sn;
	}

	/**
	 * 二版四要素验证【目前使用这个】
	 */
	@SuppressWarnings("unused")
	public ElementValidaResponse elementWithSms(ElementValidationRequest req) {
		long timel = System.currentTimeMillis();
		Timestamp time = new Timestamp(timel);
		log.info("{},四要素验证,{}", req.getUserOid(), JSONObject.toJSONString(req));
		ElementValidaResponse elementValidaResponse = new ElementValidaResponse();
		elementValidaResponse.setRequestNo(req.getRequestNo());
		String desCardNo = DesPlus.encrypt(req.getCardNo().trim()),
				desCertNo = DesPlus.encrypt(req.getCertificateNo().trim());
		String bankName = "";
		BankUtilEntity bank=null;
		try{
			bank =  bankUtilService.getBankByCard(req.getCardNo().trim());
			if(null == bank){
				elementValidaResponse.setReturnCode(TradeEventCodeEnum.trade_1015.getCode());
				elementValidaResponse.setErrorMessage(TradeEventCodeEnum.getEnumName(TradeEventCodeEnum.trade_1015.getCode()));
				log.info(elementValidaResponse.getErrorMessage());
				return elementValidaResponse;
			}
			bankName = bank.getBankName();
		}catch(Exception e){
			e.printStackTrace();
			log.error("四要素验证,获取银行编码异常");
			elementValidaResponse.setReturnCode(TradeEventCodeEnum.trade_1015.getCode());
			elementValidaResponse.setErrorMessage(TradeEventCodeEnum.getEnumName(TradeEventCodeEnum.trade_1015.getCode()));
			log.error(elementValidaResponse.getErrorMessage());
			return elementValidaResponse;
		}
		Object[] channels=channelDao.queryChannels(bank.getBankCode());
		if (null == channels || channels.length == 0) {
			elementValidaResponse.setReturnCode(TradeEventCodeEnum.trade_1009.getCode());
			elementValidaResponse.setErrorMessage(TradeEventCodeEnum.getEnumName(TradeEventCodeEnum.trade_1009.getCode()));
			log.info(elementValidaResponse.getErrorMessage());
			return elementValidaResponse;
		}
		boolean isSupportJyt=false;
		boolean isSupportBaofu=false;
		boolean isSupportUcfpay=false;
		log.info("channels{}",channels);
		for (Object channelObj : channels) {
			log.info("支付渠道：{}", JSONObject.toJSONString(channelObj));
			Object[] channel = (Object[]) channelObj;
			String channelStr=(nullToStr(channel[0]));
			if(StringUtil.in(channelStr, TradeChannel.jinyuntongpayee.getValue())){
				isSupportJyt=true;
				tradeType=TradeType.jytTradeType.getValue();
				break;
			}
			if(StringUtil.in(channelStr, TradeChannel.baofoopay.getValue(),TradeChannel.baofoopayee.getValue(),TradeChannel.baofooDkWithoiding.getValue())){
				isSupportBaofu=true;
			}
			
		}
		log.info("isSupportJyt={},isSupportBaofu={}",isSupportJyt,isSupportBaofu);
		//如果不支持金运通，看是否支付宝付 ，如果支持宝付就通过宝付绑卡，如果支持金运通就直接通过金运通绑卡
		if(!isSupportJyt){
			if(isSupportBaofu){
				tradeType=TradeType.baofooTradeType.getValue();
			}else{
				elementValidaResponse.setReturnCode(TradeEventCodeEnum.trade_1015.getCode());
				elementValidaResponse.setErrorMessage(TradeEventCodeEnum.getEnumName(TradeEventCodeEnum.trade_1015.getCode()));
				log.info(elementValidaResponse.getErrorMessage());
				return elementValidaResponse;
			}
		}
		
		BeanUtils.copyProperties(req, elementValidaResponse);
		if (StringUtil.isEmpty(req.getUserOid())) {
			elementValidaResponse.setReturnCode(Constant.FAIL);
			elementValidaResponse.setErrorMessage("用户id不能为空");
			return elementValidaResponse;
		}

		ProtocolVo protocol = null;
		// 判断是否已经验证成功过
		protocol = protocolDao.findOneByUserOidAndStatus(req.getUserOid(), ErrorDesEnum.ElELOCK.getCode());
		if (protocol != null) {
			if (checkSameInfo(req, protocol)) {
				elementValidaResponse.setErrorMessage("您已绑定过此银行卡");
				elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
				elementValidaResponse.setReturnCode(Constant.SUCCESS);
				return elementValidaResponse;
			}
			elementValidaResponse.setReturnCode(Constant.FAIL);
			elementValidaResponse.setErrorMessage("您已绑卡无需再绑卡！");
			log.info("elementValidaResponse:{}",JSONObject.toJSON(elementValidaResponse));
			return elementValidaResponse;
		} else {
			// 验证卡号、身份证、手机号是否被绑定过
			protocol = protocolDao.findOneBySome(desCardNo, desCertNo, req.getPhone().trim());
			if (null != protocol) {
				elementValidaResponse.setReturnCode(ErrorDesEnum.ELEMENTVALI.getCode());
				elementValidaResponse.setResult(PayEnum.ERRORCODE0.getCode());
				if (req.getPhone().trim().equals(protocol.getPhone())) {
					elementValidaResponse.setErrorMessage("该手机号已被用户使用！");
					return elementValidaResponse;
				}
				if (desCardNo.equals(protocol.getCardNo())) {
					elementValidaResponse.setErrorMessage("该卡号已被用户使用！");
					return elementValidaResponse;
				}
				if (desCertNo.equals(protocol.getCertificateNo())) {
					elementValidaResponse.setErrorMessage("该身份证号已被用户使用！");
					return elementValidaResponse;
				}
				log.info("信息已被使用过:{}", req.getUserOid());
				return elementValidaResponse;
			}
		}

		// -----------调用--------
		ElementValidationVo vo = new ElementValidationVo();
		setPojo(elementValidaResponse, req, vo, time, desCardNo, desCertNo);
		AuthenticationEvent authenticationEvent = new AuthenticationEvent();
		authenticationEvent.setCardNo(req.getCardNo());
		authenticationEvent.setIdentityNo(req.getCertificateNo());
		authenticationEvent.setMobileNum(req.getPhone());
		authenticationEvent.setUserName(req.getRealName());
		authenticationEvent.setTradeType(tradeType);
		authenticationEvent.setOrderId(seqGenerator.next("BA"));
		authenticationEvent.setCustId(req.getUserOid());
		authenticationEvent.setTransNo(seqGenerator.next("TN"));

		if (testTradeType.equals("test")) {
			authenticationEvent.setReturnCode(Constant.SUCCESS);
		} else {
			log.info("四要素验证组合请求数据,{}", JSONObject.toJSONString(authenticationEvent));
			event.publishEvent(authenticationEvent);
			log.info("四要素验证返回,{}", JSONObject.toJSONString(authenticationEvent));
		}

		// -------------修改-------
		if (Constant.SUCCESS.equals(authenticationEvent.getReturnCode())) {
			vo.setStatus(PayEnum.ERRORCODE3.getCode());
			vo.setCardOrderId(authenticationEvent.getBindOrderId());
			vo.setSmsCode(authenticationEvent.getVerifyCode());
			vo.setBankCode(bankName);
			elementValidaResponse.setSmsCode(authenticationEvent.getVerifyCode());
			if(StringUtil.isEmpty(authenticationEvent.getBindOrderId())){
				authenticationEvent.setBindOrderId(authenticationEvent.getOrderId());
				vo.setCardOrderId(authenticationEvent.getOrderId());
			}
			elementValidaResponse.setCardOrderId(vo.getCardOrderId());
			elementValidaResponse.setErrorMessage(authenticationEvent.getErrorDesc());
			elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
		} else {
			vo.setStatus(PayEnum.ERRORCODE0.getCode());
			elementValidaResponse.setErrorMessage(authenticationEvent.getErrorDesc());
			elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
		}

		vo.setFailDetail(authenticationEvent.getErrorDesc());
		vo.setErrorCode(authenticationEvent.getReturnCode());
		vo.setFeedbackTime(new Timestamp(System.currentTimeMillis()));
		vo.setBindChannel(tradeType);
		elementValidationDao.save(vo);

		elementValidaResponse.setReturnCode(authenticationEvent.getReturnCode());
		log.info("四要素验证返回页面端数据：{}", JSONObject.toJSONString(elementValidaResponse));
		return elementValidaResponse;
	}

	/**
	 * 四要素验证(三方不发短信)
	 */
	@Transactional
	public ElementValidaResponse elementWithoutSms(ElementValidationRequest req) {
		log.info("{},四要素验证(不发短信),{}", req.getUserOid(), JSONObject.toJSONString(req));

		ElementValidaResponse elementValidaResponse = new ElementValidaResponse();
		if (StringUtil.isEmpty(req.getUserOid())) {
			elementValidaResponse.setReturnCode(Constant.FAIL);
			elementValidaResponse.setErrorMessage("用户id不能为空");
			log.info("四要素验证(不发短信)返回,{}",elementValidaResponse);
			return elementValidaResponse;
		}
		Timestamp time = new Timestamp(System.currentTimeMillis());
		BeanUtils.copyProperties(req, elementValidaResponse);

		final String phone = req.getPhone();
		final String realName = req.getRealName();
		final String certificateNo = req.getCertificateNo();
		final String cardNo = req.getCardNo();

		String desCardNo = DesPlus.encrypt(cardNo.trim());
		String	desCertNo = DesPlus.encrypt(certificateNo.trim());

			// 判断是否已经验证成功过
		ProtocolVo protocol = protocolDao.findOneByUserOidAndStatus(req.getUserOid(), ErrorDesEnum.ElELOCK.getCode());
		if (protocol != null) {
			if (checkSameInfo(req, protocol)) {
				elementValidaResponse.setErrorMessage("您已绑定过此银行卡");
				elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
				elementValidaResponse.setReturnCode(Constant.SUCCESS);
				return elementValidaResponse;
			}
			elementValidaResponse.setReturnCode(Constant.FAIL);
			elementValidaResponse.setErrorMessage("您已绑卡无需再绑卡！");
			log.info("四要素验证(不发短信)返回:{}", JSONObject.toJSON(elementValidaResponse));
			return elementValidaResponse;
		}
		// 验证卡号、身份证、手机号是否被绑定过
		protocol = protocolDao.findOneBySome(desCardNo, desCertNo, req.getPhone().trim());
		if (null != protocol) {
			elementValidaResponse.setReturnCode(ErrorDesEnum.ELEMENTVALI.getCode());
			elementValidaResponse.setResult(PayEnum.ERRORCODE0.getCode());
			if (req.getPhone().trim().equals(protocol.getPhone())) {
				elementValidaResponse.setErrorMessage("该手机号已被用户使用！");
				return elementValidaResponse;
			}
			if (desCardNo.equals(protocol.getCardNo())) {
				elementValidaResponse.setErrorMessage("该卡号已被用户使用！");
				return elementValidaResponse;
			}
			if (desCertNo.equals(protocol.getCertificateNo())) {
				elementValidaResponse.setErrorMessage("该身份证号已被用户使用！");
				return elementValidaResponse;
			}
			log.info("四要素验证(不发短信)信息已被使用过:{}", req.getUserOid());
			return elementValidaResponse;
		}
		String bankName;
		try {
			BankUtilEntity bank = bankUtilService.getBankByCard(req.getCardNo().trim());
			if (null == bank) {
				elementValidaResponse.setReturnCode(TradeEventCodeEnum.trade_1015.getCode());
				elementValidaResponse.setErrorMessage(TradeEventCodeEnum.getEnumName(TradeEventCodeEnum.trade_1015.getCode()));
				log.info("四要素验证(不发短信)返回:{}", JSONObject.toJSON(elementValidaResponse));
				return elementValidaResponse;
			}
			bankName = bank.getBankName();
		} catch (Exception e) {
			log.error("四要素验证(不发短信),获取银行编码异常:{}",e.getMessage());
			elementValidaResponse.setReturnCode(TradeEventCodeEnum.trade_1015.getCode());
			elementValidaResponse.setErrorMessage(TradeEventCodeEnum.getEnumName(TradeEventCodeEnum.trade_1015.getCode()));
			log.error(elementValidaResponse.getErrorMessage());
			return elementValidaResponse;
		}

		// 先锋四要素适配器需要的参数
		tradeType = TradeType.ucfPayElement.getValue();
		String channel = TradeChannel.ucfPayCertPay.getValue();

		// save data
		ElementValidationVo elementValidationVo = new ElementValidationVo();
		setPojo(elementValidaResponse, req, elementValidationVo, time, desCardNo, desCertNo);

		// -----------调用--------
		AuthenticationEvent authenticationEvent = new AuthenticationEvent();
		authenticationEvent.setUserOid(req.getUserOid());
		authenticationEvent.setOrderId(this.genSn());
		authenticationEvent.setChannel(channel);
		authenticationEvent.setTradeType(tradeType);
		authenticationEvent.setBindOrderId(req.getCardOrderId());
		authenticationEvent.setMobileNum(phone);
		authenticationEvent.setIdentityNo(certificateNo);
		authenticationEvent.setCardNo(cardNo);
		authenticationEvent.setUserName(realName);
		authenticationEvent.setTransNo(seqGenerator.next("TN"));
		log.info("四要素验证(不发短信)组合请求数据,{}", JSONObject.toJSONString(authenticationEvent));
		event.publishEvent(authenticationEvent);
		log.info("四要素验证(不发短信)返回,{}", JSONObject.toJSONString(authenticationEvent));

		// -------------修改-------
		if (Constant.SUCCESS.equals(authenticationEvent.getReturnCode())) {
			//  20170826 向用户表同步用户姓名
			final UserInfoEntity user = userInfoDao.findByUserOid(req.getUserOid());
			if (null!=user && StringUtil.isEmpty(user.getName())) {
				user.setName(req.getRealName());
				userInfoDao.save(user);
			}
			elementValidationVo.setStatus(PayEnum.ERRORCODE1.getCode());
			// 验证成功的加入协议表中
			ProtocolVo protocolVo = new ProtocolVo();

			protocolVo.setBankName(bankName);
			protocolVo.setCardNo(desCardNo);
			protocolVo.setBankTypeCode(bankName);
			protocolVo.setRealName(realName);
			protocolVo.setCertificateNo(desCertNo);
			protocolVo.setAccountBankType(PayEnum.ELEMENT_CERTIFICATETYPE.getCode());
			protocolVo.setCreateTime(time);
			protocolVo.setOid(StringUtil.uuid());
			protocolVo.setPhone(phone);
			protocolVo.setUserOid(req.getUserOid());
			protocolVo.setStatus(ErrorDesEnum.ElELOCK.getCode());
			protocolVo.setCardOrderId(authenticationEvent.getBindOrderId());
			protocolVo.setSmsCode("");// 短信验证码为空
			protocolVo.setProtocolNo(nullToStr(authenticationEvent.getBindId()));
			protocolVo.setCertificates(CertificatesTypeEnum.IDCARD.getCode());
			protocolVo.setCardType("个人");
			protocolDao.save(protocolVo);
			elementValidaResponse.setErrorMessage(authenticationEvent.getErrorDesc());
			elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
		} else {
			if (authenticationEvent.getReturnCode().equals(Constant.INPROCESS)) {
				elementValidationVo.setStatus(PayEnum.ERRORCODE0.getCode());
			}
			elementValidaResponse.setErrorMessage(authenticationEvent.getErrorDesc());
			elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
		}
		elementValidationVo.setBankCode(bankName);
		elementValidationVo.setCardOrderId(authenticationEvent.getBindOrderId());
		elementValidationVo.setErrorCode(authenticationEvent.getReturnCode());
		elementValidationVo.setBindChannel(tradeType);
		elementValidationVo.setUpdateTime(time);
		elementValidationVo.setOrderNo(authenticationEvent.getOrderId());
		elementValidationVo.setFailDetail(authenticationEvent.getErrorDesc());
		elementValidationVo.setErrorCode(authenticationEvent.getReturnCode());
		elementValidationVo.setFeedbackTime(new Timestamp(System.currentTimeMillis()));
		elementValidationDao.save(elementValidationVo);

		elementValidaResponse.setReturnCode(authenticationEvent.getReturnCode());
		log.info("四要素验证(不发短信)返回页面端数据：{}", JSONObject.toJSONString(elementValidaResponse));
		return elementValidaResponse;
	}
	/**
	 * 二版四要素验证 短信确认
	 * 
	 * @param req
	 * @return
	 */
	@SuppressWarnings("unused")
	public ElementValidaResponse smsChekc(ElementValidationRequest req) {
		long timel = System.currentTimeMillis();
		Timestamp time = new Timestamp(timel);
		ElementValidaResponse elementValidaResponse = new ElementValidaResponse();
		BeanUtils.copyProperties(req, elementValidaResponse);
		log.info("{},四要素短信验证,{}", req.getUserOid(), JSONObject.toJSONString(req));

		BeanUtils.copyProperties(req, elementValidaResponse);
		ProtocolVo vo = protocolDao.findOneByUserOidAndStatus(req.getUserOid(), ErrorDesEnum.ElELOCK.getCode());
		if (vo != null) {
			elementValidaResponse.setReturnCode(Constant.FAIL);
			elementValidaResponse.setErrorMessage("您已绑卡无需再绑卡！");
			return elementValidaResponse;
		}

		ElementValidationVo elementValidationVo = elementValidationDao.findByCardOrderId(req.getUserOid(),
				req.getCardOrderId());
		if (elementValidationVo == null) {
			elementValidaResponse.setReturnCode(Constant.FAIL);
			elementValidaResponse.setErrorMessage("请先绑卡申请,在绑卡确认");
			return elementValidaResponse;
		}

		// -----------调用--------
		AuthenticationEvent authenticationEvent = new AuthenticationEvent();
		authenticationEvent.setCustId(req.getUserOid());
		authenticationEvent.setOrderId(this.genSn());
		authenticationEvent.setTradeType(elementValidationVo.getBindChannel());
		authenticationEvent.setVerifyCode(req.getSmsCode());
		authenticationEvent.setBindOrderId(req.getCardOrderId());
		authenticationEvent.setMobileNum(elementValidationVo.getPhone());
		authenticationEvent.setTransNo(seqGenerator.next("TN"));
		authenticationEvent.setTradeType(elementValidationVo.getBindChannel());
		if (testTradeType.equals("test")) {
			authenticationEvent.setReturnCode(Constant.SUCCESS);
		} else {
			log.info("四要素短信验证组合请求数据,{}", JSONObject.toJSONString(authenticationEvent));
			event.publishEvent(authenticationEvent);
			log.info("四要素短信验证返回,{}", JSONObject.toJSONString(authenticationEvent));
		}

		String userType = UserTypeEnum.INVESTOR.getCode();
		// -------------修改-------
		if (Constant.SUCCESS.equals(authenticationEvent.getReturnCode())) {
			//  20170826 向用户表同步用户姓名
			final UserInfoEntity user = userInfoDao.findByUserOid(req.getUserOid());
			userType = user.getUserType();
			if (null!=user && StringUtil.isEmpty(user.getName())) {
				user.setName(elementValidationVo.getRealName());
				userInfoDao.save(user);
			}
			elementValidationVo.setStatus(PayEnum.ERRORCODE1.getCode());
			// 验证成功的加入协议表中
			ProtocolVo protocolVo = new ProtocolVo();
			protocolVo.setBankName(elementValidationVo.getBankCode());
			protocolVo.setCardNo(elementValidationVo.getCardNo());
			protocolVo.setBankTypeCode(elementValidationVo.getBankCode());
			protocolVo.setRealName(elementValidationVo.getRealName());
			protocolVo.setCertificateNo(elementValidationVo.getCertificateNo());
			protocolVo.setAccountBankType(PayEnum.ELEMENT_CERTIFICATETYPE.getCode());
			protocolVo.setCreateTime(time);
			protocolVo.setOid(StringUtil.uuid());
			protocolVo.setPhone(elementValidationVo.getPhone());
			protocolVo.setUserOid(req.getUserOid());
			protocolVo.setStatus(ErrorDesEnum.ElELOCK.getCode());
			protocolVo.setCardOrderId(authenticationEvent.getBindOrderId());
			protocolVo.setSmsCode(authenticationEvent.getVerifyCode());
			protocolVo.setProtocolNo(authenticationEvent.getBindId());
			protocolVo.setCertificates(CertificatesTypeEnum.IDCARD.getCode());
			protocolVo.setCardType("个人");
			UserInfoEntity u = userInfoDao.findByUserOid(req.getUserOid());
			if(u !=null) {
				protocolVo.setUserType(u.getUserType());
			}
			protocolDao.save(protocolVo);
			elementValidaResponse.setErrorMessage(authenticationEvent.getErrorDesc());
			elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
		} else {
			if (authenticationEvent.getReturnCode().equals(Constant.INPROCESS)) {
				elementValidationVo.setStatus(PayEnum.ERRORCODE0.getCode());
			}
			elementValidaResponse.setErrorMessage(authenticationEvent.getErrorDesc());
			elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
		}

		elementValidationVo.setUpdateTime(time);
		elementValidationVo.setOrderNo(authenticationEvent.getOrderId());
		elementValidationVo.setFailDetail(authenticationEvent.getErrorDesc());
		elementValidationVo.setErrorCode(authenticationEvent.getReturnCode());
		elementValidationVo.setFeedbackTime(new Timestamp(System.currentTimeMillis()));
		elementValidationDao.save(elementValidationVo);

		elementValidaResponse.setReturnCode(authenticationEvent.getReturnCode());
		log.info("四要素短信验证返回页面端数据：{}", JSONObject.toJSONString(elementValidaResponse));
		return elementValidaResponse;
	}

	/**
	 * 四要素解绑
	 * 
	 * @param req
	 * @return
	 */
	public ElementValidaResponse unLock(ElementValidationRequest req) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		log.info("{},解除实名支付银行卡,{}", req.getUserOid(), JSONObject.toJSONString(req));
		ElementValidaResponse elementValidaResponse = new ElementValidaResponse();
		if (StringUtil.isEmpty(req.getCardNo())) {
			elementValidaResponse.setErrorMessage("银行卡号为空，请勿重复解绑！");
			elementValidaResponse.setReturnCode(Constant.FAIL);
			return elementValidaResponse;
		}
		// 查询用户是否绑过
		ProtocolVo protocol = protocolDao.findOneByUserOid(req.getUserOid(), DesPlus.encrypt(req.getCardNo()),
				PayEnum.ERRORCODE1.getCode());

		boolean isjytElement = false;
		boolean isBaofoElement = false;
		
		if (protocol != null) {

			ElementValidationVo elementVo = elementValidationDao.findByBindChannel(req.getUserOid(),TradeType.jytTradeType.getValue());
			ElementValidationVo baofooElementVo = elementValidationDao.findByBindChannel(req.getUserOid(),TradeType.baofooTradeType.getValue());
			
			//金运通解绑
			if(elementVo!=null){
				if(PayEnum.ERRORCODE1.getCode().equals(elementVo.getStatus())){
					AuthenticationEvent authenticationEvent = new AuthenticationEvent();// 这个event是解除绑卡的event，不是代扣
					authenticationEvent.setCardNo(req.getCardNo());
					authenticationEvent.setCustId(req.getUserOid());
					authenticationEvent.setTradeType("jytUnlockCard");
					event.publishEvent(authenticationEvent);
					if (Constant.SUCCESS.equals(authenticationEvent.getReturnCode())) {
						isjytElement = true;
						//保存金运通解绑状态
						elementVo.setStatus(ErrorDesEnum.ELEUNLOCK.getCode());
						elementVo.setFailDetail(ErrorDesEnum.ELEUNLOCK.getName());
						elementVo.setUpdateTime(time);
						elementValidationDao.save(elementVo);
						log.info("四要素解绑，金运通解绑成功");
					}else{
						log.info("四要素解绑，金运通解绑失败");
					}
				}else{
					isjytElement = true;
					log.info("金运通已解绑");
				}
			}else {
				isjytElement = true;
				log.info("金运通未绑卡");
			}
			
			//宝付解绑,宝付未绑卡或已解绑，isBaofoElement = true;
			if(!StringUtils.isEmpty(protocol.getProtocolNo())){
				//短信验证绑卡,解绑
				if(baofooElementVo!=null){
					if (this.baofooUbindCard(req, protocol.getProtocolNo())) {
						protocol.setProtocolNo("");// 解绑成功设为空
						baofooElementVo.setStatus(ErrorDesEnum.ELEUNLOCK.getCode());
						baofooElementVo.setFailDetail(ErrorDesEnum.ELEUNLOCK.getName());
						baofooElementVo.setUpdateTime(time);
						elementValidationDao.save(baofooElementVo);
						isBaofoElement = true;
						log.info("四要素解绑，宝付解绑成功");
					}else{
						log.info("四要素解绑，宝付解绑失败");
					}
				//静默绑卡,解绑
				}else{
					if (this.baofooUbindCard(req, protocol.getProtocolNo())) {
						protocol.setProtocolNo("");// 解绑成功设为空
						isBaofoElement = true;
						log.info("四要素解绑，宝付解绑成功");
					}else{
						log.info("四要素解绑，宝付解绑失败");
					}
				}
			}else{
				isBaofoElement = true;
				log.info("宝付未绑卡或已解绑");
			}
			
			if (isjytElement == true  && isBaofoElement==true) {
				// 修改协议表状态为解绑
				protocol.setStatus(ErrorDesEnum.ELEUNLOCK.getCode());
				protocol.setUpdateTime(time);

				elementValidaResponse.setErrorMessage("解绑成功");
				elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
				elementValidaResponse.setReturnCode(Constant.SUCCESS);
			} else {
				elementValidaResponse.setErrorMessage("解绑失败");
				elementValidaResponse.setResult(PayEnum.ERRORCODE0.getCode());
				elementValidaResponse.setReturnCode(Constant.FAIL);
			}
			protocolDao.save(protocol);
			log.info("四要素解绑返回页面端数据：{}", JSONObject.toJSONString(elementValidaResponse));
			return elementValidaResponse;

		} else {
			ProtocolVo unPro = protocolDao.findOneByUserOid(req.getUserOid(), DesPlus.encrypt(req.getCardNo()),
					ErrorDesEnum.ELEUNLOCK.getCode());
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
	 * 四要素解绑 用户强制解绑（admin）
	 * 
	 * @param req
	 * @return
	 */
	public ElementValidaResponse unLockOperotar(ElementValidationRequest req) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		log.info("{},解除实名支付银行卡,{}", req.getUserOid(), JSONObject.toJSONString(req));
		ElementValidaResponse elementValidaResponse = new ElementValidaResponse();
		// 查询用户是否解绑过
		ProtocolVo protocol = protocolDao.findOneByUserOid(req.getUserOid(), DesPlus.encrypt(req.getCardNo()),
				PayEnum.ERRORCODE1.getCode());
		//金运通解绑
		AuthenticationEvent authenticationEvent = new AuthenticationEvent();// 这个event是解除绑卡的event，不是代扣
		authenticationEvent.setCardNo(req.getCardNo());
		authenticationEvent.setCustId(req.getUserOid());
		authenticationEvent.setTradeType("jytUnlockCard");
		event.publishEvent(authenticationEvent);
		
		//宝付解绑
		if(!StringUtil.isEmpty(protocol.getProtocolNo())){
			if (this.baofooUbindCard(req, protocol.getProtocolNo())) {
				protocol.setProtocolNo("");// 解绑成功设为空
				log.info("四要素解绑，宝付解绑成功");
			}
		}else{
			log.info("宝付已解绑");
		}

		if (protocol != null) {
				ElementValidationVo elementVo = elementValidationDao.findBySingleOne(req.getUserOid(),
						DesPlus.encrypt(req.getCardNo()));
				// 修改成功为解绑状态
				elementVo.setStatus(ErrorDesEnum.ELEUNLOCK.getCode());
				elementVo.setFailDetail(ErrorDesEnum.ELEUNLOCK.getName());
				elementVo.setUpdateTime(time);
				elementValidationDao.save(elementVo);

				// 修改协议表状态为解绑
				protocol.setStatus(ErrorDesEnum.ELEUNLOCK.getCode());
				protocol.setUpdateTime(time);
				protocolDao.save(protocol);
		}
			elementValidaResponse.setErrorMessage("解绑成功");
			elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
			elementValidaResponse.setReturnCode(Constant.SUCCESS);
			log.info("四要素解绑返回页面端数据：{}", JSONObject.toJSONString(elementValidaResponse));
			return elementValidaResponse;
	}

	/**
	 * 验证用户绑定的卡和上次绑定成功的是否一样
	 * 
	 * @param req
	 * @return
	 */
	private boolean checkSameInfo(ElementValidationRequest req, ProtocolVo protocol) {
		if (req.getCardNo().trim().equals(DesPlus.decrypt(protocol.getCardNo().trim()))
				&& req.getRealName().trim().equals(protocol.getRealName())
				&& req.getPhone().trim().equals(protocol.getPhone().trim())
				&& req.getCertificateNo().trim().equals(DesPlus.decrypt(protocol.getCertificateNo().trim()))) {
			return true;
		}
		return false;
	}

	/**
	 * 查询用户下的鉴权数据
	 * 
	 * @param userOid
	 * @return
	 */
	public List<ElementValidationVo> findByUserOid(String userOid) {
		List<ElementValidationVo> listVos = elementValidationDao.findByUserOid(userOid);
		if (!listVos.isEmpty()) {
			for (ElementValidationVo vo : listVos) {
				vo.setCardNo(DesPlus.decrypt(vo.getCardNo()));
				vo.setCertificateNo(DesPlus.decrypt(vo.getCertificateNo()));
			}
		}
		return listVos;
	}

	/**
	 * 分页条件查询鉴权
	 * 
	 * @param req
	 * @return
	 */
	public ElementValidationRes page(ElementValidationForm req) {
		log.info("四要素查询条件---》{}", JSONObject.toJSONString(req));
		Page<ElementValidationVo> listPage = elementValidationDao.findAll(buildSpecification(req),
				new PageRequest(req.getPage() - 1, req.getRows()));
		ElementValidationRes res = new ElementValidationRes();
		if (listPage != null && listPage.getSize() > 0) {
			if (listPage.getContent().size() > 0) {
				for (ElementValidationVo vo : listPage.getContent()) {
					vo.setCardNo(DesPlus.decrypt(vo.getCardNo()));
					vo.setCertificateNo(DesPlus.decrypt(vo.getCertificateNo()));
				}
			}
			res.setRows(listPage.getContent());
			res.setTotalPage(listPage.getTotalPages());
			res.setPage(req.getPage());
			res.setRow(req.getRows());
			res.setTotal(listPage.getTotalElements());
			return res;
		}
		return null;
	}

	public Specification<ElementValidationVo> buildSpecification(final ElementValidationForm req) {
		Specification<ElementValidationVo> spec = new Specification<ElementValidationVo>() {
			@Override
			public Predicate toPredicate(Root<ElementValidationVo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> bigList = new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(req.getReuqestNo()))
					bigList.add(cb.like(root.get("requestNo").as(String.class), "%" + req.getReuqestNo() + "%"));
				if (!StringUtil.isEmpty(req.getUserOid()))
					bigList.add(cb.equal(root.get("userOid").as(String.class), req.getUserOid()));
				if (!StringUtil.isEmpty(req.getCardNo()))
					bigList.add(cb.like(root.get("cardNo").as(String.class), "%" + req.getCardNo() + "%"));
				if (!StringUtil.isEmpty(req.getSystemSource()))
					bigList.add(cb.like(root.get("systemSource").as(String.class), "%" + req.getSystemSource() + "%"));
				if (!StringUtil.isEmpty(req.getStatus()))
					bigList.add(cb.equal(root.get("status").as(String.class), req.getStatus()));
				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
				query.orderBy(cb.desc(root.get("createTime")));

				// 条件查询
				return query.getRestriction();
			}
		};
		return spec;
	}

	public void setPojo(ElementValidaResponse elementValidaResponse, ElementValidationRequest req,
			ElementValidationVo vo, Timestamp time, String desCardNo, String desCeriNo) {
		// 记录验证信息
		vo.setCardNo(desCardNo);
		vo.setCertificateNo(desCeriNo);
		vo.setRealName(req.getRealName());
		vo.setPhone(req.getPhone());
		vo.setUserOid(req.getUserOid());
		vo.setCertificatetype(PayEnum.ELEMENT_CERTIFICATETYPE.getCode());
		vo.setReceivingTime(time);
		vo.setOid(StringUtil.uuid());
		vo.setCreateTime(time);
		vo.setUpdateTime(time);
		vo.setSystemSource(req.getSystemSource());
		vo.setFailDetail("绑卡");
		vo.setReuqestNo(req.getRequestNo());
		try {
			elementValidationDao.save(vo);
		} catch (Exception e) {
			Throwable cause = e.getCause();
			if (cause instanceof org.hibernate.exception.ConstraintViolationException) {
				if (cause.getCause().getMessage().indexOf(req.getRequestNo()) != -1) {
					elementValidaResponse.setReturnCode(Constant.FAIL);
					elementValidaResponse.setErrorMessage("请求流水号重复提交！");
					elementValidaResponse.setRequestNo(req.getRequestNo());
				}
			}
			log.error("四要素验证异常,{}", e);
			elementValidaResponse.setReturnCode(Constant.FAIL);
			elementValidaResponse.setErrorMessage("系统错误,请联系管理员处理！");
		}
	}

	/**
	 * 宝付直接绑卡
	 */
	public String baofooElement(ElementValidationRequest req) {
		log.info("宝付绑直接卡 baofooElement");
		String bindId = "";
		AuthenticationEvent authenticationEvent = new AuthenticationEvent();
		authenticationEvent.setCardNo(req.getCardNo());
		authenticationEvent.setBankCode(req.getBankCode());
		authenticationEvent.setIdentityNo(req.getCertificateNo());
		authenticationEvent.setMobileNum(req.getPhone());
		authenticationEvent.setUserName(req.getRealName());
		authenticationEvent.setTradeType("baofoobindCard");
		authenticationEvent.setOrderId(seqGenerator.next("BA"));
		event.publishEvent(authenticationEvent);
		bindId = authenticationEvent.getBindId();
		return bindId;
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
	
	public ElementValidaResponse bindingBaofoo(ElementValidationRequest req) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		ElementValidaResponse resp = new ElementValidaResponse();
		resp.setReturnCode(Constant.FAIL);
		resp.setRequestNo(req.getRequestNo());
		ProtocolVo protocolVo = protocolDao.findOneByUserOidAndStatus(req.getUserOid().trim(),
				ErrorDesEnum.ElELOCK.getCode());
		if (null != protocolVo) {
			resp.setErrorMessage("用户已绑定过银行卡");
			return resp;
		}
		Channel channel = channelDao.queryChannel(TradeChannel.baofoopay.getValue());
		List<ChannelBankVo> channelBankList = channelBankService.findChannelBank(req.getCardNo().trim(),
				channel.getChannelNo());
		if (CollectionUtils.isEmpty(channelBankList)) {
			resp.setErrorMessage("不支持些银行卡或未把银行加入到通道配置");
			return resp;
		}

		log.info("宝付静默绑卡，渠道银行信息,{}", JSONObject.toJSONString(channelBankList.get(0)));
		String pay_code = channelBankList.get(0).getChannelbankCode();
		ElementValidationRequest elementReq = new ElementValidationRequest();
		elementReq.setCardNo(req.getCardNo().trim());
		elementReq.setCertificateNo(req.getCertificateNo().trim());
		elementReq.setPhone(req.getPhone());
		elementReq.setRealName(req.getRealName());
		elementReq.setBankCode(pay_code);
		String bindId = "";
		AuthenticationEvent authenticationEvent= elementService.baofooBind(elementReq);
		if(Constant.SUCCESS.equals(authenticationEvent.getReturnCode())){
			protocolVo = new ProtocolVo();
			protocolVo.setOid(StringUtil.uuid());
			BeanUtils.copyProperties(elementReq, protocolVo);
			protocolVo.setCardNo(DesPlus.encrypt(protocolVo.getCardNo().trim()));
			protocolVo.setCertificateNo(DesPlus.encrypt(protocolVo.getCertificateNo().trim()));
			protocolVo.setUserOid(req.getUserOid());
			protocolVo.setProtocolNo(bindId);
			protocolVo.setUpdateTime(time);
			protocolVo.setCreateTime(time);
			if(StringUtil.isEmpty(elementReq.getBankName())){
			protocolVo.setBankName(channelBankList.get(0).getChannelbankName());
			}
			protocolVo.setAccountBankType(PayEnum.ELEMENT_CERTIFICATETYPE.getCode());
			protocolVo.setStatus(ErrorDesEnum.ElELOCK.getCode());
			protocolVo.setCardOrderId(authenticationEvent.getOrderId());
			protocolVo.setCertificates(CertificatesTypeEnum.IDCARD.getCode());
			protocolVo.setCardType("个人");
			protocolDao.save(protocolVo);
		}
		resp.setReturnCode(authenticationEvent.getReturnCode());
		resp.setErrorMessage(authenticationEvent.getErrorDesc());
		
		return resp;
	}
	/**
	 * 宝付查询绑卡
	 */
	public String baofooQueryEle(ElementValidationRequest req) {
		log.info("宝付查询绑卡 baofooQueryElement");
		String bindId = "";
		AuthenticationEvent authenticationEvent = new AuthenticationEvent();
		authenticationEvent.setCardNo(req.getCardNo());
		authenticationEvent.setTradeType("baofoobindquery");
		authenticationEvent.setOrderId(seqGenerator.next("BA"));
		event.publishEvent(authenticationEvent);
		bindId = authenticationEvent.getBindId();
		return bindId;
	}

	/**
	 * 宝付解除绑卡
	 */
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
	}

	/**
	 * 先锋解绑
	 */
	public ElementValidaResponse unLockUcPay(ElementValidationRequest req) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		log.info("{},解除实名支付银行卡,{}", req.getUserOid(), JSONObject.toJSONString(req));
		ElementValidaResponse elementValidaResponse = new ElementValidaResponse();
		BeanUtils.copyProperties(req, elementValidaResponse);
		if (StringUtil.isEmpty(req.getCardNo())) {
			elementValidaResponse.setErrorMessage("银行卡号为空！");
			elementValidaResponse.setReturnCode(Constant.FAIL);
			return elementValidaResponse;
		}
		// 查询用户是否绑卡
		ProtocolVo protocol = protocolDao.findOneByUserOid(req.getUserOid(), DesPlus.encrypt(req.getCardNo()),
				PayEnum.ERRORCODE1.getCode());
		if (protocol == null) {
			elementValidaResponse.setErrorMessage("用户未绑定此银行卡！");
			elementValidaResponse.setReturnCode(Constant.FAIL);
			return elementValidaResponse;
		}
			AuthenticationEvent authenticationEvent = new AuthenticationEvent();// 这个event是解除绑卡的event，不是代扣
			authenticationEvent.setCardNo(req.getCardNo());
			authenticationEvent.setCustId(req.getUserOid());
			authenticationEvent.setTradeType("unbindCard");
			authenticationEvent.setChannel(TradeChannel.ucfPayCertPay.getValue());
			event.publishEvent(authenticationEvent);
			if (Constant.SUCCESS.equals(authenticationEvent.getReturnCode())) {
				protocol.setStatus(ErrorDesEnum.ELEUNLOCK.getCode());
				protocol.setUpdateTime(time);
				elementValidaResponse.setErrorMessage("解绑成功");
				elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
				elementValidaResponse.setReturnCode(Constant.SUCCESS);
			} else {
				elementValidaResponse.setErrorMessage("解绑失败");
				elementValidaResponse.setResult(PayEnum.ERRORCODE0.getCode());
				elementValidaResponse.setReturnCode(Constant.FAIL);
		}
		return elementValidaResponse;
	}


	String nullToStr(Object str){
		if(null == str){
			return "";
		}
		return str.toString();
	}
	
	/**
	 * 绑卡申请
	 * @param req 绑卡申请信息
	 * @return 绑卡申请结果
	 */
	public ElementValidaResponse bindApply(ElementValidationRequest req) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		log.info("接收用户{}绑卡申请请求,{}", req.getUserOid(), JSONObject.toJSONString(req));
		//加密绑卡信息
		ElementValidaResponse elementValidaResponse = this.checkBindApplyPrarm(req);
		BeanUtils.copyProperties(req, elementValidaResponse);
		if(!BaseResponse.isSuccess(elementValidaResponse)){
			log.info("elementValidaResponse:{}", JSONObject.toJSONString(elementValidaResponse));
			return elementValidaResponse;
		}

		String encCardNo = DesPlus.encrypt(req.getCardNo().trim()), encCertNo = DesPlus.encrypt(req.getCertificateNo().trim());
		Map<String,Object> returnMap = this.bindRuleCheck(req, elementValidaResponse, encCardNo, encCertNo);
		elementValidaResponse = (ElementValidaResponse) returnMap.get("elementValidaResponse");
		if(!BaseResponse.isSuccess(elementValidaResponse)){
			log.info("elementValidaResponse:{}", JSONObject.toJSONString(elementValidaResponse));
			return elementValidaResponse;
		}
		
		//绑卡通道
		Map<String,String> channelMsg = this.getChannelMsg(req.getCardNo());
		if(!Constant.SUCCESS.equals(channelMsg.get("returnCode"))){
			elementValidaResponse.setReturnCode(channelMsg.get("returnCode"));
			elementValidaResponse.setErrorMessage(channelMsg.get("errorMessage"));
			log.info("elementValidaResponse:{}", JSONObject.toJSONString(elementValidaResponse));
			return elementValidaResponse;
		}
		//保存绑卡信息
		ElementValidationVo vo = new ElementValidationVo();
		setPojo(elementValidaResponse, req, vo, time, encCardNo, encCertNo);
		//组装绑卡信息参数
		AuthenticationEvent authenticationEvent = new AuthenticationEvent();
		authenticationEvent.setCardNo(req.getCardNo());
		authenticationEvent.setIdentityNo(req.getCertificateNo());
		authenticationEvent.setMobileNum(req.getPhone());
		authenticationEvent.setUserName(req.getRealName());
		authenticationEvent.setTradeType(TradeType.bankCardAuth.getValue());
		authenticationEvent.setOrderId(seqGenerator.next("BA"));
		authenticationEvent.setCustId(req.getUserOid());
		authenticationEvent.setTransNo(seqGenerator.next("TN"));
		authenticationEvent.setChannel(channelMsg.get("channelNo"));
		//判断通道知否发送绑卡短信
		if("N".equals(channelMsg.get("sendBindSms"))){
			log.info("结算绑卡发送验证码");
			authenticationEvent = this.sendBindSms(authenticationEvent);
		}else{
			// 调用三方绑卡
			log.info("四要素验证组合请求数据,{}", JSONObject.toJSONString(authenticationEvent));
			event.publishEvent(authenticationEvent);
			log.info("四要素验证返回,{}", JSONObject.toJSONString(authenticationEvent));
		}
		
		// 保存结算或三方绑卡申请返回信息
		if (Constant.SUCCESS.equals(authenticationEvent.getReturnCode())) {
			vo.setStatus(PayEnum.ERRORCODE3.getCode());
			vo.setCardOrderId(authenticationEvent.getBindOrderId());
			vo.setSmsCode(authenticationEvent.getVerifyCode());
			vo.setBankCode(channelMsg.get("bankName"));
			elementValidaResponse.setSmsCode(authenticationEvent.getVerifyCode());
			if(StringUtil.isEmpty(authenticationEvent.getBindOrderId())){
				authenticationEvent.setBindOrderId(authenticationEvent.getOrderId());
				vo.setCardOrderId(authenticationEvent.getOrderId());
			}
			elementValidaResponse.setCardOrderId(vo.getCardOrderId());
			elementValidaResponse.setErrorMessage(authenticationEvent.getErrorDesc());
			elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
		} else {
			vo.setStatus(PayEnum.ERRORCODE0.getCode());
			elementValidaResponse.setErrorMessage(authenticationEvent.getErrorDesc());
			elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
		}

		vo.setFailDetail(authenticationEvent.getErrorDesc());
		vo.setErrorCode(authenticationEvent.getReturnCode());
		vo.setFeedbackTime(new Timestamp(System.currentTimeMillis()));
		vo.setBindChannel(authenticationEvent.getChannel());
		elementValidationDao.save(vo);

		elementValidaResponse.setReturnCode(authenticationEvent.getReturnCode());
		log.info("四要素验证返回页面端数据：{}", JSONObject.toJSONString(elementValidaResponse));
		return elementValidaResponse;
	}
	
	/**
	 * 绑卡规则校验
	 * @param req 绑卡请求参数
	 * @param elementValidaResponse 绑卡返回参数
	 * @param encCardNo 加密卡号
	 * @param encCertNo 加密身份证信息
	 * @return 校验结果、是否为实名
	 */
	private Map<String,Object> bindRuleCheck(ElementValidationRequest req,
			ElementValidaResponse elementValidaResponse, String encCardNo,
			String encCertNo) {
		Map<String,Object> returnMap = new HashMap<String,Object>();
		String authenticationProtocol = "Y";
		// 查询已绑定卡数
		int protocolCount = protocolDao.findBindedCount(req.getUserOid());
		if(protocolCount >= Integer.parseInt(userMaxBindNum)){
			elementValidaResponse.setReturnCode(Constant.FAIL);
			elementValidaResponse.setErrorMessage("新增绑卡失败，用户绑卡数量超过系统限制");
			returnMap.put("elementValidaResponse", elementValidaResponse);
			returnMap.put("authenticationProtocol", authenticationProtocol);
			log.info("绑卡规则校验结果：{}",elementValidaResponse.getErrorMessage());
			return returnMap;
		}
		// 查询此卡是否已被绑定
		List<ProtocolVo> protocolVoList = protocolDao.findProtocolListByCardNo(encCardNo);
		if(protocolVoList != null&&protocolVoList.size()>0){
			if(protocolVoList.size() >= Integer.parseInt(cardMaxBindNum)){
				elementValidaResponse.setReturnCode(Constant.FAIL);
				elementValidaResponse.setErrorMessage("新增绑卡失败，该银行卡被绑定次数超过系统限制");
				returnMap.put("elementValidaResponse", elementValidaResponse);
				returnMap.put("authenticationProtocol", authenticationProtocol);
				log.info("绑卡规则校验结果：{}",elementValidaResponse.getErrorMessage());
				return returnMap;
			}
			for(ProtocolVo protocolVo : protocolVoList){
				if(protocolVo.getUserOid().equals(req.getUserOid())){
					elementValidaResponse.setReturnCode(Constant.FAIL);
					elementValidaResponse.setErrorMessage("用户已绑定该卡");
					returnMap.put("elementValidaResponse", elementValidaResponse);
					returnMap.put("authenticationProtocol", authenticationProtocol);
					log.info("绑卡规则校验结果：{}",elementValidaResponse.getErrorMessage());
					return returnMap;
				}
			}
		}
		// 查询用户实名卡信息 
		ProtocolVo authenticationedProtocolVo = protocolDao.findAuthenticationByUserOid(req.getUserOid());
		if(authenticationedProtocolVo != null){
			authenticationProtocol = "N";
			if(!"Y".equals(bindOtherUserCard)&&!authenticationedProtocolVo.getCertificateNo().equals(encCertNo)){
				elementValidaResponse.setReturnCode(Constant.FAIL);
				elementValidaResponse.setErrorMessage("新增绑卡失败，系统不允许绑定非本人银行卡");
				returnMap.put("elementValidaResponse", elementValidaResponse);
				returnMap.put("authenticationProtocol", authenticationProtocol);
				log.info("绑卡规则校验结果：{}",elementValidaResponse.getErrorMessage());
				return returnMap;
			}
		}
		// 查询该身份证是否被绑定为实名认证
		ProtocolVo authenticationProtocolVo = protocolDao.findAuthenticationByCertNo(encCertNo);
		if(authenticationProtocolVo != null){
			if("Y".equals(authenticationProtocol)){
				elementValidaResponse.setReturnCode(Constant.FAIL);
				elementValidaResponse.setErrorMessage("认证绑卡失败，该身份证信息已经被实名认证");
				returnMap.put("elementValidaResponse", elementValidaResponse);
				returnMap.put("authenticationProtocol", authenticationProtocol);
				log.info("绑卡规则校验结果：{}",elementValidaResponse.getErrorMessage());
				return returnMap;
			}
		}
		elementValidaResponse.setReturnCode(Constant.SUCCESS);
		elementValidaResponse.setErrorMessage("绑卡规则校验成功");
		returnMap.put("elementValidaResponse", elementValidaResponse);
		returnMap.put("authenticationProtocol", authenticationProtocol);
		log.info("绑卡规则校验结果：{}",elementValidaResponse.getErrorMessage());
		return returnMap;
	}

	/**
	 * 绑卡确认
	 * @param req 绑卡确认信息
	 * @return 绑卡结果
	 */
	public ElementValidaResponse bindConfrim(ElementValidationRequest req) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		ElementValidaResponse elementValidaResponse = new ElementValidaResponse();
		log.info("{用户{}发起绑卡确认请求，{}", req.getUserOid(), JSONObject.toJSONString(req));
		BeanUtils.copyProperties(req, elementValidaResponse);
		ElementValidationVo elementValidationVo = elementValidationDao.findByCardOrderId(req.getUserOid(),
				req.getCardOrderId());
		if (elementValidationVo == null) {
			elementValidaResponse.setReturnCode(Constant.FAIL);
			elementValidaResponse.setErrorMessage("请先绑卡申请,在绑卡确认");
			log.info("elementValidaResponse:{}", JSONObject.toJSONString(elementValidaResponse));
			return elementValidaResponse;
		}
		// 校验绑卡信息
		Map<String,Object> returnMap = this.bindRuleCheck(req, elementValidaResponse, 
				elementValidationVo.getCardNo(), elementValidationVo.getCertificateNo());
		elementValidaResponse = (ElementValidaResponse) returnMap.get("elementValidaResponse");
		if(!BaseResponse.isSuccess(elementValidaResponse)){
			log.info("elementValidaResponse:{}", JSONObject.toJSONString(elementValidaResponse));
			return elementValidaResponse;
		}
		// 调用三方绑卡确认
		AuthenticationEvent authenticationEvent = new AuthenticationEvent();
		//鉴权需要信息
		authenticationEvent.setUserOid(req.getUserOid());
		authenticationEvent.setChannel(elementValidationVo.getBindChannel());
		authenticationEvent.setIdentityNo(elementValidationVo.getCertificateNo());
		authenticationEvent.setCardNo(elementValidationVo.getCardNo());
		authenticationEvent.setUserName(elementValidationVo.getRealName());
		//绑卡确认需要信息
		authenticationEvent.setCustId(req.getUserOid());
		authenticationEvent.setOrderId(this.genSn());
		authenticationEvent.setVerifyCode(req.getSmsCode());
		authenticationEvent.setBindOrderId(req.getCardOrderId());
		authenticationEvent.setMobileNum(elementValidationVo.getPhone());
		authenticationEvent.setTransNo(seqGenerator.next("TN"));
		authenticationEvent.setTradeType(TradeType.bankCardAuth.getValue());
		// 查询绑卡通道信息
		ChannelVo channel = comChannelDao.findByChannelNo(elementValidationVo.getBindChannel());
		if(channel == null){
			elementValidaResponse.setReturnCode(Constant.FAIL);
			elementValidaResponse.setErrorMessage("未知绑卡通道");
			return elementValidaResponse;
		}
		Boolean checkResult = true;
		if("N".equals(channel.getSendBindSms())){
			log.info("结算绑卡校验验证码");
			checkResult = sendSMSUtils.checkVeriCode(authenticationEvent.getMobileNum(),
					SMSTypeEnum.BINDCARD.getCode(), authenticationEvent.getVerifyCode());
		}
		if(checkResult){
			log.info("四要素短信验证请求适配器数据,{}", JSONObject.toJSONString(authenticationEvent));
			event.publishEvent(authenticationEvent);
			log.info("四要素短信验证适配器返回,{}", JSONObject.toJSONString(authenticationEvent));
		}else{
			authenticationEvent.setReturnCode(Constant.FAIL);
			authenticationEvent.setErrorDesc("验证码错误");
		}
		// 判断三方返回绑卡结果
		if (Constant.SUCCESS.equals(authenticationEvent.getReturnCode())) {
			// 向用户表同步用户姓名
			final UserInfoEntity user = userInfoDao.findByUserOid(req.getUserOid());
			if (null!=user && StringUtil.isEmpty(user.getName())) {
				user.setName(elementValidationVo.getRealName());
				userInfoDao.save(user);
			}
			elementValidationVo.setStatus(PayEnum.ERRORCODE1.getCode());
			// 验证成功的加入协议表中
			ProtocolVo protocolVo = new ProtocolVo();
			protocolVo.setBankName(elementValidationVo.getBankCode());
			protocolVo.setCardNo(elementValidationVo.getCardNo());
			protocolVo.setBankTypeCode(elementValidationVo.getBankCode());
			protocolVo.setRealName(elementValidationVo.getRealName());
			protocolVo.setCertificateNo(elementValidationVo.getCertificateNo());
			protocolVo.setAccountBankType(PayEnum.ELEMENT_CERTIFICATETYPE.getCode());
			protocolVo.setCreateTime(time);
			protocolVo.setOid(StringUtil.uuid());
			protocolVo.setPhone(elementValidationVo.getPhone());
			protocolVo.setUserOid(req.getUserOid());
			protocolVo.setStatus(ErrorDesEnum.ElELOCK.getCode());
			protocolVo.setCardOrderId(authenticationEvent.getBindOrderId());
			protocolVo.setSmsCode(authenticationEvent.getVerifyCode());
			protocolVo.setProtocolNo(authenticationEvent.getBindId());
			protocolVo.setCertificates(CertificatesTypeEnum.IDCARD.getCode());
			protocolVo.setCardType("个人");
			String authenticationProtocol =(String) returnMap.get("authenticationProtocol");
			protocolVo.setAuthenticationStatus(authenticationProtocol);
			UserInfoEntity u = userInfoDao.findByUserOid(req.getUserOid());
			if(u !=null) {
				protocolVo.setUserType(u.getUserType());
			}
			if("Y".equals(authenticationProtocol)){
				// 查询是否已存在认证的信息
				ProtocolVo authenticationProtocolVo = protocolDao.findAuthenticationByCertNo(elementValidationVo.getCertificateNo());
				if(authenticationProtocolVo != null){
					elementValidaResponse.setErrorMessage("认证绑卡失败，该身份证信息已经被实名认证");
					elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
					elementValidaResponse.setReturnCode(Constant.FAIL);
					log.info("四要素短信验证结束返回数据：{}", JSONObject.toJSONString(elementValidaResponse));
					return elementValidaResponse;
				}
			}
			protocolDao.save(protocolVo);
		}

		elementValidationVo.setUpdateTime(time);
		elementValidationVo.setOrderNo(authenticationEvent.getOrderId());
		elementValidationVo.setFailDetail(authenticationEvent.getErrorDesc());
		elementValidationVo.setErrorCode(authenticationEvent.getReturnCode());
		elementValidationVo.setFeedbackTime(new Timestamp(System.currentTimeMillis()));
		elementValidationDao.save(elementValidationVo);

		elementValidaResponse.setErrorMessage(authenticationEvent.getErrorDesc());
		elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
		elementValidaResponse.setReturnCode(authenticationEvent.getReturnCode());
		log.info("四要素短信验证结束返回数据：{}", JSONObject.toJSONString(elementValidaResponse));
		return elementValidaResponse;
	}
	
	/**
	 * 解绑
	 * @param req 解绑信息
	 * @return 解绑结果
	 */
	public ElementValidaResponse unbundling(ElementValidationRequest req) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		log.info("{},解除实名支付银行卡,{}", req.getUserOid(), JSONObject.toJSONString(req));
		ElementValidaResponse elementValidaResponse = new ElementValidaResponse();
		if (StringUtil.isEmpty(req.getCardNo())) {
			elementValidaResponse.setErrorMessage("银行卡号为空，请勿重复解绑！");
			elementValidaResponse.setReturnCode(Constant.FAIL);
			log.error("四要素解绑返回数据：{}", JSONObject.toJSONString(elementValidaResponse));
			return elementValidaResponse;
		}
		String enCardNo = DesPlus.encrypt(req.getCardNo());
		// 查询用户是否绑过该卡
		ProtocolVo protocol = protocolDao.findOneByUserOid(req.getUserOid(), enCardNo,
				PayEnum.ERRORCODE1.getCode());
		if(protocol == null){
			ProtocolVo unPro = protocolDao.findOneByUserOid(req.getUserOid(), enCardNo,
					ErrorDesEnum.ELEUNLOCK.getCode());
			if (unPro != null) {
				elementValidaResponse.setErrorMessage("卡号已解绑,不能再解绑！");
				elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
				elementValidaResponse.setReturnCode(Constant.SUCCESS);
				log.error("四要素解绑返回数据：{}", JSONObject.toJSONString(elementValidaResponse));
				return elementValidaResponse;
			}
			elementValidaResponse.setErrorMessage("您未绑卡成功或解绑卡号不正确，解绑失败！");
			elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
			elementValidaResponse.setReturnCode(Constant.FAIL);
			log.error("四要素解绑返回数据：{}", JSONObject.toJSONString(elementValidaResponse));
			return elementValidaResponse;
		}
		//当解绑卡存在未完成的订单时不允许解绑
		int rechargeInProcessOrderCount = paymentDao.findRechargeInProcessCountByUserOidAndCard(req.getUserOid(), enCardNo);
		int withdrawalsInProcessOrderCount = accOrderDao.findWithdrawalsInProcessCountByUserOidAndCard(req.getUserOid(), enCardNo);
		if(rechargeInProcessOrderCount >0 || withdrawalsInProcessOrderCount >0){
			elementValidaResponse.setErrorMessage("解绑银行卡失败，该银行卡有订单尚未处理完成！");
			elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
			elementValidaResponse.setReturnCode(Constant.FAIL);
			log.error("四要素解绑返回数据：{}", JSONObject.toJSONString(elementValidaResponse));
			return elementValidaResponse;
		}
		//获取开启的绑卡通道
		ChannelVo channel = comChannelDao.findBindChannel();
		if(channel == null){
			elementValidaResponse.setErrorMessage("未知绑卡通道，解绑失败！");
			elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
			elementValidaResponse.setReturnCode(Constant.FAIL);
			log.error("四要素解绑返回数据：{}", JSONObject.toJSONString(elementValidaResponse));
			return elementValidaResponse;
		}
		AuthenticationEvent authenticationEvent = new AuthenticationEvent();// 这个event是解除绑卡的event，不是代扣
		authenticationEvent.setCardNo(req.getCardNo());
		authenticationEvent.setCustId(req.getUserOid());
		authenticationEvent.setOrderId(seqGenerator.next("BA"));
		authenticationEvent.setBindId(protocol.getProtocolNo());
		authenticationEvent.setChannel(channel.getChannelNo());
		String tradeType = "";
		ElementValidationVo elementVo = elementValidationDao.findByBindChannel(req.getUserOid(), channel.getChannelNo());;
		if(TradeChannel.baofoopay.getValue().equals(channel.getChannelNo())){
			tradeType = "baofooubindCard";
		}else if(TradeChannel.ucfPayCertPay.getValue().equals(channel.getChannelNo())){
			tradeType = "unbindCard";
		}else{
			elementValidaResponse.setErrorMessage("未知绑卡通道，解绑失败！");
			elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
			elementValidaResponse.setReturnCode(Constant.FAIL);
			log.error("四要素解绑返回数据：{}", JSONObject.toJSONString(elementValidaResponse));
			return elementValidaResponse;
		}
		authenticationEvent.setTradeType(tradeType);
		log.info("四要素解绑请求适配器数据,{}", JSONObject.toJSONString(authenticationEvent));
		event.publishEvent(authenticationEvent);
		log.info("四要素解绑适配器返回,{}", JSONObject.toJSONString(authenticationEvent));
		if (Constant.SUCCESS.equals(authenticationEvent.getReturnCode())) {
			// 修改协议表状态为解绑
			protocol.setStatus(ErrorDesEnum.ELEUNLOCK.getCode());
			protocol.setUpdateTime(time);
			protocol.setProtocolNo("");// 解绑成功设为空
			if(elementVo != null){
				elementVo.setStatus(ErrorDesEnum.ELEUNLOCK.getCode());
				elementVo.setFailDetail(ErrorDesEnum.ELEUNLOCK.getName());
				elementVo.setUpdateTime(time);
				elementValidationDao.save(elementVo);
			}
			elementValidaResponse.setErrorMessage("解绑成功");
			elementValidaResponse.setResult(PayEnum.ERRORCODE1.getCode());
			elementValidaResponse.setReturnCode(Constant.SUCCESS);
		} else {
			elementValidaResponse.setErrorMessage("解绑失败");
			elementValidaResponse.setResult(PayEnum.ERRORCODE0.getCode());
			elementValidaResponse.setReturnCode(Constant.FAIL);
		}
		protocolDao.save(protocol);
		log.info("四要素解绑返回结果：{}", JSONObject.toJSONString(elementValidaResponse));
		return elementValidaResponse;
	}

	/**
	 * 绑卡申请结算发送验证码
	 * @param authenticationEvent 绑卡event
	 * @return 获取验证码结果
	 */
	private AuthenticationEvent sendBindSms(AuthenticationEvent authenticationEvent) {
		String phone = authenticationEvent.getMobileNum();
		//绑卡短信类型
		String smsType = SMSTypeEnum.BINDCARD.getCode();
		
		String[] values = new String[2];
		//调用发送验证码
		BaseResp resp = sendSMSUtils.sendSMSBySendTypes(phone, smsType, values);
		if(-1==resp.getErrorCode()){
			log.error("发送验证码失败,失败原因：{}",resp.getErrorMessage());
			authenticationEvent.setReturnCode(Constant.FAIL);
			String errorDesc = "com.guohuai.component.exception.AMPException: 验证码已生成";
			if(errorDesc.equals(resp.getErrorMessage().substring(0, 52))){
				authenticationEvent.setErrorDesc("验证码已生成");
			}else{
				authenticationEvent.setErrorDesc("发送验证码失败");
			}
			return authenticationEvent;
		}
		//获取验证码
		String verifyCode = sendSMSUtils.getVeriCode(phone, smsType);
		if(StringUtil.isEmpty(verifyCode)){
			authenticationEvent.setReturnCode(Constant.FAIL);
			authenticationEvent.setErrorDesc("发送验证码失败");
			log.error("发送验证码后获取验证码异常");
			return authenticationEvent;
		}
		authenticationEvent.setVerifyCode(verifyCode);
		authenticationEvent.setReturnCode(Constant.SUCCESS);
		authenticationEvent.setErrorDesc("发送成功");
		return authenticationEvent;
	}

	/**
	 * 处理绑卡通道信息
	 * @param cardNo 银行卡号
	 * @return 绑卡通道信息
	 */
	private Map<String, String> getChannelMsg(String cardNo) {
		Map<String, String> returnMap = new HashMap<String, String>();
		ChannelVo channel = null;
		BankUtilEntity bank = null;
		ChannelBankVo channelBank = null;
		try{
			//获取开启的绑卡通道
			channel = comChannelDao.findBindChannel();
			if(channel == null){
				returnMap.put("returnCode", TradeEventCodeEnum.trade_1015.getCode());
				returnMap.put("errorMessage", TradeEventCodeEnum.getEnumName(TradeEventCodeEnum.trade_1015.getCode()));
				log.error(TradeEventCodeEnum.getEnumName(TradeEventCodeEnum.trade_1015.getCode()));
				return returnMap;
			}
			//获取绑定银行卡信息
			bank =  bankUtilService.getBankByCard(cardNo);
			if(null == bank){
				returnMap.put("returnCode", TradeEventCodeEnum.trade_1015.getCode());
				returnMap.put("errorMessage", TradeEventCodeEnum.getEnumName(TradeEventCodeEnum.trade_1015.getCode()));
				log.error(TradeEventCodeEnum.getEnumName(TradeEventCodeEnum.trade_1015.getCode()));
				return returnMap;
			}
			//获取通道银行信息
			channelBank = comChannelBankDao.findByChannelAndBankCode(channel.getChannelNo(), bank.getBankCode());
			if(null == channelBank){
				returnMap.put("returnCode", TradeEventCodeEnum.trade_1015.getCode());
				returnMap.put("errorMessage", TradeEventCodeEnum.getEnumName(TradeEventCodeEnum.trade_1015.getCode()));
				log.error(TradeEventCodeEnum.getEnumName(TradeEventCodeEnum.trade_1015.getCode()));
				return returnMap;
			}
		}catch(Exception e){
			e.printStackTrace();
			log.error("四要素验证,获取通道及银行编码异常");
			returnMap.put("returnCode", TradeEventCodeEnum.trade_1015.getCode());
			returnMap.put("errorMessage", TradeEventCodeEnum.getEnumName(TradeEventCodeEnum.trade_1015.getCode()));
			log.error(TradeEventCodeEnum.getEnumName(TradeEventCodeEnum.trade_1015.getCode()));
			return returnMap;
		}
		returnMap.put("returnCode", Constant.SUCCESS);
		returnMap.put("channelNo", channel.getChannelNo());
		returnMap.put("sendBindSms", channel.getSendBindSms());
		returnMap.put("bankName", bank.getBankName());
		return returnMap;
	}

	private ElementValidaResponse checkBindApplyPrarm(ElementValidationRequest req) {
		ElementValidaResponse elementValidaResponse = new ElementValidaResponse();
		BeanUtils.copyProperties(req, elementValidaResponse);
		elementValidaResponse.setReturnCode(Constant.SUCCESS);
		elementValidaResponse.setErrorMessage("绑卡校验参数成功");
		if (StringUtil.isEmpty(req.getUserOid())) {
			elementValidaResponse.setReturnCode(Constant.FAIL);
			elementValidaResponse.setErrorMessage("用户id不能为空");
			return elementValidaResponse;
		}
		if (StringUtil.isEmpty(req.getCardNo())) {
			elementValidaResponse.setReturnCode(Constant.FAIL);
			elementValidaResponse.setErrorMessage("银行卡号不能为空");
			return elementValidaResponse;
		}
		if (StringUtil.isEmpty(req.getCertificateNo())) {
			elementValidaResponse.setReturnCode(Constant.FAIL);
			elementValidaResponse.setErrorMessage("身份证号不能为空");
			return elementValidaResponse;
		}
		return elementValidaResponse;
	}

	/**
	 * 获取绑卡规则
	 * @return ElementValidaRulesResponse
	 */
	public ElementValidaRulesResponse bindingRules() {
		ElementValidaRulesResponse resp = new ElementValidaRulesResponse();
		resp.setUserMaxBindNum(Integer.parseInt(userMaxBindNum));
		resp.setCardMaxBindNum(Integer.parseInt(cardMaxBindNum));
		resp.setBindOtherUserCard(bindOtherUserCard);
		return resp;
	}

	/**
	 * 获取用户绑卡信息
	 * @param req
	 * @return
	 */
	public UserProtocolResponse bindCardInfo(UserProtocolRequest req) {
		UserProtocolResponse resp = new UserProtocolResponse();
		String userOid = req.getUserOid();
		if(StringUtil.isEmpty(userOid)){
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("用户userOid不能为空");
			return resp;
		}
		resp.setUserOid(userOid);
		//查询用户已绑卡信息
		List<ProtocolVo> protocolList = protocolDao.findListByUserOidAndStatus(userOid, ProtocolVo.STATUS_BIND);
		List<ProtocolDTO> cardList = this.installQueryCardList(protocolList);
		resp.setCardList(cardList);
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("查询成功");
		return resp;
	}

	private List<ProtocolDTO> installQueryCardList(List<ProtocolVo> protocolList) {
		List<ProtocolDTO> list = new ArrayList<ProtocolDTO>();
		if(protocolList != null && !protocolList.isEmpty()){
			for(ProtocolVo protocolVo : protocolList){
				ProtocolDTO dto = new ProtocolDTO();
				dto.setAccountBankType(protocolVo.getAccountBankType());
				dto.setAuthenticationStatus(protocolVo.getAuthenticationStatus());
				dto.setBankName(protocolVo.getBankName());
				dto.setBranch(protocolVo.getBranch());
				dto.setCardNo(DesPlus.decrypt(protocolVo.getCardNo()));
				dto.setCardType(protocolVo.getCardType());
				dto.setCertificateNo(DesPlus.decrypt(protocolVo.getCertificateNo()));
				dto.setCertificates(protocolVo.getCertificates());
				dto.setCity(protocolVo.getCity());
				dto.setCounty(protocolVo.getCounty());
				dto.setPhone(protocolVo.getPhone());
				dto.setProvince(protocolVo.getProvince());
				dto.setRealName(protocolVo.getRealName());
				dto.setUserType(protocolVo.getUserType());
				list.add(dto);
			}
		}
		return list;
	}
	
}