package com.guohuai.boot.account.service.accountinfo;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
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
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountQueryRequest;
import com.guohuai.account.api.request.CreateAccountRequest;
import com.guohuai.account.api.response.AccountBalanceResponse;
import com.guohuai.account.api.response.AccountListResponse;
import com.guohuai.account.api.response.AccountQueryResponse;
import com.guohuai.account.api.response.CreateAccountResponse;
import com.guohuai.account.api.response.FinanceAccountResp;
import com.guohuai.account.api.response.PublisherAccountBalanceResponse;
import com.guohuai.account.api.response.entity.AccountInfoDto;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.basic.component.ext.web.PageResp;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.service.UserInfoService;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.CalculationUtil;
import com.guohuai.component.util.CodeConstants;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.UserTypeEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 账户信息查询服务
 * @author ZJ
 * @date 2018年1月19日 下午3:14:09
 * @version V1.0
 */
@Slf4j
@Service
public class AccountInfoQueryService {
	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private UserInfoDao userInfoDao;
	@Autowired
	private UserInfoService userInfoService;
	@Autowired
	private SeqGenerator seqGenerator;

	@Value("${needRechargeFrozenAccount:N}")
	private String needRechargeFrozenAccount;

	/**
	 * 检查类型是否存在
	 */
	public CreateAccountResponse checkAccountType(CreateAccountRequest req) {
		CreateAccountResponse resp = new CreateAccountResponse();
		resp.setReturnCode(Constant.SUCCESS);

		if (StringUtil.isEmpty(req.getUserOid())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("userOid不能为空!");
			return resp;
		}
		// 判断用户是否存在
		UserInfoEntity userInfo = userInfoService.getAccountUserByUserOid(req.getUserOid());
		if (userInfo == null) {
			resp.setReturnCode(Constant.USERNOTEXISTS);
			resp.setErrorMessage("用户不存在!");
			log.error("用户不存在![userOid=" + req.getUserOid() + "]");
			return resp;
		}
		if (StringUtil.isEmpty(UserTypeEnum.getEnumName(req.getUserType()))) {
			// 用户类型不存在
			resp.setReturnCode(Constant.USERTYPENOTEXISTS);
			resp.setErrorMessage("用户类型不存在！");
			log.error("用户类型不存在，[userType=" + req.getUserType() + "]");
			return resp;
		}
		if (StringUtil.isEmpty(AccountTypeEnum.getEnumName(req.getAccountType()))) {
			// 账户类型不存在
			resp.setReturnCode(Constant.ACCOUNTTYPENOTEXISTS);
			resp.setErrorMessage("账户类型不存在！");
			log.error("账户类型不存在，[accountType=" + req.getAccountType() + "]");
			return resp;
		}

		// if(UserTypeEnum.INVESTOR.getCode().equals(req.getUserType())) {
		// if(StringUtil.isEmpty(req.getRelationProduct())){
		// //账户类型不存在
		// resp.setReturnCode(Constant.RelationProductNotNULL);
		// resp.setErrorMessage("关联产品不能为空");
		// log.error("关联产品不能为空，{}",JSONObject.toJSON(req));
		// return resp;
		// }
		// }

		return resp;
	}
	
	/**
	 * 查询账户，如果不存在则新建一个账号
	 * @param userOid
	 * @param accountType
	 * @param userType
	 * @return
	 */
	public AccountInfoEntity findAccountOrCreate(String userOid, String accountType, String userType) {
		log.info("新增账户:[userOid={}, accountType={}, userType={}]", userOid, accountType, userType);
		CreateAccountResponse resp = new CreateAccountResponse();
		AccountInfoEntity accountInfo = accountInfoDao.findByUserOidAndAccountTypeAndUserType(userOid, userType,
				accountType);
		if (null == accountInfo) {
			Timestamp nowTime = new Timestamp(System.currentTimeMillis());
			AccountInfoEntity account = new AccountInfoEntity();
			account.setOid(StringUtil.uuid());
			account.setUserOid(userOid);
			account.setUserType(userType);
			account.setAccountType(accountType);
			account.setAccountName(UserTypeEnum.getEnumName(userType) + AccountTypeEnum.getEnumName(accountType));
			String accountNo = this.seqGenerator.next(CodeConstants.ACCOUNT_NO_PREFIX);
			account.setAccountNo(accountNo);
			account.setOpenTime(nowTime);
			account.setBalance(BigDecimal.ZERO);
			account.setLineOfCredit(BigDecimal.ZERO);
			account.setStatus(AccountInfoEntity.AUDIT_STATUS_SUBMIT);
			account.setAuditStatus(AccountInfoEntity.AUDIT_STATUS_SUBMIT);
			account.setFrozenStatus(AccountInfoEntity.FROZENSTATUS_NORMAL);
			account.setCreateTime(nowTime);
			account.setUpdateTime(nowTime);
			log.info("保存用户账户信息{}", JSONObject.toJSONString(account));
			account = accountInfoDao.save(account);
			log.info("保存用户账户信息完成  CreateAccountResponse:{}", JSONObject.toJSON(resp));
			return account;
		}
		return accountInfo;
	}
	
