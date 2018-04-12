package com.guohuai.boot.account.service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountTransRequest;
import com.guohuai.account.api.request.CreateTransRequest;
import com.guohuai.account.api.response.CreateTransResponse;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.component.exception.SETException;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.RedeemTypeEnum;
import com.guohuai.component.util.UserTypeEnum;
import com.guohuai.settlement.api.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * @ClassName: AccountRedeemService
 * @Description: 账户赎回
 * @author CHENDONGHUI
 * @date 2017年6月6日 下午 13:39:22
 */
@Slf4j
@Service
public class InvestorRedeemService {

	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private AccountInfoService accountInfoService;
	@Autowired
	private AccountTradeService accountTradeService;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private AccOrderDao accOrderDao;
	@Autowired
	private AccountTransferService accountTransferService;
	/**
	 * 是否需要赎回在途户
	 */
	@Value("${needOnwayAccount:N}")
	private String needOnwayAccount;

	/**
	 * 赎回投资者账户交易 发行人归集户-->投资者基本户 投资者在途户扣除订单金额 修改订单 状态
	 * 
	 * @param accOrderEntity
	 * @return
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public BaseResponse investorRedeem(AccOrderEntity accOrderEntity) {
		String userOid = accOrderEntity.getUserOid();
		String publisherUserOid = accOrderEntity.getPublisherUserOid();
		log.info("用户{}订单赎回{}", userOid, JSONObject.toJSONString(accOrderEntity));
		BaseResponse resp = new BaseResponse();
		CreateTransResponse transResp = null;
		//赎回订单，发行人资金冻结户减少
		log.info("投资人{}账户交易，赎回订单{}，金额{},操作和记录交易流水", userOid, accOrderEntity.getOrderNo(), accOrderEntity.getBalance());
		//获取发行人资金冻结户
		AccountInfoEntity frozenAccount = accountInfoDao
				.findAccountByAccountTypeAndUserOid(AccountTypeEnum.REDEEMFROZEN.getCode(), publisherUserOid);
		//获取投资人基本户
		AccountInfoEntity basicAccount = accountInfoDao
				.findAccountByAccountTypeAndUserOid(AccountTypeEnum.BASICER.getCode(), userOid);
		AccountInfoEntity onwayAccount = null;
		//配置是否使用在途户
		if("Y".equals(needOnwayAccount)){
			//获取投资人在途户
			onwayAccount = accountInfoDao.findAccountByAccountTypeAndUserOid(AccountTypeEnum.ONWAY.getCode(), userOid);
			if(onwayAccount == null){
				log.error("赎回投资者订单{}，投资人在途户不存在!");
				resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
				resp.setErrorMessage("投资人在途户不存在");
				return resp;
			}
		}
		//判断发行人资金冻结户是否存在
		if(frozenAccount == null){
			log.error("赎回投资者订单{}，发行人资金冻结户不存在!");
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("发行人资金冻结不存在");
			return resp;
		}
		//判断投资人基本户是否存在
		if(basicAccount == null){
			log.error("赎回投资者订单{}，投资人基本户不存在!");
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("投资人基本户不存在");
			return resp;
		}
		basicAccount=accountInfoDao.findByOidForUpdate(basicAccount.getOid());
		entityManager.refresh(basicAccount);
		
		//操作发行人资金冻结户
		log.info("交易账户 AccountInfoEntity={},锁定账户", JSONObject.toJSON(frozenAccount));
		frozenAccount = accountInfoDao.findByOidForUpdate(frozenAccount.getOid());
		entityManager.refresh(frozenAccount);
		int result = 0;
		result = accountInfoDao.subtractBalance(accOrderEntity.getBalance(), frozenAccount.getAccountNo());
		log.info("投资人赎回扣除发行人资金冻结户余额结果={}", result);
		if (result == 0) {
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("投资人赎回扣除发行人资金冻结户余额失败，发行人资金冻结户余额不足");
			log.info("投资人赎回扣除发行人资金冻结户余额失败，发行人{}资金冻结户{}余额不足", accOrderEntity.getPublisherUserOid(), 
					frozenAccount.getAccountNo());
			return resp;
		}
		BigDecimal afterBalance = frozenAccount.getBalance().subtract(accOrderEntity.getBalance());
		log.info("投资人赎回记录发行人资金冻结户明细 accountNo={}，orderBalance={}，orderNo={}， userOid={}，publisherUserOid={}",
				frozenAccount.getAccountNo(), accOrderEntity.getBalance(), accOrderEntity.getOrderNo(), userOid,
				publisherUserOid);
		String orderDesc = "投资人赎回记录发行人资金冻结户明细";
	
		transResp = this.addTrans(basicAccount.getAccountNo(), frozenAccount.getAccountNo(), accOrderEntity,
				afterBalance, orderDesc, "02", AccountTypeEnum.REDEEMFROZEN.getCode(), "T2", publisherUserOid, frozenAccount.getAccountNo());
		if (Constant.SUCCESS.equals(transResp.getReturnCode())) {
			log.info("投资人赎回增加投资人基本户余额 accountNo={},orderBalacne={}", basicAccount.getAccountNo(), 
					accOrderEntity.getBalance());
			accountInfoDao.addBalance(accOrderEntity.getBalance(), basicAccount.getAccountNo());
			afterBalance = basicAccount.getBalance().add(accOrderEntity.getBalance());
			// 记录投资人交易明细，发行的明细及余额更新
			log.info("投资人赎回记录投资人基本户交易明细accountNo={}，orderbanlance={}，orderNo={}， userOid={}",
					basicAccount.getAccountNo(), accOrderEntity.getBalance(), accOrderEntity.getOrderNo(),
					accOrderEntity.getUserOid());
			orderDesc = "投资人赎回记录投资人基本户明细";
			transResp = this.addTrans(basicAccount.getAccountNo(), frozenAccount.getAccountNo(), accOrderEntity,
					afterBalance, orderDesc, "01", AccountTypeEnum.BASICER.getCode(), "T1", userOid, basicAccount.getAccountNo());
		}
		if (Constant.SUCCESS.equals(transResp.getReturnCode())) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		} else {
			resp.setReturnCode(transResp.getReturnCode());
			resp.setErrorMessage(transResp.getErrorMessage());
			log.info("用户{}赎回订单{}结算交易失败,resp={}", accOrderEntity.getUserOid(), accOrderEntity.getOrderNo(),
					JSONObject.toJSON(resp));
		}
		return resp;
	}

    /**
     * 增加账户流水
     * @param inputAccountNo
     * @param outpuptAccountNo
     * @param accOrderEntity
     * @param afterBalance
     * @param orderDesc
     * @param direction
     * @param accountType
     * @param userType
     * @param userOid
     * @param accountNo
     * @return
     */
	private CreateTransResponse addTrans(String inputAccountNo, String outpuptAccountNo, AccOrderEntity accOrderEntity,
			BigDecimal afterBalance, String orderDesc, String direction, String accountType, String userType,
			String userOid, String accountNo) {
		CreateTransRequest transRequest = new CreateTransRequest();
		transRequest.setUserOid(userOid);
		transRequest.setAccountOid(accountNo);
		transRequest.setUserType(userType);
		transRequest.setRequestNo(accOrderEntity.getRequestNo());
		transRequest.setAccountOrderOid(accOrderEntity.getOrderNo());
		transRequest.setOrderType(accOrderEntity.getOrderType());
		transRequest.setSystemSource(accOrderEntity.getSystemSource());
		transRequest.setOrderNo(accOrderEntity.getOrderNo());
		transRequest.setRelationProductNo(accOrderEntity.getRelationProductNo());
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
	 * investorRedeemDay:T+0赎回 用于续投 <br/>
	 * <pre>
	 * 入账、出账账户逻辑：
	 * 1.扣减发行人可用金：钱从发行人可用金户出，其为出账账户；
	 * 2.增加投资者金额：钱进入投资者基本户，其为入账账户。
	 * </pre>
	 * @param accountTransRequest
	 * @param oid
	 * @return
	 */
	@Transactional
	public BaseResponse investorRedeemDay(AccountTransRequest accountTransRequest, String oid) {
		String userOid = accountTransRequest.getUserOid();
		AccOrderEntity accOrderEntity = accOrderDao.getOne(oid);
		String publisherUserOid = accOrderEntity.getPublisherUserOid();
		BaseResponse resp = new BaseResponse();
		log.info("投资人{}账户交易，赎回订单{}，金额{},操作和记录交易流水", userOid, accOrderEntity.getOrderNo(), accOrderEntity.getBalance());
		// 获取发行人账户：可用金账户(T0赎回)/资金冻结账户(T1赎回)
		AccountInfoEntity availableAccount = null;
		if(RedeemTypeEnum.REDEEMT0.getCode().equals(accountTransRequest.getRemark())){
			availableAccount = accountInfoDao
					.findAccountByAccountTypeAndUserOid(AccountTypeEnum.AVAILABLE_AMOUNT.getCode(), publisherUserOid);
		} else if(RedeemTypeEnum.REDEEMT1.getCode().equals(accountTransRequest.getRemark())){
			availableAccount = accountInfoDao
					.findAccountByAccountTypeAndUserOid(AccountTypeEnum.REDEEMFROZEN.getCode(), publisherUserOid);
		} else {
			log.error("赎回投资者订单{}，赎回订单类型:{}异常，只能是T0/T1 ", accountTransRequest.getOrderNo(), accountTransRequest.getRemark());
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("赎回订单类型异常");
			return resp;
		}
		// 判断发行人资金冻结户是否存在
		if (availableAccount == null) {
			log.error("赎回投资者订单{}，发行人可用金不存在!");
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("发行人可用金不存在");
			return resp;
		}

		// 获取投资人基本户
		AccountInfoEntity basicAccount = accountInfoDao
				.findAccountByAccountTypeAndUserOid(AccountTypeEnum.BASICER.getCode(), userOid);
		if (basicAccount == null) {
			log.error("赎回投资者订单{}，投资人基本户不存在!", accountTransRequest.getOrderNo());
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("投资人基本户不存在");
			return resp;
		}
		
		// 获取投资人续投冻结户
		AccountInfoEntity continuFrozenAccount = accountInfoService.findAccountOrCreate(userOid,
				AccountTypeEnum.CONTINUED_INVESTMENT_FROZEN.getCode(), UserTypeEnum.INVESTOR.getCode());
		
		BaseResponse continuResponse = this.AccountRedeemHandler(availableAccount, basicAccount, continuFrozenAccount, accountTransRequest, accOrderEntity);
		return continuResponse;
	}

	
	/**
	 * <pre>
	 * 投资者赎回、续投账户操作逻辑
	 * 1.扣减发行人(可用金/冻结)账户,交易额
	 * 2.增加投资者基本户，交易额
	 * 3.增加投资者续投冻结户，冻结金额
	 * </pre>
	 * @param availableAccount 出账账户（发行人）
	 * @param basicAccount 入账账户（投资者基本户）
	 * @param continuFrozenAccount 入账账户（投资者续投冻结户）
	 * @param accountTransRequest 交易参数
	 * @param accOrderEntity 订单信息
	 * @return
	 */
	@Transactional
	public BaseResponse AccountRedeemHandler(AccountInfoEntity availableAccount, AccountInfoEntity basicAccount,
			AccountInfoEntity continuFrozenAccount, AccountTransRequest accountTransRequest,
			AccOrderEntity accOrderEntity) {
		BaseResponse resp = new BaseResponse();
		BaseResponse availableAccountResponse = accountTransferService.subtractBalance(availableAccount.getAccountNo(),
				basicAccount.getAccountNo(), accountTransRequest.getBalance(), accountTransRequest.getOrderType(),
				accountTransRequest.getOrderNo(), accountTransRequest.getRequestNo(),"");
		if (!BaseResponse.isSuccess(availableAccountResponse)) {
			log.info("赎回操作订单-扣减发行人(可用金/冻结)账户：{}, baseResponse= {} ", accountTransRequest.getOrderNo(), JSONObject.toJSON(availableAccountResponse));
			resp.setReturnCode(availableAccountResponse.getReturnCode());
			resp.setErrorMessage(availableAccountResponse.getErrorMessage());
			return resp;
		}

		// 2.操作用户（投资人）基本户：增加用户基本户
		BaseResponse baseResponse = accountTransferService.addBalance(availableAccount.getAccountNo(),
				basicAccount.getAccountNo(), accountTransRequest.getBalance(), accountTransRequest.getOrderType(),
				accountTransRequest.getOrderNo(), accountTransRequest.getRequestNo(),"");
		if (!BaseResponse.isSuccess(baseResponse)) {
			log.info("赎回操作订单-增加用户基本户：{}, baseResponse= {} ", accountTransRequest.getOrderNo(), JSONObject.toJSON(baseResponse));
			throw new SETException(baseResponse.getReturnCode());
		}

		// 3.操作用户续投冻结账户
		BaseResponse continuResponse = accountTransferService.addBalance(availableAccount.getAccountNo(),
				continuFrozenAccount.getAccountNo(), accountTransRequest.getFrozenBalance(), accountTransRequest.getOrderType(),
				accountTransRequest.getOrderNo(), accountTransRequest.getRequestNo(),"");
		if (!BaseResponse.isSuccess(continuResponse)) {
			log.info("赎回操作订单：{}, baseResponse= {} ", accountTransRequest.getOrderNo(), JSONObject.toJSON(continuResponse));
			throw new SETException(baseResponse.getReturnCode());
		}

		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");

		return resp;
	}

}
