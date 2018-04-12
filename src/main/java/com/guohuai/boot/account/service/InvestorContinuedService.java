package com.guohuai.boot.account.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountTransRequest;
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

/**
 * @ClassName: InvestorContinuedService
 * @Description: 账户续投
 * @author hugo
 * @date 2017年12月1日11:38:58
 */
@Slf4j
@Service
public class InvestorContinuedService {
	
	@Autowired
	private AccountInfoService accountInfoService;
	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private AccOrderDao accOrderDao;
	@Autowired
	private AccountTransferService accountTransferService;
	
	/**
	 * 续投服务（包括T+1、T+0）
	 * 
	 * @param accountTransRequest
	 * @param oid
	 * @return
	 */
	@Transactional
	public BaseResponse investorContinue(AccountTransRequest accountTransRequest, String oid) {
		AccOrderEntity accOrderEntity = accOrderDao.getOne(oid);
		String userOid = accountTransRequest.getUserOid();
		String publisherUserOid = accOrderEntity.getPublisherUserOid();
		BaseResponse resp = new BaseResponse();
		//1.验证通过之后扣减用户续投金额，失败返回错误
		// 获取投资人续投冻结户
		AccountInfoEntity userContinuFrozenAccount = accountInfoService.findAccountOrCreate(userOid,
				AccountTypeEnum.CONTINUED_INVESTMENT_FROZEN.getCode(), UserTypeEnum.INVESTOR.getCode());
		if (userContinuFrozenAccount == null) {
			log.error("赎回投资者订单{}，投资人续投冻结户不存在!", accountTransRequest.getOrderNo());
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("投资人续投冻结户不存在");
			return resp;
		}
		// 获取发行人账户：可用金账户(T0赎回)/归集清算户(T1赎回)
		AccountInfoEntity publisherAccount = null;
		if(RedeemTypeEnum.REDEEMT0.getCode().equals(accountTransRequest.getRemark())){
			publisherAccount = accountInfoDao
					.findAccountByAccountTypeAndUserOid(AccountTypeEnum.AVAILABLE_AMOUNT.getCode(), publisherUserOid);
			if (publisherAccount == null) {
				resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
				resp.setErrorMessage("发行人可用金账户不存在!");
				log.info("发行人可用金账户不存在!publisherUserOid = {}" + publisherUserOid);
				return resp;
			}
		} else if(RedeemTypeEnum.REDEEMT1.getCode().equals(accountTransRequest.getRemark())){
			// 查询发行人账户清算户
			publisherAccount = accountInfoDao.findByUserOidAndAccountTyp(publisherUserOid, AccountTypeEnum.COLLECTION_SETTLEMENT.getCode());
			if (publisherAccount == null) {
				resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
				resp.setErrorMessage("发行人账户归集清算户不存在!");
				log.info("发行人账户归集清算户不存在!publisherUserOid = {}" + publisherUserOid);
				return resp;
			}
		}
		if (publisherAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("发行人账户不存在!");
			log.info("发行人账户不存在!publisherUserOid = {}" + publisherUserOid);
			return resp;
		}
		//出-扣减投资人续投冻结户，入-转入发行人（T0:可用金户;T1:归集清算户）账户
		BaseResponse continuFrozenResponse = accountTransferService.subtractBalance(userContinuFrozenAccount.getAccountNo(),
				publisherAccount.getAccountNo(), accountTransRequest.getBalance(), accountTransRequest.getOrderType(),
				accountTransRequest.getOrderNo(), accountTransRequest.getRequestNo(),"");
		if (!BaseResponse.isSuccess(continuFrozenResponse)) {
			log.info("续投操作订单：{}, continuFrozenResponse= {} ", accountTransRequest.getOrderNo(), JSONObject.toJSON(continuFrozenResponse));
			resp.setReturnCode(continuFrozenResponse.getReturnCode());
			resp.setErrorMessage(continuFrozenResponse.getErrorMessage());
			return resp;
		}
		
		//2.调用原申购接口执行续投，失败则抛出异常，回滚整个事务
		BaseResponse purchaseResponse = null;
		if(RedeemTypeEnum.REDEEMT0.getCode().equals(accountTransRequest.getRemark())){
			purchaseResponse = this.accountContinuPurchaseT0(userOid, publisherUserOid, accountTransRequest);
		} else if(RedeemTypeEnum.REDEEMT1.getCode().equals(accountTransRequest.getRemark())){
			purchaseResponse = this.accountContinuPurchaseT1(userOid, publisherUserOid, accountTransRequest);
		}
		if (!BaseResponse.isSuccess(purchaseResponse)) {
			log.info("续投操作订单，申购异常：{}, purchaseResponse= {} ", accountTransRequest.getOrderNo(), JSONObject.toJSON(purchaseResponse));
			throw new SETException(resp.getReturnCode());
		}
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");
		
		return resp;
	}
	
