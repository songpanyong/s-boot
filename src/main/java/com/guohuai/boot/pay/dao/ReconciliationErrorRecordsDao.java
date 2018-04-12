package com.guohuai.boot.pay.dao;

import com.guohuai.boot.pay.vo.ReconciliationErrorRecordsVo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import javax.transaction.Transactional;
import java.sql.Timestamp;

@RepositoryRestResource(path = "mete")
public interface ReconciliationErrorRecordsDao extends JpaRepository<ReconciliationErrorRecordsVo, String>, JpaSpecificationExecutor<ReconciliationErrorRecordsVo> {

    @Query(value = "update t_bank_reconciliation_error_records set errorStatus=?2,errorResult=?3,updateTime=SYSDATE() where oid = ?1", nativeQuery = true)
    @Modifying
    @Transactional
    public int reviewResultByOid(String oid, String errorStatus, String errorResult);

    @Query(value = "select count(*) from t_bank_reconciliation_error_records a where a.channelNo = ?1 and a.orderTime >= ?2 and a.orderTime < date_sub( ?2 ,interval -1 day) and a.errorStatus=?3", nativeQuery = true)
    public int getNotcompleteCount(String channelNo, Timestamp completeDate, String errorStatus);

    @Transactional
    @Modifying
    @Query(value = "UPDATE t_bank_reconciliation_error_records SET  errorStatus=?2, errorResult=?3,  remark=?4,errorSort='1', updateTime= now() WHERE oid=?1 and errorStatus=?5", nativeQuery = true)
    int artificialCompositeUpdateData(String oid, String errorStatus, String errorResult, String remark, String oldErrorStatus);

    @Transactional
    @Modifying
    @Query(value = "UPDATE t_bank_reconciliation_error_records SET orderStatus=?2, errorStatus=?3, errorResult=?4, remark=?5,errorSort='1', updateTime= now() WHERE oid=?1 and errorStatus=?6", nativeQuery = true)
    int sureUpdate(String oid, String orderStatus, String errorStatus, String errorResult, String remark, String oldErrorStatus);
}
