package com.guohuai.boot.account.service;

import com.guohuai.account.api.request.AccountTransferRequest;
import com.guohuai.account.api.request.CreateAccountRequest;
import com.guohuai.account.api.request.entity.TradeEvent;
import com.guohuai.account.api.response.CreateAccountResponse;
import com.guohuai.boot.account.dao.AccountEventChildDao;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.dao.PlatformAccountInfoDao;
import com.guohuai.boot.account.entity.AccountEventChildEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.entity.PlatformAccountInfoEntity;
import com.guohuai.boot.account.res.AccountEventResponse;
import com.guohuai.component.exception.SETException;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.UserTypeEnum;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

/**
 * @ClassName: AccountEventAdapterService
 * @Description: 账户事件出入款账户适配
 * @author chendonghui
 * @date 2018年1月29日14:02:12
 */
@Slf4j
@Service
public class AccountEventAdapterService {
	@Autowired
	private AccountEventChildDao accountEventChildDao;
	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private PlatformAccountInfoDao platformAccountInfoDao;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private AccountInfoService accountInfoService;
	
	/**
	 * 登账事件适配
	 * @param req 交易请求参数
	 * @param tradeEvent 事件信息
	 * @return 出入款账户信息及事件信息
	 */
	@Transactional
	public AccountEventResponse accountEventAdapter(
			AccountTransferRequest req, TradeEvent tradeEvent) {
		AccountEventResponse resp = new AccountEventResponse();
		resp.setRequestNo(req.getRequestNo());
		resp.setOrderNo(req.getOrderNo());
		resp.setOrderType(req.getOrderType());
		resp.setRemark(req.getRemark());
		resp.setTradeEvent(tradeEvent);
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("获取账户信息成功");
		// 获取事件
		AccountEventChildEntity accountEventChildEntity = accountEventChildDao
				.findByChildEventType(tradeEvent.getEventType());
		resp.setAccountEventChildEntity(accountEventChildEntity);
		String userOid = req.getUserOid();
		String publishUserOid = req.getPublisherUserOid();
		String outputAccountType = accountEventChildEntity.getOutputAccountType();
		String inputAccountType = accountEventChildEntity.getInputAccountType();
		AccountInfoEntity inputAccountEntity = null;
		AccountInfoEntity outputAccountEntity = null;
		switch (accountEventChildEntity.getChildEventType()) {
		case "useRedPacket":
			log.info("使用红包");
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, userOid);
			outputAccountEntity = this.getReserveOutputAccountNo(accountEventChildEntity.getOutputAccountNo());
			break;
		case "grantExperienceProfit":
			log.info("发放体验金收益");
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, userOid);
			outputAccountEntity = this.getReserveOutputAccountNo(accountEventChildEntity.getOutputAccountNo());
			break;
		case "grantRateCouponProfit":
			log.info("发放加息券收益");
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, userOid);
			outputAccountEntity = this.getReserveOutputAccountNo(accountEventChildEntity.getOutputAccountNo());
			break;
		case "grantRateCouponProfitContinued":
			log.info("续投-发放加息券收益");
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, userOid);
			outputAccountEntity = this.getReserveOutputAccountNo(accountEventChildEntity.getOutputAccountNo());
			break;
		case "useVoucherT0":
			log.info("实时使用代金券");
			inputAccountEntity = this.getProductInputAccountNo(req.getProductAccountNo());
			outputAccountEntity = this.getReserveOutputAccountNo(accountEventChildEntity.getOutputAccountNo());
			break;
		case "useVoucherT1":
			log.info("非实时使用代金券");
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, publishUserOid);
			outputAccountEntity = this.getReserveOutputAccountNo(accountEventChildEntity.getOutputAccountNo());
			break;
		case "rebate":
			log.info("返佣");
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, userOid);
			outputAccountEntity = this.getReserveOutputAccountNo(accountEventChildEntity.getOutputAccountNo());
			break;
		case "chargingPlatformFee":
			log.info("收取平台服务费");
			inputAccountEntity = this.getReserveInputAccountNo(accountEventChildEntity.getInputAccountNo());
			outputAccountEntity = this.getEventOutputAccountNo(outputAccountType, userOid);
			break;
		case "investT0":
			log.info("实时投资");
			//投资人基本户-->发行人产品户
			inputAccountEntity = this.getProductInputAccountNo(req.getProductAccountNo());
			outputAccountEntity = this.getEventOutputAccountNo(outputAccountType, userOid);
			break;
		case "investT1":
			log.info("非实时投资");
			//投资人基本户-->发行人归集户
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, publishUserOid);
			outputAccountEntity = this.getEventOutputAccountNo(outputAccountType, userOid);
			break;
		case "redeemT0":
			log.info("实时兑付");
			//发行人产品户-->投资人基本户
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, userOid);
			outputAccountEntity = this.getProductOutputAccountNo(req.getProductAccountNo());
			break;
		case "redeemT1":
			log.info("非实时兑付");
			//发行人归集户-->投资人基本户
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, userOid);
			outputAccountEntity = this.getEventOutputAccountNo(outputAccountType, publishUserOid);
			break;
		case "redeemT0Change":
			log.info("转换-实时兑付");
			//发行人产品户-->投资人续投冻结户
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, userOid);
			outputAccountEntity = this.getProductOutputAccountNo(req.getProductAccountNo());
			break;
		case "investT0Change":
			log.info("转换-实时投资");
			//投资人续投冻结户-->发行人产品户
			inputAccountEntity = this.getProductInputAccountNo(req.getProductAccountNo());
			outputAccountEntity = this.getEventOutputAccountNo(outputAccountType, userOid);
			break;
		case "redeemT0Continued":
			log.info("续投-实时兑付");
			//发行人产品户-->投资人续投冻结户
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, userOid);
			outputAccountEntity = this.getProductOutputAccountNo(req.getProductAccountNo());
			break;
		case "redeemT1Continued":
			log.info("续投-非实时兑付");
			//发行人归集户-->投资人续投冻结户
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, userOid);
			outputAccountEntity = this.getEventOutputAccountNo(outputAccountType, publishUserOid);
			break;
		case "investT0Continued":
			log.info("续投-实时投资");
			//投资人续投冻结户-->发行人产品户
			inputAccountEntity = this.getProductInputAccountNo(req.getProductAccountNo());
			outputAccountEntity = this.getEventOutputAccountNo(outputAccountType, userOid);
			break;
		case "investT1Continued":
			log.info("续投-非实时投资");
			//投资人续投冻结户-->发行人归集户
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, publishUserOid);
			outputAccountEntity = this.getEventOutputAccountNo(outputAccountType, userOid);
			break;
		case "unfreezeContinued":
			log.info("续投解冻");
			//投资人续投冻结户-->投资人基本户
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, userOid);
			outputAccountEntity = this.getEventOutputAccountNo(outputAccountType, userOid);
			break;
		case "recharge":
			log.info("充值");
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, userOid);
			break;
		case "withdraw":
			log.info("提现");
			outputAccountEntity = this.getEventInputAccountNo(outputAccountType, userOid);
			break;
		case "withdrawFrozen":
			log.info("提现冻结");
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, userOid);
			outputAccountEntity = this.getEventInputAccountNo(outputAccountType, userOid);
			break;
		case "withdrawUnfrozen":
			log.info("提现解冻");
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, userOid);
			outputAccountEntity = this.getEventInputAccountNo(outputAccountType, userOid);
			break;
		case "reFundInvestT0":
			log.info("实时投资退款");
			//发行人产品户-->投资人基本户
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, userOid);
			outputAccountEntity = this.getProductOutputAccountNo(req.getProductAccountNo());
			break;
		case "reFundInvestT1":
			log.info("非实时投资退款");
			//发行人归集户-->投资人基本户
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, userOid);
			outputAccountEntity = this.getEventOutputAccountNo(outputAccountType, publishUserOid);
			break;
		case "reFundUseVoucherT0":
			log.info("实时使用卡券退款");
			//发行人产品户-->平台备付金户
			inputAccountEntity = this.getReserveInputAccountNo(accountEventChildEntity.getInputAccountNo());
			outputAccountEntity = this.getProductOutputAccountNo(req.getProductAccountNo());
			break;
		case "reFundUseVoucherT1":
			log.info("非实时使用卡券退款");
			//发行人归集户-->平台备付金户
			inputAccountEntity = this.getReserveInputAccountNo(accountEventChildEntity.getInputAccountNo());
			outputAccountEntity = this.getEventOutputAccountNo(outputAccountType, publishUserOid);
			break;
		case "nettingIncome":
			log.info("轧差-入款");
			inputAccountEntity = this.getProductInputAccountNo(req.getProductAccountNo());
			outputAccountEntity = this.getEventOutputAccountNo(outputAccountType, publishUserOid);
			break;
		case "nettingOutcome":
			log.info("轧差-出款");
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, publishUserOid);
			outputAccountEntity = this.getProductOutputAccountNo(req.getProductAccountNo());
			break;
		case "transferPlatformBasic":
			log.info("平台基本户转账");
			inputAccountEntity = this.getRequestInputAccountNo(req.getInputAccountNo());
			outputAccountEntity = this.getPlatformAccountNo(outputAccountType);
			break;
		case "transferPlatformPayment":
			log.info("平台备付金转账");
			inputAccountEntity = this.getPlatformAccountNo(inputAccountType);
			outputAccountEntity = this.getRequestOutputAccountNo(req.getOutputAccountNo());
			break;
		case "transferPublisherBasic":
			log.info("发行人基本户转账");
			inputAccountEntity = this.getRequestInputAccountNo(req.getInputAccountNo());
			outputAccountEntity = this.getEventOutputAccountNo(outputAccountType, publishUserOid);
			break;
		case "transferPublisherProduct":
			log.info("发行人产品户转账");
			inputAccountEntity = this.getEventInputAccountNo(inputAccountType, publishUserOid);
			outputAccountEntity = this.getRequestOutputAccountNo(req.getOutputAccountNo());
			break;
		case "adjustAmountPlatformPayment":
			log.info("平台备付金调额");
			break;
		case "adjustAmountPublisherProduct":
			log.info("发行人产品户调额");
			break;
		default:
			log.error("未知事件类型");
			break;
		}
		resp.setInputAccountEntity(inputAccountEntity);
		resp.setOutputAccountEntity(outputAccountEntity);
		PlatformAccountInfoEntity platformAccountInfo = null;
		if(outputAccountEntity != null){
			if(UserTypeEnum.PLATFORMER.getCode().equals(outputAccountEntity.getUserType())){
				// 判断平台户是否被开启
				platformAccountInfo = platformAccountInfoDao
						.findByAccountNo(outputAccountEntity.getAccountNo());
				if (PlatformAccountInfoEntity.STATUS_STOP
						.equals(platformAccountInfo.getAccountStatus())) {
					resp.setReturnCode(Constant.FAIL);
					resp.setErrorMessage("出款账户被停用");
				}
			}
			// 刷新缓存
			entityManager.refresh(outputAccountEntity);
			// 判断出账账户余额是否充足
			if (outputAccountEntity.getBalance().compareTo(tradeEvent.getBalance()) < 0) {
				resp.setReturnCode(Constant.BALANCELESS);
				resp.setErrorMessage("账户余额不足");
			}
		}
		if(inputAccountEntity != null){
			if(UserTypeEnum.PLATFORMER.getCode().equals(inputAccountEntity.getUserType())){
				// 判断平台户是否被开启
				platformAccountInfo = platformAccountInfoDao
						.findByAccountNo(inputAccountEntity.getAccountNo());
				if (PlatformAccountInfoEntity.STATUS_STOP
						.equals(platformAccountInfo.getAccountStatus())) {
					resp.setReturnCode(Constant.FAIL);
					resp.setErrorMessage("出款账户被停用");
				}
			}
		}
		return resp;
	}
	
	/**
	 * 平台账户信息获取
	 * @param accountType 账户类型
	 * @return 出入款账户信息
	 */
	private AccountInfoEntity getPlatformAccountNo(String accountType) {
		AccountInfoEntity entity = accountInfoDao.findAccountByAccountTypeAndUserType(
				accountType, UserTypeEnum.PLATFORMER.getCode());
		if(entity == null){
			log.error("交易账户信息不存在");
			throw new SETException(9006);
		}
		return entity;
	}
	
	/**
	 * 转账出款账户
	 * @param outputAccountNo 转账出款账户号
	 * @return 转账出款账户
	 */
	private AccountInfoEntity getRequestOutputAccountNo(String outputAccountNo) {
		AccountInfoEntity entity = accountInfoDao.findByAccountNo(outputAccountNo);
		if(entity == null){
			log.error("交易账户信息不存在");
			throw new SETException(9006);
		}
		return entity;
	}

	/**
	 * 转账入款账户
	 * @param inputAccountNo 转账入款账户号
	 * @return 转账入款账户
	 */
	private AccountInfoEntity getRequestInputAccountNo(String inputAccountNo) {
		AccountInfoEntity entity = accountInfoDao.findByAccountNo(inputAccountNo);
		if(entity == null){
			log.error("交易账户信息不存在");
			throw new SETException(9006);
		}
		return entity;
	}

	/**
	 * 产品户入款
	 * @param productAccountNo 产品户号
	 * @return 入账账户
	 */
	private AccountInfoEntity getProductInputAccountNo(String productAccountNo) {
		AccountInfoEntity entity = accountInfoDao.findByAccountNo(productAccountNo);
		if(entity == null){
			log.error("交易账户信息不存在");
			throw new SETException(9006);
		}
		return entity;
	}
	
	/**
	 * 产品户出款
	 * @param productAccountNo 产品户号
	 * @return 出账账户
	 */
	private AccountInfoEntity getProductOutputAccountNo(String productAccountNo) {
		AccountInfoEntity entity = accountInfoDao.findByAccountNo(productAccountNo);
		if(entity == null){
			log.error("交易账户信息不存在");
			throw new SETException(9006);
		}
		return entity;
	}
	
	/**
	 * 投资人发行人出款
	 * @param outputAccountType 入款账户类型
	 * @param userOid 出款人userOid
	 * @return 出账账户
	 */
	private AccountInfoEntity getEventOutputAccountNo(String outputAccountType, String userOid) {
		log.info("outputAccountType:{},userOid:{}",outputAccountType,userOid);
		AccountInfoEntity entity = accountInfoDao.findAccountByAccountTypeAndUserOid(
				outputAccountType, userOid);
		if(entity == null){
			//账户不存在创建账户
			if(AccountTypeEnum.CONTINUED_INVESTMENT_FROZEN.getCode().equals(outputAccountType)){
				CreateAccountRequest req = new CreateAccountRequest();
				req.setUserOid(userOid);
				req.setAccountType(AccountTypeEnum.CONTINUED_INVESTMENT_FROZEN.getCode());
				req.setUserType(UserTypeEnum.INVESTOR.getCode());
				CreateAccountResponse resp = accountInfoService.addAccount(req);
				entity = accountInfoDao.findAccountByAccountTypeAndUserOid(
						outputAccountType, userOid);
				if(!Constant.SUCCESS.equals(resp.getReturnCode())){
					log.error("创建账户异常");
					throw new SETException(9075);
				}
			}else{
				log.error("交易账户信息不存在");
				throw new SETException(9006);
			}
		}
		return entity;
	}
	
	/**
	 * 投资人发行人入款
	 * @param inputAccountType 出款账户类型
	 * @param userOid 入款人userOid
	 * @return 入账账户
	 */
	public AccountInfoEntity getEventInputAccountNo(String inputAccountType, String userOid) {
		log.info("inputAccountType:{},userOid:{}",inputAccountType,userOid);
		AccountInfoEntity entity = accountInfoDao.findAccountByAccountTypeAndUserOid(
				inputAccountType, userOid);
		if(entity == null){
			//账户不存在创建账户
			if(AccountTypeEnum.CONTINUED_INVESTMENT_FROZEN.getCode().equals(inputAccountType)){
				CreateAccountRequest req = new CreateAccountRequest();
				req.setUserOid(userOid);
				req.setAccountType(AccountTypeEnum.CONTINUED_INVESTMENT_FROZEN.getCode());
				req.setUserType(UserTypeEnum.INVESTOR.getCode());
				CreateAccountResponse resp = accountInfoService.addAccount(req);
				entity = accountInfoDao.findAccountByAccountTypeAndUserOid(
						inputAccountType, userOid);
				if(!Constant.SUCCESS.equals(resp.getReturnCode())){
					log.error("创建账户异常");
					throw new SETException(9075);
				}
			}else{
				log.error("交易账户信息不存在");
				throw new SETException(9006);
			}
		}
		return entity;
	}

	/**
	 * 备付金入款
	 * @param provisionOutputAccountNo 备付金账户号
	 * @return 入账账户
	 */
	private AccountInfoEntity getReserveInputAccountNo(String provisionOutputAccountNo) {
		AccountInfoEntity entity = accountInfoDao.findByAccountNo(provisionOutputAccountNo);
		if(entity == null){
			log.error("交易账户信息不存在");
			throw new SETException(9006);
		}
		return entity;
	}

	/**
	 * 备付金出款
	 * @param provisionOutputAccountNo 备付金账户号
	 * @return 出账账户
	 */
	private AccountInfoEntity getReserveOutputAccountNo(String provisionOutputAccountNo) {
		AccountInfoEntity entity = accountInfoDao.findByAccountNo(provisionOutputAccountNo);
		if(entity == null){
			log.error("交易账户信息不存在");
			throw new SETException(9006);
		}
		return entity;
	}

}