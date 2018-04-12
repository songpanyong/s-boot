package com.guohuai.boot.pay.service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountTransRequest;
import com.guohuai.account.api.request.CreateOrderRequest;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.account.api.response.CreateOrderResponse;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.PayEnum;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.service.*;
import com.guohuai.boot.pay.dao.BankLogDao;
import com.guohuai.boot.pay.dao.ComOrderDao;
import com.guohuai.boot.pay.dao.PaymentDao;
import com.guohuai.boot.pay.form.PaymentForm;
import com.guohuai.boot.pay.res.PaymentVoRes;
import com.guohuai.boot.pay.vo.BankLogVo;
import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.boot.pay.vo.PaymentVo;
import com.guohuai.component.common.CSVUtil;
import com.guohuai.component.exception.SETException;
import com.guohuai.component.util.*;
import com.guohuai.payadapter.component.CallBackEnum;
import com.guohuai.payadapter.component.TradeType;
import com.guohuai.payadapter.listener.event.TradeEvent;
import com.guohuai.payadapter.redeem.CallBackDao;
import com.guohuai.payadapter.redeem.CallBackInfo;
import com.guohuai.settlement.api.SettlementSdk;
import com.guohuai.settlement.api.request.InteractiveRequest;
import com.guohuai.settlement.api.request.OrderRequest;
import com.guohuai.settlement.api.response.BaseResponse;
import com.guohuai.settlement.api.response.OrderResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.File;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

@Service
public class PaymentService {
	private final static Logger log = LoggerFactory.getLogger(PaymentService.class);
	@Autowired
	private PaymentDao paymentDao;

	@Autowired
	private BankLogDao bankLogDao;
	@Autowired
	private ComOrderDao comOrderDao;
	@Autowired
	private ApplicationEventPublisher event;
	@Autowired
	private SettlementSdk settlementSdk;
	@Autowired
	private PayTwoRedisUtil payTwoRedisUtil;

	@Autowired
	private CallBackDao callbackDao;

	private TransService transService;

	@Autowired
	public void setTransService(TransService transService) {
		this.transService = transService;
	}
	@Autowired
	private AccOrderService accOrderService;
	
	@Autowired
	private AccountInfoService accountInfoService;
	
	@Autowired
	private AccountWithdrawalsService accountWithdrawalsService;
	
	@Autowired
	private UserInfoService userInfoService;
	
	@Autowired
	private UserInfoDao userInfoDao;

	/**
	 * 修改【修改状态|补单|支付|撤单】
	 */
	public OrderResponse update(PaymentForm req) {
		log.info("{},修改状态|补单|支付|撤单,", req.getUserOid(), JSONObject.toJSONString(req));
		Timestamp time = new Timestamp(System.currentTimeMillis());
		OrderResponse orderRes = new OrderResponse();

		if (!StringUtil.isEmpty(req.getOid())) {
			PaymentVo pay = paymentDao.findOne(req.getOid());
			if (pay != null) {
				Long check = payTwoRedisUtil.setRedis("pay_order_submit_redis_tag" + pay.getOrderNo(),
						pay.getOrderNo());
				if (check.intValue() == 0) {
					orderRes.setReturnCode(Constant.FAIL);
					orderRes.setErrorMessage("请上一个操作审核过后再做该操作！");
					return orderRes;
				}
				if (!StringUtil.isEmpty(req.getOperatorStatus())) {// 单笔支付：01；失败重发：02；撤销：03
					pay.setOperator(req.getUserOid());
					pay.setOperatorReson(req.getOperatorReson());
					pay.setOperatorStatus(req.getOperatorStatus());
					pay.setAuditOperatorStatus(null);// 审核操作状态
					pay.setOperatorTime(time);
					pay.setAuditStatus(req.getAuditStatus());// 提交审核
				} else if (!StringUtil.isEmpty(req.getUpdateStatus())) {// 未处理：0；交易成功：1；交易失败：2；交易处理中：3；超时：4
					pay.setUpdateStatus(req.getUpdateStatus());
					pay.setUpdateReson(req.getUpdateReson());
					pay.setUpdateOperator(req.getUserOid());
					pay.setAuditUpdateStatus(null);// 审核修改状态
					pay.setUpTime(time);
					pay.setAuditStatus(req.getAuditStatus());// 提交审核
				} else if (!StringUtil.isEmpty(req.getResetOperatorStatus())) {// 撤销状态，单笔支付：01；失败重发：02；撤销：03
					pay.setResetOperatorStatus(req.getResetOperatorStatus());
					pay.setResetOpertatorReson(req.getResetOpertatorReson());
					pay.setResetOperator(req.getUserOid());
					pay.setAuditResetOperatorStatus(null);// 审核撤销操作状态
					pay.setResetOperatorTime(time);
					pay.setAuditStatus(req.getAuditStatus());// 提交审核
				}
				pay.setUpdateTime(time);
				paymentDao.save(pay);
				orderRes.setReturnCode(Constant.SUCCESS);
			} else {
				orderRes.setReturnCode(Constant.FAIL);
				orderRes.setErrorMessage("指令id不能为空！");
			}
		} else {
			orderRes.setReturnCode(Constant.FAIL);
			orderRes.setErrorMessage("指令id不能为空！");
		}

		return orderRes;
	}

