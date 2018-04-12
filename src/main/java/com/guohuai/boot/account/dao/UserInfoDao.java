package com.guohuai.boot.account.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.guohuai.boot.account.entity.UserInfoEntity;

public interface UserInfoDao extends JpaRepository<UserInfoEntity, String>, JpaSpecificationExecutor<UserInfoEntity> {
	
	@Query(value = "SELECT * FROM t_account_userinfo WHERE userOid = ?1", nativeQuery = true)
	public UserInfoEntity findByUserOid(String userOid);
	
	@Query(value = "SELECT * FROM t_account_userinfo WHERE userType = ?1 and systemSource = ?2", nativeQuery = true)
	public List<UserInfoEntity> findByUserTypeSystemSource(String userType, String systemSource);
	
	/**
	 * 修改密码
	 * @param oid
	 * @param password
	 * @return
	 */
	@Query("update UserInfoEntity set password = ?2, updateTime = sysdate() where oid = ?1 ")
	@Modifying
	public int updatePassword(String oid, String password);
	
	@Query(value = "select DISTINCT(b.userOid),b.realName from t_account_info a LEFT JOIN t_bank_protocol b on a.userOid = b.userOid where b.status='1' and b.realName is not null", nativeQuery = true)
	public Object[] findAllUserName();
	
	
	@Query(value = "SELECT * FROM t_account_userinfo WHERE phone = ?1", nativeQuery = true)
	public UserInfoEntity findByPhone(String phone);
	
	/**
	 * 修改手机号
	 * @param oid
	 * @param password
	 * @return
	 */
	@Query("update UserInfoEntity set phone = ?1, updateTime = sysdate(), remark=?2 where oid = ?3 ")
	@Modifying
	@Transactional
	public int updatePhone(String phone, String remark,String oid);
	
	@Query(value = "SELECT * FROM t_account_userinfo WHERE name = ?1 limit 1", nativeQuery = true)
	public UserInfoEntity findByRealName(String realName);
}
