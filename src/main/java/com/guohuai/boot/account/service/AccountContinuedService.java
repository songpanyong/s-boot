package com.guohuai.boot.account.service;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountTransRequest;
import com.guohuai.account.api.request.CreateOrderRequest;
import com.guohuai.account.api.response.AccountBalanceResponse;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.component.exception.SETException;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.RedeemTypeEnum;
import com.guohuai.settlement.api.response.BaseResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName: AccountContinuedService
 * @Description: 账户续投
 * @author hugo
 * @date 2017年12月1日15:06:47
 */
@Slf4j
@Service
public class AccountContinuedService {

	@Autowired
	private AccOrderService orderServive;
	@Autowired
	private AccOrderDao accOrderDao;
	@Autowired
	private UserInfoService userInfoService;
	@Autowired
	private InvestorContinuedService investorContinuedService;
	@Autowired
	private TransService transService;
	@Autowired
	private AccountInfoService accountInfoService;

	/**
	 * 续投服务入口
	 * <pre>
	 * 1.续投验证、2.扣减续投冻结户金额、3.申购
	 * </pre>
	 * @param accountTransRequest remark=T0/T1
	 * @return
	 */
	public AccountTransResponse accountContinuInvest(AccountTransRequest accountTransRequest) {
		log.info("账户续投申请订单:[" + JSONObject.toJSONString(accountTransRequest) + "]");
		AccountTransResponse resp = new AccountTransResponse();
		
		//验证交易参数
		resp = this.checkRedeemAccountTrans(accountTransRequest);
		if (!resp.getReturnCode().equals(Constant.SUCCESS)) {
			return resp;
		}
		
		CreateOrderRequest ordReq = new CreateOrderRequest();
		ordReq.setOrderNo(accountTransRequest.getOrderNo());
		ordReq.setRequestNo(accountTransRequest.getRequestNo());
		ordReq.setUserOid(accountTransRequest.getUserOid());
		ordReq.setPublisherUserOid(accountTransRequest.getPublisherUserOid());
		ordReq.setOrderType(accountTransRequest.getOrderType());
		ordReq.setProductType(accountTransRequest.getProductType());
		ordReq.setRelationProductNo(accountTransRequest.getRelationProductNo());
		ordReq.setOutputRelationProductNo(accountTransRequest.getOutputRelationProductNo());
		ordReq.setOutputRelationProductName(accountTransRequest.getOutputRelationProductName());
		ordReq.setBalance(accountTransRequest.getBalance());
		ordReq.setVoucher(accountTransRequest.getVoucher()); // 代金券
		ordReq.setSystemSource(accountTransRequest.getSystemSource());
		ordReq.setRemark(accountTransRequest.getRemark());
		ordReq.setOrderDesc(accountTransRequest.getOrderDesc());
		ordReq.setOrderCreatTime(accountTransRequest.getOrderCreatTime());
		ordReq.setFee(accountTransRequest.getFee());
		ordReq.setFrozenBalance(accountTransRequest.getFrozenBalance());
		AccOrderEntity accOrderEntity=null;
		try {
			accOrderEntity = orderServive.saveAccOrder(ordReq);
		} catch (Exception e) {
			log.error("系统繁忙,保存定单失败", e);
			resp.setErrorMessage("系统繁忙,保存定单失败");
			resp.setReturnCode(Constant.FAIL);
			return resp;
		}
		try {
			BaseResponse baseResponse=investorContinuedService.investorContinue(accountTransRequest, accOrderEntity.getOid());
			log.info("定单{}，续投返回 baseResponse：{}",accountTransRequest.getOrderNo(),JSONObject.toJSON(baseResponse));
			if(BaseResponse.isSuccess(baseResponse)){
				accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_SUCCESS);
			}else{
				accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
			}
			resp.setErrorMessage(baseResponse.getErrorMessage());
			resp.setReturnCode(baseResponse.getReturnCode());
		} catch (SETException e) {
			log.error("续投操作异常", e);
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
			accOrderEntity.setUpdateTime(new Timestamp(System.currentTimeMillis()));
			resp.setReturnCode(e.getMessage());
		} catch (Exception e) {
			log.error("转换续投异常", e);
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
			accOrderEntity.setUpdateTime(new Timestamp(System.currentTimeMillis()));
			resp.setErrorMessage("转换续投异常");
			resp.setReturnCode(Constant.FAIL);
		}
		accOrderDao.save(accOrderEntity);
		return resp;
	}
	
	/**
	 * 验证续投参数
	 * @param accountTransRequest
	 * @return
	 */
	public AccountTransResponse checkRedeemAccountTrans(AccountTransRequest accountTransRequest){
		AccountTransResponse resp = transService.checkAccountTransRequest(accountTransRequest);
		if (!resp.getReturnCode().equals(Constant.SUCCESS)) {
			return resp;
		}
		if (StringUtil.isEmpty(accountTransRequest.getOrderNo())) {
			// 订单号不能为空
			resp.setReturnCode(Constant.ACCOUNT_ORDER_IS_NULL);
			resp.setErrorMessage("订单号不能为空！");
			return resp;
		}
		AccOrderEntity order = orderServive.getOrderByNo(accountTransRequest.getOrderNo());
		if (null != order) {
			// 订单号已存在
			resp.setReturnCode(Constant.ACCOUNT_ORDER_EXISTS);
			resp.setErrorMessage("订单号已存在！");
			log.error("订单号已存在，[orderNo=" + accountTransRequest.getOrderNo() + "]");
			return resp;
		}
		if (StringUtil.isEmpty(accountTransRequest.getOldOrderNo())) {
			// 原订单号不能为空
			resp.setReturnCode(Constant.ACCOUNT_ORDER_IS_NULL);
			resp.setErrorMessage("原订单号不能为空！");
			return resp;
		}
		AccOrderEntity oldOrder = orderServive.getOrderByNo(accountTransRequest.getOldOrderNo());
		if (null == oldOrder) {
			// 原订单号不存在
			resp.setReturnCode(Constant.ORDERNOEXISTS);
			resp.setErrorMessage("原订单号不存在！");
			log.error("原订单号不存在，[orderNo=" + accountTransRequest.getOldOrderNo() + "]");
			return resp;
		}
		// 判断用户是否存在
		UserInfoEntity publishUserInfo = userInfoService.getAccountUserByUserOid(accountTransRequest.getPublisherUserOid());
		if (publishUserInfo == null) {
			resp.setReturnCode(Constant.REQUEST_USEROID_IS_NULL);
			resp.setErrorMessage("发行人不存在!");
			log.error("发行人不存在![publishUserInfo=" + accountTransRequest.getPublisherUserOid() + "]");
			return resp;
		}
		if(StringUtil.isEmpty(accountTransRequest.getRemark())){
			resp.setReturnCode(Constant.ORDERTYPENOT_IS_NULL);
			resp.setErrorMessage("交易类型不能为空!");
			log.error("交易类型不能为空![remark=" + accountTransRequest.getRemark() + "]");
			return resp;
		}
		if(!RedeemTypeEnum.REDEEMT0.getCode().equals(accountTransRequest.getRemark()) && !RedeemTypeEnum.REDEEMT1.getCode().equals(accountTransRequest.getRemark())){
			resp.setReturnCode(Constant.ORDERTYPENOTEXISTS);
			resp.setErrorMessage("交易类型不存在!");
			log.error("交易类型不存在![remark=" + accountTransRequest.getRemark() + "]");
			return resp;
		}
		AccOrderEntity redeemOrder = orderServive.getOrderByNo(accountTransRequest.getOldOrderNo());
		if(!redeemOrder.getUserOid().equals(accountTransRequest.getUserOid())){
			//订单对应的用户不匹配
			resp.setReturnCode(Constant.ORDER_USER_MISMATCH);
			resp.setErrorMessage("订单对应的用户不匹配");
			return resp;
		}
		if(null == accountTransRequest.getBalance()){
			//续投金额不能为空
			resp.setReturnCode(Constant.CONTINU_BALANCE_IS_NULL);
			resp.setErrorMessage("续投金额不能为空");
			return resp;
		}
		if(BigDecimal.ZERO.compareTo(accountTransRequest.getBalance()) == 1){
			//续投金额不能为负
			resp.setReturnCode(Constant.CONTINU_BALANCE_IS_MINUS);
			resp.setErrorMessage("续投金额不能为负");
			return resp;
		}
		int res = redeemOrder.getFrozenBalance().compareTo(accountTransRequest.getBalance());
		if(res != 0){
			//订单对应的续投金额不匹配
			resp.setReturnCode(Constant.ORDER_CONTINU_BALANCE_MISMATCH);
			resp.setErrorMessage("订单对应的续投金额不匹配");
			return resp;
		}
		//投资人基本户余额必须>= 订单金额
		AccountBalanceResponse balanceResp = accountInfoService.getAccountBalanceByUserOid(accountTransRequest.getUserOid());
		if(balanceResp.getBalance().compareTo(accountTransRequest.getBalance()) == -1){
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("投资人基本户余额不足");
			log.info("投资人基本户余额不足![applyAvailableBalance={}]", balanceResp.getBalance());
			return resp;
		}
		//投资人续投冻结户余额必须>= 订单金额
		if(balanceResp.getContinuedInvestmentFrozenBalance().compareTo(accountTransRequest.getBalance()) == -1){
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("投资人续投冻结户余额不足");
			log.info("投资人续投冻结户余额不足![applyAvailableBalance={}]", balanceResp.getContinuedInvestmentFrozenBalance());
			return resp;
		}
		
		return resp;
	}
	
	/**
	 * 续投解冻
	 * @param accountTransRequest
	 * @return
	 */
	public AccountTransResponse accountContinuUnFrozen(AccountTransRequest accountTransRequest) {
		log.info("续投解冻订单:[" + JSONObject.toJSONString(accountTransRequest) + "]");
		AccountTransResponse resp = new AccountTransResponse();
		resp = this.checkUnFrozenBalance(accountTransRequest);
		try {
			BaseResponse baseResponse=investorContinuedService.continueUnFrozen(accountTransRequest);
			log.info("续投解冻定单{}，返回 baseResponse：{}",accountTransRequest.getOrderNo(),JSONObject.toJSON(baseResponse));
			resp.setErrorMessage(baseResponse.getErrorMessage());
			resp.setReturnCode(baseResponse.getReturnCode());
		} catch (SETException e) {
			log.error("续投解冻操作异常", e);
			resp.setReturnCode(e.getMessage());
		} catch (Exception e) {
			log.error("续投解冻异常", e);
			resp.setErrorMessage("续投解冻异常");
			resp.setReturnCode(Constant.FAIL);
		}
		return resp;
	}
	
	/**
	 * 验证续投解冻参数
	 * @param accountTransRequest
	 * @return
	 */
	public AccountTransResponse checkUnFrozenBalance(AccountTransRequest accountTransRequest){
		AccountTransResponse resp = transService.checkAccountTransRequest(accountTransRequest);
		if (!resp.getReturnCode().equals(Constant.SUCCESS)) {
			return resp;
		}
		if (StringUtil.isEmpty(accountTransRequest.getOrderNo())) {
			// 订单号不能为空
			resp.setReturnCode(Constant.ACCOUNT_ORDER_IS_NULL);
			resp.setErrorMessage("订单号不能为空！");
			return resp;
		}
		AccOrderEntity order = orderServive.getOrderByNo(accountTransRequest.getOrderNo());
		if (null != order) {
			// 订单号已存在
			resp.setReturnCode(Constant.ACCOUNT_ORDER_EXISTS);
			resp.setErrorMessage("订单号已存在！");
			log.error("订单号已存在，[orderNo=" + accountTransRequest.getOrderNo() + "]");
			return resp;
		}
		if (StringUtil.isEmpty(accountTransRequest.getOldOrderNo())) {
			// 原订单号不能为空
			resp.setReturnCode(Constant.ACCOUNT_ORDER_IS_NULL);
			resp.setErrorMessage("原订单号不能为空！");
			return resp;
		}
		AccOrderEntity oldOrder = orderServive.getOrderByNo(accountTransRequest.getOldOrderNo());
		if (null == oldOrder) {
			// 原订单号不存在
			resp.setReturnCode(Constant.ORDERNOEXISTS);
			resp.setErrorMessage("原订单号不存在！");
			log.error("原订单号不存在，[orderNo=" + accountTransRequest.getOldOrderNo() + "]");
			return resp;
		}
		if (AccOrderEntity.ORDERSTATUS_SUCCESS.equals(oldOrder.getOrderStatus())) {
			// 原申购订单只有在失败的情况下才能解冻
			resp.setReturnCode(Constant.ORDERSTATUSERROR);
			resp.setErrorMessage("续投解冻失败，已申购成功订单不能解冻！");
			log.error("续投解冻失败，已申购成功订单不能解冻，[orderNo={}, orderStatus={}]", accountTransRequest.getOldOrderNo(), oldOrder.getOrderStatus());
			return resp;
		}
		if(StringUtil.isEmpty(accountTransRequest.getRemark())){
			resp.setReturnCode(Constant.ORDERTYPENOT_IS_NULL);
			resp.setErrorMessage("交易类型不能为空!");
			log.error("交易类型不能为空![remark=" + accountTransRequest.getRemark() + "]");
			return resp;
		}
		if(!RedeemTypeEnum.REDEEMT0.getCode().equals(accountTransRequest.getRemark()) && !RedeemTypeEnum.REDEEMT1.getCode().equals(accountTransRequest.getRemark())){
			resp.setReturnCode(Constant.ORDERTYPENOTEXISTS);
			resp.setErrorMessage("交易类型不存在!");
			log.error("交易类型不存在![remark=" + accountTransRequest.getRemark() + "]");
			return resp;
		}
		AccOrderEntity redeemOrder = orderServive.getOrderByNo(accountTransRequest.getOldOrderNo());
		if(!redeemOrder.getUserOid().equals(accountTransRequest.getUserOid())){
			//订单对应的用户不匹配
			resp.setReturnCode(Constant.ORDER_USER_MISMATCH);
			resp.setErrorMessage("订单对应的用户不匹配");
			return resp;
		}
		if(null == accountTransRequest.getBalance()){
			//续投金额不能为空
			resp.setReturnCode(Constant.CONTINU_BALANCE_IS_NULL);
			resp.setErrorMessage("续投金额不能为空");
			return resp;
		}
		if(BigDecimal.ZERO.compareTo(accountTransRequest.getBalance()) == 1){
			//续投金额不能为负
			resp.setReturnCode(Constant.CONTINU_BALANCE_IS_MINUS);
			resp.setErrorMessage("续投金额不能为负");
			return resp;
		}
		int res = redeemOrder.getFrozenBalance().compareTo(accountTransRequest.getBalance());
		if(res != 0){
			//订单对应的续投金额不匹配
			resp.setReturnCode(Constant.ORDER_CONTINU_BALANCE_MISMATCH);
			resp.setErrorMessage("订单对应的续投金额不匹配");
			return resp;
		}
		//续投冻结余额必须>= 订单金额
		AccountBalanceResponse balanceResp = accountInfoService.getAccountBalanceByUserOid(accountTransRequest.getUserOid());
		if(balanceResp.getContinuedInvestmentFrozenBalance().compareTo(accountTransRequest.getBalance()) == -1){
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("申购可用金额不足");
			log.info("申购可用金额不足![applyAvailableBalance={}]", balanceResp.getApplyAvailableBalance());
			return resp;
		}
		
		return resp;
	}

}