	/***
	 * 审核操作[通过]|【不通过】
	 */
	public synchronized  OrderResponse  audit(PaymentForm req) {
		log.info("{},审核状态|补单|支付|撤单,", req.getUserOid(), JSONObject.toJSONString(req));
		OrderResponse orderRes = new OrderResponse();
		Timestamp time = new Timestamp(System.currentTimeMillis());
		String oldOrderStatus = "";//原订单状态
		if (!StringUtil.isEmpty(req.getOid()) && !StringUtil.isEmpty(req.getOrderNo())) {
			PaymentVo pay = paymentDao.findOne(req.getOid());
			if (pay != null) {
				oldOrderStatus = pay.getCommandStatus();
				pay.setUpdateTime(time);
				if (req.getOperatorType().trim().equals(PayEnum.OPERATORTYPE01.getCode())) {// 支付/重发
					pay.setAuditOperator(req.getUserOid());
					pay.setAuditOperatorReson(req.getReson());
					pay.setAuditOperatorStatus(req.getAuditStatus());
					pay.setAuditOperatorTime(time);
					pay.setAuditStatus(req.getAuditStatus());
				} else if (req.getOperatorType().trim().equals(PayEnum.OPERATORTYPE02.getCode())) {// 修改
					pay.setAuditUpdateOperator(req.getUserOid());
					pay.setAuditUpdateReson(req.getReson());
					pay.setAuditUpdateStatus(req.getAuditStatus());
					pay.setAuditUpdateTime(time);
					pay.setAuditStatus(req.getAuditStatus());
				} else if (req.getOperatorType().trim().equals(PayEnum.OPERATORTYPE03.getCode())) {// 撤销
					pay.setAuditResetOperator(req.getUserOid());
					pay.setAuditResetOperatorReson(req.getReson());
					pay.setAuditResetOperatorStatus(req.getAuditStatus());// 审核撤销操作状态
					pay.setAuditResetOperatorTime(time);
					pay.setAuditStatus(req.getAuditStatus());
				}
				//审核通过
				if (req.getAuditStatus().trim().equals(PayEnum.AUDIT1.getCode())) {
					//修改订单状态
					if (req.getOperatorType().trim().equals(PayEnum.OPERATORTYPE02.getCode())) {
						log.info("修改订单类型{}订单号{}状态{}改为{}",pay.getType(),pay.getOrderNo(),oldOrderStatus,pay.getUpdateStatus());
						boolean needSave = false;//保存修改后订单
						boolean needCallBack = false;//回调业务系统
						boolean needChangeStatus = false;//调用业务修改订单状态
						if (PayEnum.PAYTYPE01.getCode().equals(pay.getType())){//充值（申购）
							// 将充值订单状态修改为成功
							if (PayEnum.PAY1.getCode().equals(pay.getUpdateStatus())) {
								//订单原状态为未处理、交易处理中、超时的，记账并回调
								if(PayEnum.PAY0.getCode().equals(oldOrderStatus)||PayEnum.PAY3.getCode().equals(oldOrderStatus)
										||PayEnum.PAY4.getCode().equals(oldOrderStatus)){
									try {
										//开始记账
										log.info("修改充值订单，开始记账...");
										AccountTransResponse accountTransResponse = accounting(pay, null);
										log.info("修改订单状态查询账户余额");
										accountInfoService.getAccountBalanceByUserOid(pay.getUserOid());
										//判断记账状态
										if(Constant.SUCCESS.equals(accountTransResponse.getReturnCode())){
											//记账成功，修改保存订单状态,回调业务 
											needSave = true;//保存修改后订单
											needCallBack = true;//回调业务系统
										}
									} catch (Exception e) {
										orderRes.setReturnCode(Constant.FAIL);
										orderRes.setErrorMessage("修改订单状态记账异常！");
										log.error("记账异常,定单号：{},错误信息：{}", pay.getOrderNo(), e);
									}
								}else if(PayEnum.PAY2.getCode().equals(oldOrderStatus)){//订单原状态为失败，记账并修改订单状态
									log.info("修改充值订单，查询记账状态...");
									//查询记账状态
									String accountingTimes = transService.getWithdrawalsAccountStatus(pay.getUserOid(), pay.getOrderNo());
									log.info("获取记账状态，已记账{}次",accountingTimes);
									//开始记账
									log.info("修改充值订单，开始记账...");
									AccountTransResponse accountTransResponse = accounting(pay, null);
									log.info("修改订单状态查询账户余额");
									accountInfoService.getAccountBalanceByUserOid(pay.getUserOid());
									//判断记账状态
									if(Constant.SUCCESS.equals(accountTransResponse.getReturnCode())){
										//记账成功，修改保存订单状态，业务修改
										needSave = true;//保存修改后订单
										//20170703修改不调用业务系统,由t+1对账实现同步或使用业务系统单笔对账实现同步
										needChangeStatus = false;//调业务系统修改订单状态
									}
								}else{
									orderRes.setReturnCode(Constant.FAIL);
									orderRes.setErrorMessage("修改订单状态不支持！");
									log.info("修改订单状态不支持！订单号{}",pay.getOrderNo());
								}
							}else if(PayEnum.PAY2.getCode().equals(pay.getUpdateStatus())){//将充值订单改为失败
								//未处理、处理中、超时的订单修改为失败，记录失败订单并回调
								if(PayEnum.PAY0.getCode().equals(oldOrderStatus)||PayEnum.PAY3.getCode().equals(oldOrderStatus)
										||PayEnum.PAY4.getCode().equals(oldOrderStatus)){
									log.info("修改充值订单，开始保存账户订单...");
									//账户保存订单
									AccountTransResponse accountTransResponse = saveAccountFailOrder(pay);
									//判断记账状态
									if(Constant.SUCCESS.equals(accountTransResponse.getReturnCode())){
										//记账成功，修改保存订单状态，业务修改
										needSave = true;//保存修改后订单
										needCallBack = true;//回调业务系统
									}
								}else{//订单原状态为其他，不能修改为失败
									orderRes.setReturnCode(Constant.FAIL);
									orderRes.setErrorMessage("修改订单状态不支持！");
									log.info("修改订单状态不支持！订单号{}",pay.getOrderNo());
								}
							}else{
								orderRes.setReturnCode(Constant.FAIL);
								orderRes.setErrorMessage("修改订单状态不支持！");
								log.info("修改订单状态不支持！订单号{}",pay.getOrderNo());
							}
						}else if(PayEnum.PAYTYPE02.getCode().equals(pay.getType())){//提现（赎回）
							// 将提现订单状态修改为成功
							if (PayEnum.PAY1.getCode().equals(pay.getUpdateStatus())) {
								//未处理、处理中、超时的订单修改为成功，记账并回调
								if(PayEnum.PAY0.getCode().equals(oldOrderStatus)||PayEnum.PAY3.getCode().equals(oldOrderStatus)
										||PayEnum.PAY4.getCode().equals(oldOrderStatus)){
									log.info("修改提现订单，查询记账状态...");
									//查询记账状态
									String accountingTimes = transService.getWithdrawalsAccountStatus(pay.getUserOid(), pay.getOrderNo());
									log.info("获取记账状态，已记账{}次",accountingTimes);
									//未记账，先记账提现冻结户，再记账基本户和提现冻结户
									if(Constant.NOACCTING.equals(accountingTimes)){
										try {
											log.info("修改提现订单，第1记账，开始记账...");
											AccountTransResponse accountTransResponse1 = accounting(pay, null);
											if (Constant.SUCCESS.equals(accountTransResponse1.getReturnCode())) {
												log.info("记账成功");
												//记账完成,查询余额
												accountInfoService.getAccountBalanceByUserOid(pay.getUserOid());
												log.info("修改提现订单，第2记账，开始记账...");
												AccountTransResponse accountTransResponse2 = accounting(pay, null);
												if (Constant.SUCCESS.equals(accountTransResponse2.getReturnCode())) {
													log.info("记账成功");
													//记账完成,查询余额
													accountInfoService.getAccountBalanceByUserOid(pay.getUserOid());
													needSave = true;//保存修改后订单
													needCallBack = true;//回调业务系统
												}else{
													orderRes.setReturnCode(Constant.FAIL);
													orderRes.setErrorMessage("修改订单状态记账失败！");
													log.info("修改订单状态第二次记账失败！订单号{}",pay.getOrderNo());
												}
											} else {
												orderRes.setReturnCode(Constant.FAIL);
												orderRes.setErrorMessage("修改订单状态记账失败！");
												log.info("修改订单状态第一次记账失败！订单号{}",pay.getOrderNo());
											}
										} catch (Exception e) {
											orderRes.setReturnCode(Constant.FAIL);
											orderRes.setErrorMessage("修改订单状态记账异常！");
											log.error("修改订单状态第记账异常！订单号{}",pay.getOrderNo());
										}
									}else if(Constant.FROZENACCTING.equals(accountingTimes)){//已记账提现提现冻结户
										log.info("修改提现订单，已记账冻结户，开始记录基本户，第2记账，开始记账...");
										AccountTransResponse accountTransResponse2 = accounting(pay, null);
										if (Constant.SUCCESS.equals(accountTransResponse2.getReturnCode())) {
											log.info("记账成功");
											//记账完成,查询余额
											accountInfoService.getAccountBalanceByUserOid(pay.getUserOid());
											needSave = true;//保存修改后订单
											needCallBack = true;//回调业务系统
										}else{
											orderRes.setReturnCode(Constant.FAIL);
											orderRes.setErrorMessage("修改订单状态记账失败！");
											log.info("修改订单状态第二次记账失败！订单号{}",pay.getOrderNo());
										}
									}else if(Constant.BASICERACCTING.equals(accountingTimes)){//已记账基本户
										needSave = true;//保存修改后订单
										needCallBack = true;//回调业务系统
									}else{
										orderRes.setReturnCode(Constant.FAIL);
										orderRes.setErrorMessage("修改订单状态查询记账状态异常！");
										log.info("修改订单状态查询记账状态异常！订单号{}",pay.getOrderNo());
									}
								}else if(PayEnum.PAY2.getCode().equals(oldOrderStatus)){//失败订单改为成功
									//正常情况下，失败订单已完成记账，无需再次记账，直接回调业务系统
									log.info("修改提现订单，查询记账状态...");
									//查询记账状态
									String accountingTimes = transService.getWithdrawalsAccountStatus(pay.getUserOid(), pay.getOrderNo());
									log.info("获取记账状态，已记账{}次",accountingTimes);
									//已记账完成
									if(Constant.BASICERACCTING.equals(accountingTimes)){
										needSave = true;//保存修改后订单
										needCallBack = true;//回调业务系统
									}else{//未完成记账的失败订单，暂时不允许修改
										orderRes.setReturnCode(Constant.FAIL);
										orderRes.setErrorMessage("订单记账状态异常，不能修改订单状态！");
										log.info("订单记账状态异常，不能修改订单状态！订单号{}",pay.getOrderNo());
									}
								}else{
									orderRes.setReturnCode(Constant.FAIL);
									orderRes.setErrorMessage("修改订单状态不支持！");
									log.info("修改订单状态不支持！订单号{}",pay.getOrderNo());
								}
							}else{
								orderRes.setReturnCode(Constant.FAIL);
								orderRes.setErrorMessage("修改订单状态不支持！");
								log.info("修改订单状态不支持！订单号{}",pay.getOrderNo());
							}
						}else{
							orderRes.setReturnCode(Constant.FAIL);
							orderRes.setErrorMessage("修改订单类型不支持！");
							log.info("修改订单类型不支持！订单号{}",pay.getOrderNo());
						}
						//调用业务系统修改订单状态
						if(needChangeStatus){
							try {
								InteractiveRequest interactiveRequest = new InteractiveRequest();
								interactiveRequest.setOrderNo(req.getOrderNo());
								boolean changeStatus = settlementSdk.changeOrderStatus(interactiveRequest);// 调业务系统
								log.info("结算修改订单号{}的订单状态返回：{}", pay.getOrderNo(), JSONObject.toJSONString(changeStatus));
								if(!changeStatus){
									needSave = false;
									orderRes.setReturnCode(Constant.FAIL);
									orderRes.setErrorMessage("调用业务系统修改订单状态失败！");
									log.info("调用业务系统修改订单状态失败！订单号{}",pay.getOrderNo());
								}else{
									needSave = true;
								}
							} catch (Exception e) {
								log.error("sdk修改订单系统异常:" + e);
								orderRes.setReturnCode(Constant.FAIL);
								orderRes.setErrorMessage("修改订单系统异常！");
								log.error("修修改订单系统异常！订单号{}",pay.getOrderNo());
								needSave = false;
							}
						}
						//操作订单
						if(needSave){
							log.info("结算系统修改订单状态，记录交互日志，订单号{},原定单状态：{},修改为：{}",pay.getOrderNo(),pay.getCommandStatus(),pay.getUpdateStatus());
							// ---修改
							pay.setCommandStatus(pay.getUpdateStatus());
							pay.setLaunchplatform(PayEnum.PAYMETHOD2.getCode());
							pay.setUpdateTime(time);
							paymentDao.save(pay);

							// ---收单
							OrderVo order = comOrderDao.findByorderNo(pay.getOrderNo());
							order.setStatus(pay.getCommandStatus());
							order.setUpdateTime(time);
							comOrderDao.save(order);

							// ---交互日志
							BankLogVo banklog = bankLogDao.findByPayNo(pay.getPayNo());
							banklog.setTradStatus(pay.getCommandStatus());
							banklog.setUpdateTime(time);
							banklog.setLaunchplatform(pay.getLaunchplatform());
							bankLogDao.save(banklog);
							orderRes.setReturnCode(Constant.SUCCESS);
						}
						//回调业务系统
						if(needCallBack){
							log.info("回调业务系统！");
							String returnCode = Constant.FAIL;
							if(PayEnum.PAY1.getCode().equals(pay.getCommandStatus())){
								returnCode = Constant.SUCCESS;
							}
							CallBackInfo callBackInfo = CallBackInfo.builder().orderNO(pay.getOrderNo())
									.tradeType(pay.getType()).payNo(pay.getPayNo()).channelNo(pay.getChannelNo())
									.type("settlement").minute(1).totalCount(20).totalMinCount(20).countMin(0)
									.returnCode(returnCode).status(CallBackEnum.INIT.getCode())
									.createTime(new Date()).build();
							callbackDao.save(callBackInfo);
							orderRes.setReturnCode(Constant.SUCCESS);
						}
						
						payTwoRedisUtil.delRedis("pay_order_submit_redis_tag" + req.getOrderNo());
					//20170817去掉撤单功能
//					} else if (req.getOperatorType().trim().equals(PayEnum.OPERATORTYPE03.getCode())) {
//						//20170401，撤销先记账，记账成功后调用业务系统
//						AccountTransResponse accountTransResponse = new AccountTransResponse();
//						if(TradeType.payee.getValue().equals(pay.getType())){
//							//查询是否已撤单
//							AccOrderEntity accOrderEntity = accOrderService.getOrderByNo(pay.getOrderNo());
//							if(accOrderEntity != null&&AccOrderEntity.ORDERSTATUS_KILL.equals(accOrderEntity.getOrderStatus())){
//								log.info("账户已撤单,不需要再次撤单：{}",pay.getOrderNo());
//								accountTransResponse.setReturnCode(Constant.SUCCESS);
//								accountTransResponse.setErrorMessage("账户已撤单");
//							}else {
//								//查询该订单记账状态
//								String accountingTimes = accountTradeService.getWithdrawalsAccountStatus(pay.getUserOid(), pay.getOrderNo());
//								if(!"0".equals(accountingTimes)&&!StringUtil.isEmpty(accountingTimes)){//未记账的撤单无需操作账户,标记为记账成功
//									try {
//										log.info("撤单开始，开始记账...");
//										 accountTransResponse = accounting(pay, Constant.KILLORDER);
//									} catch (Exception e) {
//										log.error("记账异常,定单号：{},错误信息：{}", pay.getOrderNo(), e);
//										accountTransResponse.setReturnCode(Constant.FAIL);
//										accountTransResponse.setErrorMessage("账户撤单记账没有成功，请联系结算账户人员!");
//									}
//								}else if(!StringUtil.isEmpty(accountingTimes)){
//									accountTransResponse.setReturnCode(Constant.SUCCESS);
//									accountTransResponse.setErrorMessage("成功");
//								}
//							}
//						}
//						if (Constant.SUCCESS.equals(accountTransResponse.getReturnCode())) {
//							log.info("记账成功");
//							// ---撤销
//							WriterOffOrderRequest witer = new WriterOffOrderRequest();
//							witer.setOriginalRedeemOrderCode(pay.getOrderNo());
//							BaseResponse isRest = settlementSdk.writerOffOrder(witer);
//							log.info("结算撤销返回：{}", JSONObject.toJSONString(isRest));
//							if (isRest != null && isRest.getReturnCode().equals(Constant.SUCCESS)) {
//								pay.setCommandStatus(PayEnum.PAY5.getCode());
//								paymentDao.save(pay);
//
//								// ---收单
//								OrderVo order = comOrderDao.findByorderNo(pay.getOrderNo());
//								order.setStatus(pay.getCommandStatus());
//								order.setUpdateTime(time);
//								comOrderDao.save(order);
//
//								// ---交互日志
//								BankLogVo banklog = bankLogDao.findByPayNo(pay.getPayNo());
//								banklog.setTradStatus(pay.getCommandStatus());
//								banklog.setUpdateTime(time);
//								bankLogDao.save(banklog);
//								orderRes.setReturnCode(Constant.SUCCESS);
//								
//								payTwoRedisUtil.delRedis("pay_order_submit_redis_tag" + req.getOrderNo());
//							} else if (isRest != null) {
//								orderRes.setReturnCode(isRest.getReturnCode());
//								orderRes.setErrorMessage(isRest.getErrorMessage());
//							} else {
//								orderRes.setReturnCode(Constant.FAIL);
//								orderRes.setErrorMessage("调用业务接口修改状态没有成功，请联系业务人员!");
//							}
//						} else {
//							log.info("记账失败，accountTransResponse ={}",
//									JSONObject.toJSON(accountTransResponse));
//							orderRes.setReturnCode(Constant.FAIL);
//							orderRes.setErrorMessage("账户撤单记账没有成功，请联系结算账户人员!");
//						}
//						payTwoRedisUtil.delRedis("pay_order_submit_redis_tag" + req.getOrderNo());
					} else if (req.getOperatorType().trim().equals(PayEnum.OPERATORTYPE01.getCode())) {
						// ---补单|支付
						if (!pay.getCommandStatus().equals(PayEnum.PAY1.getCode())) {
							// 拦截点击两次的，对写成功的做处理
							Long check = payTwoRedisUtil.setRedisByTime("pay_order_redis_tag" + pay.getOrderNo(),
									pay.getOrderNo());
							if (check.intValue() != 0) {
								pay.setLaunchplatform(PayEnum.PAYMETHOD2.getCode());
//								orderRes = comOrderBootService.feed(pay);
								payTwoRedisUtil.delRedis("pay_order_submit_redis_tag" + req.getOrderNo());
							}
						}
					}
				} else {
					paymentDao.save(pay);
					orderRes.setReturnCode(Constant.SUCCESS);
					payTwoRedisUtil.delRedis("pay_order_submit_redis_tag" + req.getOrderNo());
				}
			} else {
				orderRes.setReturnCode(Constant.FAIL);
				orderRes.setErrorMessage("无效的Id！");

			}

		} else {
			orderRes.setReturnCode(Constant.FAIL);
			orderRes.setErrorMessage("无效的订单Id或者无效的Id");
		}
		return orderRes;
	}

