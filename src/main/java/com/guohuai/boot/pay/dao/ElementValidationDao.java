package com.guohuai.boot.pay.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.guohuai.boot.pay.vo.ElementValidationVo;

@RepositoryRestResource(path="elementValidation")
public interface ElementValidationDao extends JpaRepository<ElementValidationVo, String>, JpaSpecificationExecutor<ElementValidationVo> {
	//查询用户下所有的数据
	public List<ElementValidationVo> findByUserOid(String userOid);
	@Query(value="select * from t_bank_element_validation  where (phone=?1 or cardNo=?2 or certificateNo=?3 or realName=?4 or  userOid=?5)  and status='1'  and  errorCode='0000' limit 1", nativeQuery = true)
	public ElementValidationVo findBySingleOne(String phone,String cardNo,String certificateNo,String realName,String userOid);
	
	@Query(value="select * from t_bank_element_validation  where userOid=?1 and cardNo=?2  and status='1'  and  errorCode='0000' ", nativeQuery = true)
	public ElementValidationVo findBySingleOne(String userOid,String cardNo);
	
	@Query(value="select * from t_bank_element_validation  where userOid=?1 and cardNo=?2  and status='2'  and  errorCode='0000' ", nativeQuery = true)
	public ElementValidationVo findBySingleOneByUN(String userOid,String cardNo);
	
	@Query(value="select * from t_bank_element_validation  where userOid=?1 and cardOrderId=?2  and status='3' ", nativeQuery = true)
	public ElementValidationVo findByCardOrderId(String userOid,String carderOrderId);	
	
	@Query(value="select * from t_bank_element_validation  where userOid=?1 and bindChannel=?2  and status='1' limit 1", nativeQuery = true)
	public ElementValidationVo findByBindChannel(String userOid,String bindChannel);
	
}
