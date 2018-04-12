package com.guohuai.boot.account.service;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.PlatformAccountInfoDao;
import com.guohuai.boot.account.dao.PlatformInfoAuditDao;
import com.guohuai.boot.account.dao.PlatformInfoDao;
import com.guohuai.boot.account.entity.PlatformAccountInfoEntity;
import com.guohuai.boot.account.entity.PlatformInfoAuditEntity;
import com.guohuai.boot.account.entity.PlatformInfoEntity;
import com.guohuai.boot.account.form.AccountInfoForm;
import com.guohuai.boot.account.form.PlatformInfoForm;
import com.guohuai.boot.account.res.PlatformInfoPageResponse;
import com.guohuai.boot.account.res.PlatformInfoResponse;
import com.guohuai.component.util.ApplyAuditTypeEnum;

@Service
public class PlatformInfoService {

	private final static Logger log = LoggerFactory.getLogger(PlatformInfoService.class);

	@Autowired
	private PlatformInfoDao platformInfoDao;
	@Autowired
	private PlatformAccountInfoDao platformAccountInfoDao;
	@Autowired
	private PlatformInfoAuditDao platformInfoAuditDao;
	
	/**
	 * 根据平台用户id查询平台信息
	 * @param userOid 平台id
	 * @return 平台信息
	 */
	public PlatformInfoEntity getPlatformInfoByUserOid(String userOid) {
		log.info("据平台用户id查询平台信息，请求参数：{}", userOid);
		PlatformInfoEntity entity = platformInfoDao.findByUserOid(userOid);
		log.info("据平台用户id查询平台信息，返回结果：{}", entity);
		return entity;
	}
	
	/**
	 * 根据平台平台用户名称查询平台信息
	 * @param platformName 平台名称
	 * @return 平台信息
	 */
	public PlatformInfoEntity getPlatformInfoByName(String platformName) {
		log.info("根据平台平台用户名称查询平台信息，请求参数：{}", platformName);
		PlatformInfoEntity entity = platformInfoDao.findByPlatformName(platformName);
		log.info("根据平台平台用户名称查询平台信息，返回结果：{}", entity);
		return entity;
	}
	
	/**
	 * 获取所有平台信息
	 * @return 平台信息List
	 */
	public List<PlatformInfoEntity> getAllPlatformInfo() {
		log.info("获取所有平台信息");
		List<PlatformInfoEntity> platformList = platformInfoDao.findAll();
		log.info("获取所有平台信息，返回结果：{}", platformList);
		return platformList;
	}

	/**
	 * 平台首页展示平台信息及账户信息
	 * @param userOid 平台userOid
	 * @return 平台信息及账户信息
	 */
	public PlatformInfoResponse platformInfo(String userOid) {
		log.info("查询平台信息及账户信息，请求参数:{}",userOid);
		PlatformInfoResponse resp = new PlatformInfoResponse();
		//查询平台信息
		PlatformInfoEntity entity = null;
		if(userOid == null||"".equals(userOid)){
			entity = platformInfoDao.findFirst();
		}else{
			entity = platformInfoDao.findByUserOid(userOid);
		}
		if(entity != null){
			resp.setBindCardStatus(entity.getBindCardStatus());
			resp.setPlatformName(entity.getPlatformName());
			resp.setPlatformStatus(entity.getPlatformStatus());
			resp.setUserOid(entity.getUserOid());
			//查询平台账户信息
			List<String> openList = platformAccountInfoDao.findByUserOidAndStatus(entity.getUserOid(), PlatformAccountInfoEntity.STATUS_RUN);
			List<String> closeList = platformAccountInfoDao.findByUserOidAndStatus(entity.getUserOid(), PlatformAccountInfoEntity.STATUS_STOP);
			resp.setOpenList(openList);
			resp.setCloseList(closeList);
		}
		String settleStatus = ApplyAuditTypeEnum.AUDIT_NO;
		//查询平台是否有待审核信息
		PlatformInfoAuditEntity platformInfoAuditEntity = platformInfoAuditDao.findAuditingByUserOid(entity.getUserOid());
		if(platformInfoAuditEntity != null){
			settleStatus = ApplyAuditTypeEnum.AUDIT_YES;
		}
		resp.setSettleStatus(settleStatus);
		log.info("查询平台信息及账户信息，返回结果:{}", resp);
		return resp;
	}

