package com.guohuai.boot.pay.service;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.PayXFEnum;
import com.guohuai.boot.pay.dao.BankCallbackDao;
import com.guohuai.boot.pay.form.BankCallbackForm;
import com.guohuai.boot.pay.res.BankCallbackVoRes;
import com.guohuai.boot.pay.vo.BankCallbackVo;
import com.guohuai.boot.pay.vo.PaymentVo;

@Service
public class BankCallbackService {
	private final static Logger log = LoggerFactory.getLogger(BankCallbackService.class);
	@Autowired
	private BankCallbackDao bankCallbackDao;
	
	@Autowired
	private PaymentService paymentService;
	
	public BankCallbackVoRes page(BankCallbackForm req){
		log.info("回调查询{}",JSONObject.toJSONString(req));
		Page<BankCallbackVo> listPage=bankCallbackDao.findAll(buildSpecification(req),new PageRequest(req.getPage() - 1, req.getRows()));
		BankCallbackVoRes res =new BankCallbackVoRes();
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
	
	
	public Specification<BankCallbackVo> buildSpecification(final BankCallbackForm req) {
		Specification<BankCallbackVo> spec = new Specification<BankCallbackVo>() {
			@SuppressWarnings("unlikely-arg-type")
			@Override
			public Predicate toPredicate(Root<BankCallbackVo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> bigList =new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(req.getChannelNo()))
					bigList.add(cb.equal(root.get("channelNo").as(String.class),req.getChannelNo()));
				if (!StringUtil.isEmpty(req.getOrderNO()))
					bigList.add(cb.equal(root.get("orderNO").as(String.class),req.getOrderNO()));
				if (!StringUtil.isEmpty(req.getTradeType()))
					bigList.add(cb.equal(root.get("tradeType").as(String.class),req.getTradeType()));
				if (!StringUtil.isEmpty(req.getStatus()))
					bigList.add(cb.equal(root.get("status").as(String.class),req.getStatus()));
				if (!StringUtil.isEmpty(req.getPayNo()))
					bigList.add(cb.equal(root.get("payNo").as(String.class),req.getPayNo()));
				if (!StringUtil.isEmpty(req.getType()))
					bigList.add(cb.equal(root.get("type").as(String.class),req.getType()));
				if (req.getCount() != null&&!"null".equals(req.getCount()))
					bigList.add(cb.equal(root.get("count").as(String.class),req.getCount()));
				if (!StringUtil.isEmpty(req.getStartTime())) {
					Date beginDate = DateUtil.parseDate(req.getStartTime(), "yyyy-MM-dd HH:mm:ss");
					bigList.add(cb.greaterThanOrEqualTo(root.get("createTime").as(Timestamp.class),
							new Timestamp(beginDate.getTime())));
				}
				if (!StringUtil.isEmpty(req.getEndTime())) {
					Date endDate = DateUtil.parseDate(req.getEndTime(), "yyyy-MM-dd HH:mm:ss");
					bigList.add(cb.lessThanOrEqualTo(root.get("createTime").as(Timestamp.class),
							new Timestamp(DateUtil.endTimeInMillis(endDate).getTime())));
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
	 * 增加回调次数
	 * @param oid
	 * @param handCallBackCount
	 */
	public void addCallBackCount(String oid, int handCallBackCount) {
		if(oid != null){
			bankCallbackDao.addCallBackCount(oid, handCallBackCount);
		}
	}
	
	/**
	 * 网关支付回调
	 * @param needCallBackList
	 */
	public void gatewayCallBackByServerItself(List<PaymentVo> needCallBackList){
		if(null != needCallBackList && needCallBackList.size() > 0){
			long start = System.nanoTime();
			log.info("Gateway call back by server itself start, totle number="+needCallBackList.size());
			for(PaymentVo paymentVo: needCallBackList){
				Map<String, String> map = new HashMap<String, String>();// 保存参与验签字段
				map.put("tradeNo", paymentVo.getPayNo());//金运通返回平台支付订单号，没返回，设置为订单号
				map.put("merchantNo", paymentVo.getPayNo());
				map.put("resMessage", PayXFEnum.XF11000.getName());//订单支付中断
				map.put("status", PayXFEnum.XFF.getCode());// 支付失败
				map.put("resCode", PayXFEnum.XF11000.getCode());
				log.info("通知订单信息", JSONObject.toJSONString(map));
				paymentService.noticUrl(map);
			}
			long end = System.nanoTime();
			log.info("Gateway call back by server itself end, The elapsed time="+(end - start));
		}else{
			log.info("Gateway call back by server itself end with do nothing");
		}
	}
	
}