package com.guohuai.boot.account.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.guohuai.boot.account.entity.AccountEventChildEntity;

public interface AccountEventChildDao extends JpaRepository<AccountEventChildEntity, String>, JpaSpecificationExecutor<AccountEventChildEntity> {
	
	@Query(value = "select * from t_account_event_child t where t.eventOid = ?1", nativeQuery = true)
	public List<AccountEventChildEntity> findByEventOid(String eventOid);

	public AccountEventChildEntity findByChildEventType(String childEventType);

	@Query(value = "SELECT childEventType FROM t_account_event_child WHERE outputAccountNo = ?1 OR inputAccountNo = ?1 ", nativeQuery = true)
	public List<String> findEventNameByReservedAccountNo(String accountNo);

}