	/**
	 * 平台信息分页查询
	 * @param req 平台信息参数
	 * @return 查询结果
	 */
	public PlatformInfoPageResponse page(PlatformInfoForm req) {
		log.info("平台查询,{},",JSONObject.toJSONString(req));
		Page<PlatformInfoEntity> listPage=platformInfoDao.findAll(buildSpecification(req), 
				new PageRequest(req.getPage() - 1, req.getRows()));
		PlatformInfoPageResponse res =new PlatformInfoPageResponse();
		if (listPage != null && listPage.getSize() > 0) {
			res.setRows(listPage.getContent());
			res.setTotalPage(listPage.getTotalPages());
			res.setPage(req.getPage());
			res.setRow(req.getRows());
			res.setTotal(listPage.getTotalElements());
			return res;
		}
		return null;
	}
	
	/**
	 * 组装分页查询参数
	 * @param req 请求参数
	 * @return 组装参数
	 */
	public Specification<PlatformInfoEntity> buildSpecification(final PlatformInfoForm req) {
		Specification<PlatformInfoEntity> spec = new Specification<PlatformInfoEntity>() {
			@Override
			public Predicate toPredicate(Root<PlatformInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> bigList =new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(req.getUserOid()))//平台userOid
					bigList.add(cb.equal(root.get("userOid").as(String.class),req.getUserOid()));
				if (!StringUtil.isEmpty(req.getPlatformName()))//平台名称
					bigList.add(cb.equal(root.get("platformName").as(String.class),req.getPlatformName()));
				if (!StringUtil.isEmpty(req.getPlatformStatus()))//平台状态
					bigList.add(cb.equal(root.get("platformStatus").as(String.class),req.getPlatformStatus()));
				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
				query.orderBy(cb.desc(root.get("platformStatus")),cb.asc(root.get("createTime")));
				// 条件查询
				return query.getRestriction();
			}
		};
		return spec;
	}

	/**
	 * 查询所有平台List
	 * @return 平台List
	 */
	public List<PlatformInfoEntity> getPlatformInfoList() {
		log.info("查询所有平台List");
		List<PlatformInfoEntity> platformList = platformInfoDao.findAll();
		log.info("查询所有平台List,返回结果：{}", platformList);
		return platformList;
	}
	
	/**
	 * 可更换账户下拉列表查询
	 * @param req 平台userOid，账户类型，账户状态
	 * @return 可更换账户信息
	 */
	public List<PlatformAccountInfoEntity> getChangeAccountInfoList(AccountInfoForm req) {
		log.info("更换账户list查询:{}", JSONObject.toJSONString(req));
		List<PlatformAccountInfoEntity> platformAccountList = null;
		if(req.getAccountStatus() == null || "".equals(req.getAccountStatus())){
			platformAccountList = platformAccountInfoDao
					.findByUserOidAndType(req.getUserOid(), req.getAccountType());
		}else{
			platformAccountList = platformAccountInfoDao
					.findByUserOidAndTypeAndStatus(req.getUserOid(), req.getAccountType(), req.getAccountStatus());
		}
		log.info("更换账户list查询返回结果:{}", platformAccountList);
		return platformAccountList;
	}

	/**
	 * 修改平台绑卡状态
	 * @param userOid 平台用户id
	 * @param bindCardStatus 绑卡状态
	 */
	public void changeBindStatus(String userOid, String bindCardStatus) {
		log.info("修改平台绑卡状态,请求参数：用户userOid{}，绑卡状态{}", userOid, bindCardStatus);
		PlatformInfoEntity entity = platformInfoDao.findByUserOid(userOid);
		entity.setBindCardStatus(bindCardStatus);
		log.info("修改平台绑卡状态,返回结果：{}", entity);
		platformInfoDao.saveAndFlush(entity);
	}
	
}