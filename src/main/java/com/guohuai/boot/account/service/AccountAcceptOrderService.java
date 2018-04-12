package com.guohuai.boot.account.service;


import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountTransferRequest;
import com.guohuai.account.api.request.CreateOrderRequest;
import com.guohuai.account.api.request.entity.TradeEvent;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.account.component.util.EventTypeEnum;
import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.AccountEventChildEntity;
import com.guohuai.boot.account.entity.AccountEventTransEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.listener.event.AccountTransferEvent;
import com.guohuai.boot.account.res.AccountEventResponse;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.CodeConstants;
import com.guohuai.component.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: AccountAcceptOrderService
 * @Description:账户收单
 * @author chendonghui
 * @date 2018年1月22日9:56:32
 */
@Slf4j
@Service
public class AccountAcceptOrderService {
	
	@Autowired
	ApplicationEventPublisher eventPublisher;
	
	@Autowired
	private AccOrderService accOrderService;
	
	@Autowired
	private AccountEventAdapterService accountEventAdapterService;
	
	@Autowired
	private AccountEventTransService accountEventTransService;
	
	@Autowired
	private SeqGenerator seqGenerator;
	
	/**
	 * 账户收单并处理
	 * @param req 交易请求参数
	 * @return 交易结果
	 */
		public AccountTransResponse acceptOrder(AccountTransferRequest req) {
		log.info("账户接收定单 ：{}" + JSONObject.toJSONString(req));
		AccountTransResponse transResp = new AccountTransResponse();
		// 保存订单
		try{
			transResp = this.saveOrder(req);
		}catch(Exception e){
			transResp.setReturnCode(Constant.FAIL);
			transResp.setErrorMessage("系统异常");
			return transResp;
		}
		if(!Constant.SUCCESS.equals(transResp.getReturnCode())){
			log.info("账户接收定单，保存订单失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
			//订单已处理完成的不再更新订单状态
			if(!Constant.ACCOUNT_ORDER_EXISTS.equals(transResp.getReturnCode())){
				// 更新订单状态
				log.info("更新订单状态");
				accOrderService.updateOrderStatus(req.getOrderNo(), AccOrderEntity.ORDERSTATUS_FAIL);
			}
			return transResp;
		}
		// 触发事件校验及保存
		transResp = this.checkAndSaveEvent(req);
		if(!Constant.SUCCESS.equals(transResp.getReturnCode())){
			log.info("账户接收定单，获取并保存事件流水失败，返回信息 ：{}", JSONObject.toJSONString(transResp));
			// 更新订单状态
			log.info("更新订单状态");
			accOrderService.updateOrderStatus(req.getOrderNo(), AccOrderEntity.ORDERSTATUS_FAIL);
			return transResp;
		}
		// 组装事件Event
		AccountTransferEvent event = this.installEvent(req);
		eventPublisher.publishEvent(event);
		String orderStatus = AccOrderEntity.ORDERSTATUS_INIT;
		if(Constant.SUCCESS.equals(event.getReturnCode())){
			orderStatus = AccOrderEntity.ORDERSTATUS_SUCCESS;
		}else{
			orderStatus = AccOrderEntity.ORDERSTATUS_FAIL;
		}
		// 更新订单状态
		log.info("更新订单状态");
		accOrderService.updateOrderStatus(req.getOrderNo(), orderStatus);
		// 更新事件流水状态
		log.info("更新事件流水状态");
		accountEventTransService.updateEventTransStatus(req.getOrderNo(), orderStatus);

		BeanUtils.copyProperties(req, transResp);
		transResp.setOrderStatus(orderStatus);
		transResp.setReturnCode(event.getReturnCode());
		transResp.setErrorMessage(event.getErrorMessage());

		return transResp;
	}

	/**
	 * 触发事件校验及保存
	 * @param req 交易请求参数
	 * @return 事件流水校验及保存结果
	 */
	public AccountTransResponse checkAndSaveEvent(AccountTransferRequest req) {
		log.info("触发事件校验及保存");
		AccountTransResponse resp = new AccountTransResponse();
		resp.setReturnCode(Constant.SUCCESS);
		// 校验事件信息
		AccountEventResponse accountEventResp = new AccountEventResponse();
		List<AccountEventTransEntity> eventTransEntityList = new ArrayList<AccountEventTransEntity>();
		String status = AccountEventTransEntity.STATUS_INIT;
		for(TradeEvent tradeEvent : req.getEventList()){
			// 获取出入款账户信息
			log.info("获取出入款账户信息,事件信息：{}", JSONObject.toJSONString(tradeEvent));
			try{
				accountEventResp = accountEventAdapterService.accountEventAdapter(req, tradeEvent);
			}catch(Exception e){
				log.error("获取出入款账户信息异常:{}", e);
				resp.setErrorMessage("系统异常");
				resp.setReturnCode(Constant.FAIL);
				return resp;
			}
			if(!Constant.SUCCESS.equals(accountEventResp.getReturnCode())){
				log.error("获取出入款账户信息失败, 失败原因：{}", accountEventResp.getErrorMessage());
				status = AccountEventTransEntity.STATUS_FAIL;
				resp.setErrorMessage(accountEventResp.getErrorMessage());
				resp.setReturnCode(accountEventResp.getReturnCode());
				return resp;
			}
			//组装事件流水
			AccountEventTransEntity entity = this.installEventTrans(accountEventResp);
			entity.setStatus(status);
			if(!eventTransEntityList.isEmpty() && AccountEventTransEntity.STATUS_FAIL.equals(status)){
				eventTransEntityList.get(0).setStatus(status);
			}
			eventTransEntityList.add(entity);
			if(AccountTypeEnum.RESERVE.getCode().equals(entity.getOutputAccountType())){
				resp.setReservedAccountNo(entity.getOutputAccountNo());
			}
			if(AccountTypeEnum.RESERVE.getCode().equals(entity.getInputAccountType())){
				resp.setReservedAccountNo(entity.getInputAccountNo());
			}
		}
		// 保存eventTransList
		log.info("保存事件流水信息，共{}条", eventTransEntityList.size());
		accountEventTransService.saveEventTransList(eventTransEntityList);
		return resp;
	}

	/**
	 * 组装事件流水
	 * @param accountEventResp 出入款账户信息
	 * @return AccountEventTransEntity
	 */
	private AccountEventTransEntity installEventTrans(AccountEventResponse accountEventResp) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		//登帐事件请求信息
		TradeEvent tradeEvent = accountEventResp.getTradeEvent();
		//出账账户信息
		AccountInfoEntity outputAccountEntity = accountEventResp.getOutputAccountEntity();
		//入账账户信息
		AccountInfoEntity inputAccountEntity = accountEventResp.getInputAccountEntity();
		//登帐子事件
		AccountEventChildEntity accountEventChildEntity = accountEventResp.getAccountEventChildEntity();
		
		AccountEventTransEntity entity = new AccountEventTransEntity();
		entity.setBalance(tradeEvent.getBalance());
		entity.setChildEventName(accountEventChildEntity.getChildEventName());
		entity.setChildEventType(accountEventChildEntity.getChildEventType());
		entity.setEventChildOid(accountEventChildEntity.getOid());
		entity.setInputAccountName(inputAccountEntity.getAccountName());
		entity.setInputAccountNo(inputAccountEntity.getAccountNo());
		entity.setInputAccountType(inputAccountEntity.getAccountType());
		entity.setInputUserType(inputAccountEntity.getUserType());
		entity.setOrderNo(accountEventResp.getOrderNo());
		entity.setOrderType(accountEventResp.getOrderType());
		entity.setOutputAccountName(outputAccountEntity.getAccountName());
		entity.setOutputAccountNo(outputAccountEntity.getAccountNo());
		entity.setOutputAccountType(outputAccountEntity.getAccountType());
		entity.setOutputUserType(outputAccountEntity.getUserType());
		entity.setRemark(accountEventResp.getRemark());
		entity.setRequestNo(accountEventResp.getRequestNo());
		entity.setStatus(AccountEventTransEntity.STATUS_INIT);
		entity.setTransNo(seqGenerator.next(CodeConstants.EVENT_TRANS_PREFIX));
		entity.setCreateTime(time);
		entity.setUpdateTime(time);
		return entity;
	}

