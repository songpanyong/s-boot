package com.guohuai.boot.pay.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.guohuai.boot.pay.vo.AuthenticationVo;

@RepositoryRestResource(path="authentication")
public interface AuthenticationDao extends JpaRepository<AuthenticationVo, String>, JpaSpecificationExecutor<AuthenticationVo> {
	@Query(value="select * from t_bank_authentication  where orderNo=?1 and status='1'  and  errorCode='0000' ", nativeQuery = true)
	public List<AuthenticationVo> findByOrderNo(String orderNo);
 	
	@Query(value="select * from t_bank_authentication  where (phone=?1 or cardNo=?2 or certificateNo=?3  or  userOid=?4)  and status='1'  and  errorCode='0000' ", nativeQuery = true)
	public List<AuthenticationVo> findByPhone(String phone,String cardNo,String certificateNo,String userOid);
	
}
