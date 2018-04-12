package com.guohuai.boot.account.service;

import java.math.BigDecimal;
import java.sql.Timestamp;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.CreateTransRequest;
import com.guohuai.account.api.response.CreateTransResponse;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.Constant;
import com.guohuai.settlement.api.response.BaseResponse;

/**
 * @ClassName: AccountRechargeService
 * @Description: 充值
 * @author CHENDONGHUI
 * @date 2017年8月17日 下午 11:38:22
 */
@Slf4j
@Service
public class AccountRechargeService {

	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private AccountTradeService accountTradeService;
	
	/**
	 * 用户基本户余额修复
	 * @param userType
	 * @param userOid
	 * @param frozenAccountNo
	 * @param basicAccount
	 * @param orderBalance
	 * @param withdrawalsFrozenAccount
	 * @param accOrderEntity
	 * @return
	 */
	@Transactional
	public BaseResponse addBasicAccountTrans(String userType, String userOid, 
			AccountInfoEntity basicAccount, BigDecimal orderBalance, AccOrderEntity accOrderEntity ){
		log.info("用户{}基本户余额修复,增加用户基本户{}余额,金额{}", userOid, basicAccount.getAccountNo(), orderBalance);
		BaseResponse resp = new BaseResponse();
		CreateTransResponse transResp = null;
		//操作账户
		accountInfoDao.addBalance(orderBalance, basicAccount.getAccountNo());
		BigDecimal afterBalance = basicAccount.getBalance().add(orderBalance);
		log.info("充值记录基本户明细 accountNo={}，orderBalance={}，orderNo={}， userOid={}，publisherUserOid={}",
				basicAccount.getAccountNo(), orderBalance, accOrderEntity.getOrderNo(), userOid);
		String orderDesc = "用户基本户余额修复，增加用户基本户余额";
		transResp = this.addTrans(accOrderEntity, basicAccount.getAccountNo(), 
				"", afterBalance, orderDesc, "01" ,AccountTypeEnum.BASICER.getCode());
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		}else{
			resp.setReturnCode(transResp.getReturnCode());
			resp.setErrorMessage(transResp.getErrorMessage());
			log.info("修复基本户余额增加失败，记录明细余额失败，,resp={}", JSONObject.toJSON(resp));
		}
		return resp;
	}
	
	/**
	 * 组装交易流水,调用原有方法
	 * @param inputAccountNo
	 * @param outpuptAccountNo
	 * @param userOid
	 * @param orderBalance
	 * @param afterBalance
	 * @param remark
	 * @param orderDesc
	 * @param direction
	 * @return
	 */
	@Transactional
	private CreateTransResponse addTrans(AccOrderEntity accOrderEntity, String inputAccountNo, 
			String outpuptAccountNo, BigDecimal afterBalance, String orderDesc, String direction,String accountType) {
		CreateTransRequest transRequest = new CreateTransRequest();
		transRequest.setUserOid(accOrderEntity.getUserOid());
		transRequest.setAccountOid(inputAccountNo);
		transRequest.setUserType(accOrderEntity.getUserType());
		transRequest.setRequestNo(accOrderEntity.getRequestNo());
		transRequest.setAccountOrderOid(accOrderEntity.getOrderNo());
		transRequest.setOrderType(accOrderEntity.getOrderType());
		transRequest.setSystemSource(accOrderEntity.getSystemSource());
		transRequest.setOrderNo(accOrderEntity.getOrderNo());
		transRequest.setRelationProductNo("");
		transRequest.setDirection(direction);
		transRequest.setInputAccountNo(inputAccountNo);
		transRequest.setOutpuptAccountNo(outpuptAccountNo);
		transRequest.setOrderBalance(accOrderEntity.getBalance());
		transRequest.setVoucher(accOrderEntity.getVoucher());
		transRequest.setTransTime(new Timestamp(System.currentTimeMillis()));
		transRequest.setDataSource("mimosa");
		transRequest.setBalance(afterBalance);
		transRequest.setRamark(accOrderEntity.getRemark());
		transRequest.setOrderDesc(orderDesc);
		transRequest.setAccountType(accountType);
		// 财务入账标志
		return accountTradeService.addAccountTrans(transRequest);
	}
	
}
