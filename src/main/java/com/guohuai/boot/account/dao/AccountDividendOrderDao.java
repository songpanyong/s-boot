package com.guohuai.boot.account.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.guohuai.boot.account.entity.AccountDividendOrderEntity;


public interface AccountDividendOrderDao extends JpaRepository<AccountDividendOrderEntity, String>, JpaSpecificationExecutor<AccountDividendOrderEntity> {
	@Query()
	public AccountDividendOrderEntity findByOrderNo(String orderNo);
	
	@Query(value = "SELECT * FROM t_account_dividend_order WHERE dividendStatus = ?1", nativeQuery = true)
	public List<AccountDividendOrderEntity> findByDividendStatus(String dividendStatus);	
}
