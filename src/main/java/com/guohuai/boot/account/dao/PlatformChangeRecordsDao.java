package com.guohuai.boot.account.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.guohuai.boot.account.entity.PlatformChangeRecordsEntity;

public interface PlatformChangeRecordsDao extends JpaRepository<PlatformChangeRecordsEntity, String>, JpaSpecificationExecutor<PlatformChangeRecordsEntity> {

	@Query(value = "SELECT * FROM t_account_platform_change_records WHERE auditOid = ?1", nativeQuery = true)
	public List<PlatformChangeRecordsEntity> findByAuditOid(String auditOid);

	@Query(value = "SELECT count(*) FROM t_account_platform_change_records s LEFT JOIN t_account_platform_info_audit t ON s.auditOid = t.oid WHERE s.accountNo = ?1 and s.changeType = ?2 and t.auditStatus = '0'", nativeQuery = true)
	public int findReadyAuditRecords(String accountNo, String changeType);

	@Query(value = "SELECT * FROM t_account_platform_change_records WHERE eventOid = ?1 ORDER BY updateTime desc limit 1 ", nativeQuery = true)
	public PlatformChangeRecordsEntity findByEventOid(String eventOid);
	
}
