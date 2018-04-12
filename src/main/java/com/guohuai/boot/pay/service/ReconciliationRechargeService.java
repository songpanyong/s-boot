/**
 * 
 */
package com.guohuai.boot.pay.service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.PayEnum;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.service.AccountWithdrawalsService;
import com.guohuai.boot.pay.dao.BankLogDao;
import com.guohuai.boot.pay.dao.ComOrderDao;
import com.guohuai.boot.pay.dao.PaymentDao;
import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.boot.pay.vo.PaymentVo;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.CheckConstant;
import com.guohuai.component.util.Constant;
import com.guohuai.payadapter.component.CallBackEnum;
import com.guohuai.payadapter.redeem.CallBackDao;
import com.guohuai.payadapter.redeem.CallBackInfo;
import com.guohuai.settlement.api.response.BaseResponse;
import com.guohuai.settlement.api.response.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 充值对账
 * 
 * @author xueyunlong
 *
 */
@Slf4j
@Service
public class ReconciliationRechargeService {

	@Autowired
	private CallBackDao callbackDao;

	@Autowired
	PaymentService paymentService;

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
	private EntityManager entityManager;

	@Value("${needRechargeFrozenAccount:N}")
	private String needRechargeFrozenAccount;
	
	/**
	 * 充值状态修复
	 * 
	 * @param orderNo
	 *            定单号
	 * @param externalState
	 *            三方状态 1：成功 2：失败
	 * @param systemState
	 *            系统状态
	 * @return
	 */
	public OrderResponse rechargeRecon(String orderNo, String externalState, String systemState) {
		log.info("充值做对账处理，orderNo:{},三方状态：{},系统状态 ：{}", orderNo, externalState, systemState);
		OrderResponse orderResponse = new OrderResponse();
		orderResponse.setOrderNo(orderNo);
		orderResponse.setReturnCode(Constant.SUCCESS);

		OrderVo order = comOrderDao.findByorderNo(orderNo);
		if (null == order) {
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("定单不存在");
			return orderResponse;
		}

		if(!systemState.equals(order.getStatus())) {
			log.info("对账传入系统状态，与系统真实状态不一至，orderNO:{} ,系统真实状态：{}", orderNo, systemState);
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("对账传入系统状态，与系统真实状态不一至!");
			return orderResponse;
		}
		switch (externalState) {
		case "1":
			log.info("充值，三方成功，系统状态为未处理、失败、超时、处理中的修改为成功，并做账务处理，orderNo:{}", orderNo);
			if (StringUtil.in(systemState, PayEnum.PAY2.getCode(), PayEnum.PAY3.getCode(), PayEnum.PAY4.getCode(),
					PayEnum.PAY0.getCode())) {
				try {
					orderResponse = rechargeSuccessRecon(order, systemState);
				} catch (Exception e) {
					orderResponse.setReturnCode(Constant.FAIL);
					orderResponse.setErrorMessage("三方成功,充值对账状态修改处理异常");
					log.error(JSONObject.toJSONString(orderResponse), e);
				}
			} else {
				log.info("充值，三方成功，系统状态为成功，不做处理，orderNo:{}", orderNo);
			}
			break;
		case "2":
			log.info("充值，三方失败，系统状态为成功、超时、处理中的修改为失败，并做账务处理，orderNo:{}", orderNo);
			if (StringUtil.in(systemState, PayEnum.PAY1.getCode(), PayEnum.PAY3.getCode(), PayEnum.PAY4.getCode())) {
				try {
					orderResponse = rechargeFailRecon(order, systemState);
				} catch (Exception e) {
					orderResponse.setReturnCode(Constant.FAIL);
					orderResponse.setErrorMessage("三方失败,充值对账状态修改处理异常");
					log.error(JSONObject.toJSONString(orderResponse), e);
				}
			} else {
				log.info("充值，三方失败，系统状态为为失败，不做处理，orderNo:{}", orderNo);
			}
			break;

		default:
			orderResponse.setReturnCode(Constant.FAIL);
			orderResponse.setErrorMessage("外部定单状态异常");
			break;
		}
		log.info("充值对账处理结果：{}", JSONObject.toJSONString(orderResponse));
		return orderResponse;
	}

