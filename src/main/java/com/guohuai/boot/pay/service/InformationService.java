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
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.pay.dao.InformationDao;
import com.guohuai.boot.pay.form.InformationForm;
import com.guohuai.boot.pay.res.InformationVoRes;
import com.guohuai.boot.pay.vo.InformationVo;

@Service
public class InformationService {
	private final static Logger log = LoggerFactory.getLogger(InformationService.class);
	@Autowired
	private InformationDao informationDao;
	
	public void save(InformationForm req){
		log.info("{},账户添加,{},",JSONObject.toJSONString(req));
		InformationVo vo = new InformationVo();
		BeanUtils.copyProperties(req, vo);
		vo.setCreateTime(new Timestamp(System.currentTimeMillis()));
		vo.setOid(StringUtil.uuid());
		vo.setAccountStatus("3");
		informationDao.save(vo);
	}
	
	public void update(InformationForm req){
		log.info("账户修改,{},账户状态,{}",JSONObject.toJSONString(req),req.getAccountStatus());
		InformationVo old=informationDao.findOne(req.getOid());
		if(old!=null){
			InformationVo vo=new InformationVo();
			BeanUtils.copyProperties(req, vo);
			vo.setCreateTime(old.getCreateTime());
			vo.setOid(old.getOid());
			vo.setUpdateTime(new Timestamp(System.currentTimeMillis()));
			if("0".equals(req.getAccountStatus()) ){
				vo.setAccountStatus("2");
			}
			informationDao.save(vo);
		}
	}	
	
	public void  updateStatus(String status,String oid){
		log.info("{},账户审批,{},","oid:"+oid,"accountStatus:"+status);
		if(oid!=null && status!=null ){
			informationDao.updateStatus(status, oid);
		}
	}
	
	public InformationVoRes page(InformationForm req){
		log.info("{},所有账户查询,{},",req.getUserOid(),JSONObject.toJSONString(req));
		Page<InformationVo> listPage=informationDao.findAll(buildSpecification(req),new PageRequest(req.getPage() - 1, req.getRows()));
		InformationVoRes res =new InformationVoRes();
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
	
	public InformationVoRes approvalPage(InformationForm req){
		log.info("{},待审批账户查询,{},",req.getUserOid(),JSONObject.toJSONString(req));
		req.setAccountStatus("2");
		Page<InformationVo> listPage=informationDao.findAll(buildSpecification(req),new PageRequest(req.getPage() - 1, req.getRows()));
		InformationVoRes res =new InformationVoRes();
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

	
	public Specification<InformationVo> buildSpecification(final InformationForm req) {
		Specification<InformationVo> spec = new Specification<InformationVo>() {
			@Override
			public Predicate toPredicate(Root<InformationVo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> bigList =new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(req.getUserOid()))
					bigList.add(cb.equal(root.get("userOid").as(String.class),req.getUserOid()));
				if (!StringUtil.isEmpty(req.getBankAccountName()))
					bigList.add(cb.like(root.get("bankAccountName").as(String.class),"%"+req.getBankAccountName()+"%"));
				if (!StringUtil.isEmpty(req.getAccountType()))
					bigList.add(cb.equal(root.get("accountType").as(String.class),req.getAccountType()));
				if (!StringUtil.isEmpty(req.getAccountStatus()))
					bigList.add(cb.equal(root.get("accountStatus").as(String.class),req.getAccountStatus()));
				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
				query.orderBy(cb.desc(root.get("createTime")));
				
				// 条件查询
				return query.getRestriction();
			}
		};
		return spec;
	}
}