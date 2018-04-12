package com.guohuai.boot.pay.service;


import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.pay.dao.ReconciliationRecordsDao;
import com.guohuai.boot.pay.form.ReconciliationRecordsForm;
import com.guohuai.boot.pay.vo.ReconciliationRecordsVo;

@Service
public class ReconciliationRecordsService {
	private final static Logger log = LoggerFactory.getLogger(ReconciliationRecordsService.class);
	@Autowired
	private ReconciliationRecordsDao reconciliationRecordsDao;
	
	public void save(ReconciliationRecordsForm req){
		log.info("{},对账记录添加,{},",JSONObject.toJSONString(req));
		ReconciliationRecordsVo vo=new ReconciliationRecordsVo();
		BeanUtils.copyProperties(req, vo);
		vo.setCreateTime(new Timestamp(System.currentTimeMillis()));
		vo.setOid(StringUtil.uuid());
		reconciliationRecordsDao.save(vo);
	}

	
	public void changeStatus(String reconDate, String reconStatus, String channelId){
		log.info("{},对账记录修改,{},",reconDate,reconStatus);
		reconciliationRecordsDao.changeStatus(reconDate, reconStatus, channelId);
	}
	
	public ReconciliationRecordsVo findByDate(String reconDate,String channelId){
		log.info("{},根据日期对账记录查询,{},",reconDate,channelId);
		return reconciliationRecordsDao.findByDate(reconDate,channelId);
	}
	
}