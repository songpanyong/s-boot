package com.guohuai.boot.account.service.accountbindcardaudit;

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
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.AccountBindCardAuditDao;
import com.guohuai.boot.account.entity.AccountBindCardAuditEntity;
import com.guohuai.boot.account.form.AccountBindCardAuditForm;
import com.guohuai.boot.account.res.AccountBindCardAuditPageResponse;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.DesPlus;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: TODO
 * @author ZJ
 * @date 2018年1月19日 下午6:14:50
 * @version V1.0
 */
@Slf4j
@Component
public class AccountBindCardAuditQueryService {
	@Autowired
	private AccountBindCardAuditDao accountBindCardAuditDao;

	/**
	 * 根据用户id获取绑卡审核信息
	 * @param userOid 用户id
	 * @return 绑卡信息
	 */
	public AccountBindCardAuditEntity auditInfo(String userOid) {
		log.info("根据用户id获取绑卡审核信息,请求参数{}", userOid);
		AccountBindCardAuditEntity entity = accountBindCardAuditDao.findByUserOid(userOid);
		entity.setCardNo(DesPlus.decrypt(entity.getCardNo()));
		entity.setCertificateNo(DesPlus.decrypt(entity.getCertificateNo()));
		log.info("根据用户id获取绑卡审核信息,返回结果{}", entity);
		return entity;
	}

	/**
	 * 绑卡审核页面查询
	 * @param req 查询参数
	 * @return page页面
	 */
	public AccountBindCardAuditPageResponse page(AccountBindCardAuditForm req) {
		log.info("平台绑卡审核分页查询,请求参数{},", JSONObject.toJSONString(req));
		Page<AccountBindCardAuditEntity> listPage = accountBindCardAuditDao.findAll(buildSpecification(req),
				new PageRequest(req.getPage() - 1, req.getRows()));
		AccountBindCardAuditPageResponse res = new AccountBindCardAuditPageResponse();
		if (listPage != null && listPage.getSize() > 0) {
			res.setRows(listPage.getContent());
			res.setTotalPage(listPage.getTotalPages());
			res.setPage(req.getPage());
			res.setRow(req.getRows());
			res.setTotal(listPage.getTotalElements());
			for (AccountBindCardAuditEntity entity : listPage) {
				entity.setCardNo(DesPlus.decrypt(entity.getCardNo()));
				entity.setCertificateNo(DesPlus.decrypt(entity.getCertificateNo()));
			}
			return res;
		}
		return null;
	}

	/**
	 * 组装分页查询参数
	 * @param req 查询参数
	 * @return 组装结果
	 */
	public Specification<AccountBindCardAuditEntity> buildSpecification(final AccountBindCardAuditForm req) {
		Specification<AccountBindCardAuditEntity> spec = new Specification<AccountBindCardAuditEntity>() {
			@Override
			public Predicate toPredicate(Root<AccountBindCardAuditEntity> root, CriteriaQuery<?> query,
					CriteriaBuilder cb) {
				List<Predicate> bigList = new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(req.getUserOid()))// 平台userOid
					bigList.add(cb.equal(root.get("userOid").as(String.class), req.getUserOid()));
				if (!StringUtil.isEmpty(req.getPlatformName()))// 平台名称
					bigList.add(cb.equal(root.get("platformName").as(String.class), req.getPlatformName()));
				if (!StringUtil.isEmpty(req.getAuditStatus()))// 审核状态
					bigList.add(cb.equal(root.get("auditStatus").as(String.class), req.getAuditStatus()));
				if (!StringUtil.isEmpty(req.getRealName()))// 绑卡姓名、名称
					bigList.add(cb.equal(root.get("realName").as(String.class), req.getRealName()));
				if (!StringUtil.isEmpty(req.getAccountBankType()))// 绑卡银行类型，企业个人
					bigList.add(cb.equal(root.get("accountBankType").as(String.class), req.getAccountBankType()));
				if (!StringUtil.isEmpty(req.getBeginTime())) {// 开始时间
					java.util.Date beginDate = DateUtil
							.beginTimeInMillis(DateUtil.parseDate(req.getBeginTime(), Constant.fomat));
					bigList.add(cb.greaterThanOrEqualTo(root.get("createTime").as(Timestamp.class),
							new Timestamp(beginDate.getTime())));
				}
				if (!StringUtil.isEmpty(req.getEndTime())) {// 结束时间
					java.util.Date beginDate = DateUtil
							.endTimeInMillis(DateUtil.parseDate(req.getEndTime(), Constant.fomat));
					bigList.add(cb.lessThanOrEqualTo(root.get("createTime").as(Timestamp.class),
							new Timestamp(beginDate.getTime())));
				}
				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
				query.orderBy(cb.asc(root.get("createTime")));
				// 条件查询
				return query.getRestriction();
			}
		};
		return spec;
	}
}