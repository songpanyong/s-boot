package com.guohuai.boot.pay.dao;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.guohuai.boot.pay.vo.ProtocolVo;

public interface ProtocolDao extends JpaRepository<ProtocolVo, String>, JpaSpecificationExecutor<ProtocolVo> {
	
	public ProtocolVo findOneByUserOidAndStatus(String userOid,String status);
	
	@Query(value = "select * from t_bank_protocol a where a.userOid = ?1 and a.cardNo=?2 and a.status=?3 limit 1", nativeQuery = true)
	public ProtocolVo findOneByUserOid(String userOid,String cardNo,String status);
	
	@Query(value = "select * from t_bank_protocol a where (a.cardNo=?1 or a.certificateNo=?2  or a.phone=?3) and a.status='1' limit 1", nativeQuery = true)
	public ProtocolVo findOneBySome(String cardNo,String certificateNo,String phone);
	
	@Query(value="update t_bank_protocol a set a.status=?1,a.updateTime=NOW() where a.userOid=?2 and a.cardNo=?3", nativeQuery = true)
	@Modifying
	@Transactional
	public int updateProtocolBytatus(String status,String userOid,String cardNo);
	
	@Query(value = "select * from t_bank_protocol  where userOid=?1 and status=?2", nativeQuery = true)
	public List<ProtocolVo> findListByUserOidAndStatus(String userOid,String status);

	@Query(value = "SELECT * FROM t_bank_protocol WHERE userOid = ?1 AND cardNo = ?2 ORDER BY createTime DESC LIMIT 1", nativeQuery = true)
	public ProtocolVo findProtocolByUserOidAndCard(String userOid, String cardNo);

    public ProtocolVo findByCardNoAndStatus(String cardNo,String status);

    @Query(value = "SELECT * FROM t_bank_protocol WHERE certificateNo = ?1 AND authenticationStatus = 'Y'", nativeQuery = true)
	public ProtocolVo findAuthenticationByCertNo(String encCertNo);
    @Query(value = "SELECT * FROM t_bank_protocol WHERE cardNo = ?1 AND `status` = '1'", nativeQuery = true)
	public List<ProtocolVo> findProtocolListByCardNo(String encCardNo);
    @Query(value = "SELECT count(*) FROM t_bank_protocol WHERE userOid = ?1 AND `status` = '1'", nativeQuery = true)
	public int findBindedCount(String userOid);
    @Query(value = "SELECT * FROM t_bank_protocol WHERE userOid = ?1 AND authenticationStatus = 'Y'", nativeQuery = true)
	public ProtocolVo findAuthenticationByUserOid(String userOid);
    @Query(value = "SELECT * FROM t_bank_protocol WHERE userOid = ?1 AND `status` = ?2 AND cardNo = ?3", nativeQuery = true)
	public ProtocolVo findOneByUserOidAndStatusAndCarNo(String userOid,String status,String cardNo);
}
