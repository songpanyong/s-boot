//package com.guohuai.boot.pay.service;
//
//import java.sql.Timestamp;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import javax.persistence.criteria.CriteriaBuilder;
//import javax.persistence.criteria.CriteriaQuery;
//import javax.persistence.criteria.Predicate;
//import javax.persistence.criteria.Root;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.BeanUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.ApplicationEventPublisher;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.stereotype.Service;
//
//import com.alibaba.fastjson.JSONObject;
//import com.guohuai.basic.common.SeqGenerator;
//import com.guohuai.basic.common.StringUtil;
//import com.guohuai.boot.ErrorDesEnum;
//import com.guohuai.boot.PayEnum;
//import com.guohuai.boot.pay.dao.AuthenticationDao;
//import com.guohuai.boot.pay.dao.ProtocolDao;
//import com.guohuai.boot.pay.form.AuthenticationForm;
//import com.guohuai.boot.pay.res.AuthenticationRes;
//import com.guohuai.boot.pay.vo.AuthenticationVo;
//import com.guohuai.boot.pay.vo.ElementValidationVo;
//import com.guohuai.boot.pay.vo.ProtocolVo;
//import com.guohuai.component.config.ElementValConfig;
//import com.guohuai.component.util.Constant;
//import com.guohuai.component.util.DesPlus;
//import com.guohuai.payadapter.component.TradeChannel;
//import com.guohuai.payadapter.component.TradeType;
//import com.guohuai.payadapter.listener.event.PayAgreementEvent;
//import com.guohuai.settlement.api.request.AuthenticationRequest;
//import com.guohuai.settlement.api.response.AuthenticationResponse;
//import com.guohuai.settlement.api.response.ElementValidaResponse;
//
//@Service
//public class AuthenticationService {
//	private final static Logger log = LoggerFactory.getLogger(AuthenticationService.class);
//	@Autowired
//	private AuthenticationDao authenticationDao;
//	@Autowired
//	private ProtocolDao protocolDao;
//	@Autowired
//	private ElementValidationService elementValidationService;
//	@Autowired
//	private ApplicationEventPublisher event;
//	@Autowired
//	private SeqGenerator seqGenerator;
//	@Autowired
//	private ElementValConfig elementValConfig;
//	public String genSn() {
//		String sn = this.seqGenerator.next("SEL");
//		return sn;
//	}
//
//	public AuthenticationResponse apply(AuthenticationRequest req) {
//		log.info("{},代扣协议申请{}", req.getUserOid(), JSONObject.toJSONString(req));
//		Timestamp time = new Timestamp(System.currentTimeMillis());
//		AuthenticationResponse authenticationResponse = new AuthenticationResponse();
//		
//		// -----四要素验证----
//		authenticationResponse=queryByElement(req, time);
//		if(!authenticationResponse.getReturnCode().equals(Constant.SUCCESS)){
//			return authenticationResponse;
//		}
//		
//		// ----判断是否重复申请代扣协议---
//		List<AuthenticationVo> auths = authenticationDao.findByPhone(req.getPhone(), DesPlus.encrypt(req.getCardNo()),
//				DesPlus.encrypt(req.getCertificateNo()), req.getUserOid());
//		AuthenticationVo authenticationVo = new AuthenticationVo();
//		
//		//这个是对邦卡的约束，手机号，卡号，身份证号，用户id只要有一个符合已经邦卡的，是不能在邦卡
//		applyOrConfirm(auths, authenticationResponse, authenticationVo, PayEnum.PROXY01.getCode());
//		if (!StringUtil.isEmpty(authenticationResponse.getReturnCode())
//				&& (!authenticationResponse.getReturnCode().equals(Constant.SUCCESS))) {
//			log.info("{},代扣申请返回前段,{}", req.getUserOid(), JSONObject.toJSONString(authenticationResponse));
//			
//			if (authenticationResponse.getReturnCode().equals(ErrorDesEnum.APPLYMORCONFIRMMOR.getCode())) {
//				
//				// ----当在协议中存在已绑卡，并且不是当前用户，则添加一条
//				ProtocolVo protocol = protocolDao.findOneBySome(authenticationVo.getCardNo(),
//						authenticationVo.getCertificateNo(), authenticationVo.getPhone());
//				if (protocol != null && (!protocol.getUserOid().equals(req.getUserOid()))) {
//					ProtocolVo proto = new ProtocolVo();
//					BeanUtils.copyProperties(protocol, proto);
//					proto.setOid(StringUtil.uuid());
//					proto.setUserOid(req.getUserOid());
//					protocolDao.save(proto);
//				}
//			}
//			return authenticationResponse;
//		}
//		AuthenticationVo vo = new AuthenticationVo();
//		BeanUtils.copyProperties(req, vo);
//		vo.setProtocolType(PayEnum.PROTOCOLTYPE.getCode());
//		vo.setRequestNo(req.getReuqestNo());
//		vo.setOid(StringUtil.uuid());
//		vo.setOrderNo(this.genSn());
//		vo.setCreateTime(time);
//		vo.setCardNo(DesPlus.encrypt(req.getCardNo()));
//		vo.setCertificateNo(DesPlus.encrypt(req.getCertificateNo()));
//		vo.setType(PayEnum.PROXY01.getCode());
//		vo.setCardType(PayEnum.ELEMENT_CERTIFICATETYPE.getCode());
//		vo.setCertificatetype(PayEnum.CERTIFICATETYPE.getCode());
//		try {
//			authenticationDao.save(vo);
//		} catch (Exception e) {
//			return sqlEx(e, req.getReuqestNo());
//		}
//
//		// ----------------调用----------------
//		vo.setRequestTime(new Timestamp(System.currentTimeMillis()));
//		PayAgreementEvent payAgreementEvent = new PayAgreementEvent();
//		payAgreementEvent.setCardNo(req.getCardNo());
//		payAgreementEvent.setIdentityNo(req.getCertificateNo());
//		payAgreementEvent.setMobileNum(vo.getPhone());
//		payAgreementEvent.setUserName(vo.getRealName());
//		payAgreementEvent.setOrderNo(vo.getOrderNo());
//		payAgreementEvent.setTradeType(TradeType.applyAgreement.getValue());
//		payAgreementEvent.setChannel(TradeChannel.lycheepay.getValue());
//		payAgreementEvent.setRquestNo(vo.getOrderNo());
//		log.info("申请代扣组合数据请求:{}", JSONObject.toJSONString(payAgreementEvent));
//		event.publishEvent(payAgreementEvent);
//		log.info("申请代扣返回：{}", JSONObject.toJSONString(payAgreementEvent));
//
//		// ------------修改-----------
//		vo.setStatus(payAgreementEvent.getStatus());
//		vo.setFailDetail(payAgreementEvent.getErrorDesc());
//		vo.setErrorCode(payAgreementEvent.getReturnCode());
//		vo.setReturnTime(new Timestamp(System.currentTimeMillis()));
//		vo.setSmsNo(payAgreementEvent.getTreatyId());
//		vo.setMerchantId(payAgreementEvent.getMerchantId());
//		vo.setOrderNo(payAgreementEvent.getOrderNo());
//		vo.setBankCode(payAgreementEvent.getBankType());
//		authenticationDao.save(vo);
//
//		authenticationResponse.setReuqestNo(payAgreementEvent.getRquestNo());
//		authenticationResponse.setReturnCode(payAgreementEvent.getReturnCode());
//		authenticationResponse.setErrorMessage(payAgreementEvent.getErrorDesc());
//		authenticationResponse.setSmsSeq(payAgreementEvent.getTreatyId());
//		authenticationResponse.setResult(payAgreementEvent.getStatus());
//		authenticationResponse.setOrderNo(payAgreementEvent.getOrderNo());
//		log.info("{},代扣申请返回前段,{}", req.getUserOid(), JSONObject.toJSONString(authenticationResponse));
//		return authenticationResponse;
//
//	}
//
//	public AuthenticationResponse confirm(AuthenticationRequest req) {
//		log.info("{},代扣协议确认{}", req.getUserOid(), JSONObject.toJSONString(req));
//		Timestamp time = new Timestamp(System.currentTimeMillis());
//		AuthenticationResponse authenticationResponse = new AuthenticationResponse();
//
//		// ----判断是否重复申请代扣协议---
//		List<AuthenticationVo> auths = authenticationDao.findByOrderNo(req.getOrderNo());
//		AuthenticationVo authenticationVo = new AuthenticationVo();
//		applyOrConfirm(auths, authenticationResponse, authenticationVo, PayEnum.PROXY02.getCode());
//		if (!StringUtil.isEmpty(authenticationResponse.getReturnCode())
//				&& (!authenticationResponse.getReturnCode().equals(Constant.SUCCESS))) {
//			log.info("{},代扣确认返回前段,{}", req.getUserOid(), JSONObject.toJSONString(authenticationResponse));
//			if (authenticationResponse.getReturnCode().equals(ErrorDesEnum.APPLYMORCONFIRMMOR.getCode())) {
//				ProtocolVo protocol = protocolDao.findOneBySome(authenticationVo.getCardNo(),
//						authenticationVo.getCertificateNo(), authenticationVo.getPhone());
//				if (protocol != null && (!protocol.getUserOid().equals(req.getUserOid()))) {
//					ProtocolVo proto = new ProtocolVo();
//					BeanUtils.copyProperties(protocol, proto);
//					proto.setOid(StringUtil.uuid());
//					proto.setUserOid(req.getUserOid());
//					protocolDao.save(proto);
//				}
//			}
//			return authenticationResponse;
//		}
//		AuthenticationVo auth = auths.get(0);
//		AuthenticationVo vo = new AuthenticationVo();
//		BeanUtils.copyProperties(auth, vo);
//		vo.setOid(StringUtil.uuid());
//		vo.setCreateTime(time);
//		vo.setType(PayEnum.PROXY02.getCode());
//		vo.setProtocolType(PayEnum.PROTOCOLTYPE.getCode());
//		vo.setRequestNo(req.getReuqestNo());
//		vo.setOrderNo(req.getOrderNo());
//		vo.setSmsCode(req.getSmsCode());
//		vo.setUserOid(req.getUserOid());
//		vo.setRequestTime(time);
//		try {
//			authenticationDao.save(vo);
//		} catch (Exception e) {
//			return sqlEx(e, req.getReuqestNo());
//		}
//
//		// ----------------调用----------------
//		PayAgreementEvent payAgreementEvent = new PayAgreementEvent();
//		payAgreementEvent.setTradeType(TradeType.confirmAgreement.getValue());
//		payAgreementEvent.setChannel(TradeChannel.lycheepay.getValue());
//		payAgreementEvent.setOrderNo(vo.getOrderNo());
//		payAgreementEvent.setTreatyId(vo.getSmsNo());
//		payAgreementEvent.setSmsCode(vo.getSmsCode());
//		log.info("确认代扣组合数据请求:{}", JSONObject.toJSONString(payAgreementEvent));
//		event.publishEvent(payAgreementEvent);
//		log.info("确认代扣返回:{}", JSONObject.toJSONString(payAgreementEvent));
//
//		// ------------修改-----------
//		vo.setStatus(payAgreementEvent.getStatus());
//		vo.setFailDetail(payAgreementEvent.getErrorDesc());
//		vo.setErrorCode(payAgreementEvent.getReturnCode());
//		vo.setReturnTime(new Timestamp(System.currentTimeMillis()));
//		authenticationDao.save(vo);
//
//		// ------如果成功记入开通协议表中-------
//		if (Constant.SUCCESS.equals(payAgreementEvent.getReturnCode())) {
//			ProtocolVo protocolVo = new ProtocolVo();
//			BeanUtils.copyProperties(vo, protocolVo);
//			protocolVo.setAccountBankType(PayEnum.ELEMENT_CERTIFICATETYPE.getCode());
//			protocolVo.setProtocolNo(vo.getSmsNo());
//			protocolVo.setCreateTime(time);
//			protocolVo.setOid(StringUtil.uuid());
//			protocolVo.setPhone(auth.getPhone());
//			protocolVo.setBankTypeCode(vo.getBankCode());
//			protocolDao.save(protocolVo);
//		}
//		authenticationResponse.setReuqestNo(req.getReuqestNo());
//		authenticationResponse.setReturnCode(payAgreementEvent.getReturnCode());
//		authenticationResponse.setErrorMessage(vo.getFailDetail());
//		authenticationResponse.setSmsSeq(vo.getSmsNo());
//		authenticationResponse.setResult(vo.getStatus());
//		authenticationResponse.setOrderNo(payAgreementEvent.getOrderNo());
//		log.info("{},代扣确认返回前段,{}", req.getUserOid(), JSONObject.toJSONString(authenticationResponse));
//		return authenticationResponse;
//	}
//
//	public void applyOrConfirm(List<AuthenticationVo> auths, AuthenticationResponse authenticationResponse,
//			AuthenticationVo authenticationVo, String type) {
//		if (!auths.isEmpty()) {
//			boolean bug01 = false, bug02 = false;
//			Map<String, AuthenticationVo> map = new HashMap<String, AuthenticationVo>();
//			for (AuthenticationVo vo : auths) {
//				if (vo.getType().trim().equals(PayEnum.PROXY01.getCode())) {
//					map.put("01", vo);
//					bug01 = true;
//				}
//
//				if (vo.getType().trim().equals(PayEnum.PROXY02.getCode())) {
//					map.put("02", vo);
//					bug02 = true;
//				}
//			}
//			if (bug01 && bug02) {
//				authenticationVo = map.get("02");
//				authenticationResponse.setOrderNo(authenticationVo.getOrderNo());
//				authenticationResponse.setReuqestNo(authenticationVo.getRequestNo());
//				authenticationResponse.setReturnCode(ErrorDesEnum.APPLYMORCONFIRMMOR.getCode());
//				authenticationResponse.setErrorMessage(ErrorDesEnum.APPLYMORCONFIRMMOR.getName());
//			} else if (bug01 && type.trim().equals(PayEnum.PROXY01.getCode())) {
//				authenticationVo = map.get("01");
//				authenticationResponse.setOrderNo(authenticationVo.getOrderNo());
//				authenticationResponse.setReuqestNo(authenticationVo.getRequestNo());
//				authenticationResponse.setSmsSeq(authenticationVo.getSmsNo());
//				authenticationResponse.setReturnCode(ErrorDesEnum.APPLYIN.getCode());
//				authenticationResponse.setErrorMessage(ErrorDesEnum.APPLYIN.getName());
//			} else if (!bug01 && type.trim().equals(PayEnum.PROXY02.getCode())) {
//				authenticationResponse.setReturnCode(ErrorDesEnum.CONFIRMIN.getCode());
//				authenticationResponse.setErrorMessage(ErrorDesEnum.CONFIRMIN.getName());
//			}
//		} else {
//			if (type.trim().equals(PayEnum.PROXY02.getCode())) {
//				authenticationResponse.setReturnCode(ErrorDesEnum.CONFIRMIN.getCode());
//				authenticationResponse.setErrorMessage(ErrorDesEnum.CONFIRMIN.getName());
//			}
//		}
//	}
//
//	public AuthenticationResponse sqlEx(Exception e, String requestNo) {
//		AuthenticationResponse authenticationResponse = new AuthenticationResponse();
//		Throwable cause = e.getCause();
//		if (cause instanceof org.hibernate.exception.ConstraintViolationException) {
//			if (cause.getCause().getMessage().indexOf(requestNo) != -1) {
//				authenticationResponse.setReturnCode(ErrorDesEnum.REPEAT.getCode());
//				authenticationResponse.setErrorMessage(ErrorDesEnum.REPEAT.getName());
//				authenticationResponse.setReuqestNo(requestNo);
//				return authenticationResponse;
//			}
//		}
//		log.error("代扣申请异常,{}", e);
//		authenticationResponse.setReturnCode(ErrorDesEnum.SYSTEMMSG.getCode());
//		authenticationResponse.setErrorMessage(ErrorDesEnum.SYSTEMMSG.getName());
//		return authenticationResponse;
//	}
//
//	public AuthenticationResponse queryByElement(AuthenticationRequest req, Timestamp time) {
//		// 四要素验证
//		AuthenticationResponse authenticationResponse = new AuthenticationResponse();
//		authenticationResponse.setReturnCode(Constant.SUCCESS);
//		
//		//需要验证的时候验证，，否则不用验证
//		if(elementValConfig.isElement.trim().equals("1")){
//			ElementValidationVo elementValidationVo = new ElementValidationVo();
//			BeanUtils.copyProperties(req, elementValidationVo);
//			elementValidationVo.setOid(StringUtil.uuid());
//			elementValidationVo.setReceivingTime(time);
//			elementValidationVo.setCardNo(DesPlus.encrypt(req.getCardNo()));
//			elementValidationVo.setCertificateNo(DesPlus.encrypt(req.getCertificateNo()));
//			elementValidationVo.setCertificatetype(PayEnum.ELEMENT_CERTIFICATETYPE.getCode());
//			elementValidationVo.setCreateTime(time);
//			ElementValidaResponse elementValidaResponse = elementValidationService.check(elementValidationVo);
//			if (!elementValidaResponse.getReturnCode().equals(Constant.SUCCESS)) {
//				BeanUtils.copyProperties(req, authenticationResponse);
//				authenticationResponse.setErrorMessage(elementValidaResponse.getErrorMessage());
//				authenticationResponse.setReturnCode(ErrorDesEnum.ELEMENTVALI.getCode());
//				
//			}
//		}
//		return authenticationResponse;
//	}
//
//	/**
//	 * 查询用户下代扣协议状态
//	 * 
//	 * @param userOid
//	 * @return
//	 */
//	public AuthenticationVo findByStatus(String userOid) {
//		AuthenticationForm req = new AuthenticationForm();
//		req.setUserOid(userOid);
//		List<AuthenticationVo> listVo = authenticationDao.findAll(buildSpecification(req));
//		if (!listVo.isEmpty()) {
//			return listVo.get(0);
//		}
//		return null;
//	}
//
//	/**
//	 * 查询用户下代扣申请流程
//	 * 
//	 * @param userOid
//	 * @param queryType
//	 *            [0:查询状态;1:查询代扣申请流程]
//	 * @return
//	 */
//	public List<AuthenticationVo> findByDetails(String userOid) {
//		AuthenticationForm req = new AuthenticationForm();
//		req.setUserOid(userOid);
//		req.setQueryType(1);
//		List<AuthenticationVo> listVo = authenticationDao.findAll(buildSpecification(req));
//		if (!listVo.isEmpty()) {
//			return listVo;
//		}
//		return null;
//	}
//
//	/**
//	 * 分页条件查询状态
//	 * 
//	 * @param req
//	 * @return
//	 */
//	public AuthenticationRes findPage(AuthenticationForm req) {
//		log.info("代扣协议查询条件---》{}", JSONObject.toJSONString(req));
//		Page<AuthenticationVo> listPage = authenticationDao.findAll(buildSpecification(req),
//				new PageRequest(req.getPage() - 1, req.getRows()));
//		AuthenticationRes res = new AuthenticationRes();
//		if (listPage != null && listPage.getSize() > 0) {
//			res.setRows(listPage.getContent());
//			res.setTotalPage(listPage.getTotalPages());
//			res.setPage(req.getPage());
//			res.setRow(req.getRows());
//			res.setTotal(listPage.getTotalElements());
//			return res;
//		}
//		return null;
//	}
//
//	public Specification<AuthenticationVo> buildSpecification(final AuthenticationForm req) {
//		Specification<AuthenticationVo> spec = new Specification<AuthenticationVo>() {
//			@Override
//			public Predicate toPredicate(Root<AuthenticationVo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
//				List<Predicate> bigList = new ArrayList<Predicate>();
//				if (!StringUtil.isEmpty(req.getUserOid()))
//					bigList.add(cb.equal(root.get("userOid").as(String.class), req.getUserOid()));
//				if (!StringUtil.isEmpty(req.getRequestNo()))
//					bigList.add(cb.like(root.get("requestNo").as(String.class), "%" + req.getRequestNo() + "%"));
//				if (!StringUtil.isEmpty(req.getCardNo()))
//					bigList.add(cb.like(root.get("cardNo").as(String.class), "%" + req.getCardNo() + "%"));
//				if (!StringUtil.isEmpty(req.getBankCode()))
//					bigList.add(cb.like(root.get("bankCode").as(String.class), "%" + req.getBankCode() + "%"));
//				if (!StringUtil.isEmpty(req.getType()))
//					bigList.add(cb.like(root.get("type").as(String.class), "%" + req.getType() + "%"));
//				if (!StringUtil.isEmpty(req.getStatus()))
//					bigList.add(cb.equal(root.get("status").as(String.class), req.getStatus()));
//
//				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
//				if (req.getQueryType() == 0) {
//					query.orderBy(cb.desc(root.get("createTime")));
//				} else {
//					query.orderBy(cb.asc(root.get("createTime")));
//				}
//				// 条件查询
//				return query.getRestriction();
//			}
//		};
//		return spec;
//	}
//
//}