package com.guohuai.boot.account.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.guohuai.boot.account.entity.AccountEventChangeRecordsEntity;

public interface AccountEventChangeRecordsDao extends JpaRepository<AccountEventChangeRecordsEntity, String>, JpaSpecificationExecutor<AccountEventChangeRecordsEntity> {

	@Query(value = "SELECT * FROM t_account_event_change_records WHERE auditOid = ?1", nativeQuery = true)
	public List<AccountEventChangeRecordsEntity> findByAuditOid(String auditOid);

	@Query(value = "SELECT * FROM t_account_event_change_records WHERE effevtiveStatus = '0' AND effectiveTime <= NOW()", nativeQuery = true)
	public List<AccountEventChangeRecordsEntity> findNeedChangeList();
	
}
