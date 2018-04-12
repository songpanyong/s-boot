package com.guohuai.boot.account.service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.*;
import com.guohuai.account.api.request.entity.AccountOrderDto;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.account.api.response.CreateOrderResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: AccountRedeemService
 * @Description: 账户赎回
 * @author CHENDONGHUI
 * @date 2017年6月6日 下午 13:39:22
 */
@Slf4j
@Service
public class AccountRedeemService {

	@Autowired
	private AccOrderService orderServive;
	@Autowired
	private AccOrderDao accOrderDao;
	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private AccountInfoService accountInfoService;
	@Autowired
	private AccountTradeService tradeService;
	@Autowired
	private SettlementSdk settlementSdk;
	@Autowired
	private CallBackDao callbackDao;
	@Autowired
	private UserInfoService userInfoService;
	@Autowired
	private InvestorRedeemService investorRedeemService;
	@Autowired
	private TransService transService;

	/**
	 * 赎回申请/赎回补单 创建并保存赎回订单
	 * 
	 * @param accountTransRequest
	 * @return
	 */
	public AccountTransResponse receiveRedeemApply(AccountTransRequest accountTransRequest) {
		log.info("账户接收赎回申请订单:[" + JSONObject.toJSONString(accountTransRequest) + "]");
		AccountTransResponse resp = new AccountTransResponse();
		CreateOrderRequest ordReq = new CreateOrderRequest();
		ordReq.setOrderNo(accountTransRequest.getOrderNo());
		ordReq.setRequestNo(accountTransRequest.getRequestNo());
		ordReq.setUserOid(accountTransRequest.getUserOid());
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
		CreateOrderResponse orderResp = new CreateOrderResponse();
		try {
			log.info("生成赎回交易定单");
			orderResp = orderServive.addAccOrder(ordReq);
			if(StringUtil.in(orderResp.getReturnCode(),Constant.SECONDTIME,Constant.REDEEM_SUCCESSED)){
				resp.setReturnCode(Constant.SUCCESS);
			}else {
				resp.setReturnCode(orderResp.getReturnCode());
			}
			resp.setErrorMessage(orderResp.getErrorMessage());
		} catch (Exception e) {
			log.error("系统繁忙,保存定单失败", e);
			resp.setErrorMessage("系统繁忙,保存定单失败");
			resp.setReturnCode(Constant.FAIL);
			return resp;
		}
		return resp;
	}

