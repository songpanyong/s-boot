package com.guohuai.boot.pay.service;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections.CollectionUtils;
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
import com.guohuai.boot.pay.dao.ComChannelDao;
import com.guohuai.boot.pay.form.ChannelForm;
import com.guohuai.boot.pay.res.ChannelVoRes;
import com.guohuai.boot.pay.vo.ChannelVo;
import com.guohuai.component.util.Constant;
import com.guohuai.settlement.api.request.BankChannelRequest;
import com.guohuai.settlement.api.response.BankChannelResponse;
import com.guohuai.settlement.api.response.BankChannelResponse.BankChannel;

@Service
public class ComChannelService {
	private final static Logger log = LoggerFactory.getLogger(ComChannelService.class);
	@Autowired
	private ComChannelDao comChannelDao;
	
	public void save(ChannelForm req){
		log.info("{},渠道添加,{},",JSONObject.toJSONString(req));
		ChannelVo vo=new ChannelVo();
		BeanUtils.copyProperties(req, vo);
		vo.setCreateTime(new Timestamp(System.currentTimeMillis()));
		vo.setOid(StringUtil.uuid());
		comChannelDao.save(vo);
	}

	
	public void update(ChannelForm req){
		log.info("{},渠道修改,{},",JSONObject.toJSONString(req));
		ChannelVo old=comChannelDao.findOne(req.getOid());
		if(old!=null){
			ChannelVo vo=new ChannelVo();
			BeanUtils.copyProperties(req, vo);
			vo.setCreateTime(old.getCreateTime());
			vo.setOid(old.getOid());
			vo.setUpdateTime(new Timestamp(System.currentTimeMillis()));
			comChannelDao.save(vo);
		}
		
	}
	
	public ChannelVoRes page(ChannelForm req){
		log.info("{},渠道查询,{},",req.getUserOid(),JSONObject.toJSONString(req));
		Page<ChannelVo> listPage=comChannelDao.findAll(buildSpecification(req),new PageRequest(req.getPage() - 1, req.getRows()));
		ChannelVoRes res =new ChannelVoRes();
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

	
	public Specification<ChannelVo> buildSpecification(final ChannelForm req) {
		Specification<ChannelVo> spec = new Specification<ChannelVo>() {
			@Override
			public Predicate toPredicate(Root<ChannelVo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> bigList =new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(req.getUserOid()))
					bigList.add(cb.equal(root.get("userOid").as(String.class),req.getUserOid()));
				if (!StringUtil.isEmpty(req.getSourceType()))
					bigList.add(cb.equal(root.get("sourceType").as(String.class),req.getSourceType()));
				if (!StringUtil.isEmpty(req.getTradeType()))
					bigList.add(cb.equal(root.get("tradeType").as(String.class),req.getTradeType()));
				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
				query.orderBy(cb.desc(root.get("status")),cb.asc(root.get("channelNo")));
				
				// 条件查询
				return query.getRestriction();
			}
		};
		return spec;
	}

	public void changeStatus(String oid, String status) {
		log.info("{},渠道状态修改,{},",oid,"status:"+status);
		ChannelVo old=comChannelDao.findOne(oid);
		if(old!=null){
			comChannelDao.changeStatus(oid, status);
		}
	}
	
	/**
	 * 支付通道查询
	 * @param req
	 * @return
	 */
	public BankChannelResponse findBankChannelList(BankChannelRequest req){
		log.info("支付通道查询,{}",  JSONObject.toJSONString(req));
		BankChannelResponse res = new BankChannelResponse();
		List<BankChannelResponse.BankChannel> bankChannelVos=new ArrayList<>(); 
		BeanUtils.copyProperties(req, res);
		List<ChannelVo> listPage;
		try {
			listPage = comChannelDao.findAll(queryConditionStitching(req));
		} catch (Exception e) {
			res.setReturnCode(Constant.FAIL);
			res.setErrorMessage("查询支付通道异常");
			log.error("查询支付通道异常", e);
			return res;
		}
		
		if(CollectionUtils.isNotEmpty(listPage)) {
			for (ChannelVo channelVo : listPage) {
				BankChannel bankChannel = new BankChannelResponse.BankChannel();
				bankChannel.setChannelName(channelVo.getChannelName());
				bankChannel.setChannelNo(channelVo.getChannelNo());
				bankChannel.setMaxAmount(channelVo.getMaxAmount());
				bankChannel.setMinAmount(channelVo.getMinAmount());
				bankChannel.setRate(channelVo.getRate());
				bankChannel.setTradeType(channelVo.getTradeType());
				bankChannelVos.add(bankChannel);
			}
			res.setReturnCode(Constant.SUCCESS);
			res.setErrorMessage("查询成功");
			res.setBankChannes(bankChannelVos);
			log.error("查询支付通道返回页面端数据：{}", JSONObject.toJSONString(res));
			return res; 
		}else {
			res.setReturnCode(Constant.FAIL);
			res.setErrorMessage("没有任何支付渠道信息");
			log.error("查询支付通道异常返回页面端数据：{}", JSONObject.toJSONString(res));
			return res;
		}
	}
	
	public Specification<ChannelVo> queryConditionStitching(final BankChannelRequest req) {
		Specification<ChannelVo> spec = new Specification<ChannelVo>() {
			@Override
			public Predicate toPredicate(Root<ChannelVo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> bigList =new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(req.getSourceType())) {
					bigList.add(cb.equal(root.get("sourceType").as(String.class),req.getSourceType()));
				}
				bigList.add(cb.equal(root.get("status").as(String.class),"1"));
				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
				query.orderBy(cb.desc(root.get("status")),cb.asc(root.get("channelNo")));
				// 条件查询
				return query.getRestriction();
			}
		};
		return spec;
	}
}