	/**
	 * 批量提交审核【修改状态|补单|支付|撤单】
	 */
	public OrderResponse updateBath(PaymentForm req) {
		log.info("{},批量修改状态|补单|支付|撤单,", req.getUserOid(), JSONObject.toJSONString(req));

		Timestamp time = new Timestamp(System.currentTimeMillis());
		OrderResponse orderRes = new OrderResponse();

		// 提交审核全部提交
		if (!StringUtil.isEmpty(req.getSumbitTy()) && req.getSumbitTy().equals("0")) {
			List<String> bathOid = getTotalOid(req);
			if (!bathOid.isEmpty()) {
				req.setBathOid((String[]) bathOid.toArray());
			}
		}
		if (req.getBathOid() != null && req.getBathOid().length > 0) {
			for (String str : req.getBathOid()) {
				PaymentVo pay = paymentDao.findOne(str);
				if (pay != null) {
					Long check = payTwoRedisUtil.setRedis("pay_order_submit_redis_tag" + pay.getOrderNo(),
							pay.getOrderNo());
					if (check == 1) {
						// 单笔支付(失败重发)：01；修改状态：04；撤销：03
						if ((!StringUtil.isEmpty(req.getBathOperatorTag()))
								&& (req.getBathOperatorTag().equals("01") || req.getBathOperatorTag().equals("02"))) {
							pay.setOperator(req.getUserOid());
							pay.setOperatorReson(req.getBathReson());
							pay.setOperatorStatus(req.getBathOperatorTag());
							pay.setAuditOperatorStatus(null);
							pay.setOperatorTime(time);
						} else if ((!StringUtil.isEmpty(req.getBathOperatorTag()))
								&& (req.getBathOperatorTag().equals("04"))) {
							pay.setUpdateStatus(req.getUpdateStatus());
							pay.setUpdateReson(req.getBathReson());
							pay.setUpdateOperator(req.getUserOid());
							pay.setOperatorStatus(req.getBathOperatorTag());
							pay.setAuditUpdateStatus(null);
							pay.setUpTime(time);
						} else if ((!StringUtil.isEmpty(req.getBathOperatorTag()))
								&& (req.getBathOperatorTag().equals("03"))) {
							pay.setResetOperatorStatus(req.getBathOperatorTag());
							pay.setResetOpertatorReson(req.getBathReson());
							pay.setResetOperator(req.getUserOid());
							pay.setAuditResetOperatorStatus(null);
							pay.setResetOperatorTime(time);
						}
						pay.setUpdateTime(time);
						paymentDao.saveAndFlush(pay);
						log.info(str + ":提交审核成功！");
					} else {
						log.error(str + ":上一步操作未走完,不能进行下一步操作！");
					}

				} else {
					log.error(str + ":指令id错误！！");
				}
			}
		} else {
			log.error("指令id不能为空！");
		}
		orderRes.setReturnCode(Constant.SUCCESS);
		return orderRes;
	}

