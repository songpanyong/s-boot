package com.guohuai.boot.pay.dao;

import java.sql.Timestamp;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.guohuai.boot.pay.vo.ReconciliationPassVo;

@RepositoryRestResource(path="mete")
public interface ReconciliationPassDao extends JpaRepository<ReconciliationPassVo, String>, JpaSpecificationExecutor<ReconciliationPassVo> {
	
	public ReconciliationPassVo findByOrderId(String orderId);
	
	@Query(value="select * from t_bank_reconciliation_pass a where a.reconStatus=0",nativeQuery = true)
	public List<ReconciliationPassVo> findByNo();
	
	@Query(value="select * from t_bank_reconciliation_pass a where a.reconStatus in (0,2) and a.channelId = ?1 and a.transactionTime < ?2",nativeQuery = true)
	public List<ReconciliationPassVo> findByChannelAndTime(String channelId, Timestamp endDate);
	
	@Query(value="select * from t_bank_reconciliation_pass a where a.channelId = ?4 and a.reconStatus = ?3 and a.transactionTime > ?1 and a.transactionTime < ?2",nativeQuery = true)
	public List<ReconciliationPassVo> findByDateAndStatus(Timestamp startDate, Timestamp endDate, int reconStatus, String channelId);

	@Query(value="select * from t_bank_reconciliation_pass a where a.channelId = ?3 and a.reconStatus !='3' and a.transactionTime > ?1 and a.transactionTime < ?2",nativeQuery = true)
	public List<ReconciliationPassVo> findByDate(Timestamp startDate,Timestamp endDate, String channelId);

	@Modifying
	@Transactional
	@Query(value="update t_bank_reconciliation_pass set reconStatus=3,checkMark='对账忽略',updateTime=SYSDATE() where oid=?1 ",nativeQuery = true)
	public int updateStatus(String oid);

	@Modifying
	@Transactional
	@Query(value="update t_bank_reconciliation_pass set repairStatus='Y',updateTime=SYSDATE() where orderId=?1 ",nativeQuery = true)
	public int updateRepairStatus(String orderId);

	@Query(value="select * from t_bank_reconciliation_pass a where a.reconStatus in (0,2) and a.channelId = ?1 and a.transactionTime >= ?2 and a.transactionTime < ?3",nativeQuery = true)
	public List<ReconciliationPassVo> findReconciliationByChannelAndTime(String channel, Timestamp startDate, Timestamp endDate);

	@Query(value="select sum(transactionAmount) from t_bank_reconciliation_pass a where a.reconStatus in (0,2) and a.channelId = ?1 and a.transactionTime >= ?2 and a.transactionTime < ?3",nativeQuery = true)
	public String findReconciliationAmountByChannelAndTime(String channel, Timestamp startDate, Timestamp endDate);

}
