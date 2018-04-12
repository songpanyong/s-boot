package com.guohuai.boot.pay.dao;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.guohuai.boot.pay.vo.ChannelVo;

@RepositoryRestResource(path="elementValidation")
public interface ComChannelDao extends JpaRepository<ChannelVo, String>, JpaSpecificationExecutor<ChannelVo> {
	
	@Query("UPDATE ChannelVo SET status = ?2, updateTime = SYSDATE() WHERE oid = ?1")
	@Modifying
	@Transactional
	public int changeStatus(String oid, String status);
	
	public ChannelVo findByChannelNo(String channelNo);

	@Query(value="SELECT * FROM t_bank_channel a WHERE a.tradeType = '01' AND a.`status` = '1' LIMIT 1",nativeQuery = true)
	public ChannelVo findBindChannel();
}