	/**
	 * 账户查询接口
	 * @param req
	 * @return
	 */
	public AccountListResponse accountQueryList(final AccountQueryRequest req) {
		int page = req.getPage();
		int rows = req.getRows();
		if (page < 1) {
			page = 1;
		}
		if (rows < 1) {
			rows = 1;
		}

		Direction sortDirection = Direction.DESC;
		if (!"desc".equals(req.getSort())) {
			sortDirection = Direction.ASC;
		}

		String sortField = req.getSortField();
		if (StringUtil.isEmpty(sortField)) {
			sortField = "createTime";
		}

		Specification<AccountInfoEntity> spec = new Specification<AccountInfoEntity>() {
			public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				List<Predicate> list = new ArrayList<Predicate>();
				// 根据用户id查询
				String userOid = req.getUserOid();
				if (!StringUtil.isEmpty(userOid)) {
					Predicate pre = cb.equal(root.get("userOid").as(String.class), userOid);
					list.add(pre);
				}
				// 用户类型
				String userType = req.getUserType();
				if (!StringUtil.isEmpty(userType)) {
					Predicate pre = cb.equal(root.get("userType").as(String.class), userType);
					list.add(pre);
				}
				// 账户类型
				String accountType = req.getAccountType();
				if (!StringUtil.isEmpty(userType)) {
					Predicate pre = cb.equal(root.get("accountType").as(String.class), accountType);
					list.add(pre);
				}
				// 关联产品
				// String relationProductName = req.getRelationProductName();
				// if(!StringUtil.isEmpty(relationProductName)){
				// Predicate pre = cb.equal(root.get("relationProductName").as(String.class),
				// relationProductName);
				// list.add(pre);
				// }

				Predicate[] p = new Predicate[list.size()];
				return cb.and(list.toArray(p));
			}
		};

		Pageable pageable = new PageRequest(page - 1, rows, new Sort(new Order(sortDirection, sortField)));

		Page<AccountInfoEntity> result = accountInfoDao.findAll(spec, pageable);
		AccountListResponse resp = new AccountListResponse();

