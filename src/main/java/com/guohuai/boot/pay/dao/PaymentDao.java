package com.guohuai.boot.pay.dao;

import java.sql.Timestamp;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.guohuai.boot.pay.vo.PaymentVo;

@RepositoryRestResource(path="payment")
public interface PaymentDao extends JpaRepository<PaymentVo, String>, JpaSpecificationExecutor<PaymentVo> {
	public PaymentVo findByOrderNo(String orderNo);
	
	@Query(value="select a.* from t_bank_payment a left join t_bank_reconciliation_pass s on a.payNo=s.orderId where a.type=?1  and  a.reconStatus=0 and s.reconStatus=0",nativeQuery = true)
	public List<PaymentVo> findByDiffPass(String type);
	
	@Query(value="select a.* from t_bank_payment a left join t_bank_history s on a.payNo=s.transactionFlow where a.type=?1  and  a.reconStatus=0 and s.reconStatus=0",nativeQuery = true)
	public List<PaymentVo> findByDiffMeta(String type);
	
	@Query(value="select * from t_bank_payment a where a.type=?1 and a.reconStatus=0 ",nativeQuery = true)
	public List<PaymentVo> findByNo(String type);
	
	@Query(value="select * from t_bank_payment a where a.reconStatus in (0,2) and a.upTime < ?1 and a.channelNo = ?2 and a.commandStatus !=0",nativeQuery = true)
	public List<PaymentVo> findByStatusAndTime(Timestamp endDate, String channelNo);
	
	@Query(value="select count(*) from t_bank_payment a where a.commandStatus='1' and  a.reconStatus in(1,2,3) and a.createTime>= ?1 and a.createTime <= ?2",nativeQuery = true)
	public long findCount(Timestamp startDate, Timestamp endDate);
	
	@Query(value="select * from t_bank_payment a where a.commandStatus='1' and  a.reconStatus in(1,2,3) and a.createTime>= ?1 and a.createTime <= ?2 limit ?3,?4",nativeQuery = true)
	public List<PaymentVo> findByRecon(Timestamp startDate, Timestamp endDate,long page,long size);
	
	@Query(value="select * from t_bank_payment a where a.reconStatus='2' and a.createTime>= ?1 and a.createTime <= ?2 order by a.oid limit ?3,?4",nativeQuery = true)
	public List<PaymentVo> findAllEXRecon(Timestamp startDate, Timestamp endDate, long page, long size);
	@Query(value="select * from t_bank_payment a where a.reconStatus='2' and a.channelNo = ?1 and a.createTime>= ?2 and a.createTime <= ?3 order by a.oid limit ?4,?5",nativeQuery = true)
	public List<PaymentVo> findAllEXReconByChannelNo(String channelNo, Timestamp startDate, Timestamp endDate, long page, long size);
	
	@Query(value="select * from t_bank_payment a where a.reconStatus='2' and  a.reconciliationMark = ?1 and a.createTime>= ?2 and a.createTime <= ?3 order by a.oid limit ?4,?5",nativeQuery = true)
	public List<PaymentVo> findEXRecon(String reconciliationMark, Timestamp startDate, Timestamp endDate, long page, long size);
	@Query(value="select * from t_bank_payment a where a.reconStatus='2' and channelNo = ?1 and  a.reconciliationMark = ?2 and a.createTime>= ?3 and a.createTime <= ?4 order by a.oid limit ?5,?6",nativeQuery = true)
	public List<PaymentVo> findEXReconByChannelNo(String channelNo, String reconciliationMark, Timestamp startDate, Timestamp endDate, long page, long size);
	
	@Query(value="select count(*) from t_bank_payment a where a.reconStatus='2' and a.reconciliationMark = ?1 and a.createTime>= ?2 and a.createTime <= ?3",nativeQuery = true)
	public long findEXCount(String reconciliationMark, Timestamp startDate, Timestamp endDate);
	@Query(value="select count(*) from t_bank_payment a where a.reconStatus='2' and a.channelNo = ?1 and a.reconciliationMark = ?2 and a.createTime>= ?3 and a.createTime <= ?4",nativeQuery = true)
	public long findEXCountByChannelNo(String channelNo, String reconciliationMark, Timestamp startDate, Timestamp endDate);
	
	@Query(value="select count(*) from t_bank_payment a where a.reconStatus='2' and a.createTime>= ?1 and a.createTime <= ?2",nativeQuery = true)
	public long findAllEXCount(Timestamp startDate, Timestamp endDate);
	@Query(value="select count(*) from t_bank_payment a where a.reconStatus='2' and a.channelNo = ?1 and a.createTime>= ?2 and a.createTime <= ?3",nativeQuery = true)
	public long findAllEXCountByChannelNo(String channelNo, Timestamp startDate, Timestamp endDate);
	
	@Query(value="select * from t_bank_payment a where a.payNo=?1",nativeQuery = true)
	public PaymentVo findByPayNo(String payNo);
	
	@Query(value="select * from t_bank_payment a where a.oid=?1 for update",nativeQuery = true)
	public PaymentVo findByOidForUpdate(String oid);
	
	@Query(value="update  t_bank_payment set commandStatus=?1 , updateTime=NOW() where payNo=?2", nativeQuery = true)
	@Modifying
	@Transactional
	public int updateByPayNo(String commandStatus,String payNo);
	
