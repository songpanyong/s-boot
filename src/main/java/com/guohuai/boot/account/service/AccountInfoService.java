package com.guohuai.boot.account.service;

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

import lombok.extern.slf4j.Slf4j;

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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountQueryRequest;
import com.guohuai.account.api.request.CreateAccountRequest;
import com.guohuai.account.api.request.PlatformAccountInfoRequest;
import com.guohuai.account.api.request.PlatformReservedAccountDetailRequest;
import com.guohuai.account.api.request.ProductAccountRequest;
import com.guohuai.account.api.request.PublisherAccountInfoRequest;
import com.guohuai.account.api.request.UpdateAccountForm;
import com.guohuai.account.api.response.AccountBalanceResponse;
import com.guohuai.account.api.response.AccountListResponse;
import com.guohuai.account.api.response.AccountQueryResponse;
import com.guohuai.account.api.response.CreateAccountResponse;
import com.guohuai.account.api.response.FinanceAccountResp;
import com.guohuai.account.api.response.PlatformAccountInfoResponse;
import com.guohuai.account.api.response.PlatformReservedAccountDetailResponse;
import com.guohuai.account.api.response.ProductAccountListResponse;
import com.guohuai.account.api.response.PublisherAccountBalanceResponse;
import com.guohuai.account.api.response.PublisherAccountInfoResponse;
import com.guohuai.account.api.response.entity.AccountInfoDto;
import com.guohuai.account.api.response.entity.PlatformReservedAccountDto;
import com.guohuai.account.api.response.entity.ProductAccountDto;
import com.guohuai.account.component.util.EventTypeEnum;
import com.guohuai.account.component.util.OrderTypeEnum;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.basic.component.ext.web.BaseResp;
import com.guohuai.basic.component.ext.web.PageResp;
import com.guohuai.basic.config.ErrorDefineConfig;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.dao.AccountEventChildDao;
import com.guohuai.boot.account.dao.AccountEventTransDao;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.dao.PlatformAccountInfoDao;
import com.guohuai.boot.account.dao.PlatformInfoAuditDao;
import com.guohuai.boot.account.dao.PlatformInfoDao;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.entity.PlatformAccountInfoEntity;
import com.guohuai.boot.account.entity.PlatformInfoEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.CodeConstants;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.UserTypeEnum;

@Slf4j
@Service
public class AccountInfoService {
    @Autowired
    private AccountInfoDao accountInfoDao;
    @Autowired
    private UserInfoDao userInfoDao;
    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private SeqGenerator seqGenerator;
    @Autowired
    private PlatformInfoAuditDao platformInfoAuditDao;
    @Autowired
    private AccOrderDao accOrderDao;
    @Autowired
    private AccountEventTransDao accountEventTransDao;
    @Autowired
    private PlatformAccountInfoDao platformAccountInfoDao;
    @Autowired
    private AccountEventChildDao accountEventChildDao;
    @Autowired
    private PlatformInfoDao platformInfoDao;
    
    @Value("${needRechargeFrozenAccount:N}")
    private String needRechargeFrozenAccount;

    /**
     * 创建(投资人/发行人)默认账户
     */
    @Transactional
    public AccountInfoEntity addBaseAccount(String userOid, String remark, UserTypeEnum userType) {
        AccountInfoEntity account = new AccountInfoEntity();
        account.setOid(StringUtil.uuid());

        Timestamp nowTime = new Timestamp(System.currentTimeMillis());

        String accountNo = this.seqGenerator.next(CodeConstants.ACCOUNT_NO_PREFIX);
        account.setAccountNo(accountNo);
        account.setUserOid(userOid);
        account.setUserType(userType.getCode());
        account.setAccountType(AccountTypeEnum.BASICER.getCode());
//		account.setRelationProduct(relationProduct);
        account.setAccountName(userType.getName() + AccountTypeEnum.BASICER.getName());
        account.setOpenTime(nowTime);
//		account.setOpenOperator(openOperator);
        account.setBalance(BigDecimal.ZERO);
        account.setLineOfCredit(BigDecimal.ZERO);
        account.setStatus(AccountInfoEntity.STATUS_SUBMIT);
        account.setFrozenStatus(AccountInfoEntity.FROZENSTATUS_NORMAL);
        account.setRemark(remark);
        account.setCreateTime(nowTime);
        account.setUpdateTime(nowTime);
        account = accountInfoDao.save(account);
        log.info("创建用户基本户：{}", account);
        return account;
    }

    /**
     * 新增账户(创建子账户)
     */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public CreateAccountResponse addAccount(CreateAccountRequest req) {
		CreateAccountResponse resp = saveAccount(req, AccountInfoEntity.STATUS_SUBMIT);
		return resp;
	}
    
