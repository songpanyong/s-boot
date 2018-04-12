package com.guohuai.boot.account.service.accountinfo;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountSettlementRequest;
import com.guohuai.account.api.request.AccountTransRequest;
import com.guohuai.account.api.response.AccountSettlementResponse;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.account.api.response.CreateOrderResponse;
import com.guohuai.account.api.response.CreateTransResponse;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.service.AccOrderService;
import com.guohuai.boot.account.service.AccountRedeemService;
import com.guohuai.boot.account.service.AccountTransferService;
import com.guohuai.boot.account.service.UserInfoService;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.OrderTypeEnum;
import com.guohuai.settlement.api.response.BaseResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 账户轧差结算基本操作
 * @author ZJ
 * @date 2018年1月22日 上午10:20:59
 * @version V1.0
 */
@Slf4j
@Service
@Transactional
public class AccountNettingBaseService {
	@Autowired
	private AccOrderDao accOrderDao;
	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private UserInfoService userInfoService;
	@Autowired
	private AccountRedeemService accountRedeemService;
	@Autowired
	private AccOrderService accOrderService;
	@Autowired
	private AccountTransferService accountTransferService;

	/**
	 * 赎回轧差结算 订单轧差，校验金额 锁定发行人可用金户，判断是否可进行结算 申购>赎回，发行人归集户-->发行人可用金户
	 * 赎回>申购，发行人可用金户-->发行人归集户 操作赎回订单账户
	 * @param accOrderEntity
	 */
	public AccountSettlementResponse nettingSettlement(AccountSettlementRequest req) {
		log.info("赎回轧差结算，请求参数：{}", JSONObject.toJSON(req));
		AccountSettlementResponse resp = new AccountSettlementResponse();
		String userOid = req.getPublisherUserOid();
		BigDecimal nettingBalance = req.getNettingBalance();// 轧差额
		BigDecimal redeemBalance = req.getRedeemBalance();// 赎回单据总额
		resp.setPublisherUserOid(userOid);
		resp.setNettingBalance(nettingBalance);
		BigDecimal availableBalance = BigDecimal.ZERO;// 发行人可用金户金额
		AccountInfoEntity availableAccount = null;
		CreateTransResponse transResp = null;
		// 参数校验
		if (!StringUtil.isEmpty(userOid)) {
			// 查询发行人是否存在
			UserInfoEntity userInfo = userInfoService.getAccountUserByUserOid(userOid);
			if (userInfo != null) {
				// 查询发行人可用金账户
				availableAccount = accountInfoDao
						.findAccountByAccountTypeAndUserOid(AccountTypeEnum.AVAILABLE_AMOUNT.getCode(), userOid);
				if (availableAccount != null) {
					availableBalance = availableAccount.getBalance();
					log.info("发行人{}可用金账户余额：{}", userOid, availableBalance);
				} else {
					log.error("发行人{}可用金账户不存在!", userOid);
					resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
					resp.setErrorMessage("发行人可用金账户不存在");
					return resp;
				}
			} else {
				resp.setReturnCode(Constant.USERNOTEXISTS);
				resp.setErrorMessage("发行人不存在!");
				log.error("发行人不存在![userOid=" + userOid + "]");
				return resp;
			}
		} else {
			// 发行人userOid不能为空
			resp.setReturnCode(Constant.REQUEST_USEROID_IS_NULL);
			resp.setErrorMessage("发行人userOid不能为空");
			log.info("发行人userOid不能为空!");
			return resp;
		}
		if (nettingBalance == null) {
			// 轧差金额不能为空
			resp.setReturnCode(Constant.REQUEST_NETTING_BALANCE_IS_NULL);
			resp.setErrorMessage("轧差金额不能为空");
			return resp;
		}

		// 接收定单
		AccountTransRequest accountTransRequest = new AccountTransRequest();
		accountTransRequest.setBalance(nettingBalance.abs());
		accountTransRequest.setFee(BigDecimal.ZERO);
		accountTransRequest.setOrderCreatTime(req.getOrderCreatTime());
		accountTransRequest.setOrderDesc(req.getOrderDesc());
		accountTransRequest.setOrderNo(req.getOrderNo());
		accountTransRequest.setOrderType(req.getOrderType());
		accountTransRequest.setPublisherUserOid(userOid);
		accountTransRequest.setRemark(req.getRemark());
		accountTransRequest.setRequestNo(req.getRequestNo());
		accountTransRequest.setSystemSource(req.getSystemSource());
		accountTransRequest.setUserOid(userOid);
		accountTransRequest.setUserType(req.getUserType());
		accountTransRequest.setVoucher(BigDecimal.ZERO);

		AccountTransResponse accountTransResponse = this.checkPurchaseTrans(accountTransRequest);
		if (!Constant.SUCCESS.equals(accountTransResponse.getReturnCode())) {
			log.info("结算轧差交易参数校验失败：{}", accountTransResponse);
			resp.setErrorMessage(accountTransResponse.getErrorMessage());
			resp.setReturnCode(accountTransResponse.getReturnCode());
			return resp;
		}
		if (!(OrderTypeEnum.ABAPPLY.getCode().equals(req.getOrderType())
				|| OrderTypeEnum.ABREDEEM.getCode().equals(req.getOrderType()))) {
			resp.setErrorMessage("订单类型不支持");
			resp.setReturnCode(Constant.ORDERTYPENOTEXISTS);
			log.info("非结算轧差订单！");
			return resp;
		}
		CreateOrderResponse cResp = accOrderService.acceptOrder(accountTransRequest);
		if (!Constant.SUCCESS.equals(cResp.getReturnCode())) {
			log.error("创建订单失败!订单号{}", accountTransRequest.getOrderNo());
			resp.setReturnCode(cResp.getReturnCode());
			resp.setErrorMessage(cResp.getErrorMessage());
			return resp;
		}
		AccOrderEntity accOrderEntity = null;
		accOrderEntity = accOrderDao.findByOrderNo(cResp.getOrderNo());
		if (accOrderEntity == null) {
			log.error("创建订单失败!订单号{}", accountTransRequest.getOrderNo());
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("系统异常");
			return resp;
		}
		if (nettingBalance.compareTo(BigDecimal.ZERO) == 0) {// 申购等于赎回
			resp = this.applyEquelRedeem(resp, transResp, nettingBalance, userOid, accOrderEntity, redeemBalance);
		} else if (nettingBalance.compareTo(BigDecimal.ZERO) > 0) {// 申购大于赎回
			resp = this.applyMoreThanRedeem(resp, transResp, nettingBalance, availableBalance, userOid,
					availableAccount, accOrderEntity, redeemBalance);
		} else if (nettingBalance.compareTo(BigDecimal.ZERO) < 0) {// 申购小于赎回并且可用金大于轧差额
			nettingBalance = nettingBalance.abs();
			if (availableBalance.compareTo(nettingBalance) >= 0) {
				resp = this.redeemMoreThanApply(resp, transResp, nettingBalance, availableBalance, userOid,
						availableAccount, accOrderEntity, redeemBalance);
			} else {
				log.info("发行人可用金不足 ,发行人Uid:{},发行人可用金账户:{},可用金：{}，轧差额{}", userOid, availableAccount.getAccountNo(),
						availableBalance, nettingBalance);
				resp.setReturnCode(Constant.REQUEST_PUBLISHER_BALANCE_NOT_ENOUGH);
				resp.setErrorMessage("发行人可用金不足，请放款到可用金账户");
			}
		}
		if (Constant.SUCCESS.equals(resp.getReturnCode())) {
			// 修改订单状态
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_SUCCESS);
			accOrderEntity.setOrderDesc("交易成功");
			log.info("结算轧差交易成功!");
		} else {
			// 修改订单状态
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
			accOrderEntity.setOrderDesc(resp.getErrorMessage());
			log.info("结算轧差交易失败!");
		}
		accOrderDao.saveAndFlush(accOrderEntity);
		return resp;
	}

	/**
	 * 申购等于赎回，需冻结参与轧差赎回单总金额 先判断归集户余额是否充足， 若充足，扣除发行人归集户赎回单总金额，增加发行人资金冻结户余额
	 * 若不足，扣除发行人可用金户余额补足赎回单总额到发行人资金冻结户余额 可用金不足，轧差失败
	 * @param resp
	 * @param transResp
	 * @param nettingBalance
	 * @param redeemOrderBalance
	 * @param applyOrderBalance
	 * @param userOid
	 * @return
	 */
	@Transactional
	public AccountSettlementResponse applyEquelRedeem(AccountSettlementResponse resp, CreateTransResponse transResp,
			BigDecimal nettingBalance, String userOid, AccOrderEntity accOrderEntity, BigDecimal redeemBalance) {
		BaseResponse baseResp = new BaseResponse();
		// 发行人可用金账户不变，扣除发行人归集户，
		log.info("发行人{}，轧差金额为{}，赎回金额{}，冻结轧差赎回金额！", userOid, nettingBalance, redeemBalance);
		// 扣除发行人归集户余额，增加发行人可用金户余额
		log.info("轧差结算金额{}，发行人{}账户交易，冻结赎回金额，操作和记录交易流水", redeemBalance, userOid);
		AccountInfoEntity settlementAccount = accountInfoDao
				.findAccountByAccountTypeAndUserOid(AccountTypeEnum.COLLECTION_SETTLEMENT.getCode(), userOid);
		if (settlementAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("发行人归集户不存在");
			log.info("轧差结算冻结发行人赎回金额失败，发行人{}归集户不存在", userOid);
			return resp;
		}
		AccountInfoEntity availableAccount = accountInfoDao
				.findAccountByAccountTypeAndUserOid(AccountTypeEnum.AVAILABLE_AMOUNT.getCode(), userOid);
		if (availableAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("发行人可用金户不存在");
			log.info("轧差结算冻结发行人赎回金额失败，发行人{}可用金户不存在", userOid);
			return resp;
		}
		AccountInfoEntity frozenAccount = accountInfoDao
				.findAccountByAccountTypeAndUserOid(AccountTypeEnum.REDEEMFROZEN.getCode(), userOid);
		if (frozenAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("发行人资金冻结户不存在");
			log.info("轧差结算冻结发行人赎回金额失败，发行人{}资金冻结户不存在", userOid);
			return resp;
		}
		BigDecimal settlementAccountBalance = settlementAccount.getBalance();
		BigDecimal availableBalance = availableAccount.getBalance();
		BigDecimal frozenBalance = frozenAccount.getBalance();
		BigDecimal difference = BigDecimal.ZERO;
		// 判断归集户是否够扣除赎回总额
		if (settlementAccountBalance.compareTo(redeemBalance) >= 0) {
			// 发行人归集户转账给发行人资金冻结户
			baseResp = accountTransferService.publisherSettlementAccountTransferToPublisherFrozenAccount(accOrderEntity,
					userOid, redeemBalance, settlementAccountBalance, settlementAccount, frozenBalance, frozenAccount);
		} else if (settlementAccountBalance.add(availableBalance).compareTo(redeemBalance) >= 0) {
			difference = redeemBalance.subtract(settlementAccountBalance);
			// 发行人归集户转账给发行人资金冻结户
			baseResp = accountTransferService.publisherSettlementAccountTransferToPublisherFrozenAccount(accOrderEntity,
					userOid, settlementAccountBalance, settlementAccountBalance, settlementAccount, frozenBalance,
					frozenAccount);
			// 发行人可用金户转账给发行人资金冻结户
			if (Constant.SUCCESS.equals(baseResp.getReturnCode())) {
				baseResp = accountTransferService.publisherAvailableAccountTransferToPublisherFrozenAccount(
						accOrderEntity, userOid, difference, availableBalance, availableAccount, frozenBalance,
						frozenAccount);
			}
		}
		if (Constant.SUCCESS.equals(baseResp.getReturnCode())) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		} else {
			resp.setReturnCode(baseResp.getReturnCode());
			resp.setErrorMessage(baseResp.getErrorMessage());
		}
		return resp;
	}

	/**
	 * 申购大于赎回，操作账户 1、将轧差金额转入可用金账户 2、将赎回金额转入资金冻结户 若归集户余额不够，去可用金户拿
	 * @param resp
	 * @param transResp
	 * @param nettingBalance
	 * @param redeemOrderBalance
	 * @param userOid
	 * @param frozenAccount
	 * @return
	 */
	@Transactional
	public AccountSettlementResponse applyMoreThanRedeem(AccountSettlementResponse resp, CreateTransResponse transResp,
			BigDecimal nettingBalance, BigDecimal availableBalance, String userOid, AccountInfoEntity availableAccount,
			AccOrderEntity accOrderEntity, BigDecimal redeemBalance) {
		BaseResponse baseResp = new BaseResponse();
		// 扣除发行人归集户余额，增加发行人可用金户余额
		log.info("轧差结算金额{}，发行人{}账户交易，扣除轧差金额及赎回金额，操作和记录交易流水", nettingBalance, userOid);
		AccountInfoEntity settlementAccount = accountInfoDao
				.findAccountByAccountTypeAndUserOid(AccountTypeEnum.COLLECTION_SETTLEMENT.getCode(), userOid);
		if (settlementAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("发行人归集户不存在");
			log.info("轧差结算冻结发行人赎回金额失败，发行人{}归集户不存在", userOid);
			return resp;
		}
		AccountInfoEntity frozenAccount = accountInfoDao
				.findAccountByAccountTypeAndUserOid(AccountTypeEnum.REDEEMFROZEN.getCode(), userOid);
		if (frozenAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("发行人资金冻结户不存在");
			log.info("轧差结算冻结发行人赎回金额失败，发行人{}资金冻结户不存在", userOid);
			return resp;
		}
		BigDecimal settlementAccountBalance = settlementAccount.getBalance();
		BigDecimal frozenBalance = frozenAccount.getBalance();
		BigDecimal difference = BigDecimal.ZERO;
		// 判断归集户是否够扣除轧差额及轧差赎回单总额
		if (settlementAccountBalance.compareTo(nettingBalance.add(redeemBalance)) >= 0) {
			// 发行人归集户向发行人可用金转账
			baseResp = accountTransferService.publisherSettlementAccountTransferToPublisherAvailableAccount(
					accOrderEntity, userOid, nettingBalance, settlementAccountBalance, settlementAccount,
					availableBalance, availableAccount);
			// 发行人归集户向发行人资金冻结户转账
			if (Constant.SUCCESS.equals(baseResp.getReturnCode())) {
				baseResp = accountTransferService.publisherSettlementAccountTransferToPublisherFrozenAccount(
						accOrderEntity, userOid, redeemBalance, settlementAccountBalance, settlementAccount,
						frozenBalance, frozenAccount);
			}
		} else {
			difference = (nettingBalance.add(redeemBalance)).subtract(settlementAccountBalance);
			// 判断归集户余额是否够
			if (availableBalance.compareTo(difference) < 0) {
				log.info("发行人可用金不足 ,发行人Uid:{},发行人可用金账户:{},可用金：{}，轧差额{}", userOid, availableAccount.getAccountNo(),
						availableBalance, nettingBalance);
				resp.setReturnCode(Constant.REQUEST_PUBLISHER_BALANCE_NOT_ENOUGH);
				resp.setErrorMessage("发行人可用金不足,请放款到可用金账户");
				return resp;
			}
			// 判断差额是否大于轧差额
			if (difference.compareTo(nettingBalance) > 0) {
				// 发行人归集户向发行人资金冻结户转账
				baseResp = accountTransferService.publisherSettlementAccountTransferToPublisherFrozenAccount(
						accOrderEntity, userOid, settlementAccountBalance, settlementAccountBalance, settlementAccount,
						frozenBalance, frozenAccount);
				// 发行人可用金户向发行人资金冻结户转账
				if (Constant.SUCCESS.equals(baseResp.getReturnCode())) {
					baseResp = accountTransferService.publisherAvailableAccountTransferToPublisherFrozenAccount(
							accOrderEntity, userOid, redeemBalance.subtract(settlementAccountBalance), availableBalance,
							availableAccount, frozenBalance, frozenAccount);
				}
			} else if (difference.compareTo(nettingBalance) == 0) {
				// 发行人归集户向发行人资金冻结户转账
				baseResp = accountTransferService.publisherSettlementAccountTransferToPublisherFrozenAccount(
						accOrderEntity, userOid, redeemBalance, settlementAccountBalance, settlementAccount,
						frozenBalance, frozenAccount);

			} else {
				// 发行人归集户向发行人可用金户转账
				baseResp = accountTransferService.publisherSettlementAccountTransferToPublisherAvailableAccount(
						accOrderEntity, userOid, settlementAccountBalance.subtract(redeemBalance),
						settlementAccountBalance, settlementAccount, availableBalance, availableAccount);
				// 发行人归集户向发行人资金冻结户转账
				if (Constant.SUCCESS.equals(baseResp.getReturnCode())) {
					baseResp = accountTransferService.publisherSettlementAccountTransferToPublisherFrozenAccount(
							accOrderEntity, userOid, redeemBalance, settlementAccountBalance, settlementAccount,
							frozenBalance, frozenAccount);
				}
			}

		}
		if (Constant.SUCCESS.equals(baseResp.getReturnCode())) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		} else {
			resp.setReturnCode(baseResp.getReturnCode());
			resp.setErrorMessage(baseResp.getErrorMessage());
		}
		return resp;
	}

	/**
	 * 申购小于赎回并且可用金大于轧差额 1、将可用金账户轧差金额转入发行人归集户 若赎回额不够扣除，从发行人可用金拿
	 * @param resp
	 * @param transResp
	 * @param nettingBalance
	 * @param redeemOrderBalance
	 * @param userOid
	 * @param redeemBalance
	 * @param frozenAccount
	 * @return
	 */
	@Transactional
	public AccountSettlementResponse redeemMoreThanApply(AccountSettlementResponse resp, CreateTransResponse transResp,
			BigDecimal nettingBalance, BigDecimal availableBalance, String userOid, AccountInfoEntity availableAccount,
			AccOrderEntity accOrderEntity, BigDecimal redeemBalance) {
		// 扣除发行人可用金户余额，增加发行人归集户余额
		BaseResponse baseResp = new BaseResponse();
		// 扣除发行人归集户余额，增加发行人可用金户余额
		log.info("轧差结算金额{}，发行人{}账户交易，扣除轧差金额及赎回金额，操作和记录交易流水", nettingBalance, userOid);
		AccountInfoEntity settlementAccount = accountInfoDao
				.findAccountByAccountTypeAndUserOid(AccountTypeEnum.COLLECTION_SETTLEMENT.getCode(), userOid);
		if (settlementAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("发行人归集户不存在");
			log.info("轧差结算冻结发行人赎回金额失败，发行人{}归集户不存在", userOid);
			return resp;
		}
		AccountInfoEntity frozenAccount = accountInfoDao
				.findAccountByAccountTypeAndUserOid(AccountTypeEnum.REDEEMFROZEN.getCode(), userOid);
		if (frozenAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("发行人资金冻结户不存在");
			log.info("轧差结算冻结发行人赎回金额失败，发行人{}资金冻结户不存在", userOid);
			return resp;
		}
		BigDecimal settlementAccountBalance = settlementAccount.getBalance();
		BigDecimal frozenBalance = frozenAccount.getBalance();
		BigDecimal difference = BigDecimal.ZERO;
		// 判断赎回单总额是否大于轧差额
		if (redeemBalance.compareTo(nettingBalance) > 0) {
			difference = redeemBalance.subtract(nettingBalance);
			if (settlementAccountBalance.compareTo(difference) >= 0) {
				// 发行人可用金户向发行人资金冻结户转账
				baseResp = accountTransferService.publisherAvailableAccountTransferToPublisherFrozenAccount(
						accOrderEntity, userOid, nettingBalance, availableBalance, availableAccount, frozenBalance,
						frozenAccount);
				// 发行人归集户向发行人资金冻结户转账
				if (Constant.SUCCESS.equals(baseResp.getReturnCode())) {
					baseResp = accountTransferService.publisherSettlementAccountTransferToPublisherFrozenAccount(
							accOrderEntity, userOid, difference, settlementAccountBalance, settlementAccount,
							frozenBalance, frozenAccount);
				}
			} else {
				difference = redeemBalance.subtract(settlementAccountBalance);
				// 发行人可用金户向发行人资金冻结户转账
				baseResp = accountTransferService.publisherAvailableAccountTransferToPublisherFrozenAccount(
						accOrderEntity, userOid, difference, availableBalance, availableAccount, frozenBalance,
						frozenAccount);
				// 发行人归集户向发行人资金冻结户转账
				if (Constant.SUCCESS.equals(baseResp.getReturnCode())) {
					baseResp = accountTransferService.publisherSettlementAccountTransferToPublisherFrozenAccount(
							accOrderEntity, userOid, settlementAccountBalance, settlementAccountBalance,
							settlementAccount, frozenBalance, frozenAccount);
				}
			}
			// 赎回单总额是否等于轧差额
		} else if (redeemBalance.compareTo(nettingBalance) == 0) {
			// 发行人可用金户向发行人资金冻结户转账
			baseResp = accountTransferService.publisherAvailableAccountTransferToPublisherFrozenAccount(accOrderEntity,
					userOid, redeemBalance, availableBalance, availableAccount, frozenBalance, frozenAccount);
			// 赎回单总额小于轧差额
		} else {
			resp.setReturnCode(Constant.NETTING_BALANCE_LESS_THAN_REDEEM_BALANCE);
			resp.setErrorMessage("赎回订单总额不能小于轧差金额");
			log.info("轧差结算失败，发行人{}赎回订单总额不能小于轧差金额", userOid);
			return resp;
		}
		if (Constant.SUCCESS.equals(baseResp.getReturnCode())) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		} else {
			resp.setReturnCode(baseResp.getReturnCode());
			resp.setErrorMessage(baseResp.getErrorMessage());
		}
		return resp;
	}

	/**
	 * 投资人异步赎回订单
	 * @param userOid
	 * @param nettingTime
	 */
	@Async("settlementAsync")
	public void batchRedeem(List<AccOrderEntity> redeemList, String batchTime) {
		log.info("赎回订单保存完成，异步进行投资者订单赎回，共{}条", redeemList.size());
		if (redeemList != null && redeemList.size() > 0) {
			accountRedeemService.investorRedeemList(redeemList, batchTime);
		} else {
			log.info("无赎回订单，无需操作用户赎回订单");
		}
	}

	/**
	 * 参数校验
	 * @param req
	 * @return
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
		resp.setReturnCode(Constant.SUCCESS);
		return resp;
	}
}