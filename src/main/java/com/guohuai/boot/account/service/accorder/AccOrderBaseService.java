package com.guohuai.boot.account.service.accorder;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountBatchTransferRequest;
import com.guohuai.account.api.request.AccountTransRequest;
import com.guohuai.account.api.request.CreateOrderRequest;
import com.guohuai.account.api.request.entity.AccountOrderDto;
import com.guohuai.account.api.response.CreateOrderResponse;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.service.AccountTransferService;
import com.guohuai.boot.account.service.UserInfoService;
import com.guohuai.boot.pay.res.CreateBatchAccountOrderRes;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.OrderTypeEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 账户订单基本服务
 * @author ZJ
 * @date 2018年1月19日 下午5:29:39
 * @version V1.0
 */
@Slf4j
@Component
public class AccOrderBaseService {
	@Autowired
	private AccOrderDao orderDao;
	@Autowired
	private UserInfoService userInfoService;
	@Autowired
	private UserInfoDao userInfoDao;
	@Autowired
	private AccountTransferService accountTransferService;
	@Autowired
	private AccOrderQueryService accOrderQueryService;

	public CreateOrderResponse acceptOrder(AccountTransRequest req) {
		log.info("接收定单 req=[" + JSONObject.toJSONString(req) + "]");
		String userOid = req.getUserOid();
		String relationProductNo = req.getRelationProductNo();
		String orderType = req.getOrderType();
		CreateOrderResponse orderResp = new CreateOrderResponse();
		String orderNo = req.getOrderNo();

		try {
			/*
			 * 入账单据表
			 */
			CreateOrderRequest ordReq = new CreateOrderRequest();
			ordReq.setOrderNo(req.getOrderNo());
			ordReq.setRequestNo(req.getRequestNo());
			ordReq.setOrderNo(orderNo);
			ordReq.setPublisherUserOid(req.getPublisherUserOid());
			ordReq.setUserOid(userOid);
			ordReq.setOrderType(orderType);
			ordReq.setProductType(req.getProductType());
			ordReq.setRelationProductNo(relationProductNo);
			ordReq.setOutputRelationProductNo(req.getOutputRelationProductNo());
			ordReq.setOutputRelationProductName(req.getOutputRelationProductName());
			ordReq.setBalance(req.getBalance());
			ordReq.setVoucher(req.getVoucher()); // 代金券
			ordReq.setSystemSource(req.getSystemSource());
			ordReq.setRemark(req.getRemark());
			ordReq.setOrderDesc(req.getOrderDesc());
			ordReq.setOrderCreatTime(req.getOrderCreatTime());
			ordReq.setFee(req.getFee());
			ordReq.setUserType(req.getUserType());
			log.info("生成交易定单");
			orderResp = addAccOrder(ordReq);
		} catch (Exception e) {
			log.error("系统繁忙,保存定单失败", e);
			orderResp.setErrorMessage("系统繁忙,保存定单失败");
			orderResp.setReturnCode(Constant.FAIL);
			return orderResp;
		}
		return orderResp;
	}

