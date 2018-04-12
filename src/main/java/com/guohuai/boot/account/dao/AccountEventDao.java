package com.guohuai.boot.account.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.guohuai.boot.account.entity.AccountEventEntity;

public interface AccountEventDao extends JpaRepository<AccountEventEntity, String>, JpaSpecificationExecutor<AccountEventEntity> {

	public AccountEventEntity findByEventName(String eventName);

	public AccountEventEntity findByUserOid(String userOid);
	
	@Transactional
	@Modifying
	@Query(value = "UPDATE t_account_event SET setUpStatus = ?2 , updateTime = NOW() WHERE oid = ?1", nativeQuery = true)
	public int changeEventStatusByOid(String eventOid, String setUpStatus);

	@Query(value = "SELECT * FROM t_account_event a WHERE a.eventName = ?1 AND userOid = ?2", nativeQuery = true)
	public AccountEventEntity findByEventNameAndUserOid(String eventName, String userOid);

	@Query(value = "select * from t_account_event t where t.userOid = ?1 and t.transType = ?2 and t.eventType = ?3", nativeQuery = true)
	public AccountEventEntity findByUserOidAndTransTypeAndEventType(String userOid,String transType,String eventType);
}