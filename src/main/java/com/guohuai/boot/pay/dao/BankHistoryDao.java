package com.guohuai.boot.pay.dao;

import java.sql.Timestamp;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.guohuai.boot.pay.vo.BankHistoryVo;

@RepositoryRestResource(path="elementValidation")
public interface BankHistoryDao extends JpaRepository<BankHistoryVo, String>, JpaSpecificationExecutor<BankHistoryVo> {
	@Query(value="select * from t_bank_history  a where a.reconStatus=0",nativeQuery = true)
	public List<BankHistoryVo> findByNo();
	
	//对账
	@Query(value="select * from t_bank_history  a where a.reconStatus in (0,2) and createTime < ?1 and bankType = ?2",nativeQuery = true)
	public List<BankHistoryVo> findByChannelAndTime(Timestamp endDate, String bankType);
	
	//忽略对账
	@Query(value="update t_bank_history set reconStatus=3,updateTime=SYSDATE() where oid=?1 ",nativeQuery = true)
	@Modifying
	@Transactional
	public int updateStatus(String oid);
	
}
