package com.guohuai.boot.account.dao;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.guohuai.boot.account.entity.TransEntity;

public interface TransDao extends JpaRepository<TransEntity, String>, JpaSpecificationExecutor<TransEntity> {
	/**
	 * 根据用户userOid、定单号查询交易记录
	 * @param userOid
	 * @param orderNo
	 * @return
	 */
	public List<TransEntity> findByUserOidAndOrderNo(String userOid, String orderNo);


	public TransEntity findFirstByOrderNoAndUserOid(String orderNo, String userOid);

	@Query(value="select * from t_account_trans a LEFT JOIN t_account_info b ON a.accountOid = b.accountNo "
			+ "WHERE b.accountType = '14' AND a.createTime>?1 "
			+ "AND a.createTime<=date_sub(?1 ,interval -1 day) "
			+ "GROUP BY a.userOid limit ?2,?3",nativeQuery = true)
	public List<TransEntity> getNeedUnfreezeAccountList(Timestamp outsideDate, int start, int end);
	
	@Query(value="select count(t.counts) from (select count(*) counts from t_account_trans a LEFT JOIN t_account_info b ON a.accountOid = b.accountNo "
			+ "WHERE b.accountType = '14' AND a.createTime>?1 "
			+ "AND a.createTime<=date_sub(?1 ,interval -1 day)group by a.userOid) t",nativeQuery = true)
	public int getNeedUnfreezeAccountCount(Timestamp outsideDate);
	
	@Query(value="SELECT * FROM t_account_trans WHERE accountOid = ?1 AND createTime>?2 AND createTime<=date_sub(?2 ,interval -1 day) ORDER BY createTime DESC LIMIT 1",nativeQuery = true)
	public TransEntity getNeedUnfreezeAccount(String accountOid,Timestamp outsideDate);
	
}