	/**
	 * 赎回撤单
	 */
	public AccountTransResponse cancelOrder(AccountTransRequest req) {
		log.info("账户撤单交易:[" + JSONObject.toJSONString(req) + "]");
		Timestamp nowTime = new Timestamp(System.currentTimeMillis());
		AccountTransResponse resp = new AccountTransResponse();
		resp.setReturnCode(Constant.SUCCESS);
		try {
			AccOrderEntity orderEntity = null;
			orderEntity = orderServive.getOrderByNo(req.getOrderNo());
			if (orderEntity != null) {
				if (OrderTypeEnum.REDEEM.getCode().equals(orderEntity.getOrderType())
						&& AccOrderEntity.ORDERSTATUS_INIT.equals(orderEntity.getOrderStatus())) {
					// 设为撤单
//					orderServive.updateOrderStatus(req.getOrderNo(), AccOrderEntity.ORDERSTATUS_KILL);
					orderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_KILL);
					orderEntity.setRemark(req.getRemark());
					orderEntity.setUpdateTime(nowTime);
					accOrderDao.save(orderEntity);
					resp.setReturnCode(Constant.SUCCESS);
					resp.setErrorMessage("成功");
					return resp;
				} else {
					log.info("原订单状态:{},原订单类型:{}", orderEntity.getOrderStatus(), orderEntity.getOrderType());
					resp.setReturnCode(Constant.FAIL);
					resp.setErrorMessage("订单状态或订单类型不匹配");
					return resp;
				}
			} else {
				log.info("撤单失败，订单不存在!");
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("订单不存在!");
				return resp;
			}
		} catch (Exception e) {
			log.error("撤单失败", e);
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("撤单失败");
			return resp;
		}
	}

	/**
	 * 赎回确认 
	 * 创建在途户,增加在途户余额,记录账户交易明细
	 * 
	 * @param accOrderEntity
	 */
	@Transactional
	public void confirmRedeem(AccOrderEntity accOrderEntity) {
		log.info("用户{},订单赎回{},赎回确认", accOrderEntity.getUserOid(), JSONObject.toJSONString(accOrderEntity));
		if (AccOrderEntity.ORDERSTATUS_INIT.equals(accOrderEntity.getOrderStatus())) {
			AccountInfoEntity accountInfoEntity = null;
			accountInfoEntity = accountInfoDao.findAccountByAccountTypeAndUserOid(AccountTypeEnum.ONWAY.getCode(),
					accOrderEntity.getUserOid());
			if (null == accountInfoEntity) {
				//创建投资人在途户
				accountInfoEntity = new AccountInfoEntity();
				CreateAccountRequest accountReq = new CreateAccountRequest();
				accountReq.setUserOid(accOrderEntity.getUserOid());
				accountReq.setAccountType(AccountTypeEnum.ONWAY.getCode());
				accountReq.setRelationProduct(accOrderEntity.getRelationProductNo());
				accountReq.setRelationProductName(accOrderEntity.getRelationProductName());
				accountReq.setUserType(UserTypeEnum.INVESTOR.getCode());
				accountInfoService.saveAccount(accountReq,AccountInfoEntity.STATUS_SUBMIT);
			}
			
			//查询在途户,修改在途户账户余额
			Timestamp nowTime = new Timestamp(System.currentTimeMillis());
			accountInfoEntity = accountInfoDao.findAccountByAccountTypeAndUserOid(AccountTypeEnum.ONWAY.getCode(), accOrderEntity.getUserOid());
			BigDecimal balance = accountInfoEntity.getBalance().add(accOrderEntity.getBalance());
			accountInfoEntity.setBalance(balance);
			accountInfoEntity.setUpdateTime(nowTime);
			accountInfoDao.save(accountInfoEntity);
			
			//记录账户交易明细
			CreateTransRequest transReq = new CreateTransRequest();
			transReq.setAccountOid(accountInfoEntity.getAccountNo());
			transReq.setUserOid(accOrderEntity.getUserOid());
			transReq.setUserType(accountInfoEntity.getUserType());
			transReq.setRequestNo(accOrderEntity.getRequestNo());
			transReq.setAccountOrderOid(accOrderEntity.getOrderNo());
			transReq.setOrderType(accOrderEntity.getOrderType());
			transReq.setSystemSource(accOrderEntity.getSystemSource());
			transReq.setOrderNo(accOrderEntity.getOrderNo());
			transReq.setRelationProductNo(accOrderEntity.getRelationProductNo());
			transReq.setInputAccountNo(accountInfoEntity.getAccountNo());
			transReq.setDirection("01");
			transReq.setOrderBalance(accOrderEntity.getBalance());
			transReq.setRamark("赎回确认");
			transReq.setOrderDesc("赎回确认记录在途户交易明细");
			transReq.setAccountName(accountInfoEntity.getAccountName());
			transReq.setDataSource(accOrderEntity.getSystemSource());
			transReq.setBalance(accOrderEntity.getBalance());
			// 入账，出账用户
			transReq.setInputAccountNo(accountInfoEntity.getAccountNo());
			transReq.setOutpuptAccountNo(accOrderEntity.getOutpuptAccountNo());
			// 财务入账标志
			transReq.setFinanceMark("");
			transReq.setVoucher(accOrderEntity.getVoucher());
			transReq.setIsDelete("");
			transReq.setCurrency("");
			tradeService.addAccountTrans(transReq);
			
			//修改订单状态为处理中
			orderServive.updateOrderStatus(accOrderEntity.getOrderNo(), AccOrderEntity.ORDERSTATUS_DEAL);
		} else {
			log.info("赎回确认失败,原订单状态为" + accOrderEntity.getOrderStatus() + ",订单状态不匹配");
			
		}
	}

	/**
	 * 批量赎回确认
	 * 
	 * @param reqOrderList
	 */
	public AccountTransResponse confirmRedeemList(List<CreateOrderRequest> reqOrderList) {
		log.info("账户批量赎回确认交易");
		Timestamp nowTime = new Timestamp(System.currentTimeMillis());
		AccountTransResponse resp = new AccountTransResponse();
		List<AccOrderEntity> orderList = new ArrayList<AccOrderEntity>();
		for(CreateOrderRequest reqorder : reqOrderList){
			AccOrderEntity entity = new AccOrderEntity();
			entity = accOrderDao.findOrderByOrderNo(reqorder.getOrderNo());
			if(null!=entity){
				entity.setRemark(reqorder.getRemark());
				entity.setCreateTime(nowTime);
				entity.setUpdateTime(nowTime);
				orderList.add(entity);
			}else{
				log.error("订单不存在,{}",JSONObject.toJSONString(entity));
			}
		}
		if (null != orderList && orderList.size() > 0) {
			for (AccOrderEntity accOrderEntity : orderList) {
				confirmRedeem(accOrderEntity);
			}
		}else{
			log.info("批量交易为空");
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("赎回确认订单为空");
		}
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("批量赎回确认交易");
		return resp;
	}

	/**
	 * 赎回投资者账户批量交易
	 * 修改订单状态
	 * 回调业务系统
	 * @param orderList
	 */
	public void investorRedeemList(List<AccOrderEntity> orderList,String batchTime) {
		log.info("账户赎回批量交易,批次号：{}",batchTime);
		if (null != orderList && orderList.size() > 0) {
			BaseResponse baseResp = new BaseResponse();
			int successCount = 0;
			for (AccOrderEntity accOrderEntity : orderList) {
				try{
					baseResp = investorRedeemService.investorRedeem(accOrderEntity);
					//查询赎回后用户余额
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
					accOrderDao.saveAndFlush(accOrderEntity);
					//回调业务系统
					try{
						this.callBack(accOrderEntity);
					}catch(Exception e){
						log.error("赎回确认回调异常，accOrderEntity={},error={}",JSONObject.toJSONString(accOrderEntity),e);
					}
				}else{
					accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
					accOrderEntity.setOrderDesc(baseResp.getErrorMessage());
					//更新订单状态
					accOrderDao.saveAndFlush(accOrderEntity);
				}
			}
			
			int failCount = orderList.size()-successCount;
			log.info("投资者批量赎回完成，批次号：{}，共{}条赎回单据，成功{}单，失败{}单",batchTime, orderList.size(), successCount, failCount);
		}else{
			log.info("账户赎回批量交易完成，无赎回单据！");
		}

	}
	
	/**
	 * 赎回发行人账户交易
	 * 
	 * @param req
	 * @return
	 */
	public AccountTransResponse redeem(AccountTransRequest req) {
		log.info("账户赎回交易:[" + JSONObject.toJSONString(req) + "]");
		AccountTransResponse resp = new AccountTransResponse();

		return resp;
	}

	
	/**
	 * 回调推送业务系统
	 */
	public void callBack(AccOrderEntity orderEntity){
		log.info("赎回交易回调，赎回订单{}",JSONObject.toJSONString(orderEntity));
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
			log.error(">>>>>>赎回回调信息[订单号orderNo="+orderEntity.getOrderNo()+"]已存在!<<<<<<");
		}else{
			info = CallBackInfo.builder().orderNO(orderResponse.getOrderNo()).tradeType(TradeTypeEnum.trade_redeem.getCode())
					.payNo(orderResponse.getPayNo()).channelNo("").type("settlement").minute(1)
					.totalCount(20).totalMinCount(20).countMin(0).returnCode(Constant.SUCCESS)
					.status(CallBackEnum.INIT.getCode()).returnMsg("交易成功").createTime(new Date())
					.build();
			try {
				log.info("赎回交易回调，{}", JSONObject.toJSONString(orderResponse));
				result = settlementSdk.callback(orderResponse);
			} catch (Exception e) {
				returnMsg = "推送交易信息异常";
				log.error(returnMsg + " OrderNO{},{}", orderResponse.getOrderNo(), e);
			}
			log.info("赎回交易回调结果，orderNo：{},result：{}", orderResponse.getOrderNo(), result);
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
	 * 批量创建订单
	 * @param req
	 * @return
	 */
	public CreateBatchAccountOrderRes creatBatchAccountOrder(
			AccountBatchRedeemRequest req) {
		CreateBatchAccountOrderRes resp = new CreateBatchAccountOrderRes();
		String publisherUserOid = req.getPublisherUserOid();
		String requestNo = req.getRequestNo();
		//参数校验
		if(publisherUserOid != null){
			//查询发行人是否存在
			UserInfoEntity userInfo = userInfoService.getAccountUserByUserOid(publisherUserOid);
			if (userInfo == null) {
				resp.setReturnCode(Constant.USERNOTEXISTS);
				resp.setErrorMessage("发行人不存在!");
				log.error("发行人不存在![userOid=" + publisherUserOid + "]");
				return resp;
			}
		}else{
			//发行人userOid不能为空
			resp.setReturnCode(Constant.REQUEST_USEROID_IS_NULL);
			resp.setErrorMessage("发行人userOid不能为空");
			log.info("发行人userOid不能为空!");
			return resp;
		}
		//请求批次号流水号
		if(requestNo != null){
			int requestCount = 0;
			requestCount = accOrderDao.finRequestCountByRequestNo(requestNo);
			if(requestCount >0){
				resp.setReturnCode(Constant.REQUESTNOEXISTS);
				resp.setErrorMessage("批量赎回订单，批次请求流水号已存在");
				log.info("批量赎回订单，批次请求流水号已存在!");
				return resp;
			}
		}else{
			resp.setReturnCode(Constant.REQUESTNO_IS_NULL);
			resp.setErrorMessage("发批量赎回订单，批次请求流水号不能为空");
			log.info("批量赎回订单，批次请求流水号不能为空!!");
			return resp;
		}
		//保存订单
		if(req.getOrderList() != null&&req.getOrderList().size()>0){
			//批量保存订单
			resp = this.saveBatchOrderAndFrozenAccount(req.getOrderList(), 
					requestNo, publisherUserOid, req.getSystemSource());
		}else{
			resp.setReturnCode(Constant.BATCH_REDEEM_ORDER_IS_NULL);
			resp.setErrorMessage("批量赎回订单不存在");
			log.info("批量赎回订单不存在!!");
			return resp;
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
			String phone = "";
			if(dto.getUserOid() != null){
				//查询用户信息
				UserInfoEntity userInfo = userInfoService.getAccountUserByUserOid(dto.getUserOid());
				if (userInfo != null) {
					phone = userInfo.getPhone();
				}
			}
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
			if(StringUtil.isEmpty(dto.getUserType())){
				orderEntity.setUserType("T1");
			}else{
				orderEntity.setUserType(dto.getUserType());
			}
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
			orderEntity.setPhone(phone);
			accOrderEntityList.add(orderEntity);
			orderBalance = orderBalance.add(dto.getBalance());
		}
		//查询发行人冻结户是否够本批次赎回
		AccountInfoEntity frozenAccount = accountInfoDao.findAccountByAccountTypeAndUserOid(
				AccountTypeEnum.REDEEMFROZEN.getCode(), publisherUserOid);
		if(frozenAccount == null){
			log.error("发行人{}资金冻结账户不存在!",publisherUserOid);
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("发行人资金冻结账户不存在");
			return resp;
		}
		if(frozenAccount.getBalance().compareTo(orderBalance) < 0){
			log.error("发行人{}资金冻结账户余额不足!",publisherUserOid);
			resp.setReturnCode(Constant.BALANCELESS);
			resp.setErrorMessage("发行人资金冻结账户余额不足，请确认订单是否已完成结算");
			return resp;
		}
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");
		accOrderDao.save(accOrderEntityList);
		resp.setAccOrderEntityList(accOrderEntityList);
		return resp;
	}
	
	/**
	 * <pre>
	 * T+1 赎回
	 * 从发行人冻结户，转账到用户基本户，并记录冻结金额,用于续投或者转换
	 * T+0 赎回
	 * 从发行人可用金户，转账到用户基本户，并记录冻结金额,用于续投或者转换
	 * </pre>
	 * @param accountTransRequest remark=T0/T1
	 * @return
	 */
	public AccountTransResponse redeemApplyDay(AccountTransRequest accountTransRequest) {
		log.info("账户接收赎回申请订单:[" + JSONObject.toJSONString(accountTransRequest) + "]");
		AccountTransResponse resp = new AccountTransResponse();
		BeanUtils.copyProperties(accountTransRequest,resp);
		//验证交易参数
		resp = this.checkRedeemAccountTrans(accountTransRequest);
		if (!resp.getReturnCode().equals(Constant.SUCCESS)) {
			return resp;
		}
		AccOrderEntity accOrderEntity=null;
		accOrderEntity = orderServive.getOrderByNo(accountTransRequest.getOrderNo());
		if(null==accOrderEntity) {
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
			try {
				accOrderEntity = orderServive.saveAccOrder(ordReq);
			} catch (Exception e) {
				log.error("系统繁忙,保存定单失败", e);
				resp.setErrorMessage("系统繁忙,保存定单失败");
				resp.setReturnCode(Constant.FAIL);
				return resp;
			}
		}else {
			log.info("定单已存在，失败重发 orderNo:{}", accountTransRequest.getOrderNo());
			if (AccOrderEntity.ORDERSTATUS_SUCCESS.equals(accOrderEntity.getOrderStatus())) {
				resp.setErrorMessage("定单续投申请已成功，不允许重复发起");
				resp.setReturnCode(Constant.FAIL);
				return resp;
			}
		}
		try {
			BaseResponse baseResponse=investorRedeemService.investorRedeemDay(accountTransRequest, accOrderEntity.getOid());
			log.info("定单{}，赎回返回 baseResponse：{}",accountTransRequest.getOrderNo(),JSONObject.toJSON(baseResponse));
			if(BaseResponse.isSuccess(baseResponse)){
				accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_SUCCESS);
			}else{
				accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
			}
			resp.setErrorMessage(baseResponse.getErrorMessage());
			resp.setReturnCode(baseResponse.getReturnCode());
		} catch (SETException e) {
			log.error("赎回操作异常", e);
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
			resp.setReturnCode(e.getMessage());
		} catch (Exception e) {
			log.error("转换赎回异常", e);
			accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
			resp.setErrorMessage("转换赎回异常");
			resp.setReturnCode(Constant.FAIL);
		}
		accOrderEntity.setUpdateTime(new Timestamp(System.currentTimeMillis()));
		accOrderDao.save(accOrderEntity);
		return resp;
	}
	
	/**
	 * 验证赎回参数
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
			if (AccOrderEntity.ORDERSTATUS_SUCCESS.equals(order.getOrderStatus())) {
				resp.setErrorMessage("定单续投申请已成功，不允许重复发起");
				resp.setReturnCode(Constant.FAIL);
				return resp;
			}
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
		if(null == accountTransRequest.getBalance()){
			//赎回金额不能为空
			resp.setReturnCode(Constant.CONTINU_BALANCE_IS_NULL);
			resp.setErrorMessage("赎回金额不能为空");
			return resp;
		}
		if(BigDecimal.ZERO.compareTo(accountTransRequest.getBalance()) == 1){
			//赎回金额不能为负
			resp.setReturnCode(Constant.CONTINU_BALANCE_IS_MINUS);
			resp.setErrorMessage("赎回金额不能为负");
			return resp;
		}
		if(null == accountTransRequest.getFrozenBalance()){
			//续投金额不能为空
			resp.setReturnCode(Constant.CONTINU_BALANCE_IS_NULL);
			resp.setErrorMessage("续投金额不能为空");
			return resp;
		}
		if(BigDecimal.ZERO.compareTo(accountTransRequest.getFrozenBalance()) == 1){
			//续投金额不能为负
			resp.setReturnCode(Constant.CONTINU_BALANCE_IS_MINUS);
			resp.setErrorMessage("续投金额不能为负");
			return resp;
		}
		return resp;
	}

}
