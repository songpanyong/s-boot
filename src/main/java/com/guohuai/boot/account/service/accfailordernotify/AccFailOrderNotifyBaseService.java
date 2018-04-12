package com.guohuai.boot.account.service.accfailordernotify;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.guohuai.boot.account.dao.AccFailOrderNotifyDao;
import com.guohuai.boot.account.entity.AccFailOrderNotifyEntity;

/**
 * @Description: TODO
 * @author ZJ
 * @date 2018年1月19日 下午6:14:17
 * @version V1.0
 */
@Component
public class AccFailOrderNotifyBaseService {
	@Autowired
	private AccFailOrderNotifyDao accFailOrderNotifyDao;

	/**
	 * 发送状态修改为已发送
	 * 
	 * @param needSendMsgList
	 */
	public void update(List<AccFailOrderNotifyEntity> needSendMsgList) {
		if (needSendMsgList != null) {
			accFailOrderNotifyDao.save(needSendMsgList);
		}

	}
}