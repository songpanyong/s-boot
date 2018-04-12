package com.guohuai.boot.account.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.guohuai.boot.account.entity.PlatformInfoAuditEntity;

public interface PlatformInfoAuditDao extends JpaRepository<PlatformInfoAuditEntity, String>, JpaSpecificationExecutor<PlatformInfoAuditEntity> {

	public PlatformInfoAuditEntity findByUserOid(String UserOid);

	@Query(value = "SELECT * FROM t_account_platform_info_audit a WHERE a.userOid = ?1 AND a.applyType = ?2 AND auditStatus = ?3", nativeQuery = true)
	public PlatformInfoAuditEntity findByUserOidAndStatusAndType(
			String userOid, String applyType, String auditStatus);

	@Query(value = "SELECT a.* FROM t_account_platform_info_audit a LEFT JOIN t_account_platform_change_records b ON a.oid = b.auditOid "
			+ "WHERE b.eventOid = ?1 AND a.auditStatus IN('0','1')ORDER BY createTime DESC LIMIT 1;", nativeQuery = true)
	public PlatformInfoAuditEntity findByEventOid(String eventOid);

	@Query(value = "SELECT * FROM t_account_platform_info_audit a WHERE a.userOid = ?1 AND a.applyType = '1' AND a.auditStatus ='0' limit 1", nativeQuery = true)
	public PlatformInfoAuditEntity findAuditingByUserOid(String userOid);
	
}