	/**
	 * 新增订单
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public CreateOrderResponse addAccOrder(CreateOrderRequest req) {
		CreateOrderResponse resp = new CreateOrderResponse();
		resp.setReturnCode(Constant.SUCCESS);
		try {
			AccOrderEntity orderEntity = null;
			String orderType = req.getOrderType();
			if (!orderType.equals(OrderTypeEnum.PUBLISH.getCode())) {
				orderEntity = this.accOrderQueryService.getOrderByNo(req.getOrderNo());
				if (orderEntity != null) {
					// 撤销订单,返回原订单
					if (Constant.KILLORDER.equals(req.getRemark())) {
						if (AccOrderEntity.ORDERSTATUS_KILL.equals(orderEntity.getOrderStatus())) {
							// 已完成撤单不能再次撤单
							resp.setReturnCode(Constant.ORDERSTATUSERROR);
							resp.setErrorMessage("已完成撤单，不能再次撤单");
							log.debug("已完成撤单，不能再次撤单orderNo:[" + orderEntity.getOrderNo() + "]");
							return resp;
						} else {
							resp.setReturnCode(Constant.SECONDTIME);
							resp.setErrorMessage("成功");
							resp.setOrderOid(orderEntity.getOid());
							resp.setOrderNo(orderEntity.getOrderNo());
							return resp;
						}
						// 修改订单状态
					} else if (Constant.CHANGESTATUS.equals(req.getRemark())) {
						resp.setReturnCode(Constant.SECONDTIME);
						resp.setErrorMessage("成功");
						resp.setOrderOid(orderEntity.getOid());
						resp.setOrderNo(orderEntity.getOrderNo());
						return resp;
					}
					// 20170329新增逻辑，提现会二次调用，第二次调用返回原订单，标记为第二次记账
					// 赎回补单，订单状态为0返回成功,remark改为补单
					else if (AccOrderEntity.ORDERSTATUS_INIT.equals(orderEntity.getOrderStatus())) {
						if (OrderTypeEnum.WITHDRAWALS.getCode().equals(orderEntity.getOrderType())
								|| OrderTypeEnum.REDEEM.getCode().equals(orderEntity.getOrderType())) {
							resp.setReturnCode(Constant.SECONDTIME);
							resp.setErrorMessage("成功");
							resp.setOrderOid(orderEntity.getOid());
							resp.setOrderNo(orderEntity.getOrderNo());
							orderEntity.setRemark(req.getRemark());// 补单
							orderDao.save(orderEntity);
							return resp;
						}
						// 20170401新增逻辑，提现记账失败可重新记账
					} else if (AccOrderEntity.ORDERSTATUS_FAIL.equals(orderEntity.getOrderStatus())
							&& OrderTypeEnum.WITHDRAWALS.getCode().equals(orderEntity.getOrderType())) {
						resp.setReturnCode(Constant.SECONDTIME);
						resp.setErrorMessage("成功");
						resp.setOrderOid(orderEntity.getOid());
						resp.setOrderNo(orderEntity.getOrderNo());
						return resp;
						// 赎回补单，不是初始化状态，返回订单已存在
					} else if (!AccOrderEntity.ORDERSTATUS_INIT.equals(orderEntity.getOrderStatus())
							&& OrderTypeEnum.REDEEM.getCode().equals(orderEntity.getOrderType())) {

						if (AccOrderEntity.ORDERSTATUS_FAIL.equals(orderEntity.getOrderStatus())) {
							resp.setErrorMessage("赎回失败重发，返回成功");
							resp.setOrderOid(orderEntity.getOid());
							resp.setOrderNo(orderEntity.getOrderNo());
							resp.setRequestNo(Constant.SUCCESS);
							return resp;
						} else if (AccOrderEntity.ORDERSTATUS_SUCCESS.equals(orderEntity.getOrderStatus())) {
							resp.setReturnCode(Constant.REDEEM_SUCCESSED);
							resp.setErrorMessage("赎回重发，已赎回成功");
							resp.setOrderOid(orderEntity.getOid());
							resp.setOrderNo(orderEntity.getOrderNo());
							log.info(resp.getErrorMessage() + " orderNo=" + orderEntity.getOrderNo() + "]");
							return resp;
						}

					} else if (AccOrderEntity.ORDERSTATUS_SUCCESS.equals(orderEntity.getOrderStatus())) {
						resp.setReturnCode(Constant.ORDEREXISTS);
						resp.setErrorMessage("订单号已经存在");
						log.debug("订单号已存在orderNo:[" + orderEntity.getOrderNo() + "]");
						return resp;
					}
				}
			}
			log.info("订单交易:[" + JSONObject.toJSONString(req) + "]");
			Timestamp time = new Timestamp(System.currentTimeMillis());
			if (null == orderEntity) {
				orderEntity = new AccOrderEntity();
			}
			String orderNo = req.getOrderNo();
			orderEntity.setRequestNo(req.getRequestNo());
			orderEntity.setSystemSource(req.getSystemSource());
			orderEntity.setOrderNo(orderNo);
			orderEntity.setUserOid(req.getUserOid());
			orderEntity.setOrderType(req.getOrderType());
			orderEntity.setProductType(req.getProductType());
			orderEntity.setRelationProductNo(req.getRelationProductNo());
			orderEntity.setOutputRelationProductNo(req.getOutputRelationProductNo());
			orderEntity.setOutputRelationProductName(req.getOutputRelationProductName());
			orderEntity.setBalance(req.getBalance());
			orderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_INIT);
			// 入账账户
			orderEntity.setInputAccountNo(req.getInputAccountNo());
			// 出账账户
			orderEntity.setOutpuptAccountNo(req.getOutpuptAccountNo());
			orderEntity.setRemark(req.getRemark());
			orderEntity.setOrderDesc(req.getOrderDesc());
			orderEntity.setUserType(req.getUserType());
			// 20170301新增业务系统订单创建时间
			String orderCreatTime = req.getOrderCreatTime();// YYYY-MM-DD HH:mm:ss
			if (orderCreatTime != null) {
				orderEntity.setSubmitTime(Timestamp.valueOf(orderCreatTime));
			}
			orderEntity.setVoucher(req.getVoucher());
			orderEntity.setPublisherUserOid(req.getPublisherUserOid());
			orderEntity.setFee(req.getFee());
			orderEntity.setFrozenBalance(req.getFrozenBalance());
			orderEntity.setReceiveTime(time);
			orderEntity.setBusinessStatus(req.getBusinessStatus());
			orderEntity.setFinanceStatus(req.getFinanceStatus());
			orderEntity.setUpdateTime(time);
			if (null == orderEntity.getCreateTime()) {
				orderEntity.setCreateTime(time);
			}
			UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(orderEntity.getUserOid());
			if (null != userInfoEntity) {
				orderEntity.setPhone(userInfoEntity.getPhone());
			}
			log.info("保存定单");
			orderEntity = orderDao.save(orderEntity);
			log.info("保存定单结束");
			if (orderEntity != null) {
				resp.setReturnCode(Constant.SUCCESS);
				resp.setErrorMessage("成功");
				resp.setOrderOid(orderEntity.getOid());
				resp.setOrderNo(orderNo);
			}

		} catch (Exception e) {
			log.error("订单插入失败", e);
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("订单保存失败");
			return resp;
		}
		return resp;
	}

	/**
	 * 保存失败订单
	 */
	public CreateOrderResponse saveAccountFailOrder(CreateOrderRequest req) {
		CreateOrderResponse resp = new CreateOrderResponse();
		resp.setReturnCode(Constant.SUCCESS);
		try {
			AccOrderEntity orderEntity = null;
			String orderType = req.getOrderType();
			if (!orderType.equals(OrderTypeEnum.PUBLISH.getCode())) {
				orderEntity = this.accOrderQueryService.getOrderByNo(req.getOrderNo());
				if (orderEntity != null) {
					if (AccOrderEntity.ORDERSTATUS_SUCCESS.equals(orderEntity.getOrderStatus())) {
						resp.setReturnCode(Constant.ORDEREXISTS);
						resp.setErrorMessage("订单号已经存在");
						log.debug("订单号已存在orderNo:[" + orderEntity.getOrderNo() + "]");
						return resp;
					}
				}
				log.info("订单交易:[" + JSONObject.toJSONString(req) + "]");
				Timestamp time = new Timestamp(System.currentTimeMillis());
				if (null == orderEntity) {
					orderEntity = new AccOrderEntity();
				}
				String orderNo = req.getOrderNo();
				orderEntity.setRequestNo(req.getRequestNo());
				orderEntity.setSystemSource(req.getSystemSource());
				orderEntity.setOrderNo(orderNo);
				orderEntity.setUserOid(req.getUserOid());
				orderEntity.setOrderType(req.getOrderType());
				orderEntity.setProductType(req.getProductType());
				orderEntity.setRelationProductNo(req.getRelationProductNo());
				orderEntity.setOutputRelationProductNo(req.getOutputRelationProductNo());
				orderEntity.setOutputRelationProductName(req.getOutputRelationProductName());
				orderEntity.setBalance(req.getBalance());
				orderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);// 失败
				// 入账账户
				orderEntity.setInputAccountNo(req.getInputAccountNo());
				// 出账账户
				orderEntity.setOutpuptAccountNo(req.getOutpuptAccountNo());
				orderEntity.setRemark(req.getRemark());
				orderEntity.setOrderDesc(req.getOrderDesc());
				orderEntity.setUserType(req.getUserType());
				// 20170301新增业务系统订单创建时间
				String orderCreatTime = req.getOrderCreatTime();// YYYY-MM-DD HH:mm:ss
				if (orderCreatTime != null) {
					orderEntity.setSubmitTime(Timestamp.valueOf(orderCreatTime));
				}
				orderEntity.setVoucher(req.getVoucher());
				orderEntity.setFee(req.getFee());
				orderEntity.setReceiveTime(time);
				orderEntity.setBusinessStatus(req.getBusinessStatus());
				orderEntity.setFinanceStatus(req.getFinanceStatus());
				orderEntity.setUpdateTime(time);
				if (null == orderEntity.getCreateTime()) {
					orderEntity.setCreateTime(time);
				}
				orderEntity.setPhone(req.getPhone());
				log.info("保存定单");
				orderEntity = orderDao.save(orderEntity);
				log.info("保存定单结束");
				if (orderEntity != null) {
					resp.setReturnCode(Constant.SUCCESS);
					resp.setErrorMessage("成功");
					resp.setOrderOid(orderEntity.getOid());
					resp.setOrderNo(orderNo);
				}
			}
		} catch (Exception e) {
			log.error("订单插入失败", e);
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("订单保存失败");
			return resp;
		}
		return resp;
	}

	@Transactional
	public void updateOrderStatus(String orderNo, String orderStatus) {
		orderDao.updateOrderStatus(orderNo, orderStatus);
	}

	/**
	 * 新增订单
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public CreateOrderResponse addAccountOrder(CreateOrderRequest req) {
		CreateOrderResponse resp = new CreateOrderResponse();
		Timestamp now = new Timestamp(System.currentTimeMillis());
		resp.setReturnCode(Constant.SUCCESS);
		String orderNo = req.getOrderNo();
		AccOrderEntity orderEntity = null;
		orderEntity = this.accOrderQueryService.getOrderByNo(orderNo);
		if (orderEntity != null) {
			if (AccOrderEntity.ORDERSTATUS_SUCCESS.equals(orderEntity.getOrderStatus())) {
				resp.setReturnCode(Constant.ORDEREXISTS);
				resp.setErrorMessage("订单已存在且已成功");
				log.error("订单已存在，订单号:{}", orderEntity);
				return resp;
			} else {
				log.info("订单已存在{}，更新订单时间{}", orderEntity, req.getOrderCreatTime());
				String orderCreatTime = req.getOrderCreatTime();// YYYY-MM-DD HH:mm:ss
				if (orderCreatTime != null) {
					orderEntity.setSubmitTime(Timestamp.valueOf(orderCreatTime));
				}
				orderEntity.setUpdateTime(now);
				resp.setReturnCode(Constant.SUCCESS);
				resp.setErrorMessage("成功");
				resp.setOrderOid(orderEntity.getOid());
				resp.setOrderNo(orderEntity.getOrderNo());
				return resp;
			}
		}
		log.info("接收新订单交易:[" + JSONObject.toJSONString(req) + "]");
		if (null == orderEntity) {
			orderEntity = new AccOrderEntity();
		}
		orderEntity.setRequestNo(req.getRequestNo());
		orderEntity.setSystemSource(req.getSystemSource());
		orderEntity.setOrderNo(orderNo);
		orderEntity.setUserOid(req.getUserOid());
		orderEntity.setOrderType(req.getOrderType());
		orderEntity.setProductType(req.getProductType());
		orderEntity.setRelationProductNo(req.getRelationProductNo());
		orderEntity.setOutputRelationProductNo(req.getOutputRelationProductNo());
		orderEntity.setOutputRelationProductName(req.getOutputRelationProductName());
		orderEntity.setBalance(req.getBalance());
		orderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_INIT);
		// 入账账户
		orderEntity.setInputAccountNo(req.getInputAccountNo());
		// 出账账户
		orderEntity.setOutpuptAccountNo(req.getOutpuptAccountNo());
		orderEntity.setRemark(req.getRemark());
		orderEntity.setOrderDesc(req.getOrderDesc());
		orderEntity.setUserType(req.getUserType());
		String orderCreatTime = req.getOrderCreatTime();// YYYY-MM-DD HH:mm:ss
		if (orderCreatTime != null) {
			orderEntity.setSubmitTime(Timestamp.valueOf(orderCreatTime));
		}
		orderEntity.setVoucher(req.getVoucher());
		orderEntity.setPublisherUserOid(req.getPublisherUserOid());
		orderEntity.setFee(req.getFee());
		orderEntity.setReceiveTime(now);
		orderEntity.setBusinessStatus(req.getBusinessStatus());
		orderEntity.setFinanceStatus(req.getFinanceStatus());
		orderEntity.setUpdateTime(now);
		orderEntity.setCreateTime(now);
		UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(orderEntity.getUserOid());
		if (null != userInfoEntity) {
			orderEntity.setPhone(userInfoEntity.getPhone());
		}
		log.info("保存定单");
		orderEntity = orderDao.save(orderEntity);
		log.info("保存定单结束");
		if (orderEntity != null) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
			resp.setOrderOid(orderEntity.getOid());
			resp.setOrderNo(orderNo);
		}
		return resp;
	}

	/**
	 * 组装批量账户订单
	 * @param req
	 *            批量订单请求参数
	 * @param resp
	 *            已存在返参
	 * @return 批量账户订单
	 */
	@Transactional
	public CreateBatchAccountOrderRes creatBatchAccountOrder(AccountBatchTransferRequest req) {
		CreateBatchAccountOrderRes createBatchAccountOrderRes = new CreateBatchAccountOrderRes();
		String outputUserOid = req.getOutputUserOid();
		String requestNo = req.getRequestNo();
		// 请求批次号流水号
		if (requestNo != null) {
			int requestCount = 0;
			requestCount = orderDao.finRequestCountByRequestNo(requestNo);
			if (requestCount > 0) {
				createBatchAccountOrderRes.setReturnCode(Constant.REQUESTNOEXISTS);
				createBatchAccountOrderRes.setErrorMessage("批量转账订单，批次请求流水号已存在");
				log.info("批量转账订单，批次请求流水号已存在!");
				return createBatchAccountOrderRes;
			}
		} else {
			createBatchAccountOrderRes.setReturnCode(Constant.REQUESTNO_IS_NULL);
			createBatchAccountOrderRes.setErrorMessage("发批量转账订单，批次请求流水号不能为空");
			log.info("批量转账订单，批次请求流水号不能为空!!");
			return createBatchAccountOrderRes;
		}
		// 保存订单
		if (req.getOrderList() != null && req.getOrderList().size() > 0) {
			// 批量保存订单
			createBatchAccountOrderRes = this.saveBatchOrder(req.getOrderList(), requestNo, outputUserOid,
					req.getSystemSource());
		} else {
			createBatchAccountOrderRes.setReturnCode(Constant.BATCH_REDEEM_ORDER_IS_NULL);
			createBatchAccountOrderRes.setErrorMessage("批量转账订单不存在");
			log.info("批量转账订单不存在!!");
			return createBatchAccountOrderRes;
		}
		return createBatchAccountOrderRes;
	}

	/**
	 * 保存批量转账订单
	 * @param orderList
	 *            批量订单
	 * @param requestNo
	 *            请求批次号
	 * @param outputUserOid
	 *            转出人id
	 * @param systemSource
	 *            系统来源
	 * @return
	 */
	@Transactional
	private CreateBatchAccountOrderRes saveBatchOrder(List<AccountOrderDto> orderList, String requestNo,
			String outputUserOid, String systemSource) {
		CreateBatchAccountOrderRes resp = new CreateBatchAccountOrderRes();
		List<AccOrderEntity> accOrderEntityList = new ArrayList<AccOrderEntity>();
		Timestamp time = new Timestamp(System.currentTimeMillis());
		BigDecimal orderBalance = BigDecimal.ZERO;// 订单总金额
		for (AccountOrderDto dto : orderList) {
			AccOrderEntity orderEntity = null;
			orderEntity = this.accOrderQueryService.getOrderByNo(dto.getOrderNo());
			if (orderEntity != null) {
				if (AccOrderEntity.ORDERSTATUS_SUCCESS.equals(orderEntity.getOrderStatus())) {
					resp.setReturnCode(Constant.ORDEREXISTS);
					resp.setErrorMessage("订单已存在且已成功");
					log.error("订单已存在，订单号:{}", orderEntity);
					// 回调业务订单已成功
					accountTransferService.callBack(orderEntity);
					continue;
				}
			}
			String phone = "";
			if (dto.getUserOid() != null) {
				// 查询用户信息
				UserInfoEntity userInfo = userInfoService.getAccountUserByUserOid(dto.getUserOid());
				if (userInfo != null) {
					phone = userInfo.getPhone();
				}
			}
			if (orderEntity == null) {
				orderEntity = new AccOrderEntity();
			}
			orderEntity.setRequestNo(requestNo);
			orderEntity.setPublisherUserOid(outputUserOid);
			orderEntity.setSystemSource(systemSource);
			orderEntity.setOrderNo(dto.getOrderNo());
			orderEntity.setUserOid(dto.getUserOid());
			orderEntity.setOrderType(dto.getOrderType());
			orderEntity.setBalance(dto.getBalance());
			orderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_INIT);
			orderEntity.setRemark(dto.getRemark());
			orderEntity.setOrderDesc(dto.getOrderDesc());
			if (StringUtil.isEmpty(dto.getUserType())) {
				orderEntity.setUserType("T1");
			} else {
				orderEntity.setUserType(dto.getUserType());
			}
			String orderCreatTime = dto.getSubmitTime();// YYYY-MM-DD HH:mm:ss
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
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");
		orderDao.save(accOrderEntityList);
		resp.setAccOrderEntityList(accOrderEntityList);
		return resp;
	}

	/**
	 * 新增订单
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AccOrderEntity saveAccOrder(CreateOrderRequest req) {
		AccOrderEntity orderEntity = null;
		Timestamp time = new Timestamp(System.currentTimeMillis());
		if (null == orderEntity) {
			orderEntity = new AccOrderEntity();
		}
		String orderNo = req.getOrderNo();
		orderEntity.setRequestNo(req.getRequestNo());
		orderEntity.setSystemSource(req.getSystemSource());
		orderEntity.setOrderNo(orderNo);
		orderEntity.setUserOid(req.getUserOid());
		orderEntity.setOrderType(req.getOrderType());
		orderEntity.setProductType(req.getProductType());
		orderEntity.setRelationProductNo(req.getRelationProductNo());
		orderEntity.setOutputRelationProductNo(req.getOutputRelationProductNo());
		orderEntity.setOutputRelationProductName(req.getOutputRelationProductName());
		orderEntity.setBalance(req.getBalance());
		orderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_INIT);
		// 入账账户
		orderEntity.setInputAccountNo(req.getInputAccountNo());
		// 出账账户
		orderEntity.setOutpuptAccountNo(req.getOutpuptAccountNo());
		orderEntity.setRemark(req.getRemark());
		orderEntity.setOrderDesc(req.getOrderDesc());
		orderEntity.setUserType(req.getUserType());
		// 20170301新增业务系统订单创建时间
		String orderCreatTime = req.getOrderCreatTime();// YYYY-MM-DD
														// HH:mm:ss
		if (orderCreatTime != null) {
			orderEntity.setSubmitTime(Timestamp.valueOf(orderCreatTime));
		}
		orderEntity.setVoucher(req.getVoucher());
		orderEntity.setPublisherUserOid(req.getPublisherUserOid());
		orderEntity.setFee(req.getFee());
		orderEntity.setFrozenBalance(req.getFrozenBalance());
		orderEntity.setReceiveTime(time);
		orderEntity.setBusinessStatus(req.getBusinessStatus());
		orderEntity.setFinanceStatus(req.getFinanceStatus());
		orderEntity.setUpdateTime(time);
		if (null == orderEntity.getCreateTime()) {
			orderEntity.setCreateTime(time);
		}
		UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(orderEntity.getUserOid());
		if (null != userInfoEntity) {
			orderEntity.setPhone(userInfoEntity.getPhone());
		}
		orderEntity = orderDao.save(orderEntity);

		return orderEntity;
	}
}