    /**
     * 查询账户，如果不存在则新建一个账号
     * 
     * @param userOid
     * @param accountType
     * @param userType
     * @return
     */
    public AccountInfoEntity findAccountOrCreate(String userOid, String accountType, String userType){
    	log.info("新增账户:[userOid={}, accountType={}, userType={}]", userOid, accountType, userType);
        CreateAccountResponse resp = new CreateAccountResponse();
    	AccountInfoEntity accountInfo = accountInfoDao.findByUserOidAndAccountTypeAndUserType(userOid, userType, accountType);
    	if(null == accountInfo){
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
     * 新增账户(创建子账户)
     */
	public CreateAccountResponse saveAccount(CreateAccountRequest req, String status) {
		log.info("新增账户:[" + JSONObject.toJSONString(req) + "]");

		CreateAccountResponse resp = new CreateAccountResponse();

		resp = checkAccountType(req);
		if (!resp.getReturnCode().equals(Constant.SUCCESS)) {
			return resp;
		}

		List<AccountInfoEntity> aies = null;
		log.info("查询用户是否存在");
		if (StringUtil.isEmpty(req.getRelationProduct())) {
			aies = accountInfoDao.findByUserOidAndAccountTypeAndUserTypeNoProduct(req.getUserOid(), req.getUserType(),
					req.getAccountType());
		} else {
			aies = accountInfoDao.findByUserOidAndAccountTypeAndProductNo(req.getUserOid(), req.getUserType(),
					req.getRelationProduct(), req.getAccountType());
		}
		if (aies != null && aies.size() > 0) {

			log.info("账户已存在，不需要新建 ,userOid：{},aies:{}", req.getUserOid(), aies.size());
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
			resp.setAccountNo(aies.get(0).getAccountNo());
			return resp;
		}
		log.info("查询用户是否存在完成，需要新建 ,userOid：{}", req.getUserOid());

		Timestamp nowTime = new Timestamp(System.currentTimeMillis());

		AccountInfoEntity account = new AccountInfoEntity();
		account.setOid(StringUtil.uuid());
		account.setUserOid(req.getUserOid());
		account.setUserType(req.getUserType());
		account.setAccountType(req.getAccountType());
		account.setRelationProduct(req.getRelationProduct());
		account.setRelationProductName(req.getRelationProductName());
		String accountName = UserTypeEnum.getEnumName(req.getUserType()) + AccountTypeEnum.getEnumName(req.getAccountType());
		if(AccountTypeEnum.PRODUCT.getCode().equals(req.getAccountType()) && !"default".equals(req.getRelationProductName())){
			accountName = req.getRelationProductName() + AccountTypeEnum.getEnumName(req.getAccountType());
		}
		account.setAccountName(accountName);
		String accountNo = this.seqGenerator.next(CodeConstants.ACCOUNT_NO_PREFIX);
		account.setAccountNo(accountNo);
		account.setOpenTime(nowTime);
		account.setBalance(BigDecimal.ZERO);
		account.setLineOfCredit(BigDecimal.ZERO);
		if (AccountInfoEntity.STATUS_SUBMIT.equals(status)) {// 提交
			account.setStatus(AccountInfoEntity.STATUS_SUBMIT);
			if (UserTypeEnum.INVESTOR.getCode().equals(account.getUserType())) {// 投资人账户不需要审核
				account.setAuditStatus(AccountInfoEntity.AUDIT_STATUS_NOCOMMIT);
			} else {
				account.setAuditStatus(AccountInfoEntity.AUDIT_STATUS_SUBMIT);
			}
		} else if (AccountInfoEntity.STATUS_SAVE.equals(status)) {// 保存
			account.setStatus(AccountInfoEntity.STATUS_SAVE);
			account.setAuditStatus(AccountInfoEntity.AUDIT_STATUS_NOCOMMIT);
		}

		account.setFrozenStatus(AccountInfoEntity.FROZENSTATUS_NORMAL);
		account.setCreateTime(nowTime);
		account.setUpdateTime(nowTime);
		log.info("保存用户信息{}", JSONObject.toJSONString(account));
		Object result = accountInfoDao.save(account);
		// 返回参数
		if (result != null) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
			resp.setUserOid(account.getUserOid());
			resp.setUserType(account.getUserType());
			resp.setAccountType(account.getAccountType());
			resp.setRelationProduct(account.getRelationProduct());
			resp.setRelationProductName(req.getRelationProductName());
			resp.setAccountNo(accountNo);
			resp.setBalance(account.getBalance());
			resp.setStatus(account.getStatus());
			resp.setCreateTime(DateUtil.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss"));
		} else {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("创建用户失败");
		}
		log.info("保存用户信息完成  createUserResp:{}", JSONObject.toJSON(resp));
		return resp;
	}

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
        //判断用户是否存在
        UserInfoEntity userInfo = userInfoService.getAccountUserByUserOid(req.getUserOid());
        if (userInfo == null) {
            resp.setReturnCode(Constant.USERNOTEXISTS);
            resp.setErrorMessage("用户不存在!");
            log.error("用户不存在![userOid=" + req.getUserOid() + "]");
            return resp;
        }
        if (StringUtil.isEmpty(UserTypeEnum.getEnumName(req.getUserType()))) {
            //用户类型不存在
            resp.setReturnCode(Constant.USERTYPENOTEXISTS);
            resp.setErrorMessage("用户类型不存在！");
            log.error("用户类型不存在，[userType=" + req.getUserType() + "]");
            return resp;
        }
        if (StringUtil.isEmpty(AccountTypeEnum.getEnumName(req.getAccountType()))) {
            //账户类型不存在
            resp.setReturnCode(Constant.ACCOUNTTYPENOTEXISTS);
            resp.setErrorMessage("账户类型不存在！");
            log.error("账户类型不存在，[accountType=" + req.getAccountType() + "]");
            return resp;
        }

//		if(UserTypeEnum.INVESTOR.getCode().equals(req.getUserType())) {
//			if(StringUtil.isEmpty(req.getRelationProduct())){
//				//账户类型不存在
//				resp.setReturnCode(Constant.RelationProductNotNULL);
//				resp.setErrorMessage("关联产品不能为空");
//				log.error("关联产品不能为空，{}",JSONObject.toJSON(req));
//				return resp;
//			}
//		}

        return resp;

    }

    /**
     * 账户查询接口
     *
     * @param @param  req
     * @param @return
     * @return AccountListResponse
     * @throws
     * @Title: accountQueryList
     * @Description:
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
            public Predicate toPredicate(Root<AccountInfoEntity> root,
                                         CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> list = new ArrayList<Predicate>();
                //根据用户id查询
                String userOid = req.getUserOid();
                if (!StringUtil.isEmpty(userOid)) {
                    Predicate pre = cb.equal(root.get("userOid").as(String.class), userOid);
                    list.add(pre);
                }
                //用户类型
                String userType = req.getUserType();
                if (!StringUtil.isEmpty(userType)) {
                    Predicate pre = cb.equal(root.get("userType").as(String.class), userType);
                    list.add(pre);
                }
                //账户类型
                String accountType = req.getAccountType();
                if (!StringUtil.isEmpty(userType)) {
                    Predicate pre = cb.equal(root.get("accountType").as(String.class), accountType);
                    list.add(pre);
                }
                //关联产品
//				 String relationProductName = req.getRelationProductName();
//				 if(!StringUtil.isEmpty(relationProductName)){
//					 Predicate pre = cb.equal(root.get("relationProductName").as(String.class), relationProductName);
//					 list.add(pre);
//				 }

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
                BeanUtils.copyProperties(entity, tempEntity, new String[]{"createTime", "updateTime"});
                tempEntity.setCreateTime(DateUtil.format(entity.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
                AccountQueryResponse qresp = new AccountQueryResponse(tempEntity);
                resp.getRows().add(qresp);
            }

        }
        return resp;
    }


    /**
     * 根据账户号获取账户
     *
     * @param @param  accountNo
     * @param @return
     * @return AccountInfoEntity
     * @throws
     * @Title: getAccountByNo
     * @Description:
     */
    public AccountInfoEntity getAccountByNo(String accountNo) {
        AccountInfoEntity account = accountInfoDao.findByAccountNo(accountNo);
        return account;
    }

    /**
     * 更新账户
     *
     * @param form
     * @return
     */
    @Transactional
    public BaseResp edit(UpdateAccountForm form) {
        BaseResp response = new BaseResp();
        log.info("编辑账户参数,form:{}", JSON.toJSONString(form));

        //验证参数
        if (StringUtil.isEmpty(form.getOid())) {
            response.setErrorCode(-1);
            response.setErrorMessage("OID不能为空");
            return response;
        }

        AccountInfoEntity a = this.accountInfoDao.findOne(form.getOid());

        if (a != null && !UserTypeEnum.INVESTOR.getCode().equals(a.getUserType())) {
            response.setErrorCode(-1);
            response.setErrorMessage("不符合修改条件");
            return response;
        }

        if (a != null && (a.getAuditStatus() == null || !a.getAuditStatus().equals(AccountInfoEntity.AUDIT_STATUS_SUBMIT))) {

            List<AccountInfoEntity> aies = null;
            if (StringUtil.isEmpty(form.getRelationProduct())) {
                aies = accountInfoDao.findByUserOidAndAccountTypeAndUserTypeNoProduct(a.getUserOid(), a.getUserType(), a.getAccountType());
            } else {
                aies = accountInfoDao.findByUserOidAndAccountTypeAndProductNo(a.getUserOid(), a.getUserType(), form.getRelationProduct(), a.getAccountType());
            }
            if (aies != null && aies.size() > 0) {
                if (!aies.get(0).getOid().equals(a.getOid())) {
                    log.info("账户已存在，不能编辑 ,aies.get(0){},a{}", JSON.toJSONString(aies.get(0)), JSON.toJSONString(a));
                    response.setErrorCode(-1);
                    response.setErrorMessage("账户已存在，不能编辑");
                    return response;
                }
            }

            UserInfoEntity userEntity = this.userInfoService.getAccountUserByUserOid(a.getUserOid());
            userEntity.setPhone(form.getPhone());
            userEntity.setRemark(form.getRemark());
            userEntity.setUpdateTime(new Timestamp(System.currentTimeMillis()));

            a.setAuditStatus(AccountInfoEntity.AUDIT_STATUS_SUBMIT);
            a.setRelationProduct(form.getRelationProduct());
//			a.setRelationProductName(form.getRelationProductName());
            a.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            a.setRemark(form.getRemark());

            userInfoDao.saveAndFlush(userEntity);

            a = this.accountInfoDao.saveAndFlush(a);

            log.info("编辑AccountInfoEntity,AccountInfoEntity:{}", JSON.toJSONString(a));

            response.setErrorCode(0);
        } else {
            response.setErrorCode(-1);
            response.setErrorMessage("不符合编辑条件");
        }
        return response;
    }

    /**
     * 更新账户
     *
     * @param form
     * @return
     */
    @Transactional
    public BaseResp update(UpdateAccountForm form) {
        BaseResp response = new BaseResp();
        log.info("更新账户参数,form:{}", JSON.toJSONString(form));

        //验证参数
        if (StringUtil.isEmpty(form.getOid())) {
            response.setErrorCode(-1);
            response.setErrorMessage("OID不能为空");
            return response;
        }
        if (StringUtil.isEmpty(form.getStatus())) {
            response.setErrorCode(-1);
            response.setErrorMessage("状态不能为空");
            return response;
        }

        AccountInfoEntity a = this.accountInfoDao.findOne(form.getOid());

        if (a != null && UserTypeEnum.INVESTOR.getCode().equals(a.getUserType())) {
            response.setErrorCode(-1);
            response.setErrorMessage("不符合修改条件");
            return response;
        }

        if (a != null && a.getStatus().equals(AccountInfoEntity.STATUS_SAVE)) {
            List<AccountInfoEntity> aies = null;
            if (StringUtil.isEmpty(form.getRelationProduct())) {
                aies = accountInfoDao.findByUserOidAndAccountTypeAndUserTypeNoProduct(a.getUserOid(), a.getUserType(), form.getAccountType());
            } else {
                aies = accountInfoDao.findByUserOidAndAccountTypeAndProductNo(a.getUserOid(), a.getUserType(), form.getRelationProduct(), form.getAccountType());
            }
            if (aies != null && aies.size() > 0) {
                if (!aies.get(0).getOid().equals(a.getOid())) {
                    log.info("账户已存在，不能修改 ,aies.get(0){},a{}", JSON.toJSONString(aies.get(0)), JSON.toJSONString(a));
                    response.setErrorCode(-1);
                    response.setErrorMessage("账户已存在，不能修改");
                    return response;
                }
            }

            a.setStatus(form.getStatus());
            a.setAccountType(form.getAccountType());
            a.setRelationProduct(form.getRelationProduct());
            a.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            a = this.accountInfoDao.saveAndFlush(a);

            log.info("更新AccountInfoEntity,AccountInfoEntity:{}", JSON.toJSONString(a));

            response.setErrorCode(0);
        } else {
            response.setErrorCode(-1);
            response.setErrorMessage("不符合修改条件");
        }
        return response;
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
        far.setLineOfCredit(accountEntity.getLineOfCredit());//授信额度
        return far;
    }

    /**
     * 获取账户列表
     *
     * @param spec
     * @param pageable
     * @return 如果返回的errCode属性等于0表示成功，否则表示失败，失败原因在errMessage里面体现
     */
    public PageResp<FinanceAccountResp> list(Specification<AccountInfoEntity> spec, Pageable pageable, final String phone, Specification<AccountInfoEntity> filterSpec) {
        log.info("获取账户列表,spec:{},pageable:{},phone:{}", new Object[]{JSON.toJSONString(spec), JSON.toJSONString(pageable)}, phone);
        PageResp<FinanceAccountResp> pagesRep = new PageResp<FinanceAccountResp>();

        final Map<String, UserInfoEntity> upmap = new HashMap<String, UserInfoEntity>();
        if (!StringUtil.isEmpty(phone)) {
            final List<UserInfoEntity> us = this.userInfoService.getUsers(phone);
            log.info("根据手机号查账户列表,us:{}", JSON.toJSONString(us));
            if (us != null && us.size() > 0) {

                //查询标签
                Specification<AccountInfoEntity> uspec = new Specification<AccountInfoEntity>() {
                    @Override
                    public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
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
					for(Object user : users){
						Object[] aname = (Object[])user;
						String uid = aname[0].toString();
						String uname = aname[1].toString();
						if(!StringUtils.isEmpty(u.getUserOid()) && u.getUserOid().equals(uid)){
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
     *
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

    @Transactional
    public BaseResp delete(String oid) {
        BaseResp response = new BaseResp();
        log.info("delete,odi:{}", oid);
        AccountInfoEntity a = this.accountInfoDao.findOne(oid);

        if (a != null && a.getStatus().equals(AccountInfoEntity.STATUS_SAVE)) {
            a.setStatus(AccountInfoEntity.STATUS_DELETE);
            a.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            a = this.accountInfoDao.saveAndFlush(a);
            log.info("删除AccountInfoEntity,AccountInfoEntity:{}", JSON.toJSONString(a));

            response.setErrorCode(0);
        } else {
            response.setErrorCode(-1);
        }

        return response;
    }

    @Transactional
    public BaseResp seal(String oid) {
        BaseResp response = new BaseResp();
        log.info("封存账号,odi:{}", oid);
        AccountInfoEntity a = this.accountInfoDao.findOne(oid);

        if (a != null && a.getStatus().equals(AccountInfoEntity.STATUS_VALID)) {
            a.setStatus(AccountInfoEntity.STATUS_SEALING);
            a.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            a = this.accountInfoDao.saveAndFlush(a);
            log.info("封存AccountInfoEntity,AccountInfoEntity:{}", JSON.toJSONString(a));

            response.setErrorCode(0);
        } else {
            response.setErrorCode(-1);
        }

        return response;
    }

    @Transactional
    public BaseResp frozen(String oid) {
        BaseResp response = new BaseResp();
        log.info("冻结账号,odi:{}", oid);
        AccountInfoEntity a = this.accountInfoDao.findOne(oid);

        if (a != null && a.getFrozenStatus().equals(AccountInfoEntity.FROZENSTATUS_NORMAL)) {
            a.setFrozenStatus(AccountInfoEntity.FROZENSTATUS_AUDI);
            a.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            a = this.accountInfoDao.saveAndFlush(a);
            log.info("冻结AccountInfoEntity,AccountInfoEntity:{}", JSON.toJSONString(a));

            response.setErrorCode(0);
        } else {
            response.setErrorCode(-1);
        }

        return response;
    }

    @Transactional
    public BaseResp thaw(String oid) {
        BaseResp response = new BaseResp();
        log.info("解冻账号,odi:{}", oid);
        AccountInfoEntity a = this.accountInfoDao.findOne(oid);

        if (a != null && a.getFrozenStatus().equals(AccountInfoEntity.FROZENSTATUS_FROZEN)) {
            a.setFrozenStatus(AccountInfoEntity.FROZENSTATUS_THAW);
            a.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            a = this.accountInfoDao.saveAndFlush(a);
            log.info("解冻AccountInfoEntity,AccountInfoEntity:{}", JSON.toJSONString(a));

            response.setErrorCode(0);
        } else {
            response.setErrorCode(-1);
        }
        return response;
    }

    @Transactional
    public BaseResp addApprove(String oid, String auditComment) {
        log.info("addApprove,oid:{},auditComment:{}", new Object[]{oid, auditComment});
        BaseResp response = new BaseResp();

        AccountInfoEntity a = this.accountInfoDao.findOne(oid);

        if (a != null && a.getStatus().equals(AccountInfoEntity.STATUS_SUBMIT)) {
            a.setStatus(AccountInfoEntity.STATUS_VALID);
            a.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            a.setAuditStatus(AccountInfoEntity.AUDIT_STATUS_PASS);
            a = this.accountInfoDao.saveAndFlush(a);
            log.info("addApprove,AccountInfoEntity:{}", JSON.toJSONString(a));

            response.setErrorCode(0);
        } else {
            response.setErrorCode(-1);
        }
        return response;
    }

    @Transactional
    public BaseResp addReject(String oid, String auditComment) {
        log.info("addReject,oid:{},auditComment:{}", new Object[]{oid, auditComment});
        BaseResp response = new BaseResp();

        AccountInfoEntity a = this.accountInfoDao.findOne(oid);

        if (a != null && a.getStatus().equals(AccountInfoEntity.STATUS_SUBMIT)) {
            a.setStatus(AccountInfoEntity.STATUS_SAVE);
            a.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            a.setAuditStatus(AccountInfoEntity.AUDIT_STATUS_REJECT);
            a = this.accountInfoDao.saveAndFlush(a);
            log.info("addReject,AccountInfoEntity:{}", JSON.toJSONString(a));

            response.setErrorCode(0);
        } else {
            response.setErrorCode(-1);
        }
        return response;
    }

    @Transactional
    public BaseResp updateApprove(String oid, String auditComment) {
        log.info("updateApprove,oid:{},auditComment:{}", new Object[]{oid, auditComment});
        BaseResp response = new BaseResp();

        AccountInfoEntity a = this.accountInfoDao.findOne(oid);

        if (a != null && a.getAuditStatus().equals(AccountInfoEntity.AUDIT_STATUS_SUBMIT)) {
            a.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            a.setAuditStatus(AccountInfoEntity.AUDIT_STATUS_PASS);
            a = this.accountInfoDao.saveAndFlush(a);
            log.info("updateApprove,AccountInfoEntity:{}", JSON.toJSONString(a));

            response.setErrorCode(0);
        } else {
            response.setErrorCode(-1);
        }
        return response;
    }

    @Transactional
    public BaseResp updateReject(String oid, String auditComment) {
        log.info("updateReject,oid:{},auditComment:{}", new Object[]{oid, auditComment});
        BaseResp response = new BaseResp();

        AccountInfoEntity a = this.accountInfoDao.findOne(oid);

        if (a != null && a.getAuditStatus().equals(AccountInfoEntity.AUDIT_STATUS_SUBMIT)) {
            a.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            a.setAuditStatus(AccountInfoEntity.AUDIT_STATUS_REJECT);
            a = this.accountInfoDao.saveAndFlush(a);
            log.info("updateReject,AccountInfoEntity:{}", JSON.toJSONString(a));

            response.setErrorCode(0);
        } else {
            response.setErrorCode(-1);
        }
        return response;
    }

    @Transactional
    public BaseResp sealApprove(String oid, String auditComment) {
        log.info("sealApprove,oid:{},auditComment:{}", new Object[]{oid, auditComment});
        BaseResp response = new BaseResp();

        AccountInfoEntity a = this.accountInfoDao.findOne(oid);

        if (a != null && a.getStatus().equals(AccountInfoEntity.STATUS_SEALING)) {
            a.setStatus(AccountInfoEntity.STATUS_SEALED);
            a.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            a.setAuditStatus(AccountInfoEntity.AUDIT_STATUS_PASS);
            a = this.accountInfoDao.saveAndFlush(a);
            log.info("sealApprove,AccountInfoEntity:{}", JSON.toJSONString(a));

            response.setErrorCode(0);
        } else {
            response.setErrorCode(-1);
        }
        return response;
    }

    @Transactional
    public BaseResp sealReject(String oid, String auditComment) {
        log.info("sealReject,oid:{},auditComment:{}", new Object[]{oid, auditComment});
        BaseResp response = new BaseResp();

        AccountInfoEntity a = this.accountInfoDao.findOne(oid);

        if (a != null && a.getStatus().equals(AccountInfoEntity.STATUS_SEALING)) {
            a.setStatus(AccountInfoEntity.STATUS_VALID);
            a.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            a.setAuditStatus(AccountInfoEntity.AUDIT_STATUS_REJECT);
            a = this.accountInfoDao.saveAndFlush(a);
            log.info("sealReject,AccountInfoEntity:{}", JSON.toJSONString(a));

            response.setErrorCode(0);
        } else {
            response.setErrorCode(-1);
        }
        return response;
    }

    @Transactional
    public BaseResp thawApprove(String oid, String auditComment) {
        log.info("thawApprove,oid:{},auditComment:{}", new Object[]{oid, auditComment});
        BaseResp response = new BaseResp();

        AccountInfoEntity a = this.accountInfoDao.findOne(oid);

        if (a != null && a.getFrozenStatus().equals(AccountInfoEntity.FROZENSTATUS_THAW)) {
            a.setFrozenStatus(AccountInfoEntity.FROZENSTATUS_NORMAL);
            a.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            a.setAuditStatus(AccountInfoEntity.AUDIT_STATUS_PASS);
            a = this.accountInfoDao.saveAndFlush(a);
            log.info("thawApprove,AccountInfoEntity:{}", JSON.toJSONString(a));

            response.setErrorCode(0);
        } else {
            response.setErrorCode(-1);
        }
        return response;
    }

    @Transactional
    public BaseResp thawReject(String oid, String auditComment) {
        log.info("thawReject,oid:{},auditComment:{}", new Object[]{oid, auditComment});
        BaseResp response = new BaseResp();

        AccountInfoEntity a = this.accountInfoDao.findOne(oid);

        if (a != null && a.getFrozenStatus().equals(AccountInfoEntity.FROZENSTATUS_THAW)) {
            a.setFrozenStatus(AccountInfoEntity.FROZENSTATUS_FROZEN);
            a.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            a.setAuditStatus(AccountInfoEntity.AUDIT_STATUS_REJECT);
            a = this.accountInfoDao.saveAndFlush(a);
            log.info("thawReject,AccountInfoEntity:{}", JSON.toJSONString(a));

            response.setErrorCode(0);
        } else {
            response.setErrorCode(-1);
        }
        return response;
    }


    @Transactional
    public BaseResp frozenApprove(String oid, String auditComment) {
        log.info("frozenApprove,oid:{},auditComment:{}", new Object[]{oid, auditComment});
        BaseResp response = new BaseResp();

        AccountInfoEntity a = this.accountInfoDao.findOne(oid);

        if (a != null && a.getFrozenStatus().equals(AccountInfoEntity.FROZENSTATUS_AUDI)) {
            a.setFrozenStatus(AccountInfoEntity.FROZENSTATUS_FROZEN);
            a.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            a.setAuditStatus(AccountInfoEntity.AUDIT_STATUS_PASS);
            a = this.accountInfoDao.saveAndFlush(a);
            log.info("frozenApprove,AccountInfoEntity:{}", JSON.toJSONString(a));

            response.setErrorCode(0);
        } else {
            response.setErrorCode(-1);
        }
        return response;
    }

    @Transactional
    public BaseResp frozenReject(String oid, String auditComment) {
        log.info("frozenReject,oid:{},auditComment:{}", new Object[]{oid, auditComment});
        BaseResp response = new BaseResp();

        AccountInfoEntity a = this.accountInfoDao.findOne(oid);

        if (a != null && a.getFrozenStatus().equals(AccountInfoEntity.FROZENSTATUS_AUDI)) {
            a.setFrozenStatus(AccountInfoEntity.FROZENSTATUS_NORMAL);
            a.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            a.setAuditStatus(AccountInfoEntity.AUDIT_STATUS_REJECT);
            a = this.accountInfoDao.saveAndFlush(a);
            log.info("frozenReject,AccountInfoEntity:{}", JSON.toJSONString(a));

            response.setErrorCode(0);
        } else {
            response.setErrorCode(-1);
        }
        return response;
    }

    /**
     * 账户查询接口
     *
     * @param @param  req
     * @param @return
     * @return AccountListResponse
     * @throws
     * @Title: accountQueryList
     * @Description:
     */
    public AccountListResponse accountQueryList(Specification<AccountInfoEntity> spec, Pageable pageable) {
        if (pageable == null) {//不带分页查询
            List<AccountInfoEntity> as = null;
            if (spec != null) {//带查询条件
                as = accountInfoDao.findAll(spec);
            } else {
                as = accountInfoDao.findAll();
            }
            if (null != as && as.size() != 0) {
                List<AccountInfoDto> infos = new ArrayList<AccountInfoDto>();
                for (AccountInfoEntity entity : as) {
                    AccountInfoDto tempEntity = new AccountInfoDto();
                    BeanUtils.copyProperties(entity, tempEntity, new String[]{"createTime", "updateTime"});
                    tempEntity.setCreateTime(DateUtil.format(entity.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
                    infos.add(tempEntity);
                }
                return new AccountListResponse(infos);
            }
            return new AccountListResponse();
        } else {//带分页查询
            AccountListResponse resp = new AccountListResponse();

            Page<AccountInfoEntity> result = null;
            if (spec != null) {//带查询条件
                result = accountInfoDao.findAll(spec, pageable);
            } else {
                result = accountInfoDao.findAll(pageable);
            }
            if (null != result && result.getTotalElements() != 0) {
                resp.setTotal(result.getTotalElements());
                for (AccountInfoEntity entity : result.getContent()) {
                    AccountInfoDto tempEntity = new AccountInfoDto();
                    BeanUtils.copyProperties(entity, tempEntity, new String[]{"createTime", "updateTime"});
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
     *
     * @param userOid
     * @return
     */
    public AccountBalanceResponse getAccountBalanceByUserOid(String userOid) {
        AccountBalanceResponse resp = new AccountBalanceResponse();

        //用户基本户余额
        BigDecimal balance = BigDecimal.ZERO;

        //提现冻结户余额
        BigDecimal withdrawFrozenBalance = BigDecimal.ZERO;

        //充值冻结户余额
        BigDecimal rechargeFrozenBalance = BigDecimal.ZERO;

        //续投冻结户余额
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

        //申购可用金额=账户余额-提现冻结金额-续投冻结
        BigDecimal applyAvailableBalance = sub(balance, withdrawFrozenBalance);
//        log.info("用户ID：{}账户余额-提现冻结金额={}, 续投冻结={}", userOid, applyAvailableBalance, continuedInvestmentFrozenBalance);
        applyAvailableBalance = sub(applyAvailableBalance, continuedInvestmentFrozenBalance);
//        log.info("用户ID：{}用户账户申购可用余额：{}", userOid, applyAvailableBalance);

        if (applyAvailableBalance.compareTo(BigDecimal.ZERO) < 0) {
            applyAvailableBalance = BigDecimal.ZERO;
        }

        //提现可用金额=申购可用余额-充值冻结金额  也等于=用户基本户余额-提现冻结金额-续投冻结-充值冻结金额 
        BigDecimal withdrawAvailableBalance = sub(applyAvailableBalance, rechargeFrozenBalance);

//        log.info("用户ID：{}用户账户提现可用余额：{}", userOid, withdrawAvailableBalance);
        if (withdrawAvailableBalance.compareTo(BigDecimal.ZERO) < 0) {
            withdrawAvailableBalance = BigDecimal.ZERO;
        }
        log.info("用户ID：{},用户账户基本户余额：{}，申购可用：{}，提现可用：{}，提现冻结：{}，续投冻结：{}，充值冻结：{}",
                userOid, balance, applyAvailableBalance, withdrawAvailableBalance, withdrawFrozenBalance, continuedInvestmentFrozenBalance, rechargeFrozenBalance);
        resp.setRechargeFrozenBalance(rechargeFrozenBalance);//充值冻结余额
        resp.setWithdrawFrozenBalance(withdrawFrozenBalance);//提现冻结余额
        resp.setWithdrawAvailableBalance(withdrawAvailableBalance);//提现可用余额
        resp.setApplyAvailableBalance(applyAvailableBalance);//申购可用余额
        resp.setContinuedInvestmentFrozenBalance(continuedInvestmentFrozenBalance); //续投账户余额
        resp.setBalance(balance);//基本户余额
        resp.setReturnCode(Constant.SUCCESS);
        resp.setErrorMessage("成功");
        return resp;
    }

    /**
     * 判断用户订单是否超额
     *
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

        //断定是否需要充值冻结户 如果不需要，查询时充值冰结户设置为0
        if ("Y".equals(needRechargeFrozenAccount)) {
            rechargeFrozenAccInfoEntity = accountInfoDao.findAccountByAccountTypeAndUserOid(AccountTypeEnum.RECHARGEFROZEN.getCode(), userOid);
        }

        if (rechargeFrozenAccInfoEntity != null) {
            frozenBalance = add(frozenBalance, rechargeFrozenAccInfoEntity.getBalance());
        }

        if (isExcess(balance, add(frozenBalance, orderBalance))) {
            return false;
        }
        return true;
    }

    /**
     * BigDecimal的加法运算。
     *
     * @param b1 被加数
     * @param b2 加数
     * @return 两个参数的和
     */
    public BigDecimal add(BigDecimal b1, BigDecimal b2) {
        return b1.add(b2);
    }

    /**
     * BigDecimal的减法运算。
     *
     * @param b1 被减数
     * @param b2 减数
     * @return 两个参数的差
     */
    public BigDecimal sub(BigDecimal b1, BigDecimal b2) {
        return b1.subtract(b2);
    }

    /**
     * 判断是否超额
     *
     * @param balance1
     * @param balance2
     * @return
     */
    private boolean isExcess(BigDecimal balance1, BigDecimal balance2) {
        boolean isExcess = false;
        if (balance1.compareTo(balance2) != -1) {//-1 小于  0 等于 1 大于
            isExcess = true;
        }
        return isExcess;
    }

    /**
     * 根据用户id和账户类型获取发现行人账户余额
     */
    public PublisherAccountBalanceResponse getPublisherAccountBalanceByUserOid(String userOid, String accountType) {
        PublisherAccountBalanceResponse resp = new PublisherAccountBalanceResponse();
        BigDecimal baseAccountBalance = BigDecimal.ZERO;
        BigDecimal availableAccountBalance = BigDecimal.ZERO;
        BigDecimal collectionAccountBalance = BigDecimal.ZERO;
        BigDecimal frozenAccountBalance =BigDecimal.ZERO;
        BigDecimal rechargFrozenAccountBalance = BigDecimal.ZERO;
        
        /**
         * 授信额度
         */
        BigDecimal lineOfCredit=BigDecimal.ZERO;
        /**
         * 已使用授信额度
         */
        BigDecimal usedLineOfCredit=BigDecimal.ZERO;
        
        //发行人可用金账户
        AccountInfoEntity availableAccount=accountInfoDao.findAccountByAccountTypeAndUserOid(AccountTypeEnum.AVAILABLE_AMOUNT.getCode(), userOid);
        if(null==availableAccount){
        	resp.setErrorMessage("发行人可用金账户不存在，请核查！");
            resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
            log.info("发行人账户余额查询返回：{}", resp);
            return resp;
        }
        
        availableAccountBalance=availableAccount.getBalance();
        lineOfCredit=availableAccount.getLineOfCredit();
        //当可用金余额小于授信额度时，授信已使用额度=授信额度-可用金余额
        if(availableAccountBalance.compareTo(lineOfCredit)<0){
        	usedLineOfCredit=lineOfCredit.subtract(availableAccountBalance);
        }
        
        baseAccountBalance = accountInfoDao.finBalanceByUserOidAndType(userOid,AccountTypeEnum.BASICER.getCode());
        collectionAccountBalance = accountInfoDao.finBalanceByUserOidAndType(userOid,AccountTypeEnum.COLLECTION_SETTLEMENT.getCode());
        frozenAccountBalance = accountInfoDao.finBalanceByUserOidAndType(userOid,AccountTypeEnum.FROZEN.getCode());
        
        if (baseAccountBalance == null || availableAccountBalance == null || collectionAccountBalance == null ) {
            resp.setErrorMessage("账户不存在，请核查！");
            resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
            log.info("发行人账户余额：{}", resp);
            return resp;
        }
        //断定是否需要充值冻结户 如果不需要，查询时充值冰结户设置为0
        if ("Y".equals(needRechargeFrozenAccount)) {
        	rechargFrozenAccountBalance = accountInfoDao.finBalanceByUserOidAndType(userOid,AccountTypeEnum.RECHARGEFROZEN.getCode());
        }
        rechargFrozenAccountBalance = rechargFrozenAccountBalance==null?BigDecimal.ZERO:rechargFrozenAccountBalance;
        baseAccountBalance = baseAccountBalance==null?BigDecimal.ZERO:baseAccountBalance;
        frozenAccountBalance = frozenAccountBalance==null?BigDecimal.ZERO:frozenAccountBalance;
        availableAccountBalance = availableAccountBalance==null?BigDecimal.ZERO:availableAccountBalance;
        collectionAccountBalance = collectionAccountBalance==null?BigDecimal.ZERO:collectionAccountBalance;
         
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
            BigDecimal withdrawAvailableAmountBalance = baseAccountBalance.subtract(frozenAccountBalance.add(rechargFrozenAccountBalance));
            if(withdrawAvailableAmountBalance.compareTo(BigDecimal.ZERO)<0){
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
    
    public static void main(String[] args) {
    	BigDecimal withdrawAvailableAmountBalance = BigDecimal.valueOf(-1);
    	if(withdrawAvailableAmountBalance.compareTo(BigDecimal.ZERO)<0){
        	withdrawAvailableAmountBalance = BigDecimal.ZERO;
        }
    	System.out.println(withdrawAvailableAmountBalance);
	}
    
	/**
	 * 查询产品户可用余额
	 * @param req
	 * @return
	 */
	public ProductAccountListResponse queryProductAccountBalance(ProductAccountRequest req) {
		Specification<AccountInfoEntity> spec = getqueryProductAccountBalanceSpec(req);

		List<AccountInfoEntity> as = accountInfoDao.findAll(spec);
		List<AccountInfoDto> infos = new ArrayList<AccountInfoDto>();
		if (null != as && !as.isEmpty()) {
			AccountInfoDto tempEntity;
			for (AccountInfoEntity entity : as) {
				tempEntity = new AccountInfoDto();
				tempEntity.setUserOid(entity.getUserOid());
				tempEntity.setRelationProduct(entity.getRelationProduct());
				tempEntity.setBalance(entity.getBalance());
				infos.add(tempEntity);
			}
		}

		ProductAccountListResponse result = new ProductAccountListResponse(infos);
		result.setErrorCode(Integer.valueOf(Constant.SUCCESS));
		result.setErrorMessage(ErrorDefineConfig.define.get(result.getErrorCode()));
		return result;
	}
	
	/**
	 * 获取查询产品户可用余额接口条件
	 * @param req
	 * @return
	 */
	private Specification<AccountInfoEntity> getqueryProductAccountBalanceSpec(ProductAccountRequest req) {
		Specification<AccountInfoEntity> spec = new Specification<AccountInfoEntity>() {
			@Override
			public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.notEqual(root.get("status").as(String.class), AccountInfoEntity.STATUS_DELETE);
			}
		};
		spec = Specifications.where(spec);
		
		Specification<AccountInfoEntity> accountTypeSpec = new Specification<AccountInfoEntity>() {
			public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.equal(root.get("accountType").as(String.class), AccountTypeEnum.PRODUCT.getCode());
			}
		};
		spec = Specifications.where(spec).and(accountTypeSpec);

		final String userOid = req.getUserOid();
		final String relationProduct = req.getRelationProduct();

		if (!StringUtil.isEmpty(userOid)) {
			Specification<AccountInfoEntity> userOidSpec = new Specification<AccountInfoEntity>() {
				public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
					return cb.equal(root.get("userOid").as(String.class), userOid);
				}
			};
			spec = Specifications.where(spec).and(userOidSpec);
		}

		if (!StringUtil.isEmpty(relationProduct)) {
			Specification<AccountInfoEntity> relationProductSpec = new Specification<AccountInfoEntity>() {
				public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
					return cb.equal(root.get("relationProduct").as(String.class), relationProduct);
				}
			};
			spec = Specifications.where(spec).and(relationProductSpec);
		}
		return spec;
	}

	/**
	 * 查询平台户基本信息
	 * @param req
	 * @return
	 */
	public PlatformAccountInfoResponse platformAccountInfo(
			PlatformAccountInfoRequest req) {
		PlatformAccountInfoResponse resp = new PlatformAccountInfoResponse();
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("查询成功");
		//查询平台信息
		PlatformInfoEntity platform = platformInfoDao.findByUserOid(req.getUserOid());
		if(platform == null){
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("平台信息不存在");
		}
		resp.setAccountCreateTime(platform.getCreateTime().toString());
		resp.setAccountName(platform.getPlatformName());
		resp.setAccountOid(platform.getUserOid());
		resp.setAccountState(platform.getPlatformStatus());
		// 查询可用余额 
		BigDecimal availableBalanceAmount = accountInfoDao
				.finBalanceByUserOidAndType(req.getUserOid(), AccountTypeEnum.BASICER.getCode());
		// 查询提现冻结余额
		BigDecimal frozenBalanceAmount = accountInfoDao
				.finBalanceByUserOidAndType(req.getUserOid(), AccountTypeEnum.FROZEN.getCode());
		if(frozenBalanceAmount == null){
			frozenBalanceAmount = BigDecimal.ZERO;
		}
		// 查询充值总额 
		BigDecimal allDepositAmount = accOrderDao
				.findBalanceByUserOidAndOrderType(req.getUserOid(), OrderTypeEnum.RECHARGE.getCode());
		// 查询提现总额
		BigDecimal allWithdrawAmount = accOrderDao
				.findBalanceByUserOidAndOrderType(req.getUserOid(), OrderTypeEnum.WITHDRAWALS.getCode());
		// 查询转账总额 
		BigDecimal allTransferAmount = accountEventTransDao.findPlatformTransferBalance();
		resp.setAllDepositAmount(allDepositAmount);
		resp.setAllTransferAmount(allTransferAmount);
		resp.setAllWithdrawAmount(allWithdrawAmount);
		resp.setAvailableBalanceAmount(availableBalanceAmount);
		resp.setFrozenBalanceAmount(frozenBalanceAmount);
		// 查询备付金户
		List<PlatformReservedAccountDto> reservedAccountList = this.installReservedAccountList(req.getUserOid());
		resp.setReservedAccountList(reservedAccountList);
		return resp;
	}

	/**
	 * 组装平台备付金账户信息
	 * @param userOid 平台id
	 * @return
	 */
	private List<PlatformReservedAccountDto> installReservedAccountList(
			String userOid) {
		List<PlatformReservedAccountDto> reservedAccountList = new ArrayList<PlatformReservedAccountDto>();
		List<AccountInfoEntity> accountList = accountInfoDao
				.findByUserOidAndAccountType(userOid, AccountTypeEnum.RESERVE.getCode());
		for(AccountInfoEntity account : accountList){
			PlatformReservedAccountDto dto = new PlatformReservedAccountDto();
			dto.setAccountName(account.getAccountName());
			dto.setAccountNo(account.getAccountNo());
			//可用余额 
			dto.setAvailableBalanceAmount(account.getBalance());
			//授信额度
			dto.setLineOfCreditAmount(account.getLineOfCredit());
			//转账金额 
			BigDecimal transferAmount = accountEventTransDao.findTransferBalanceByAccountNo(account.getAccountNo());
			// 红包出金总额 
			BigDecimal redPacketsAmount = accountEventTransDao
					.findOutBalanceByAccountNoAndEvent(account.getAccountNo(), EventTypeEnum.USE_REDPACKET.getCode());
			//代金券出金总额 -退款总额
//			BigDecimal couponAmountT0 = accountEventTransDao
//					.findOutBalanceByAccountNoAndEvent(account.getAccountNo(), EventTypeEnum.USE_VOUCHER_T0.getCode());
//			BigDecimal couponAmountT1 = accountEventTransDao
//					.findOutBalanceByAccountNoAndEvent(account.getAccountNo(), EventTypeEnum.USE_VOUCHER_T1.getCode());
			BigDecimal couponAmount = accountEventTransDao.findCouponOutBalance(account.getAccountNo());
			//加息券出金总额 
//			BigDecimal rateCouponAmount = accountEventTransDao
//					.findOutBalanceByAccountNoAndEvent(account.getAccountNo(), EventTypeEnum.GRANT_RATE_COUPON_PROFIT.getCode());
			BigDecimal rateCouponAmount = accountEventTransDao.findrateCouponAmount(account.getAccountNo());
			//体验金出金总额 
			BigDecimal tasteCouponAmount = accountEventTransDao
					.findOutBalanceByAccountNoAndEvent(account.getAccountNo(), EventTypeEnum.GRANT_EXPERIENCE_PROFIT.getCode());
			//返佣出金总额 
			BigDecimal rebateAmount = accountEventTransDao
					.findOutBalanceByAccountNoAndEvent(account.getAccountNo(), EventTypeEnum.REBATE.getCode());
			dto.setCouponAmount(couponAmount);
			if(rateCouponAmount != null){
				dto.setRateCouponAmount(rateCouponAmount);
			}
			if(rebateAmount != null){
				dto.setRebateAmount(rebateAmount);
			}
			if(redPacketsAmount != null){
				dto.setRedPacketsAmount(redPacketsAmount);
			}
			if(tasteCouponAmount != null){
				dto.setTasteCouponAmount(tasteCouponAmount);
			}
			if(transferAmount != null){
				dto.setTransferAmount(transferAmount);
			}
			reservedAccountList.add(dto);
		}
		return reservedAccountList;
	}

	/**
	 * 平台备付金详情查询
	 * @param req
	 * @return
	 */
	public PlatformReservedAccountDetailResponse platformReservedAccountDetail(
			PlatformReservedAccountDetailRequest req) {
		PlatformReservedAccountDetailResponse resp = new PlatformReservedAccountDetailResponse();
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("查询成功");
		//查询账户信息
		AccountInfoEntity account = accountInfoDao.findByAccountNo(req.getAccountNo());
		if(account == null){
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("备付金账户不存在");
			return resp;
		}
		resp.setAccountName(account.getAccountName());
		resp.setAvailableBalance(account.getBalance());
		resp.setLineOfCredit(account.getLineOfCredit());
		BigDecimal netBalance = account.getBalance().subtract(account.getLineOfCredit());
		resp.setNetBalance(netBalance);
		PlatformAccountInfoEntity accountInfo = platformAccountInfoDao.findByAccountNo(req.getAccountNo());
		if(accountInfo == null){
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("备付金账户信息不存在");
			return resp;
		}
		resp.setStatus(accountInfo.getAccountStatus());
		String statusDisp = "停用";
		if(PlatformAccountInfoEntity.STATUS_RUN.endsWith(accountInfo.getAccountStatus())){
			statusDisp = "启用";
		}
		resp.setStatusDisp(statusDisp);
		//查询备付金使用类型
		List<String> useType = accountEventChildDao.findEventNameByReservedAccountNo(req.getAccountNo());
		resp.setUseType(useType);
		return resp;
	}

	/**
	 * 查询发行人账户信息
	 * @param req
	 * @return
	 */
	public PublisherAccountInfoResponse publisherAccountInfo(
			PublisherAccountInfoRequest req) {
		PublisherAccountInfoResponse resp = new PublisherAccountInfoResponse();
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("查询成功");
		String userOid = req.getUserOid();
		resp.setUserOid(userOid);
		//基本账户余额
	    BigDecimal basicBalance = accountInfoDao
	    		.finBalanceByUserOidAndType(userOid,AccountTypeEnum.BASICER.getCode());
	    //清算户余额
	    BigDecimal collectionSettlementBalance = accountInfoDao
	    		.finBalanceByUserOidAndType(userOid,AccountTypeEnum.COLLECTION_SETTLEMENT.getCode());
	    //提现可用金余额
	    BigDecimal withdrawAvailableAmountBalance = BigDecimal.ZERO;
	    //冻结户余额
	    BigDecimal frozenAmountBalance = accountInfoDao
	    		.finBalanceByUserOidAndType(userOid,AccountTypeEnum.FROZEN.getCode());
	    if(frozenAmountBalance == null){
	    	frozenAmountBalance = BigDecimal.ZERO;
	    }
		//产品户可用余额总额 
		BigDecimal productAvailableAmountBalance = accountInfoDao
				.findTotalPriductBalanceByUserOid(userOid);
		//产品户授信额度总额
		BigDecimal productCreditAmountBalance = accountInfoDao
				.findTotalPriductCreditByUserOid(userOid);
		//产品户账户净额总额
		BigDecimal productNetAmountBalance = BigDecimal.ZERO;
		
		if(basicBalance != null && frozenAmountBalance != null){
			withdrawAvailableAmountBalance = basicBalance.subtract(frozenAmountBalance);
		}
		if(productAvailableAmountBalance != null && productCreditAmountBalance != null){
			productNetAmountBalance = productAvailableAmountBalance
					.subtract(productCreditAmountBalance);
		}
		resp.setBasicBalance(basicBalance);
		resp.setCollectionSettlementBalance(collectionSettlementBalance);
		resp.setFrozenAmountBalance(frozenAmountBalance);
		resp.setProductAvailableAmountBalance(productAvailableAmountBalance);
		resp.setProductCreditAmountBalance(productCreditAmountBalance);
		resp.setProductNetAmountBalance(productNetAmountBalance);
		resp.setWithdrawAvailableAmountBalance(withdrawAvailableAmountBalance);
		//产品户信息
		List<ProductAccountDto> productAccountList = this
				.getProductAccountList(req.getProductAccountNo(), userOid);
		resp.setProductAccountList(productAccountList);
		return resp;
	}

	private List<ProductAccountDto> getProductAccountList(
			String productAccountNo, String userOid) {
		List<ProductAccountDto> productAccountList = new ArrayList<ProductAccountDto>();
		ProductAccountDto dto = null;
		if(productAccountNo != null){
			AccountInfoEntity account = accountInfoDao.findByAccountNo(productAccountNo);
			if(account == null){
				return productAccountList;
			}
			dto = new ProductAccountDto();
			dto.setAccountName(account.getAccountName());
			dto.setAccountNo(account.getAccountNo());
			dto.setAvailableCredit(account.getBalance().subtract(account.getLineOfCredit()));
			dto.setBalance(account.getBalance());
			dto.setLineOfCredit(account.getLineOfCredit());
			dto.setOpenTime(account.getOpenTime().toString());
			dto.setRelationProduct(account.getRelationProduct());
			dto.setRelationProductName(account.getRelationProductName());
			//默认产品户
			if("default".equals(account.getRelationProductName())){
				dto.setIsDefault("Y");
			}else{
				dto.setIsDefault("N");
			}
			productAccountList.add(dto);
			return productAccountList;
		}
		List<AccountInfoEntity> accountList = accountInfoDao
				.findByUserOidAndAccountType(userOid, AccountTypeEnum.PRODUCT.getCode());
		if(accountList != null){
			for(AccountInfoEntity productAccount : accountList){
				dto = new ProductAccountDto();
				dto.setAccountName(productAccount.getAccountName());
				dto.setAccountNo(productAccount.getAccountNo());
				dto.setAvailableCredit(productAccount.getBalance().subtract(productAccount.getLineOfCredit()));
				dto.setBalance(productAccount.getBalance());
				dto.setLineOfCredit(productAccount.getLineOfCredit());
				dto.setOpenTime(productAccount.getOpenTime().toString());
				dto.setRelationProduct(productAccount.getRelationProduct());
				dto.setRelationProductName(productAccount.getRelationProductName());
				//默认产品户
				if("default".equals(productAccount.getRelationProductName())){
					dto.setIsDefault("Y");
				}else{
					dto.setIsDefault("N");
				}
				productAccountList.add(dto);
			}
		}
		return productAccountList;
	}
	
}