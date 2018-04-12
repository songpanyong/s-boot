package com.guohuai.boot.account.service.platforminfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.guohuai.boot.account.dao.PlatformInfoDao;
import com.guohuai.boot.account.entity.PlatformInfoEntity;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: TODO
 * @author ZJ
 * @date 2018年1月19日 下午6:24:24
 * @version V1.0
 */
@Slf4j
@Service
public class PlatformInfoBaseService {
	@Autowired
	private PlatformInfoDao platformInfoDao;

	/**
	 * 修改平台绑卡状态
	 * @param userOid 平台用户id
	 * @param bindCardStatus 绑卡状态
	 */
	public void changeBindStatus(String userOid, String bindCardStatus) {
		log.info("修改平台绑卡状态,请求参数：用户userOid{}，绑卡状态{}", userOid, bindCardStatus);
		PlatformInfoEntity entity = platformInfoDao.findByUserOid(userOid);
		entity.setBindCardStatus(bindCardStatus);
		log.info("修改平台绑卡状态,返回结果：{}", entity);
		platformInfoDao.saveAndFlush(entity);
	}
}