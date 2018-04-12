package com.guohuai.boot.pay.service;


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

import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.pay.dao.BankCallbackLogDao;
import com.guohuai.boot.pay.form.BankCallbackLogForm;
import com.guohuai.boot.pay.res.BankCallbackLogVoRes;
import com.guohuai.boot.pay.vo.BankCallbackLogVo;

@Service
public class BankCallbackLogService {
	private final static Logger log = LoggerFactory.getLogger(BankCallbackLogService.class);
	@Autowired
	private BankCallbackLogDao bankCallbackLogDao;
	
	public BankCallbackLogVoRes findBankCallbackLogByCBOid(BankCallbackLogForm req){
		log.info("回调日志查询,callBackOid = " + req.getCallBackOid());
		
		Page<BankCallbackLogVo> listPage=bankCallbackLogDao.findAll(buildSpecification(req),new PageRequest(req.getPage() - 1, req.getRows()));
		BankCallbackLogVoRes res =new BankCallbackLogVoRes();
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
	
	public Specification<BankCallbackLogVo> buildSpecification(final BankCallbackLogForm req) {
		Specification<BankCallbackLogVo> spec = new Specification<BankCallbackLogVo>() {
			@Override
			public Predicate toPredicate(Root<BankCallbackLogVo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> bigList =new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(req.getCallBackOid()))
					bigList.add(cb.equal(root.get("callBackOid").as(String.class),req.getCallBackOid()));
				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
				query.orderBy(cb.desc(root.get("createTime")));
				
				// 条件查询
				return query.getRestriction();
			}
		};
		return spec;
	}
	
}