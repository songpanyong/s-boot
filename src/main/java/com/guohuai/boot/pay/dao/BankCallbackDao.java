package com.guohuai.boot.pay.dao;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.guohuai.boot.pay.vo.BankCallbackVo;

public interface BankCallbackDao extends JpaRepository<BankCallbackVo, String>, JpaSpecificationExecutor<BankCallbackVo> {
	
	@Query(value="update t_bank_callback set totalCount = ?2 , status = '0' , updateTime=NOW() where oid=?1", nativeQuery = true)
	@Modifying
	@Transactional
	public int addCallBackCount(String oid, int totalCount);
}
