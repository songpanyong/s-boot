package com.guohuai.boot.account.service;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.guohuai.component.util.DesPlus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.CardQueryRequest;
import com.guohuai.account.api.request.TiedCardRequest;
import com.guohuai.account.api.response.CardListResponse;
import com.guohuai.account.api.response.CardQueryResponse;
import com.guohuai.account.api.response.TiedCardResponse;
import com.guohuai.account.api.response.entity.SignDto;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.SignDao;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.SignEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.pay.dao.ProtocolDao;
import com.guohuai.boot.pay.vo.ProtocolVo;
import com.guohuai.component.util.CardTypeEnum;
import com.guohuai.component.util.Constant;


@Service
public class SignService {
	private final static Logger log = LoggerFactory.getLogger(SignService.class);
	@Autowired
	private SignDao signDao;
	
	@Autowired
	private ProtocolDao protocolDao;
	
	@Autowired
	private UserInfoService userInfoService;
	
	@Autowired
	private UserInfoDao userInfoDao;
	
	/**
	 * 绑卡
	* @Title: tiedCard
	* @Description: TODO
	* @param @param req
	* @param @return 
	* @return TiedCardResponse
	* @throws
	 */
	public TiedCardResponse tiedCard(TiedCardRequest req){
		Timestamp time = new Timestamp(System.currentTimeMillis());
		TiedCardResponse resp = new TiedCardResponse();
		SignEntity entity = new SignEntity();
		BeanUtils.copyProperties(req, entity);
		
		SignEntity signEntity = signDao.findByUserOidStatus(req.getUserOid(),SignEntity.STATUS_SIGN);
		if(signEntity != null){
			if(checkSameInfo(req,signEntity)){
				resp.setErrorMessage("您已绑定过此银行卡");
				resp.setReturnCode(Constant.SUCCESS);
				return resp;
			}
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("您已绑卡无需再绑卡！");
			return resp;
		}
		
		
		entity.setOid(StringUtil.uuid());
		entity.setIdentityNo(DesPlus.encrypt(req.getIdentityNo()));
		entity.setBankCard(DesPlus.encrypt(req.getBankCard()));
		entity.setStatus(SignEntity.STATUS_SIGN);
		entity.setCardType(CardTypeEnum.DEBITCARD.getCode());
		entity.setUpdateTime(time);
		entity.setCreateTime(time);
		signDao.save(entity);
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");
		resp.setUserOid(entity.getUserOid());
		return resp;
	}
	
	
	/**
	 * 四要素解绑
	 * @param req
	 * @return
	 */
	public TiedCardResponse unLockCard(TiedCardRequest req){
		log.info("{}：账户解绑：{}",req.getUserOid(),JSONObject.toJSONString(req));
		Timestamp time=new Timestamp(System.currentTimeMillis());
		TiedCardResponse resp = new TiedCardResponse();
		
		//查询用户是否解绑过
		SignEntity signEntity = signDao.findByUserOid(req.getUserOid(),DesPlus.encrypt(req.getBankCard()));
		if(signEntity==null){
			resp.setErrorMessage("您未绑卡成功或解绑卡号不正确，解绑失败！");
			resp.setReturnCode(Constant.FAIL);
			log.info("账户解绑返回：{}",JSONObject.toJSONString(resp));
			return resp;
		}else{
			signEntity.setStatus(SignEntity.STATUS_SURRENDER);
			signEntity.setUpdateTime(time);
			signDao.save(signEntity);
			
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
			log.info("账户解绑返回：{}",JSONObject.toJSONString(resp));
			return resp;
		}
	}
	
	/**
	 * 验证用户绑定的卡和上次绑定成功的是否一样
	 * @param req
	 * @param elementVo
	 * @return
	 */
	private boolean checkSameInfo(TiedCardRequest req, SignEntity elementVo) {
		if (req.getBankCard().trim().equals(DesPlus.decrypt(elementVo.getBankCard().trim()))
				&& req.getRealName().trim().equals(elementVo.getRealName())
				&& req.getPhone().trim().equals(elementVo.getPhone().trim())
				&& req.getIdentityNo().trim().equals(DesPlus.decrypt(elementVo.getIdentityNo().trim()))) {
			return true;
		}
		return false;
	}
	
