package com.guohuai.boot.account.service.accfailordernotify;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.guohuai.boot.account.dao.AccFailOrderNotifyDao;
import com.guohuai.boot.account.entity.AccFailOrderNotifyEntity;
import com.guohuai.component.util.DateUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: TODO
 * @author ZJ
 * @date 2018年1月19日 下午6:14:06
 * @version V1.0
 */
@Slf4j
@Component
public class AccFailOrderNotifyQueryService {
	@Autowired
	private AccFailOrderNotifyDao accFailOrderNotifyDao;

	public List<AccFailOrderNotifyEntity> getNeedSendMsgList(int sendTimeInterval) {
		log.info("查询需要发送短信通知的异常订单，时间间隔" + sendTimeInterval + "小时");
		// 需要发送短信通知List
		List<AccFailOrderNotifyEntity> needSendMsgList = new ArrayList<AccFailOrderNotifyEntity>();
		// 查询需要发送短信的异常
		List<String> failOrderDescList = accFailOrderNotifyDao.getOrderDescNotifyList(sendTimeInterval);
		for (String orderDesc : failOrderDescList) {
			// 查询最新通知的订单
			AccFailOrderNotifyEntity lastNotifyEntity = accFailOrderNotifyDao.getLastReceiveTime(orderDesc, "Y");
			// 查询最新异常订单
			AccFailOrderNotifyEntity newNotifyEntity = accFailOrderNotifyDao.getLastReceiveTime(orderDesc, "N");
			if (lastNotifyEntity != null) {
				// 判断最新通知订单和最新异常订单时间差
				if (DateUtil.judgeTimeDifference(lastNotifyEntity.getReceiveTime(), newNotifyEntity.getReceiveTime(),
						sendTimeInterval)) {
					// 差额超过需通知时间
					needSendMsgList.add(newNotifyEntity);
				}
			} else {// 未发送过短信,需要发送短信
				needSendMsgList.add(newNotifyEntity);
			}
		}
		return needSendMsgList;
	}
}