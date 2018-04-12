package com.guohuai.boot.pay.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.guohuai.boot.pay.vo.BankCallbackLogVo;

public interface BankCallbackLogDao extends JpaRepository<BankCallbackLogVo, String>, JpaSpecificationExecutor<BankCallbackLogVo> {
	
	@Query(value="select * from t_bank_callback_log  where callBackOid = ?1", nativeQuery = true)
	public List<BankCallbackLogVo> findByCallbackOid(String callBackOid);
}
