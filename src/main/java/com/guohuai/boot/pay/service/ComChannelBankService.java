package com.guohuai.boot.pay.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.response.CardQuotaQueryResponse;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.pay.dao.ComChannelBankDao;
import com.guohuai.boot.pay.form.BankInfo;
import com.guohuai.boot.pay.form.ChannelBankForm;
import com.guohuai.boot.pay.form.ChannelBankInfo;
import com.guohuai.boot.pay.res.ChannelBankVoRes;
import com.guohuai.boot.pay.vo.ChannelBankVo;
import com.guohuai.component.util.Constant;
import com.guohuai.payadapter.bankutil.BankUtilDao;
import com.guohuai.payadapter.bankutil.BankUtilEntity;
import com.guohuai.payadapter.bankutil.BankUtilService;

@Service
public class ComChannelBankService {
	private final static Logger log = LoggerFactory.getLogger(ComChannelBankService.class);
	@Autowired
	private ComChannelBankDao comChannelBankDao;
	@Autowired
	BankUtilService bankUtilService;
	
	@Autowired
	private BankUtilDao bankUtilDao;
	
	public void save(ChannelBankForm req) {
		log.info("{},渠道支持银行添加,{},", JSONObject.toJSONString(req));
		ChannelBankVo vo = new ChannelBankVo();
		BeanUtils.copyProperties(req, vo);
		String bankCode = req.getChannelbankName();
		String channelBankName = bankUtilDao.getNameByBankCode(bankCode);
		 vo.setStandardCode(bankCode);
		 vo.setChannelbankName(channelBankName);
		 vo.setCreateTime(new Timestamp(System.currentTimeMillis()));
		 vo.setOid(StringUtil.uuid());
		 comChannelBankDao.save(vo);
	}

	public void update(ChannelBankForm req) {
		log.info("{},渠道支持银行修改,{},", req.getOid(),JSONObject.toJSONString(req));
		ChannelBankVo vo=comChannelBankDao.findOne(req.getOid());
		String bankCode = req.getChannelbankName();
		String channelBankName = bankUtilDao.getNameByBankCode(bankCode);
		if (vo != null) {
			req.setCreateTime(vo.getCreateTime());
			BeanUtils.copyProperties(req, vo);
			vo.setStandardCode(bankCode);
			vo.setChannelbankName(channelBankName);
			vo.setUpdateTime(new Timestamp(System.currentTimeMillis()));
			comChannelBankDao.save(vo);
		}
	}

	public void delete(String oid) {
		log.info("{},渠道银行信息删除", oid);
		if (oid != null) {
			comChannelBankDao.delete(oid);
		}
	}
	
	//判断输入银行名称是否正确,返回银行代码
//	public String findBank(String channelbankName){
//		String standardCode = bankUtilDao.getCodeByName(channelbankName);
//		if(!StringUtils.isEmpty(standardCode)){
//			return standardCode;
//		}else{
//			return null;
//		}
//	}
	
	//判断输入渠道下的银行是否重复
	public int findBankReap(String channelNo,String standardCode){
		List<ChannelBankVo> list = comChannelBankDao.findByChannelAndBank(channelNo, standardCode);
		if(list.size()>1){
			return list.size();
		}
		return 0;
	}

//	判断通道银行代码是否错误
//	public int findChannelbankCode(String channelbankCode){
//		List<ChannelBankVo> list = comChannelBankDao.findByChannelbankCode(channelbankCode);
//		if(list.size()>0){
//			return list.size();
//		}
//		return 0;
//	}

	public ChannelBankVoRes page(ChannelBankForm req) {
		log.info("{},渠道支持银行查询,{},", req.getUserOid(), JSONObject.toJSONString(req));
		Page<ChannelBankVo> listPage = comChannelBankDao.findAll(buildSpecification(req),
				new PageRequest(req.getPage() - 1, req.getRows()));
		ChannelBankVoRes res = new ChannelBankVoRes();
		if (listPage != null && listPage.getSize() > 0) {
			res.setRows(listPage.getContent());
			res.setTotalPage(listPage.getTotalPages());
			res.setPage(req.getPage());
			res.setRow(req.getRows());
			res.setTotal(listPage.getTotalElements());
			return res;
		}
		return null;
	}

