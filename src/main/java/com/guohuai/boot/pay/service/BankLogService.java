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

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.pay.dao.BankLogDao;
import com.guohuai.boot.pay.form.BankLogForm;
import com.guohuai.boot.pay.res.BankLogVoRes;
import com.guohuai.boot.pay.vo.BankLogVo;

@Service
public class BankLogService {
	private final static Logger log = LoggerFactory.getLogger(BankLogService.class);
	@Autowired
	private BankLogDao bankLogDao;
	
	public BankLogVoRes page(BankLogForm req){
		log.info("{},交互日志查询,{},",req.getUserOid(),JSONObject.toJSONString(req));
		Page<BankLogVo> listPage=bankLogDao.findAll(buildSpecification(req),new PageRequest(req.getPage() - 1, req.getRows()));
		BankLogVoRes res =new BankLogVoRes();
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
	
	
	public Specification<BankLogVo> buildSpecification(final BankLogForm req) {
		Specification<BankLogVo> spec = new Specification<BankLogVo>() {
			@Override
			public Predicate toPredicate(Root<BankLogVo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> bigList =new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(req.getUserOid()))
					bigList.add(cb.equal(root.get("userOid").as(String.class),req.getUserOid()));
				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
				query.orderBy(cb.desc(root.get("createTime")));
				
				// 条件查询
				return query.getRestriction();
			}
		};
		return spec;
	}
	
	public BankLogVoRes findByOrderNo(String orderNo){
		BankLogVoRes res =new BankLogVoRes();
		res.setRows(bankLogDao.findByNo(orderNo));
		return res;
	}
	
}