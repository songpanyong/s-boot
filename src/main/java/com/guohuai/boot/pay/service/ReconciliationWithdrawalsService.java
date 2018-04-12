/**
 * 
 */
package com.guohuai.boot.pay.service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.basic.component.exception.GHException;
import com.guohuai.basic.component.ext.web.BaseResp;
import com.guohuai.boot.PayEnum;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.service.AccountWithdrawalsService;
import com.guohuai.boot.pay.dao.BankLogDao;
import com.guohuai.boot.pay.dao.ComOrderDao;
import com.guohuai.boot.pay.dao.PaymentDao;
import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.boot.pay.vo.PaymentVo;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.CheckConstant;
import com.guohuai.component.util.Constant;
import com.guohuai.mimosa.api.MimosaSdk;
import com.guohuai.mimosa.api.obj.OrderStatusChangeNotifyReq;
import com.guohuai.mimosa.api.obj.OrderStatusNotifyEnum;
import com.guohuai.mimosa.api.obj.SmsSendReq;
import com.guohuai.mimosa.api.obj.SmsTemplateEnum;
import com.guohuai.payadapter.component.CallBackEnum;
import com.guohuai.payadapter.redeem.CallBackDao;
import com.guohuai.payadapter.redeem.CallBackInfo;
import com.guohuai.settlement.api.response.BaseResponse;
import com.guohuai.settlement.api.response.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 提现对账
 * 
 * @author xueyunlong
 *
 */
@Slf4j
@Service
public class ReconciliationWithdrawalsService {

	/**
	 * 提现状态修复
	 * 
	 * @param orderNo
	 *            定单号
	 * @param externalState
	 *            三方状态 1：成功 2：失败
	 * @param systemState
	 * @return
	 */
	public OrderResponse withdrawalsRecon(String orderNo, String externalState, String systemState) {
		log.info("提现做对账处理，orderNo:{},三方状态：{},系统状态 ：{}",orderNo,externalState,systemState);
		OrderResponse orderResponse = new OrderResponse();
		orderResponse.setOrderNo(orderNo);
		orderResponse.setReturnCode(Constant.SUCCESS);
		OrderVo order = comOrderDao.findByorderNo(orderNo);
		if (null == order) {
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("定单不存在");
			return orderResponse;
		}
		
		if(!systemState.equals(order.getStatus())){
			log.info("对账传入系统状态，与系统真实状态不一至，orderNO:{} ,系统真实状态：{}",orderNo,systemState);
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("对账传入系统状态，与系统真实状态不一至!");
			return orderResponse;
		}
		switch (externalState) {
		case "1":
			log.info("提现三方成功，系统状态为未处理、失败、超时、处理中的修改为成功，并做账务处理，orderNo:{}",orderNo);
			if (StringUtil.in(systemState, PayEnum.PAY2.getCode(), PayEnum.PAY3.getCode(), PayEnum.PAY4.getCode(),
					PayEnum.PAY0.getCode())) {
				try {
					orderResponse = withdrawalSuccessRecon(order, systemState);
				} catch (Exception e) {
					orderResponse.setReturnCode(Constant.FAIL);
					orderResponse.setErrorMessage("三方成功,提现对账状态修改处理异常");
					log.error(JSONObject.toJSONString(orderResponse),e);
				}
			} else{
				log.info("提现三方成功，系统状态为成功，不做处理，orderNo:{}",orderNo);
			}
			break;
		case "2":
			log.info("提现三方失败，系统状态为成功、失败、超时、处理中的修改为失败，并做账务处理，orderNo:{}",orderNo);
			if (StringUtil.in(systemState, PayEnum.PAY1.getCode(),PayEnum.PAY2.getCode(), PayEnum.PAY3.getCode(), PayEnum.PAY4.getCode())) {
				try {
					orderResponse = withdrawalFailRecon(order, systemState);
					if(Constant.SUCCESS.equals(orderResponse.getReturnCode())){
						try {
							OrderStatusChangeNotifyReq req=new OrderStatusChangeNotifyReq();
							req.setOrderCode(orderNo);
							req.setStatusNotifyEnum(OrderStatusNotifyEnum.iceOut);
							BaseResp resp = mimosaSdk.notifyOrderStatusChange(req);
							if (null != resp) {
								log.info("提现定单号{}，通知业务解冻返回{}",orderNo,JSONObject.toJSONString(resp));
							}

						}catch (Exception ex){
							log.error("解冻通知异常， 定单号：{}，ex:{}",order.getOrderNo(),ex);
						}
						//调用业务系统发送短息通知用户提现解冻余额
						this.sendSMS(order);

					}
				} catch (Exception e) {
					orderResponse.setReturnCode(Constant.FAIL);
					orderResponse.setErrorMessage("三方失败,提现对账状态修改处理异常");
					log.error(JSONObject.toJSONString(orderResponse),e);
				}
			}else{
				log.info("提现三方失败，系统状态为失败，不做处理，orderNo:{}",orderNo);
			}
			break;

		default:
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("外部定单状态异常");
			break;
		}
		log.info("提现对账处理结果：{}",JSONObject.toJSONString(orderResponse));
		return orderResponse;
	}

