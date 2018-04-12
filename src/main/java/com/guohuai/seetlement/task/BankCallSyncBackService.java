package com.guohuai.seetlement.task;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.payadapter.bankutil.StringUtil;
import com.guohuai.payadapter.component.CallBackEnum;
import com.guohuai.payadapter.component.Constant;
import com.guohuai.payadapter.listener.event.CallBackEvent;
import com.guohuai.payadapter.listener.event.TradeRecordEvent;
import com.guohuai.payadapter.redeem.CallBackDao;
import com.guohuai.payadapter.redeem.CallBackDaoLog;
import com.guohuai.payadapter.redeem.CallBackInfo;
import com.guohuai.payadapter.redeem.CallBackLog;
import com.guohuai.payadapter.redeem.ConcurrentManager;
import com.guohuai.payadapter.redeem.ConcurrentManagerDao;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BankCallSyncBackService {
	@Autowired
	CallBackDao callbackDao;

	@Autowired
	CallBackDaoLog callBackDaoLog;

	@Autowired
	private ApplicationEventPublisher pushEvent;

	@Value("${pingan.callback:prod}")
	private String callBack;

	@Autowired
	ConcurrentManagerDao concurrentManagerDao;

	List<ConcurrentManager> concurrentManagers = null;

	@Value("${server.host}")
	private String host;
	
	@Value("${withOutThirdParty:no}")
	private String withOutThirdParty;
	/**
	 * @param callBackInfo
	 * @param type 按秒跑为1，按分钟跑为 2
	 */
	@Async("bankCallbackExecutor")
	public void bankCallBack(CallBackInfo callBackInfo,String type) {
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
		// 如果回调未超时,只查询状态为“0”的
		// 在数据库中status:0,未处理,1交易成功,2交易失败,3交易处理中,4超时(超时的放到处理中,等人工处理)
		if (CallBackEnum.INIT.getCode().equals(callBackInfo.getStatus())) {
			Map<String, String> hashMap = new HashMap<String, String>();
			hashMap = queryPayState(callBackInfo);
			log.info("查询定单状态返回，hashMap：{}", hashMap);
			String status = hashMap.get("status");
			String returnCode = hashMap.get("returnCode");
			String msg = hashMap.get("msg");

			// 当查询定单返回处理中，继续查询，不修改状态,此时状态还是未处理
			if (!CallBackEnum.PROCESSING.getCode().equals(status)) {
				callBackInfo.setStatus(status);
			}
			callBackInfo.setReturnCode(returnCode);

			if (callBackInfo.getCount() < callBackInfo.getTotalCount()) {
				callBackInfo.setCount(callBackInfo.getCount() + 1);
			}

			callBackInfo.setUpdateTime(time);
			callBackInfo.setReturnMsg(msg);
			log.info("保存callbackInfo={}", JSONObject.toJSON(callBackInfo));
			callbackDao.save(callBackInfo);
			CallBackLog backLog = CallBackLog.builder().callBackOid(callBackInfo.getOid()).returnMsg(msg)
					.payNo(callBackInfo.getPayNo()).createTime(time).status(callBackInfo.getStatus())
					.returnCode(callBackInfo.getReturnCode()).bankReturnSerialId(callBackInfo.getBankReturnSerialId())
					.build();
			callBackDaoLog.save(backLog);

			// 查询定单为处理中，不进行推送
			if (!CallBackEnum.PROCESSING.getCode().equals(status)) {
				// 回调返回
				CallBackEvent event = new CallBackEvent();
				event.setTradeType(callBackInfo.getTradeType());
				event.setChannel(callBackInfo.getChannelNo());
				event.setOrderNo(callBackInfo.getOrderNO());
				event.setPayNo(callBackInfo.getPayNo());
				event.setStatus(status);
				event.setReturnCode(callBackInfo.getReturnCode());
				event.setErrorDesc(msg);
				pushEvent.publishEvent(event);
				log.info("推送到结算系统：{}", JSONObject.toJSONString(event));
			}
		}

	}
	
	// 查询支付结果
		private Map<String, String> queryPayState(CallBackInfo callBackInfo) {
			Map<String, String> hashMap = new HashMap<String, String>();
			hashMap.put("status", "");
			hashMap.put("returnCode", Constant.FAIL);

			TradeRecordEvent event = new TradeRecordEvent();
			event.setChannel(callBackInfo.getChannelNo());
			event.setOrderNo(callBackInfo.getPayNo());
			
			//不经过三方直接返回结果,只对充值的
			if("yes".equals(withOutThirdParty)){
				log.info("withOutThirdParty = yes");
//				String returnCode = "";
//				if(TradeType.pay.getValue().equals(callBackInfo.getTradeType())){
//					log.info("callBackInfo订单类型为充值");
//					returnCode = getResultByNo(event.getOrderNo());
//				}else{
//					log.info("callBackInfo订单类型为提现");
//					returnCode =  getResultByNo(event.getOrderNo());
//				}
				String returnCode = getResultByNo(event.getOrderNo());
				event.setReturnCode(returnCode);
				if(Constant.SUCCESS.equals(returnCode)){
					event.setErrorDesc("交易成功");
				}else if(Constant.FAIL.equals(returnCode)){
					event.setErrorDesc("交易失败");
				}else{
					event.setErrorDesc("交易处理中");
				}
			}else{
				pushEvent.publishEvent(event);
			}
			
			log.info("定单查询返回结果：{}", JSONObject.toJSONString(event));
			
			String status = event.getStatus();
			if (Constant.SUCCESS.equals(event.getReturnCode())) {
				status = CallBackEnum.SUCCESS.getCode();
			}else if(Constant.FAIL.equals(event.getReturnCode())){
				status = CallBackEnum.FAIL.getCode();
			}else if(Constant.INPROCESS.equals(event.getReturnCode())){
				status = CallBackEnum.PROCESSING.getCode();
			}else{
				status = CallBackEnum.FAIL.getCode();
			}
			hashMap.put("status", status);
			hashMap.put("returnCode", event.getReturnCode());
			hashMap.put("msg", event.getErrorDesc());
			return hashMap;
		}
		
		/**
		 * 配合挡板,根据订单号尾数判断订单成功失败
		 * @param payNo
		 * @return
		 */
		private String getResultByNo(String payNo){
			log.info("挡板订单号:payNo{}",payNo);
			String isSuccess = Constant.SUCCESS;
			if(!StringUtil.isEmpty(payNo)){
				int length = payNo.length();
				String lastStr = payNo.substring(length-1, length);
				if("1".equals(lastStr) || "3".equals(lastStr) || "5".equals(lastStr) || "7".equals(lastStr) || "9".equals(lastStr) ){
					isSuccess = Constant.SUCCESS;
				}else if("2".equals(lastStr) || "4".equals(lastStr)  || "6".equals(lastStr)){
					isSuccess = Constant.FAIL;
				}else{
					isSuccess = Constant.INPROCESS;
				}
			}
			log.info("挡板返回:returnCode{}",isSuccess);
			return isSuccess;
		}
		

}
