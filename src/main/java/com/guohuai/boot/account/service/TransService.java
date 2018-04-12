package com.guohuai.boot.account.service;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.*;
import com.guohuai.account.api.response.*;
import com.guohuai.account.api.response.entity.TransDto;
import com.guohuai.account.component.util.EventTypeEnum;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.basic.component.exception.GHException;
import com.guohuai.boot.account.dao.*;
import com.guohuai.boot.account.entity.*;
import com.guohuai.component.util.*;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.io.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class TransService {
	@Autowired
	private TransDao transDao;
	@Autowired
	private UserInfoService userInfoService;
	@Autowired
	private AccOrderService orderService;
	@Autowired
	private AccOrderDao orderDao;
	@Autowired
	private AccFailOrderNotifyDao accFailOrderNotifyDao;
	@Autowired
	private AccountInfoService accountInfoService;
	@Autowired
	private AccountTradeService accountTradeService;
	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private AccountDividendOrderDao dividendOrderDao;

	@Value("${needRechargeFrozenAccount:N}")
	private String needRechargeFrozenAccount;

	@Autowired
	private UserInfoDao userInfoDao;

	/**
	 * 发行人收款、放款交易 增加失败修改订单状态
	 * @param req
	 * @return
	 */
	public AccountTransResponse publisherTransAndSaveOrder(AccountTransRequest req) {
		AccountTransResponse resp = this.publisherTrans(req);
		if (!Constant.SUCCESS.equals(resp.getReturnCode())) {
			AccOrderEntity orderEntity = orderService.getOrderByNo(req.getOrderNo());
			if (orderEntity != null) {
				orderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);
				orderDao.saveAndFlush(orderEntity);
			}
		}
		return resp;
	}

	/**
	 * 发行人收款、放款交易 收款, orderType传58 放款,orderType传57 
	 * @Title: publisherLoan 
	 * @param req 
	 * @return AccountTransResponse 
	 * @throws
	 */
	@Transactional
	public AccountTransResponse publisherTrans(AccountTransRequest req) {
		AccountTransResponse resp = checkAccountTransRequest(req);
		if (!resp.getReturnCode().equals(Constant.SUCCESS)) {
			return resp;
		}

		CreateOrderResponse orderResp = null;
		CreateTransResponse transResp = null;

		/**
		 * 插入订单记录
		 */
		CreateOrderRequest ordReq = new CreateOrderRequest();
		this.convertTransRequest(req, ordReq);
		orderResp = orderService.addAccOrder(ordReq);
		AccOrderEntity orderEntity = orderService.getOrderOid(orderResp.getOrderOid());
		if (orderEntity == null) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("系统异常!");
			log.info("订单未保存成功，事务异常，请联系开发人员！");
			return resp;
		}

		if (Constant.SUCCESS.equals(orderResp.getReturnCode())) {
			/**
			 * 修改账户余额
			 */
			BigDecimal orderMoney = req.getBalance();

			// 查询发行人基本户
			AccountInfoEntity basicAccount = accountInfoDao.findByUserOidAndAccountTyp(req.getUserOid(),
					AccountTypeEnum.BASICER.getCode());
			// 获取指定类型的产品户
			AccountInfoEntity accountInfo = getAccountInfoByAccountType(req);
			// 查询发行人提现冻结户
			AccountInfoEntity frozenAccount = accountInfoDao.findByUserOidAndAccountTyp(req.getUserOid(),
					AccountTypeEnum.FROZEN.getCode());
			if (basicAccount == null || accountInfo == null) {
				resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
				resp.setErrorMessage("发行人账户信息异常!");
				log.info("发行人账户不存在!userOid = " + req.getUserOid());
				return resp;
			}

			BigDecimal accountInfoBalance = accountInfo.getBalance();
			BigDecimal basicAccountBalance = basicAccount.getBalance();
			BigDecimal frozenAccountBalance = BigDecimal.ZERO;
			if (frozenAccount != null) {
				frozenAccountBalance = frozenAccount.getBalance();
			}

			String orderType = req.getOrderType();

			// 账户提示信息
			boolean isProductAccount = isProductAccount(req);
			String msg = isProductAccount ? "产品户" : "可用金户";

			/**
			 * 收款
			 */
			if (orderType.equals(OrderTypeEnum.PUBLISHERRECE.getCode())) {
				log.info("发行人收款{}",JSONObject.toJSONString(req));
				if (orderMoney.compareTo(accountInfoBalance) > 0) {
					resp.setReturnCode(Constant.BALANCELESS);
					resp.setErrorMessage("发行人" + msg + "余额不足!");
					log.info("发行人" + msg + "余额不足!userOid = " + req.getUserOid());
					return resp;
				}
				log.info("扣除发行人" + msg + "余额:{}", orderMoney);
				// 扣除发行人可用金户或产品户余额
				int updateAccountInfoResult = accountInfoDao.subtractBalance(orderMoney, accountInfo.getAccountNo());
				// 增加发行人基本户余额
				log.info("增加发行人基本户余额:{}", orderMoney);
				if (updateAccountInfoResult == 0) {
					log.error("扣除发行人" + msg + "余额，发行人" + msg + "余额不足！");
					throw new GHException(Integer.parseInt(Constant.BALANCELESS),
							"扣除发行人" + msg + "余额失败，发行人" + msg + "余额不足！");
				}
				int updateBasicAccountResult = accountInfoDao.addBalance(orderMoney, basicAccount.getAccountNo());
				if (updateBasicAccountResult == 0) {
					log.error("增加发行人基本户余额失败！");
					throw new GHException(Integer.parseInt(Constant.BALANCELESS), "增加发行人基本户余额失败！");
				}
				log.info("发行人收款账户更新结果：updateAccountInfoResult={},updateBasicAccountResult={}", updateAccountInfoResult,
						updateBasicAccountResult);

				BigDecimal accountInfoAfterBalance = BigDecimal.ZERO;
				BigDecimal basicAccountAfterBalance = BigDecimal.ZERO;

				accountInfoAfterBalance = accountInfoBalance.subtract(orderMoney);
				basicAccountAfterBalance = basicAccountBalance.add(orderMoney);

				/**
				 * 插入交易流水
				 */
				req.setOrderDesc("发行人收款基本户明细");
				transResp = accountTradeService.addTrans(req, orderResp.getOrderOid(), basicAccount.getAccountNo(),
						accountInfo.getAccountNo(), req.getUserOid(), UserTypeEnum.PUBLISHER.getCode(),
						basicAccountAfterBalance, "01", AccountTypeEnum.BASICER.getCode(), basicAccount.getAccountNo());

				req.setOrderDesc("发行人收款" + msg + "明细");
				if (isProductAccount) {
					transResp = accountTradeService.addTrans(req, orderResp.getOrderOid(), basicAccount.getAccountNo(),
							accountInfo.getAccountNo(), req.getUserOid(), UserTypeEnum.PUBLISHER.getCode(),
							accountInfoAfterBalance, "02", AccountTypeEnum.PRODUCT.getCode(),
							accountInfo.getAccountNo());
				} else {
					transResp = accountTradeService.addTrans(req, orderResp.getOrderOid(), basicAccount.getAccountNo(),
							accountInfo.getAccountNo(), req.getUserOid(), UserTypeEnum.PUBLISHER.getCode(),
							accountInfoAfterBalance, "02", AccountTypeEnum.AVAILABLE_AMOUNT.getCode(),
							accountInfo.getAccountNo());
				}

				orderEntity.setInputAccountNo(basicAccount.getAccountNo());
				orderEntity.setOutpuptAccountNo(accountInfo.getAccountNo());
				orderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_SUCCESS);
				orderDao.saveAndFlush(orderEntity);
				/**
				 * 放款
				 */
			} else if (orderType.equals(OrderTypeEnum.PUBLISHERLOAN.getCode())) {
				log.info("发行人放款{}",JSONObject.toJSONString(req));

				AccountInfoEntity rechargeFrozenAccount = null;
				// 断定是否需要充值冻结户 如果不需要，查询时充值冰结户设置为0
				if ("Y".equals(needRechargeFrozenAccount)) {
					// 查询发行人充值冻结户
					rechargeFrozenAccount = accountInfoDao.findByUserOidAndAccountTyp(req.getUserOid(),
							AccountTypeEnum.RECHARGEFROZEN.getCode());
				}

				if (orderMoney.compareTo(basicAccountBalance.subtract(frozenAccountBalance)) > 0) {
					resp.setReturnCode(Constant.BALANCELESS);
					resp.setErrorMessage("发行人基本户余额不足!");
					log.info("发行人基本户余额不足!userOid = " + req.getUserOid());
					return resp;
				}

				// 扣除发行人基本户余额
				log.info("扣除发行人基本户余额:{}", orderMoney);
				int updateBasicAccountResult = accountInfoDao.subtractBalance(orderMoney, basicAccount.getAccountNo());
				if (updateBasicAccountResult == 0) {
					log.error("扣除发行人基本户余额，发行人基本户余额不足！");
					throw new GHException(Integer.parseInt(Constant.BALANCELESS), "扣除发行人基本户余额失败，发行人基本户余额不足！");
				}
				// 判断发行人充值冻结户是否有余额，当有余额时，发行人放款时同时扣除发行人充值冻结户余额
				int updateRechargeFrozenAccountResult = 0;
				if (rechargeFrozenAccount != null
						&& rechargeFrozenAccount.getBalance().compareTo(BigDecimal.ZERO) >= 0) {
					log.info("冻结账户余额：{}", rechargeFrozenAccount.getBalance());
					BigDecimal rechargeFrozenAccountAfterBalance = BigDecimal.ZERO;
					if (rechargeFrozenAccount.getBalance().compareTo(orderMoney) >= 0) {
						log.info("扣除发行人充值冻结户余额:{}", orderMoney);
						updateRechargeFrozenAccountResult = accountInfoDao.subtractBalance(orderMoney,
								rechargeFrozenAccount.getAccountNo());
						rechargeFrozenAccountAfterBalance = rechargeFrozenAccount.getBalance().subtract(orderMoney);
					} else if (rechargeFrozenAccount.getBalance().compareTo(orderMoney) < 0) {
						log.info("扣除发行人充值冻结户余额:{}", rechargeFrozenAccount.getBalance());
						updateRechargeFrozenAccountResult = accountInfoDao.subtractBalance(
								rechargeFrozenAccount.getBalance(), rechargeFrozenAccount.getAccountNo());
					}
					if (updateRechargeFrozenAccountResult == 0) {
						log.error("扣除发行人充值冻结户余额失败，请稍后再试！");
						throw new GHException(Integer.parseInt(Constant.BALANCELESS), "扣除发行人充值冻结户余额失败，发行人充值冻结户余额不足！");
					} else {
						req.setOrderDesc("发行人放款记录充值冻结户户明细");
						transResp = accountTradeService.addTrans(req, orderResp.getOrderOid(),
								accountInfo.getAccountNo(), rechargeFrozenAccount.getAccountNo(), req.getUserOid(),
								UserTypeEnum.PUBLISHER.getCode(), rechargeFrozenAccountAfterBalance, "02",
								AccountTypeEnum.RECHARGEFROZEN.getCode(), rechargeFrozenAccount.getAccountNo());
					}
				}
				// 增加发行人可用金户或产品户余额
				log.info("增加发行人" + msg + "余额:{}", orderMoney);
				int updateAccountInfoResult = accountInfoDao.addBalance(orderMoney, accountInfo.getAccountNo());
				log.info(
						"发行人放款账户更新结果：updateAccountInfoResult={},updateBasicAccountResult={},updateRechargeFrozenAccountResult={}",
						updateAccountInfoResult, updateBasicAccountResult, updateRechargeFrozenAccountResult);
				if (updateAccountInfoResult == 0) {
					log.error("增加发行人" + msg + "余额失败！");
					throw new GHException(Integer.parseInt(Constant.BALANCELESS), "增加发行人" + msg + "余额失败！");
				}

				BigDecimal accountInfoAfterBalance = BigDecimal.ZERO;
				BigDecimal basicAccountAfterBalance = BigDecimal.ZERO;

				basicAccountAfterBalance = basicAccountBalance.subtract(orderMoney);
				accountInfoAfterBalance = accountInfoBalance.add(orderMoney);
				/**
				 * 插入交易流水
				 */
				req.setOrderDesc("发行人放款" + msg + "明细");
				if (isProductAccount) {
					transResp = accountTradeService.addTrans(req, orderResp.getOrderOid(), accountInfo.getAccountNo(),
							basicAccount.getAccountNo(), req.getUserOid(), UserTypeEnum.PUBLISHER.getCode(),
							accountInfoAfterBalance, "01", AccountTypeEnum.PRODUCT.getCode(),
							accountInfo.getAccountNo());
				} else {
					transResp = accountTradeService.addTrans(req, orderResp.getOrderOid(), accountInfo.getAccountNo(),
							basicAccount.getAccountNo(), req.getUserOid(), UserTypeEnum.PUBLISHER.getCode(),
							accountInfoAfterBalance, "01", AccountTypeEnum.AVAILABLE_AMOUNT.getCode(),
							accountInfo.getAccountNo());
				}

				req.setOrderDesc("发行人放款基本户明细");
				transResp = accountTradeService.addTrans(req, orderResp.getOrderOid(), accountInfo.getAccountNo(),
						basicAccount.getAccountNo(), req.getUserOid(), UserTypeEnum.PUBLISHER.getCode(),
						basicAccountAfterBalance, "02", AccountTypeEnum.BASICER.getCode(), basicAccount.getAccountNo());

				orderEntity.setInputAccountNo(accountInfo.getAccountNo());
				orderEntity.setOutpuptAccountNo(basicAccount.getAccountNo());
				orderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_SUCCESS);
				orderDao.saveAndFlush(orderEntity);
			}
		} else {
			resp.setReturnCode(orderResp.getReturnCode());
			resp.setErrorMessage(orderResp.getErrorMessage());
			return resp;
		}
		if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
			return resp;
		} else {
			resp.setReturnCode(transResp.getReturnCode());
			resp.setErrorMessage(transResp.getErrorMessage());
			log.info("交易失败 orderNo={},resp={}", req.getOrderNo(), JSONObject.toJSON(resp));
			return resp;
		}
	}

	/**
	 * 获取指定类型的产品户
	 * @param req
	 * @return
	 */
	private AccountInfoEntity getAccountInfoByAccountType(AccountTransRequest req) {
		AccountInfoEntity accountInfo = null;

		// 查询交易账户是否为产品户
		if (AccountTypeEnum.PRODUCT.getCode().equals(req.getTransAccountType())) {
			accountInfo = accountInfoDao.findByUserOidAndAccountTyp(req.getUserOid(),
					AccountTypeEnum.PRODUCT.getCode());
		} else {
			// 查询发行人可用金户
			accountInfo = accountInfoDao.findByUserOidAndAccountTyp(req.getUserOid(),
					AccountTypeEnum.AVAILABLE_AMOUNT.getCode());
		}

		return accountInfo;
	}

	/**
	 * 是否为产品户
	 * @param req
	 * @return
	 */
	private boolean isProductAccount(AccountTransRequest req) {
		if (AccountTypeEnum.PRODUCT.getCode().equals(req.getTransAccountType())) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 请求req转换订单req 
	 * @Title: convertTransRequest 
	 * @param req 
	 * @param ordReq 
	 * @return void 
	 * @throws
	 */
	public void convertTransRequest(AccountTransRequest req, CreateOrderRequest ordReq) {
		ordReq.setOrderNo(req.getOrderNo());
		ordReq.setRequestNo(req.getRequestNo());
		ordReq.setOrderNo(req.getOrderNo());
		ordReq.setUserOid(req.getUserOid());
		ordReq.setOrderType(req.getOrderType());
		ordReq.setProductType(req.getProductType());
		ordReq.setPublisherUserOid(req.getUserOid());
		ordReq.setOutputRelationProductNo(req.getOutputRelationProductNo());
		ordReq.setOutputRelationProductName(req.getOutputRelationProductName());
		ordReq.setBalance(req.getBalance());
		ordReq.setVoucher(req.getVoucher()); // 代金券
		ordReq.setSystemSource(req.getSystemSource());
		ordReq.setRemark(req.getRemark());
		ordReq.setOrderDesc(req.getOrderDesc());
		ordReq.setOrderCreatTime(req.getOrderCreatTime());
		ordReq.setFee(req.getFee());
	}

	/**
	 * 交易流水查询 
	 * @Title: tansDetailQueryList 
	 * @Description: 
	 * @param req 
	 * @return TransDetailListResponse 
	 * @throws
	 */
	public TransDetailListResponse tansDetailQueryList(final TransDetailQueryRequest req) {
		int page = req.getPage();
		int rows = req.getRows();
		if (page < 1) {
			page = 1;
		}
		if (rows < 1) {
			rows = 1;
		}

		Direction sortDirection = Direction.ASC;
		if (!"ASC".equals(req.getSort())) {
			sortDirection = Direction.DESC;
		}

		String sortField = req.getSortField();
		if (StringUtil.isEmpty(sortField)) {
			sortField = "createTime";
		}
		List<Order> orders = new ArrayList<>();
		orders.add(new Order(sortDirection, sortField));
		orders.add(new Order(sortDirection, "oid"));
		Pageable pageable = new PageRequest(page - 1, rows, new Sort(orders));

		Specification<TransEntity> spec = new Specification<TransEntity>() {
			public Predicate toPredicate(Root<TransEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> list = new ArrayList<Predicate>();
				/*
				 * // 根据用户id查询 String userOid = req.getUserOid(); if
				 * (!StringUtil.isEmpty(userOid)) { Predicate pre =
				 * cb.equal(root.get("userOid").as(String.class), userOid); list.add(pre); }
				 */

				// 用户类型
				String userType = req.getUserType();
				if (!StringUtil.isEmpty(userType)) {
					Predicate pre = cb.equal(root.get("userType").as(String.class), userType);
					list.add(pre);
				}
				// 账户类型
				String accountType = req.getAccountType();
				if (!StringUtil.isEmpty(accountType)) {
					Predicate pre = cb.equal(root.get("accountType").as(String.class), accountType);
					list.add(pre);
				}
				/*
				 * // 关联产品 String relationProduct = req.getRelationProduct(); if
				 * (!StringUtil.isEmpty(relationProduct)) { Predicate pre =
				 * cb.equal(root.get("relationProduct").as(String.class), relationProduct);
				 * list.add(pre); }
				 */

				// 手机号
				if (!StringUtil.isEmpty(req.getPhone())) {
					UserInfoEntity userInfoEntity = userInfoService.getAccountUserByPhone(req.getPhone());
					if (null == userInfoEntity) { // 构造一个错误的userOid
						list.add(cb.equal(root.get("userOid").as(String.class), StringUtil.uuid()));
					} else {
						list.add(cb.equal(root.get("userOid").as(String.class), userInfoEntity.getUserOid()));
					}
				}

				String accountOid = req.getAccountOid();
				if (!StringUtil.isEmpty(accountOid)) {
					Predicate pre = cb.equal(root.get("accountOid").as(String.class), accountOid);
					list.add(pre);
				}

				// 订单类型
				String orderNo = req.getOrderNo();
				if (!StringUtil.isEmpty(orderNo)) {
					Predicate pre = cb.equal(root.get("orderNo").as(String.class), orderNo);
					list.add(pre);
				}
				// 订单类型
				String orderType = req.getOrderType();
				if (!StringUtil.isEmpty(orderType)) {
					Predicate pre = cb.equal(root.get("orderType").as(String.class), orderType);
					list.add(pre);
				}
				String startTime = req.getStartTime();
				if (!StringUtil.isEmpty(startTime)) {
					Date beginDate = DateUtil.parseDate(startTime, "yyyy-MM-dd HH:mm:ss");
					list.add(cb.greaterThanOrEqualTo(root.get("updateTime").as(Timestamp.class),
							new Timestamp(beginDate.getTime())));
				}

				String endTime = req.getEndTime();
				if (!StringUtil.isEmpty(endTime)) {
					Date endDate = DateUtil.parseDate(req.getEndTime(), "yyyy-MM-dd HH:mm:ss");
					list.add(cb.lessThanOrEqualTo(root.get("updateTime").as(Timestamp.class),
							new Timestamp(endDate.getTime())));
				}

				Predicate[] p = new Predicate[list.size()];
				return cb.and(list.toArray(p));
			}
		};

		Page<TransEntity> result = transDao.findAll(spec, pageable);

		TransDetailListResponse resp = new TransDetailListResponse();
		if (null != result && result.getTotalElements() != 0) {
			resp.setTotal(result.getTotalElements());
			for (TransEntity entity : result.getContent()) {
				TransDto tempEntity = new TransDto();
				BeanUtils.copyProperties(entity, tempEntity, new String[] { "createTime", "updateTime" });
				tempEntity.setUpdateTime(DateUtil.format(entity.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
				UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(tempEntity.getUserOid());
				if (null != userInfoEntity) {
					tempEntity.setPhone(userInfoEntity.getPhone());
				}
				tempEntity.setOrderType(EventTypeEnum.getEnumName(entity.getOrderType()));
				if("62".equals(entity.getOrderType())){
					tempEntity.setOrderType(OrderTypeEnum.getEnumName(entity.getOrderType()));
				}
				TransDetailQueryResponse qresp = new TransDetailQueryResponse(tempEntity);
				resp.getRows().add(qresp);
			}

		}
		return resp;

	}

	public void updateDividendOrderStatus(AccountTransResponse resp, String orderNo) {
		AccountDividendOrderEntity orderEntity = dividendOrderDao.findByOrderNo(orderNo);
		if (null != orderEntity) {
			orderEntity.setOrderDesc(resp.getErrorMessage());
			orderEntity.setDividendStatus(
					Constant.SUCCESS.equals(resp.getReturnCode()) ? AccountDividendOrderEntity.ORDERSTATUS_SUCCESS
							: AccountDividendOrderEntity.ORDERSTATUS_FAIL);
			dividendOrderDao.saveAndFlush(orderEntity);
		}
	}

	/**
	 * 处理派息交易流程
	 */
	@Async("dividendAsync")
	public void tradeDividend(AccountTransRequest vo, String accountType) {
		log.info("派息交易:用户id[" + JSONObject.toJSONString(vo) + "]");
		CreateAccountResponse accResp = null;

		// 账户号
		String accountNo = "";
		String userOid = vo.getUserOid();
		String userType = vo.getUserType();
		String relationProductNo = vo.getRelationProductNo();
		String orderType = vo.getOrderType();

		CreateOrderResponse orderResp = null;
		String orderNo = vo.getOrderNo();

		try {
			/*
			 * 入账单据表
			 */
			CreateOrderRequest ordReq = new CreateOrderRequest();
			ordReq.setOrderNo(vo.getOrderNo());
			ordReq.setRequestNo(vo.getRequestNo());
			ordReq.setOrderNo(orderNo);
			ordReq.setUserOid(userOid);
			ordReq.setOrderType(orderType);
			ordReq.setRelationProductNo(relationProductNo);
			ordReq.setOutputRelationProductNo(vo.getOutputRelationProductNo());
			ordReq.setOutputRelationProductName(vo.getOutputRelationProductName());
			ordReq.setBalance(vo.getBalance());
			ordReq.setVoucher(vo.getVoucher()); // 代金券
			ordReq.setSystemSource(vo.getSystemSource());
			ordReq.setRemark(vo.getRemark());
			ordReq.setOrderDesc(vo.getOrderDesc());
			ordReq.setOrderCreatTime(vo.getOrderCreatTime());
			log.info("生成交易定单");
			orderResp = orderService.addAccOrder(ordReq);
		} catch (Exception e) {
			log.error("系统繁忙,保存定单失败", e);
			return;
		}

		// 判断用户，类型是否存在
		AccountTransResponse resp = checkAccountTransRequest(vo);
		if (!resp.getReturnCode().equals(Constant.SUCCESS)) {
			updateOrderStauts(resp, orderNo, "");
			updateDividendOrderStatus(resp, orderNo);
			return;
		}

		// 账户属性赋值
		CreateAccountRequest accreq = new CreateAccountRequest();
		accreq.setUserOid(userOid);
		accreq.setUserType(userType);
		accreq.setRelationProduct(relationProductNo);

		try {
			/*
			 * 对账户的余额进行处理，余额处理成功后记录交易流水
			 */
			if (Constant.SUCCESS.equals(orderResp.getReturnCode())) {
				accreq.setAccountType(accountType);
				accResp = accountInfoService.addAccount(accreq);
				if (!Constant.SUCCESS.equals(accResp.getReturnCode())) {
					log.info("创建用户失败{}", JSONObject.toJSON(accResp));
					resp.setReturnCode(accResp.getReturnCode());
					resp.setErrorMessage(accResp.getErrorMessage());
					updateOrderStauts(resp, orderNo, "");
					updateDividendOrderStatus(resp, orderNo);
					return;
				}
				accountNo = accResp.getAccountNo();
				log.info("更新各账户余额");
				CreateTransResponse transResp = null;
				transResp = accountTradeService.addAccountTrans(accountNo, orderResp.getOrderOid(), vo, accountType);
				log.info("更新各账户余额结束");
				if (Constant.SUCCESS.equals(transResp.getReturnCode())) {
					resp.setReturnCode(Constant.SUCCESS);
					resp.setErrorMessage("成功");
					resp.setUserOid(userOid);
					resp.setUserType(userType);
					resp.setTransType(orderType);
					resp.setBalance(vo.getBalance());
					resp.setRequestNo(vo.getRequestNo());
				} else {
					resp.setReturnCode(transResp.getReturnCode());
					resp.setErrorMessage(transResp.getErrorMessage());
				}
			} else {
				resp.setReturnCode(orderResp.getReturnCode());
				resp.setErrorMessage(orderResp.getErrorMessage());
			}
		} catch (GHException e) {
			log.error("系统繁忙,处理定单交易事务失败", e);
			resp.setErrorMessage(e.getMessage());
			resp.setReturnCode(String.valueOf(e.getCode()));

		} catch (Exception e) {
			log.error("系统繁忙,定单交易失败", e);
			resp.setErrorMessage("系统繁忙");
			resp.setReturnCode(Constant.FAIL);
		}
		updateOrderStauts(resp, orderNo, "");
		updateDividendOrderStatus(resp, orderNo);
	}

	/**
	 * 会员账户交易 
	 * @Title: addAccountTrans 
	 * @param req 
	 * @return AccountTransResponse 
	 * @throws
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public AccountTransResponse trade(AccountTransRequest req) {
		log.info("账户交易:用户id[" + JSONObject.toJSONString(req) + "]");
		AccountTransResponse resp = new AccountTransResponse();

		// 20170329,新增冻结账户，用户标记第一次记账
		String accountTimes = "";
		if (OrderTypeEnum.WITHDRAWALS.getCode().equals(req.getOrderType())) {
			accountTimes = Constant.FIRSTIME;
		}
		BeanUtils.copyProperties(req, resp);
		CreateAccountResponse accResp = null;
		CreateAccountResponse frozenAccResp = null;
		// 账户号
		String accountNo = "";
		String userOid = req.getUserOid();
		String userType = req.getUserType();
		String relationProductNo = req.getRelationProductNo();
		String orderType = req.getOrderType();
		String accountType = "";

		CreateOrderResponse orderResp = null;
		String orderNo = req.getOrderNo();

		try {
			/**
			 * 入账单据表
			 */
			CreateOrderRequest ordReq = new CreateOrderRequest();
			ordReq.setOrderNo(req.getOrderNo());
			ordReq.setRequestNo(req.getRequestNo());
			ordReq.setOrderNo(orderNo);
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
			orderResp = orderService.addAccOrder(ordReq);
			// 20170329,新增冻结账户，用户标记第二次记账
			if (Constant.SECONDTIME.equals(orderResp.getReturnCode())) {
				orderResp.setReturnCode(Constant.SUCCESS);
				accountTimes = Constant.SECONDTIME;
			}
		} catch (Exception e) {
			log.error("系统繁忙,保存定单失败", e);
			resp.setErrorMessage("系统繁忙,保存定单失败");
			resp.setReturnCode(Constant.FAIL);
			return resp;
		}

		// 判断用户，类型是否存在
		resp = checkAccountTransRequest(req);
		if (!resp.getReturnCode().equals(Constant.SUCCESS)) {
			updateOrderStauts(resp, orderNo, accountTimes);
			return resp;
		}

		// 账户属性赋值
		CreateAccountRequest accreq = new CreateAccountRequest();
		accreq.setUserOid(userOid);
		accreq.setUserType(userType);
		accreq.setRelationProduct(relationProductNo);

		/**
		 * 根据定单类别，获取用户账户类别 如：定单类别为申购活期， 账户类别应该是活期户
		 */
		if (orderType.equals(OrderTypeEnum.APPLY.getCode()) || orderType.equals(OrderTypeEnum.PUBLISH.getCode())
				|| orderType.equals(OrderTypeEnum.REDEEM.getCode())) {
			if (StringUtil.isEmpty(req.getProductType())) {
				resp.setErrorMessage("产品类别不能为空");
				resp.setReturnCode(Constant.PRODUCTTYPEISNULL);
				updateOrderStauts(resp, orderNo, accountTimes);
				return resp;
			}
			if (StringUtil.isEmpty(relationProductNo)) {
				resp.setErrorMessage("关联产品不能为空");
				resp.setReturnCode(Constant.RelationProductNotNULL);
				updateOrderStauts(resp, orderNo, accountTimes);
				return resp;
			}
			// 设置定单类别
			accountType = req.getProductType();
		} else if (orderType.equals(OrderTypeEnum.DIVIDEND.getCode())) {
			if (StringUtil.isEmpty(req.getProductType())) {
				resp.setErrorMessage("产品类别不能为空");
				resp.setReturnCode(Constant.PRODUCTTYPEISNULL);
				updateOrderStauts(resp, orderNo, accountTimes);
				return resp;
			}
			if (StringUtil.isEmpty(relationProductNo)) {
				resp.setErrorMessage("关联产品不能为空");
				resp.setReturnCode(Constant.RelationProductNotNULL);
				updateOrderStauts(resp, orderNo, accountTimes);
				return resp;
			}
			// 如果订单类型为派息,判断是定期还是活期派息
			if (AccountTypeEnum.CURRENT.getCode().equals(req.getProductType())) {
				accountType = AccountTypeEnum.CURRENTINTEREST.getCode();
			} else if (AccountTypeEnum.REGULAR.getCode().equals(req.getProductType())) {
				accountType = AccountTypeEnum.REGULARINTEREST.getCode();
			} else {
				resp.setErrorMessage("产品类别不支持");
				resp.setReturnCode(Constant.PRODUCTTYPEISNULL);
				updateOrderStauts(resp, orderNo, accountTimes);
				return resp;
			}
		} else if (orderType.equals(OrderTypeEnum.GIVEFREEMONEY.getCode())) {
			// 如果订单类型为赠送体验金,账户类型为体验金户
			accountType = AccountTypeEnum.EXPERIENCE.getCode();
		} else if (orderType.equals(OrderTypeEnum.EXPIREMONEY.getCode())) {
			// 如果订单类型为体验金到期,账户类型为体验金户
			accountType = AccountTypeEnum.EXPERIENCE.getCode();
		} else if (StringUtil.in(orderType, OrderTypeEnum.RECHARGE.getCode(), OrderTypeEnum.WITHDRAWALS.getCode())) {
			// 充值提现 操作基本户
			if (!StringUtil.isEmpty(relationProductNo)) {
				resp.setErrorMessage("充值或者提现不能关联产品");
				resp.setReturnCode(Constant.FAIL);
				updateOrderStauts(resp, orderNo, accountTimes);
				return resp;
			}
			// 20170329新增提现冻结账户逻辑
			if (Constant.FIRSTIME.equals(accountTimes)) {
				accountType = AccountTypeEnum.FROZEN.getCode();
			} else if (Constant.KILLORDER.equals(req.getRemark())) {
				accountType = AccountTypeEnum.FROZEN.getCode();
			} else {
				accountType = AccountTypeEnum.BASICER.getCode();
			}
		} else if (StringUtil.in(orderType, OrderTypeEnum.CURRENTTOREGULAR.getCode(),
				OrderTypeEnum.REGULARTOCURRENT.getCode())) {
			// 产品转换 获取交易类别
			if (StringUtil.isEmpty(relationProductNo) || StringUtil.isEmpty(req.getOutputRelationProductNo())) {
				resp.setErrorMessage("产品转换，转入和转出产品编号不能为空");
				resp.setReturnCode(Constant.FAIL);
				updateOrderStauts(resp, orderNo, accountTimes);
				return resp;
			}
			accountType = orderType.equals(OrderTypeEnum.CURRENTTOREGULAR.getCode()) ? AccountTypeEnum.CURRENT.getCode()
					: AccountTypeEnum.REGULAR.getCode();
		} else if (StringUtil.in(orderType, OrderTypeEnum.OFFSETPOSITIVE.getCode(),
				OrderTypeEnum.OFFSETNEGATIVE.getCode(), OrderTypeEnum.USE_RED_PACKET.getCode())) {
			// 冲正冲负、红包 操作基本户
			if (!StringUtil.isEmpty(relationProductNo)) {
				resp.setErrorMessage("冲正冲负或者红包不能关联产品");
				resp.setReturnCode(Constant.FAIL);
				updateOrderStauts(resp, orderNo, accountTimes);
				return resp;
			}
			accountType = AccountTypeEnum.BASICER.getCode();
		}

		try {
			/**
			 * 对账户的余额进行处理，余额处理成功后记录交易流水
			 */
			if (orderResp.getReturnCode().equals(Constant.SUCCESS)) {
				accreq.setAccountType(accountType);
				accResp = accountInfoService.addAccount(accreq);
				if (!Constant.SUCCESS.equals(accResp.getReturnCode())) {
					log.info("创建用户失败{}", JSONObject.toJSON(accResp));
					resp.setReturnCode(accResp.getReturnCode());
					resp.setErrorMessage(accResp.getErrorMessage());
					updateOrderStauts(resp, orderNo, accountTimes);
					return resp;
				}
				// 20170526，充值创建用户充值冻结户
				if ("Y".equals(needRechargeFrozenAccount) && OrderTypeEnum.RECHARGE.getCode().equals(orderType)) {
					accountType = AccountTypeEnum.RECHARGEFROZEN.getCode();
					accreq.setAccountType(accountType);
					frozenAccResp = accountInfoService.addAccount(accreq);
					if (!Constant.SUCCESS.equals(frozenAccResp.getReturnCode())) {
						log.info("创建用户充值冻结户失败{}", JSONObject.toJSON(frozenAccResp));
						resp.setReturnCode(frozenAccResp.getReturnCode());
						resp.setErrorMessage(frozenAccResp.getErrorMessage());
						updateOrderStauts(resp, orderNo, accountTimes);
						return resp;
					}
				}
				accountNo = accResp.getAccountNo();
				log.info("更新各账户余额");
				CreateTransResponse transResp = null;
				transResp = accountTradeService.addAccountTrans(accountNo, orderResp.getOrderOid(), req, accountType);
				log.info("更新各账户余额结束");
				if (Constant.SUCCESS.equals(transResp.getReturnCode())) {
					resp.setReturnCode(Constant.SUCCESS);
					resp.setErrorMessage("成功");
					resp.setUserOid(userOid);
					resp.setUserType(userType);
					resp.setTransType(orderType);
					resp.setBalance(req.getBalance());
					resp.setRequestNo(req.getRequestNo());
				} else {
					resp.setReturnCode(transResp.getReturnCode());
					resp.setErrorMessage(transResp.getErrorMessage());
				}
			} else {
				resp.setReturnCode(orderResp.getReturnCode());
				resp.setErrorMessage(orderResp.getErrorMessage());
			}
		} catch (GHException e) {
			log.error("系统繁忙,处理定单交易事务失败", e);
			resp.setErrorMessage(e.getMessage());
			resp.setReturnCode(String.valueOf(e.getCode()));

		} catch (Exception e) {
			log.error("系统繁忙,定单交易失败", e);
			resp.setErrorMessage("系统繁忙");
			resp.setReturnCode(Constant.FAIL);
		}
		if (AccountTypeEnum.FROZEN.getCode().equals(accountType) && Constant.SUCCESS.equals(resp.getReturnCode())) {
			resp.setOrderStatus(AccOrderEntity.ORDERSTATUS_KILL);
		}
		updateOrderStauts(resp, orderNo, accountTimes);
		return resp;
	}

	/**
	 * 对账补单
	 */
	public AccountTransResponse tradeReconciliation(AccountTransRequest req) {
		log.info("账户交易:用户id[" + JSONObject.toJSONString(req) + "]");
		AccountTransResponse resp = new AccountTransResponse();
		String accountTimes = "";
		BeanUtils.copyProperties(req, resp);
		CreateAccountResponse accResp = null;
		// 账户号
		String accountNo = "";
		String userOid = req.getUserOid();
		String userType = req.getUserType();
		String relationProductNo = req.getRelationProductNo();
		String orderType = req.getOrderType();
		String accountType = "";

		CreateOrderResponse orderResp = null;
		String orderNo = req.getOrderNo();

		try {
			/*
			 * 入账单据表
			 */
			CreateOrderRequest ordReq = new CreateOrderRequest();
			ordReq.setOrderNo(req.getOrderNo());
			ordReq.setRequestNo(req.getRequestNo());
			ordReq.setOrderNo(orderNo);
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
			orderResp = orderService.addAccOrder(ordReq);
		} catch (Exception e) {
			log.error("系统繁忙,保存定单失败", e);
			resp.setErrorMessage("系统繁忙,保存定单失败");
			resp.setReturnCode(Constant.FAIL);
			return resp;
		}

		// 判断用户，类型是否存在
		resp = checkAccountTransRequest(req);
		if (!resp.getReturnCode().equals(Constant.SUCCESS)) {
			updateOrderStauts(resp, orderNo, accountTimes);
			return resp;
		}

		// 账户属性赋值
		CreateAccountRequest accreq = new CreateAccountRequest();
		accreq.setUserOid(userOid);
		accreq.setUserType(userType);
		accreq.setRelationProduct(relationProductNo);
		accountType = AccountTypeEnum.BASICER.getCode();
		try {
			/*
			 * 对账户的余额进行处理，余额处理成功后记录交易流水
			 */
			if (orderResp.getReturnCode().equals(Constant.SUCCESS)) {
				accreq.setAccountType(accountType);
				accResp = accountInfoService.addAccount(accreq);
				if (!Constant.SUCCESS.equals(accResp.getReturnCode())) {
					log.info("创建用户失败{}", JSONObject.toJSON(accResp));
					resp.setReturnCode(accResp.getReturnCode());
					resp.setErrorMessage(accResp.getErrorMessage());
					updateOrderStauts(resp, orderNo, accountTimes);
					return resp;
				}
				accountNo = accResp.getAccountNo();
				log.info("更新各账户余额");
				CreateTransResponse transResp = null;
				transResp = accountTradeService.addAccountTransReconciliation(accountNo, orderResp.getOrderOid(), req,
						accountType);
				log.info("更新各账户余额结束");
				if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
					resp.setReturnCode(Constant.SUCCESS);
					resp.setErrorMessage("成功");
					resp.setUserOid(userOid);
					resp.setUserType(userType);
					resp.setTransType(orderType);
					resp.setBalance(req.getBalance());
					resp.setRequestNo(req.getRequestNo());
				} else {
					resp.setReturnCode(transResp.getReturnCode());
					resp.setErrorMessage(transResp.getErrorMessage());
				}
			} else {
				resp.setReturnCode(orderResp.getReturnCode());
				resp.setErrorMessage(orderResp.getErrorMessage());
			}
		} catch (GHException e) {
			log.error("系统繁忙,处理定单交易事务失败", e);
			resp.setErrorMessage(e.getMessage());
			resp.setReturnCode(String.valueOf(e.getCode()));

		} catch (Exception e) {
			log.error("系统繁忙,定单交易失败", e);
			resp.setErrorMessage("系统繁忙");
			resp.setReturnCode(Constant.FAIL);
		}
		if (AccountTypeEnum.FROZEN.getCode().equals(accountType) && Constant.SUCCESS.equals(resp.getReturnCode())) {
			resp.setOrderStatus(AccOrderEntity.ORDERSTATUS_KILL);
		}
		updateOrderStauts(resp, orderNo, accountTimes);
		return resp;
	}

	private void updateOrderStauts(AccountTransResponse resp, String orderNo, String accountTimes) {
		AccOrderEntity accOrderEntity = orderDao.findByOrderNo(orderNo);
		if (null != accOrderEntity) {
			accOrderEntity.setOrderDesc(resp.getErrorMessage());
			// 撤单将订单状态改为撤单
			if (AccOrderEntity.ORDERSTATUS_KILL.equals(resp.getOrderStatus())) {
				accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_KILL);
			} else {
				accOrderEntity.setOrderStatus(
						Constant.SUCCESS.equals(resp.getReturnCode()) ? AccOrderEntity.ORDERSTATUS_SUCCESS
								: AccOrderEntity.ORDERSTATUS_FAIL);
			}

			log.info("更新订单{}", JSONObject.toJSON(accOrderEntity));
			// 20170329新增冻结账户，提现第一次记账，订单状态置为初始化状态
			if (Constant.FIRSTIME.equals(accountTimes) && Constant.SUCCESS.equals(resp.getReturnCode())
					&& OrderTypeEnum.WITHDRAWALS.getCode().equals(resp.getTransType())) {
				accOrderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_INIT);
				log.info("提现首次记账，无需更新订单状态。");
			} else {
				orderDao.saveAndFlush(accOrderEntity);
			}

			// 20170324失败订单保存至表t_account_fail_order_notify用于短信通知
			if (AccOrderEntity.ORDERSTATUS_FAIL.equals(accOrderEntity.getOrderStatus())) {
				AccFailOrderNotifyEntity failEntity = new AccFailOrderNotifyEntity();
				failEntity.setUserOid(accOrderEntity.getUserOid());
				failEntity.setReceiveTime(accOrderEntity.getReceiveTime());
				failEntity.setOrderNo(orderNo);
				failEntity.setOrderStatus(accOrderEntity.getOrderStatus());
				failEntity.setOrderDesc(accOrderEntity.getOrderDesc());
				failEntity.setNotified("N");
				accFailOrderNotifyDao.save(failEntity);
			}
		} else {
			log.info("订单不存在orderNo：{}", orderNo);
		}
	}

	/**
	 * 批量发行人产品户增加余额方法
	 * @Title: trade 
	 * @Description: dataList 
	 * @return AccountTransResponse 
	 * @throws
	 */
	@Transactional
	public AccountTransResponse trade(List<TransPublishRequest> dataList) {
		AccountTransResponse resp = new AccountTransResponse();
		log.info("账户交易 批量增加发行人发行额");
		String orderNo = "";
		String orderType = "";
		String requestNo = "";
		String relationProductNo = "";
		String accountNo = "";
		String systemSource = "";
		BigDecimal balance = BigDecimal.ZERO;

		try {
			for (TransPublishRequest data : dataList) {
				log.info("data：{}", data);
				orderNo = data.getOrderNo();
				balance = data.getBalance();
				orderType = data.getOrderType();
				requestNo = data.getRequestNo();
				relationProductNo = data.getRelationProductNo();
				accountNo = data.getAccountNo();
				systemSource = data.getSystemSource();

				/**
				 * 入账单据表
				 */
				CreateOrderResponse orderResp = null;
				CreateOrderRequest ordReq = new CreateOrderRequest();
				ordReq.setOrderNo(orderNo);
				ordReq.setRequestNo(requestNo);
				ordReq.setUserOid("");
				ordReq.setOrderType(orderType);
				ordReq.setRelationProductNo(relationProductNo);
				ordReq.setBalance(balance);
				ordReq.setSystemSource(systemSource);
				ordReq.setInputAccountNo("");
				ordReq.setOutpuptAccountNo("");
				ordReq.setBusinessStatus("");
				ordReq.setFinanceStatus("");
				ordReq.setSystemSource(systemSource);
				ordReq.setRemark("");
				orderResp = orderService.addAccOrder(ordReq);
				/**
				 * 对账户的余额进行处理，余额处理成功后记录交易流水
				 */
				if (orderResp.getReturnCode().equals(Constant.SUCCESS)) {
					CreateTransResponse transResp = accountTradeService.addAccountTrans(accountNo,
							orderResp.getOrderOid(), data);
					if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
						resp.setReturnCode(Constant.SUCCESS);
						resp.setErrorMessage("成功");
					} else {
						resp.setReturnCode(transResp.getReturnCode());
						resp.setErrorMessage(transResp.getErrorMessage());
					}
				} else {
					resp.setReturnCode(orderResp.getReturnCode());
					resp.setErrorMessage(orderResp.getErrorMessage());
				}
			}
		} catch (GHException e) {
			log.error("系统繁忙,保存定单失败", e);
			throw new GHException(e.getCode(), e.getMessage());
		} catch (Exception e) {
			log.error("系统繁忙,保存定单失败", e);
			throw new GHException(Integer.parseInt(Constant.FAIL), resp.getErrorMessage());
		}
		return resp;
	}

	/**
	 * 平台、投资人转账 
	 * @Title: transferAccount 
	 * @param req 
	 * @return TransferAccountResponse 
	 * @throws
	 */
	public TransferAccountResponse transferAccount(TransferAccountRequest req) {
		log.info("转账交易:[" + JSONObject.toJSONString(req) + "]");
		TransferAccountResponse resp = new TransferAccountResponse();
		resp.setReturnCode(Constant.SUCCESS);
		// 判断是否类型存在
		resp = checkTransferRequest(req);
		if (!resp.getReturnCode().equals(Constant.SUCCESS)) {
			return resp;
		}

		String relationProductNo = "";
		String orderType = req.getOrderType();
		String inputAccountNo = req.getInputAccountNo();
		String outputAccountNo = req.getOutpuptAccountNo();

		try {

			/**
			 * 入账单据表
			 */
			String orderNo = req.getOrderNo();
			CreateOrderRequest ordReq = new CreateOrderRequest();
			ordReq.setOrderNo(req.getOrderNo());
			ordReq.setRequestNo(req.getRequestNo());
			ordReq.setOrderNo(orderNo);
			ordReq.setOrderType(orderType);
			ordReq.setRelationProductNo(relationProductNo);
			ordReq.setBalance(req.getBalance());
			ordReq.setInputAccountNo(inputAccountNo);
			ordReq.setOutpuptAccountNo(outputAccountNo);
			ordReq.setBusinessStatus("");
			ordReq.setFinanceStatus("");
			CreateOrderResponse orderResp = orderService.addAccOrder(ordReq);

			if (orderResp.getReturnCode().equals(Constant.SUCCESS)) {
				CreateTransResponse transResp = accountTradeService.addAccountTrans(orderResp.getOrderOid(), req);
				if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
					resp.setReturnCode(Constant.SUCCESS);
					resp.setErrorMessage("成功");
					resp.setInputAccountNo(inputAccountNo);
					resp.setOutpuptAccountNo(outputAccountNo);
					resp.setBalance(req.getBalance());
					resp.setRequestNo(req.getRequestNo());
					resp.setOrderType(orderType);
					resp.setOrderNo(orderNo);
				} else {
					resp.setReturnCode(transResp.getReturnCode());
					resp.setErrorMessage(transResp.getErrorMessage());
					// 把订单状态修改成失败
					AccOrderEntity order = orderService.getOrderByNo(orderNo);
					order.setOrderStatus(AccOrderEntity.ORDERSTATUS_INIT);
					orderDao.save(order);
				}
			}
		} catch (GHException e) {
			log.error("系统繁忙,处理定单交易事务失败", e);
			resp.setErrorMessage(e.getMessage());
			resp.setReturnCode(String.valueOf(e.getCode()));
		} catch (Exception e) {
			log.error("系统繁忙,定单交易失败", e);
			resp.setErrorMessage("系统繁忙");
			resp.setReturnCode(Constant.FAIL);
		}

		return resp;
	}

	/**
	 * 平台、投资人入账
	 * @Title: enterAccount 
	 * @Description: @param 
	 * @param req 
 	 * @return EnterAccountResponse 
 	 * @throws
	 */
	public EnterAccountResponse enterAccount(EnterAccountRequest req) {
		EnterAccountResponse resp = new EnterAccountResponse();
		resp.setReturnCode(Constant.SUCCESS);
		// 判断是否类型存在
		resp = checkEnterAccountRequest(req);
		if (!resp.getReturnCode().equals(Constant.SUCCESS)) {
			return resp;
		}
		try {
			String orderType = req.getOrderType();
			String inputAccountNo = req.getInputAccountNo();

			/**
			 * 入账单据表
			 */
			String orderNo = req.getOrderNo();
			CreateOrderRequest ordReq = new CreateOrderRequest();
			ordReq.setOrderNo(req.getOrderNo());
			ordReq.setRequestNo(req.getRequestNo());
			ordReq.setOrderNo(orderNo);
			ordReq.setOrderType(orderType);
			ordReq.setBalance(req.getBalance());
			ordReq.setInputAccountNo(inputAccountNo);
			ordReq.setBusinessStatus("");
			ordReq.setFinanceStatus("");
			ordReq.setRemark(req.getRemark());
			// ordReq.setOrderCreatTime(req.getOrderCreatTime());//后期可加上订单创建时间
			CreateOrderResponse orderResp = orderService.addAccOrder(ordReq);

			if (orderResp.getReturnCode().equals(Constant.SUCCESS)) {
				CreateTransResponse transResp = accountTradeService.addAccountTrans(orderResp.getOrderOid(), req);
				if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
					resp.setReturnCode(Constant.SUCCESS);
					resp.setErrorMessage("成功");
					resp.setInputAccountNo(inputAccountNo);
					resp.setBalance(req.getBalance());
					resp.setRequestNo(req.getRequestNo());
					resp.setOrderType(orderType);
					resp.setOrderNo(orderNo);
				} else {
					resp.setReturnCode(transResp.getReturnCode());
					resp.setErrorMessage(transResp.getErrorMessage());
					// 把订单状态修改成失败
					AccOrderEntity order = orderService.getOrderByNo(orderNo);
					order.setOrderStatus(AccOrderEntity.ORDERSTATUS_INIT);
					orderDao.save(order);
				}

			}
		} catch (GHException e) {
			log.error("系统繁忙,处理定单交易事务失败", e);
			resp.setErrorMessage(e.getMessage());
			resp.setReturnCode(String.valueOf(e.getCode()));
		} catch (Exception e) {
			log.error("系统繁忙,定单交易失败", e);
			resp.setErrorMessage("系统繁忙");
			resp.setReturnCode(Constant.FAIL);
		}
		return resp;
	}

	/**
	 * 判断类型是否存在 
	 * @Title: checkAccountTransRequest 
	 * @param req 
	 * @return AccountTransResponse 
	 * @throws
	 */
	public AccountTransResponse checkAccountTransRequest(AccountTransRequest req) {
		AccountTransResponse resp = new AccountTransResponse();
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");
		// 判断用户是否存在
		UserInfoEntity userInfo = userInfoService.getAccountUserByUserOid(req.getUserOid());
		if (userInfo == null) {
			resp.setReturnCode(Constant.USERNOTEXISTS);
			resp.setErrorMessage("用户不存在!");
			log.error("用户不存在![userOid=" + req.getUserOid() + "]");
			return resp;
		}
		if (req.getBalance().compareTo(BigDecimal.ZERO) < 0) {
			// 金额不能小于0
			resp.setReturnCode(Constant.BALANCEERROR);
			resp.setErrorMessage("金额不能为负数！");
			log.error("金额不能为负数，[balance=" + req.getBalance() + "]");
			return resp;
		}
		if (StringUtil.isEmpty(UserTypeEnum.getEnumName(req.getUserType()))) {
			// 用户类型不存在
			resp.setReturnCode(Constant.USERTYPENOTEXISTS);
			resp.setErrorMessage("用户类型不存在！");
			log.error("用户类型不存在，[userType=" + req.getUserType() + "]");
			return resp;
		}
		if (StringUtil.isEmpty(OrderTypeEnum.getEnumName(req.getOrderType()))) {
			// 交易类型不存在
			resp.setReturnCode(Constant.ORDERTYPENOTEXISTS);
			resp.setErrorMessage("交易类别不存在！");
			log.error("交易类别不存在，[orderType=" + req.getOrderType() + "]");
			return resp;
		}
		return resp;
	}

	/**
	 * 判断类型是否存在 
	 * @Title: checkTransferRequest 
	 * @param req 
	 * @return AccountTransResponse 
	 * @throws
	 */
	private TransferAccountResponse checkTransferRequest(TransferAccountRequest req) {
		TransferAccountResponse resp = new TransferAccountResponse();
		resp.setReturnCode(Constant.SUCCESS);
		AccountInfoEntity inAccount = accountInfoService.getAccountByNo(req.getInputAccountNo());
		if (inAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("转入账户不存在!");
			log.error("转入账户不存在![inputAccountNo=" + req.getInputAccountNo() + "]");
			return resp;
		}
		AccountInfoEntity outAccount = accountInfoService.getAccountByNo(req.getInputAccountNo());
		if (outAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("转入账户不存在!");
			log.error("转入账户不存在![outpuptAccountNo=" + req.getOutpuptAccountNo() + "]");
			return resp;
		}
		if (StringUtil.isEmpty(TransferOrderTypeEnum.getEnumName(req.getOrderType()))) {
			// 交易类型不存在
			resp.setReturnCode(Constant.ORDERTYPENOTEXISTS);
			resp.setErrorMessage("交易类别不存在！");
			log.error("交易类别不存在，[orderType=" + req.getOrderType() + "]");
			return resp;
		}
		return resp;
	}

	/**
	 * @Title: checkEnterAccountRequest 
	 * @Description: 判断类型是否存在 
	 * @param req 
	 * @param 
	 * @return EnterAccountResponse
	 * @throws
	 */
	private EnterAccountResponse checkEnterAccountRequest(EnterAccountRequest req) {
		EnterAccountResponse resp = new EnterAccountResponse();
		resp.setReturnCode(Constant.SUCCESS);
		AccountInfoEntity inAccount = accountInfoService.getAccountByNo(req.getInputAccountNo());
		if (inAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("转入账户不存在!");
			log.error("转入账户不存在![inputAccountNo=" + req.getInputAccountNo() + "]");
			return resp;
		}
		if (StringUtil.isEmpty(EnterOrderTypeEnum.getEnumName(req.getOrderType()))) {
			// 交易类型不存在
			resp.setReturnCode(Constant.ORDERTYPENOTEXISTS);
			resp.setErrorMessage("交易类别不存在！");
			log.error("交易类别不存在，[orderType=" + req.getOrderType() + "]");
			return resp;
		}
		return resp;

	}

	/**
	 * 生成对账文件
	 * @Title: buildTransFile 
	 * @Description: req 
	 * @throws Exception 
	 * @return void 
	 * @throws
	 */
	public String buildTransFile(TransDetailQueryRequest req) throws Exception {
		String NEWLINE = System.getProperty("line.separator", "\n");
		String WORK_DIR = System.getProperty("user.dir");
		String FILE_SEPARATOR = System.getProperty("file.separator", "/");

		String startTime = req.getStartTime();
		String endTime = req.getEndTime();

		File directory = null;

		req.setStartTime(startTime);
		req.setEndTime(endTime);

		FileWriter fw = null;
		String filePath = WORK_DIR + FILE_SEPARATOR + "report" + FILE_SEPARATOR;
		String fileName = "从" + startTime + "到" + endTime + "的交易信息";
		String suffix = ".csv";
		directory = new File(filePath);
		directory.mkdirs();
		if (!directory.exists()) {
			return "";
		}
		try {
			fw = new FileWriter(filePath + fileName + suffix, false);
			String header = "流水号,账户号,用户ID,用户类型,请求流水号,收单OID,订单类型,来源系统类型,来源系统单据号,关联产品编码,关联产品名称,金额方向,"
					+ "订单金额,备注,定单描述,账户名称,交易时间,数据来源,交易后余额,删除标记,币种,入账账户,出账账户,财务入账标识" + NEWLINE;
			fw.write(new String(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }));
			fw.write(header);

			TransDetailListResponse resp = this.tansDetailQueryList(req);
			List<TransDetailQueryResponse> transList = resp.getRows();
			int count = transList.size();
			if (transList != null && count > 0) {
				for (int j = 0; j < count; j++) {
					log.info("共" + count + "条数据,正在导出第" + (j + 1) + "条数据, 当前导出进度:"
							+ String.format("%.2f", (j + 1.0) / count * 100.0) + "%");
					StringBuffer str = new StringBuffer();

					TransDetailQueryResponse trans = transList.get(j);

					str.append(trans.getOid() + ",");
					str.append(trans.getAccountOid() + ",");
					str.append(trans.getUserOid() + ",");
					str.append(trans.getUserType() + ",");
					str.append(trans.getRequestNo() + ",");
					str.append(trans.getAccountOrderOid() + ",");
					str.append(trans.getOrderType() + ",");
					str.append(trans.getSystemSource() + ",");
					str.append(trans.getOrderNo() + ",");
					str.append(trans.getRelationProductNo() + ",");
					str.append(trans.getRelationProductName() + ",");
					str.append(trans.getDirection() + ",");
					str.append(trans.getOrderBalance() + ",");
					str.append(trans.getRamark() + ",");
					str.append(trans.getOrderDesc() + ",");
					str.append(trans.getAccountName() + ",");
					str.append(trans.getUpdateTime() + ",");
					str.append(trans.getSystemSource() + ",");
					str.append(trans.getBalance() + ",");
					str.append(trans.getIsDelete() + ",");
					str.append(trans.getCurrency() + ",");
					str.append(trans.getInputAccountNo() + ",");
					str.append(trans.getOutpuptAccountNo() + ",");
					str.append(trans.getFinanceMark());
					str.append("\r\n");

					fw.write(new String(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }));
					fw.write(str.toString());
					fw.flush();
				}
			}

			File file = new File(filePath + fileName + suffix);
			File zipFile = new File(filePath + fileName + ".zip");
			InputStream input = new FileInputStream(file);
			ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
			zipOut.putNextEntry(new ZipEntry(file.getName()));
			int temp = 0;
			while ((temp = input.read()) != -1) {
				zipOut.write(temp);
			}
			input.close();
			zipOut.close();

			log.info("文件[" + filePath + "]生成完毕!!!");
			log.info("end");
		} catch (Exception e) {
			log.error("系统繁忙,导出交易流水失败", e);
		} finally {
			if (fw != null)
				fw.close();
		}
		return filePath;

	}

	/**
	 * 根据UserOid,orderNo查询交易记录
	 * @param userOid
	 * @param orderNo
	 * @return
	 */
	public Map<String, String> getAccountStatus(String userOid, String orderNo) {
		Map<String, String> map = new HashMap<>();
		map.put("isAccount", "0");
		if (StringUtil.isEmpty(userOid) || StringUtil.isEmpty(orderNo)) {
			return map;
		}
		List<TransEntity> trans = null;
		trans = transDao.findByUserOidAndOrderNo(userOid, orderNo);
		if (!CollectionUtils.isEmpty(trans)) {
			map.put("isAccount", "1");
			for (TransEntity tran : trans) {
				AccountInfoEntity accountInfoEntity = accountInfoDao.findByAccountNo(tran.getInputAccountNo());
				map.put(accountInfoEntity.getAccountType(), accountInfoEntity.getAccountNo());
			}

		}
		log.info("根据UserOid:{},orderNo:{}查询交易记录:{}", userOid, orderNo, map.toString());
		return map;
	}

	/**
	 * 根据UserOid,orderNo查询提现冻结户、基本户是否记账 0未记账 1 提现冻结户记账 2 基本户记账
	 * @param userOid
	 * @param orderNo
	 * @return
	 */
	public String getWithdrawalsAccountStatus(String userOid, String orderNo) {
		Map<String, String> map = getAccountStatus(userOid, orderNo);
		if ("1".equals(map.get("isAccount").toString())) {
			if (null != map.get(AccountTypeEnum.BASICER.getCode())) {
				return Constant.BASICERACCTING;
			}

			if (null != map.get(AccountTypeEnum.FROZEN.getCode())) {
				return Constant.FROZENACCTING;
			}
		}
		log.info("根据用户订单号{}查询该订单记账状态{}", orderNo);
		return Constant.NOACCTING;
	}

	/**
	 * 申购撤单
	 */
	@Transactional
	public PurchaseTransCancelResponse transCancel(PurchaseTransCancelRequest req) {
		final String oldOrderNo = req.getOldOrderNo();
		final AccOrderEntity order = orderService.getOrderByNo(oldOrderNo);

		PurchaseTransCancelResponse resp = checkTransCancelRequest(req, order);
		log.info("申购撤单参数检查结果：{}", resp);
		if (!Constant.SUCCESS.equals(resp.getReturnCode())) {
			return resp;
		}

		resp.setPublisherUserOid(req.getPublisherUserOid());
		resp.setUserOid(req.getUserOid());
		resp.setOldOrderOid(req.getOldOrderNo());

		if (!AccOrderEntity.ORDERSTATUS_SUCCESS.equals(order.getOrderStatus())) {
			log.info(oldOrderNo + "订单状态不允许废单！当前状态：{}", order.getOrderStatus());
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("订单状态不允许废单！");
			return resp;
		}
		BigDecimal orderMoney = order.getBalance();
		// 查询发行人账户清算户
		AccountInfoEntity publisherAccount = accountInfoDao.findByUserOidAndAccountTyp(req.getPublisherUserOid(),
				AccountTypeEnum.COLLECTION_SETTLEMENT.getCode());
		if (publisherAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("发行人账户归集清算户户不存在!");
			log.info("发行人账户归集清算户不存在!userOid = " + req.getPublisherUserOid());
			return resp;
		}
		// 查询用户基本户
		AccountInfoEntity userAccount = accountInfoDao.findBasicAccountByUserOid(req.getUserOid());
		if (userAccount == null) {
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			resp.setErrorMessage("用户账户不存在!");
			log.info("用户基本户不存在!userOid = " + req.getUserOid());
			return resp;
		}

		// 发行人账户清算户-balance
		int updatePublisherNumber = accountInfoDao.subtractBalance(orderMoney, publisherAccount.getAccountNo());
		if (updatePublisherNumber == 0) {
			log.error("申购撤单扣除发行人归集清算户失败！");
			throw new GHException(Integer.parseInt(Constant.BALANCELESS), "申购撤单扣除发行人归集清算户失败！");
		}
		// 用户基本户 +balance
		int updateUserNumber = accountInfoDao.addBalance(orderMoney, userAccount.getAccountNo());
		log.info("申购撤单账户更新结果：updatePublisherNumber={},updateUserNumber={}", updatePublisherNumber, updateUserNumber);
		if (updateUserNumber == 0) {
			log.error("申购撤单增加用户基本户余额失败，用户基本户余额不足！");
			throw new GHException(Integer.parseInt(Constant.BALANCELESS), "申购撤单增加用户基本户余额失败，用户基本户余额不足！");
		}
		// 查询原始申购流水记录
		TransEntity transEntity = transDao.findFirstByOrderNoAndUserOid(oldOrderNo, req.getUserOid());
		// 插入失败底层会 throw GhException
		// 插入用户交易流水
		String remark = "申购撤单";
		String orderDesc = "申购撤单记录用户基本户明细";
		BigDecimal afterBalance = userAccount.getBalance().add(orderMoney);
		accountTradeService.addTrans(transEntity, order.getOid(), userAccount.getAccountNo(),
				publisherAccount.getAccountNo(), userAccount.getUserOid(), UserTypeEnum.INVESTOR.getCode(),
				afterBalance, "01", remark, orderDesc, AccountTypeEnum.BASICER.getCode());
		// 插入发行人交易流水
		orderDesc = "申购撤单记录发行人归集清算户户明细";

		afterBalance = publisherAccount.getBalance().subtract(orderMoney);
		accountTradeService.addTrans(transEntity, order.getOid(), publisherAccount.getAccountNo(),
				userAccount.getAccountNo(), publisherAccount.getUserOid(), UserTypeEnum.PUBLISHER.getCode(),
				afterBalance, "02", remark, orderDesc, AccountTypeEnum.COLLECTION_SETTLEMENT.getCode());

		orderService.updateOrderStatus(oldOrderNo, AccOrderEntity.ORDERSTATUS_KILL);

		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");

		return resp;
	}

	/**
	 * 申购撤单参数检查
	 */
	private PurchaseTransCancelResponse checkTransCancelRequest(PurchaseTransCancelRequest req, AccOrderEntity order) {
		PurchaseTransCancelResponse resp = new PurchaseTransCancelResponse();
		resp.setReturnCode(Constant.SUCCESS);
		if (order == null) {
			resp.setErrorMessage("原订单号不存在！");
			resp.setReturnCode(Constant.ORDERNOEXISTS);
			return resp;
		}
		if (StringUtil.isEmpty(req.getPublisherUserOid())) {
			resp.setErrorMessage("发行人为空！");
			resp.setReturnCode(Constant.FAIL);
			return resp;
		}
		if (StringUtil.isEmpty(req.getUserOid())) {
			resp.setErrorMessage("投资人为空！");
			resp.setReturnCode(Constant.FAIL);
			return resp;
		}
		return resp;
	}

	/**
	 * 账户转账记录转账流水
	 * @param requestNo 请求流水号
	 * @param orderNo 订单号
	 * @param orderType 订单类型
	 * @param inputAccountNo 转入账户
	 * @param outputAccountNo 转出账户
	 * @param transferBalance 转账金额
	 * @param afterBalance 转账后金额
	 * @param orderDesc 转账订单描述
	 * @param direction 转账方向，入01，出02
	 * @param accountInfoEntity 转账账户
	 * @return 创建转账流水结果
	 */
	@Transactional
	public CreateTransResponse addTransferTrans(String requestNo, String orderNo, String orderType,
			String inputAccountNo, String outputAccountNo, BigDecimal transferBalance, BigDecimal afterBalance,
			String orderDesc, String direction, AccountInfoEntity accountInfoEntity, String eventTransNo) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		CreateTransResponse resp = new CreateTransResponse();
		resp.setReturnCode("9015");
		TransEntity entity = new TransEntity();
		entity.setAccountOid(accountInfoEntity.getAccountNo());
		entity.setUserOid(accountInfoEntity.getUserOid());
		entity.setUserType(accountInfoEntity.getUserType());
		entity.setRequestNo(requestNo);
		entity.setOrderType(orderType);
		entity.setSystemSource("mimosa");
		entity.setOrderNo(orderNo);
		entity.setDirection(direction);
		entity.setOrderBalance(transferBalance);
		entity.setRamark(OrderTypeEnum.getEnumName(orderType));
		entity.setOrderDesc(orderDesc);
		entity.setAccountName(accountInfoEntity.getAccountName());
		entity.setTransTime(time);
		entity.setBalance(afterBalance);
		// 入账，出账用户
		entity.setInputAccountNo(inputAccountNo);
		entity.setOutpuptAccountNo(outputAccountNo);
		// 财务入账标志
		entity.setCreateTime(time);
		entity.setUpdateTime(time);
		entity.setAccountType(accountInfoEntity.getAccountType());
        entity.setTransNo(eventTransNo);
		UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(entity.getUserOid());
		if (null != userInfoEntity) {
			entity.setPhone(userInfoEntity.getPhone());
		}
		Object result = transDao.save(entity);
		if (result != null) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		}
		return resp;
	}
}