	/**
	 * 调用业务系统发送短息通知用户提现解冻余额
	 * @param order 提现订单
	 */
	private void sendSMS(OrderVo order) {
		//查询用户信息
		UserInfoEntity userInfo = userInfoDao.findByUserOid(order.getUserOid());
		if(null == userInfo){
			log.error("发送短信获取用户信息异常，订单信息：{}", order);
			return;
		}
		SmsSendReq smsSendReq = new SmsSendReq();
		smsSendReq.setPhone(userInfo.getPhone());
		smsSendReq.setSmsTemplateEnum(SmsTemplateEnum.msgWithdrawUnfreeze);
		/**
		 * 【米饭公社】尊敬的用户，您于{1}发起的{2}元提现到账失败，资金已退回您的APP账户。
		 * 请您确认身份证号、银行卡号、银行预留手机号是否在绑定后发生过更改，
		 * 银行卡状态是否正常后再次发起提现。如信息更改请致电客服{3}
		 */
		try{
			List<Object> msgParams = new ArrayList<Object>();
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//定义格式，不显示毫秒
			String createTimeStr = df.format(order.getCreateTime());
			msgParams.add(0, createTimeStr);
			BigDecimal amount = order.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP);//定义格式，显示两位小数
			String amountStr = amount.toString();
			msgParams.add(1, amountStr);
			smsSendReq.setMsgParams(msgParams);
			log.info("调用业务系统发送短息通知用户提现解冻余额，请求参数:{}", smsSendReq);
			BaseResp resp = mimosaSdk.sendSMS(smsSendReq);
			log.info("调用业务系统发送短息通知用户提现解冻余额,业务系统返回:{}", resp);
		}catch(Exception e){
			log.error("调用业务系统发送短息通知用户提现解冻余额异常，{}", e);
		}
	}

	/**
	 * 三方成功做提现账务处理
	 * 
	 * @param order
	 * @param systemState
	 * @return
	 */
	@Transactional
	protected OrderResponse withdrawalSuccessRecon(OrderVo order, String systemState) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		OrderResponse orderResponse = new OrderResponse();
		String orderNo = order.getOrderNo();
		orderResponse.setOrderNo(orderNo);
		
		String trastatus = order.getStatus();
		BaseResponse baseResponse = null;
		
		AccOrderEntity accOrderEntity = accOrderDao.findByOrderNo(orderNo);
		if (!AccOrderEntity.ORDERSTATUS_SUCCESS.equals(accOrderEntity.getOrderStatus())) {
			String userOid = accOrderEntity.getUserOid();
			// 用户冻结户
			AccountInfoEntity withdrawalsFrozenAccount = accountInfoDao
					.findAccountByAccountTypeAndUserOid(AccountTypeEnum.FROZEN.getCode(), userOid);
			withdrawalsFrozenAccount = accountInfoDao.findByOidForUpdate(withdrawalsFrozenAccount.getOid());
			// 用户基本户
			AccountInfoEntity basicAccount = accountInfoDao
					.findAccountByAccountTypeAndUserOid(AccountTypeEnum.BASICER.getCode(), userOid);
			
			// 提现账户已冻结
			if (AccOrderEntity.ORDERSTATUS_INIT.equals(accOrderEntity.getOrderStatus())) {

				log.info("用户提现金额已冻结，进行扣款操作，orderNo:{}", orderNo);
				baseResponse = accountWithdrawalsService.withdrawalsAccountTrans(accOrderEntity.getUserType(),
						accOrderEntity.getUserOid(), basicAccount, accOrderEntity.getBalance(),
						withdrawalsFrozenAccount, accOrderEntity);
				log.info("用户提现金额已冻结，进行扣款操作结果，orderNo:{}，baseResponse={}", orderNo, JSONObject.toJSON(baseResponse));

			} else if (AccOrderEntity.ORDERSTATUS_FAIL.equals(accOrderEntity.getOrderStatus())) {

				log.info("用户提现金额未冻结，进行冻结操作，orderNo:{}", orderNo);
				baseResponse = accountWithdrawalsService.forzenUserWithdrawalsAccount(accOrderEntity.getUserType(),
						userOid, basicAccount, accOrderEntity.getBalance(), withdrawalsFrozenAccount, accOrderEntity);
				log.info("用户提现金额未冻结，进行冻结操作结果，orderNo:{},baseResponse={}", orderNo, JSONObject.toJSON(baseResponse));

				if (Constant.SUCCESS.equals(baseResponse.getReturnCode())) {

					log.info("用户提现金额未冻结，进行扣款操作，orderNo:{}", orderNo);
					baseResponse = accountWithdrawalsService.withdrawalsAccountTrans(accOrderEntity.getUserType(),
							userOid, basicAccount, accOrderEntity.getBalance(), withdrawalsFrozenAccount,
							accOrderEntity);
					log.info("用户提现金额未冻结，进行扣款结果，orderNo:{},baseResponse={}", orderNo, JSONObject.toJSON(baseResponse));
				}

				if (!Constant.SUCCESS.equals(baseResponse.getReturnCode())) {
					throw new GHException(9999, baseResponse.getErrorMessage());
				}

			} else {
				log.info("用户提现对账处理，提现账户定单状态未在处理范围 orderNo:{},定单状态：{}", orderNo, trastatus);
				orderResponse.setErrorMessage("提现账户定单状态未在处理范围");
				orderResponse.setReturnCode(Constant.FAIL);
				return orderResponse;
			}
		} else {
			log.info("已提现，账户已扣除，结算状态修改为成功 orderNo:{}", orderNo);
			baseResponse = new BaseResponse();
			baseResponse.setReturnCode(Constant.SUCCESS);
		}

		if (Constant.SUCCESS.equals(baseResponse.getReturnCode())) {
			// 操作成功，状态改为成功
			trastatus = PayEnum.PAY1.getCode();
			orderResponse.setErrorMessage(CheckConstant.STANDARD_RESULT_1008);
			accOrderEntity.setUpdateTime(time);
			accOrderEntity.setOrderStatus(trastatus);
			accOrderDao.saveAndFlush(accOrderEntity);
			log.info("操作成功，状态改为成功，修改原账务定单状态为成功，orderNo:{}", orderNo);
		} else {
			orderResponse.setErrorMessage(baseResponse.getErrorMessage());
		}

		orderResponse.setReturnCode(baseResponse.getReturnCode());

		log.info("三方成功做提现账务处理,orderNo:{},交易状态更新为：{} orderResponse:{}", order.getOrderNo(), trastatus,
				JSONObject.toJSON(orderResponse));

		// 更改收单、指令、交互日志状态
		PaymentVo pay = paymentDao.findByOrderNo(order.getOrderNo());
		// ---收单
		int result = 0;
		result = comOrderDao.updateByOrder(trastatus, orderResponse.getReturnCode(), orderResponse.getErrorMessage(),
				pay.getOrderNo());
		log.info("orderNo ：{} 修改定单状态为trastatus：{},修改结果：{}", orderNo, trastatus, result);

		// ---交互日志
		result=bankLogDao.updateByPayNo(trastatus, pay.getPayNo());

		log.info("orderNo ：{} 修改银行日志状态为trastatus：{},修改结果：{}", orderNo, trastatus, result);

		// 修改指令表状态
		result=paymentDao.updateByPayNo(trastatus, pay.getPayNo());
		log.info("orderNo ：{} 修改指令表状态为trastatus：{}", orderNo, trastatus);
		
		CallBackInfo info = callbackDao.queryCallBackOne(pay.getPayNo(), "bank");
		if( null != info ){
			if(CallBackEnum.INIT.getCode().equals(info.getStatus())){
				info.setStatus(CallBackEnum.SUCCESS.getCode());
				info.setUpdateTime(time);
				info.setReturnMsg("对账操作完成，更新为已回调");
				callbackDao.saveAndFlush(info);
				log.info("更新回调表成功");
			}
		
		}
		
		return orderResponse;
	}

	/**
	 * 三方失败做提现账务处理
	 * 
	 * @param order
	 * @param systemState
	 * @return
	 */
	@Transactional
	protected OrderResponse withdrawalFailRecon(OrderVo order, String systemState) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		OrderResponse orderResponse = new OrderResponse();
		String orderNo = order.getOrderNo();
		orderResponse.setOrderNo(orderNo);
		AccOrderEntity accOrderEntity = accOrderDao.findByOrderNo(orderNo);

		BaseResponse baseResponse = null;
		String userOid = accOrderEntity.getUserOid();
		String trastatus = order.getStatus();

		// 用户基本户
		AccountInfoEntity basicAccount = accountInfoDao
				.findAccountByAccountTypeAndUserOid(AccountTypeEnum.BASICER.getCode(), userOid);
		
		if (AccOrderEntity.ORDERSTATUS_SUCCESS.equals(accOrderEntity.getOrderStatus())) {

			log.info("提现扣款成功，打款失败，修复（增加）基本户余额，orderNo:{}", orderNo);
			baseResponse = accountWithdrawalsService.addBasicAccountTrans(accOrderEntity.getUserType(),
					accOrderEntity.getUserOid(), basicAccount, accOrderEntity.getBalance(), accOrderEntity);
			if (Constant.SUCCESS.equals(baseResponse.getReturnCode())) {
				log.info("修改余额成功，设置原定单状态为失败，orderNo:{}", orderNo);
				accOrderEntity.setUpdateTime(time);
				accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
				accOrderDao.saveAndFlush(accOrderEntity);
			}

		} else if (AccOrderEntity.ORDERSTATUS_INIT.equals(accOrderEntity.getOrderStatus())) {

			// 用户冻结户
			AccountInfoEntity withdrawalsFrozenAccount = accountInfoDao
					.findAccountByAccountTypeAndUserOid(AccountTypeEnum.FROZEN.getCode(), userOid);
			log.info("用户提现金额已冻结，进行解冻操作，orderNo:{}", orderNo);
			baseResponse = accountWithdrawalsService.unforzenUserWithdrawalsAccount(accOrderEntity.getUserType(),
					userOid, accOrderEntity.getBalance(), withdrawalsFrozenAccount, accOrderEntity);
		} else {
			log.info("用户提现未成功，三方失败，冻结户已解冻，修改状态为失败 orderNo:{},定单状态：{}", orderNo, trastatus);
			baseResponse = new BaseResponse();
			baseResponse.setReturnCode(Constant.SUCCESS);
		}

		if (Constant.SUCCESS.equals(baseResponse.getReturnCode())) {
			// 操作成功，状态改为失败
			trastatus = PayEnum.PAY2.getCode();
			orderResponse.setErrorMessage(CheckConstant.STANDARD_RESULT_1007);
			accOrderEntity.setUpdateTime(time);
			accOrderEntity.setOrderStatus(trastatus);
			accOrderDao.saveAndFlush(accOrderEntity);
			log.info("三方失败，账务操作成功，修改原账务定单状态为失败，orderNo:{}", orderNo);
		} else {
			orderResponse.setErrorMessage(baseResponse.getErrorMessage());
		}
		orderResponse.setReturnCode(baseResponse.getReturnCode());

		log.info("三方失败做提现账务处理,orderNo:{},交易状态更新为：{} orderResponse:{}", order.getOrderNo(), trastatus,
				JSONObject.toJSON(orderResponse));

		// 更改收单、指令、交互日志状态
		PaymentVo pay = paymentDao.findByOrderNo(order.getOrderNo());
		// ---收单
		int result = 0;
		result = comOrderDao.updateByOrder(trastatus, orderResponse.getReturnCode(), orderResponse.getErrorMessage(),
				pay.getOrderNo());
		log.info("orderNo ：{} 修改定单状态为trastatus：{},修改结果：{}", orderNo, trastatus, result);

		// ---交互日志
		result = bankLogDao.updateByPayNo(trastatus, pay.getPayNo());

		log.info("orderNo ：{} 修改银行日志状态为trastatus：{},修改结果：{}", orderNo, trastatus, result);

		// 修改指令表状态
		pay.setUpdateTime(time);
		pay.setCommandStatus(trastatus);
		pay.setFailDetail(orderResponse.getErrorMessage());
		paymentDao.saveAndFlush(pay);
		log.info("orderNo ：{} 修改指令表状态为trastatus：{}", orderNo, trastatus);
		
		CallBackInfo info = callbackDao.queryCallBackOne(pay.getPayNo(), "bank");
		if( null != info ){
			if(CallBackEnum.INIT.getCode().equals(info.getStatus())){
				info.setStatus(CallBackEnum.SUCCESS.getCode());
				info.setUpdateTime(time);
				info.setReturnMsg("对账操作完成，更新为已回调");
				callbackDao.saveAndFlush(info);
				log.info("更新回调表成功");
			}
		
		}
		
		return orderResponse;
	}
	
	@Autowired
	private CallBackDao callbackDao;

	@Autowired
	BankLogDao bankLogDao;

	@Autowired
	PaymentDao paymentDao;

	@Autowired
	AccOrderDao accOrderDao;

	@Autowired
	ComOrderDao comOrderDao;

	@Autowired
	AccountWithdrawalsService accountWithdrawalsService;

	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private MimosaSdk mimosaSdk;
	@Autowired
	private UserInfoDao userInfoDao;
}