	/**
	 * 绑卡查询
	* @Title: cardQueryList
	* @Description: TODO
	* @param @param req
	* @param @return 
	* @return CardListResponse
	* @throws
	 */
	public CardListResponse cardQueryList(final CardQueryRequest req){
		int page = req.getPage();
		int rows = req.getRows();
		if (page < 1) {
			page = 1;
		}
		if (rows < 1) {
			rows = 1;
		}
		
		Direction sortDirection = Direction.ASC;
		if (!"ASC".equals(req.getSort())) {
			sortDirection = Direction.DESC;
		}
		
		String sortField = req.getSortField();
		if(StringUtil.isEmpty(sortField)){
			sortField = "createTime";
		}
		Pageable pageable = new PageRequest(page - 1, rows, new Sort(new Order(sortDirection, sortField)));
		CardListResponse resp = new CardListResponse();
		
		
		Specification<ProtocolVo> spec = new Specification<ProtocolVo>() {
			public Predicate toPredicate(Root<ProtocolVo> root,
					CriteriaQuery<?> query, CriteriaBuilder cb) {
				 List<Predicate> list = new ArrayList<Predicate>();
				 //根据用户id查询
				 String userOid = req.getUserOid();
				if (!StringUtil.isEmpty(userOid)) {
					list.add(cb.equal(root.get("userOid").as(String.class), userOid));
				} else if (!StringUtil.isEmpty(req.getPhone())) {
					UserInfoEntity userInfoEntity = userInfoService.getAccountUserByPhone(req.getPhone());
					if (null == userInfoEntity) { //构造一个错误的userOid
						list.add(cb.equal(root.get("userOid").as(String.class), StringUtil.uuid()));
					} else {
						list.add(cb.equal(root.get("userOid").as(String.class), userInfoEntity.getUserOid()));
					}
				}else if(!StringUtil.isEmpty(req.getRealName())) {
					UserInfoEntity userInfoEntity = userInfoDao.findByRealName(req.getRealName());
					if (null == userInfoEntity) { //构造一个错误的userOid
						list.add(cb.equal(root.get("userOid").as(String.class), StringUtil.uuid()));
					} else {
						list.add(cb.equal(root.get("userOid").as(String.class), userInfoEntity.getUserOid()));
					}
				}
				String userType=req.getUserType();
				if(!StringUtil.isEmpty(userType)){
					 Predicate pre = cb.equal(root.get("userType").as(String.class), userType );
					 list.add(pre);
				}
				String phone=req.getReservedCellPhone();
				if(!StringUtil.isEmpty(phone)){
					 Predicate pre = cb.equal(root.get("phone").as(String.class), phone );
					 list.add(pre);
				}
				
				String cardType =req.getCardType();
				if(!StringUtil.isEmpty(cardType)){
					 Predicate pre = cb.equal(root.get("cardType").as(String.class), cardType );
					 list.add(pre);
				 }
				
				String startTime = req.getStartTime();
				if (!StringUtil.isEmpty(startTime)) {
					Date beginDate = DateUtil.parseDate(startTime, "yyyy-MM-dd HH:mm:ss");
					list.add(cb.greaterThanOrEqualTo(root.get("createTime").as(Timestamp.class),
							new Timestamp(beginDate.getTime())));
				}

				String endTime = req.getEndTime();
				if (!StringUtil.isEmpty(endTime)) {
					Date endDate = DateUtil.parseDate(req.getEndTime(), "yyyy-MM-dd HH:mm:ss");
					list.add(cb.lessThanOrEqualTo(root.get("createTime").as(Timestamp.class),
							new Timestamp(endDate.getTime())));
				}
				 /*//真实姓名
				 String realName = req.getRealName();
				 if(!StringUtil.isEmpty(realName)){
					 Predicate pre = cb.like(root.get("realName").as(String.class), "%" + realName + "%");
					 list.add(pre);
				 }*/
				/* //身份证
				 String identityNo = req.getIdentityNo();
				 if(!StringUtil.isEmpty(identityNo)){
					 identityNo = DesPlus.encrypt(req.getIdentityNo());
					 Predicate pre = cb.equal(root.get("certificateNo").as(String.class),identityNo);
					 list.add(pre);
				 }*/
				/* //银行号
				 String  bankCard = req.getBankCard();
				 if(!StringUtil.isEmpty(bankCard)){
					 bankCard = DesPlus.encrypt(req.getBankCard());
					 Predicate pre = cb.equal(root.get("cardNo").as(String.class),bankCard);
					 list.add(pre);
				 }*/
				
				 
				/*//绑卡状态
				 String status = req.getStatus();
				 if(!StringUtil.isEmpty(status)){
					 Predicate pre = cb.equal(root.get("status").as(String.class),status);
					 list.add(pre);
				 }*/
				 
				 Predicate[] p = new Predicate[list.size()];  
			     return cb.and(list.toArray(p));  
			}
		};
		
		
		Page<ProtocolVo> result = protocolDao.findAll(spec, pageable);
		
		if (null != result && result.getTotalElements() != 0) {
			resp.setTotal(result.getTotalElements());
			for (ProtocolVo protocolVo : result.getContent()) {
				SignDto tempEntity = new  SignDto();
				BeanUtils.copyProperties(protocolVo, tempEntity,new String[] {"createTime"});
				tempEntity.setCreateTime(DateUtil.format(protocolVo.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
				tempEntity.setBankCard(DesPlus.decrypt(protocolVo.getCardNo()));
				tempEntity.setIdentityNo(DesPlus.decrypt(protocolVo.getCertificateNo()));
				tempEntity.setReservedCellPhone(protocolVo.getPhone());
				UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(tempEntity.getUserOid());
                if(null!=userInfoEntity) {
                	tempEntity.setPhone(userInfoEntity.getPhone());
                	tempEntity.setAccountName(userInfoEntity.getName());
                }
				CardQueryResponse qresp = new CardQueryResponse(tempEntity);
				resp.getRows().add(qresp);
			}
			
		}
		return resp;
	}
	
	/**
	 * 根据用户id获取已绑卡信息
	 * @param userOid 用户id
	 * @return
	 */
	public ProtocolVo getBindCardByUserOid(String userOid) {
		log.info("获取用户绑卡信息{}",userOid);
		ProtocolVo vo = protocolDao.findOneByUserOidAndStatus(userOid, "1");
		if(vo != null){
			String cardNo = DesPlus.decrypt(vo.getCardNo());
			String certificateNo = DesPlus.decrypt(vo.getCertificateNo());
			vo.setCardNo(cardNo);
			vo.setCertificateNo(certificateNo);
		}
		log.info("获取用户绑卡信息，返回结果：{}", vo);
		return vo;
	}
	
}