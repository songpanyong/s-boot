package com.guohuai.boot.pay.dao;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.guohuai.boot.pay.vo.BankLogVo;

public interface BankLogDao extends JpaRepository<BankLogVo, String>, JpaSpecificationExecutor<BankLogVo> {
	
	@Query(value="select * from t_bank_log  where orderNo=?1 order by createTime ", nativeQuery = true)
	public List<BankLogVo> findByNo(String orderNo);
	
	public BankLogVo findByPayNo(String payNo);
	
	@Query(value="select remark from t_bank_log  where sheetId=?1 order by createTime desc limit 1 ", nativeQuery = true)
	public String findRemarkByPayNo(String payNo);
	
	public BankLogVo findBySheetId(String sheetId);
	
	@Query(value="update t_bank_log set tradStatus=?1 , updateTime=NOW() where payNo=?2", nativeQuery = true)
	@Modifying
	@Transactional
	public int updateByPayNo(String tradStatus,String payNo);
	
	@Query(value="update t_bank_log set failDetail='' , updateTime=NOW() where payNo=?2", nativeQuery = true)
	@Modifying
	@Transactional
	public int updateDetailByPayNo(String payNo);
	
	@Query(value="select * from t_bank_log where requestNo=?1 limit 1", nativeQuery = true)
	public BankLogVo findOneByRequestNo(String requestNo);
}
