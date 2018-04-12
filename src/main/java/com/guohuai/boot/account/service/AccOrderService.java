package com.guohuai.boot.account.service;


import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountBatchTransferRequest;
import com.guohuai.account.api.request.AccountTransRequest;
import com.guohuai.account.api.request.CreateOrderRequest;
import com.guohuai.account.api.request.OrderQueryRequest;
import com.guohuai.account.api.request.entity.AccountOrderDto;
import com.guohuai.account.api.response.AccountReconciliationDataResponse;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.account.api.response.CreateOrderResponse;
import com.guohuai.account.api.response.OrderListResponse;
import com.guohuai.account.api.response.OrderQueryResponse;
import com.guohuai.account.api.response.entity.AccOrderDto;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.PayEnum;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.res.AccountOrderPageResponse;
import com.guohuai.boot.account.res.AccountOrderResponse;
import com.guohuai.boot.pay.res.CreateBatchAccountOrderRes;
import com.guohuai.boot.pay.service.ReconciliationStatisticsService;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.OrderTypeEnum;
import com.guohuai.settlement.api.request.OrderAccountRequest;
import com.guohuai.settlement.api.response.OrderAccountResponse;


@Service
public class AccOrderService {
	private final static Logger log = LoggerFactory.getLogger(AccOrderService.class);
	@Autowired
	private AccOrderDao orderDao;
	@Autowired
	private UserInfoService userInfoService;
	@Autowired
	private UserInfoDao userInfoDao;
	@Autowired
	private ReconciliationStatisticsService reconciliationStatisticsService;
	@Autowired
	private AccountTransferService accountTransferService;
	
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
				orderEntity = this.getOrderByNo(req.getOrderNo());
				if (orderEntity != null) {
					//撤销订单,返回原订单
					if (Constant.KILLORDER.equals(req.getRemark())) {
						if (AccOrderEntity.ORDERSTATUS_KILL.equals(orderEntity.getOrderStatus())) {
							//已完成撤单不能再次撤单
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
					//20170329新增逻辑，提现会二次调用，第二次调用返回原订单，标记为第二次记账
					//赎回补单，订单状态为0返回成功,remark改为补单
					else if (AccOrderEntity.ORDERSTATUS_INIT.equals(orderEntity.getOrderStatus())) {
						if(OrderTypeEnum.WITHDRAWALS.getCode().equals(orderEntity.getOrderType()) || OrderTypeEnum.REDEEM.getCode().equals(orderEntity.getOrderType())){
							resp.setReturnCode(Constant.SECONDTIME);
							resp.setErrorMessage("成功");
							resp.setOrderOid(orderEntity.getOid());
							resp.setOrderNo(orderEntity.getOrderNo());
							orderEntity.setRemark(req.getRemark());//补单
							orderDao.save(orderEntity);
							return resp;
						}
						//20170401新增逻辑，提现记账失败可重新记账
					} else if (AccOrderEntity.ORDERSTATUS_FAIL.equals(orderEntity.getOrderStatus())
							&& OrderTypeEnum.WITHDRAWALS.getCode().equals(orderEntity.getOrderType())) {
						resp.setReturnCode(Constant.SECONDTIME);
						resp.setErrorMessage("成功");
						resp.setOrderOid(orderEntity.getOid());
						resp.setOrderNo(orderEntity.getOrderNo());
						return resp;
						//赎回补单，不是初始化状态，返回订单已存在
					}else if(!AccOrderEntity.ORDERSTATUS_INIT.equals(orderEntity.getOrderStatus()) && OrderTypeEnum.REDEEM.getCode().equals(orderEntity.getOrderType())){
						
						if(AccOrderEntity.ORDERSTATUS_FAIL.equals(orderEntity.getOrderStatus())){
							resp.setErrorMessage("赎回失败重发，返回成功");
							resp.setOrderOid(orderEntity.getOid());
							resp.setOrderNo(orderEntity.getOrderNo());
							resp.setRequestNo(Constant.SUCCESS);
							return resp;
						}else if(AccOrderEntity.ORDERSTATUS_SUCCESS.equals(orderEntity.getOrderStatus())){
							resp.setReturnCode(Constant.REDEEM_SUCCESSED);
							resp.setErrorMessage("赎回重发，已赎回成功");
							resp.setOrderOid(orderEntity.getOid());
							resp.setOrderNo(orderEntity.getOrderNo());
							log.info(resp.getErrorMessage()+" orderNo=" + orderEntity.getOrderNo() + "]");
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
			//入账账户
			orderEntity.setInputAccountNo(req.getInputAccountNo());
			//出账账户
			orderEntity.setOutpuptAccountNo(req.getOutpuptAccountNo());
			orderEntity.setRemark(req.getRemark());
			orderEntity.setOrderDesc(req.getOrderDesc());
			orderEntity.setUserType(req.getUserType());
			//20170301新增业务系统订单创建时间
			String orderCreatTime = req.getOrderCreatTime();//YYYY-MM-DD HH:mm:ss
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
			if(null!=userInfoEntity){
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
	 * 根据orderNo获取订单
	 */
	public AccOrderEntity getOrderByNo(String orderNo) {
		return orderDao.findByOrderNo(orderNo);
	}

	/**
	 * 根据orderNo获取订单
	 */
	public AccOrderEntity getOrderOid(String oid) {
		return orderDao.findOne(oid);
	}


	/**
	 * 查询订单
	 */
	public OrderListResponse orderQueryList(final OrderQueryRequest req) {
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
		Pageable pageable = new PageRequest(page - 1, rows, new Sort(new Order(sortDirection, sortField)));

		Specification<AccOrderEntity> spec = new Specification<AccOrderEntity>() {
			public Predicate toPredicate(Root<AccOrderEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> list = new ArrayList<Predicate>();
				// 根据用户id查询
				String userOid = req.getUserOid();
				if (!StringUtil.isEmpty(userOid)) {
					Predicate pre = cb.equal(root.get("userOid").as(String.class), userOid);
					list.add(pre);
				} else if (!StringUtil.isEmpty(req.getPhone())){
					UserInfoEntity userInfoEntity = userInfoService.getAccountUserByPhone(req.getPhone());
					if (null == userInfoEntity) { //构造一个错误的userOid
						list.add(cb.equal(root.get("userOid").as(String.class), StringUtil.uuid()));
					} else {
						list.add(cb.equal(root.get("userOid").as(String.class), userInfoEntity.getUserOid()));
					}
				}

				//订单号
				String orderNo = req.getOrderNo();
				if (!StringUtil.isEmpty(orderNo)) {
					Predicate pre = cb.equal(root.get("orderNo").as(String.class), orderNo);
					list.add(pre);
				}

				//业务类型
				String orderType = req.getOrderType();
				if (!StringUtil.isEmpty(orderType)) {
					Predicate pre = cb.equal(root.get("orderType").as(String.class), orderType);
					list.add(pre);
				}

				//关联产品
				String relationProductNo = req.getRelationProductNo();
				if (!StringUtil.isEmpty(relationProductNo)) {
					Predicate pre = cb.equal(root.get("relationProductNo").as(String.class), relationProductNo);
					list.add(pre);
				}


				String startTime = req.getStartTime();
				if (!StringUtil.isEmpty(startTime)) {
					Date beginDate = DateUtil.parseDate(startTime, "yyyy-MM-dd HH:mm:ss");
                    list.add(cb.greaterThanOrEqualTo(root.<Date>get("submitTime"),
                            beginDate));
				}

				String endTime = req.getEndTime();
				if (!StringUtil.isEmpty(endTime)) {
					Date endDate = DateUtil.parseDate(req.getEndTime(), "yyyy-MM-dd HH:mm:ss");
                    list.add(cb.lessThanOrEqualTo(root.<Date>get("submitTime"),
                            endDate));
				}

				BigDecimal sBalance = req.getSBalance();
				if (sBalance != null) {
					list.add(cb.greaterThanOrEqualTo(root.get("balance").as(BigDecimal.class), sBalance));
				}

				BigDecimal eBalance = req.getEBalance();
				if (eBalance != null) {
					list.add(cb.lessThanOrEqualTo(root.get("balance").as(BigDecimal.class), eBalance));
				}

				Predicate[] p = new Predicate[list.size()];
				return cb.and(list.toArray(p));
			}
		};


		Page<AccOrderEntity> result = orderDao.findAll(spec, pageable);

		OrderListResponse resp = new OrderListResponse();
		if (null != result && result.getTotalElements() != 0) {
			resp.setTotal(result.getTotalElements());
			for (AccOrderEntity entity : result.getContent()) {
				AccOrderDto tempEntity = new AccOrderDto();
				BeanUtils.copyProperties(entity, tempEntity, new String[]{"createTime", "updateTime"});
				tempEntity.setSubmitTime(DateUtil.format(entity.getSubmitTime(), "yyyy-MM-dd HH:mm:ss"));
				UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(tempEntity.getUserOid());
				if(null!=userInfoEntity) {
					tempEntity.setPhone(userInfoEntity.getPhone());
				}
				OrderQueryResponse qresp = new OrderQueryResponse(tempEntity);
				resp.getRows().add(qresp);
			}

		}
		return resp;
	}

	/**
	 * 获取订单对账数据
	 *
	 * @param req
	 * @return
	 */
	public List<OrderAccountResponse> getAccountReconciliationData(OrderAccountRequest req) {
		log.info("订单对账数据获取：{}", JSONObject.toJSONString(req));
		boolean completeReconciliation = reconciliationStatisticsService
				.completeReconciliation(Timestamp.valueOf(req.getBeginTime()));
		if(completeReconciliation){
			
		}
		Object[] results = null;
		if (req.getDate() != null) {
			java.util.Date beginTime = DateUtil.beginTimeInMillis(DateUtil.parseDate(req.getDate(), Constant.fomatNo));
			java.util.Date endTime = DateUtil.endTimeInMillis(DateUtil.parseDate(req.getDate(), Constant.fomatNo));
			log.info("获取一天对账数据:" + req.getDate());
			results = orderDao.getAccountOrderList(new Timestamp(beginTime.getTime()),
					new Timestamp(endTime.getTime()), req.getCountNum(), Constant.size);
		} else {
			Timestamp beginTime = Timestamp.valueOf(req.getBeginTime());
			Timestamp endTime = Timestamp.valueOf(req.getEndTime());
			log.info("获取对账数据日期区间:" + "beginTime=" + beginTime + " endTime=" + endTime);
			results = orderDao.getAccountOrderList(beginTime, endTime, req.getCountNum(), Constant.size);
		}
		if (results != null && results.length != 0) {
			List<OrderAccountResponse> listAccounts = new ArrayList<>();
			OrderAccountResponse res = null;
			for (Object result : results) {
				res = new OrderAccountResponse();
				Object[] ob = (Object[]) result;
				String userOid = nullToStr(ob[0]);
				res.setInvestorOid(userOid);
				res.setUserOid(userOid);
				res.setOrderCode(nullToStr(ob[1]));
				res.setOrderStatus(nullToStr(ob[2]));
				//申购invest，赎回redeem，提现withdraw 可用金放款abpay，收款abcollect，充值deposit， 冲正offsetPositive，冲负offsetNegative，红包redEnvelope
				//申购:01、赎回:02、派息:03、赠送体验金:04、体验金到期:05、可用金放款:07、可用金放款:08、充值:50、提现:51、活转定:52、定转活:53、冲正：54、冲负：55、红包：56
				String orderType = nullToStr(ob[3]);
				if (PayEnum.PAYTYPE01.getCode().equals(orderType)) {
					orderType = "invest";
				} else if (PayEnum.PAYTYPE02.getCode().equals(orderType)) {
					orderType = "redeem";
				} else if (PayEnum.PAYTYPE50.getCode().equals(orderType)) {
					orderType = "deposit";
				} else if (PayEnum.PAYTYPE51.getCode().equals(orderType)) {
					orderType = "withdraw";
				} else if (PayEnum.PAYTYPE54.getCode().equals(orderType)) {
					orderType = "offsetPositive";
				} else if (PayEnum.PAYTYPE55.getCode().equals(orderType)) {
					orderType = "offsetNegative";
				} else if (PayEnum.PAYTYPE56.getCode().equals(orderType)) {
					orderType = "redEnvelope";
				} else if (PayEnum.PAYTYPE07.getCode().equals(orderType)) {
					orderType = "abpay";
				} else if (PayEnum.PAYTYPE08.getCode().equals(orderType)) {
					orderType = "abcollect";
				}
				res.setOrderType(orderType);
				res.setOrderAmount((BigDecimal) ob[4]);
				BigDecimal fee = BigDecimal.ZERO;
				if("".endsWith(nullToStr(ob[5]))||"null".endsWith(nullToStr(ob[5]))){
					res.setFee(fee);
				}else{
					res.setFee((BigDecimal) ob[5]);
				}
				BigDecimal voucher = BigDecimal.ZERO;
				if("".endsWith(nullToStr(ob[6]))||"null".endsWith(nullToStr(ob[6]))){
					res.setVoucher(voucher);
				}else{
					res.setVoucher((BigDecimal) ob[6]);
				}
				res.setBuzzDate(com.guohuai.component.util.DateUtil.format(((Timestamp) ob[7]).getTime(), Constant.fomat));
//				//20170410新增产品类型，20170621去掉
//				if (ob.length > 5) {
//					res.setProductType(nullToStr(ob[6]));//产品类型06定期01活期
//				}
				//20170621新增订单用户类型
				if (ob.length > 7) {
					String userType = nullToStr(ob[8]);
					if("T1".equals(userType)||"T2".equals(userType)){
						res.setUserType(userType);
					}else{
						res.setUserType("T1");
					}
					
				}
				res.setUserPhone(nullToStr(ob[9]));
				res.setUserName(nullToStr(ob[10]));

				if (req.getCountNum() == 0) {
					res.setCountNum(Long.valueOf(results.length));
				} else {
					res.setCountNum(Long.valueOf((Long.valueOf(results.length) + Long.valueOf(req.getCountNum()))));
				}

				listAccounts.add(res);
			}
			return listAccounts;
		}
		return null;
	}

	String nullToStr(Object str) {
		if (null == str) {
			return "";
		}
		return str.toString();
	}

	public OrderQueryResponse getOrderByNoResp(String orderCode) {
		log.info("账户订单查询：" + orderCode);
		AccOrderDto dto = new AccOrderDto();
		AccOrderEntity entity = orderDao.findOrderByOrderNoInOrderType(orderCode);
		if (entity != null) {
			dto.setBalance(entity.getBalance());
			dto.setBusinessStatus(entity.getBusinessStatus());
			dto.setCreateTime(entity.getCreateTime().toString());
			dto.setFinanceStatus(entity.getFinanceStatus());
			dto.setInputAccountNo(entity.getInputAccountNo());
			dto.setOrderDesc(entity.getOrderDesc());
			dto.setOrderNo(entity.getOrderNo());
			dto.setOrderStatus(entity.getOrderStatus());
			dto.setOrderType(entity.getOrderType());
			dto.setOutpuptAccountNo(entity.getOutpuptAccountNo());
			dto.setReceiveTime(entity.getReceiveTime().toString());
			dto.setRelationProductNo(entity.getRelationProductNo());
			dto.setRelationProductName(entity.getRelationProductName());
			dto.setRemark(entity.getRemark());
			dto.setRequestNo(entity.getRequestNo());
			dto.setSubmitTime(entity.getSubmitTime().toString());
			dto.setSystemSource(entity.getSystemSource());
			dto.setUpdateTime(entity.getUpdateTime().toString());
			dto.setUserOid(entity.getUserOid());
		}
		OrderQueryResponse orderQueryResponse = new OrderQueryResponse(dto);
		if (entity != null) {
			orderQueryResponse.setReturnCode("0000");
		} else {
			orderQueryResponse.setReturnCode("9999");
			orderQueryResponse.setErrorMessage("该订单不存在");
		}
		return orderQueryResponse;
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
				orderEntity = this.getOrderByNo(req.getOrderNo());
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
				orderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_FAIL);//失败
				//入账账户
				orderEntity.setInputAccountNo(req.getInputAccountNo());
				//出账账户
				orderEntity.setOutpuptAccountNo(req.getOutpuptAccountNo());
				orderEntity.setRemark(req.getRemark());
				orderEntity.setOrderDesc(req.getOrderDesc());
				orderEntity.setUserType(req.getUserType());
				//20170301新增业务系统订单创建时间
				String orderCreatTime = req.getOrderCreatTime();//YYYY-MM-DD HH:mm:ss
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
	 * 获取订单对账数据
	 *
	 * @param req
	 * @return
	 */
	public AccountReconciliationDataResponse getAccountAlreadyReconciliationData(OrderAccountRequest req) {
		log.info("订单对账数据获取：{}", JSONObject.toJSONString(req));
		AccountReconciliationDataResponse accountReconciliationDataResponse = new AccountReconciliationDataResponse();
		boolean completeReconciliation = false;
		accountReconciliationDataResponse.setReturnCode(Constant.SUCCESS);
		accountReconciliationDataResponse.setErrorMessage("获取对账数据成功");
		completeReconciliation = reconciliationStatisticsService
				.completeReconciliation(Timestamp.valueOf(req.getBeginTime()));
		if(!completeReconciliation){
			accountReconciliationDataResponse.setReturnCode(Constant.FAIL);
			accountReconciliationDataResponse.setErrorMessage("结算系统未完成对账，不能获取对账数据");
			return accountReconciliationDataResponse;
		}
		Object[] results = null;
		if (req.getDate() != null) {
			java.util.Date beginTime = DateUtil.beginTimeInMillis(DateUtil.parseDate(req.getDate(), Constant.fomatNo));
			java.util.Date endTime = DateUtil.endTimeInMillis(DateUtil.parseDate(req.getDate(), Constant.fomatNo));
			log.info("获取一天对账数据:" + req.getDate());
			results = orderDao.getAccountOrderList(new Timestamp(beginTime.getTime()),
					new Timestamp(endTime.getTime()), req.getCountNum(), Constant.size);
		} else {
			Timestamp beginTime = Timestamp.valueOf(req.getBeginTime());
			Timestamp endTime = Timestamp.valueOf(req.getEndTime());
			log.info("获取对账数据日期区间:" + "beginTime=" + beginTime + " endTime=" + endTime);
			results = orderDao.getAccountOrderList(beginTime, endTime, req.getCountNum(), Constant.size);
		}
		if (results != null && results.length != 0) {
			List<OrderAccountResponse> listAccounts = new ArrayList<>();
			OrderAccountResponse res = null;
			for (Object result : results) {
				res = new OrderAccountResponse();
				Object[] ob = (Object[]) result;
				String userOid = nullToStr(ob[0]);
				res.setInvestorOid(userOid);
				res.setUserOid(userOid);
				res.setOrderCode(nullToStr(ob[1]));
				res.setOrderStatus(nullToStr(ob[2]));
				//申购invest，赎回redeem，提现withdraw 可用金放款abpay，收款abcollect，充值deposit， 冲正offsetPositive，冲负offsetNegative，红包redEnvelope
				//申购:01、赎回:02、派息:03、赠送体验金:04、体验金到期:05、可用金放款:07、可用金放款:08、充值:50、提现:51、活转定:52、定转活:53、冲正：54、冲负：55、红包：56、返佣
				String orderType = nullToStr(ob[3]);
				res.setOrderType(orderType);
				res.setOrderAmount((BigDecimal) ob[4]);
				BigDecimal fee = BigDecimal.ZERO;
				if("".endsWith(nullToStr(ob[5]))||"null".endsWith(nullToStr(ob[5]))){
					res.setFee(fee);
				}else{
					res.setFee((BigDecimal) ob[5]);
				}
				BigDecimal voucher = BigDecimal.ZERO;
				if("".endsWith(nullToStr(ob[6]))||"null".endsWith(nullToStr(ob[6]))){
					res.setVoucher(voucher);
				}else{
					res.setVoucher((BigDecimal) ob[6]);
				}
				res.setBuzzDate(com.guohuai.component.util.DateUtil.format(((Timestamp) ob[7]).getTime(), Constant.fomat));
//				//20170410新增产品类型，20170621去掉
//				if (ob.length > 5) {
//					res.setProductType(nullToStr(ob[6]));//产品类型06定期01活期
//				}
				//20170621新增订单用户类型
				if (ob.length > 7) {
					String userType = nullToStr(ob[8]);
					if("T1".equals(userType)||"T2".equals(userType)||"T3".equals(userType)){
						res.setUserType(userType);
					}else{
						res.setUserType("T1");
					}
					
				}
				res.setUserPhone(nullToStr(ob[9]));
				res.setUserName(nullToStr(ob[10]));

				if (req.getCountNum() == 0) {
					res.setCountNum(Long.valueOf(results.length));
				} else {
					res.setCountNum(Long.valueOf((Long.valueOf(results.length) + Long.valueOf(req.getCountNum()))));
				}

				listAccounts.add(res);
			}
			accountReconciliationDataResponse.setOrderList(listAccounts);
		}
		return accountReconciliationDataResponse;
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
		orderEntity = this.getOrderByNo(orderNo);
		if (orderEntity != null) {
			if(AccOrderEntity.ORDERSTATUS_SUCCESS.equals(orderEntity.getOrderStatus())){
				resp.setReturnCode(Constant.ORDEREXISTS);
				resp.setErrorMessage("订单已存在且已成功");
				log.error("订单已存在，订单号:{}",orderEntity);
				return resp;
			}else{
				log.info("订单已存在{}，更新订单时间{}", orderEntity, req.getOrderCreatTime());
				String orderCreatTime = req.getOrderCreatTime();//YYYY-MM-DD HH:mm:ss
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
		//入账账户
		orderEntity.setInputAccountNo(req.getInputAccountNo());
		//出账账户
		orderEntity.setOutpuptAccountNo(req.getOutpuptAccountNo());
		orderEntity.setRemark(req.getRemark());
		orderEntity.setOrderDesc(req.getOrderDesc());
		orderEntity.setUserType(req.getUserType());
		String orderCreatTime = req.getOrderCreatTime();//YYYY-MM-DD HH:mm:ss
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
		if(null!=userInfoEntity){
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
	 * @param req 批量订单请求参数
	 * @param resp 已存在返参
	 * @return 批量账户订单
	 */
	@Transactional
	public CreateBatchAccountOrderRes creatBatchAccountOrder(AccountBatchTransferRequest req) {
		CreateBatchAccountOrderRes createBatchAccountOrderRes = new CreateBatchAccountOrderRes();
		String outputUserOid = req.getOutputUserOid();
		String requestNo = req.getRequestNo();
		//请求批次号流水号
		if(requestNo != null){
			int requestCount = 0;
			requestCount = orderDao.finRequestCountByRequestNo(requestNo);
			if(requestCount >0){
				createBatchAccountOrderRes.setReturnCode(Constant.REQUESTNOEXISTS);
				createBatchAccountOrderRes.setErrorMessage("批量转账订单，批次请求流水号已存在");
				log.info("批量转账订单，批次请求流水号已存在!");
				return createBatchAccountOrderRes;
			}
		}else{
			createBatchAccountOrderRes.setReturnCode(Constant.REQUESTNO_IS_NULL);
			createBatchAccountOrderRes.setErrorMessage("发批量转账订单，批次请求流水号不能为空");
			log.info("批量转账订单，批次请求流水号不能为空!!");
			return createBatchAccountOrderRes;
		}
		//保存订单
		if(req.getOrderList() != null&&req.getOrderList().size()>0){
			//批量保存订单
			createBatchAccountOrderRes = this.saveBatchOrder(req.getOrderList(), 
					requestNo, outputUserOid, req.getSystemSource());
		}else{
			createBatchAccountOrderRes.setReturnCode(Constant.BATCH_REDEEM_ORDER_IS_NULL);
			createBatchAccountOrderRes.setErrorMessage("批量转账订单不存在");
			log.info("批量转账订单不存在!!");
			return createBatchAccountOrderRes;
		}
		return createBatchAccountOrderRes;
	}

	/**
	 * 保存批量转账订单
	 * @param orderList 批量订单
	 * @param requestNo 请求批次号
	 * @param outputUserOid 转出人id
	 * @param systemSource 系统来源
	 * @return
	 */
	@Transactional
	private CreateBatchAccountOrderRes saveBatchOrder(
			List<AccountOrderDto> orderList, String requestNo,
			String outputUserOid, String systemSource) {
		CreateBatchAccountOrderRes resp = new CreateBatchAccountOrderRes();
		List<AccOrderEntity> accOrderEntityList = new ArrayList<AccOrderEntity>();
		Timestamp time = new Timestamp(System.currentTimeMillis());
		BigDecimal orderBalance = BigDecimal.ZERO;//订单总金额
		for(AccountOrderDto dto : orderList){
			AccOrderEntity orderEntity = null;
			orderEntity = this.getOrderByNo(dto.getOrderNo());
			if (orderEntity != null) {
				if(AccOrderEntity.ORDERSTATUS_SUCCESS.equals(orderEntity.getOrderStatus())){
					resp.setReturnCode(Constant.ORDEREXISTS);
					resp.setErrorMessage("订单已存在且已成功");
					log.error("订单已存在，订单号:{}",orderEntity);
					//回调业务订单已成功
					accountTransferService.callBack(orderEntity);
					continue;
				}
			}
			String phone = "";
			if(dto.getUserOid() != null){
				//查询用户信息
				UserInfoEntity userInfo = userInfoService.getAccountUserByUserOid(dto.getUserOid());
				if (userInfo != null) {
					phone = userInfo.getPhone();
				}
			}
			if(orderEntity == null){
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
			if(StringUtil.isEmpty(dto.getUserType())){
				orderEntity.setUserType("T1");
			}else{
				orderEntity.setUserType(dto.getUserType());
			}
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
	
	/**
	 * 创建并保存订单
	 * @param req 交易请求参数
	 * @return 创建结果
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AccountTransResponse saveAccountOrder(CreateOrderRequest req) {
		AccountTransResponse transResp = new AccountTransResponse();
		AccOrderEntity orderEntity = null;
		// 查询订单是否已存在
		orderEntity = orderDao.findByOrderNo(req.getOrderNo());
		if(orderEntity != null){
			// 若存在订单并且为处理中，不再再次处理该订单
			if(AccOrderEntity.ORDERSTATUS_INIT.equals(orderEntity.getOrderStatus())){
				transResp.setReturnCode(Constant.FAIL);
				transResp.setErrorMessage("订单正在处理中，请联系运维人员");
				log.error("订单正在处理中，请联系运维人员");
				return transResp;
			}
			// 若存在订单并且为成功，不再再次处理该订单
			if(AccOrderEntity.ORDERSTATUS_SUCCESS.equals(orderEntity.getOrderStatus())){
				transResp.setReturnCode(Constant.ACCOUNT_ORDER_EXISTS);
				transResp.setErrorMessage("订单已处理成功，无需再次处理，请联系运维人员");
				log.error("订单已处理成功，无需再次处理，请联系运维人员");
				return transResp;
			}
			// 若存在订单并且为失败，再次处理该订单
			if(AccOrderEntity.ORDERSTATUS_FAIL.equals(orderEntity.getOrderStatus())){
				log.info("订单已存在且订单为失败订单，可再次处理转账，不需再次保存订单，更新原订单信息");
				//先将订单更新为处理中
				int updateCount = orderDao.updateOrderStatusByStatus(req.getOrderNo(),
						AccOrderEntity.ORDERSTATUS_INIT, AccOrderEntity.ORDERSTATUS_FAIL);
				log.info("更新订单状态结果：{}",updateCount);
				if(updateCount < 1){
					transResp.setReturnCode(Constant.FAIL);
					transResp.setErrorMessage("订单正在处理中，请联系运维人员");
					log.error("订单正在处理中，请联系运维人员");
					return transResp;
				}
			}
		}else{
			orderEntity = new AccOrderEntity();
		}
		
		Timestamp time = new Timestamp(System.currentTimeMillis());
		orderEntity.setRequestNo(req.getRequestNo());
		orderEntity.setSystemSource(req.getSystemSource());
		orderEntity.setOrderNo(req.getOrderNo());
		orderEntity.setUserOid(req.getUserOid());
		orderEntity.setPublisherUserOid(req.getPublisherUserOid());
		orderEntity.setOrderType(req.getOrderType());
		orderEntity.setBalance(req.getBalance());
		orderEntity.setVoucher(req.getVoucher());
		orderEntity.setOrderStatus(AccOrderEntity.ORDERSTATUS_INIT);
		orderEntity.setRemark(req.getRemark());
		orderEntity.setOrderDesc(req.getOrderDesc());
		String orderCreatTime = req.getOrderCreatTime();// YYYY-MM-DD hh:mm:ss
		if (orderCreatTime != null) {
			orderEntity.setSubmitTime(Timestamp.valueOf(orderCreatTime));
		}
		orderEntity.setFee(req.getFee());
		orderEntity.setFrozenBalance(req.getFrozenBalance());
		orderEntity.setContinUnfreezBalance(req.getContinUnfreezBalance());
		orderEntity.setRateBalance(req.getRateBalance());
		orderEntity.setRelationProductName(req.getRelationProductName());
		orderEntity.setReceiveTime(time);
		orderEntity.setUpdateTime(time);
		if (null == orderEntity.getCreateTime()) {
			orderEntity.setCreateTime(time);
		}
		UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(orderEntity.getUserOid());
		if (null != userInfoEntity) {
			orderEntity.setPhone(userInfoEntity.getPhone());
			orderEntity.setUserType(userInfoEntity.getUserType());
		}
		orderEntity = orderDao.save(orderEntity);
		if(orderEntity != null){
			transResp.setReturnCode(Constant.SUCCESS);
			transResp.setErrorMessage("新增订单成功");
		}else{
			transResp.setReturnCode(Constant.FAIL);
			transResp.setErrorMessage("新增订单失败");
		}
		return transResp;
	}
	
	
	/**
	 * 订单分页查询
	 * @param req 查询参数
	 * @return 查询结果
	 */
	public AccountOrderPageResponse page(final OrderQueryRequest req) {
		log.info("订单分页查询,请求参数{},",JSONObject.toJSONString(req));
		Page<AccOrderEntity> listPage = orderDao.findAll(buildSpecification(req), 
				new PageRequest(req.getPage() - 1, req.getRows()));
		AccountOrderPageResponse res =new AccountOrderPageResponse();
		if (listPage != null && listPage.getSize() > 0) {
			res.setRows(listPage.getContent());
			res.setTotalPage(listPage.getTotalPages());
			res.setPage(req.getPage());
			res.setRow(req.getRows());
			res.setTotal(listPage.getTotalElements());
			return res;
		}
		return res;
	}
	
	/**
	 * 组装订单查询参数
	 * @param req 查询参数
	 * @return 组装参数
	 */
	public Specification<AccOrderEntity> buildSpecification(final OrderQueryRequest req) {
		Specification<AccOrderEntity> spec = new Specification<AccOrderEntity>() {
			@Override
			public Predicate toPredicate(Root<AccOrderEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> bigList =new ArrayList<Predicate>();
				if (!StringUtil.isEmpty(req.getOrderType()))//订单类型
					bigList.add(cb.equal(root.get("orderType").as(String.class),req.getOrderType()));
				if (!StringUtil.isEmpty(req.getOrderNo()))//登账事件名称
					bigList.add(cb.equal(root.get("orderNo").as(String.class),req.getOrderNo()));
				if (!StringUtil.isEmpty(req.getStartTime()))
					bigList.add(cb.greaterThanOrEqualTo(root.get("submitTime").as(String.class),req.getStartTime()));
				if (!StringUtil.isEmpty(req.getEndTime()))
					bigList.add(cb.lessThanOrEqualTo(root.get("submitTime").as(String.class),req.getEndTime()));
				
				query.where(cb.and(bigList.toArray(new Predicate[bigList.size()])));
				query.orderBy(cb.desc(root.get("submitTime")));
				// 条件查询
				return query.getRestriction();
			}
		};
		return spec;
	}
	
	/**
	 * 根据订单号查询订单详情
	 * @param orderNo
	 * @return
	 */
	public AccountOrderResponse findAccOrderDetails(String orderNo){
		AccOrderEntity entity = orderDao.findByOrderNo(orderNo);
		AccountOrderResponse resp = new AccountOrderResponse();
		BeanUtils.copyProperties(entity, resp);
		if(!StringUtil.isEmpty(resp.getUserOid())){
			UserInfoEntity user = userInfoService.getAccountUserByUserOid(resp.getUserOid());
			if(null != user){
				resp.setUserName(user.getName());
				resp.setUserType(user.getUserType());
				resp.setPhone(user.getPhone());
			}
		}
		return resp;
	}
	
}