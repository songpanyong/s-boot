package com.guohuai.boot.account.service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.*;
import com.guohuai.account.api.request.entity.AccountOrderDto;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.account.api.response.AccountTransferResponse;
import com.guohuai.account.api.response.CreateOrderResponse;
import com.guohuai.account.api.response.CreateTransResponse;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.pay.res.CreateBatchAccountOrderRes;
import com.guohuai.component.exception.SETException;
import com.guohuai.component.util.*;
import com.guohuai.payadapter.component.CallBackEnum;
import com.guohuai.payadapter.redeem.CallBackDao;
import com.guohuai.payadapter.redeem.CallBackInfo;
import com.guohuai.settlement.api.SettlementSdk;
import com.guohuai.settlement.api.response.BaseResponse;
import com.guohuai.settlement.api.response.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: AccountTransferService
 * @Description: 账户转账
 * @author CHENDONGHUI
 * @date 2017年6月16日 下午 15:36:25
 */
@Slf4j
@Service
@Transactional
public class AccountTransferService {

	@Autowired
	private AccOrderDao accOrderDao;
	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private UserInfoService userInfoService;
	@Autowired
	private AccountTradeService accountTradeService;
	@Autowired
	private AccOrderService accOrderService;
	@Autowired
	private TransService transService;
	@Autowired
	private AccountInfoService accountInfoService;
	@Autowired
	private SettlementSdk settlementSdk;
	@Autowired
	private CallBackDao callbackDao;
	
	@Autowired
	private EntityManager entityManager;
	
	@Value("${needRechargeFrozenAccount:N}")
	private String needRechargeFrozenAccount;

    /**
     * 账户之间转账
     * 投资人基本户和发和发行人可用金之间转账
     * @param accountTransRequest
     * @return
     */

