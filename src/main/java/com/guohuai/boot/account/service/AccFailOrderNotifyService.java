package com.guohuai.boot.account.service;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.guohuai.boot.account.dao.AccFailOrderNotifyDao;
import com.guohuai.boot.account.entity.AccFailOrderNotifyEntity;


@Service
public class AccFailOrderNotifyService {
	private final static Logger log = LoggerFactory.getLogger(AccFailOrderNotifyService.class);
	
	@Autowired
	private AccFailOrderNotifyDao accFailOrderNotifyDao;
	
	public List<AccFailOrderNotifyEntity> getNeedSendMsgList(int sendTimeInterval){
		log.info("查询需要发送短信通知的异常订单，时间间隔"+sendTimeInterval+"小时");
		//需要发送短信通知List
		List<AccFailOrderNotifyEntity> needSendMsgList = new ArrayList<AccFailOrderNotifyEntity>();
		//查询需要发送短信的异常
		List<String> failOrderDescList = accFailOrderNotifyDao.getOrderDescNotifyList(sendTimeInterval);
		for(String orderDesc : failOrderDescList){
			//查询最新通知的订单
			AccFailOrderNotifyEntity lastNotifyEntity = accFailOrderNotifyDao.getLastReceiveTime(orderDesc,"Y");
			//查询最新异常订单
			AccFailOrderNotifyEntity newNotifyEntity = accFailOrderNotifyDao.getLastReceiveTime(orderDesc,"N");
			if(lastNotifyEntity != null){
				//判断最新通知订单和最新异常订单时间差
				if(judgeTimeDifference(lastNotifyEntity.getReceiveTime(),newNotifyEntity.getReceiveTime(),sendTimeInterval)){
					//差额超过需通知时间
					needSendMsgList.add(newNotifyEntity);
				}
			}else{//未发送过短信,需要发送短信
				needSendMsgList.add(newNotifyEntity);
			}
		}
		return needSendMsgList;
	}
	
	/**
	 * 判断最新通知订单和最新异常订单时间差
	 * @param sendTimeInterval 
	 * @param receiveTime
	 * @param receiveTime2
	 * @return
	 */
	private static boolean judgeTimeDifference(Timestamp lastReceiveTime,
			Timestamp newReceiveTime, int sendTimeInterval) {
		//Timestamp转Date
		Date lastReceiveDate = new Date(lastReceiveTime.getTime());
		//Date转Calendar
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(lastReceiveDate);
		//Calendar加上时间
		calendar.add(Calendar.HOUR,+sendTimeInterval);
		//Calendar转Timestamp
	    Timestamp time = new Timestamp(calendar.getTimeInMillis());
	    //判断是否过期，true就是超过
	    if (newReceiveTime.after(time)){
	    	return true;
	    };
		return false;
	}

	/**
	 * 发送状态修改为已发送
	 * @param needSendMsgList
	 */
	public void update(List<AccFailOrderNotifyEntity> needSendMsgList) {
		if(needSendMsgList != null){
			accFailOrderNotifyDao.save(needSendMsgList);
		}
		
	}
	
}