	/**
	 * 续投申购
	 * 投资人户到发行人户
	 * T0：投资人基本户到发行人可用金户
	 * @param userOid
	 * @param publisherUserOid
	 * @param accountTransRequest
	 * @return
	 */
	@Transactional
	public BaseResponse accountContinuPurchaseT0(String userOid, String publisherUserOid, AccountTransRequest accountTransRequest) {
		BaseResponse resp = new BaseResponse();
		AccountInfoEntity userAccount = accountInfoService.findAccountOrCreate(userOid,
				AccountTypeEnum.BASICER.getCode(), UserTypeEnum.INVESTOR.getCode());
		if (userAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("投资人基本户不存在!");
			log.info("投资人基本户不存在!userOid = {}" + userOid);
			return resp;
		}
		
		AccountInfoEntity publisherAccount = accountInfoDao
				.findAccountByAccountTypeAndUserOid(AccountTypeEnum.AVAILABLE_AMOUNT.getCode(), publisherUserOid);
		if (publisherAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("发行人可用金账户不存在!");
			log.info("发行人可用金账户不存在!publisherUserOid = {}" + publisherUserOid);
			return resp;
		}
		//1.扣减投资人基本户
		BaseResponse userAccountResponse = accountTransferService.subtractBalance(userAccount.getAccountNo(),
				publisherAccount.getAccountNo(), accountTransRequest.getBalance(), accountTransRequest.getOrderType(),
				accountTransRequest.getOrderNo(), accountTransRequest.getRequestNo(),"");
		if (!BaseResponse.isSuccess(userAccountResponse)) {
			log.info("赎回-扣减投资人基本户操作订单：{}, userAccountResponse= {} ", accountTransRequest.getOrderNo(), JSONObject.toJSON(userAccountResponse));
			resp.setReturnCode(userAccountResponse.getReturnCode());
			resp.setErrorMessage(userAccountResponse.getErrorMessage());
			return resp;
		}
		
		// 2.增加发行人可用金户
		BaseResponse baseResponse = accountTransferService.addBalance(userAccount.getAccountNo(),
				publisherAccount.getAccountNo(), accountTransRequest.getBalance(), accountTransRequest.getOrderType(),
				accountTransRequest.getOrderNo(), accountTransRequest.getRequestNo(),"");
		if (!BaseResponse.isSuccess(baseResponse)) {
			log.info("赎回-增加发行人可用金户操作订单：{}, baseResponse= {} ", accountTransRequest.getOrderNo(), JSONObject.toJSON(baseResponse));
			throw new SETException(baseResponse.getReturnCode());
		}
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("t0申购成功");

		return resp;
	}

