package com.guohuai.boot.account.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.guohuai.boot.account.entity.PlatformInfoEntity;

public interface PlatformInfoDao extends JpaRepository<PlatformInfoEntity, String>, JpaSpecificationExecutor<PlatformInfoEntity> {

	public PlatformInfoEntity findByPlatformName(String platformName);

	public PlatformInfoEntity findByUserOid(String userOid);

	@Query(value="SELECT * FROM t_account_platform_info ORDER BY createTime DESC LIMIT 1",nativeQuery = true)
	public PlatformInfoEntity findFirst();

	@Transactional
	@Modifying
	@Query(value = "UPDATE t_account_platform_info SET bindCardStatus = ?1 WHERE userOid = ?2", nativeQuery = true)
	public int updateBindCardStatus(String bindCardStatus, String userOid);

	@Transactional
	@Modifying
	@Query(value = "UPDATE t_account_platform_info SET platformStatus = ?1 , updateTime = NOW() ", nativeQuery = true)
	public void disOrEnablePlatform(String platformStatus);
}