		if (null != result && result.getTotalElements() != 0) {
			resp.setTotal(result.getTotalElements());
			for (AccountInfoEntity entity : result.getContent()) {
				AccountInfoDto tempEntity = new AccountInfoDto();
				BeanUtils.copyProperties(entity, tempEntity, new String[] { "createTime", "updateTime" });
				tempEntity.setCreateTime(DateUtil.format(entity.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
				AccountQueryResponse qresp = new AccountQueryResponse(tempEntity);
				resp.getRows().add(qresp);
			}

		}
		return resp;
	}

	/**
	 * 根据账户号获取账户
	 * @param accountNo
	 * @return
	 */
	public AccountInfoEntity getAccountByNo(String accountNo) {
		AccountInfoEntity account = accountInfoDao.findByAccountNo(accountNo);
		return account;
	}

	public FinanceAccountResp read(String oid) {
		log.info("获取账户详情,oid:{}", oid);
		AccountInfoEntity accountEntity = this.accountInfoDao.findOne(oid);
		log.info("获取账户详情,UserInfoEntity:{}", JSON.toJSONString(accountEntity));
		FinanceAccountResp far = userEntityTranforToResp(accountEntity);
		UserInfoEntity u = userInfoService.getAccountUserByUserOid(accountEntity.getUserOid());
		if (u != null) {
			far.setUserName(u.getName());
			far.setPhone(u.getPhone());
		}
		far.setReturnCode(Constant.SUCCESS);
		log.info("转化后账户详情,FinanceAccountResp:{}", JSON.toJSONString(far));

		return far;
	}

	private FinanceAccountResp userEntityTranforToResp(AccountInfoEntity accountEntity) {
		FinanceAccountResp far = new FinanceAccountResp();
		far.setOid(accountEntity.getOid());
		far.setUserType(accountEntity.getUserType());
		far.setUserOid(accountEntity.getUserOid());
		far.setAccountNo(accountEntity.getAccountNo());
		far.setAccountType(accountEntity.getAccountType());
		far.setRelationProduct(accountEntity.getRelationProduct());
		far.setRelationProductName(accountEntity.getRelationProductName());
		far.setAccountName(accountEntity.getAccountName());
		far.setOpenTime(accountEntity.getOpenTime());
		far.setBalance(accountEntity.getBalance());
		far.setStatus(accountEntity.getStatus());
		far.setFrozenStatus(accountEntity.getFrozenStatus());
		far.setAuditStatus(accountEntity.getAuditStatus());
		far.setRemark(accountEntity.getRemark()); // 备注
		far.setUpdateTime(accountEntity.getUpdateTime()); // 更新时间
		far.setCreateTime(accountEntity.getCreateTime()); // 创建时间
		far.setLineOfCredit(accountEntity.getLineOfCredit());// 授信额度
		return far;
	}

	/**
	 * 获取账户列表
	 * @param spec
	 * @param pageable
	 * @return 如果返回的errCode属性等于0表示成功，否则表示失败，失败原因在errMessage里面体现
	 */
	public PageResp<FinanceAccountResp> list(Specification<AccountInfoEntity> spec, Pageable pageable,
			final String phone, Specification<AccountInfoEntity> filterSpec) {
		log.info("获取账户列表,spec:{},pageable:{},phone:{}",
				new Object[] { JSON.toJSONString(spec), JSON.toJSONString(pageable) }, phone);
		PageResp<FinanceAccountResp> pagesRep = new PageResp<FinanceAccountResp>();

		final Map<String, UserInfoEntity> upmap = new HashMap<String, UserInfoEntity>();
		if (!StringUtil.isEmpty(phone)) {
			final List<UserInfoEntity> us = this.userInfoService.getUsers(phone);
			log.info("根据手机号查账户列表,us:{}", JSON.toJSONString(us));
			if (us != null && us.size() > 0) {

				// 查询标签
				Specification<AccountInfoEntity> uspec = new Specification<AccountInfoEntity>() {
					@Override
					public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query,
							CriteriaBuilder cb) {
						In<String> inUserOids = cb.in(root.get("userOid").as(String.class));
						for (UserInfoEntity u : us) {
							inUserOids.value(u.getUserOid());
							upmap.put(u.getUserOid(), u);
						}
						return inUserOids;
					}
				};
				spec = Specifications.where(uspec).and(filterSpec);

			} else {
				return pagesRep;
			}
		}
		Page<AccountInfoEntity> cas = this.accountInfoDao.findAll(spec, pageable);
		if (cas != null && cas.getContent() != null && cas.getTotalElements() > 0) {
			Map<String, UserInfoEntity> umap = null;
			if (!StringUtil.isEmpty(phone)) {
				umap = upmap;
			} else {
				Set<String> userOids = new HashSet<String>();
				for (AccountInfoEntity a : cas) {
					userOids.add(a.getUserOid());
				}
				umap = this.userInfoService.findUsers(userOids);
			}

			UserInfoEntity u = null;
			List<FinanceAccountResp> rows = new ArrayList<FinanceAccountResp>();
			Object[] users = userInfoDao.findAllUserName();
			for (AccountInfoEntity a : cas) {
				FinanceAccountResp queryRep = userEntityTranforToResp(a);
				u = umap.get(a.getUserOid());
				if (u != null) {
					for (Object user : users) {
						Object[] aname = (Object[]) user;
						String uid = aname[0].toString();
						String uname = aname[1].toString();
						if (!StringUtils.isEmpty(u.getUserOid()) && u.getUserOid().equals(uid)) {
							u.setName(uname);
						}
					}
					queryRep.setUserName(u.getName());
					queryRep.setPhone(u.getPhone());
				}
				rows.add(queryRep);
			}
			pagesRep.setRows(rows);
		}
		pagesRep.setTotal(cas.getTotalElements());
		return pagesRep;
	}

