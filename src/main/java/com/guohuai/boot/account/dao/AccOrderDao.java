package com.guohuai.boot.account.dao;

import com.guohuai.boot.account.entity.AccOrderEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

public interface AccOrderDao extends JpaRepository<AccOrderEntity, String>, JpaSpecificationExecutor<AccOrderEntity> {
	
	public AccOrderEntity findByOrderNo(String orderNo);
	
	@Query(value="select r.userOid,r.orderNo,r.orderStatus,r.orderType,r.balance-fee,r.fee,r.voucher,r.submitTime,r.userType,o.phone,o.`name` "
			+ "from t_account_order r LEFT JOIN t_account_userinfo o ON o.userOid = r.userOid where "
			+ " r.submitTime>=?1 and r.submitTime<=?2  order by r.createTime  limit ?3,?4 ",nativeQuery = true)
	public Object[] getAccountOrderList(Timestamp beginTime,Timestamp endTime,long pageSize,int size);

	@Query(value = "SELECT * FROM t_account_order WHERE orderNo = ?1 and orderType IN('01','02','50','51','54','55','56')", nativeQuery = true)
	public AccOrderEntity findOrderByOrderNoInOrderType(String orderCode);

	@Query
	public AccOrderEntity findOrderByOrderNo(String orderNo);

	@Transactional
	@Modifying
	@Query(value = "UPDATE t_account_order SET orderStatus=?2 , updateTime=NOW() WHERE orderNo=?1", nativeQuery = true)
	void updateOrderStatus(String orderNo, String orderStatus);
	
	@Query(value = "SELECT SUM(balance) FROM t_account_order WHERE orderType =?1 AND orderStatus = '0' AND submitTime < date_sub(?2,interval -1 day) AND submitTime > ?2", nativeQuery = true)
	public BigDecimal getOrderBalanceByDate(String orderType, Timestamp nettingTime);
	

	@Query(value = "SELECT * FROM t_account_order WHERE orderType ='01' AND orderStatus = '0' AND submitTime < date_sub(?2,interval -1 day) AND submitTime > ?2", nativeQuery = true)
	public List<AccOrderEntity> getRedeemListByTimeAndUserOid(
			String userOid, Timestamp nettingTime);

	@Query(value = "SELECT COUNT(*) FROM t_account_order WHERE requestNo = ?1", nativeQuery = true)
	public int finRequestCountByRequestNo(String requestNo);
	
	@Query(value = "SELECT * FROM t_account_order WHERE orderNo = ?1 and orderType =?2 ", nativeQuery = true)
	public AccOrderEntity findOrderByOrderNoAndOrderType(String orderNo,String orderType);

	@Query(value = "SELECT COUNT(*) FROM t_account_order a LEFT JOIN t_bank_order b ON a.orderNo = b.orderNo WHERE a.orderStatus = '0' AND a.orderType = '51' AND a.userOid = ?1 AND b.cardNo = ?2", nativeQuery = true)
	public int findWithdrawalsInProcessCountByUserOidAndCard(String userOid, String enCardNo);

	@Query(value = "SELECT SUM(balance) FROM t_account_order WHERE userOid = ?1 AND orderType = ?2 AND orderStatus = '1'", nativeQuery = true)
	public BigDecimal findBalanceByUserOidAndOrderType(String userOid, String orderType);

	@Transactional
	@Modifying
	@Query(value = "UPDATE t_account_order SET orderStatus=?2 , updateTime=NOW() WHERE orderNo=?1 AND orderStatus= ?3 ", nativeQuery = true)
	public int updateOrderStatusByStatus(String orderNo, String orderstatusInit, String orderstatusFail);

}
