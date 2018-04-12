package com.guohuai.seetlement.task;

import java.sql.Timestamp;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.pay.dao.BankLogDao;
import com.guohuai.boot.pay.dao.ComOrderDao;
import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.component.util.OrderTypeEnum;
import com.guohuai.payadapter.component.CallBackEnum;
import com.guohuai.payadapter.redeem.CallBackDao;
import com.guohuai.payadapter.redeem.CallBackDaoLog;
import com.guohuai.payadapter.redeem.CallBackInfo;
import com.guohuai.payadapter.redeem.CallBackLog;
import com.guohuai.settlement.api.SettlementSdk;
import com.guohuai.settlement.api.response.OrderResponse;

@Slf4j
@Component
public class CallbackSyncService {
	
	@Autowired
	CallBackDao callbackDao;
	@Autowired
	CallBackDaoLog callBackDaoLog;
	@Autowired
	BankLogDao bankLogDao;
	@Autowired
	ComOrderDao comOrderDao;
	@Autowired
	AccOrderDao accOrderDao;

	@Autowired
	SettlementSdk settlementSdk;
	
	@Async("settlmentCallbackExecutor")
	public void callbackSync(CallBackInfo callBackInfo,String type) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		// 先按秒执行查询，执行完成后无结果按分钟执行
		if (callBackInfo.getCount() >= callBackInfo.getTotalCount()) {

			if (callBackInfo.getCountMin() >= callBackInfo.getTotalMinCount()) {
				log.info("按分钟查询代付结果执行完成，末获取最终结果，设置为处理中。通过对账处理。PayNo:{}",callBackInfo.getPayNo());
				callBackInfo.setStatus(CallBackEnum.PROCESSING.getCode());
				callBackInfo.setUpdateTime(new Date());
				callbackDao.save(callBackInfo);
				return;
			}
			
			if("1".equals(type)){
				//按秒执行都已跑完，直接返回，等待按分钟执行。
				return;
			}else{
				// 按秒查询执行完成 对按分钟跑的定单加1
				callBackInfo.setCountMin(callBackInfo.getCountMin() + 1);
			}
		}

		OrderResponse orderResponse = new OrderResponse();
		orderResponse.setOrderNo(callBackInfo.getOrderNO());
		orderResponse.setErrorMessage(callBackInfo.getReturnMsg());
		orderResponse.setReturnCode(callBackInfo.getReturnCode());
		//20170925将支付流水号及订单类型返还给业务系统
		OrderVo order = comOrderDao.findByorderNo(callBackInfo.getOrderNO());
		String orderType = "";
		if(order != null){
			orderResponse.setPayNo(order.getPayNo());
			orderType = order.getType();
			//201701115增加回调支付通道id
			orderResponse.setChannelNo(order.getChannel());
			orderResponse.setUserType(order.getUserType());
		}
		//当结算不存在订单时为账户系统订单，订单类型取账户订单类型及用户类型
		AccOrderEntity entity = accOrderDao.findByOrderNo(callBackInfo.getOrderNO());
		if(entity != null&&"".equals(orderType)){
			orderType = entity.getOrderType();
			if(OrderTypeEnum.REDEEM.getCode().equals(orderType)){
				orderType = "03";//由于结算系统提现订单类型为02，账户赎回订单类型为02，避免回调错误，区分赎回为03
			}
			orderResponse.setUserType(entity.getUserType());
		}
		orderResponse.setType(orderType);
		// 推送处理
		Boolean result = false;
		String returnMsg = "推送失败";

		try {
			log.info("交易回调，{}", JSONObject.toJSONString(orderResponse));
			result = settlementSdk.callback(orderResponse);
		} catch (Exception e) {
			returnMsg = "推送交易信息异常";
			log.error(returnMsg + " OrderNO{},{}", callBackInfo.getOrderNO(), e);
		}

		log.info("回调结果，orderNo：{},result：{}", orderResponse.getOrderNo(), result);
		if (result) {
			returnMsg = "推送成功";
			callBackInfo.setStatus(CallBackEnum.SUCCESS.getCode());
		}
		
		if (callBackInfo.getCount() < callBackInfo.getTotalCount()) {
			callBackInfo.setCount(callBackInfo.getCount() + 1);
		}
		
		callBackInfo.setUpdateTime(time);
		callbackDao.save(callBackInfo);
		CallBackLog backLog = CallBackLog.builder().callBackOid(callBackInfo.getOid()).updateTime(time).createTime(time)
				.returnMsg(returnMsg).payNo(callBackInfo.getPayNo()).status(callBackInfo.getStatus()).build();
		callBackDaoLog.save(backLog);
	}
	
	
}
