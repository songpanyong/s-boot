package com.guohuai.boot.account.service;

import java.math.BigDecimal;
import java.sql.Timestamp;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.guohuai.account.api.request.AccountTransRequest;
import com.guohuai.account.api.request.CreateTransRequest;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.account.api.response.CreateTransResponse;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.basic.component.exception.GHException;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.UserTypeEnum;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class PurchaseTransService {
	
	@Value("${needRechargeFrozenAccount:N}")
	private String needRechargeFrozenAccount;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private AccountTradeService accountTradeService;
	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private AccOrderDao orderDao;
	/**
	 * 申购交易处理
	 */
	@Transactional
	public AccountTransResponse purchaseTrans(AccountTransRequest req) {
		AccountTransResponse resp = checkPurchaseTrans(req);

		if (!Constant.SUCCESS.equals(resp.getReturnCode())) {
			log.info("申购交易参数校验失败：{}",resp);
			return resp;
		}
		BigDecimal realOrderMoney = req.getBalance();// 扣除代金券后的实际金额订单
		BigDecimal orderMoney = realOrderMoney;
		BigDecimal voucher = req.getVoucher();// 代金券的金额
		if (null == req.getVoucher()) {
			voucher=BigDecimal.ZERO;
		}
		
		//查询用户基本户
		AccountInfoEntity userAccount = accountInfoDao.findBasicAccountByUserOid(req.getUserOid());
		if (userAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("用户账户不存在!");
			log.info("用户基本户不存在!userOid = " + req.getUserOid());
			return resp;
		}
		userAccount=accountInfoDao.findByOidForUpdate(userAccount.getOid());
	
		// 订单记录
		AccOrderEntity order = orderDao.findByOrderNo(req.getOrderNo());
		// 查询发行人账户清算户
		AccountInfoEntity publisherAccount = accountInfoDao.findByUserOidAndAccountTyp(req.getPublisherUserOid(), AccountTypeEnum.COLLECTION_SETTLEMENT.getCode());
		if (publisherAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("发行人账户归集清算户不存在!");
			log.info("发行人账户归集清算户不存在!userOid = " + req.getPublisherUserOid());
			return resp;
		}
		publisherAccount=accountInfoDao.findByOidForUpdate(publisherAccount.getOid());
		
		log.info("同步hibernate缓存，userAccount balance:{}",userAccount.getBalance());
		entityManager.refresh(userAccount);
		log.info("同步hibernate缓存后，userAccount balance:{}",userAccount.getBalance());
		//用户基本户 -balance
		int updateUserNumber = accountInfoDao.subtractBalance(realOrderMoney, userAccount.getAccountNo());
		if (updateUserNumber == 0) {
			log.error("申购扣除用户基本户余额失败，用户基本户余额不足！");
			throw new GHException(Integer.parseInt(Constant.BALANCELESS), "申购扣除用户基本户余额失败，用户基本户余额不足！");
		}
		
		// 插入用户交易流水
		req.setOrderDesc("申购记录用户基本户明细");
		req.setVoucher(voucher);
		BigDecimal afterBalance =BigDecimal.ZERO;
		afterBalance = userAccount.getBalance().subtract(orderMoney);
		accountTradeService.addTrans(req, order.getOid(), publisherAccount.getAccountNo(), userAccount.getAccountNo(), userAccount.getUserOid(),
				req.getUserType(), afterBalance, "02", AccountTypeEnum.BASICER.getCode(), userAccount.getAccountNo());
		
		//查询用户充值冻结户
		AccountInfoEntity userRechargeFrozenAccount = null;
		//断定是否需要充值冻结户 如果不需要，查询时充值冰结户设置为0
		if ("Y".equals(needRechargeFrozenAccount)) {
			userRechargeFrozenAccount = accountInfoDao.findByUserOidAndAccountTyp(req.getUserOid(),AccountTypeEnum.RECHARGEFROZEN.getCode());
		}
		// 用户充值冻结户 -balance
        if (userRechargeFrozenAccount != null) {
    		
    		userRechargeFrozenAccount=accountInfoDao.findByOidForUpdate(userRechargeFrozenAccount.getOid());
    		log.info("同步hibernate缓存，用户充值冻结户");
    		entityManager.refresh(userRechargeFrozenAccount);
            // 如果用户的可用余额小于申购的金额，则充值冻结户余额为0
            accountInfoDao.subtractBalanceLowerLimitZero(realOrderMoney, userRechargeFrozenAccount.getAccountNo());
            //20170810记录扣除充值冻结户流水
			String orderDesc = "投资人转账记录投资人充值冻结户明细";
			BigDecimal userRechargeAfterBalance = BigDecimal.ZERO;
			if(userRechargeFrozenAccount.getBalance().compareTo(realOrderMoney)>=0){
				userRechargeAfterBalance = userRechargeFrozenAccount.getBalance().subtract(realOrderMoney);
			}
			this.addTrans(order, userRechargeFrozenAccount.getAccountNo(), 
					publisherAccount.getAccountNo(), userRechargeAfterBalance, orderDesc, "02",AccountTypeEnum.RECHARGEFROZEN.getCode());
        }
        log.info("同步hibernate缓存，publisherAccount");
		entityManager.refresh(publisherAccount);
		// 发行人账户清算户+balance
		int updatePublisherNumber = accountInfoDao.addBalance(orderMoney, publisherAccount.getAccountNo());
		if (updatePublisherNumber == 0) {
			log.error("申购增加发行人归集清算户失败！");
			throw new GHException(Integer.parseInt(Constant.BALANCELESS), "申购增加发行人归集清算户失败！");
		}
		log.info("申购账户更新结果：updatePublisherNumber={},updateUserNumber={}", updatePublisherNumber, updateUserNumber);
		
		// 插入发行人交易流水
		req.setOrderDesc("申购记录发行人归集清算户户明细");
		req.setVoucher(voucher);
        afterBalance = publisherAccount.getBalance().add(orderMoney);
        accountTradeService.addTrans(req, order.getOid(), publisherAccount.getAccountNo(), userAccount.getAccountNo(), publisherAccount.getUserOid(),
				UserTypeEnum.PUBLISHER.getCode(), afterBalance, "01" ,AccountTypeEnum.COLLECTION_SETTLEMENT.getCode(), publisherAccount.getAccountNo());
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");
		return resp;
	}

	/**
	 * 申购交易参数检查
	 */
	private AccountTransResponse checkPurchaseTrans(AccountTransRequest req) {
		AccountTransResponse resp = new AccountTransResponse();
		if (StringUtil.isEmpty(req.getUserOid())) {
			resp.setReturnCode(Constant.USERNOTEXISTS);
			resp.setErrorMessage("用户不存在");
			return resp;
		}
		if (StringUtil.isEmpty(req.getUserType())) {
			resp.setReturnCode(Constant.ACCOUNTTYPENOTEXISTS);
			resp.setErrorMessage("账户类型不存在");
			return resp;
		}
		if (req.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
			resp.setReturnCode(Constant.BALANCEERROR);
			resp.setErrorMessage("错误的金额");
			return resp;
		}
		if (StringUtil.isEmpty(req.getPublisherUserOid())) {
			resp.setReturnCode(Constant.USERNOTEXISTS);
			resp.setErrorMessage("发行人不存在");
			return resp;
		}
		if (StringUtil.isEmpty(req.getOrderNo())) {
			resp.setReturnCode(Constant.ORDERNOEXISTS);
			resp.setErrorMessage("订单号不存在");
			return resp;
		}
		if (StringUtil.isEmpty(req.getRequestNo())) {
			resp.setReturnCode(Constant.REQUEST_NUMBER_IS_NULL);
			resp.setErrorMessage("请求流水为空");
			return resp;
		}
		if (StringUtil.isEmpty(req.getUserType())) {
			resp.setReturnCode(Constant.ACCOUNTTYPENOTEXISTS);
			resp.setErrorMessage("账户类型不存在");
			return resp;
		}
		resp.setReturnCode(Constant.SUCCESS);
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
	public CreateTransResponse addTrans(AccOrderEntity accOrderEntity, String inputAccountNo, 
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