	public Specification<ChannelBankVo> buildSpecification(final ChannelBankForm req) {
		Specification<ChannelBankVo> spec = new Specification<ChannelBankVo>() {
			@Override
			public Predicate toPredicate(Root<ChannelBankVo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> bigList = new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(req.getUserOid()))
					bigList.add(cb.equal(root.get("userOid").as(String.class), req.getUserOid()));
				if (!StringUtil.isEmpty(req.getChannelNo()))
					bigList.add(cb.equal(root.get("channelNo").as(String.class), req.getChannelNo()));
				if (!StringUtil.isEmpty(req.getStandardCode()))
					bigList.add(cb.equal(root.get("standardCode").as(String.class), req.getStandardCode()));
				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
				query.orderBy(cb.desc(root.get("createTime")));

				// 条件查询
				return query.getRestriction();
			}
		};
		return spec;
	}

	/**
	 * 
	 * @param bankCard
	 * @return
	 */
	public boolean whetherSupportBank(String bankCard) {
		// 根据银行卡号获取银行信息
		BankUtilEntity bank = bankUtilService.getBankByCard(bankCard);
		if(bank.getBankCode() != null){
			//根据人行代码查看是否支持该家银行
			List<ChannelBankVo> list = comChannelBankDao.findByCode(bank.getBankCode());
			if(list.size()>0){
				return true;
			}
		}
		return false;
	}
	
	// 根据银行卡号获取银行信息
	public String findBankName(String bankCard) {
		BankUtilEntity bank = null;
			bank = bankUtilService.getBankByCard(bankCard);
		if(bank != null){
			return bank.getBankName();
		}else{
			return null;
		}
	}
	
	//查询所有支持的银行
	public List<String> findAllBankName(){
		List<String> bankList = null;
		bankList = comChannelBankDao.findAllBankName() ;
		return bankList;
	}
	
	// 查询所有支持的银行
	public List<BankInfo> findAllBank() {
		List<BankInfo> bankInfoList = new ArrayList<>();
		Object[] bankList = comChannelBankDao.findAllBank();
		for (Object _bankInfo : bankList) {
			Object[] bank = (Object[]) _bankInfo;
			BankInfo bankInfo = new BankInfo();
			bankInfo.setBankName(bank[0].toString());
			bankInfo.setBankCode(bank[1].toString());
			bankInfoList.add(bankInfo);
		}
		return bankInfoList;
	}
	
	/**
	 * 查询银行基础表中所有银行名称
	 */
	public List<BankInfo>  findAllBankNameAndBankCode() {
		log.info("查询所有银行名称和代码");
		List<BankInfo> bankInfoList = new ArrayList<>();
		Object[] bankList = comChannelBankDao.findAllBankNameAndBankCode();
		for (Object _bankInfo : bankList) {
			Object[] bank = (Object[]) _bankInfo;
			BankInfo bankInfo = new BankInfo();
			bankInfo.setBankName(bank[1].toString());
			bankInfo.setBankCode(bank[0].toString());
			bankInfoList.add(bankInfo);
		}
		return bankInfoList;
	}
	
	/**
	 * 根据银行卡查询支持的单笔限额
	 * @param bankCard
	 * @return
	 */
	public BigDecimal getSingleQuota(String bankCard) {
		// 根据银行卡号获取银行信息
		BankUtilEntity bank = bankUtilService.getBankByCard(bankCard);
		if(bank.getBankCode() != null){
			//根据人行代码查看是否支持该家银行
			BigDecimal singleQuota = comChannelBankDao.findSingleQuotaByCode(bank.getBankCode(), "01");
			if(singleQuota != null){
				return singleQuota;
			}
		}
		return new BigDecimal("0");
	}

	/**
	 * 根据渠道查询渠道银行限额信息
	 * @param channelNo
	 * @return
	 */
	@SuppressWarnings("unused")
	public List<ChannelBankInfo> getChannelBank(String channelNo) {
		List<ChannelBankInfo> bankList = null;
		bankList = comChannelBankDao.getChannelBank(channelNo);
		return null;
	}
	
	/**
	 * 查询渠道支持的银行
	 */
	public List<ChannelBankVo> findChannelBank(String cardNo,String channelNo){
		log.info("根据卡号查银行信息,{}",JSONObject.toJSONString(cardNo));
		BankUtilEntity bank =  bankUtilService.getBankByCard(cardNo);
		log.info("根据卡号查银行信息,{}",JSONObject.toJSONString(bank));
		List<ChannelBankVo>  channelBankList = null;
		if(bank!=null){
			channelBankList = comChannelBankDao.findChannelbankByCode(bank.getBankCode(), channelNo);
			return channelBankList;
		}
		return null;
	}
	
	/**
	 * 根据银行卡查询支持的限额信息
	 * @param bankCard
	 * @return
	 */
	public CardQuotaQueryResponse getBanKQuotaByBankCard(String bankCard) {
		CardQuotaQueryResponse resp = new CardQuotaQueryResponse();
		// 根据银行卡号获取银行信息
		BankUtilEntity bank = bankUtilService.getBankByCard(bankCard);
		if(bank.getBankCode() != null){
			resp.setBankCard(bankCard);
			resp.setBankCode(bank.getBankCode());
			resp.setBankName(bank.getBankName());
			resp.setKindOfCard(bank.getKindOfCard());
			//根据人行代码查看是否支持该家银行
			BigDecimal rechageSingleQuota= comChannelBankDao.findSingleQuotaByCode(bank.getBankCode(), "01");
			BigDecimal rechageDailyLimit= comChannelBankDao.findDailyLimitByCode(bank.getBankCode(), "01");
			BigDecimal withdrawalsSingleQuota= comChannelBankDao.findSingleQuotaByCode(bank.getBankCode(), "02");
			BigDecimal withdrawalsDailyLimit= comChannelBankDao.findDailyLimitByCode(bank.getBankCode(), "02");
			if(rechageSingleQuota != null){
				resp.setRechageDailyLimit(rechageDailyLimit);
				resp.setRechageSingleQuota(rechageSingleQuota);
				resp.setReturnCode(Constant.SUCCESS);
				resp.setErrorMessage("查询成功");
			}else {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("支付渠道不支持该银行卡");
				return resp;
			}
			if(withdrawalsSingleQuota != null){
				resp.setWithdrawalsDailyLimit(withdrawalsDailyLimit);
				resp.setWithdrawalsSingleQuota(withdrawalsSingleQuota);
			}else{
				resp.setWithdrawalsDailyLimit(BigDecimal.ZERO);
				resp.setWithdrawalsSingleQuota(BigDecimal.ZERO);
			}
		}else{
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("未知银行卡");
		}
		return resp;
	}
	
	
}