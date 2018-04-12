package com.guohuai.boot.pay.dao;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.guohuai.boot.pay.vo.ReconciliationRecordsVo;

public interface ReconciliationRecordsDao extends JpaRepository<ReconciliationRecordsVo, String>, JpaSpecificationExecutor<ReconciliationRecordsVo> {
	
	@Query(value="UPDATE t_bank_reconciliation_records SET reconStatus = ?2, updateTime = SYSDATE() WHERE reconDate = ?1 and channelId = ?3",nativeQuery = true)
	@Modifying
	@Transactional
	public int changeStatus(String reconDate, String reconStatus, String channelId);
	
	@Query(value="select * from t_bank_reconciliation_records a where a.reconDate = ?1 and a.channelId = ?2",nativeQuery = true)
	public ReconciliationRecordsVo findByDate(String reconDate,String channelId);
	
}
