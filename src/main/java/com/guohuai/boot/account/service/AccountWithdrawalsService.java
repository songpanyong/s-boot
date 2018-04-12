package com.guohuai.boot.account.service;

import java.math.BigDecimal;
import java.sql.Timestamp;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountTransRequest;
import com.guohuai.account.api.request.CreateAccountRequest;
import com.guohuai.account.api.request.CreateTransRequest;
import com.guohuai.account.api.response.CreateAccountResponse;
import com.guohuai.account.api.response.CreateOrderResponse;
import com.guohuai.account.api.response.CreateTransResponse;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.Constant;
import com.guohuai.settlement.api.response.BaseResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName: AccountTransferService
 * @Description: 账户转账
 * @author CHENDONGHUI
 * @date 2017年6月16日 下午 15:36:25
 */
@Slf4j
@Service
@Transactional
public class AccountWithdrawalsService {

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
	private AccountInfoService accountInfoService;
	@Autowired
	private EntityManager entityManager;
	
	/**
	 * 提现
	 * @param transType
	 * @param accountTransRequest
	 * @return
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public BaseResponse withdrawals(String transType, AccountTransRequest accountTransRequest){
		String userOid = accountTransRequest.getUserOid();
		BigDecimal orderBalance = accountTransRequest.getBalance();
		String userType = accountTransRequest.getUserType();
		BaseResponse resp = new BaseResponse();
		BigDecimal basicBalance = BigDecimal.ZERO;//用户基本户金额
		AccountInfoEntity basicAccount = null;//用户基本户
		BigDecimal withdrawalsFrozenBalance = BigDecimal.ZERO;//用户提现冻结户金额
		AccountInfoEntity withdrawalsFrozenAccount = null;//用户提现冻结
		if(!StringUtil.isEmpty(userOid)){
			//查询投资人是否存在
			UserInfoEntity userInfo = userInfoService.getAccountUserByUserOid(userOid);
			if(userInfo != null){
				//查询用户基本账户
				basicAccount = accountInfoDao.findAccountByAccountTypeAndUserOid(
						AccountTypeEnum.BASICER.getCode(), userOid);
				if(basicAccount != null){
					basicBalance = basicAccount.getBalance();
					log.info("用户{}基本账户余额：{}", userOid, basicBalance);
				}else{
					log.error("用户{}基本账户不存在!",userOid);
					resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
					resp.setErrorMessage("投用户基本账户不存在");
					return resp;
				}
				//查询用户提现冻结账户
				withdrawalsFrozenAccount = accountInfoDao.findAccountByAccountTypeAndUserOid(
						AccountTypeEnum.FROZEN.getCode(), userOid);
				if(withdrawalsFrozenAccount == null){
					log.error("用户{}提现冻结账户不存在，创建提现冻结户!",userOid);
					CreateAccountRequest accountReq = new CreateAccountRequest();
					accountReq.setUserOid(userOid);
					accountReq.setAccountType(AccountTypeEnum.FROZEN.getCode());
					accountReq.setUserType(accountTransRequest.getUserType());
					CreateAccountResponse createAccountResponse = accountInfoService.addAccount(accountReq);
					if(Constant.SUCCESS.equals(createAccountResponse.getReturnCode())){
						//创建成功时再次查询用户提现冻结账户
						withdrawalsFrozenAccount = accountInfoDao.findAccountByAccountTypeAndUserOid(
								AccountTypeEnum.FROZEN.getCode(), userOid);
					}else{
						log.error("创建用户{}提现冻结户失败!",userOid);
						resp.setReturnCode(createAccountResponse.getReturnCode());
						resp.setErrorMessage("创建用户提现冻结户失败");
						return resp;
					}
				}
				if(withdrawalsFrozenAccount != null){
					withdrawalsFrozenBalance = withdrawalsFrozenAccount.getBalance();
				}else{
					log.error("创建用户{}提现冻结户成功，查询出错!",userOid);
					resp.setReturnCode(Constant.FAIL);
					resp.setErrorMessage("查询用户提现冻结户信息失败");
					return resp;
				}
				log.info("用户{}提现冻结账户余额：{}", userOid, withdrawalsFrozenBalance);
				if("FROZEN".equals(transType)){
					if(basicBalance.compareTo(withdrawalsFrozenBalance.add(orderBalance)) < 0){
						resp.setReturnCode(Constant.BALANCEERROR);
						resp.setErrorMessage("用户提现基本户余额不足");
						log.info("用户提现基本户余额不足!");
						return resp;
					}
				}
			}else{
				resp.setReturnCode(Constant.USERNOTEXISTS);
				resp.setErrorMessage("用户不存在!");
				log.error("用户不存在![userOid=" + userOid + "]");
				return resp;
			}
		}else{
			//用户userOid不能为空
			resp.setReturnCode(Constant.REQUEST_USEROID_IS_NULL);
			resp.setErrorMessage("用户userOid不能为空");
			log.info("用户userOid不能为空!");
			return resp;
		}
		if(orderBalance != null){
			if(orderBalance.compareTo(BigDecimal.ZERO) < 0){
				resp.setReturnCode(Constant.BALANCEERROR);
				resp.setErrorMessage("转账金额不能为负数");
				log.info("转账金额不能为负数!");
				return resp;
			}
		}else{
			//转账金额不能为空
			resp.setReturnCode(Constant.BALANCEERROR);
			resp.setErrorMessage("订单金额不能为空");
			log.info("订单金额不能为空!");
			return resp;
		}
		// 接收定单
        CreateOrderResponse cResp = accOrderService.acceptOrder(accountTransRequest);
        if(Constant.SECONDTIME.equals(cResp.getReturnCode())){
    		cResp.setReturnCode(Constant.SUCCESS);
    	}
        if (!Constant.SUCCESS.equals(cResp.getReturnCode())) {
        	log.error("创建订单失败!订单号{}", accountTransRequest.getOrderNo());
			resp.setReturnCode(cResp.getReturnCode());
			resp.setErrorMessage(cResp.getErrorMessage());
			return resp;
        }
        AccOrderEntity accOrderEntity = null;
        accOrderEntity = accOrderDao.findByOrderNo(cResp.getOrderNo());
        if(accOrderEntity == null){
        	log.error("创建订单失败!订单号{}", accountTransRequest.getOrderNo());
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("系统异常");
			return resp;
        }
		if("FROZEN".equals(transType)){
			resp=this.forzenUserWithdrawalsAccount(userType, userOid,
					basicAccount, orderBalance, withdrawalsFrozenAccount, accOrderEntity);
		}else if("UNFROZEN".equals(transType)){
			resp=this.unforzenUserWithdrawalsAccount(userType, userOid, orderBalance, withdrawalsFrozenAccount, accOrderEntity);
		}else if("WITHDRAWALS".equals(transType)){
			resp=this.withdrawalsAccountTrans(userType, userOid, basicAccount, 
					orderBalance, withdrawalsFrozenAccount, accOrderEntity);
		}else{
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("提现账户交易类型为空");
		}
		
		if(Constant.SUCCESS.equals(resp.getReturnCode())&&"FROZEN".equals(transType)){
			//修改订单状态
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_INIT);
			accOrderEntity.setOrderDesc("冻结成功");
			log.info("提现冻结成功!");
		}else if(Constant.SUCCESS.equals(resp.getReturnCode())&&"UNFROZEN".equals(transType)){
			//修改订单状态
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
			accOrderEntity.setOrderDesc("解冻成功");
			log.info("提现解冻成功!");
		}else if(Constant.SUCCESS.equals(resp.getReturnCode())&&"WITHDRAWALS".equals(transType)){
			//修改订单状态
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_SUCCESS);
			accOrderEntity.setOrderDesc("提现成功");
			log.info("提现成功!");
		}else{
			//修改订单状态
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
			accOrderEntity.setOrderDesc(resp.getErrorMessage());
			log.info("提现交易失败!");
		}
		accOrderDao.saveAndFlush(accOrderEntity);
		return resp;
	}
	
	/**
	 * 提现申请冻结余额
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
	public BaseResponse forzenUserWithdrawalsAccount(String userType, String userOid, 
			AccountInfoEntity basicAccount, BigDecimal orderBalance,
			AccountInfoEntity withdrawalsFrozenAccount, AccOrderEntity accOrderEntity ){
		log.info("{}用户{}提现，冻结基本户余额,金额{}", userType, userOid, orderBalance);
		BaseResponse resp = new BaseResponse();
		CreateTransResponse transResp = null;
		//操作账户
		log.info("交易账户 AccountInfoEntity={}", JSONObject.toJSON(withdrawalsFrozenAccount));
		int result = 0;
		
		withdrawalsFrozenAccount = accountInfoDao.findByOidForUpdate(withdrawalsFrozenAccount.getOid());
		log.info("withdrawalsFrozenAccount:{}",withdrawalsFrozenAccount.getBalance());
		//hibernate 同步缓存
		entityManager.refresh(withdrawalsFrozenAccount);
		
		log.info("withdrawalsFrozenAccount1:{}",withdrawalsFrozenAccount.getBalance());
		BigDecimal afterBalance= BigDecimal.ZERO;
		afterBalance= withdrawalsFrozenAccount.getBalance().add(orderBalance);
		log.info("afterBalance:{}",afterBalance);
		result = accountInfoDao.addBalance(orderBalance, withdrawalsFrozenAccount.getAccountNo());
		log.info("{}用户{}提现增加提现冻结户余额结果={}", userType, userOid, result);
		if (result == 0) {
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("用户提现增加提现冻结户余额失败");
			log.info("{}用户{}提现增加提现冻结户余额失败，冻结户账户号：", userType, userOid, withdrawalsFrozenAccount.getAccountNo());
			return resp;
		}
		
		log.info("{}用户{}提现增加提现冻结户余额记录提现冻结户明细 accountNo={}，orderBalance={}，orderNo={}， userOid={}",
				userType, userOid, withdrawalsFrozenAccount.getAccountNo(), accOrderEntity.getOrderNo(), userOid);
		String orderDesc = "提现增加提现冻结户余额记录提现冻结户明细";
		transResp = this.addTrans(accOrderEntity, withdrawalsFrozenAccount.getAccountNo(), 
				basicAccount.getAccountNo(), afterBalance, orderDesc, "01", 
				AccountTypeEnum.FROZEN.getCode(), withdrawalsFrozenAccount.getAccountNo());
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("冻结成功");
		}else{
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("用户提现增加提现冻结户余额失败");
		}
		return resp;
	}
	
	/**
	 * 解冻提现冻结户余额
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
	public BaseResponse unforzenUserWithdrawalsAccount(String userType, String userOid, BigDecimal orderBalance,
			AccountInfoEntity withdrawalsFrozenAccount, AccOrderEntity accOrderEntity ){
		log.info("{}用户{}提现，解冻提现冻结户余额,金额{}", userType, userOid, orderBalance);
		BaseResponse resp = new BaseResponse();
		CreateTransResponse transResp = null;
		//操作账户
		log.info("交易账户 AccountInfoEntity={},锁定账户", JSONObject.toJSON(withdrawalsFrozenAccount));
		withdrawalsFrozenAccount = accountInfoDao.findByOidForUpdate(withdrawalsFrozenAccount.getOid());
		//hibernate 同步缓存
		entityManager.refresh(withdrawalsFrozenAccount);
		int result = 0;
		result = accountInfoDao.subtractBalance(orderBalance, withdrawalsFrozenAccount.getAccountNo());
		log.info("{}用户{}提现，解冻提现冻结户余额结果={}", userType, userOid, result);
		if (result == 0) {
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("用户提现扣除用户冻结户余额失败，用户提现冻结户余额不足");
			log.info("用户提现扣除用户冻结户余额失败，用户{}提现冻结户{}余额不足", userOid, 
					withdrawalsFrozenAccount.getAccountNo());
			return resp;
		}
		BigDecimal afterBalance = withdrawalsFrozenAccount.getBalance().subtract(orderBalance);
		log.info("用户提现扣除用户冻结户余额明细 accountNo={}，orderBalance={}，orderNo={}， userOid={}",
				withdrawalsFrozenAccount.getAccountNo(), orderBalance, accOrderEntity.getOrderNo(), userOid);
		String orderDesc = "用户提现扣除用户冻结户余额明细";
		transResp = this.addTrans(accOrderEntity, "", 
				withdrawalsFrozenAccount.getAccountNo(), afterBalance, orderDesc, "02", 
				AccountTypeEnum.FROZEN.getCode(), withdrawalsFrozenAccount.getAccountNo());
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		}else{
			resp.setReturnCode(transResp.getReturnCode());
			resp.setErrorMessage(transResp.getErrorMessage());
			log.info("用户提现扣除用户冻结户余额失败,resp={}", JSONObject.toJSON(resp));
		}
		return resp;
	}
	
	/**
	 * 提现成功扣除冻结户余额及基本户余额
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
	public BaseResponse withdrawalsAccountTrans(String userType, String userOid, 
			AccountInfoEntity basicAccount, BigDecimal orderBalance,
			AccountInfoEntity withdrawalsFrozenAccount, AccOrderEntity accOrderEntity ){
		log.info("提现成功扣除冻结户余额及基本户余额,金额{}", orderBalance);
		BaseResponse resp = new BaseResponse();
		CreateTransResponse transResp = null;
		//操作账户
		log.info("交易账户 AccountInfoEntity={},锁定账户", JSONObject.toJSON(basicAccount));
		basicAccount = accountInfoDao.findByOidForUpdate(basicAccount.getOid());
		
		//hibernate 同步缓存
		entityManager.refresh(basicAccount);
		
		withdrawalsFrozenAccount = accountInfoDao.findByOidForUpdate(withdrawalsFrozenAccount.getOid());
		
		//hibernate 同步缓存
		entityManager.refresh(withdrawalsFrozenAccount);
		
		int result = 0;
		BigDecimal balance=basicAccount.getBalance();
		
		result = accountInfoDao.subtractBalance(orderBalance, basicAccount.getAccountNo());
		log.info("投提现成功扣除{}用户{}基本户余额结果={}", userType, userOid, result);
		if (result == 0) {
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("提现成功扣除基本户余额失败，用户基本户余额不足");
			log.info("提现成功扣除基本户余额失败，用户{}基本户{}余额不足", userOid, 
					basicAccount.getAccountNo());
			return resp;
		}
		
		BigDecimal afterBalance = balance.subtract(orderBalance);
		log.info("提现成功扣除基本户余额 accountNo={}，orderBalance={}，orderNo={}， userOid={}，publisherUserOid={}",
				basicAccount.getAccountNo(), orderBalance, accOrderEntity.getOrderNo(), userOid);
		
		log.info("定单号{}记录账务明细，当前余额{}，定单金额：{}，交易后余额：{}",accOrderEntity.getOrderNo(),balance,orderBalance,afterBalance);
		
		String orderDesc = "提现成功扣除基本户余额";
		transResp = this.addTrans(accOrderEntity, "", 
				basicAccount.getAccountNo(), afterBalance, orderDesc, "02", 
				AccountTypeEnum.BASICER.getCode(), basicAccount.getAccountNo());
		
		//解冻用户提现冻结户余额
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			log.info("提现解冻用户提现冻结户余额 accountNo={},orderBalance={}", withdrawalsFrozenAccount.getAccountNo(), 
					orderBalance);
			accountInfoDao.subtractBalance(orderBalance, withdrawalsFrozenAccount.getAccountNo());
			afterBalance = withdrawalsFrozenAccount.getBalance().subtract(orderBalance);
			log.info("提现解冻用户提现冻结户交易明细accountNo={},orderBalance={}",withdrawalsFrozenAccount.getAccountNo(), 
					orderBalance);
			orderDesc = "提现解冻用户提现冻结户交易明细";
			transResp = this.addTrans(accOrderEntity, "", 
					withdrawalsFrozenAccount.getAccountNo(), afterBalance, orderDesc, "02", 
					AccountTypeEnum.FROZEN.getCode(), withdrawalsFrozenAccount.getAccountNo());
		}
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		}else{
			resp.setReturnCode(transResp.getReturnCode());
			resp.setErrorMessage(transResp.getErrorMessage());
			log.info("提现成功扣除冻结户余额及基本户余额交易失败,resp={}", JSONObject.toJSON(resp));
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
	 * 用户基本户余额修复
	 * @param userType
	 * @param userOid
	 * @param frozenAccountNo
	 * @param lockbasicAccount
	 * @param orderBalance
	 * @param withdrawalsFrozenAccount
	 * @param accOrderEntity
	 * @return
	 */
	@Transactional
	public BaseResponse addBasicAccountTrans(String userType, String userOid, 
			AccountInfoEntity basicAccount, BigDecimal orderBalance, AccOrderEntity accOrderEntity ){
		log.info("用户基本户余额修复,增加用户基本户余额,金额{}", orderBalance);
		BaseResponse resp = new BaseResponse();
		CreateTransResponse transResp = null;
		
		
		//操作账户
		log.info("交易账户 AccountInfoEntity={},锁定账户", JSONObject.toJSON(basicAccount));
		AccountInfoEntity lockbasicAccount = accountInfoDao.findByOidForUpdate(basicAccount.getOid());
		//hibernate 同步缓存
		entityManager.refresh(lockbasicAccount);
		int result = 0;
		BigDecimal balance=lockbasicAccount.getBalance();
		result = accountInfoDao.addBalance(orderBalance, lockbasicAccount.getAccountNo());
		if (result == 0) {
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage(" ");
			log.info("增加用户{}基本户{}异常", userOid, 
					lockbasicAccount.getAccountNo());
			return resp;
		}
		
		BigDecimal afterBalance = balance.add(orderBalance);
		
		log.info("投资人转账记录投资人基本户明细 accountNo={}，orderBalance={}，orderNo={}， userOid={}，publisherUserOid={}",
				lockbasicAccount.getAccountNo(), orderBalance, accOrderEntity.getOrderNo(), userOid);
		log.info("定单号{}记录账务明细，当前余额{}，定单金额：{}，交易后余额：{}",accOrderEntity.getOrderNo(),balance,orderBalance,afterBalance);
		
		String orderDesc = "用户基本户余额修复，增加用户基本户余额";
		transResp = this.addTrans(accOrderEntity, lockbasicAccount.getAccountNo(), 
				lockbasicAccount.getAccountNo(), afterBalance, orderDesc, "01", 
				AccountTypeEnum.BASICER.getCode(), lockbasicAccount.getAccountNo());
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		}else{
			resp.setReturnCode(transResp.getReturnCode());
			resp.setErrorMessage(transResp.getErrorMessage());
			log.info("修复基本户余额失败记录明细余额失败，,resp={}", JSONObject.toJSON(resp));
		}
		return resp;
	}
	
	/**
	 * 用户基本户余额修复
	 * @param userType
	 * @param userOid
	 * @param frozenAccountNo
	 * @param lockBasicAccount
	 * @param orderBalance
	 * @param withdrawalsFrozenAccount
	 * @param accOrderEntity
	 * @return
	 */
	@Transactional
	public BaseResponse subtractBasicAccountTrans(String userType, String userOid, 
			AccountInfoEntity basicAccount, BigDecimal orderBalance, AccOrderEntity accOrderEntity ){
		log.info("用户基本户余额修复,减少用户基本户余额,金额{}", orderBalance);
		BaseResponse resp = new BaseResponse();
		CreateTransResponse transResp = null;
		
		
		//操作账户
		log.info("交易账户 AccountInfoEntity={},锁定账户", JSONObject.toJSON(basicAccount));
		AccountInfoEntity lockBasicAccount = accountInfoDao.findByOidForUpdate(basicAccount.getOid());
		//hibernate 同步缓存
		entityManager.refresh(lockBasicAccount);
		int result = 0;
		BigDecimal balance=lockBasicAccount.getBalance();
		result = accountInfoDao.subtractBalance(orderBalance, lockBasicAccount.getAccountNo());
		if (result == 0) {
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("扣除基本户余额失败，余额不足");
			log.info("扣除基本户余额失败,用户:{},基本户:{}", userOid, 
					lockBasicAccount.getAccountNo());
			return resp;
		}
		
		BigDecimal afterBalance = balance.subtract(orderBalance);
		log.info("投资人转账记录投资人基本户明细 accountNo={}，orderBalance={}，orderNo={}， userOid={}，publisherUserOid={}",
				lockBasicAccount.getAccountNo(), orderBalance, accOrderEntity.getOrderNo(), userOid);
		log.info("定单号{}记录账务明细，当前余额{}，定单金额：{}，交易后余额：{}",accOrderEntity.getOrderNo(),balance,orderBalance,afterBalance);
		String orderDesc = "用户基本户余额修复，扣减用户基本户余额";
		transResp = this.addTrans(accOrderEntity, "", 
				lockBasicAccount.getAccountNo(), afterBalance, orderDesc, "02", 
				AccountTypeEnum.BASICER.getCode(), lockBasicAccount.getAccountNo());
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		}else{
			resp.setReturnCode(transResp.getReturnCode());
			resp.setErrorMessage(transResp.getErrorMessage());
			log.info("修复基本户余额扣减失败，记录明细余额失败，,resp={}", JSONObject.toJSON(resp));
		}
		return resp;
	}
	
}
