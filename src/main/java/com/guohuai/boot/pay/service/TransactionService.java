package com.guohuai.boot.pay.service;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.pay.dao.ComOrderDao;
import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.settlement.api.request.TransactionRequest;
import com.guohuai.settlement.api.response.TransactionResponse;

@Service
public class TransactionService {
	private final static Logger log = LoggerFactory.getLogger(TransactionService.class);
	@Autowired
	private ComOrderDao orderDao;
	
	public TransactionResponse queryOrder(TransactionRequest req){
		log.info("{},交易订单查询,{},",req.getUserOid(),JSONObject.toJSONString(req));
		Page<OrderVo> listPage=orderDao.findAll(buildSpecification(req),new PageRequest(req.getPage() - 1, req.getRow()));
		TransactionResponse res =new TransactionResponse();
		if (listPage != null && listPage.getSize() > 0) {
			List<TransactionResponse> rows=new ArrayList<TransactionResponse>();
			if(listPage.getContent().size()>0){
				TransactionResponse transactionResponse=null;
				for(OrderVo vo:listPage.getContent()){
					transactionResponse=new TransactionResponse();
					BeanUtils.copyProperties(vo, transactionResponse);
					transactionResponse.setPayTime(DateUtil.formatDatetime(vo.getCreateTime().getTime()));
					transactionResponse.setResult(vo.getStatus());
					rows.add(transactionResponse);
				}
			}
			res.setRows(rows);
			res.setTotalPage(listPage.getTotalPages());
			res.setPage(req.getPage());
			res.setRow(req.getRow());
			res.setTotal(listPage.getTotalElements());
			return res;
		}
		return null;
	}
	
	
	public Specification<OrderVo> buildSpecification(final TransactionRequest req) {
		Specification<OrderVo> spec = new Specification<OrderVo>() {
			@Override
			public Predicate toPredicate(Root<OrderVo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> bigList =new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(req.getUserOid()))
					bigList.add(cb.equal(root.get("userOid").as(String.class),req.getUserOid()));
				if (!StringUtil.isEmpty(req.getType()))
					bigList.add(cb.equal(root.get("type").as(String.class),req.getUserOid()));
				if (!StringUtil.isEmpty(req.getStatus()))
					bigList.add(cb.equal(root.get("status").as(String.class), req.getStatus()));
				if (!StringUtil.isEmpty(req.getBeginTime())){
					java.util.Date beginDate=DateUtil.parseDate(req.getBeginTime(), "yyyy-MM-dd HH:mm:ss");
					bigList.add(cb.greaterThanOrEqualTo(root.get("createTime").as(Timestamp.class),new Timestamp(beginDate.getTime())));
				}
				if (!StringUtil.isEmpty(req.getEndTime())){
					java.util.Date beginDate=DateUtil.parseDate(req.getBeginTime(), "yyyy-MM-dd HH:mm:ss");
					bigList.add(cb.lessThanOrEqualTo(root.get("status").as(Timestamp.class),new Timestamp(DateUtil.endTimeInMillis(beginDate).getTime())));
				}
				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
				query.orderBy(cb.desc(root.get("createTime")));
				
				// 条件查询
				return query.getRestriction();
			}
		};
		return spec;
	}
}