package com.guohuai.boot.pay.service;

import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.dao.TransDao;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.entity.TransEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.pay.dao.ComOrderDao;
import com.guohuai.boot.pay.dao.PaymentDao;
import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.boot.pay.vo.PaymentVo;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.OrderTypeEnum;
import com.guohuai.component.util.TradeTypeEnum;
import com.guohuai.payadapter.listener.event.TradeRecordEvent;
import com.guohuai.settlement.api.request.DepositConfirmRequest;
import com.guohuai.settlement.api.response.DepositConfirmResponse;
import com.guohuai.settlement.api.response.OrderResponse;


/**
 * 单笔充值实时对账
 * 
 */
@Service
@Slf4j
public class ComOrderConfirmService {
	@Autowired
	private ComOrderDao comOrderDao;
	@Autowired
	private PaymentDao paymentDao;
	@Autowired
	private ApplicationEventPublisher publish;
	@Autowired
	AccOrderDao accOrderDao;
	@Autowired
	TransDao transDao;
	@Autowired
	AccountInfoDao accountInfoDao;
	@Autowired
	UserInfoDao userDao;
	@Autowired
	private ReconciliationRechargeService reconciliationRechargeService;
	
	/**
	 * 单笔充值实时对账
	 * 
	 * @param req
	 * @return
	 */
	public DepositConfirmResponse depositConfirm(DepositConfirmRequest req) {
		log.info("OrderNo{},接收订单,{}", req.getOrderNo(), JSONObject.toJSONString(req));
		DepositConfirmResponse response = new DepositConfirmResponse();
		UserInfoEntity user = userDao.findByUserOid(req.getUserOid());
		if(user == null){
			response.setReturnCode(Constant.SUCCESS);
			response.setErrorMessage("用户{"+req.getUserOid()+"} 的不存在!");
			return response;
		}
		
		OrderVo orderVo = comOrderDao.findByorderNo(req.getOrderNo());
		PaymentVo paymentVo = paymentDao.findByOrderNo(req.getOrderNo());
		if (null == orderVo) {
			response.setReturnCode(Constant.SUCCESS);
			response.setErrorMessage("支付订单不存在!");
			return response;
		}
		if(null == paymentVo){
			response.setReturnCode(Constant.SUCCESS);
			response.setErrorMessage("订单已成功交易!");
			response.setStatus("F");// 订单状态失败
			return response;
		}
		if ("1".equals(orderVo.getStatus())) {
			response.setReturnCode(Constant.SUCCESS);
			response.setErrorMessage("订单已成功交易!");
			response.setStatus("S");// 返回成功状态
			return response;
		}
		if (!"01".equals(orderVo.getType())) {
			response.setReturnCode(Constant.SUCCESS);
			response.setErrorMessage("订单为非充值订单!");
			return response;
		}
		if (!"2".equals(orderVo.getStatus())) {
			response.setReturnCode(Constant.SUCCESS);
			response.setErrorMessage("订单处理中，请稍后再试!");
			return response;
		}
		
		if(paymentVo.getChannelNo() == null){
			response.setReturnCode(Constant.SUCCESS);
			response.setErrorMessage("订单异常，请联系运维人员!");
			return response;
		}
		/**
		 * 调第三方接口查询交易状态
		 */
		TradeRecordEvent event = new TradeRecordEvent();
		event.setChannel(paymentVo.getChannelNo());
		event.setOrderNo(paymentVo.getPayNo());
		publish.publishEvent(event);
		if (Constant.SUCCESS.equals(event.getReturnCode())) {
			log.info("第三方订单状态为成功!");
//			map = queryAccountOrder(orderVo.getOrderNo(),basicAccountInfo,rechAccountInfo);
			//充值三方查询成功，结算订单失败，实时处理该订单
			response = this.dealRechageFailToSucc(orderVo, paymentVo);
		} else if (Constant.FAIL.equals(event.getReturnCode())) {
			log.info("第三方订单状态为失败!");
			response.setReturnCode(Constant.SUCCESS);
			response.setErrorMessage("订单已成功交易!");
			response.setStatus("F");// 订单状态失败
		} else {
			log.info("第三方订单状态为未知!");
			response.setReturnCode(Constant.SUCCESS);
			response.setErrorMessage("查询订单状态失败!");
		}
		return response;
	}

	/**
	 * 冲正处理先失败后成功的充值订单
	 * @param orderVo 收单信息
	 * @param paymentVo 指令信息
	 * @return 处理结果
	 */
	@Transactional
	private DepositConfirmResponse dealRechageFailToSucc(OrderVo orderVo,
			PaymentVo paymentVo) {
		DepositConfirmResponse response = new DepositConfirmResponse();
		//处理账户余额修复
		OrderResponse resp = reconciliationRechargeService.rechargeRecon(
				paymentVo.getOrderNo(), "1", paymentVo.getCommandStatus());
		if(Constant.SUCCESS.equals(resp.getReturnCode())){
			//修改指令及订单状态
			log.info("处理账户余额成功，修复结算订单状态");
			orderVo.setStatus("1");
			paymentVo.setCommandStatus("1");
			comOrderDao.saveAndFlush(orderVo);
			paymentDao.saveAndFlush(paymentVo);
			response.setReturnCode(Constant.SUCCESS);
			response.setErrorMessage("订单已成功交易!");
			response.setStatus("S");// 订单状态成功
		}else{
			response.setReturnCode(Constant.SUCCESS);
			response.setErrorMessage("处理订单账户余额失败，请联系运维人员");
			response.setStatus("F");// 订单状态失败
		}		
		return response;
	}