	/**
	 * 三方成功做充值账务处理
	 * 
	 * @param order
	 * @param systemState
	 * @return
	 */
	@Transactional
	protected OrderResponse rechargeSuccessRecon(OrderVo order, String systemState) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		OrderResponse orderResponse = new OrderResponse();
		String orderNo = order.getOrderNo();
		String trastatus = order.getStatus();

		orderResponse.setOrderNo(orderNo);
		BaseResponse baseResponse = new BaseResponse();

		PaymentVo pay = paymentDao.findByOrderNo(orderNo);
		if (null == pay) {
			orderResponse.setErrorMessage("定单不存在");
			orderResponse.setReturnCode(Constant.FAIL);
			return orderResponse;
		}
		AccOrderEntity accOrderEntity = accOrderDao.findByOrderNo(orderNo);

		if (null == accOrderEntity) {
			log.info("三方成功，账务无定单，重新发起充值");
			AccountTransResponse accountTransResponse = paymentService.accounting(pay, "");
			log.info("充值记账返回，orderNo：{}，accountTransResponse：{}", orderNo, JSONObject.toJSON(accountTransResponse));
			baseResponse.setReturnCode(accountTransResponse.getReturnCode());
			baseResponse.setErrorMessage(accountTransResponse.getErrorMessage());
		} else {
			if (!AccOrderEntity.ORDERSTATUS_SUCCESS.equals(accOrderEntity.getOrderStatus())) {
				log.info("三方成功，定单状态不正确，账务调账失败，进行记账并修改定单状态，定单号：{}，账户定单状态：{}", orderNo, accOrderEntity.getOrderStatus());
				AccountInfoEntity basicAccount = accountInfoDao
						.findAccountByAccountTypeAndUserOid(AccountTypeEnum.BASICER.getCode(), order.getUserOid());
				
				baseResponse = accountWithdrawalsService.addBasicAccountTrans(accOrderEntity.getUserType(),
						accOrderEntity.getUserOid(), basicAccount, accOrderEntity.getBalance(), accOrderEntity);
				
				log.info("增加用户基本户，定单：{} 返回：{}", orderNo, JSONObject.toJSON(baseResponse));

				if (Constant.SUCCESS.equals(baseResponse.getReturnCode())) {
					accOrderEntity.setUpdateTime(time);
					accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_SUCCESS);
					accOrderDao.saveAndFlush(accOrderEntity);
					log.info("更新原定单状态为成功");
				}
			} else {
				log.info("已充值，账户已增加，结算状态修改为成功 orderNo:{}", orderNo);
				baseResponse.setReturnCode(Constant.SUCCESS);
			}
		}

		if (Constant.SUCCESS.equals(baseResponse.getReturnCode())) {
			// 操作成功，状态改为成功
			trastatus = PayEnum.PAY1.getCode();
			orderResponse.setErrorMessage(CheckConstant.STANDARD_RESULT_1005);
		} else {
			orderResponse.setErrorMessage(baseResponse.getErrorMessage());
		}

		orderResponse.setReturnCode(baseResponse.getReturnCode());

		log.info("三方成功做充值账务处理,orderNo:{},交易状态更新为：{} orderResponse:{}", order.getOrderNo(), trastatus,
				JSONObject.toJSON(orderResponse));

		// 更改收单、指令、交互日志状态
		int result = 0;
		result = comOrderDao.updateByOrder(trastatus, orderResponse.getReturnCode(), orderResponse.getErrorMessage(),
				pay.getOrderNo());
		log.info("orderNo ：{} 修改定单状态为trastatus：{},修改结果：{}", orderNo, trastatus, result);

		// ---交互日志
		result = bankLogDao.updateByPayNo(trastatus, pay.getPayNo());

		log.info("orderNo ：{} 修改三方交互日志状态为trastatus：{},修改结果：{}", orderNo, trastatus, result);

		// 修改指令表状态
		pay.setUpdateTime(time);
		pay.setCommandStatus(trastatus);
		pay.setFailDetail(orderResponse.getErrorMessage());
		paymentDao.saveAndFlush(pay);
		log.info("orderNo ：{} 修改指令表状态为trastatus：{}", orderNo, trastatus);

		CallBackInfo info = callbackDao.queryCallBackOne(pay.getPayNo(), "bank");
		if (null != info) {
			if (CallBackEnum.INIT.getCode().equals(info.getStatus())) {
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
	 * 三方失败做充值账务处理
	 * 
	 * @param order
	 * @param systemState
	 * @return
	 */
	@Transactional
	protected OrderResponse rechargeFailRecon(OrderVo order, String systemState) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		String orderNo = order.getOrderNo();
		String userOid = order.getUserOid();
		String trastatus = order.getStatus();

		OrderResponse orderResponse = new OrderResponse();
		orderResponse.setOrderNo(orderNo);

		BaseResponse baseResponse = new BaseResponse();
		String errorMsg="";
		// 用户基本户
		AccountInfoEntity basicAccount = null;
		// 用户提现冻结户
		AccountInfoEntity frozenAccount = null;
		AccOrderEntity accOrderEntity = accOrderDao.findByOrderNo(orderNo);
		PaymentVo pay = paymentDao.findByOrderNo(orderNo);
		if (null == accOrderEntity) {
			log.info("三方失败做充值账务处理，增加失败定单");
			AccountTransResponse accountTransResponse = paymentService.saveAccountFailOrder(pay);
			if(Constant.SUCCESS.equals(accountTransResponse.getReturnCode())){
				errorMsg=CheckConstant.STANDARD_RESULT_1006;
			}
			baseResponse.setErrorMessage(accountTransResponse.getErrorMessage());
			baseResponse.setReturnCode(accountTransResponse.getReturnCode());
		} else {
			if (AccOrderEntity.ORDERSTATUS_SUCCESS.equals(accOrderEntity.getOrderStatus())) {
				// 用户基本户
				basicAccount = accountInfoDao.findAccountByAccountTypeAndUserOid(AccountTypeEnum.BASICER.getCode(),
						userOid);
				// 用户提现冻结户
				frozenAccount = accountInfoDao.findAccountByAccountTypeAndUserOid(AccountTypeEnum.FROZEN.getCode(),
						userOid);
				// 判断余额是否充足
				BigDecimal balance = BigDecimal.ZERO;
				if(frozenAccount != null){
					balance = basicAccount.getBalance().subtract(frozenAccount.getBalance().add(accOrderEntity.getBalance()));
				}else{
					balance = basicAccount.getBalance().subtract(accOrderEntity.getBalance());
				}
				if(balance.compareTo(BigDecimal.ZERO)<0){
					baseResponse.setReturnCode(Constant.BALANCELESS);
					baseResponse.setErrorMessage(CheckConstant.STANDARD_RESULT_1002);
				}else{
					log.info("充值成功，打款失败，修复（减少）基本户余额，orderNo:{}", orderNo);
					baseResponse = accountWithdrawalsService.subtractBasicAccountTrans(accOrderEntity.getUserType(),
							accOrderEntity.getUserOid(), basicAccount, accOrderEntity.getBalance(), accOrderEntity);
				}
				log.info("自动扣减用户余额 resp:{}",JSONObject.toJSON(baseResponse));
				if (Constant.SUCCESS.equals(baseResponse.getReturnCode())) {
					AccountInfoEntity rechargefrozenAccount = null;
			        //断定是否需要充值冻结户 如果不需要，查询时充值冰结户设置为0
			        if ("Y".equals(needRechargeFrozenAccount)) {
			        	//查询发行人充值冻结户
			        	rechargefrozenAccount = accountInfoDao.findAccountByAccountTypeAndUserOid(AccountTypeEnum.RECHARGEFROZEN.getCode(),userOid);
			        }
					if(null != rechargefrozenAccount){
						rechargefrozenAccount = accountInfoDao.findByOidForUpdate(rechargefrozenAccount.getOid());
						entityManager.refresh(rechargefrozenAccount);
						log.info("刷新冻结户");
						if(rechargefrozenAccount.getBalance().compareTo(accOrderEntity.getBalance())>=0){
							int result =0;
							result = accountInfoDao.subtractBalance(accOrderEntity.getBalance(), rechargefrozenAccount.getAccountNo());
							if(result>0){
								BigDecimal afterBalance = rechargefrozenAccount.getBalance().subtract(accOrderEntity.getBalance());
								accountWithdrawalsService.addTrans(accOrderEntity, "", rechargefrozenAccount.getAccountNo(), afterBalance, 
										"充值平台成功，三方成功，扣除冻结金额", "02", AccountTypeEnum.FROZEN.getCode(), rechargefrozenAccount.getAccountNo());
							}
							
						}
					}
					errorMsg=CheckConstant.STANDARD_RESULT_1001;
					accOrderEntity.setUpdateTime(time);
					accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
					accOrderDao.saveAndFlush(accOrderEntity);
					log.info("扣款成功，更新原定单状态为失败");
					
				}else{
					baseResponse.setErrorMessage(CheckConstant.STANDARD_RESULT_1002);
				}
			} else {
				errorMsg=CheckConstant.STANDARD_RESULT_1006;
				log.info("用户充值未成功，三方失败，修改状态为失败 orderNo:{},定单状态：{}", orderNo, trastatus);
				baseResponse.setReturnCode(Constant.SUCCESS);
				accOrderEntity.setUpdateTime(time);
				accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
				accOrderDao.saveAndFlush(accOrderEntity);
				log.info("更新原定单状态为失败");
			}
		}

		if (Constant.SUCCESS.equals(baseResponse.getReturnCode())) {
			// 操作成功，状态改为失败
			trastatus = PayEnum.PAY2.getCode();
			orderResponse.setErrorMessage(errorMsg);
		} else {
			orderResponse.setErrorMessage(baseResponse.getErrorMessage());
		}
		orderResponse.setReturnCode(baseResponse.getReturnCode());

		log.info("三方失败，做充值账务处理,orderNo:{},交易状态更新为：{} orderResponse:{}", order.getOrderNo(), trastatus,
				JSONObject.toJSON(orderResponse));
		// 更改收单、指令、交互日志状态
		int result = 0;
		result = comOrderDao.updateByOrder(trastatus, orderResponse.getReturnCode(), orderResponse.getErrorMessage(),
				pay.getOrderNo());
		log.info("orderNo ：{} 修改定单状态为trastatus：{},修改结果：{}", orderNo, trastatus, result);

		// ---交互日志
		result = bankLogDao.updateByPayNo(trastatus, pay.getPayNo());

		log.info("orderNo ：{} 修改三方交互日志状态为trastatus：{},修改结果：{}", orderNo, trastatus, result);

		// 修改指令表状态
		pay.setUpdateTime(time);
		pay.setCommandStatus(trastatus);
		pay.setFailDetail(orderResponse.getErrorMessage());
		paymentDao.saveAndFlush(pay);

		CallBackInfo info = callbackDao.queryCallBackOne(pay.getPayNo(), "bank");
		if (null != info) {
			if (CallBackEnum.INIT.getCode().equals(info.getStatus())) {
				info.setStatus(CallBackEnum.SUCCESS.getCode());
				info.setUpdateTime(time);
				info.setReturnMsg("对账操作完成，更新为已回调");
				callbackDao.saveAndFlush(info);
				log.info("更新回调表成功");
			}

		}

		return orderResponse;
	}
}