	@Query(value="update  t_bank_payment set failDetail='' , updateTime=NOW() where payNo=?2", nativeQuery = true)
	@Modifying
	@Transactional
	public int updateDetailByPayNo(String payNo);

	@Query(value="update t_bank_payment set reconStatus=3,reconciliationMark='对账忽略',updateTime=SYSDATE() where oid=?1 ",nativeQuery = true)
	@Modifying
	@Transactional
	public int updateStatus(String oid);
	
	@Query(value="SELECT a.oid FROM t_bank_payment a where type='02' and commandStatus not in('1','5') and "
			+ "IF (?1='0',((a.operatorStatus IS NULL OR a.auditUpdateStatus IS NOT NULL) "
			+ "AND (a.updateStatus IS NULL OR a.auditUpdateStatus IS NOT NULL) "
			+ "AND (a.resetOperatorStatus IS NULL OR a.auditResetOperatorStatus IS NOT NULL)),"
			+ "((a.operatorStatus IS NOT NULL and a.auditUpdateStatus IS NULL) "
			+ "OR (a.updateStatus IS NOT NULL and a.auditUpdateStatus IS NULL) "
			+ "OR (a.resetOperatorStatus IS NOT NULL and a.auditResetOperatorStatus IS NULL)) )"
			+ "AND IF(?2= '', a.userOid != '', a.userOid= ?2)"
			+ "AND IF(?3= '', a.realName != '', a.realName LIKE ?3 ) "
			+ "AND IF(?4= '', a.orderNo !='', a.orderNo LIKE ?4) "
			+ "AND IF(?5= '', a.payNo !='', a.payNo LIKE ?5)"
			+ "AND IF(?6 IS NULL OR ?6= '', 1= 1, a.createTime >= ?6) "
			+ "AND IF(?7 IS NULL OR ?7= '', 1= 1, a.createTime <= ?7)",nativeQuery = true)
	public Object[] queryOid(String queryType,String userOid, String realname, String orderNo, String payNo, String beginTime, String endTime);
	
	@Query(value="SELECT * FROM t_bank_payment a WHERE a.commandStatus = '0' AND a.Launchplatform ='2' AND (a.auditStatus !='2' OR a.auditStatus IS NULL)  AND IF(?1= '%%', a.realName != '', a.realName LIKE ?1) AND IF(?2= '', a.type != '', a.type= ?2) AND IF(?3= '', a.orderNo != '', a.orderNo= ?3) AND IF(?4= '', a.payNo != '', a.payNo= ?4) AND IF(?5= '', a.phone != '', a.phone= ?5) order by a.oid limit ?6,?7",nativeQuery = true)
	public List<PaymentVo> findLargeAmount(String realName, String type, String orderNo, String payNo,String phone, long page, long size);

	@Query(value="SELECT count(*) FROM t_bank_payment a WHERE a.commandStatus = '0' AND a.Launchplatform ='2' AND (a.auditStatus !='2' OR a.auditStatus IS NULL) AND IF(?1= '%%', a.realName != '', a.realName LIKE ?1) AND IF(?2= '', a.type != '', a.type= ?2) AND IF(?3= '', a.orderNo != '', a.orderNo= ?3) AND IF(?4= '', a.payNo != '', a.payNo= ?4) AND IF(?5= '', a.phone != '', a.phone= ?5)",nativeQuery = true)
	public long findLargeAmountCount(String realName, String type, String orderNo, String payNo, String phone);

	@Query(value="update t_bank_payment a set a.commandStatus = (select b.tradStatus from t_bank_reconciliation_pass b where b.orderId = ?1) WHERE a.payNo=?1",nativeQuery = true)
	@Modifying
	@Transactional
	public int updateStatusByPayNo(String payNo);
	
	@Query(value="SELECT * FROM t_bank_payment a WHERE a.commandStatus = '0' AND a.type = '01' AND a.channelNo = '9' AND a.createTime <= date_sub(now(), interval 6 hour) AND a.createTime > date_sub(now(), interval 12 hour)",nativeQuery = true)
	public List<PaymentVo> findNeedCloseOrderList();
	
	@Query(value="select SUM(amount) from t_bank_payment where type=?1 and auditStatus=?2 ",nativeQuery = true)
	public String findMemBalance(String type,String auditStatus);
	
	@Query(value="update  t_bank_payment set commandStatus=?1 , updateTime=NOW() where orderNo=?2", nativeQuery = true)
	@Modifying
	@Transactional
	public int updateStatusByOrderNo(String commandStatus,String orderNo);

	@Query(value="select * from t_bank_payment a where a.reconStatus in (0,2) and a.createTime >= ?1 and a.createTime < ?2 and a.channelNo = ?3",nativeQuery = true)
	public List<PaymentVo> findReconciliationByStatusAndTime(Timestamp startDate, Timestamp endDate, String channelNo);

	@Query(value="select sum(amount) from t_bank_payment a where a.reconStatus in (0,2) and a.createTime >= ?1 and a.createTime < ?2 and a.channelNo = ?3",nativeQuery = true)
	public String findReconciliationAmountByStatusAndTime(Timestamp startDate,
			Timestamp endDate, String channel);
	
	@Query(value = "select count(*) from t_bank_payment a where a.userOid =?1 and a.cardNo =?2 and a.type = '01' and a.commandStatus not in ('1','2')", nativeQuery = true)
	public int findRechargeInProcessCountByUserOidAndCard(String userOid, String cardNo);
}
