package com.guohuai.boot.account.service.accountevent;

import static com.guohuai.boot.account.validate.service.AccountEventServiceVal.valQueryAccountEventInfo;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.AccountEventChildDao;
import com.guohuai.boot.account.dao.AccountEventDao;
import com.guohuai.boot.account.dao.PlatformChangeRecordsDao;
import com.guohuai.boot.account.dto.AccountEventReqDTO;
import com.guohuai.boot.account.dto.AccountEventResDTO;
import com.guohuai.boot.account.entity.AccountEventChildEntity;
import com.guohuai.boot.account.entity.AccountEventEntity;
import com.guohuai.boot.account.entity.PlatformChangeRecordsEntity;
import com.guohuai.boot.account.form.AccountEventForm;
import com.guohuai.boot.account.res.AccountEventEffectInfoResponse;
import com.guohuai.boot.account.res.AccountEventPageResponse;
import com.guohuai.component.util.ErrorEnum;

import lombok.extern.slf4j.Slf4j;

/**   
 * @Description: TODO 
 * @author ZJ   
 * @date 2018年1月19日 下午6:17:20 
 * @version V1.0   
 */
@Slf4j
@Service
public class AccountEventQueryService {
	@Autowired
	private AccountEventDao accountEventDao;
	@Autowired
	private AccountEventChildDao accountEventChildDao;
	@Autowired
	private PlatformChangeRecordsDao platformChangeRecordsDao;
	
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
}