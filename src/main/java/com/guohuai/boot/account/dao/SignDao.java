package com.guohuai.boot.account.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.guohuai.boot.account.entity.SignEntity;

public interface SignDao extends JpaRepository<SignEntity, String>, JpaSpecificationExecutor<SignEntity> {
	
	@Query(value = "SELECT * FROM t_account_sign WHERE userOid = ?1 and status=?2", nativeQuery = true)
	public SignEntity findByUserOidStatus(String userOid,String status);
	
	@Query(value = "SELECT * FROM t_account_sign WHERE userOid = ?1 and bankCard=?2 and status='0' ", nativeQuery = true)
	public SignEntity findByUserOid(String userOid,String card);
}