	/**
	 * 续投申购
	 * T1: 投资人基本户到发行人归集清算户
	 * @param userOid
	 * @param publisherUserOid
	 * @param accountTransRequest
	 * @return
	 */
	@Transactional
	public BaseResponse accountContinuPurchaseT1(String userOid, String publisherUserOid, AccountTransRequest accountTransRequest) {
		BaseResponse resp = new BaseResponse();
		AccountInfoEntity userAccount = accountInfoService.findAccountOrCreate(userOid,
				AccountTypeEnum.BASICER.getCode(), UserTypeEnum.INVESTOR.getCode());
		if (userAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("投资人基本户不存在!");
			log.info("投资人基本户不存在!userOid = {}" + userOid);
			return resp;
		}
		// 查询发行人账户清算户
		AccountInfoEntity publisherAccount = accountInfoDao.findByUserOidAndAccountTyp(publisherUserOid, AccountTypeEnum.COLLECTION_SETTLEMENT.getCode());
		if (publisherAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("发行人账户归集清算户不存在!");
			log.info("发行人账户归集清算户不存在!publisherUserOid = {}" + publisherUserOid);
			return resp;
		}
		//1.扣减投资人基本户
		BaseResponse userAccountResponse = accountTransferService.subtractBalance(userAccount.getAccountNo(),
				publisherAccount.getAccountNo(), accountTransRequest.getBalance(), accountTransRequest.getOrderType(),
				accountTransRequest.getOrderNo(), accountTransRequest.getRequestNo(),"");
		if (!BaseResponse.isSuccess(userAccountResponse)) {
			log.info("申购-扣减投资人基本户操作订单异常：{}, userAccountResponse= {} ", accountTransRequest.getOrderNo(), JSONObject.toJSON(userAccountResponse));
			resp.setReturnCode(userAccountResponse.getReturnCode());
			resp.setErrorMessage(userAccountResponse.getErrorMessage());
			return resp;
		}
		
		// 2.增加发行人归集清算户
		BaseResponse baseResponse = accountTransferService.addBalance(userAccount.getAccountNo(),
				publisherAccount.getAccountNo(), accountTransRequest.getBalance(), accountTransRequest.getOrderType(),
				accountTransRequest.getOrderNo(), accountTransRequest.getRequestNo(),"");
		if (!BaseResponse.isSuccess(baseResponse)) {
			log.info("申购-增加发行人归集清算户操作订单异常：{}, baseResponse= {} ", accountTransRequest.getOrderNo(), JSONObject.toJSON(baseResponse));
			throw new SETException(baseResponse.getReturnCode());
		}
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("t1申购成功");

		return resp;
	}
	
	/**
	 * 续投解冻
	 * @param accountTransRequest
	 * @return
	 */
	@Transactional
	public BaseResponse continueUnFrozen(AccountTransRequest accountTransRequest) {
		String userOid = accountTransRequest.getUserOid();
		BaseResponse resp = new BaseResponse();
		// 获取投资人续投冻结户
		AccountInfoEntity continuFrozenAccount = accountInfoService.findAccountOrCreate(userOid,
				AccountTypeEnum.CONTINUED_INVESTMENT_FROZEN.getCode(), UserTypeEnum.INVESTOR.getCode());
		
		//解冻 投资人续投冻结金额， 即出-扣减投资人续投冻结户
		BaseResponse userAccountResponse = accountTransferService.subtractBalance(continuFrozenAccount.getAccountNo(),
				"", accountTransRequest.getBalance(), accountTransRequest.getOrderType(),
				accountTransRequest.getOrderNo(), accountTransRequest.getRequestNo(),"");
		if (!BaseResponse.isSuccess(userAccountResponse)) {
			log.info("申购-扣减投资人基本户操作订单异常：{}, userAccountResponse= {} ", accountTransRequest.getOrderNo(), JSONObject.toJSON(userAccountResponse));
			resp.setReturnCode(userAccountResponse.getReturnCode());
			resp.setErrorMessage(userAccountResponse.getErrorMessage());
			return resp;
		}
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("解冻成功");

		return resp;
	}
	
}
