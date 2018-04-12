package com.guohuai.boot.pay.service;


import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import com.guohuai.boot.pay.dao.BankHistoryDao;
import com.guohuai.boot.pay.form.BankHistoryForm;
import com.guohuai.boot.pay.res.BankHistoryVoRes;
import com.guohuai.boot.pay.vo.BankHistoryVo;

@Service
public class BankHistoryService {
	private final static Logger log = LoggerFactory.getLogger(BankHistoryService.class);
	@Autowired
	private BankHistoryDao bankHistoryDao;
	
	public BankHistoryVoRes page(BankHistoryForm req){
		log.info("{},账户查询,{},",req.getUserOid(),JSONObject.toJSONString(req));
		Page<BankHistoryVo> listPage=bankHistoryDao.findAll(buildSpecification(req),new PageRequest(req.getPage() - 1, req.getRows()));
		BankHistoryVoRes res =new BankHistoryVoRes();
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
	
	public void ignore(String[] oids){
		log.info("忽略对账,{}",JSONObject.toJSONString(oids));
		try{
			if(oids.length>0){
				for(int i=0;i<oids.length;i++){
					bankHistoryDao.updateStatus(oids[i]);
				}
			}
		}catch(Exception e){
			log.error("忽略对账异常",e);
		}
	}
	
	public Specification<BankHistoryVo> buildSpecification(final BankHistoryForm req) {
		Specification<BankHistoryVo> spec = new Specification<BankHistoryVo>() {
			@Override
			public Predicate toPredicate(Root<BankHistoryVo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> bigList =new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(req.getUserOid()))
					bigList.add(cb.equal(root.get("userOid").as(String.class),req.getUserOid()));
				if (!StringUtil.isEmpty(req.getBankType()))
					bigList.add(cb.equal(root.get("bankType").as(String.class),req.getBankType()));
				if (!StringUtil.isEmpty(String.valueOf(req.getReconStatus())) && req.getReconStatus()!=4)
					bigList.add(cb.equal(root.get("reconStatus").as(Integer.class),req.getReconStatus()));
				if (!StringUtil.isEmpty(req.getStract()))
					bigList.add(cb.equal(root.get("stract").as(String.class),req.getStract()));
				if (req.getTradTime()!=null && !req.getTradTime().equals("")){
					Timestamp startDateTime = null;
					Timestamp endDateTime = null;
					try {
						startDateTime = new Timestamp(new SimpleDateFormat("yyyy-MM-dd").parse(req.getTradTime()).getTime());
					} catch (ParseException e) {
						log.error("日期转换异常",e);
					}
					Date startDate = null;
					try {
						startDate = new SimpleDateFormat("yyyy-MM-dd").parse(req.getTradTime());
					} catch (ParseException e) {
						log.error("日期转换异常",e);
					}
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(startDate);
					calendar.add(Calendar.DATE,1);
					endDateTime = new Timestamp(calendar.getTime().getTime());
					bigList.add(cb.between(root.get("tradTime").as(Timestamp.class),startDateTime ,endDateTime));
					log.info("查询开始时间:,{},查询结束时间:,{}",startDateTime,endDateTime);
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