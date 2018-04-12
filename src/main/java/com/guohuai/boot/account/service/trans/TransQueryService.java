package com.guohuai.boot.account.service.trans;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

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
import org.springframework.stereotype.Service;

import com.guohuai.account.api.request.AccountTransRequest;
import com.guohuai.account.api.request.TransDetailQueryRequest;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.account.api.response.TransDetailListResponse;
import com.guohuai.account.api.response.TransDetailQueryResponse;
import com.guohuai.account.api.response.entity.TransDto;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.dao.TransDao;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.entity.TransEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.service.UserInfoService;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.OrderTypeEnum;
import com.guohuai.component.util.UserTypeEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 账户交易查询服务
 * @author ZJ
 * @date 2018年1月19日 下午3:36:37
 * @version V1.0
 */
@Slf4j
@Service
public class TransQueryService {
	@Autowired
	private TransDao transDao;
	@Autowired
	private UserInfoService userInfoService;
	@Autowired
	private AccountInfoDao accountInfoDao;

	@Value("${needRechargeFrozenAccount:N}")
	private String needRechargeFrozenAccount;

	@Autowired
	private UserInfoDao userInfoDao;

	/**
	 * 交易流水查询 @Title: tansDetailQueryList @Description: @param @param
	 * req @param @return @return TransDetailListResponse @throws
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
				TransDetailQueryResponse qresp = new TransDetailQueryResponse(tempEntity);
				resp.getRows().add(qresp);
			}

		}
		return resp;

	}

	/**
	 * 判断类型是否存在 @Title: checkAccountTransRequest @Description: @param @param
	 * req @param @return @return AccountTransResponse @throws
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
}