	/**
	 * 获取关联产品下拉列表
	 * @return
	 */
	public List<JSONObject> getRelationProducts() {
		List<JSONObject> jsonObjList = new ArrayList<JSONObject>();
		List<AccountInfoEntity> arps = this.accountInfoDao.findRelationProduct();
		log.info("获取关联产品下拉列表,arps:{}", JSON.toJSONString(arps));
		if (arps != null && arps.size() > 0) {
			JSONObject jsonObj = null;
			for (AccountInfoEntity arp : arps) {
				jsonObj = new JSONObject();
				jsonObj.put("oid", arp.getRelationProduct());
				jsonObj.put("name", arp.getRelationProductName());
				jsonObjList.add(jsonObj);
			}
		}
		return jsonObjList;
	}

	/**
	 * 账户查询接口
	 * @param spec
	 * @param pageable
	 * @return
	 */
	public AccountListResponse accountQueryList(Specification<AccountInfoEntity> spec, Pageable pageable) {
		if (pageable == null) {// 不带分页查询
			List<AccountInfoEntity> as = null;
			if (spec != null) {// 带查询条件
				as = accountInfoDao.findAll(spec);
			} else {
				as = accountInfoDao.findAll();
			}
			if (null != as && as.size() != 0) {
				List<AccountInfoDto> infos = new ArrayList<AccountInfoDto>();
				for (AccountInfoEntity entity : as) {
					AccountInfoDto tempEntity = new AccountInfoDto();
					BeanUtils.copyProperties(entity, tempEntity, new String[] { "createTime", "updateTime" });
					tempEntity.setCreateTime(DateUtil.format(entity.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
					infos.add(tempEntity);
				}
				return new AccountListResponse(infos);
			}
			return new AccountListResponse();
		} else {// 带分页查询
			AccountListResponse resp = new AccountListResponse();

			Page<AccountInfoEntity> result = null;
			if (spec != null) {// 带查询条件
				result = accountInfoDao.findAll(spec, pageable);
			} else {
				result = accountInfoDao.findAll(pageable);
			}
			if (null != result && result.getTotalElements() != 0) {
				resp.setTotal(result.getTotalElements());
				for (AccountInfoEntity entity : result.getContent()) {
					AccountInfoDto tempEntity = new AccountInfoDto();
					BeanUtils.copyProperties(entity, tempEntity, new String[] { "createTime", "updateTime" });
					tempEntity.setCreateTime(DateUtil.format(entity.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
					AccountQueryResponse qresp = new AccountQueryResponse(tempEntity);
					resp.getRows().add(qresp);
				}
			}
			return resp;
		}
	}

	/**
	 * 根据用户id查询用户基本户余额
	 * @param userOid
	 * @return
	 */
	public AccountBalanceResponse getAccountBalanceByUserOid(String userOid) {
		AccountBalanceResponse resp = new AccountBalanceResponse();

		// 用户基本户余额
		BigDecimal balance = BigDecimal.ZERO;

		// 提现冻结户余额
		BigDecimal withdrawFrozenBalance = BigDecimal.ZERO;

		// 充值冻结户余额
		BigDecimal rechargeFrozenBalance = BigDecimal.ZERO;

		// 续投冻结户余额
		BigDecimal continuedInvestmentFrozenBalance = BigDecimal.ZERO;

		List<AccountInfoEntity> accountInfoEntitys = accountInfoDao.findByUserOid(userOid);
		for (AccountInfoEntity info : accountInfoEntitys) {
			String accountType = info.getAccountType();
			if (AccountTypeEnum.BASICER.getCode().equals(accountType)) {
				balance = info.getBalance();
				continue;
			}
			if (AccountTypeEnum.FROZEN.getCode().equals(accountType)) {
				withdrawFrozenBalance = info.getBalance();
				continue;
			}
			if ("Y".equals(needRechargeFrozenAccount)) {
				if (AccountTypeEnum.RECHARGEFROZEN.getCode().equals(accountType)) {
					rechargeFrozenBalance = info.getBalance();
					continue;
				}
			}
			if (AccountTypeEnum.CONTINUED_INVESTMENT_FROZEN.getCode().equals(accountType)) {
				continuedInvestmentFrozenBalance = info.getBalance();
				continue;
			}
		}

		// 申购可用金额=账户余额-提现冻结金额-续投冻结
		BigDecimal applyAvailableBalance = CalculationUtil.sub(balance, withdrawFrozenBalance);
		// log.info("用户ID：{}账户余额-提现冻结金额={}, 续投冻结={}", userOid, applyAvailableBalance,
		// continuedInvestmentFrozenBalance);
		applyAvailableBalance = CalculationUtil.sub(applyAvailableBalance, continuedInvestmentFrozenBalance);
		// log.info("用户ID：{}用户账户申购可用余额：{}", userOid, applyAvailableBalance);

		if (applyAvailableBalance.compareTo(BigDecimal.ZERO) < 0) {
			applyAvailableBalance = BigDecimal.ZERO;
		}

		// 提现可用金额=申购可用余额-充值冻结金额 也等于=用户基本户余额-提现冻结金额-续投冻结-充值冻结金额
		BigDecimal withdrawAvailableBalance = CalculationUtil.sub(applyAvailableBalance, rechargeFrozenBalance);

		// log.info("用户ID：{}用户账户提现可用余额：{}", userOid, withdrawAvailableBalance);
		if (withdrawAvailableBalance.compareTo(BigDecimal.ZERO) < 0) {
			withdrawAvailableBalance = BigDecimal.ZERO;
		}
		log.info("用户ID：{},用户账户基本户余额：{}，申购可用：{}，提现可用：{}，提现冻结：{}，续投冻结：{}，充值冻结：{}", userOid, balance,
				applyAvailableBalance, withdrawAvailableBalance, withdrawFrozenBalance,
				continuedInvestmentFrozenBalance, rechargeFrozenBalance);
		resp.setRechargeFrozenBalance(rechargeFrozenBalance);// 充值冻结余额
		resp.setWithdrawFrozenBalance(withdrawFrozenBalance);// 提现冻结余额
		resp.setWithdrawAvailableBalance(withdrawAvailableBalance);// 提现可用余额
		resp.setApplyAvailableBalance(applyAvailableBalance);// 申购可用余额
		resp.setContinuedInvestmentFrozenBalance(continuedInvestmentFrozenBalance); // 续投账户余额
		resp.setBalance(balance);// 基本户余额
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");
		return resp;
	}

	/**
	 * 判断用户订单是否超额
	 * @param userOid
	 * @param orderBalance
	 * @return
	 */
	public boolean isOrderExcess(String userOid, BigDecimal orderBalance) {
		BigDecimal balance = BigDecimal.ZERO;
		BigDecimal frozenBalance = BigDecimal.ZERO;
		AccountInfoEntity baseAccInfoEntity = accountInfoDao.findBasicAccountByUserOid(userOid);
		AccountInfoEntity withdrawFrozenAccInfoEntity = accountInfoDao.findFrozenAccountByUserOid(userOid);
		AccountInfoEntity rechargeFrozenAccInfoEntity = null;
		if (baseAccInfoEntity != null) {
			balance = baseAccInfoEntity.getBalance();
		}

		if (withdrawFrozenAccInfoEntity != null) {
			frozenBalance = withdrawFrozenAccInfoEntity.getBalance();
		}

		// 断定是否需要充值冻结户 如果不需要，查询时充值冰结户设置为0
		if ("Y".equals(needRechargeFrozenAccount)) {
			rechargeFrozenAccInfoEntity = accountInfoDao
					.findAccountByAccountTypeAndUserOid(AccountTypeEnum.RECHARGEFROZEN.getCode(), userOid);
		}

		if (rechargeFrozenAccInfoEntity != null) {
			frozenBalance = CalculationUtil.add(frozenBalance, rechargeFrozenAccInfoEntity.getBalance());
		}

		if (CalculationUtil.isExcess(balance, CalculationUtil.add(frozenBalance, orderBalance))) {
			return false;
		}
		return true;
	}

	/**
	 * 根据用户id和账户类型获取发现行人账户余额
	 */
	public PublisherAccountBalanceResponse getPublisherAccountBalanceByUserOid(String userOid, String accountType) {
		PublisherAccountBalanceResponse resp = new PublisherAccountBalanceResponse();
		BigDecimal baseAccountBalance = BigDecimal.ZERO;
		BigDecimal availableAccountBalance = BigDecimal.ZERO;
		BigDecimal collectionAccountBalance = BigDecimal.ZERO;
		BigDecimal frozenAccountBalance = BigDecimal.ZERO;
		BigDecimal rechargFrozenAccountBalance = BigDecimal.ZERO;

		/**
		 * 授信额度
		 */
		BigDecimal lineOfCredit = BigDecimal.ZERO;
		/**
		 * 已使用授信额度
		 */
		BigDecimal usedLineOfCredit = BigDecimal.ZERO;

		// 发行人可用金账户
		AccountInfoEntity availableAccount = accountInfoDao
				.findAccountByAccountTypeAndUserOid(AccountTypeEnum.AVAILABLE_AMOUNT.getCode(), userOid);
		if (null == availableAccount) {
			resp.setErrorMessage("发行人可用金账户不存在，请核查！");
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			log.info("发行人账户余额查询返回：{}", resp);
			return resp;
		}

		availableAccountBalance = availableAccount.getBalance();
		lineOfCredit = availableAccount.getLineOfCredit();
		// 当可用金余额小于授信额度时，授信已使用额度=授信额度-可用金余额
		if (availableAccountBalance.compareTo(lineOfCredit) < 0) {
			usedLineOfCredit = lineOfCredit.subtract(availableAccountBalance);
		}

		baseAccountBalance = accountInfoDao.finBalanceByUserOidAndType(userOid, AccountTypeEnum.BASICER.getCode());
		collectionAccountBalance = accountInfoDao.finBalanceByUserOidAndType(userOid,
				AccountTypeEnum.COLLECTION_SETTLEMENT.getCode());
		frozenAccountBalance = accountInfoDao.finBalanceByUserOidAndType(userOid, AccountTypeEnum.FROZEN.getCode());

		if (baseAccountBalance == null || availableAccountBalance == null || collectionAccountBalance == null) {
			resp.setErrorMessage("账户不存在，请核查！");
			resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
			log.info("发行人账户余额：{}", resp);
			return resp;
		}
		// 断定是否需要充值冻结户 如果不需要，查询时充值冰结户设置为0
		if ("Y".equals(needRechargeFrozenAccount)) {
			rechargFrozenAccountBalance = accountInfoDao.finBalanceByUserOidAndType(userOid,
					AccountTypeEnum.RECHARGEFROZEN.getCode());
		}
		rechargFrozenAccountBalance = rechargFrozenAccountBalance == null ? BigDecimal.ZERO
				: rechargFrozenAccountBalance;
		baseAccountBalance = baseAccountBalance == null ? BigDecimal.ZERO : baseAccountBalance;
		frozenAccountBalance = frozenAccountBalance == null ? BigDecimal.ZERO : frozenAccountBalance;
		availableAccountBalance = availableAccountBalance == null ? BigDecimal.ZERO : availableAccountBalance;
		collectionAccountBalance = collectionAccountBalance == null ? BigDecimal.ZERO : collectionAccountBalance;

		if (AccountTypeEnum.AVAILABLE_AMOUNT.getCode().equals(accountType)) {
			resp.setAvailableAmountBalance(availableAccountBalance);

		} else if (AccountTypeEnum.COLLECTION_SETTLEMENT.getCode().equals(accountType)) {
			resp.setCollectionSettlementBalance(collectionAccountBalance);

		} else if (AccountTypeEnum.BASICER.getCode().equals(accountType)) {
			resp.setBasicBalance(baseAccountBalance);

		} else if (AccountTypeEnum.FROZEN.getCode().equals(accountType)) {
			resp.setFrozenAmountBalance(frozenAccountBalance);

		} else {
			resp.setAvailableAmountBalance(availableAccountBalance);
			resp.setCollectionSettlementBalance(collectionAccountBalance);
			resp.setBasicBalance(baseAccountBalance);
			resp.setFrozenAmountBalance(frozenAccountBalance);
			BigDecimal withdrawAvailableAmountBalance = baseAccountBalance
					.subtract(frozenAccountBalance.add(rechargFrozenAccountBalance));
			if (withdrawAvailableAmountBalance.compareTo(BigDecimal.ZERO) < 0) {
				withdrawAvailableAmountBalance = BigDecimal.ZERO;
			}
			resp.setWithdrawAvailableAmountBalance(withdrawAvailableAmountBalance);
			resp.setRechargFrozenBalance(rechargFrozenAccountBalance);
		}

		resp.setLineOfCredit(lineOfCredit);
		resp.setUsedLineOfCredit(usedLineOfCredit);
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");
		log.info("发行人账户余额：{}", resp);
		return resp;
	}
}