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
import com.guohuai.boot.pay.dao.ProtocolDao;
import com.guohuai.boot.pay.form.ProtocolForm;
import com.guohuai.boot.pay.res.ProtocolVoRes;
import com.guohuai.boot.pay.vo.ProtocolVo;
import com.guohuai.component.util.DesPlus;

@Service
public class ProtocolService {
	private final static Logger log = LoggerFactory.getLogger(ProtocolService.class);
	@Autowired
	private ProtocolDao protocolDao;
	
	public ProtocolVoRes page(ProtocolForm req){
		log.info("{},交易订单查询,{},",req.getUserOid(),JSONObject.toJSONString(req));
		Page<ProtocolVo> listPage=protocolDao.findAll(buildSpecification(req),new PageRequest(req.getPage() - 1, req.getRows()));
		ProtocolVoRes res =new ProtocolVoRes();
		if (listPage != null && listPage.getSize() > 0) {
			if(listPage.getContent().size()>0){
				for(ProtocolVo vo:listPage.getContent()){
					vo.setCardNo(DesPlus.decrypt(vo.getCardNo()));
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
	
	
	public Specification<ProtocolVo> buildSpecification(final ProtocolForm req) {
		Specification<ProtocolVo> spec = new Specification<ProtocolVo>() {
			@Override
			public Predicate toPredicate(Root<ProtocolVo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> bigList =new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(req.getUserOid()))
					bigList.add(cb.equal(root.get("userOid").as(String.class),req.getUserOid()));
				if (!StringUtil.isEmpty(req.getProtocolNo()))
					bigList.add(cb.like(root.get("protocolNo").as(String.class),"%"+req.getProtocolNo()+"%"));
				if (!StringUtil.isEmpty(req.getAccountBankType()))
					bigList.add(cb.like(root.get("accountBankType").as(String.class), "%"+req.getAccountBankType()+"%"));
//				if (!StringUtil.isEmpty(req.getBeginTime())){
//					java.util.Date beginDate=DateUtil.parseDate(req.getBeginTime(), "yyyy-MM-dd HH:mm:ss");
//					bigList.add(cb.greaterThanOrEqualTo(root.get("createTime").as(Timestamp.class),new Timestamp(beginDate.getTime())));
//				}
//				if (!StringUtil.isEmpty(req.getEndTime())){
//					java.util.Date beginDate=DateUtil.parseDate(req.getBeginTime(), "yyyy-MM-dd HH:mm:ss");
//					bigList.add(cb.lessThanOrEqualTo(root.get("status").as(Timestamp.class),new Timestamp(DateUtil.endTimeInMillis(beginDate).getTime())));
//				}
				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
				query.orderBy(cb.desc(root.get("createTime")));
				
				// 条件查询
				return query.getRestriction();
			}
		};
		return spec;
	}
}