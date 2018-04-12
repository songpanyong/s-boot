package com.guohuai.boot.account.service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountTransferRequest;
import com.guohuai.account.api.request.entity.TradeEvent;
import com.guohuai.account.component.util.EventTypeEnum;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.*;
import com.guohuai.boot.account.dto.AccountEventReqDTO;
import com.guohuai.boot.account.dto.AccountEventResDTO;
import com.guohuai.boot.account.entity.*;
import com.guohuai.boot.account.form.AccountEventForm;
import com.guohuai.boot.account.res.AccountEventEffectInfoResponse;
import com.guohuai.boot.account.res.AccountEventPageResponse;
import com.guohuai.boot.account.res.AccountEventResponse;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.ErrorEnum;
import com.guohuai.component.util.UserTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

import static com.guohuai.boot.account.validate.service.AccountEventServiceVal.valQueryAccountEventInfo;

/**
 * @ClassName: AccountEventService
 * @Description: 账户事件
 * @author chendonghui
 * @date 2017年12月8日10:22:18
 */
@Slf4j
@Service
public class AccountEventService {
	@Autowired
	private AccountEventDao accountEventDao;
	@Autowired
	private AccountEventChildDao accountEventChildDao;
	@Autowired
	private PlatformChangeRecordsDao platformChangeRecordsDao;
	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private PlatformAccountInfoDao platformAccountInfoDao;
	
	@Autowired
	private EntityManager entityManager;
	
	/**
	 * 根据平台用户id查询平台信息
	 * @param userOid 平台id
	 * @return 平台信息
	 */
	public AccountEventEntity getPlatformInfoByUserOid(String userOid) {
		log.info("据平台用户id查询平台信息请求参数:{}", userOid);
		AccountEventEntity entity = accountEventDao.findByUserOid(userOid);
		log.info("据平台用户id查询平台信息返回结果:{}", entity);
		return entity;
	}
	
	/**
	 * 登账事件分页查询
	 * @param req 查询参数
	 * @return 查询结果
	 */
	public AccountEventPageResponse page(AccountEventForm req) {
		log.info("登账事件分页查询,请求参数{},",JSONObject.toJSONString(req));
		Page<AccountEventEntity> listPage=accountEventDao.findAll(buildSpecification(req), 
				new PageRequest(req.getPage() - 1, req.getRows()));
		AccountEventPageResponse res =new AccountEventPageResponse();
		if (listPage != null && listPage.getSize() > 0) {
			res.setRows(listPage.getContent());
			res.setTotalPage(listPage.getTotalPages());
			res.setPage(req.getPage());
			res.setRow(req.getRows());
			res.setTotal(listPage.getTotalElements());
			return res;
		}
		return res;
	}
	
