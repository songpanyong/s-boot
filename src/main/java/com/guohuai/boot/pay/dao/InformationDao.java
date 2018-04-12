package com.guohuai.boot.pay.dao;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.guohuai.boot.pay.vo.InformationVo;

@RepositoryRestResource(path="elementValidation")
public interface InformationDao extends JpaRepository<InformationVo, String>, JpaSpecificationExecutor<InformationVo> {
	
	//更新
	@Query("update  InformationVo set accountStatus=?1 ,updateTime=SYSDATE() where oid=?2")
	@Modifying
	@Transactional
	public int updateStatus(String status,String oid);
	
}