	public AccountTransResponse transfer(AccountTransRequest accountTransRequest){
		BaseResponse resp = new BaseResponse();
		log.info("账户交易:请求参数[" + JSONObject.toJSONString(accountTransRequest) + "]");
        AccountTransResponse accountTransResponse = new AccountTransResponse();
        BeanUtils.copyProperties(accountTransRequest, accountTransResponse);
		BigDecimal availableBalance = BigDecimal.ZERO;//发行人可用金户金额
		AccountInfoEntity availableAccount = null;//发行人可用金户
		BigDecimal basicBalance = BigDecimal.ZERO;//投资人基本户金额
		AccountInfoEntity basicAccount = null;//投资人基本户
		String publisherUserOid = accountTransRequest.getPublisherUserOid();
		String userOid = accountTransRequest.getUserOid();
		BigDecimal orderBalance = accountTransRequest.getBalance();
		if(!StringUtil.isEmpty(publisherUserOid)){
			//查询发行人是否存在
			UserInfoEntity userInfo = userInfoService.getAccountUserByUserOid(publisherUserOid);
			if (userInfo != null) {
				//查询发行人可用金账户
				availableAccount = accountInfoDao.findAccountByAccountTypeAndUserOid(
						AccountTypeEnum.AVAILABLE_AMOUNT.getCode(), publisherUserOid);
				if(availableAccount != null){
					availableBalance = availableAccount.getBalance();
					log.info("发行人{}可用金账户余额：{}", publisherUserOid, availableBalance);
				}else{
					log.error("发行人{}可用金账户不存在!",publisherUserOid);
					accountTransResponse.setReturnCode(Constant.ACCOUNTNOTEXISTS);
					accountTransResponse.setErrorMessage("发行人可用金账户不存在");
					return accountTransResponse;
				}
			}else{
				accountTransResponse.setReturnCode(Constant.USERNOTEXISTS);
				accountTransResponse.setErrorMessage("发行人不存在!");
				log.error("发行人不存在![publisherUserOid=" + publisherUserOid + "]");
				return accountTransResponse;
			}
		}else{
			//发行人userOid不能为空
			accountTransResponse.setReturnCode(Constant.REQUEST_USEROID_IS_NULL);
			accountTransResponse.setErrorMessage("发行人userOid不能为空");
			log.info("发行人userOid不能为空!");
			return accountTransResponse;
		}
		if(!StringUtil.isEmpty(userOid)){
			//查询投资人是否存在
			UserInfoEntity userInfo = userInfoService.getAccountUserByUserOid(userOid);
			if(userInfo != null){
				//查询投资人基本账户
				basicAccount = accountInfoDao.findAccountByAccountTypeAndUserOid(
						AccountTypeEnum.BASICER.getCode(), userOid);
				if(basicAccount != null){
					basicBalance = basicAccount.getBalance();
					log.info("投资人{}基本账户余额：{}", userOid, basicBalance);
				}else{
					log.error("投资人{}基本账户不存在!",userOid);
					accountTransResponse.setReturnCode(Constant.ACCOUNTNOTEXISTS);
					accountTransResponse.setErrorMessage("投资人基本账户不存在");
					return accountTransResponse;
				}
			}else{
				accountTransResponse.setReturnCode(Constant.USERNOTEXISTS);
				accountTransResponse.setErrorMessage("投资人不存在!");
				log.error("投资人不存在![userOid=" + userOid + "]");
				return accountTransResponse;
			}
		}else{
			//投资人userOid不能为空
			accountTransResponse.setReturnCode(Constant.REQUEST_USEROID_IS_NULL);
			accountTransResponse.setErrorMessage("投资人userOid不能为空");
			log.info("投资人userOid不能为空!");
			return accountTransResponse;
		}

		if(orderBalance != null){
			if(orderBalance.compareTo(BigDecimal.ZERO) < 0){
				accountTransResponse.setReturnCode(Constant.BALANCEERROR);
				accountTransResponse.setErrorMessage("转账金额不能为负数");
				log.info("转账金额不能为负数!");
				return accountTransResponse;
			}
		}else{
			//转账金额不能为空
			accountTransResponse.setReturnCode(Constant.BALANCEERROR);
			accountTransResponse.setErrorMessage("订单金额不能为空");
			log.info("订单金额不能为空!");
			return accountTransResponse;
		}
		// 接收定单
        CreateOrderResponse cResp = accOrderService.acceptOrder(accountTransRequest);
        if (!Constant.SUCCESS.equals(cResp.getReturnCode())) {
        	if(StringUtil.in(cResp.getReturnCode(),Constant.REDEEM_SUCCESSED)){
				resp.setReturnCode(Constant.SUCCESS);
				resp.setErrorMessage(cResp.getErrorMessage());
				return accountTransResponse;
			}
        	log.error("创建订单失败!订单号{}", accountTransRequest.getOrderNo());
			resp.setReturnCode(cResp.getReturnCode());
			resp.setErrorMessage(cResp.getErrorMessage());
			return accountTransResponse;
        }
        AccOrderEntity accOrderEntity = null;
        accOrderEntity = accOrderDao.findByOrderNo(cResp.getOrderNo());
        if(accOrderEntity == null){
        	log.error("创建订单失败!订单号{}", accountTransRequest.getOrderNo());
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("系统异常");
			return accountTransResponse;
        }
		//根据订单类型判断转账类型
		if(OrderTypeEnum.APPLY.getCode().equals(accOrderEntity.getOrderType())){
			resp = investorBasicAccountTransferToPublisherAvailableAccount(accOrderEntity, 
					publisherUserOid, userOid, orderBalance, availableBalance, availableAccount, 
					basicBalance, basicAccount);
			
		}else if(OrderTypeEnum.REDEEM.getCode().equals(accOrderEntity.getOrderType())){
			resp = publisherAvailableAccountTransferToInvestorBasicAccount(accOrderEntity, 
					publisherUserOid, userOid, orderBalance, availableBalance, availableAccount, 
					basicBalance, basicAccount);
			
		}else{
			accountTransResponse.setReturnCode(Constant.ORDERTYPENOTEXISTS);
			accountTransResponse.setErrorMessage("交易类型不支持");
			accountTransResponse.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
			accOrderEntity.setOrderDesc("交易类型不支持");
			log.info("交易类型不支持!");
		}
		if(Constant.SUCCESS.equals(resp.getReturnCode())){
			accountTransResponse.setReturnCode(Constant.SUCCESS);
			accountTransResponse.setErrorMessage("成功");
			//修改订单状态
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_SUCCESS);
			accOrderEntity.setOrderDesc("交易成功");
			accountTransResponse.setOrderStatus(AccOrderEntity.ORDERSTATUS_SUCCESS);
			log.info("转账交易成功!");
		}else{
			accountTransResponse.setReturnCode(resp.getReturnCode());
			accountTransResponse.setErrorMessage(resp.getErrorMessage());
			//修改订单状态
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
			accOrderEntity.setOrderDesc(resp.getErrorMessage());
			accountTransResponse.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
			log.info("转账交易失败!");
		}
		accOrderDao.saveAndFlush(accOrderEntity);
		return accountTransResponse;
	}
	/**
	 * 转账
	 * 发行人可用金户到投资人基本户
	 * @param publisherUserOid
	 * @param userOid
	 * @param orderBalance
	 * @return
	 */
	@Transactional
	public BaseResponse publisherAvailableAccountTransferToInvestorBasicAccount(
			AccOrderEntity accOrderEntity, String publisherUserOid, String userOid, 
			BigDecimal orderBalance, BigDecimal availableBalance, AccountInfoEntity availableAccount, 
			BigDecimal basicBalance, AccountInfoEntity basicAccount){
		log.info("发行人可用金户到投资人基本户,金额{}", orderBalance);
		BaseResponse resp = new BaseResponse();
		CreateTransResponse transResp = null;
		//操作账户
		log.info("交易账户 AccountInfoEntity={},锁定账户", JSONObject.toJSON(availableAccount));
		availableAccount = accountInfoDao.findByOidForUpdate(availableAccount.getOid());
		entityManager.refresh(availableAccount);
		
		int result = 0;
		result = accountInfoDao.subtractBalance(orderBalance, availableAccount.getAccountNo());
		log.info("发行人转账扣除发行人可用金户余额结果={}", result);
		if (result == 0) {
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("发行人转账扣除发行人可用金户余额失败，发行人可用金户余额不足");
			log.info("发行人转账扣除发行人可用金户余额失败，发行人{}可用金户{}余额不足", publisherUserOid, 
					availableAccount.getAccountNo());
			return resp;
		}
		BigDecimal afterBalance = availableAccount.getBalance().subtract(orderBalance);
		log.info("发行人转账记录发行人可用金户明细 accountNo={}，orderBalance={}，orderNo={}， userOid={}，publisherUserOid={}",
				availableAccount.getAccountNo(), accOrderEntity.getOrderNo(), userOid, publisherUserOid);
		String orderDesc = "发行人转账记录发行人可用金户明细";
		
		accOrderEntity.setUserOid(publisherUserOid);
		accOrderEntity.setUserType(UserTypeEnum.PUBLISHER.getCode());
		
		transResp = this.addTrans(accOrderEntity, basicAccount.getAccountNo(), 
				availableAccount.getAccountNo(), afterBalance, orderDesc, "02", 
				AccountTypeEnum.AVAILABLE_AMOUNT.getCode(), availableAccount.getAccountNo());
		//增加投资人基本户余额
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			log.info("发行人转账增加投资人基本户余额 accountNo={},orderBalance={}", basicAccount.getAccountNo(), 
					orderBalance);
			entityManager.refresh(basicAccount);
			
			accountInfoDao.addBalance(orderBalance, basicAccount.getAccountNo());
			afterBalance = basicAccount.getBalance().add(orderBalance);
			// 记录投资人基本户的明细及余额更新
			log.info("发行人转账记录投资人基本户交易明细accountNo={},orderBalance={}",basicAccount.getAccountNo(), 
					orderBalance);
			orderDesc = "发行人转账记录投资人基本户明细";
			
			accOrderEntity.setUserOid(userOid);
			accOrderEntity.setUserType(UserTypeEnum.INVESTOR.getCode());
			transResp = this.addTrans(accOrderEntity, basicAccount.getAccountNo(), 
					availableAccount.getAccountNo(), afterBalance, orderDesc, "01", 
					AccountTypeEnum.BASICER.getCode(), basicAccount.getAccountNo());
		}
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		}else{
			resp.setReturnCode(transResp.getReturnCode());
			resp.setErrorMessage(transResp.getErrorMessage());
			log.info("发行人转账交易失败,resp={}", JSONObject.toJSON(resp));
		}
		return resp;
	}
	
	/**
	 * 转帐
	 * 投资人基本户到发行人可用金户
	 * @param publisherUserOid
	 * @param userOid
	 * @param orderBalance
	 * @return
	 */
	@Transactional
	public BaseResponse investorBasicAccountTransferToPublisherAvailableAccount(
			AccOrderEntity accOrderEntity, String publisherUserOid, String userOid, 
			BigDecimal orderBalance, BigDecimal availableBalance, AccountInfoEntity availableAccount, 
			BigDecimal basicBalance, AccountInfoEntity basicAccount){
		log.info("投资人基本户到发行人可用金户,金额{}", orderBalance);
		BaseResponse resp = new BaseResponse();
		CreateTransResponse transResp = null;
		//操作账户
		log.info("交易账户 AccountInfoEntity={},锁定账户", JSONObject.toJSON(basicAccount));
		basicAccount = accountInfoDao.findByOidForUpdate(basicAccount.getOid());
		entityManager.refresh(basicAccount);
		int result = 0;
		result = accountInfoDao.subtractBalance(orderBalance, basicAccount.getAccountNo());
		log.info("投资人转账扣投资人基本户余额结果={}", result);
		if (result == 0) {
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("投资人转账扣投资人基本户余额失败，投资人可基本户余额不足");
			log.info("投资人转账扣投资人基本户余额失败，投资人{}基本户{}余额不足", userOid, 
					basicAccount.getAccountNo());
			return resp;
		}
		
		AccountInfoEntity userRechargeFrozenAccount = null;
		//断定是否需要充值冻结户 如果不需要，查询时充值冰结户设置为0
		if ("Y".equals(needRechargeFrozenAccount)) {
			//查询用户充值冻结户
			userRechargeFrozenAccount = accountInfoDao.findByUserOidAndAccountTyp(userOid, AccountTypeEnum.RECHARGEFROZEN.getCode());
		}
		// 用户充值冻结户 -balance  (新注册用户使用红包投资，无此账户)
		if (userRechargeFrozenAccount != null) {
			userRechargeFrozenAccount=accountInfoDao.findByOidForUpdate(userRechargeFrozenAccount.getOid());
			entityManager.refresh(userRechargeFrozenAccount);
			// 如果用户的可用余额小于申购的金额，则充值冻结户余额为0
			accountInfoDao.subtractBalanceLowerLimitZero(orderBalance, userRechargeFrozenAccount.getAccountNo());
			//20170810记录扣除充值冻结户流水
			String orderDesc = "投资人转账记录投资人充值冻结户明细";
			BigDecimal afterBalance = BigDecimal.ZERO;
			if(userRechargeFrozenAccount.getBalance().compareTo(orderBalance)>=0){
				afterBalance = userRechargeFrozenAccount.getBalance().subtract(orderBalance);
			}
			
			this.addTrans(accOrderEntity, availableAccount.getAccountNo(), 
					userRechargeFrozenAccount.getAccountNo(), afterBalance, orderDesc, "02", 
					AccountTypeEnum.RECHARGEFROZEN.getCode(), userRechargeFrozenAccount.getAccountNo());
		}

		BigDecimal afterBalance = basicAccount.getBalance().subtract(orderBalance);
		log.info("投资人转账记录投资人基本户明细 accountNo={}，orderBalance={}，orderNo={}， userOid={}，publisherUserOid={}",
				basicAccount.getAccountNo(), orderBalance, accOrderEntity.getOrderNo(), userOid, publisherUserOid);
		String orderDesc = "投资人转账记录投资人基本户明细";
		transResp = this.addTrans(accOrderEntity, availableAccount.getAccountNo(), 
				basicAccount.getAccountNo(), afterBalance, orderDesc, "02" , 
				AccountTypeEnum.BASICER.getCode(), basicAccount.getAccountNo());
		//增加发行人可用金户余额
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			log.info("投资人转账增加发行人可用金户余额 accountNo={},orderBalance={}", availableAccount.getAccountNo(), 
					orderBalance);
			availableAccount=accountInfoDao.findByOidForUpdate(availableAccount.getOid());
			entityManager.refresh(availableAccount);
			
			accountInfoDao.addBalance(orderBalance, availableAccount.getAccountNo());
			afterBalance = availableAccount.getBalance().add(orderBalance);
			// 记录投资人基本户的明细及余额更新
			log.info("投资人转账记录发行人可用金户交易明细accountNo={},orderBalance={}",availableAccount.getAccountNo(), 
					orderBalance);
			orderDesc = "投资人转账记录发行人可用金户明细";
			accOrderEntity.setUserOid(accOrderEntity.getPublisherUserOid());
			accOrderEntity.setUserType(UserTypeEnum.PUBLISHER.getCode());
			transResp = this.addTrans(accOrderEntity, availableAccount.getAccountNo(), 
					basicAccount.getAccountNo(), afterBalance, orderDesc, "01", 
					AccountTypeEnum.AVAILABLE_AMOUNT.getCode(), availableAccount.getAccountNo());
		}
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		}else{
			resp.setReturnCode(transResp.getReturnCode());
			resp.setErrorMessage(transResp.getErrorMessage());
			log.info("投资人转账交易失败,resp={}", JSONObject.toJSON(resp));
		}
		return resp;
	}
	
	
	public void batchTransfer(List<AccOrderEntity> redeemList, String batchTime) {
		log.info("转账订单保存完成，批量转账，共{}条",redeemList.size());
		if(redeemList != null && redeemList.size()>0){
			
		}else{
			log.info("无转账订单，无需操作用户账户单");
		}
	}

	/**
	 * 转账
	 * 发行人可用金户到发行人资金冻结户
	 * @param accOrderEntity
	 * @param publisherUserOid
	 * @param orderBalance
	 * @param availableBalance
	 * @param availableAccount
	 * @param frozenBalance
	 * @param frozenAccount
	 * @return
	 */
	@Transactional
	public BaseResponse publisherAvailableAccountTransferToPublisherFrozenAccount(
			AccOrderEntity accOrderEntity, String publisherUserOid, 
			BigDecimal orderBalance, BigDecimal availableBalance, AccountInfoEntity availableAccount, 
			BigDecimal frozenBalance, AccountInfoEntity frozenAccount){
		log.info("发行人可用金户到发行人资金冻结户,金额{}", orderBalance);
		BaseResponse resp = new BaseResponse();
		CreateTransResponse transResp = null;
		//操作账户
		log.info("交易账户 AccountInfoEntity={},锁定账户", JSONObject.toJSON(availableAccount));
		availableAccount = accountInfoDao.findByOidForUpdate(availableAccount.getOid());
		entityManager.refresh(availableAccount);
		int result = 0;
		result = accountInfoDao.subtractBalance(orderBalance, availableAccount.getAccountNo());
		log.info("发行人转账扣除发行人可用金户余额结果={}", result);
		if (result == 0) {
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("发行人转账扣除发行人可用金户余额失败，发行人可用金户余额不足");
			log.info("发行人转账扣除发行人可用金户余额失败，发行人{}可用金户{}余额不足", publisherUserOid, 
					availableAccount.getAccountNo());
			return resp;
		}
		BigDecimal afterBalance = availableAccount.getBalance().subtract(orderBalance);
		log.info("发行人转账记录发行人可用金户明细 accountNo={}，orderBalance={}，orderNo={}，publisherUserOid={}",
				availableAccount.getAccountNo(), accOrderEntity.getOrderNo(), publisherUserOid);
		String orderDesc = "发行人转账记录发行人可用金户明细";
		accOrderEntity.setUserOid(accOrderEntity.getPublisherUserOid());
		accOrderEntity.setUserType(UserTypeEnum.PUBLISHER.getCode());
		transResp = this.addTrans(accOrderEntity, frozenAccount.getAccountNo(), 
				availableAccount.getAccountNo(), afterBalance, orderDesc, "02" , 
				AccountTypeEnum.AVAILABLE_AMOUNT.getCode(), availableAccount.getAccountNo());
		//增加发行人资金冻结户余额
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			log.info("发行人转账增加发行人资金冻结户余额 accountNo={},orderBalance={}", frozenAccount.getAccountNo(), 
					orderBalance);
			frozenAccount=accountInfoDao.findByOidForUpdate(frozenAccount.getOid());
			entityManager.refresh(frozenAccount);
			accountInfoDao.addBalance(orderBalance, frozenAccount.getAccountNo());
			afterBalance = frozenAccount.getBalance().add(orderBalance);
			// 记录发行人资金冻结户的明细及余额更新
			log.info("发行人转账记录发行人资金冻结户交易明细accountNo={},orderBalance={}",frozenAccount.getAccountNo(), 
					orderBalance);
			orderDesc = "发行人转账记录发行人资金冻结户明细";
			transResp = this.addTrans(accOrderEntity, frozenAccount.getAccountNo(), 
					availableAccount.getAccountNo(), afterBalance, orderDesc, "01", 
					AccountTypeEnum.REDEEMFROZEN.getCode(), frozenAccount.getAccountNo());
		}
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		}else{
			resp.setReturnCode(transResp.getReturnCode());
			resp.setErrorMessage(transResp.getErrorMessage());
			log.info("发行人转账交易失败,resp={}", JSONObject.toJSON(resp));
		}
		return resp;
	}
	
	
	/**
	 * 转帐
	 * 发行人归集户到发行人资金冻结户
	 * @param accOrderEntity
	 * @param publisherUserOid
	 * @param orderBalance
	 * @param settlementBalance
	 * @param settlementAccount
	 * @param frozenBalance
	 * @param frozenAccount
	 * @return
	 */
	@Transactional
	public BaseResponse publisherSettlementAccountTransferToPublisherFrozenAccount(
			AccOrderEntity accOrderEntity, String publisherUserOid, 
			BigDecimal orderBalance, BigDecimal settlementBalance, AccountInfoEntity settlementAccount, 
			BigDecimal frozenBalance, AccountInfoEntity frozenAccount){
		log.info("发行人归集户到发行人资金冻结户,金额{}", orderBalance);
		BaseResponse resp = new BaseResponse();
		CreateTransResponse transResp = null;
		//操作账户
		log.info("交易账户 AccountInfoEntity={},锁定账户", JSONObject.toJSON(settlementAccount));
		settlementAccount = accountInfoDao.findByOidForUpdate(settlementAccount.getOid());
		entityManager.refresh(settlementAccount);
		
		int result = 0;
		result = accountInfoDao.subtractBalance(orderBalance, settlementAccount.getAccountNo());
		log.info("发行人转账扣除发行人归集户余额结果={}", result);
		if (result == 0) {
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("扣除发行人归集户余额失败，发行人归集户余额不足，请稍后重试");
			log.info("扣除发行人归集户余额失败，发行人{}归集户{}余额不足", publisherUserOid, 
					settlementAccount.getAccountNo());
			return resp;
		}
		BigDecimal afterBalance = settlementAccount.getBalance().subtract(orderBalance);
		log.info("发行人转账记录发行人归集户明细 accountNo={}，orderBalance={}，orderNo={}， publisherUserOid={}",
				settlementAccount.getAccountNo(), orderBalance, accOrderEntity.getOrderNo(), publisherUserOid);
		String orderDesc = "发行人转账记录发行人归集户明细";
		accOrderEntity.setBalance(orderBalance);
		transResp = this.addTrans(accOrderEntity, frozenAccount.getAccountNo(), 
				settlementAccount.getAccountNo(), afterBalance, orderDesc, "02", 
				AccountTypeEnum.COLLECTION_SETTLEMENT.getCode(), settlementAccount.getAccountNo());
		//增加发行人资金冻结户余额
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			log.info("发行人转账增加发行人资金冻结户余额 accountNo={},orderBalance={}", frozenAccount.getAccountNo(), 
					orderBalance);
			frozenAccount=accountInfoDao.findByOidForUpdate(frozenAccount.getOid());
			entityManager.refresh(frozenAccount);
			
			accountInfoDao.addBalance(orderBalance, frozenAccount.getAccountNo());
			afterBalance = frozenAccount.getBalance().add(orderBalance);
			// 记录发行人资金冻结户的明细及余额更新
			log.info("发行人转账记录发行人资金冻结户交易明细accountNo={},orderBalance={}",frozenAccount.getAccountNo(), 
					orderBalance);
			orderDesc = "发行人转账记录发行人资金冻结户明细";
			transResp = this.addTrans(accOrderEntity, frozenAccount.getAccountNo(), 
					settlementAccount.getAccountNo(), afterBalance, orderDesc, "01", 
					AccountTypeEnum.REDEEMFROZEN.getCode(), frozenAccount.getAccountNo());
		}
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		}else{
			resp.setReturnCode(transResp.getReturnCode());
			resp.setErrorMessage(transResp.getErrorMessage());
			log.info("发行人转账交易失败,resp={}", JSONObject.toJSON(resp));
		}
		return resp;
	}

    /**
     *  转账
     * 发行人归集户到发行人可用金户
     * @param accOrderEntity
     * @param publisherUserOid
     * @param orderBalance
     * @param settlementBalance
     * @param settlementAccount
     * @param availableBalance
     * @param availableAccount
     * @return
     */

	@Transactional
	public BaseResponse publisherSettlementAccountTransferToPublisherAvailableAccount(
			AccOrderEntity accOrderEntity, String publisherUserOid, 
			BigDecimal orderBalance, BigDecimal settlementBalance, AccountInfoEntity settlementAccount, 
			BigDecimal availableBalance, AccountInfoEntity availableAccount){
		log.info("发行人归集户到发行人可用金户,金额{}", orderBalance);
		BaseResponse resp = new BaseResponse();
		CreateTransResponse transResp = null;
		//操作账户
		log.info("交易账户 AccountInfoEntity={},锁定账户", JSONObject.toJSON(settlementAccount));
		settlementAccount = accountInfoDao.findByOidForUpdate(settlementAccount.getOid());
		entityManager.refresh(settlementAccount);
		
		int result = 0;
		result = accountInfoDao.subtractBalance(orderBalance, settlementAccount.getAccountNo());
		log.info("发行人转账扣除发行人归集户余额结果={}", result);
		if (result == 0) {
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("扣除发行人归集户余额失败，发行人归集户余额不足，请稍后重试");
			log.info("扣除发行人归集户余额失败，发行人{}归集户{}余额不足", publisherUserOid, 
					settlementAccount.getAccountNo());
			return resp;
		}
		BigDecimal afterBalance = settlementAccount.getBalance().subtract(orderBalance);
		log.info("发行人转账记录发行人归集户明细 accountNo={}，orderNo={}，orderBalance={}， publisherUserOid={}",
				settlementAccount.getAccountNo(), accOrderEntity.getOrderNo(), orderBalance, publisherUserOid);
		String orderDesc = "发行人转账记录发行人归集户明细";
		transResp = this.addTrans(accOrderEntity, availableAccount.getAccountNo(), 
				settlementAccount.getAccountNo(), afterBalance, orderDesc, "02", 
				AccountTypeEnum.COLLECTION_SETTLEMENT.getCode(), settlementAccount.getAccountNo());
		//增加发行人可用金户余额
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			log.info("发行人转账增加发行人可用金户余额 accountNo={},orderBalance={}", availableAccount.getAccountNo(), 
					orderBalance);
			availableAccount=accountInfoDao.findByOidForUpdate(availableAccount.getOid());
			entityManager.refresh(availableAccount);
			
			accountInfoDao.addBalance(orderBalance, availableAccount.getAccountNo());
			afterBalance = availableAccount.getBalance().add(orderBalance);
			// 记录增加发行人可用金户的明细及余额更新
			log.info("发行人转账记录发行人可用金户交易明细accountNo={},orderBalance={}",availableAccount.getAccountNo(), 
					orderBalance);
			orderDesc = "发行人转账记录发行人可用金户明细";
			transResp = this.addTrans(accOrderEntity, availableAccount.getAccountNo(), 
					settlementAccount.getAccountNo(), afterBalance, orderDesc, "01", 
					AccountTypeEnum.AVAILABLE_AMOUNT.getCode(), availableAccount.getAccountNo());
		}
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		}else{
			resp.setReturnCode(transResp.getReturnCode());
			resp.setErrorMessage(transResp.getErrorMessage());
			log.info("发行人转账交易失败,resp={}", JSONObject.toJSON(resp));
		}
		return resp;
	}
	

	/**
	 * 批量赎回保存订单、操作发行人账户
	 * 1、扣除发行人归集户批量赎回订单金额
	 * 2、增加发行人资金冻结金额
	 * 3、保存批订单
	 * @param orderList
	 * @param requestNo
	 * @param publisherUserOid
	 * @param systemSource
	 * @return
	 */
	@Transactional
	public CreateBatchAccountOrderRes saveBatchOrderAndFrozenAccount(List<AccountOrderDto> orderList, 
			String requestNo, String publisherUserOid, String systemSource) {
		CreateBatchAccountOrderRes resp = new CreateBatchAccountOrderRes();
		List<AccOrderEntity> accOrderEntityList = new ArrayList<AccOrderEntity>();
		Timestamp time = new Timestamp(System.currentTimeMillis());
		BigDecimal orderBalance = BigDecimal.ZERO;//订单总金额
		for(AccountOrderDto dto : orderList){
			AccOrderEntity orderEntity = new AccOrderEntity();
			orderEntity.setRequestNo(requestNo);
			orderEntity.setPublisherUserOid(publisherUserOid);
			orderEntity.setSystemSource(systemSource);
			orderEntity.setOrderNo(dto.getOrderNo());
			orderEntity.setUserOid(dto.getUserOid());
			orderEntity.setOrderType(dto.getOrderType());
			orderEntity.setBalance(dto.getBalance());
			orderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_INIT);
			orderEntity.setRemark(dto.getRemark());
			orderEntity.setOrderDesc(dto.getOrderDesc());
			//20170301新增业务系统订单创建时间
			String orderCreatTime = dto.getSubmitTime();//YYYY-MM-DD HH:mm:ss
			if (orderCreatTime != null) {
				orderEntity.setSubmitTime(Timestamp.valueOf(orderCreatTime));
			}
			orderEntity.setVoucher(dto.getVoucher());
			orderEntity.setFee(dto.getFee());
			orderEntity.setReceiveTime(time);
			orderEntity.setUpdateTime(time);
			orderEntity.setCreateTime(time);
			accOrderEntityList.add(orderEntity);
			orderBalance = orderBalance.add(dto.getBalance());
		}
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");
		accOrderDao.save(accOrderEntityList);
		resp.setAccOrderEntityList(accOrderEntityList);
		return resp;
	}

    /**
     * 组装交易流水,调用原有方法
     * @param accOrderEntity
     * @param inputAccountNo
     * @param outpuptAccountNo
     * @param afterBalance
     * @param orderDesc
     * @param direction
     * @param accountType
     * @param accountNo
     * @return
     */

	public CreateTransResponse addTrans(AccOrderEntity accOrderEntity, String inputAccountNo, 
			String outpuptAccountNo, BigDecimal afterBalance, String orderDesc, String direction, String accountType, String accountNo) {
		CreateTransRequest transRequest = new CreateTransRequest();
		transRequest.setUserOid(accOrderEntity.getUserOid());
		transRequest.setAccountOid(accountNo);
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
	
	/**
	 * 批量转账冻结
	 * 将转账批次的订单总额预冻结，用于批量订单的转账
	 * @param req 请求冻结参数
	 * @return 冻结结果
	 */
	public AccountTransferResponse batchTransferFrozen(AccountBatchTransferFrozenRequest req) {
		AccountTransferResponse resp = new AccountTransferResponse();
		String userOid = req.getOutputUserOid();
		resp.setInputUserOid(userOid);
		resp.setOutputUserOid(userOid);
		resp.setRequestNo(req.getRequestNo());
		//校验参数
		resp = this.checkTransferParam(req, null, null, null, resp);
		if(Constant.FAIL.equals(resp.getReturnCode())){
			log.info("批量转账冻结参数校验失败：{}",resp.getErrorMessage());
			return resp;
		}
		//判断用户是否存在
		UserInfoEntity userInfo = userInfoService.getAccountUserByUserOid(userOid);
		if(userInfo == null){
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("转账人不存在");
			return resp;
		}
		//根据订单号判断出账账户及入账账户
		resp = this.getTransferAccountNo(resp, userInfo, req.getOrderType());
		if(Constant.FAIL.equals(resp.getReturnCode())){
			log.info("批量转账冻结获取账户信息失败：{}",resp.getErrorMessage());
			return resp;
		}
		//记录转账订单并处理转账
		resp = this.receiveBatchTransferFrozenOrder(req, resp);
		return resp;
	}
	
	/**
	 * 接收批量转账冻结订单并处理转账
	 * @param req 请求参数
	 * @param resp 已组装返参
	 * @return 处理订单结果
	 */
	@Transactional
	private AccountTransferResponse receiveBatchTransferFrozenOrder(
			AccountBatchTransferFrozenRequest req, AccountTransferResponse resp) {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("交易成功");
		//保存订单
		CreateOrderRequest createOrderRequest = new CreateOrderRequest();
		createOrderRequest.setBalance(req.getBalance());
		createOrderRequest.setFee(BigDecimal.ZERO);
		createOrderRequest.setInputAccountNo(resp.getInputAccountNo());
		createOrderRequest.setOrderCreatTime(req.getSubmitTime());
		createOrderRequest.setOrderDesc(req.getOrderDesc());
		createOrderRequest.setOrderNo(req.getOrderNo());
		createOrderRequest.setOrderType(req.getOrderType());
		createOrderRequest.setOutpuptAccountNo(resp.getOutputAccountNo());
		createOrderRequest.setPublisherUserOid(resp.getInputUserOid());
		createOrderRequest.setRemark(req.getRemark());
		createOrderRequest.setRequestNo(req.getRequestNo());
		createOrderRequest.setSystemSource(req.getSystemSource());
		createOrderRequest.setUserOid(resp.getOutputUserOid());
		createOrderRequest.setUserType(req.getUserType());
		createOrderRequest.setVoucher(BigDecimal.ZERO);
		CreateOrderResponse createOrderResponse = accOrderService.addAccountOrder(createOrderRequest);
		if(!Constant.SUCCESS.equals(createOrderResponse.getReturnCode())){
			resp.setReturnCode(createOrderResponse.getReturnCode());
			resp.setErrorMessage(createOrderResponse.getErrorMessage());
		}
		AccOrderEntity accOrderEntity = null;
        accOrderEntity = accOrderDao.findByOrderNo(req.getOrderNo());
        if(accOrderEntity == null){
        	log.error("创建订单失败!订单号{}", req.getOrderNo());
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("系统异常");
			return resp;
        }
		//处理转账
		String outputAccountNo = resp.getOutputAccountNo();
		String inputAccountNo = resp.getInputAccountNo();
		BigDecimal transferBalance = req.getBalance();
		String orderType = req.getOrderType();
		String orderNo = req.getOrderNo();
		String requestNo = req.getRequestNo();
		BaseResponse baseResp = this.transfer(outputAccountNo, inputAccountNo, transferBalance, orderType, orderNo, requestNo);
		//更新订单状态
		if(Constant.SUCCESS.endsWith(baseResp.getReturnCode())){
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_SUCCESS);
		}else{
			resp.setReturnCode(baseResp.getReturnCode());
			resp.setErrorMessage(baseResp.getErrorMessage());
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
		}
		accOrderEntity.setUpdateTime(now);
		accOrderDao.saveAndFlush(accOrderEntity);
		return resp;
	}
	
	/**
	 * 单笔转账
	 * 将用户A的账户A转入用户B的账户B中
	 * @param req 单笔转账参数
	 * @return 转账结果
	 */
	public AccountTransferResponse singleTransfer(AccountSingleTransferRequest req) {
		AccountTransferResponse resp = new AccountTransferResponse();
		String outputUserOid = req.getOutputUserOid();
		String inputUserOid = req.getInputUserOid();
		resp.setInputUserOid(inputUserOid);
		resp.setOutputUserOid(outputUserOid);
		resp.setRequestNo(req.getRequestNo());
		//校验参数
		resp = this.checkTransferParam(null, req, null, null, resp);
		if(Constant.FAIL.equals(resp.getReturnCode())){
			log.info("单笔转账参数校验失败：{}",resp.getErrorMessage());
			return resp;
		}
		//判断用户是否存在
		UserInfoEntity outputUserInfo = userInfoService.getAccountUserByUserOid(outputUserOid);
		if(outputUserInfo == null){
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("转账人不存在");
			return resp;
		}
		UserInfoEntity inputUserInfo = userInfoService.getAccountUserByUserOid(inputUserOid);
		if(inputUserInfo == null){
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("转入人不存在");
			return resp;
		}
		//根据订单号判断出账账户及入账账户
		resp = this.getTransferAccountNo(resp, outputUserInfo, req.getOrderType());
		if(Constant.FAIL.equals(resp.getReturnCode())){
			log.info("单笔转账获取账户信息失败：{}",resp.getErrorMessage());
			return resp;
		}
		//记录转账订单并处理转账
		resp = this.receiveSingleTransferOrder(req, resp);
		return resp;
	}
	
	/**
	 * 接收单笔转账订单并处理转账
	 * @param req 请求参数
	 * @param resp 已组装返参
	 * @return 处理订单结果
	 */
	@Transactional
	private AccountTransferResponse receiveSingleTransferOrder(
			AccountSingleTransferRequest req, AccountTransferResponse resp) {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("交易成功");
		CreateOrderRequest createOrderRequest = new CreateOrderRequest();
		createOrderRequest.setBalance(req.getBalance());
		createOrderRequest.setFee(req.getFee());
		createOrderRequest.setInputAccountNo(resp.getInputAccountNo());
		createOrderRequest.setOrderCreatTime(req.getSubmitTime());
		createOrderRequest.setOrderDesc(req.getOrderDesc());
		createOrderRequest.setOrderNo(req.getOrderNo());
		createOrderRequest.setOrderType(req.getOrderType());
		createOrderRequest.setOutpuptAccountNo(resp.getOutputAccountNo());
		createOrderRequest.setPublisherUserOid(resp.getOutputUserOid());
		createOrderRequest.setRemark(req.getRemark());
		createOrderRequest.setRequestNo(req.getRequestNo());
		createOrderRequest.setSystemSource(req.getSystemSource());
		createOrderRequest.setUserOid(resp.getInputUserOid());
		createOrderRequest.setUserType(req.getUserType());
		createOrderRequest.setVoucher(req.getVoucher());
		CreateOrderResponse createOrderResponse = accOrderService.addAccountOrder(createOrderRequest);
		if(!Constant.SUCCESS.equals(createOrderResponse.getReturnCode())){
			resp.setReturnCode(createOrderResponse.getReturnCode());
			resp.setErrorMessage(createOrderResponse.getErrorMessage());
		}
		AccOrderEntity accOrderEntity = null;
        accOrderEntity = accOrderDao.findByOrderNo(req.getOrderNo());
        if(accOrderEntity == null){
        	log.error("创建订单失败!订单号{}", req.getOrderNo());
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("系统异常");
			return resp;
        }
		//处理转账
		String outputAccountNo = resp.getOutputAccountNo();
		String inputAccountNo = resp.getInputAccountNo();
		BigDecimal transferBalance = req.getBalance();
		String orderType = req.getOrderType();
		String orderNo = req.getOrderNo();
		String requestNo = req.getRequestNo();
		BaseResponse baseResp = this.transfer(outputAccountNo, inputAccountNo, transferBalance, orderType, orderNo, requestNo);
		//更新订单状态
		if(Constant.SUCCESS.endsWith(baseResp.getReturnCode())){
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_SUCCESS);
		}else{
			resp.setReturnCode(baseResp.getReturnCode());
			resp.setErrorMessage(baseResp.getErrorMessage());
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
		}
		accOrderEntity.setUpdateTime(now);
		accOrderDao.saveAndFlush(accOrderEntity);
		return resp;
	}
	
	/**
	 * 解冻
	 * 用户转账前预冻结的金额不在转账，将冻结的金额转入非冻结账户
	 * @param req 解冻请求参数
	 * @return 解冻结果
	 */
	public AccountTransferResponse unFrozenTransfer(AccountUnFrozenTransferRequest req) {
		AccountTransferResponse resp = new AccountTransferResponse();
		String userOid = req.getOutputUserOid();
		resp.setInputUserOid(userOid);
		resp.setOutputUserOid(userOid);
		resp.setRequestNo(req.getRequestNo());
		//校验参数
		resp = this.checkTransferParam(null, null, req, null, resp);
		if(Constant.FAIL.equals(resp.getReturnCode())){
			log.info("解冻参数校验失败：{}",resp.getErrorMessage());
			return resp;
		}
		//判断用户是否存在
		UserInfoEntity userInfo = userInfoService.getAccountUserByUserOid(userOid);
		if(userInfo == null){
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("转账人不存在");
			return resp;
		}
		//根据订单号判断出账账户及入账账户
		resp = this.getTransferAccountNo(resp, userInfo, req.getOrderType());
		if(Constant.FAIL.equals(resp.getReturnCode())){
			log.info("解冻获取账户信息失败：{}",resp.getErrorMessage());
			return resp;
		}
		//记录转账订单
		resp = this.receiveUnFrozenTransferOrder(req, resp);
		return resp;
	}
	
	/**
	 * 接收解冻订单并处理转账
	 * @param req 请求参数
	 * @param resp 已组装返参
	 * @return 处理订单结果
	 */
	@Transactional
	private AccountTransferResponse receiveUnFrozenTransferOrder(
			AccountUnFrozenTransferRequest req, AccountTransferResponse resp) {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("交易成功");
		CreateOrderRequest createOrderRequest = new CreateOrderRequest();
		createOrderRequest.setBalance(req.getBalance());
		createOrderRequest.setFee(BigDecimal.ZERO);
		createOrderRequest.setInputAccountNo(resp.getInputAccountNo());
		createOrderRequest.setOrderCreatTime(req.getSubmitTime());
		createOrderRequest.setOrderDesc(req.getOrderDesc());
		createOrderRequest.setOrderNo(req.getOrderNo());
		createOrderRequest.setOrderType(req.getOrderType());
		createOrderRequest.setOutpuptAccountNo(resp.getOutputAccountNo());
		createOrderRequest.setPublisherUserOid(resp.getInputUserOid());
		createOrderRequest.setRemark(req.getRemark());
		createOrderRequest.setRequestNo(req.getRequestNo());
		createOrderRequest.setSystemSource(req.getSystemSource());
		createOrderRequest.setUserOid(resp.getOutputUserOid());
		createOrderRequest.setUserType(req.getUserType());
		createOrderRequest.setVoucher(BigDecimal.ZERO);
		CreateOrderResponse createOrderResponse = accOrderService.addAccountOrder(createOrderRequest);
		if(!Constant.SUCCESS.equals(createOrderResponse.getReturnCode())){
			resp.setReturnCode(createOrderResponse.getReturnCode());
			resp.setErrorMessage(createOrderResponse.getErrorMessage());
		}
		AccOrderEntity accOrderEntity = null;
        accOrderEntity = accOrderDao.findByOrderNo(req.getOrderNo());
        if(accOrderEntity == null){
        	log.error("创建订单失败!订单号{}", req.getOrderNo());
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("系统异常");
			return resp;
        }
		//处理转账
		String outputAccountNo = resp.getOutputAccountNo();
		String inputAccountNo = resp.getInputAccountNo();
		BigDecimal transferBalance = req.getBalance();
		String orderType = req.getOrderType();
		String orderNo = req.getOrderNo();
		String requestNo = req.getRequestNo();
		BaseResponse baseResp = this.transfer(outputAccountNo, inputAccountNo, transferBalance, orderType, orderNo, requestNo);
		//更新订单状态
		if(Constant.SUCCESS.endsWith(baseResp.getReturnCode())){
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_SUCCESS);
		}else{
			resp.setReturnCode(baseResp.getReturnCode());
			resp.setErrorMessage(baseResp.getErrorMessage());
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
		}
		accOrderEntity.setUpdateTime(now);
		accOrderDao.saveAndFlush(accOrderEntity);
		return resp;
	}
	
	/**
	 * 批量转账
	 * @param req 批量转账订单
	 * @return 转账接收结果
	 */
	public AccountTransferResponse batchTransfer(AccountBatchTransferRequest req) {
		AccountTransferResponse resp = new AccountTransferResponse();
		String userOid = req.getOutputUserOid();
		resp.setInputUserOid(userOid);
		resp.setOutputUserOid(userOid);
		resp.setRequestNo(req.getRequestNo());
		//校验参数
		resp = this.checkTransferParam(null, null, null, req, resp);
		if(Constant.FAIL.equals(resp.getReturnCode())){
			log.info("批量转账参数校验失败：{}",resp.getErrorMessage());
			return resp;
		}
		//判断用户是否存在
		UserInfoEntity userInfo = userInfoService.getAccountUserByUserOid(userOid);
		if(userInfo == null){
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("转账人不存在");
			return resp;
		}
		//根据订单号判断出账账户及入账账户
		resp = this.getTransferAccountNo(resp, userInfo, null);
		if(Constant.FAIL.equals(resp.getReturnCode())){
			log.info("批量转账获取账户信息失败：{}",resp.getErrorMessage());
			return resp;
		}
		//记录转账订单并处理订单转账
		resp = this.receiveBatchTransferOrder(req, resp);
		return resp;
	}
	
	/**
	 * 接收批量转账订单并处理转账
	 * @param req 请求参数
	 * @param resp 已组装返参
	 * @return 处理订单结果
	 */
	@Transactional
	private AccountTransferResponse receiveBatchTransferOrder(
			AccountBatchTransferRequest req, AccountTransferResponse resp) {
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("交易成功");
		//组装账户订单
		CreateBatchAccountOrderRes createBatchAccountOrderRes = new CreateBatchAccountOrderRes();
		createBatchAccountOrderRes = accOrderService.creatBatchAccountOrder(req);
		if(!Constant.SUCCESS.equals(createBatchAccountOrderRes.getReturnCode())){
			resp.setReturnCode(createBatchAccountOrderRes.getReturnCode());
			resp.setErrorMessage(createBatchAccountOrderRes.getErrorMessage());
			return resp;
		}
		log.info("保存批量转账订单共{}条，成功{}条，已存在成功订单{}条", req.getOrderList().size(),
				createBatchAccountOrderRes.getAccOrderEntityList().size(), req.getOrderList().size()-createBatchAccountOrderRes.getAccOrderEntityList().size());
		//异步处理订单
		this.asyncBatchTransfer(resp, createBatchAccountOrderRes.getAccOrderEntityList());
		return resp;
	}
	
	/**
	 * 异步处理批量转账订单
	 * @param resp
	 * @param orderList
	 */
	@Async("batchTransferAsync")
	private void asyncBatchTransfer(AccountTransferResponse resp, List<AccOrderEntity> orderList) {
		log.info("批量转账订单保存完成，异步进行订单转账，共{}条", orderList.size());
		if(orderList != null && orderList.size()>0){
			this.transferList(orderList,resp);
		}else{
			log.info("无转账订单，无需操作用户赎回订单");
		}
	}
	
	/**
	 * 批量转账
	 * @param orderList 批量转账订单
	 * @param resp 批量转账结果
	 */
	private void transferList(List<AccOrderEntity> orderList,
			AccountTransferResponse resp) {
		String requestNo = resp.getRequestNo();
		log.info("账户转账批量交易,批次号：{}", requestNo);
		if (null != orderList && orderList.size() > 0) {
			BaseResponse baseResp = new BaseResponse();
			int successCount = 0;
			for (AccOrderEntity accOrderEntity : orderList) {
				try{
					//处理转账
					String outputAccountNo = resp.getOutputAccountNo();
					String inputAccountNo = "";
					AccountInfoEntity inputAccount = accountInfoDao.findBasicAccountByUserOid(accOrderEntity.getUserOid());
					if(inputAccount != null){
						inputAccountNo = inputAccount.getAccountNo();
					}
					BigDecimal transferBalance = accOrderEntity.getBalance();
					String orderType = accOrderEntity.getOrderType();
					String orderNo = accOrderEntity.getOrderNo();
					baseResp = this.transfer(outputAccountNo, inputAccountNo, transferBalance, orderType, orderNo, requestNo);
					//查询转账后用户余额
					accountInfoService.getAccountBalanceByUserOid(accOrderEntity.getUserOid());
				}catch(Exception e){
					log.error("赎回确认异常，accOrderEntity={},error={}",JSONObject.toJSONString(accOrderEntity),e);
					accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
					accOrderEntity.setOrderDesc(baseResp.getErrorMessage());
					//更新订单状态
					accOrderDao.saveAndFlush(accOrderEntity);
				}
				if(Constant.SUCCESS.equals(baseResp.getReturnCode())){
					//成功赎回订单
					successCount++;
					accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_SUCCESS);
					accOrderEntity.setOrderDesc("成功");
					//更新订单状态
				}else{
					accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
					accOrderEntity.setOrderDesc(baseResp.getErrorMessage());
				}
				accOrderDao.saveAndFlush(accOrderEntity);
				//回调业务系统
				try{
					this.callBack(accOrderEntity);
				}catch(Exception e){
					log.error("批量转账回调异常，accOrderEntity={},error={}",JSONObject.toJSONString(accOrderEntity),e);
				}
			}
			int failCount = orderList.size()-successCount;
			log.info("批量转账完成，批次号：{}，共{}条转账单据，成功{}单，失败{}单",requestNo, orderList.size(), successCount, failCount);
		}else{
			log.info("账户转账批量交易完成，无转账单据！");
		}
	}
	
	/**
	 * 转账参数校验
	 * @param batchTransferFrozenReq 请求冻结参数
	 * @param singleTransferReq 单笔转账参数
	 * @param unFrozenTransferReq 解冻请求参数
	 * @param batchTransferReq 批量转账订单参数
	 * @return 校验结果
	 */
	private AccountTransferResponse checkTransferParam(AccountBatchTransferFrozenRequest batchTransferFrozenReq, 
			AccountSingleTransferRequest singleTransferReq, AccountUnFrozenTransferRequest unFrozenTransferReq,
			AccountBatchTransferRequest batchTransferReq, AccountTransferResponse resp) {
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("交易成功");
		if(batchTransferFrozenReq != null){
			if(StringUtil.isEmpty(batchTransferFrozenReq.getOrderNo())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("订单号不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(batchTransferFrozenReq.getOrderType())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("订单类型不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(batchTransferFrozenReq.getOutputUserOid())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("转账人ID不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(batchTransferFrozenReq.getRequestNo())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("请求流水号不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(batchTransferFrozenReq.getOrderType())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("订单类型不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(batchTransferFrozenReq.getSubmitTime())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("订单时间不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(batchTransferFrozenReq.getSystemSource())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("系统来源不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(batchTransferFrozenReq.getUserType())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("用户类型不能为空");
				return resp;
			}
			if(batchTransferFrozenReq.getBalance() == null||BigDecimal.ZERO.compareTo(batchTransferFrozenReq.getBalance())>0){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("转账金额校验失败");
				return resp;
			}
			if(StringUtil.isEmpty(batchTransferFrozenReq.getTransferType())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("转账类型不能为空");
				return resp;
			}
		}else if(singleTransferReq != null){
			if(StringUtil.isEmpty(singleTransferReq.getOrderNo())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("订单号不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(singleTransferReq.getOrderType())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("订单类型不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(singleTransferReq.getOutputUserOid())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("转账人ID不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(singleTransferReq.getRequestNo())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("请求流水号不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(singleTransferReq.getOrderType())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("订单类型不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(singleTransferReq.getSubmitTime())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("订单时间不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(singleTransferReq.getSystemSource())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("系统来源不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(singleTransferReq.getUserType())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("用户类型不能为空");
				return resp;
			}
			if(singleTransferReq.getBalance() == null||BigDecimal.ZERO.compareTo(singleTransferReq.getBalance())>0){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("转账金额校验失败");
				return resp;
			}
		}else if(unFrozenTransferReq != null){
			if(StringUtil.isEmpty(unFrozenTransferReq.getOrderNo())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("订单号不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(unFrozenTransferReq.getOrderType())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("订单类型不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(unFrozenTransferReq.getOutputUserOid())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("转账人ID不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(unFrozenTransferReq.getRequestNo())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("请求流水号不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(unFrozenTransferReq.getOrderType())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("订单类型不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(unFrozenTransferReq.getSubmitTime())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("订单时间不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(unFrozenTransferReq.getSystemSource())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("系统来源不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(unFrozenTransferReq.getUserType())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("用户类型不能为空");
				return resp;
			}
			if(unFrozenTransferReq.getBalance() == null||BigDecimal.ZERO.compareTo(unFrozenTransferReq.getBalance())>0){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("转账金额校验失败");
				return resp;
			}
		}else if(batchTransferReq != null){
			if(StringUtil.isEmpty(batchTransferReq.getOutputUserOid())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("转账人ID不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(batchTransferReq.getRequestNo())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("请求流水号不能为空");
				return resp;
			}
			if(StringUtil.isEmpty(batchTransferReq.getSystemSource())){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("系统来源不能为空");
				return resp;
			}
			if(batchTransferReq.getOrderList().size()==0){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("批量转账订单不存在");
				return resp;
			}
		}else{
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("请求参数不能为空");
		}
		return resp;
	}
	
	/**
	 * 根据转账人信息及转账类型获取转入转出账户号
	 * @param resp 已有返回参数
	 * @param userInfo 用户信息
	 * @param orderType 订单类型
	 * @return 转入转出账户号
	 */
	private AccountTransferResponse getTransferAccountNo(
			AccountTransferResponse resp, UserInfoEntity userInfo, String orderType) {
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("交易成功");
		//支持批量冻结（批量返佣冻结59）、批量转账（返佣批量转账）、单笔转账(返佣60)、解冻（返佣解冻61）
		String outputAccountType = "";
		String inputAccountType = "";
		if(OrderTypeEnum.REBATEFROZEN.getCode().equals(orderType)){//批量返佣订单出账账户类型为可用金户，入账账户类型为资金冻结户
			outputAccountType = AccountTypeEnum.AVAILABLE_AMOUNT.getCode();
			inputAccountType = AccountTypeEnum.REDEEMFROZEN.getCode();
		}else if(OrderTypeEnum.REBATE.getCode().equals(orderType)){//返佣订单出账账户类型为资金冻结户，入账账户类型为基本户
			outputAccountType = AccountTypeEnum.REDEEMFROZEN.getCode();
			inputAccountType = AccountTypeEnum.BASICER.getCode();
		}else if(OrderTypeEnum.REBATEUNFROZEN.getCode().equals(orderType)){//返佣解冻订单出账账户类型为资金冻结户，入账账户类型为可用金户
			outputAccountType  = AccountTypeEnum.REDEEMFROZEN.getCode();
			inputAccountType = AccountTypeEnum.AVAILABLE_AMOUNT.getCode();
		}else if(orderType == null){//返佣批量转账出账账户类型为资金冻结户
			outputAccountType  = AccountTypeEnum.REDEEMFROZEN.getCode();
		}
		if("".equals(outputAccountType)){
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("暂不支持该交易，程序猿努力开发中");
			return resp;
		}
		//获取出账账户信息
		AccountInfoEntity outputAccount = accountInfoDao.findAccountByAccountTypeAndUserOid(outputAccountType, resp.getOutputUserOid());
		if(outputAccount == null){
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("转账人账户信息不存在");
			return resp;
		}
		resp.setOutputAccountNo(outputAccount.getAccountNo());
		//获取入账账户信息
		if(orderType != null){
			AccountInfoEntity inputAccount = accountInfoDao.findAccountByAccountTypeAndUserOid(inputAccountType, resp.getInputUserOid());
			if(inputAccount == null){
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("转入人账户信息不存在");
				return resp;
			}
			resp.setInputAccountNo(inputAccount.getAccountNo());
		}
		return resp;
	}
	
	/**
	 * 转账
	 * @param outputAccountNo 转出账户id
	 * @param inputAccountNo 转入账户id
	 * @param transferBalance 转账金额
	 * @param orderType 转账订单类型
	 * @return 转账结果
	 */
	@Transactional
	private BaseResponse transfer(String outputAccountNo,
			String inputAccountNo, BigDecimal transferBalance, String orderType, String orderNo, String requestNo) {
		String orderTypeDesc = OrderTypeEnum.getEnumName(orderType);
		BaseResponse resp = new BaseResponse();
		log.info("接收{}订单{}，产生账户交易，出账账户{}，入账账户{}，转账金额{}", orderTypeDesc, orderNo, outputAccountNo, inputAccountNo, transferBalance);
		AccountInfoEntity outputAccount = accountInfoDao.findByAccountNo(outputAccountNo);
		AccountInfoEntity inputAccount = accountInfoDao.findByAccountNo(inputAccountNo);
		if(outputAccount == null||inputAccount == null){
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("转账账户信息异常");
		}
		CreateTransResponse transResp = null;
		//操作账户
		log.info("交易账户 AccountInfoEntity={},锁定账户", JSONObject.toJSON(outputAccount));
		outputAccount = accountInfoDao.findByOidForUpdate(outputAccount.getOid());
		//刷新缓存
		entityManager.refresh(outputAccount);
		int result = 0;
		result = accountInfoDao.subtractBalance(transferBalance, outputAccount.getAccountNo());
		log.info("{}转账扣除转出账户余额结果={}", orderTypeDesc, result);
		if (result == 0) {
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("转账扣除转出账户余额失败，转出账户余额不足，请稍后重试");
			log.info("{}转账扣除转出账户余额失败，转账人{}转出账户{}余额不足", orderTypeDesc, outputAccount.getUserOid(), 
					outputAccount.getAccountNo());
			return resp;
		}
		BigDecimal afterBalance = outputAccount.getBalance().subtract(transferBalance);
		log.info("{}转账扣除转出账户余额明细 accountNo={}，transferBalance={}，orderNo={}， userOid={}",
				orderTypeDesc, outputAccount.getAccountNo(), orderNo, outputAccount.getUserOid());
		String orderDesc = orderTypeDesc + "转账记录转账人账户明细";
		transResp = transService.addTransferTrans(requestNo, orderNo, orderType, inputAccountNo, outputAccountNo, transferBalance,
				afterBalance, orderDesc, "02", outputAccount,"");
		//增加发行人资金冻结户余额
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			log.info("{}转账增加转入账户余额 accountNo={}，transferBalance={}", orderTypeDesc, inputAccount.getAccountNo(), 
					transferBalance);
			inputAccount=accountInfoDao.findByOidForUpdate(inputAccount.getOid());
			//刷新缓存
			entityManager.refresh(inputAccount);
			accountInfoDao.addBalance(transferBalance, inputAccount.getAccountNo());
			afterBalance = inputAccount.getBalance().add(transferBalance);
			log.info("{}转账增加转入账户余额交易明细accountNo={},transferBalance={}", orderTypeDesc, inputAccount.getAccountNo(), 
					transferBalance);
			orderDesc = orderTypeDesc + "转账记录转账账户明细";
			transResp = transService.addTransferTrans(requestNo, orderNo, orderType, inputAccountNo, outputAccountNo, transferBalance,
					afterBalance, orderDesc, "01", inputAccount,"");
		}
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		}else{
			resp.setReturnCode(transResp.getReturnCode());
			resp.setErrorMessage(transResp.getErrorMessage());
			log.info("{}转账交易失败,resp={}", orderTypeDesc, JSONObject.toJSON(resp));
		}
		return resp;
	}
	
	public void callBack(AccOrderEntity orderEntity){
		log.info("转账交易回调，转账订单{}",JSONObject.toJSONString(orderEntity));
		OrderResponse  orderResponse = new OrderResponse();
		orderResponse.setUserOid(orderEntity.getUserOid());
		orderResponse.setOrderNo(orderEntity.getOrderNo());
		orderResponse.setUserType(orderEntity.getUserType());
		String orderType = orderEntity.getOrderType();
		if(OrderTypeEnum.REDEEM.getCode().equals(orderType)){
			orderType = "03";//由于结算系统提现订单类型为02，账户赎回订单类型为02，避免回调错误，区分赎回为03
		}
		orderResponse.setType(orderType);//充值：01 提现：02 赎回：03 返佣：60
		orderResponse.setAmount(orderEntity.getBalance());
		orderResponse.setRemark(orderEntity.getRemark());
		String submitTime = DateUtil.format(orderEntity.getSubmitTime().getTime(), Constant.fomat);
		orderResponse.setPayTime(submitTime);
		orderResponse.setStatus(orderEntity.getOrderStatus());
		if(AccOrderEntity.ORDERSTATUS_SUCCESS.equals(orderEntity.getOrderStatus())){
			orderResponse.setReturnCode(Constant.SUCCESS);
			orderResponse.setErrorMessage("交易成功");
		}else{
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("交易失败");
		}
		String returnMsg = "";
		Boolean result = false;
		CallBackInfo info = null;
		//通过订单号查回调信息
		info = callbackDao.queryCallBackOneByOrderNo(orderEntity.getOrderNo(), "settlement");
		if(null!=info ){
			log.error(">>>>>>转账回调信息[订单号orderNo="+orderEntity.getOrderNo()+"]已存在!<<<<<<");
		}else{
			info = CallBackInfo.builder().orderNO(orderResponse.getOrderNo()).tradeType(TradeTypeEnum.trade_redeem.getCode())
					.payNo(orderResponse.getPayNo()).channelNo("").type("settlement").minute(1)
					.totalCount(20).totalMinCount(20).countMin(0).returnCode(orderResponse.getReturnCode())
					.status(CallBackEnum.INIT.getCode()).returnMsg(orderResponse.getErrorMessage()).createTime(new Date())
					.build();
			try {
				log.info("转账交易回调，{}", JSONObject.toJSONString(orderResponse));
				result = settlementSdk.callback(orderResponse);
			} catch (Exception e) {
				returnMsg = "推送交易信息异常";
				log.error(returnMsg + " OrderNO{},{}", orderResponse.getOrderNo(), e);
			}
			log.info("转账交易回调结果，orderNo：{},result：{}", orderResponse.getOrderNo(), result);
			if(true == result){
				info.setStatus(CallBackEnum.SUCCESS.getCode());
				info.setUpdateTime(new Date());
			}else{
				info.setStatus(CallBackEnum.INIT.getCode());
				info.setUpdateTime(new Date());
			}
			callbackDao.saveAndFlush(info);
		}
	}
	
	/**
	 * 添加账户金额
	 * @param outputAccountNo 转出账户id
	 * @param inputAccountNo 转入账户id
	 * @param transferBalance 转账金额
	 * @param orderType 转账订单类型
	 * @param orderNo 定单号
	 * @param requestNo 请求流水号
	 * @return 转账结果
	 */
	@Transactional
	public BaseResponse addBalance(String outputAccountNo,
			String inputAccountNo, BigDecimal transferBalance, String orderType, String orderNo, String requestNo,String eventTransNo) {
		String orderTypeDesc = OrderTypeEnum.getEnumName(orderType);
		BaseResponse resp = new BaseResponse();
		log.info("接收{}订单{}，产生账户交易，出账账户{}，入账账户{}，转账金额{}", orderTypeDesc, orderNo, outputAccountNo, inputAccountNo,
				transferBalance);
		AccountInfoEntity inputAccount = accountInfoDao.findByAccountNo(inputAccountNo);
		if (inputAccount == null) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("转账账户信息异常，用户账户不存在");
			return resp;
		}

		CreateTransResponse transResp = null;
		log.info("{}转账增加转入账户余额 accountNo={}，transferBalance={}", orderTypeDesc, inputAccount.getAccountNo(),
				transferBalance);
		inputAccount = accountInfoDao.findByOidForUpdate(inputAccount.getOid());
		// 刷新缓存
		entityManager.refresh(inputAccount);

		int result = 0;
		result = accountInfoDao.addBalance(transferBalance, inputAccount.getAccountNo());
		if (result == 0) {
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("转入交易异常");
			log.info("转入交易异常，定单号:{}，账户号：{}", orderNo, inputAccount);
			return resp;
		}
		BigDecimal afterBalance = inputAccount.getBalance().add(transferBalance);
		log.info("定单号{}，{}转账增加转入账户余额交易明细accountNo={},transferBalance={}，afterBalance={}", orderNo, orderTypeDesc, inputAccountNo,
				transferBalance,afterBalance);
		String orderDesc = orderTypeDesc + "转账记录转账账户明细";
		transResp = transService.addTransferTrans(requestNo, orderNo, orderType, inputAccountNo, outputAccountNo,
				transferBalance, afterBalance, orderDesc, "01", inputAccount,eventTransNo);
		// 刷新缓存
		entityManager.refresh(inputAccount);
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		} else {
			resp.setReturnCode(transResp.getReturnCode());
			resp.setErrorMessage(transResp.getErrorMessage());
			log.info("{}转账交易失败,resp={}", orderTypeDesc, JSONObject.toJSON(resp));
			throw new SETException(9015);
		}
		return resp;
	}
	
	/**
	 * 扣减账户金额
	 * @param outputAccountNo 转出账户id
	 * @param inputAccountNo 转入账户id
	 * @param transferBalance 转账金额
	 * @param orderType 转账订单类型
	 * @param orderNo 定单号
	 * @param requestNo 请求流水号
	 * @return 转账结果
	 */
	@Transactional
	public BaseResponse subtractBalance(String outputAccountNo,
			String inputAccountNo, BigDecimal transferBalance, String orderType, String orderNo, String requestNo,String eventTransNo) {
		String orderTypeDesc = OrderTypeEnum.getEnumName(orderType);
		BaseResponse resp = new BaseResponse();
		log.info("接收{}订单{}，产生账户交易，出账账户{}，入账账户{}，转账金额{}", orderTypeDesc, orderNo, outputAccountNo, inputAccountNo,
				transferBalance);
		AccountInfoEntity outputAccount = accountInfoDao.findByAccountNo(outputAccountNo);
		if (outputAccount == null) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("转账账户信息异常，用户账户不存在");
			return resp;
		}

		CreateTransResponse transResp = null;
		log.info("{}转账扣减转入账户余额 accountNo={}，transferBalance={}", orderTypeDesc, outputAccount.getAccountNo(),
				transferBalance);
		outputAccount = accountInfoDao.findByOidForUpdate(outputAccount.getOid());
		// 刷新缓存
		entityManager.refresh(outputAccount);

		int result = 0;
		result = accountInfoDao.subtractBalance(transferBalance, outputAccount.getAccountNo());
		if (result == 0) {
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("转账扣除转出账户余额失败，转出账户余额不足");
			log.info("{}转账扣除转出账户余额失败，转账人{}转出账户{}余额不足", orderTypeDesc, outputAccount.getUserOid(),
					outputAccount.getAccountNo());
			return resp;
		}
		BigDecimal afterBalance = outputAccount.getBalance().subtract(transferBalance);
		log.info("定单号{}，{}转账增加转入账户余额交易明细accountNo={},transferBalance={},afterBalance={}", orderNo, orderTypeDesc, inputAccountNo,
				transferBalance, afterBalance);
		String orderDesc = orderTypeDesc + "转账记录转账账户明细";
		transResp = transService.addTransferTrans(requestNo, orderNo, orderType, inputAccountNo, outputAccountNo,
				transferBalance, afterBalance, orderDesc, "02", outputAccount,eventTransNo);
		// 刷新缓存
		entityManager.refresh(outputAccount);
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		} else {
			resp.setReturnCode(transResp.getReturnCode());
			resp.setErrorMessage(transResp.getErrorMessage());
			log.info("{}转账交易失败,resp={}", orderTypeDesc, JSONObject.toJSON(resp));
			throw new SETException(9015);
		}
		return resp;
	}
	
}