	/***
	 * 批量审核操作[通过]|【不通过】
	 */
	/*public OrderResponse auditBath(PaymentForm req) {
		log.info("{},批量审核状态|补单|支付|撤单,", req.getUserOid(), JSONObject.toJSONString(req));
		OrderResponse orderRes = new OrderResponse();
		Timestamp time = new Timestamp(System.currentTimeMillis());

		// 审核全部提交
		if (!StringUtil.isEmpty(req.getSumbitTy()) && req.getSumbitTy().equals("1")) {
			List<String> bathOid = getTotalOid(req);
			if (!bathOid.isEmpty()) {
				req.setBathOid((String[]) bathOid.toArray());
			}
		}
		if (req.getBathOid() != null && req.getBathOid().length > 0) {
			for (String str : req.getBathOid()) {
				PaymentVo pay = paymentDao.findOne(str);
				if (pay != null) {
					// 判断提交的审核是否与审核的操作一致（提交操作类型=审核操作类型）
					if ((!StringUtil.isEmpty(pay.getOperatorStatus())
							&& req.getBathOperatorTag().equals(pay.getOperatorStatus()))
							|| (!StringUtil.isEmpty(pay.getResetOperatorStatus())
									&& req.getBathOperatorTag().equals(pay.getResetOperatorStatus()))) {
						pay.setUpdateTime(time);
						if (req.getBathOperatorTag().trim().equals(PayEnum.OPERATORTYPE01.getCode())
								|| req.getBathOperatorTag().trim().equals("04")) {
							pay.setAuditOperator(req.getUserOid());
							pay.setAuditOperatorReson(req.getBathReson());
							pay.setAuditOperatorStatus(req.getAuditStatus());
							pay.setAuditOperatorTime(time);
						} else if (req.getBathOperatorTag().trim().equals(PayEnum.OPERATORTYPE02.getCode())) {
							pay.setAuditUpdateOperator(req.getUserOid());
							pay.setAuditUpdateReson(req.getBathReson());
							pay.setAuditUpdateStatus(req.getAuditStatus());
							pay.setAuditUpdateTime(time);
						} else if (req.getBathOperatorTag().trim().equals(PayEnum.OPERATORTYPE03.getCode())) {
							pay.setAuditResetOperator(req.getUserOid());
							pay.setAuditResetOperatorReson(req.getBathReson());
							pay.setAuditResetOperatorStatus(req.getAuditStatus());
							pay.setAuditResetOperatorTime(time);
						}
						if (req.getAuditStatus().trim().equals(PayEnum.AUDIT1.getCode())) {
							if (req.getBathOperatorTag().trim().equals("04")) {
								// 添加一条回调日志
								if (pay.getUpdateStatus().equals(PayEnum.PAY1.getCode())) {
									CallBackInfo callBackInfo = CallBackInfo.builder().orderNO(pay.getOrderNo())
											.tradeType(pay.getType()).payNo(pay.getPayNo())
											.channelNo(pay.getChannelNo()).type("settlement").minute(1).totalCount(20)
											.totalMinCount(20).countMin(0).returnCode(Constant.SUCCESS)
											.status(CallBackEnum.INIT.getCode()).createTime(new Date()).build();
									callbackDao.save(callBackInfo);
								}

								// ---修改
								pay.setCommandStatus(req.getUpdateStatus());
								pay.setLaunchplatform(PayEnum.PAYMETHOD2.getCode());
								pay.setUpdateTime(time);
								paymentDao.save(pay);

								// ---收单
								OrderVo order = comOrderDao.findByorderNo(pay.getOrderNo());
								order.setStatus(pay.getCommandStatus());
								order.setUpdateTime(time);
								comOrderDao.save(order);

								// ---交互日志
								BankLogVo banklog = bankLogDao.findByPayNo(pay.getPayNo());
								banklog.setTradStatus(pay.getCommandStatus());
								banklog.setUpdateTime(time);
								banklog.setLaunchplatform(pay.getLaunchplatform());
								bankLogDao.save(banklog);
								payTwoRedisUtil.delRedis("pay_order_submit_redis_tag" + pay.getOrderNo());
								log.info(str + ":修改审核成功！");
//							} else if (req.getBathOperatorTag().trim().equals(PayEnum.OPERATORTYPE03.getCode())) {
//								// ---撤销
//								WriterOffOrderRequest witer = new WriterOffOrderRequest();
//								witer.setOriginalRedeemOrderCode(pay.getOrderNo());
//								BaseResponse isRest = settlementSdk.writerOffOrder(witer);
//								log.info("结算撤销返回：{}", JSONObject.toJSONString(isRest));
//								if (isRest != null && isRest.getReturnCode().equals(Constant.SUCCESS)) {
//									pay.setCommandStatus(PayEnum.PAY5.getCode());
//									paymentDao.save(pay);
//
//									// ---收单
//									OrderVo order = comOrderDao.findByorderNo(pay.getOrderNo());
//									order.setStatus(pay.getCommandStatus());
//									order.setUpdateTime(time);
//									comOrderDao.save(order);
//
//									// ---交互日志
//									BankLogVo banklog = bankLogDao.findByPayNo(pay.getPayNo());
//									banklog.setTradStatus(pay.getCommandStatus());
//									banklog.setUpdateTime(time);
//									bankLogDao.save(banklog);
//									payTwoRedisUtil.delRedis("pay_order_submit_redis_tag" + pay.getOrderNo());
//									log.info(str + ":撤销审核成功！");
//								} else if (isRest != null) {
//									log.error(str + ":撤销审核" + JSONObject.toJSONString(isRest));
//								} else {
//									log.error(str + ":撤销审核调用业务接口修改状态没有成功，请联系业务人员!");
//								}
							} else if ("01,02".indexOf(req.getBathOperatorTag()) != -1) {
								// ---补单|支付
								if (!pay.getCommandStatus().equals(PayEnum.PAY1.getCode())) {
									// 拦截点击两次的，对写成功的做处理
									Long check = payTwoRedisUtil
											.setRedisByTime("pay_order_redis_tag" + pay.getOrderNo(), pay.getOrderNo());
									if (check.intValue() != 0) {
										pay.setLaunchplatform(PayEnum.PAYMETHOD2.getCode());
//										orderRes = comOrderBootService.feed(pay);
										payTwoRedisUtil.delRedis("pay_order_submit_redis_tag" + req.getOrderNo());
									}
								}
							}
						} else {
							paymentDao.save(pay);
							log.error(str + ":审核不通过成功");
							payTwoRedisUtil.delRedis("pay_order_submit_redis_tag" + str);
						}
					}

				} else {
					log.error(str + ":无效的Id！");
				}
			}
		} else {
			log.error("无效的订单Id或者无效的Id");
		}
		orderRes.setReturnCode(Constant.SUCCESS);
		return orderRes;
	}*/

	public PaymentVoRes page(PaymentForm req) {
		log.info("{},支付指令查询,{},", req.getUserOid(), JSONObject.toJSONString(req));
		Page<PaymentVo> listPage = paymentDao.findAll(buildSpecification(req),
				new PageRequest(req.getPage() - 1, req.getRows()));
		PaymentVoRes res = new PaymentVoRes();
		if (listPage != null && listPage.getSize() > 0) {
			if (listPage.getContent().size() > 0) {
				for (PaymentVo vo : listPage.getContent()) {
					vo.setCardNo(DesPlus.decrypt(vo.getCardNo()));
					UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(vo.getUserOid());
	                if(null!=userInfoEntity) {
	                	vo.setPhone(userInfoEntity.getPhone());
	                }
				}
			}
			res.setRows(listPage.getContent());
			res.setTotalPage(listPage.getTotalPages());
			res.setPage(req.getPage());
			res.setRow(req.getRows());
			res.setTotal(listPage.getTotalElements());
			return res;
		}
		return null;
	}

	/**
	 * 20170309，提现审核页面查询修改 调用业务系统查询用户的注册时间及账户可用余额 时间紧急，暂copy一份处理，防止出现问题导致原查询不可用
	 * 
	 * @param req
	 * @return
	 */
	public PaymentVoRes page2(PaymentForm req) {
		log.info("{},提现审核查询,{},", req.getUserOid(), JSONObject.toJSONString(req));
		Page<PaymentVo> listPage = paymentDao.findAll(buildSpecification(req),
				new PageRequest(req.getPage() - 1, req.getRows()));
		PaymentVoRes res = new PaymentVoRes();
		res.setRows(listPage.getContent());
		res.setTotalPage(listPage.getTotalPages());
		res.setPage(req.getPage());
		res.setRow(req.getRows());
		res.setTotal(listPage.getTotalElements());
		if (listPage != null && listPage.getSize() > 0) {
			if (listPage.getContent().size() > 0) {
				// 拿到查询的分页list，取出userOidList去业务系统查询用户创建时间及可用余额
//				String[] userOid = new String[listPage.getContent().size()];
//				int i = 0;
				for (PaymentVo vo : listPage.getContent()) {
					vo.setCardNo(DesPlus.decrypt(vo.getCardNo()));
//					userOid[i] = vo.getUserOid();
//					i++;
				}
//				List<UserAccountInfoResponse> userAccountInfoList = null;
//				try {
//					// userOid = deleteRepeat(userOid);//去重
//					userAccountInfoList = settlementSdk.getUserAccountInfo(userOid);
//				} catch (Exception e) {
//					log.error("提现审核获取用户信息异常：" + e);
//				}
//				if (null != userAccountInfoList && userAccountInfoList.size() > 0) {
//					for (PaymentVo vo : listPage.getContent()) {
//						for (UserAccountInfoResponse resp : userAccountInfoList) {
//							if (vo.getUserOid().equals(resp.getMemberId())) {
//								vo.setEmergencyMark(resp.getCreateTime());//用户注册时间
//								vo.setDistanceMark(resp.getBalance().toString());//提现后可用余额
////								log.info("业务返回用户状态:{}",resp.isFrozen());
//								vo.setCrossFlag("已锁定");//是否锁定，默认锁定
//								if(!resp.isFrozen()){
//									vo.setCrossFlag("正常");//是否锁定
//								}
//							}
//						}
//					}
//				}

			}
			res.setRows(listPage.getContent());
			res.setTotalPage(listPage.getTotalPages());
			res.setPage(req.getPage());
			res.setRow(req.getRows());
			res.setTotal(listPage.getTotalElements());
		}
		return res;
	}