	/**
	 * 组装登账事件Event
	 * @param req 交易请求参数
	 * @return event
	 */
	private AccountTransferEvent installEvent(AccountTransferRequest req) {
		AccountTransferEvent event = new AccountTransferEvent();
		event.setOrderNo(req.getOrderNo());
		return event;
	}
	
	/**
	 * 组装保存订单参数并保存订单
	 * @param req 机器已请求参数
	 * @return 保存订单结果
	 */
	public AccountTransResponse saveOrder(AccountTransferRequest req) {
		AccountTransResponse resp = new AccountTransResponse();
		CreateOrderRequest orderReq = new CreateOrderRequest();
		orderReq.setOrderCreatTime(req.getOrderCreatTime());
		orderReq.setOrderDesc(req.getOrderDesc());
		orderReq.setOrderNo(req.getOrderNo());
		orderReq.setPublisherUserOid(req.getPublisherUserOid());
		orderReq.setRemark(req.getRemark());
		orderReq.setRequestNo(req.getRequestNo());
		orderReq.setSystemSource(req.getSystemSource());
		orderReq.setUserOid(req.getUserOid());
		orderReq.setOrderType(req.getOrderType());
		orderReq.setOrigOrderNo(req.getOrigOrderNo());
		orderReq.setRelationProductName(req.getRelationProductName());
		orderReq.setRelationProductNo(req.getRelationProductNo());
		for(TradeEvent tradeEvent : req.getEventList()){
			if(EventTypeEnum.USE_VOUCHER_T0.getCode().equals(tradeEvent.getEventType()) 
					|| EventTypeEnum.USE_VOUCHER_T1.getCode().equals(tradeEvent.getEventType()) 
					|| EventTypeEnum.REFUND_USE_VOUCHER_T0.getCode().equals(tradeEvent.getEventType())
					|| EventTypeEnum.REFUND_USE_VOUCHER_T1.getCode().equals(tradeEvent.getEventType()) ){
				// 卡券
				orderReq.setVoucher(tradeEvent.getBalance());
			}else if(EventTypeEnum.CHARGING_PLATFORM_FEE.getCode().equals(tradeEvent.getEventType())){
				// 平台服务费
				orderReq.setFee(tradeEvent.getBalance());
			}else if(EventTypeEnum.GRANT_RATE_COUPON_PROFIT.getCode().equals(tradeEvent.getEventType()) 
					|| EventTypeEnum.GRANT_RATE_COUPON_PROFIT_CONTINUED.getCode().equals(tradeEvent.getEventType())){
				// 加息券收益
				orderReq.setRateBalance(tradeEvent.getBalance());
			}else if(EventTypeEnum.UNFREEZE_CONTINUED.getCode().equals(tradeEvent.getEventType())){
				// 续投解冻
				orderReq.setContinUnfreezBalance(tradeEvent.getBalance());
			}else{
				orderReq.setBalance(tradeEvent.getBalance());
			}
		}
		resp = accOrderService.saveAccountOrder(orderReq);
		return resp;
	}
	
}