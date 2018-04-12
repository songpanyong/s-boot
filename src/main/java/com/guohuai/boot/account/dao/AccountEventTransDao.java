package com.guohuai.boot.account.dao;

import com.guohuai.boot.account.entity.AccountEventTransEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface AccountEventTransDao extends JpaRepository<AccountEventTransEntity, String>, JpaSpecificationExecutor<AccountEventTransEntity> {
	
	public AccountEventTransEntity findByTransNo(String transNo);
	
	@Query(value = "SELECT * FROM t_account_event_trans WHERE orderNo = ?1 and childEventType =?2 ", nativeQuery = true)
	public AccountEventTransEntity findByOrderNoAndChildEventType(String orderNo,String childEventType);
	
	@Query(value = "SELECT * FROM t_account_event_trans WHERE orderNo = ?1 AND requestNo = ?2 AND `status` = '0' ", nativeQuery = true)
	public List<AccountEventTransEntity> findPendingByOrderNoAndRequestNo(String orderNo, String requestNo);

	@Transactional
	@Modifying
	@Query(value = "UPDATE t_account_event_trans SET status = ?2 , updateTime = NOW() WHERE orderNo = ?1", nativeQuery = true)
	public void updateEventTransStatus(String orderNo, String orderStatus);
	
	public List<AccountEventTransEntity> findByOrderNo(String orderNo);

	@Query(value = "SELECT SUM(balance) FROM t_account_event_trans WHERE outputUserType = 'T3' OR inputUserType = 'T3' AND `status` = '1' "
			+ " AND childEventType NOT IN('recharge','withdraw','withdrawFrozen','withdrawUnfrozen'); ", nativeQuery = true)
	public BigDecimal findPlatformTransferBalance();

	@Query(value = "SELECT SUM(balance) FROM t_account_event_trans WHERE outputAccountNo = ?1 AND childEventType = ?2 AND `status` = '1' ", nativeQuery = true)
	public BigDecimal findOutBalanceByAccountNoAndEvent(String accountNo, String code);

	@Query(value = "SELECT SUM(balance) FROM t_account_event_trans WHERE outputAccountNo = ?1 OR inputAccountNo = ?1 AND `status` = '1' ", nativeQuery = true)
	public BigDecimal findTransferBalanceByAccountNo(String accountNo);

	@Query(value = "SELECT IFNULL((SELECT SUM(balance) FROM t_account_event_trans WHERE outputAccountNo = ?1 AND childEventType = 'useVoucherT0' AND `status` = '1'),0)+IFNULL((SELECT SUM(balance) FROM t_account_event_trans WHERE outputAccountNo = ?1 AND childEventType = 'useVoucherT1' AND `status` = '1'),0)-IFNULL((SELECT SUM(balance) FROM t_account_event_trans WHERE inputAccountNo = ?1 AND childEventType = 'reFundUseVoucherT0' AND `status` = '1'),0)-IFNULL((SELECT SUM(balance) FROM t_account_event_trans WHERE inputAccountNo = ?1 AND childEventType = 'reFundUseVoucherT1' AND `status` = '1'),0) ", nativeQuery = true)
	public BigDecimal findCouponOutBalance(String accountNo);
	
	@Query(value = "SELECT IFNULL((SELECT SUM(balance) FROM t_account_event_trans WHERE outputAccountNo = ?1 AND childEventType = 'grantRateCouponProfitContinued' AND `status` = '1'),0)+IFNULL((SELECT SUM(balance) FROM t_account_event_trans WHERE outputAccountNo = ?1 AND childEventType = 'grantRateCouponProfit' AND `status` = '1'),0) ", nativeQuery = true)
	public BigDecimal findrateCouponAmount(String accountNo);

}
