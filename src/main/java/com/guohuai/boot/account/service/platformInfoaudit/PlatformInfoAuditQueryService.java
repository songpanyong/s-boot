package com.guohuai.boot.account.service.platformInfoaudit;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.PlatformInfoAuditDao;
import com.guohuai.boot.account.entity.PlatformChangeRecordsEntity;
import com.guohuai.boot.account.entity.PlatformInfoAuditEntity;
import com.guohuai.boot.account.form.PlatformInfoAuditForm;
import com.guohuai.boot.account.res.PlatformInfoAuditResponse;
import com.guohuai.boot.account.service.PlatformChangeRecordsService;
import com.guohuai.component.util.ApplyAuditTypeEnum;
import com.guohuai.component.util.Constant;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: TODO
 * @author ZJ
 * @date 2018年1月19日 下午6:21:48
 * @version V1.0
 */
@Slf4j
@Service
public class PlatformInfoAuditQueryService {
	@Autowired
	private PlatformInfoAuditDao plarformInfoAuditDao;
	@Autowired
	private PlatformChangeRecordsService platformChangeRecordsService;

	/**
	 * 平台信息审核分页查询
	 * @param auditForm 查询参数
	 * @return 查询结果
	 */
	public PlatformInfoAuditResponse page(PlatformInfoAuditForm auditForm) {
		log.info("审核记录查询,{},", JSONObject.toJSONString(auditForm));
		Page<PlatformInfoAuditEntity> listPage = plarformInfoAuditDao.findAll(buildSpecification(auditForm),
				new PageRequest(auditForm.getPage() - 1, auditForm.getRows()));
		PlatformInfoAuditResponse res = new PlatformInfoAuditResponse();
		if (listPage != null && listPage.getSize() > 0) {
			res.setRows(listPage.getContent());
			res.setTotalPage(listPage.getTotalPages());
			res.setPage(auditForm.getPage());
			res.setRow(auditForm.getRows());
			res.setTotal(listPage.getTotalElements());
			return res;
		}
		return null;
	}

	/**
	 * 分页查询参数组装
	 * @param auditForm 请求参数
	 * @return 组装结果
	 */
	public Specification<PlatformInfoAuditEntity> buildSpecification(final PlatformInfoAuditForm auditForm) {
		Specification<PlatformInfoAuditEntity> spec = new Specification<PlatformInfoAuditEntity>() {
			@Override
			public Predicate toPredicate(Root<PlatformInfoAuditEntity> root, CriteriaQuery<?> query,
					CriteriaBuilder cb) {
				List<Predicate> bigList = new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(auditForm.getUserName()))// 平台名称
					bigList.add(cb.equal(root.get("userName").as(String.class), auditForm.getUserName()));
				if (!StringUtil.isEmpty(auditForm.getUserType()))// 平台类型
					bigList.add(cb.equal(root.get("userType").as(String.class), auditForm.getUserType()));
				if (!StringUtil.isEmpty(auditForm.getAuditStatus()))// 审核状态
					bigList.add(cb.equal(root.get("auditStatus").as(String.class), auditForm.getAuditStatus()));
				if (!StringUtil.isEmpty(auditForm.getBeginTime())) {// 开始时间
					java.util.Date beginDate = DateUtil.parseDate(auditForm.getBeginTime(), Constant.fomat);
					bigList.add(cb.greaterThanOrEqualTo(root.get("createTime").as(Timestamp.class),
							new Timestamp(beginDate.getTime())));
				}
				if (!StringUtil.isEmpty(auditForm.getEndTime())) {// 结束时间
					java.util.Date beginDate = DateUtil.parseDate(auditForm.getEndTime(), Constant.fomat);
					bigList.add(cb.lessThanOrEqualTo(root.get("createTime").as(Timestamp.class),
							new Timestamp(beginDate.getTime())));
				}
				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
				query.orderBy(cb.desc(root.get("createTime")));
				// 条件查询
				return query.getRestriction();
			}
		};
		return spec;
	}

	/**
	 * 根据审核oid查询修改记录
	 * @param auditOid 审核oid
	 * @return 查询结果
	 */
	public List<PlatformChangeRecordsEntity> getChangeRecords(String auditOid) {
		log.info("根据审核oid查询修改记录，请求参数：{}", auditOid);
		List<PlatformChangeRecordsEntity> list = platformChangeRecordsService.findByAuditOid(auditOid);
		log.info("根据审核oid查询修改记录，查询结果：{}", list);
		return list;
	}

	/**
	 * 根据用户userOid查询变更记录
	 * @param userOid 平台userOid
	 * @return 变更记录
	 */
	public List<PlatformChangeRecordsEntity> paltformChangeRecords(String userOid) {
		log.info("根据用户userOid查询变更记录，请求参数：{}", userOid);
		// 根据平台userOid获取平台变更审核中信息
		PlatformInfoAuditEntity entity = plarformInfoAuditDao.findByUserOidAndStatusAndType(userOid,
				ApplyAuditTypeEnum.PLATFORM_INFO_CHANGE.getCode(), Constant.AUDIT);
		if (entity != null) {
			List<PlatformChangeRecordsEntity> list = platformChangeRecordsService.findByAuditOid(entity.getOid());
			log.info("根据用户userOid查询变更记录，返回结果：{}", list);
			return list;
		}
		log.info("根据用户userOid查询变更记录，返回结果不存在");
		return null;
	}
}