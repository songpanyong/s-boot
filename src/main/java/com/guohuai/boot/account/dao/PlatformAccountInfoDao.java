package com.guohuai.boot.account.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.guohuai.boot.account.entity.PlatformAccountInfoEntity;

public interface PlatformAccountInfoDao extends JpaRepository<PlatformAccountInfoEntity, String>, JpaSpecificationExecutor<PlatformAccountInfoEntity> {

	@Query(value = "SELECT a.accountName FROM t_account_platform_account_info a WHERE a.userOid = ?1 and a.accountStatus = ?2", nativeQuery = true)
	public List<String> findByUserOidAndStatus(String userOid,String status);
	
	@Query(value = "SELECT a.accountName FROM t_account_platform_account_info a WHERE a.accountType = ?1 and a.accountStatus = '2'", nativeQuery = true)
	public List<PlatformAccountInfoEntity> findByAccountType(String accountType);

	@Query(value = "SELECT * FROM t_account_platform_account_info a WHERE a.userOid = ?1 and a.accountType = ?2 and a.accountStatus = ?3", nativeQuery = true)
	public List<PlatformAccountInfoEntity> findByUserOidAndTypeAndStatus(
			String userOid, String accountType, String accountStatus);
	
	@Query(value = "SELECT * FROM t_account_platform_account_info a WHERE a.accountNo = ?1", nativeQuery = true)
	public PlatformAccountInfoEntity findByAccountNo(String accountNo);

	@Transactional
	@Modifying
	@Query(value = "UPDATE t_account_platform_account_info SET accountStatus = ?1 , updateTime = NOW() WHERE userOid = ?2 and userType = ?3", nativeQuery = true)
	public int disOrEnablePlatformAccount(String accountStatus, String userOid, String userType);

	@Query(value = "SELECT * FROM t_account_platform_account_info a WHERE a.userOid = ?1 and a.accountType = ?2", nativeQuery = true)
	public List<PlatformAccountInfoEntity> findByUserOidAndType(String userOid,
			String accountType);

}