	public Specification<PaymentVo> buildSpecification(final PaymentForm req) {
		Specification<PaymentVo> spec = new Specification<PaymentVo>() {
			@Override
			public Predicate toPredicate(Root<PaymentVo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> bigList = new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(req.getCreator()) && req.getAutorType() != 0) {
					List<Predicate> chirldList = new ArrayList<>();
					chirldList.add(cb.isNotNull(root.get("operatorStatus")));
					chirldList.add(cb.isNotNull(root.get("updateStatus")));
					chirldList.add(cb.isNotNull(root.get("resetOperatorStatus")));
					bigList.add(cb.or(chirldList.toArray(new Predicate[chirldList.size()])));
				}
				if (!StringUtil.isEmpty(req.getLaunchplatform()))
					bigList.add(cb.equal(root.get("launchplatform").as(String.class), req.getLaunchplatform()));
				if (!StringUtil.isEmpty(req.getAuditStatus()))
					bigList.add(cb.equal(root.get("auditStatus").as(String.class), req.getAuditStatus()));
				if (!StringUtil.isEmpty(req.getUserOid())) {
					bigList.add(cb.equal(root.get("userOid").as(String.class), req.getUserOid()));
				} else if (!StringUtil.isEmpty(req.getPhone())) {
					UserInfoEntity userInfoEntity = userInfoService.getAccountUserByPhone(req.getPhone());
					if (null == userInfoEntity) { //构造一个错误的userOid
						bigList.add(cb.equal(root.get("userOid").as(String.class), StringUtil.uuid()));
					} else {
						bigList.add(cb.equal(root.get("userOid").as(String.class), userInfoEntity.getUserOid()));
					}
				}
				
				if (!StringUtil.isEmpty(req.getType()))
					bigList.add(cb.equal(root.get("type").as(String.class), req.getType()));
				if (!StringUtil.isEmpty(req.getReconStatus()))
					bigList.add(
							cb.equal(root.get("reconStatus").as(Integer.class), Integer.valueOf(req.getReconStatus())));
				if (!StringUtil.isEmpty(req.getCommandStatus()))
					bigList.add(cb.equal(root.get("commandStatus").as(String.class), req.getCommandStatus()));
				if (!StringUtil.isEmpty(req.getRealName()))
					bigList.add(cb.like(root.get("realName").as(String.class), "%" + req.getRealName() + "%"));
				if (!StringUtil.isEmpty(req.getChannelNo()))
					bigList.add(cb.like(root.get("channelNo").as(String.class), "%" + req.getChannelNo() + "%"));
				if (!StringUtil.isEmpty(req.getOrderNo()))
					bigList.add(cb.like(root.get("orderNo").as(String.class), "%" + req.getOrderNo() + "%"));
				if (!StringUtil.isEmpty(req.getPayNo()))
					bigList.add(cb.like(root.get("payNo").as(String.class), "%" + req.getPayNo() + "%"));
				if (!StringUtil.isEmpty(req.getLimitAmount()))
					bigList.add(cb.greaterThanOrEqualTo(root.get("amount").as(BigDecimal.class),
							BigDecimal.valueOf(Long.valueOf(req.getLimitAmount()))));
				if (!StringUtil.isEmpty(req.getMaxAmount()))
					bigList.add(cb.lessThanOrEqualTo(root.get("amount").as(BigDecimal.class),
							BigDecimal.valueOf(Long.valueOf(req.getMaxAmount()))));
				if (!StringUtil.isEmpty(req.getBeginTime())) {
					java.util.Date beginDate = DateUtil
							.beginTimeInMillis(DateUtil.parseDate(req.getBeginTime(), Constant.fomat));
					bigList.add(cb.greaterThanOrEqualTo(root.get("createTime").as(Timestamp.class),
							new Timestamp(beginDate.getTime())));
				}
				if (!StringUtil.isEmpty(req.getEndTime())) {
					java.util.Date beginDate = DateUtil
							.endTimeInMillis(DateUtil.parseDate(req.getEndTime(), Constant.fomat));
					bigList.add(cb.lessThanOrEqualTo(root.get("createTime").as(Timestamp.class),
							new Timestamp(beginDate.getTime())));
				}
				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
				query.orderBy(cb.desc(root.get("createTime")), cb.desc(root.get("updateTime")));

				// 条件查询
				return query.getRestriction();
			}
		};
		return spec;
	}

	/**
	 * 获取指令id集合
	 * 
	 * @param req
	 * @return
	 */
	public List<String> getTotalOid(PaymentForm req) {

		String userOid = req.getUserOid();
		if (StringUtil.isEmpty(userOid)) {// ""
			userOid = "";
		}

		String realname = req.getRealName();
		if (StringUtil.isEmpty(realname)) {// ""
			realname = "";
		} else {
			realname = "%" + realname + "%";
		}
		String channelNo = req.getChannelNo();
		if (StringUtil.isEmpty(channelNo)) {// ""
			channelNo = "";
		} else {
			channelNo = "%" + channelNo + "%";
		}
		String orderNo = req.getOrderNo();
		if (StringUtil.isEmpty(orderNo)) {// ""
			orderNo = "";
		} else {
			orderNo = "%" + orderNo + "%";
		}
		String payNo = req.getPayNo();
		if (StringUtil.isEmpty(payNo)) {// ""
			payNo = "";
		} else {
			payNo = "%" + payNo + "%";
		}

		String commandStatus = req.getCommandStatus();
		if (StringUtil.isEmpty(commandStatus)) {// ""
			commandStatus = "";
		}

		List<String> oList = new ArrayList<>();
		try {
			Object[] oidList = paymentDao.queryOid(req.getSumbitTy(), userOid, realname, orderNo, payNo,
					req.getBeginTime(), req.getEndTime());
			if (oidList != null && oidList.length > 0) {
				for (Object ob : oidList) {
					Object[] o = (Object[]) ob;
					String str = nullToStr(o[0]);
					if (!StringUtil.isEmpty(str)) {
						oList.add(str);
					}

				}
			}
		} catch (Exception e) {
			log.error("全部提交报错：", e);
		}

		return oList;
	}

	String nullToStr(Object str) {
		if (null == str) {
			return "";
		}
		return str.toString();
	}

	public PaymentVoRes pageBath(PaymentForm req) {
		log.info("{},批量审核支付指令查询,{},", req.getUserOid(), JSONObject.toJSONString(req));
		Page<PaymentVo> listPage = paymentDao.findAll(buildSpecificationBath(req),
				new PageRequest(req.getPage() - 1, req.getRows()));
		PaymentVoRes res = new PaymentVoRes();
		if (listPage != null && listPage.getSize() > 0) {
			if (listPage.getContent().size() > 0) {
				for (PaymentVo vo : listPage.getContent()) {
					vo.setCardNo(DesPlus.decrypt(vo.getCardNo()));
				}
			}
			res.setRows(listPage.getContent());
			res.setTotalPage(listPage.getTotalPages());
			res.setPage(req.getPage());
			res.setRow(req.getRows());
			res.setTotal(listPage.getTotalElements());
			return res;
		}
		return null;
	}

	public Specification<PaymentVo> buildSpecificationBath(final PaymentForm req) {
		Specification<PaymentVo> spec = new Specification<PaymentVo>() {
			@Override
			public Predicate toPredicate(Root<PaymentVo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> bigList = new ArrayList<Predicate>();

				// 赎回状态不为撤销和成功
				bigList.add(cb.equal(root.get("type").as(String.class), PayEnum.PAYTYPE02.getCode()));
				bigList.add(cb.notEqual(root.get("commandStatus").as(String.class), PayEnum.PAY5.getCode()));
				bigList.add(cb.notEqual(root.get("commandStatus").as(String.class), PayEnum.PAY1.getCode()));

				List<Predicate> chirldList = new ArrayList<>();
				if (req.getAutorType() == 0) {
					// 提交审核页面查询非审核中的数据(状态等null || 审核状态不等于 null)
					List<Predicate> simalO = new ArrayList<>();
					simalO.add(cb.isNull(root.get("operatorStatus")));
					simalO.add(cb.isNotNull(root.get("auditOperatorStatus")));
					chirldList.add(cb.or(simalO.toArray(new Predicate[simalO.size()])));

					List<Predicate> simalU = new ArrayList<>();
					simalU.add(cb.isNull(root.get("updateStatus")));
					simalU.add(cb.isNotNull(root.get("auditUpdateStatus")));
					chirldList.add(cb.or(simalU.toArray(new Predicate[simalU.size()])));

					List<Predicate> simalR = new ArrayList<>();
					simalR.add(cb.isNull(root.get("resetOperatorStatus")));
					simalR.add(cb.isNotNull(root.get("auditResetOperatorStatus")));
					chirldList.add(cb.or(simalR.toArray(new Predicate[simalR.size()])));

					bigList.add(cb.and(chirldList.toArray(new Predicate[chirldList.size()])));
				} else {
					// 审核页面查询审核中的数据(状态不等null && 审核状态等于 null)
					List<Predicate> simalO = new ArrayList<>();
					simalO.add(cb.isNotNull(root.get("operatorStatus")));
					simalO.add(cb.isNull(root.get("auditOperatorStatus")));
					chirldList.add(cb.and(simalO.toArray(new Predicate[simalO.size()])));

					List<Predicate> simalU = new ArrayList<>();
					simalU.add(cb.isNotNull(root.get("updateStatus")));
					simalU.add(cb.isNull(root.get("auditUpdateStatus")));
					chirldList.add(cb.and(simalU.toArray(new Predicate[simalU.size()])));

					List<Predicate> simalR = new ArrayList<>();
					simalR.add(cb.isNotNull(root.get("resetOperatorStatus")));
					simalR.add(cb.isNull(root.get("auditResetOperatorStatus")));
					chirldList.add(cb.and(simalR.toArray(new Predicate[simalR.size()])));

					bigList.add(cb.or(chirldList.toArray(new Predicate[chirldList.size()])));
				}

				if (!StringUtil.isEmpty(req.getCommandStatus()))
					bigList.add(cb.equal(root.get("commandStatus").as(String.class), req.getCommandStatus()));
				if (!StringUtil.isEmpty(req.getRealName()))
					bigList.add(cb.like(root.get("realName").as(String.class), "%" + req.getRealName() + "%"));
				if (!StringUtil.isEmpty(req.getChannelNo()))
					bigList.add(cb.like(root.get("channelNo").as(String.class), "%" + req.getChannelNo() + "%"));
				if (!StringUtil.isEmpty(req.getOrderNo()))
					bigList.add(cb.like(root.get("orderNo").as(String.class), "%" + req.getOrderNo() + "%"));
				if (!StringUtil.isEmpty(req.getPayNo()))
					bigList.add(cb.like(root.get("payNo").as(String.class), "%" + req.getPayNo() + "%"));
				if (!StringUtil.isEmpty(req.getBeginTime())) {
					java.util.Date beginDate = DateUtil.parseDate(req.getBeginTime(), Constant.fomat);
					bigList.add(cb.greaterThanOrEqualTo(root.get("createTime").as(Timestamp.class),
							new Timestamp(beginDate.getTime())));
				}
				if (!StringUtil.isEmpty(req.getEndTime())) {
					java.util.Date beginDate = DateUtil.parseDate(req.getEndTime(), Constant.fomat);
					bigList.add(cb.lessThanOrEqualTo(root.get("createTime").as(Timestamp.class),
							new Timestamp(beginDate.getTime())));
				}
				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
				query.orderBy(cb.desc(root.get("createTime")), cb.desc(root.get("updateTime")));

				// 条件查询
				return query.getRestriction();
			}
		};
		return spec;
	}

	/**
	 * 返回地址
	 * 
	 * @param req
	 * @return
	 * @throws Exception
	 */
	public String findPaymentWithReconc(PaymentForm req) throws Exception {
		log.info("{},拉取对账文件,{}", req.getUserOid(), JSONObject.toJSONString(req));
		if (req.getReconciliationDate() != null) {
			java.util.Date beginDate = DateUtil
					.beginTimeInMillis(DateUtil.parseDate(req.getReconciliationDate(), Constant.fomat));
			java.util.Date endDate = DateUtil
					.endTimeInMillis(DateUtil.parseDate(req.getReconciliationDate(), Constant.fomat));
			List<String> files = new ArrayList<>();
			// 输出
			String parnetFile = "file";
			File f = new File(parnetFile);
			if (!f.exists()) {
				f.mkdirs();
			}

			long count = paymentDao.findCount(new Timestamp(beginDate.getTime()), new Timestamp(endDate.getTime()));
			if (count > 0) {
				long size = 5000;
				int page = Integer.valueOf(String.valueOf(count % size > 0 ? ((count / size) + 1) : (count / size)));
				List<PaymentVo> listVos = null;

				for (int i = 0; i < page; i++) {
					long pageIndex = 0;
					if (i != 0) {
						pageIndex = size * new Long(i);
					}
					listVos = paymentDao.findByRecon(new Timestamp(beginDate.getTime()),
							new Timestamp(endDate.getTime()), pageIndex, size);
					File chilFile = File.createTempFile(req.getReconciliationDate() + i, ".csv", new File(parnetFile));
					if (!chilFile.exists()) {
						chilFile.mkdirs();
					}
					List<String> strs = setStrs(listVos);
					boolean isBug = CSVUtil.exportCsv(chilFile, strs);
					if (isBug) {
						files.add(chilFile.toString());
					}
				}
			}
			try {
				if (!files.isEmpty()) {
					String zipFile = "file/" + req.getReconciliationDate() + ".zip";
					ZipCompressor zip = new ZipCompressor(zipFile);
					zip.compressExe(files);
					for (String str : files) {
						if (new File(str).exists()) {
							new File(str).delete();
						}
					}
				}

			} catch (Exception e) {
				log.error("拉取对账文件异常",e);
			}
			return parnetFile;
		} else {
			throw new SETException("请输入日期！");
		}
	}

	public List<String> setStrs(List<PaymentVo> listVos) {
		// 集合格式化数据
		List<String> strs = new ArrayList<String>();
		strs.add("序号,订单号,交易状态,交易金额,交易类型,失败详情");
		if (!listVos.isEmpty()) {
			StringBuffer sub = null;
			int index = 0;
			for (PaymentVo vo : listVos) {
				sub = new StringBuffer();
				if (StringUtil.isEmpty(vo.getOrderNo())) {
					vo.setOrderNo("");
				}
				if (StringUtil.isEmpty(vo.getCommandStatus())) {
					vo.setCommandStatus("");
				} else {
					if (vo.getCommandStatus().trim().equals(PayEnum.PAY0.getCode())) {
						vo.setCommandStatus("toHandle");
					} else if (vo.getCommandStatus().trim().equals(PayEnum.PAY1.getCode())) {
						vo.setCommandStatus("success");
					} else if (vo.getCommandStatus().trim().equals(PayEnum.PAY2.getCode())) {
						vo.setCommandStatus("failure");
					} else if (vo.getCommandStatus().trim().equals(PayEnum.PAY3.getCode())) {
						vo.setCommandStatus("processing");
					} else if (vo.getCommandStatus().trim().equals(PayEnum.PAY4.getCode())) {
						vo.setCommandStatus("expired");
					}
				}
				if (StringUtil.isEmpty(vo.getType())) {
					vo.setType("");
				} else {
					vo.setType(vo.getType().trim().equals(PayEnum.PAYTYPE01.getCode()) ? "invest" : "redeem");
				}
				if (StringUtil.isEmpty(vo.getFailDetail())) {
					vo.setFailDetail("");
				}
				index++;
				sub.append(index + "," + vo.getOrderNo() + "," + vo.getCommandStatus() + "," + vo.getAmount() + ","
						+ vo.getType() + "," + vo.getFailDetail());
				strs.add(sub.toString());
			}
		}
		return strs;
	}

	public OrderResponse findPayStatus(OrderRequest req) {
		log.info("交易状态查询,{}", JSONObject.toJSONString(req));
		OrderResponse res = new OrderResponse();
		PaymentVo vo = paymentDao.findByOrderNo(req.getOrderNo());
		if (vo != null) {
			TradeEvent tradeEvent = new TradeEvent();
			tradeEvent.setOrderNo(vo.getPayNo());
			if (req.getType().trim().equals(PayEnum.PAYTYPE01.getCode())) {
				// 申购查询
				tradeEvent.setTradeType(req.getType());
				tradeEvent.setChannel("1");
				log.info("快付通状态请求查询,{}", JSONObject.toJSONString(tradeEvent));
				event.publishEvent(tradeEvent);
				log.info("快付通状态返回查询,{}", JSONObject.toJSONString(tradeEvent));

			} else {
				// 赎回查询
				tradeEvent.setTradeType("tradeRecord");
				tradeEvent.setChannel("lycheepay");
				log.info("平安 银行状态请求查询,{}", JSONObject.toJSONString(tradeEvent));
				event.publishEvent(tradeEvent);
				log.info("平安 银行状态返回查询,{}", JSONObject.toJSONString(tradeEvent));
			}
			if (tradeEvent.getReturnCode().equals(Constant.SUCCESS)) {
				res.setStatus(PayEnum.PAY1.getCode());
			} else {
				res.setStatus(PayEnum.PAY2.getCode());
			}
			res.setErrorMessage(tradeEvent.getErrorDesc());
			res.setReturnCode(tradeEvent.getReturnCode());
			return res;
		}
		res.setErrorMessage("无效的订单号！");
		res.setReturnCode(Constant.FAIL);
		return res;
	}

	/**
	 * 异常订单查询
	 * 
	 * @param startDate
	 * @param endDate
	 * @param reconciliationMark
	 * @param page
	 * @param row
	 * @return
	 */
	public PaymentVoRes pageEX(String channelNo, Timestamp startDate, Timestamp endDate, String reconciliationMark,
			int page, int row) {
		long total;// 总条数
		List<PaymentVo> list;
		if ("all".endsWith(channelNo)) {
			if ("all".equals(reconciliationMark)) {
				total = paymentDao.findAllEXCount(startDate, endDate);// 总条数
				list = paymentDao.findAllEXRecon(startDate, endDate, (page - 1) * row, row);
			} else {
				total = paymentDao.findEXCount(reconciliationMark, startDate, endDate);// 总条数
				list = paymentDao.findEXRecon(reconciliationMark, startDate, endDate, (page - 1) * row, row);
			}
		} else {
			if ("all".equals(reconciliationMark)) {
				total = paymentDao.findAllEXCountByChannelNo(channelNo, startDate, endDate);// 总条数
				list = paymentDao.findAllEXReconByChannelNo(channelNo, startDate, endDate, (page - 1) * row, row);
			} else {
				total = paymentDao.findEXCountByChannelNo(channelNo, reconciliationMark, startDate, endDate);// 总条数
				list = paymentDao.findEXReconByChannelNo(channelNo, reconciliationMark, startDate, endDate,
						(page - 1) * row, row);
			}
		}
		List<PaymentVo> newlist = new ArrayList<PaymentVo>();
		if (list.size() > 0) {
			for (PaymentVo vo : list) {
				vo.setCardNo(DesPlus.decrypt(vo.getCardNo()));// 解密银行卡号
				newlist.add(vo);
			}
		}
		PaymentVoRes res = new PaymentVoRes();
		res.setPage(page);
		res.setRow(row);
		res.setRows(newlist);
		res.setTotal(total);
		return res;
	}

	/**
	 * 对账忽略
	 * 
	 * @param oids
	 */
	public void ignoreRecon(String[] oids) {
		log.info("忽略对账oids,{}", JSONObject.toJSONString(oids));
		try {
			if (oids.length > 0) {
				for (int i = 0; i < oids.length; i++) {
					paymentDao.updateStatus(oids[i]);
				}
			}
		} catch (Exception e) {
			log.error("根据oid忽略对账异常:" + e);
		}
	}

	/**
	 * 添加一条回掉信息
	 * 
	 * @param map
	 * @return
	 */
	public boolean noticUrl(Map<String, String> map) {
		log.info(JSONObject.toJSONString(map));
		try {
			if (null != map.get("tradeNo") && null != map.get("status") && null != map.get("merchantNo")) {
				// 查询是否存在该订单
				String resMessage = "";
				if (null != map.get("resMessage")) {
					resMessage = map.get("resMessage").toString();
				}
				String tradeNo = map.get("tradeNo").toString(), status = map.get("status").toString(),
						returnCode = map.get("resCode").toString(), merchantNo = map.get("merchantNo").toString();
				
				PaymentVo pay = paymentDao.findByPayNo(merchantNo);
				log.info("pay :{}", JSONObject.toJSONString(pay));
				if (pay != null) {
					
					String trastatus=pay.getCommandStatus(); 
					
					if (PayEnum.PAY1.getCode().equals(trastatus)) {
						log.info("定单已成功，payNo={}", merchantNo);
					}
					
					log.info("回调返回流水号{}，回调返回交易状态{}，回调返回信息{}，回调返回交易流水号{}，回调返回代码{}", tradeNo, status, resMessage,
							merchantNo, returnCode);
					
					CallBackInfo info = callbackDao.queryCallBackOne(pay.getPayNo(), "settlement");
					// 支付状态,先入为主
					if (null != info) {
						if (returnCode.equals(info.getReturnCode())) {
							log.info("orderNo :{},定单已同步处理，或者已回调过，通知返回true", pay.getOrderNo());
							return true;
						} 
					}else{
						info = CallBackInfo.builder().orderNO(pay.getOrderNo()).tradeType(pay.getType())
								.payNo(pay.getPayNo()).channelNo(pay.getChannelNo()).type("settlement").minute(1)
								.totalCount(2).totalMinCount(2).countMin(0).returnCode(returnCode)
								.status(CallBackEnum.INIT.getCode()).returnMsg(resMessage).createTime(new Date()).
								updateTime(new Date()).build();
					}
					
					AccountTransResponse accountTransResponse = new AccountTransResponse();
					accountTransResponse.setReturnCode(Constant.SUCCESS);
					if (Constant.SUCCESS.equals(returnCode)) {
						// 支付成功记账
						try {
							if (TradeType.pay.getValue().equals(pay.getType())) {
								log.info("支付成功，通知处理完成，开始记账...");
								accountTransResponse = accounting(pay, null);
							} else {
								log.info("提现订单：{},记账开始", pay.getOrderNo());
								AccountTransRequest accountTransRequest = new AccountTransRequest();
								accountTransRequest.setOrderNo(pay.getOrderNo());
								OrderVo order = comOrderDao.findByorderNo(pay.getOrderNo());
								accountTransRequest.setBalance(order.getAmount().add(order.getFee()));
								accountTransRequest.setSystemSource(order.getSystemSource());
								accountTransRequest.setUserOid(order.getUserOid());
								accountTransRequest.setOrderType(OrderTypeEnum.WITHDRAWALS.getCode());
								accountTransRequest.setUserType(order.getUserType());
								accountTransRequest.setFee(order.getFee());
								accountTransRequest
										.setOrderCreatTime(DateUtil.format(new Date(), DateUtil.datetimePattern));
								BaseResponse resp = accountWithdrawalsService.withdrawals("WITHDRAWALS",
										accountTransRequest);

								log.info("提现订单号{}，记账结果{}", pay.getOrderNo(), JSONObject.toJSON(resp));
								accountTransResponse.setReturnCode(resp.getReturnCode());
								accountTransResponse.setErrorMessage(resp.getErrorMessage());
							}

						} catch (Exception e) {
							log.error("记账异常,定单号：{},错误信息：{}", pay.getOrderNo(), e);
							accountTransResponse.setReturnCode(Constant.FAIL);
							accountTransResponse.setErrorMessage("支付成功，记账异常 ");
						}

					} else {
						if (TradeType.pay.getValue().equals(pay.getType())) {
							// 20170411失败的支付订单记账：只记录失败订单
							log.info("订单充值失败，账户记录失败订单{}", pay.getOrderNo());
							accountTransResponse = saveAccountFailOrder(pay);
						}else{
							log.info("代付失败，失败订单{}", pay.getOrderNo());
						}
						//支付失败，交易状态设置为失败
						trastatus=PayEnum.PAY2.getCode();
					}
					
					Boolean result = false;
					if (Constant.SUCCESS.equals(accountTransResponse.getReturnCode())) {
						log.info("通知回调成功，记账成功");
						// 推送处理
						OrderResponse orderResponse = new OrderResponse();
						orderResponse.setOrderNo(pay.getOrderNo());
						orderResponse.setErrorMessage(resMessage);
						orderResponse.setReturnCode(returnCode);
						orderResponse.setPayNo(pay.getPayNo());
						orderResponse.setType(pay.getType());
						orderResponse.setUserType(pay.getUserType());
						orderResponse.setChannelNo(pay.getChannelNo());
						String returnMsg = "";
						try {
							log.info("交易回调，{}", JSONObject.toJSONString(orderResponse));
							result = settlementSdk.callback(orderResponse);
						} catch (Exception e) {
							returnMsg = "推送交易信息异常";
							log.error(returnMsg + " OrderNO{},{}", pay.getOrderNo(), e);
						}
						log.info("回调结果，orderNo：{},result：{}", orderResponse.getOrderNo(), result);
						// 记录回调状态
						info.setStatus(result == true ? CallBackEnum.SUCCESS.getCode() : CallBackEnum.INIT.getCode());
						
						if(Constant.SUCCESS.equals(returnCode)){
							log.info("orderNO : {} 记账成功，并且支付成功，设置状态为成功",pay.getOrderNo());
							trastatus=PayEnum.PAY1.getCode();
						}else{
							log.info("orderNO : {} 记账成功，支付失败，设置状态为失败",pay.getOrderNo());
							trastatus=PayEnum.PAY2.getCode();
						}
						
					} else {
						log.info("记账失败，不进行回调，增加回调记录，accountTransResponse ={}",
								JSONObject.toJSON(accountTransResponse));
						// 添加一条callbackInfo数据 对于推送成功的，只做记录
						info.setStatus(CallBackEnum.FAIL.getCode());
						info.setReturnMsg("记账失败，不进行回调，支付信息："+resMessage);
					}
					//如果回调失败，记录信息，重新回调
					if(!result){
						log.info("回调业务系统失败，mes={}",info.getReturnMsg());
						callbackDao.saveAndFlush(info);
					}
					
					// 更改收单、指令、交互日志状态
					int payInt = paymentDao.updateByPayNo(trastatus, pay.getPayNo());

					// ---收单
					int orderInt = comOrderDao.updateByOrder(trastatus, returnCode, resMessage, pay.getOrderNo());

					// ---交互日志
					int logInt = bankLogDao.updateByPayNo(trastatus, pay.getPayNo());
					
					// 删除提现的redis
					if("02".equals(pay.getType())){
						log.info("提现成功删除申请时redis该订单{}缓存",pay.getOrderNo());
						payTwoRedisUtil.delRedis("wthdrawal_order_redis_tag" + pay.getOrderNo());
					}
					log.info("修改指令表结果：{},修改订单表结果：{},修改交互日志结果：{}", payInt, orderInt, logInt);
					if (payInt == 1 && orderInt == 1 && logInt == 1) {
						return true;
					}
					return false;
				}
				log.info("无效的交易流水号={}", merchantNo);
			}
		} catch (Exception e) {
			log.error("处理回调异常", e);
		}
		return false;

	}

	/**
	 * 账户保存失败订单
	 * @param pay
	 * @return
	 */
	public AccountTransResponse saveAccountFailOrder(PaymentVo pay) {
		AccountTransResponse resp = new AccountTransResponse();
		AccountTransRequest req = new AccountTransRequest();
		String userType = pay.getUserType();
		req.setUserOid(pay.getUserOid());
		req.setUserType(UserTypeEnum.INVESTOR.getCode());
		if(!StringUtils.isEmpty(userType)){
			//可能为发行人记账
			req.setUserType(userType);
		}
		req.setBalance(pay.getAmount());
		OrderVo order = comOrderDao.findByorderNo(pay.getOrderNo());
		if (null != order.getCreateTime()) {
			req.setOrderCreatTime(DateUtil.format(order.getCreateTime(), Constant.fomat));
		}
		if (pay.getType().trim().equals(PayEnum.PAYTYPE01.getCode())) {
			req.setOrderType(OrderTypeEnum.RECHARGE.getCode());
			req.setRemark("充值");
		} else if (pay.getType().trim().equals(PayEnum.PAYTYPE02.getCode())) {
			req.setOrderType(OrderTypeEnum.WITHDRAWALS.getCode());
			req.setRemark("提现");
			if (order.getFee().compareTo(BigDecimal.ZERO) > 0) {
				log.info("提现包含手续费，定单金额={},手续费={}", req.getBalance(), order.getFee());
				req.setBalance(req.getBalance().add(order.getFee()));
				req.setFee(order.getFee());
				log.info("增加手续费后金额：{}", req.getBalance());
			}
		} else {
			log.info("账户操作，交易类别不支持.type={}", pay.getType());
			resp.setErrorMessage("账户操作，交易类别不支持");
			resp.setReturnCode(Constant.ORDERTYPENOTEXISTS);
			return resp;
		}
		req.setOrderNo(pay.getOrderNo());
		req.setSystemSource("mimosa");
		CreateOrderRequest ordReq = new CreateOrderRequest();
		ordReq.setOrderNo(req.getOrderNo());
		ordReq.setRequestNo(req.getRequestNo());
		ordReq.setOrderNo(pay.getOrderNo());
		ordReq.setUserOid(pay.getUserOid());
		ordReq.setOrderType(req.getOrderType());
		ordReq.setProductType(req.getProductType());
		ordReq.setRelationProductNo(req.getRelationProductNo());
		ordReq.setOutputRelationProductNo(req.getOutputRelationProductNo());
		ordReq.setOutputRelationProductName(req.getOutputRelationProductName());
		ordReq.setBalance(req.getBalance());
		ordReq.setVoucher(req.getVoucher()); // 代金券
		ordReq.setSystemSource(req.getSystemSource());
		ordReq.setRemark(req.getRemark());
		ordReq.setOrderDesc(req.getOrderDesc());
		ordReq.setOrderCreatTime(req.getOrderCreatTime());
		ordReq.setFee(req.getFee());
		ordReq.setUserType(userType);
		ordReq.setPhone(order.getPhone());
		
		CreateOrderResponse createOrderResponse = accOrderService.saveAccountFailOrder(ordReq);
		if(null != createOrderResponse){
			resp.setReturnCode(createOrderResponse.getReturnCode());
			resp.setErrorMessage(createOrderResponse.getErrorMessage());
		}else{
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("保存失败订单失败");
		}
		return resp;
	}

	/**
	 * 账户记账
	 * @param pay
	 * @param remark 撤单标记
	 * @return
	 */
	public AccountTransResponse accounting(PaymentVo pay, String remark) {
		AccountTransResponse accountTransResponse = new AccountTransResponse();
		AccountTransRequest req = new AccountTransRequest();
		String userType = pay.getUserType();
		req.setUserOid(pay.getUserOid());
		req.setUserType(UserTypeEnum.INVESTOR.getCode());
		if(!StringUtils.isEmpty(userType)){
			//可能为发行人记账
			req.setUserType(userType);
		}
		req.setBalance(pay.getAmount());
		OrderVo order = comOrderDao.findByorderNo(pay.getOrderNo());
		if (null != order.getCreateTime()) {
			req.setOrderCreatTime(DateUtil.format(order.getCreateTime(), Constant.fomat));
		}
		if (pay.getType().trim().equals(PayEnum.PAYTYPE01.getCode())) {
			req.setOrderType(OrderTypeEnum.RECHARGE.getCode());
			req.setRemark("充值");
		} else if (pay.getType().trim().equals(PayEnum.PAYTYPE02.getCode())) {
			req.setOrderType(OrderTypeEnum.WITHDRAWALS.getCode());
			req.setRemark("提现");
			if (order.getFee().compareTo(BigDecimal.ZERO) > 0) {
				log.info("提现包含手续费，定单金额={},手续费={}", req.getBalance(), order.getFee());
				req.setBalance(req.getBalance().add(order.getFee()));
				req.setFee(order.getFee());
				log.info("增加手续费后金额：{}", req.getBalance());
			}
		} else {
			log.info("账户操作，交易类别不支持.type={}", pay.getType());
			accountTransResponse.setErrorMessage("账户操作，交易类别不支持");
			accountTransResponse.setReturnCode(Constant.ORDERTYPENOTEXISTS);
			return accountTransResponse;
		}
		req.setOrderNo(pay.getOrderNo());
		req.setSystemSource("mimosa");
		if(Constant.KILLORDER.equals(remark)){//判断是否是撤单
			req.setRemark(Constant.KILLORDER);
		}else if(Constant.CHANGESTATUS.equals(remark)){
			req.setRemark(Constant.CHANGESTATUS);
		}
		accountTransResponse = transService.trade(req);
		return accountTransResponse;
	}

	/**
	 * 补单账户记账
	 */
	public AccountTransResponse accountingReconciliation(PaymentVo pay, String remark) {
		AccountTransResponse accountTransResponse = new AccountTransResponse();
		AccountTransRequest req = new AccountTransRequest();
		String userType = pay.getUserType();
		req.setUserOid(pay.getUserOid());
		req.setUserType(UserTypeEnum.INVESTOR.getCode());
		if(!StringUtils.isEmpty(userType)){
			//可能为发行人记账
			req.setUserType(userType);
		}
		req.setBalance(pay.getAmount());
		OrderVo order = comOrderDao.findByorderNo(pay.getOrderNo());
		if (null != order.getCreateTime()) {
			req.setOrderCreatTime(DateUtil.format(order.getCreateTime(), Constant.fomat));
		}
		if (pay.getType().trim().equals(PayEnum.PAYTYPE01.getCode())) {
			req.setOrderType(OrderTypeEnum.RECHARGE.getCode());
			req.setRemark("充值");
		} else if (pay.getType().trim().equals(PayEnum.PAYTYPE02.getCode())) {
			req.setOrderType(OrderTypeEnum.WITHDRAWALS.getCode());
			req.setRemark("提现");
			if (order.getFee().compareTo(BigDecimal.ZERO) > 0) {
				log.info("提现包含手续费，定单金额={},手续费={}", req.getBalance(), order.getFee());
				req.setBalance(req.getBalance().add(order.getFee()));
				req.setFee(order.getFee());
				log.info("增加手续费后金额：{}", req.getBalance());
			}
		} else {
			log.info("账户操作，交易类别不支持.type={}", pay.getType());
			accountTransResponse.setErrorMessage("账户操作，交易类别不支持");
			accountTransResponse.setReturnCode(Constant.ORDERTYPENOTEXISTS);
			return accountTransResponse;
		}
		req.setOrderNo(pay.getOrderNo());
		req.setSystemSource("mimosa");
		if(Constant.KILLORDER.equals(remark)){//判断是否是撤单
			req.setRemark(Constant.KILLORDER);
		}else if(Constant.CHANGESTATUS.equals(remark)){
			req.setRemark(Constant.CHANGESTATUS);
		}
		accountTransResponse = transService.tradeReconciliation(req);
		return accountTransResponse;
	}

	/**
	 * 大额指令查询
	 * 
	 * @param req
	 * @return
	 */
	public PaymentVoRes pageLargeAmount(PaymentForm req) {
		String realName = "%" + req.getRealName() + "%";
		long total = paymentDao.findLargeAmountCount(realName, req.getType(), req.getOrderNo(), req.getPayNo(),req.getPhone());// 总条数
		List<PaymentVo> pageList = paymentDao.findLargeAmount(realName, req.getType(), req.getOrderNo(), req.getPayNo(),req.getPhone(),
				(req.getPage()-1)*req.getRows(), req.getRows());
		PaymentVoRes res = new PaymentVoRes();
		res.setPage(req.getPage());
		res.setRow(req.getRows());
		res.setRows(pageList);
		res.setTotal(total);
		return res;
	}

	/**
	 * 批量审核申请
	 */
	public int batchUpdate(String[] oids, String batchOperatorStatus, String batchOperatorReson) {
		OrderResponse orderRes = new OrderResponse();
		int successCount = 0;
		if (oids.length > 0) {
			for (int i = 0; i < oids.length; i++) {
				PaymentForm req = new PaymentForm();
				req.setOid(oids[i]);
				req.setOperatorStatus(batchOperatorStatus);
				req.setOperatorReson(batchOperatorReson);
				req.setAuditStatus("2");// 提交审核
				// req.setUserOid(userOid);
				orderRes = update(req);
				if (Constant.SUCCESS.equals(orderRes.getReturnCode())) {
					successCount++;
				}
			}
		}
		return successCount;
	}

	/**
	 * 批量审核
	 */
	public int batchAudit(String[] oids, String[] orderNos, String reson, String auditStatus, String[] operatorTypes,
			String[] operatorStatus, String[] updateStatus) {
		OrderResponse orderRes = new OrderResponse();
		int successCount = 0;
		if (oids.length > 0) {
			for (int i = 0; i < oids.length; i++) {
				
				PaymentForm req = new PaymentForm();
				req.setOid(oids[i]);
				req.setOrderNo(orderNos[i]);
				req.setReson(reson);
				req.setAuditStatus(auditStatus);// 审核状态，0审核不通过，1审核通过，2提交审核
				req.setOperatorType(operatorTypes[i]);
				req.setOperatorStatus(operatorStatus[i]);
				req.setUpdateStatus(updateStatus[i]);
				// req.setUserOid(userOid);
				orderRes = audit(req);
				if (Constant.SUCCESS.equals(orderRes.getReturnCode())) {
					successCount++;
				}
				try {
					Thread.sleep(200);
				} catch (Exception e) {
					log.error("批量审核"+e);
				}
			}
		}
		return successCount;
	}

	/**
	 * 修改订单状态
	 * 
	 * @param req
	 * @return
	 */
	public boolean changeStatus(PaymentForm req) {
		boolean changeStatus = false;
		try {
			InteractiveRequest interactiveRequest = new InteractiveRequest();
			interactiveRequest.setOrderNo(req.getOrderNo());
			changeStatus = settlementSdk.changeOrderStatus(interactiveRequest);// 调业务系统
		} catch (Exception e) {
			log.error("sdk修改订单系统异常:" + e);
		}
		log.info("结算修改订单状态返回：{}", JSONObject.toJSONString(changeStatus));
		if (changeStatus) {
			// 修改payment表交易状态状态
			paymentDao.updateStatusByPayNo(req.getPayNo());
			// 修改order表订单状态
			comOrderDao.updateStatusByOrder(req.getOrderNo());
		} else {
			return false;
		}
		return true;
	}

	/**
	 * 查询6个小时前到12个小时前的网关支付未处理订单
	 * 
	 * @return
	 */
	public List<PaymentVo> findNeedCloseOrderList() {
		List<PaymentVo> needCloseList = paymentDao.findNeedCloseOrderList();
		if (needCloseList.size() > 0) {
			return needCloseList;
		}
		return null;
	}

	/**
	 * String [] 去重
	 * 
	 * @param s
	 * @return
	 */
	public String[] deleteRepeat(String[] s) {
		List<String> list = Arrays.asList(s);
		Set<String> set = new HashSet<String>(list);

		String[] rid = (String[]) set.toArray(new String[0]);
		return rid;
	}
	
	/**
	 * 查询带审核订单总金额
	 */
	public String getMemBalance() {
		String balance = paymentDao.findMemBalance(TradeTypeEnum.trade_payee.getCode(), "2");
		if(!StringUtil.isEmpty(balance)){
			return balance;
		}
		return "0.00";
	}
}