	@Transactional
	private Map<String, String> queryAccountOrder(String orderNo,AccountInfoEntity accountEntityBasic,AccountInfoEntity accountFrozenEntity) {
		log.info("查询账户订单状态：账户订单号 {}", orderNo);
		Map<String, String> map = new HashMap<String, String>();
		AccOrderEntity orderEntity = accOrderDao.findOrderByOrderNoAndOrderType(orderNo,OrderTypeEnum.RECHARGE.getCode());
		log.info("账户订单：AccOrderEntity  {}", JSONObject.toJSONString(orderEntity));
		// 订单不存在，返回，不操作账户
		if (orderEntity == null) {
			log.error("**********账户订单不存在!**********");
			map.put("returnCode", Constant.SUCCESS);
			map.put("errorMessage", "账户订单不存在");
			return map;
		}
		// 账户的订单是成功状态,不操作账户，修改结算订单状态
		if (AccOrderEntity.ORDERSTATUS_SUCCESS.equals(orderEntity.getOrderStatus())) {
			map.put("returnCode", Constant.SUCCESS);
			map.put("errorMessage", "订单已成功记过账!");
			map.put("status", "S");
			// 修改结算的订单状态
			paymentDao.updateStatusByOrderNo(AccOrderEntity.ORDERSTATUS_SUCCESS, orderNo);
			comOrderDao.updateStatusByOrderNo(AccOrderEntity.ORDERSTATUS_SUCCESS, orderNo);
			return map;
		}
		// 账户的状态是失败，操作账户，修改基本户和充值冻结户余额
		if (AccOrderEntity.ORDERSTATUS_FAIL.equals(orderEntity.getOrderStatus())) {
			TransEntity transEntity = transDao.findFirstByOrderNoAndUserOid(orderNo, orderEntity.getUserOid());
			if (transEntity != null) {
				log.error("**********账户失败订单，存在账户交易流水!**********");
				map.put("returnCode", Constant.FAIL);
				map.put("errorMessage", "账户失败订单，存在账户交易流水!");
				return map;
			}

			
			// 新增账户流水
			List<TransEntity> list = new ArrayList<TransEntity>();
			transEntity = new TransEntity();
			BeanUtils.copyProperties(orderEntity, transEntity);
			transEntity.setOrderBalance(orderEntity.getBalance());
			transEntity.setBalance(accountEntityBasic.getBalance().add(orderEntity.getBalance()));
			transEntity.setAccountOrderOid(orderEntity.getOid());
			transEntity.setAccountOid(accountEntityBasic.getAccountNo());
			transEntity.setInputAccountNo(accountEntityBasic.getAccountNo());
			transEntity.setDirection(TradeTypeEnum.trade_pay.getCode());
			transEntity.setDataSource(orderEntity.getSystemSource());
			transEntity.setRamark("充值");
			transEntity.setOrderDesc("单笔充值实时对账");
			transEntity.setTransTime(orderEntity.getReceiveTime());
			transEntity.setCreateTime(new Timestamp(new Date().getTime()));
			transEntity.setUpdateTime(new Timestamp(new Date().getTime()));
			log.info("充值基本户流水 {}",JSON.toJSONString(transEntity));
			list.add(transEntity);
			
			TransEntity transEntityFrozen = new TransEntity();
			BeanUtils.copyProperties(transEntity, transEntityFrozen);
			transEntityFrozen.setBalance(accountFrozenEntity.getBalance().add(orderEntity.getBalance()));
			transEntity.setAccountOid(accountFrozenEntity.getAccountNo());
			transEntityFrozen.setInputAccountNo(accountFrozenEntity.getAccountNo());
			log.info("充值冻结户流水 {}",JSON.toJSONString(accountFrozenEntity));
			list.add(transEntityFrozen);
			transDao.save(list);

			// 修改基本户和充值冻结户余额
			accountInfoDao.updateBalance(orderEntity.getBalance(), accountEntityBasic.getAccountNo());
			accountInfoDao.updateBalance(orderEntity.getBalance(), accountFrozenEntity.getAccountNo());

			// 修改帐户订单状态
			accOrderDao.updateOrderStatus(orderNo, AccOrderEntity.ORDERSTATUS_SUCCESS);
			// 修改结算的订单状态
			paymentDao.updateStatusByOrderNo(AccOrderEntity.ORDERSTATUS_SUCCESS, orderNo);
			comOrderDao.updateStatusByOrderNo(AccOrderEntity.ORDERSTATUS_SUCCESS, orderNo);
			map.put("returnCode", Constant.SUCCESS);
			map.put("errorMessage", "修改订单状态成功!");
			map.put("status", "S");
		}
		return map;
	}

}