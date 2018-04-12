package com.guohuai.boot.pay.dao;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.guohuai.boot.pay.form.ChannelBankInfo;
import com.guohuai.boot.pay.vo.ChannelBankVo;

public interface ComChannelBankDao extends JpaRepository<ChannelBankVo, String>, JpaSpecificationExecutor<ChannelBankVo> {
	
	//根据人行代码查看是否支持该银行卡
	@Query(value="select * from t_bank_channel_bank a where a.standardCode =?1",nativeQuery = true)
	public List<ChannelBankVo> findByCode(String standardCode);
	
	@Query(value="select * from t_bank_channel_bank where channelNo =?1 and standardCode=?2 ",nativeQuery = true)
	public List<ChannelBankVo> findByChannelAndBank(String channelNo,String standardCode); 
	
	@Query(value="select * from t_bank_channel_bank where channelbankCode =?1",nativeQuery = true)
	public List<ChannelBankVo> findByChannelbankCode(String channelbankCode);
	
	//查询支持的所有银行
	@Query(value="select a.channelbankName from t_bank_channel_bank as a where a.oid in ( select MIN(a.oid) from t_bank_channel_bank as a LEFT JOIN t_bank_channel as b on a.channelNo = b.channelNo where b.`status`=1 GROUP BY a.standardCode ) ",nativeQuery = true)
	public List<String>  findAllBankName();
	
	//查询支持的所有银行
	@Query(value="select a.channelbankName,a.standardCode from t_bank_channel_bank as a where a.oid in ( select MIN(a.oid) from t_bank_channel_bank as a LEFT JOIN t_bank_channel as b on a.channelNo = b.channelNo where b.`status`=1 GROUP BY a.standardCode ) ",nativeQuery = true)
	public Object[]  findAllBank();
	
	//根据人行代码查看支持的单笔限额
	@Query(value="select a.singleQuota from t_bank_channel_bank a left join t_bank_channel b on a.channelNo = b.channelNo where a.standardCode = ?1 and b.status = 1 and b.tradeType = ?2 order by a.singleQuota desc limit 1",nativeQuery = true)
	public BigDecimal findSingleQuotaByCode(String standardCode, String tradeType);

	//根据渠道查询该渠道下银行信息
	@Query(value="select a.standardCode,a.channelbankName,a.channelbankCode,a.singleQuota,a.dailyLimit,a.monthlyLimit from t_bank_channel_bank a where a.channelNo=?1",nativeQuery = true)
	public List<ChannelBankInfo> getChannelBank(String channelNo);
	
	@Query(value="select * from t_bank_channel_bank where standardCode =?1 and channelNo=?2",nativeQuery = true)
	public List<ChannelBankVo> findChannelbankByCode(String standardCode,String channelNo);
	
	//查询银行基础表里的所有银行名称
	@Query(value="select distinct bankCode,bankName from t_bank_card_bin_basic GROUP BY bankCode",nativeQuery = true)
	public Object[] findAllBankNameAndBankCode();
	
	//根据人行代码查看支持的单日限额
	@Query(value="select a.dailyLimit from t_bank_channel_bank a left join t_bank_channel b on a.channelNo = b.channelNo where a.standardCode = ?1 and b.status = 1 and b.tradeType = ?2 order by a.singleQuota desc limit 1",nativeQuery = true)
	public BigDecimal findDailyLimitByCode(String standardCode, String tradeType);

	@Query(value="SELECT * FROM t_bank_channel_bank WHERE channelNo = ?1 AND standardCode = ?2 LIMIT 1",nativeQuery = true)
	public ChannelBankVo findByChannelAndBankCode(String channelNo,String bankCode);
}