	/**
	 * 组装登账事件查询参数
	 * @param req 查询参数
	 * @return 组装参数
	 */
	public Specification<AccountEventEntity> buildSpecification(final AccountEventForm req) {
		Specification<AccountEventEntity> spec = new Specification<AccountEventEntity>() {
			@Override
			public Predicate toPredicate(Root<AccountEventEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> bigList =new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(req.getUserOid()))//平台userOid
					bigList.add(cb.equal(root.get("userOid").as(String.class),req.getUserOid()));
				if (!StringUtil.isEmpty(req.getEventName()))//登账事件名称
					bigList.add(cb.equal(root.get("eventName").as(String.class),req.getEventName()));
				if (!StringUtil.isEmpty(req.getEventType()))//登账事件类型
					bigList.add(cb.equal(root.get("eventType").as(String.class),req.getEventType()));
				if (!StringUtil.isEmpty(req.getSetUpStatus()))//等账事件设置状态
					bigList.add(cb.equal(root.get("setUpStatus").as(String.class),req.getSetUpStatus()));
				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
				query.orderBy(cb.desc(root.get("setUpStatus")),cb.asc(root.get("createTime")));
				// 条件查询
				return query.getRestriction();
			}
		};
		return spec;
	}

	/**
	 * 查看登账事件设置生效状态
	 * @param req 登账事件
	 * @return 设置信息
	 */
	public AccountEventEffectInfoResponse getEffectInfo(AccountEventForm req) {
		log.info("登账事件变更生效信息查询,请求参数{},",JSONObject.toJSONString(req));
		AccountEventEffectInfoResponse resp = new AccountEventEffectInfoResponse();
		//查询事件信息
		AccountEventEntity event = accountEventDao.findOne(req.getOid());
		//查看变更申请
		PlatformChangeRecordsEntity record = platformChangeRecordsDao.findByEventOid(event.getOid());
		resp.setSetUpTime(record.getCreateTime().toString());
		resp.setEffectiveTimeType(record.getEffectiveTimeType());
		resp.setSetUpStatus(event.getSetUpStatus());
		log.info("登账事件变更生效信息查询,返回结果{},",JSONObject.toJSONString(resp));
		return resp;
	}
	
	/**
	 * 查询登账事件信息
	 * @param req
	 * @return
	 */
	public AccountEventResDTO queryAccountEventInfo(AccountEventReqDTO req) {
		log.info("查询登账事件信息请求参数：accountEventReqDTO = {}", req);
		AccountEventResDTO result = new AccountEventResDTO();

		result = valQueryAccountEventInfo(req);
		if (!StringUtils.equals(ErrorEnum.SUCCESS.getCode(), result.getReturnCode())) {
			log.debug(result.getErrorMessage());
			return result;
		}

		AccountEventEntity accountEventEntity = this.accountEventDao
				.findByUserOidAndTransTypeAndEventType(req.getUserOid(), req.getTransType(), req.getEventType());
		if (null == accountEventEntity) {
			log.debug("根据平台Id、交易类型和事件类型查询登账事件, userOid = {}, transType = {}, eventType = {}", req.getUserOid(),
					req.getTransType(), req.getEventType());
			result.setError("9058");
			return result;
		}

		List<AccountEventChildEntity> accountEventChildEntitys = this.accountEventChildDao
				.findByEventOid(accountEventEntity.getOid());
		if (null == accountEventChildEntitys || accountEventChildEntitys.isEmpty()) {
			log.debug("根据事件Id查询登账子事件, eventOid = {}", accountEventEntity.getOid());
			result.setError("9059");
			return result;
		}

		result.setAccountEventEntity(accountEventEntity);
		result.setAccountEventChildEntitys(accountEventChildEntitys);
		log.info("查询登账事件信息结果：" + result.getErrorMessage());
		return result;
	}

	/**
	 * 查询登账事件信息
	 * @param userOid
	 * @param transType
	 * @param eventType
	 * @return
	 */
	public AccountEventResDTO queryAccountEventInfo(String userOid, String transType, String eventType) {
		AccountEventReqDTO req = new AccountEventReqDTO();
		req.setUserOid(userOid);
		req.setTransType(transType);
		req.setEventType(eventType);
		return queryAccountEventInfo(req);
	}

	/**
	 * 登账事件适配
	 * @param req 交易请求参数
	 * @param tradeEvent 事件信息
	 * @return 出入款账户信息及事件信息
	 */
	@Transactional
	public AccountEventResponse accountEventAdapter(
			AccountTransferRequest req, TradeEvent tradeEvent) {
		AccountEventResponse resp = new AccountEventResponse();
		resp.setRequestNo(req.getRequestNo());
		resp.setOrderNo(req.getOrderNo());
		resp.setOrderType(req.getOrderType());
		resp.setRemark(req.getRemark());
		resp.setTradeEvent(tradeEvent);
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("获取账户信息成功");
		// 获取事件
		AccountEventChildEntity accountEventChildEntity = accountEventChildDao
				.findByChildEventType(tradeEvent.getEventType());
		resp.setAccountEventChildEntity(accountEventChildEntity);
		String returnCode = "";
		String errorMsg = "";
		// 查询出账账户信息
		resp = this.getInputAccountEntity(accountEventChildEntity, req, resp);
		if(!Constant.SUCCESS.equals(resp.getReturnCode())){
			returnCode = resp.getReturnCode();
			errorMsg = resp.getErrorMessage();
		}
		// 查询入账账户信息
		resp = this.getOutputAccountEntity(accountEventChildEntity, req, resp);
		if(!"".equals(returnCode)){
			resp.setReturnCode(returnCode);
			resp.setErrorMessage(errorMsg);
		}
		// 刷新缓存
		entityManager.refresh(resp.getOutputAccountEntity());
		// 判断出账账户余额是否充足
		if (resp.getOutputAccountEntity().getBalance().compareTo(tradeEvent.getBalance()) < 0) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("账户余额不足");
		}
		return resp;
	}

	/**
	 * 回去出账账户信息
	 * @param accountEventChildEntity 子事件
	 * @param req 交易请求参数
	 * @param resp 交易返回信息
	 *
	 *             非实时兑付出款账户 实时兑付
	 * @return 出账账户信息
	 */
	private AccountEventResponse getOutputAccountEntity(
			AccountEventChildEntity accountEventChildEntity,
			AccountTransferRequest req, AccountEventResponse resp) {
		PlatformAccountInfoEntity platformAccountInfo = null;
		String userOid = req.getUserOid();
		String outputAccountNo = accountEventChildEntity.getOutputAccountNo();
		AccountInfoEntity outputAccountEntity = null;
		if(EventTypeEnum.REDEEM_T1.getCode().equals(resp.getTradeEvent().getEventType()) 
				|| EventTypeEnum.NETTING_OUTCOME.getCode().equals(resp.getTradeEvent().getEventType())){
			outputAccountNo = req.getProductAccountNo();
		}
		if (StringUtil.isEmpty(outputAccountNo)) {
			if (UserTypeEnum.INVESTOR.getCode().equals(
					accountEventChildEntity.getInputUserType())) {
				userOid = req.getUserOid();
			} else if (UserTypeEnum.PUBLISHER.getCode().equals(
					accountEventChildEntity.getInputUserType())) {
				userOid = req.getPublisherUserOid();
			}
			outputAccountEntity = accountInfoDao.findAccountByAccountTypeAndUserOid(
							accountEventChildEntity.getOutputAccountType(), userOid);
			outputAccountNo = outputAccountEntity.getAccountNo();
		} else {
			outputAccountEntity = accountInfoDao
					.findByAccountNo(outputAccountNo);
			userOid = outputAccountEntity.getUserOid();
			// 判断平台户是否被开启
			platformAccountInfo = platformAccountInfoDao
					.findByAccountNo(outputAccountNo);
			if (PlatformAccountInfoEntity.STATUS_STOP
					.equals(platformAccountInfo.getAccountStatus())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("出款账户被停用");
			}
		}
		resp.setOutputAccountEntity(outputAccountEntity);
		return resp;
	}

	/**
	 * 获取入账账户信息
	 * @param accountEventChildEntity 子事件
	 * @param req 交易请求参数
	 * @param resp 交易返回参数
 	 * @return 入账账户信息
	 */
	private AccountEventResponse getInputAccountEntity(
			AccountEventChildEntity accountEventChildEntity,
			AccountTransferRequest req, AccountEventResponse resp) {
		PlatformAccountInfoEntity platformAccountInfo = null;
		AccountInfoEntity inputAccountEntity = null;
		String userOid = req.getUserOid();
		String inputAccountNo = accountEventChildEntity.getInputAccountNo();
		if(EventTypeEnum.INVEST_T1.getCode().equals(resp.getTradeEvent().getEventType()) 
				|| EventTypeEnum.NETTING_DEPOSIT.getCode().equals(resp.getTradeEvent().getEventType())){
			inputAccountNo = req.getProductAccountNo();
		}
		if (StringUtil.isEmpty(inputAccountNo)) {
			if (UserTypeEnum.INVESTOR.getCode().equals(
					accountEventChildEntity.getInputUserType())) {
				userOid = req.getUserOid();
			} else if (UserTypeEnum.PUBLISHER.getCode().equals(
					accountEventChildEntity.getInputUserType())) {
				userOid = req.getPublisherUserOid();
			}
			inputAccountEntity = accountInfoDao.findAccountByAccountTypeAndUserOid(
							accountEventChildEntity.getInputAccountType(), userOid);
			inputAccountNo = inputAccountEntity.getAccountNo();
		} else {
			inputAccountEntity = accountInfoDao.findByAccountNo(inputAccountNo);
			userOid = inputAccountEntity.getUserOid();
			// 判断平台账户户是否被开启
			platformAccountInfo = platformAccountInfoDao.findByAccountNo(inputAccountNo);
			if (PlatformAccountInfoEntity.STATUS_STOP
					.equals(platformAccountInfo.getAccountStatus())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("入款账户被停用");
			}
		}
		resp.setInputAccountEntity(inputAccountEntity);
		return resp;
	}
}