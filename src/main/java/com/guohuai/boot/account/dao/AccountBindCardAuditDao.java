package com.guohuai.boot.account.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.guohuai.boot.account.entity.AccountBindCardAuditEntity;

/**
 * @author chendonghui
 * @version 创建时间 ：2017年12月15日下午3:42:12
 *
 */

public interface AccountBindCardAuditDao extends JpaRepository<AccountBindCardAuditEntity, String>, JpaSpecificationExecutor<AccountBindCardAuditEntity> {

	@Query(value = "SELECT * FROM t_account_bind_card_audit WHERE userOid = ?1 and auditStatus = '1' order by createTime desc LIMIT 1", nativeQuery = true)
	public AccountBindCardAuditEntity findByUserOid(String userOid);
	
	@Query(value = "SELECT * FROM t_account_bind_card_audit WHERE userOid = ?1 and auditStatus = '0'", nativeQuery = true)
	public AccountBindCardAuditEntity findAuditingByUserOid(String userOid);

	@Query(value = "SELECT * FROM t_account_bind_card_audit WHERE userOid = ?1 orderBy createTime desc limit 1", nativeQuery = true)
	public AccountBindCardAuditEntity findNewByUserOid(String userOid);

}
