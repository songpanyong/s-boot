package com.guohuai.boot.pay.dao;

import java.sql.Timestamp;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.guohuai.boot.pay.vo.ReconciliationStatisticsVo;

@RepositoryRestResource(path="mete")
public interface ReconciliationStatisticsDao extends JpaRepository<ReconciliationStatisticsVo, String>, JpaSpecificationExecutor<ReconciliationStatisticsVo> {
	
	@Query(value="select * from t_bank_reconciliation_statistics a where a.channelNo=?1 and a.outsideDate = ?2",nativeQuery = true)
	public ReconciliationStatisticsVo findByChannelNoAndOutsideDate(String channelNo, Timestamp outsideDate);

	@Query(value="update t_bank_reconciliation_statistics set reconciliationStatus=1,confirmDate=SYSDATE() where channelNo=?1 and outsideDate=?2 ",nativeQuery = true)
	@Modifying
	@Transactional
	public int confirmCompleteReconciliation(String channleNo, Timestamp outsideDate);

	@Query(value="select count(*) from t_bank_reconciliation_statistics a where a.outsideDate = ?1 and a.reconciliationStatus=1",nativeQuery = true)
	public int getcompleteCount(Timestamp outsideDate);
	
	@Query(value="select count(*) from t_bank_reconciliation_statistics a where a.outsideDate = ?1 and a.channelNo = ?2",nativeQuery = true)
	public int getReconciliationCount(Timestamp outsideDate,String channelNo);

	@Query(value="select count(*) from t_bank_reconciliation_statistics a where a.channelNo = ?1 and a.outsideDate = ?2 and a.reconciliationStatus = ?3",nativeQuery = true)
	public int findCompleteReconciliation(String channleNo,Timestamp outsideDate,String reconciliationStatus);
}
