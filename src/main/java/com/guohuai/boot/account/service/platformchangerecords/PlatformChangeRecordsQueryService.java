package com.guohuai.boot.account.service.platformchangerecords;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.guohuai.boot.account.dao.PlatformChangeRecordsDao;
import com.guohuai.boot.account.entity.PlatformChangeRecordsEntity;

import lombok.extern.slf4j.Slf4j;

/**   
 * @Description: TODO 
 * @author ZJ   
 * @date 2018年1月19日 下午6:19:44 
 * @version V1.0   
 */
@Slf4j
@Service
public class PlatformChangeRecordsQueryService {
	@Autowired
	private PlatformChangeRecordsDao platformChangeRecordsDao;

	/**
	 * 根据审核oid获取审核对应的修改记录
	 * @param auditOid 审核oid
	 * @return 修改记录
	 */
	public List<PlatformChangeRecordsEntity> findByAuditOid(String auditOid) {
		log.info("根据审核oid获取审核对应的修改记录，请求参数{}", auditOid);
		List<PlatformChangeRecordsEntity> changeRecordsList = platformChangeRecordsDao.findByAuditOid(auditOid);
		log.info("根据审核oid获取审核对应的修改记录，返回结果{}", changeRecordsList);
		return changeRecordsList;
	}

	/**
	 * 根据登账事件oid获取修改记录
	 * @param eventOid 登账事件oid
	 * @return 修改记录
	 */
	public PlatformChangeRecordsEntity findByEventOid(String eventOid) {
		log.info("根据登账事件oid获取修改记录，请求参数：{}", eventOid);
		PlatformChangeRecordsEntity entity = platformChangeRecordsDao.findByEventOid(eventOid);
		log.info("根据登账事件oid获取修改记录，返回结果：{}", entity);
		return entity;
	}
}