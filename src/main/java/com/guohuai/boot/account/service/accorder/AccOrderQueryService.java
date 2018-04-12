package com.guohuai.boot.account.service.accorder;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.OrderQueryRequest;
import com.guohuai.account.api.response.AccountReconciliationDataResponse;
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
import com.guohuai.boot.account.service.UserInfoService;
import com.guohuai.boot.pay.service.ReconciliationStatisticsService;
import com.guohuai.component.util.Constant;
import com.guohuai.settlement.api.request.OrderAccountRequest;
import com.guohuai.settlement.api.response.OrderAccountResponse;
import static com.guohuai.component.util.StringUtil.nullToStr;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 账户订单查询服务
 * @author ZJ
 * @date 2018年1月19日 下午5:28:24
 * @version V1.0
 */
@Slf4j
@Component
public class AccOrderQueryService {
	@Autowired
	private AccOrderDao orderDao;
	@Autowired
	private UserInfoService userInfoService;
	@Autowired
	private UserInfoDao userInfoDao;
	@Autowired
	private ReconciliationStatisticsService reconciliationStatisticsService;

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
				} else if (!StringUtil.isEmpty(req.getPhone())) {
					UserInfoEntity userInfoEntity = userInfoService.getAccountUserByPhone(req.getPhone());
					if (null == userInfoEntity) { // 构造一个错误的userOid
						list.add(cb.equal(root.get("userOid").as(String.class), StringUtil.uuid()));
					} else {
						list.add(cb.equal(root.get("userOid").as(String.class), userInfoEntity.getUserOid()));
					}
				}

				// 订单号
				String orderNo = req.getOrderNo();
				if (!StringUtil.isEmpty(orderNo)) {
					Predicate pre = cb.equal(root.get("orderNo").as(String.class), orderNo);
					list.add(pre);
				}

				// 业务类型
				String orderType = req.getOrderType();
				if (!StringUtil.isEmpty(orderType)) {
					Predicate pre = cb.equal(root.get("orderType").as(String.class), orderType);
					list.add(pre);
				}

				// 关联产品
				String relationProductNo = req.getRelationProductNo();
				if (!StringUtil.isEmpty(relationProductNo)) {
					Predicate pre = cb.equal(root.get("relationProductNo").as(String.class), relationProductNo);
					list.add(pre);
				}

				String startTime = req.getStartTime();
				if (!StringUtil.isEmpty(startTime)) {
					Date beginDate = DateUtil.parseDate(startTime, "yyyy-MM-dd HH:mm:ss");
					list.add(cb.greaterThanOrEqualTo(root.<Date>get("submitTime"), beginDate));
				}

				String endTime = req.getEndTime();
				if (!StringUtil.isEmpty(endTime)) {
					Date endDate = DateUtil.parseDate(req.getEndTime(), "yyyy-MM-dd HH:mm:ss");
					list.add(cb.lessThanOrEqualTo(root.<Date>get("submitTime"), endDate));
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
				BeanUtils.copyProperties(entity, tempEntity, new String[] { "createTime", "updateTime" });
				tempEntity.setSubmitTime(DateUtil.format(entity.getSubmitTime(), "yyyy-MM-dd HH:mm:ss"));
				UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(tempEntity.getUserOid());
				if (null != userInfoEntity) {
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
	 * @param req
	 * @return
	 */
	public List<OrderAccountResponse> getAccountReconciliationData(OrderAccountRequest req) {
		log.info("订单对账数据获取：{}", JSONObject.toJSONString(req));
		boolean completeReconciliation = reconciliationStatisticsService
				.completeReconciliation(Timestamp.valueOf(req.getBeginTime()));
		if (completeReconciliation) {

		}
		Object[] results = null;
		if (req.getDate() != null) {
			java.util.Date beginTime = DateUtil.beginTimeInMillis(DateUtil.parseDate(req.getDate(), Constant.fomatNo));
			java.util.Date endTime = DateUtil.endTimeInMillis(DateUtil.parseDate(req.getDate(), Constant.fomatNo));
			log.info("获取一天对账数据:" + req.getDate());
			results = orderDao.getAccountOrderList(new Timestamp(beginTime.getTime()), new Timestamp(endTime.getTime()),
					req.getCountNum(), Constant.size);
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
				// 申购invest，赎回redeem，提现withdraw 可用金放款abpay，收款abcollect，充值deposit，
				// 冲正offsetPositive，冲负offsetNegative，红包redEnvelope
				// 申购:01、赎回:02、派息:03、赠送体验金:04、体验金到期:05、可用金放款:07、可用金放款:08、充值:50、提现:51、活转定:52、定转活:53、冲正：54、冲负：55、红包：56
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
				if ("".endsWith(nullToStr(ob[5])) || "null".endsWith(nullToStr(ob[5]))) {
					res.setFee(fee);
				} else {
					res.setFee((BigDecimal) ob[5]);
				}
				BigDecimal voucher = BigDecimal.ZERO;
				if ("".endsWith(nullToStr(ob[6])) || "null".endsWith(nullToStr(ob[6]))) {
					res.setVoucher(voucher);
				} else {
					res.setVoucher((BigDecimal) ob[6]);
				}
				res.setBuzzDate(
						com.guohuai.component.util.DateUtil.format(((Timestamp) ob[7]).getTime(), Constant.fomat));
				// //20170410新增产品类型，20170621去掉
				// if (ob.length > 5) {
				// res.setProductType(nullToStr(ob[6]));//产品类型06定期01活期
				// }
				// 20170621新增订单用户类型
				if (ob.length > 7) {
					String userType = nullToStr(ob[8]);
					if ("T1".equals(userType) || "T2".equals(userType)) {
						res.setUserType(userType);
					} else {
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
	 * 获取订单对账数据
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
		if (!completeReconciliation) {
			accountReconciliationDataResponse.setReturnCode(Constant.FAIL);
			accountReconciliationDataResponse.setErrorMessage("结算系统未完成对账，不能获取对账数据");
			return accountReconciliationDataResponse;
		}
		Object[] results = null;
		if (req.getDate() != null) {
			java.util.Date beginTime = DateUtil.beginTimeInMillis(DateUtil.parseDate(req.getDate(), Constant.fomatNo));
			java.util.Date endTime = DateUtil.endTimeInMillis(DateUtil.parseDate(req.getDate(), Constant.fomatNo));
			log.info("获取一天对账数据:" + req.getDate());
			results = orderDao.getAccountOrderList(new Timestamp(beginTime.getTime()), new Timestamp(endTime.getTime()),
					req.getCountNum(), Constant.size);
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
				// 申购invest，赎回redeem，提现withdraw 可用金放款abpay，收款abcollect，充值deposit，
				// 冲正offsetPositive，冲负offsetNegative，红包redEnvelope
				// 申购:01、赎回:02、派息:03、赠送体验金:04、体验金到期:05、可用金放款:07、可用金放款:08、充值:50、提现:51、活转定:52、定转活:53、冲正：54、冲负：55、红包：56、返佣
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
				} else if (PayEnum.PAYTYPE57.getCode().equals(orderType)) {
					orderType = "withdraw";
				} else if (PayEnum.PAYTYPE58.getCode().equals(orderType)) {
					orderType = "deposit";
				}
				res.setOrderType(orderType);
				res.setOrderAmount((BigDecimal) ob[4]);
				BigDecimal fee = BigDecimal.ZERO;
				if ("".endsWith(nullToStr(ob[5])) || "null".endsWith(nullToStr(ob[5]))) {
					res.setFee(fee);
				} else {
					res.setFee((BigDecimal) ob[5]);
				}
				BigDecimal voucher = BigDecimal.ZERO;
				if ("".endsWith(nullToStr(ob[6])) || "null".endsWith(nullToStr(ob[6]))) {
					res.setVoucher(voucher);
				} else {
					res.setVoucher((BigDecimal) ob[6]);
				}
				res.setBuzzDate(
						com.guohuai.component.util.DateUtil.format(((Timestamp) ob[7]).getTime(), Constant.fomat));
				// //20170410新增产品类型，20170621去掉
				// if (ob.length > 5) {
				// res.setProductType(nullToStr(ob[6]));//产品类型06定期01活期
				// }
				// 20170621新增订单用户类型
				if (ob.length > 7) {
					String userType = nullToStr(ob[8]);
					if ("T1".equals(userType) || "T2".equals(userType)) {
						res.setUserType(userType);
					} else {
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
}