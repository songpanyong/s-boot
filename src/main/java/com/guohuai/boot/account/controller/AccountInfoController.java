package com.guohuai.boot.account.controller;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.*;
import com.guohuai.account.api.response.*;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.basic.component.ext.web.BaseResp;
import com.guohuai.basic.component.ext.web.PageResp;
import com.guohuai.basic.component.ext.web.Response;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.service.AccountInfoService;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.UserTypeEnum;
import com.guohuai.settlement.api.request.InteractiveRequest;

import lombok.extern.slf4j.Slf4j;
import net.kaczmarzyk.spring.data.jpa.domain.DateAfterInclusive;
import net.kaczmarzyk.spring.data.jpa.domain.DateBeforeInclusive;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.domain.Like;
import net.kaczmarzyk.spring.data.jpa.web.annotation.And;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;

import java.util.List;

import static com.guohuai.boot.account.validate.controller.AccountInfoControllerVal.valCreateProductAccount;

@Slf4j
@SuppressWarnings("deprecation")
@RestController
@RequestMapping(value = "/account/account")
public class AccountInfoController {
    @Autowired
    private AccountInfoService accountInfoService;

	@RequestMapping(value = "/add", method = RequestMethod.POST)
	public CreateAccountResponse createAccount(@RequestBody CreateAccountRequest req) {
		CreateAccountResponse resp = accountInfoService.addAccount(req);
		return resp;
	}

    /**
     * 账户查询
     */
    @RequestMapping(value = "/accountlist", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<AccountListResponse> accountQueryList(@RequestBody AccountQueryRequest req) {

        Direction sortDirection = Direction.DESC;
        if (!"desc".equals(req.getSort())) {
            sortDirection = Direction.ASC;
        }

        String sortField = req.getSortField();
        if (StringUtil.isEmpty(sortField)) {
            sortField = "createTime";
        }

        int page = req.getPage();
        int rows = req.getRows();

        Specification<AccountInfoEntity> spec = new Specification<AccountInfoEntity>() {
            @Override
            public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                return cb.notEqual(root.get("status").as(String.class), AccountInfoEntity.STATUS_DELETE);
            }
        };
        spec = Specifications.where(spec);

        final String userOid = req.getUserOid();//根据用户id查询
        final String userType = req.getUserType();//用户类型
        final String accountType = req.getAccountType();//账户类型
        final String relationProduct = req.getRelationProduct();//关联产品
        final String relationProductName = req.getRelationProductName();//关联产品名称
        final String accountNo = req.getAccountNo();//账户号

        if (!StringUtil.isEmpty(userOid)) {
            Specification<AccountInfoEntity> userOidSpec = new Specification<AccountInfoEntity>() {
                @Override
                public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                    return cb.equal(root.get("userOid").as(String.class), userOid);
                }
            };

            spec = Specifications.where(spec).and(userOidSpec);
        }
        if (!StringUtil.isEmpty(userType)) {

            Specification<AccountInfoEntity> userTypeSpec = new Specification<AccountInfoEntity>() {
                @Override
                public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                    return cb.equal(root.get("userType").as(String.class), userType);
                }
            };
            spec = Specifications.where(spec).and(userTypeSpec);
        }

        if (!StringUtil.isEmpty(accountType)) {
            Specification<AccountInfoEntity> accountTypeSpec = new Specification<AccountInfoEntity>() {
                @Override
                public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                    return cb.equal(root.get("accountType").as(String.class), accountType);
                }
            };
            spec = Specifications.where(spec).and(accountTypeSpec);
        }

        if (!StringUtil.isEmpty(accountNo)) {
            Specification<AccountInfoEntity> accountNoSpec = new Specification<AccountInfoEntity>() {
                @Override
                public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                    return cb.equal(root.get("accountNo").as(String.class), accountNo);
                }
            };
            spec = Specifications.where(spec).and(accountNoSpec);
        }
        if (!StringUtil.isEmpty(relationProduct)) {
            Specification<AccountInfoEntity> relationProductSpec = new Specification<AccountInfoEntity>() {
                @Override
                public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                    return cb.equal(root.get("relationProduct").as(String.class), relationProduct);
                }
            };
            spec = Specifications.where(spec).and(relationProductSpec);
        }
        if (!StringUtil.isEmpty(relationProductName)) {
            Specification<AccountInfoEntity> relationProductSpec = new Specification<AccountInfoEntity>() {
                @Override
                public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                    return cb.equal(root.get("relationProductName").as(String.class), relationProductName);
                }
            };
            spec = Specifications.where(spec).and(relationProductSpec);
        }
        Pageable pageable = null;
        if (page > 0 && rows > 0) {
            pageable = new PageRequest(page - 1, rows, new Sort(new Order(sortDirection, sortField)));
        }

        AccountListResponse resp = accountInfoService.accountQueryList(spec, pageable);

        return new ResponseEntity<AccountListResponse>(resp, HttpStatus.OK);
    }

    /**
     * 保存账户
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @ResponseBody
    public CreateAccountResponse save(CreateAccountRequest req) {
        CreateAccountResponse resp = accountInfoService.saveAccount(req, AccountInfoEntity.STATUS_SAVE);
        return resp;
    }

    /**
     * 提交账户
     */
    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    @ResponseBody
    public CreateAccountResponse submit(CreateAccountRequest req) {
        CreateAccountResponse resp = accountInfoService.saveAccount(req, AccountInfoEntity.STATUS_SUBMIT);
        return resp;
    }

    /**
     * 更新投资人账户
     */
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<BaseResp> edit(UpdateAccountForm from) {
        BaseResp repponse = accountInfoService.edit(from);
        return new ResponseEntity<BaseResp>(repponse, HttpStatus.OK);
    }

    /**
     * 更新账户(投资人类型除外的账户)
     */
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<BaseResp> update(UpdateAccountForm from) {
        BaseResp repponse = accountInfoService.update(from);
        return new ResponseEntity<BaseResp>(repponse, HttpStatus.OK);
    }

    /**
     * 详情
     */
    @RequestMapping(value = "/detail", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<FinanceAccountResp> detail(@RequestParam(required = true) String oid) {
        FinanceAccountResp pr = this.accountInfoService.read(oid);
        return new ResponseEntity<FinanceAccountResp>(pr, HttpStatus.OK);
    }

    /**
     * 查询账户列表
     */
    @RequestMapping(value = "/list", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<PageResp<FinanceAccountResp>> list(HttpServletRequest request,
                                                             @And({@Spec(path = "accountType", params = "accountType", spec = Equal.class),
//				@Spec(path = "relationProduct", params = "relationProduct", spec = Equal.class),
                                                                     @Spec(path = "status", params = "status", spec = Equal.class),
//																	 @Spec(path = "openTime", params = "openTimeBegin", spec = DateAfterInclusive.class),
//																	 @Spec(path = "openTime", params = "openTimeEnd", spec = DateBeforeInclusive.class),
                                                                     @Spec(path = "accountNo", params = "accountNo", spec = Equal.class),
                                                                     @Spec(path = "userType", params = "userType", spec = Equal.class)}) Specification<AccountInfoEntity> filterSpec,
                                                             @RequestParam String phone,
                                                             @RequestParam String userName,
                                                             @RequestParam int page, @RequestParam int rows,
                                                             @RequestParam(required = false, defaultValue = "updateTime") String sort,
                                                             @RequestParam(required = false, defaultValue = "desc") String order) {

        if (page < 1) {
            page = 1;
        }
        if (rows < 1) {
            rows = 1;
        }

        Direction sortDirection = Direction.DESC;
        if (!"desc".equals(order)) {
            sortDirection = Direction.ASC;
        }

        Specification<AccountInfoEntity> spec = new Specification<AccountInfoEntity>() {
            @Override
            public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                return cb.notEqual(root.get("status").as(String.class), AccountInfoEntity.STATUS_DELETE);
            }
        };
		spec = Specifications.where(spec).and(filterSpec);

        Pageable pageable = new PageRequest(page - 1, rows, new Sort(new Order(sortDirection, sort)));
        PageResp<FinanceAccountResp> rep = this.accountInfoService.list(spec, pageable, phone, filterSpec);
        return new ResponseEntity<PageResp<FinanceAccountResp>>(rep, HttpStatus.OK);
    }
    
    /**
     * 查询关联产品下拉列表
     */
    @RequestMapping(value = "/getRelationProducts", method = {RequestMethod.POST})
    public @ResponseBody
    ResponseEntity<Response> getRelationProducts() {
        List<JSONObject> jsonList = accountInfoService.getRelationProducts();
        Response r = new Response();
        r.with("rows", jsonList);
        return new ResponseEntity<Response>(r, HttpStatus.OK);
    }

    @RequestMapping(value = "/delete", method = {RequestMethod.POST, RequestMethod.DELETE})
    @ResponseBody
    public ResponseEntity<BaseResp> delete(@RequestParam(required = true) String oid) {
        BaseResp repponse = this.accountInfoService.delete(oid);
        return new ResponseEntity<BaseResp>(repponse, HttpStatus.OK);
    }

    /**
     * 封存账户
     */
    @RequestMapping(value = "/seal", method = {RequestMethod.POST, RequestMethod.DELETE})
    @ResponseBody
    public ResponseEntity<BaseResp> seal(@RequestParam(required = true) String oid) {
        BaseResp repponse = this.accountInfoService.seal(oid);
        return new ResponseEntity<BaseResp>(repponse, HttpStatus.OK);
    }

    /**
     * 冻结账户
     */
    @RequestMapping(value = "/frozen", method = {RequestMethod.POST, RequestMethod.DELETE})
    @ResponseBody
    public ResponseEntity<BaseResp> frozen(@RequestParam(required = true) String oid) {
        BaseResp repponse = this.accountInfoService.frozen(oid);
        return new ResponseEntity<BaseResp>(repponse, HttpStatus.OK);
    }

    /**
     * 解冻账户
     */
    @RequestMapping(value = "/thaw", method = {RequestMethod.POST, RequestMethod.DELETE})
    @ResponseBody
    public ResponseEntity<BaseResp> thaw(@RequestParam(required = true) String oid) {
        BaseResp repponse = this.accountInfoService.thaw(oid);
        return new ResponseEntity<BaseResp>(repponse, HttpStatus.OK);
    }

    /**
     * 查询审核账户列表
     */
    @RequestMapping(value = "/audit/list", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<PageResp<FinanceAccountResp>> auditList(HttpServletRequest request,
                                                                  @RequestParam final String userType, @RequestParam final String status,
                                                                  @RequestParam int page, @RequestParam int rows,
                                                                  @RequestParam(required = false, defaultValue = "updateTime") String sort,
                                                                  @RequestParam(required = false, defaultValue = "desc") String order) {

        if (page < 1) {
            page = 1;
        }
        if (rows < 1) {
            rows = 1;
        }

        Direction sortDirection = Direction.DESC;
        if (!"desc".equals(order)) {
            sortDirection = Direction.ASC;
        }

        Specification<AccountInfoEntity> spec = new Specification<AccountInfoEntity>() {
            @Override
            public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                String[] userTypes = userType.split(",");
                In<String> inUserTypes = cb.in(root.get("userType").as(String.class));
                for (String ut : userTypes) {
                    inUserTypes.value(ut);
                }
                String[] statuss = status.split(",");
                In<String> inStatuss = cb.in(root.get("status").as(String.class));
                for (String s : statuss) {
                    inStatuss.value(s);
                }
                return cb.and(cb.notEqual(root.get("status").as(String.class), AccountInfoEntity.STATUS_DELETE), inUserTypes, inStatuss);
            }
        };
        spec = Specifications.where(spec);

        Pageable pageable = new PageRequest(page - 1, rows, new Sort(new Order(sortDirection, sort)));
        PageResp<FinanceAccountResp> rep = this.accountInfoService.list(spec, pageable, "", null);
        return new ResponseEntity<PageResp<FinanceAccountResp>>(rep, HttpStatus.OK);
    }


    /**
     * 查询修改审核列表
     */
    @RequestMapping(value = "/update/list", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<PageResp<FinanceAccountResp>> updateAuditList(HttpServletRequest request,
                                                                        @RequestParam int page, @RequestParam int rows,
                                                                        @RequestParam(required = false, defaultValue = "updateTime") String sort,
                                                                        @RequestParam(required = false, defaultValue = "desc") String order) {

        if (page < 1) {
            page = 1;
        }
        if (rows < 1) {
            rows = 1;
        }

        Direction sortDirection = Direction.DESC;
        if (!"desc".equals(order)) {
            sortDirection = Direction.ASC;
        }

        Specification<AccountInfoEntity> spec = new Specification<AccountInfoEntity>() {
            @Override
            public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                return cb.and(cb.notEqual(root.get("status").as(String.class), AccountInfoEntity.STATUS_DELETE),
                        cb.equal(root.get("userType").as(String.class), UserTypeEnum.INVESTOR.getCode()),
                        cb.equal(root.get("auditStatus").as(String.class), AccountInfoEntity.AUDIT_STATUS_SUBMIT));
            }
        };
        spec = Specifications.where(spec);

        Pageable pageable = new PageRequest(page - 1, rows, new Sort(new Order(sortDirection, sort)));
        PageResp<FinanceAccountResp> rep = this.accountInfoService.list(spec, pageable, "", null);
        return new ResponseEntity<PageResp<FinanceAccountResp>>(rep, HttpStatus.OK);
    }

    /**
     * 查询账户冻结解冻列表
     */
    @RequestMapping(value = "/thawFrozen/list", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<PageResp<FinanceAccountResp>> thawFrozenList(HttpServletRequest request,
                                                                       @And({@Spec(path = "userOid", params = "userOid", spec = Like.class),
                                                                               @Spec(path = "accountType", params = "accountType", spec = Equal.class),
                                                                               @Spec(path = "relationProduct", params = "relationProduct", spec = Equal.class),
                                                                               @Spec(path = "openTime", params = "openTimeBegin", spec = DateAfterInclusive.class),
                                                                               @Spec(path = "openTime", params = "openTimeEnd", spec = DateBeforeInclusive.class),
                                                                               @Spec(path = "userType", params = "userType", spec = Equal.class)}) Specification<AccountInfoEntity> filterSpec,
                                                                       @RequestParam final String phone, @RequestParam final String frozenStatus,
                                                                       @RequestParam int page, @RequestParam int rows,
                                                                       @RequestParam(required = false, defaultValue = "updateTime") String sort,
                                                                       @RequestParam(required = false, defaultValue = "desc") String order) {

        if (page < 1) {
            page = 1;
        }
        if (rows < 1) {
            rows = 1;
        }

        Direction sortDirection = Direction.DESC;
        if (!"desc".equals(order)) {
            sortDirection = Direction.ASC;
        }

        Specification<AccountInfoEntity> spec = new Specification<AccountInfoEntity>() {
            @Override
            public Predicate toPredicate(Root<AccountInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                String[] frozenStatuss = frozenStatus.split(",");
                In<String> inFrozenStatuss = cb.in(root.get("frozenStatus").as(String.class));
                for (String fs : frozenStatuss) {
                    inFrozenStatuss.value(fs);
                }
                return cb.and(cb.notEqual(root.get("status").as(String.class), AccountInfoEntity.STATUS_DELETE), inFrozenStatuss);
            }
        };
        spec = Specifications.where(spec).and(filterSpec);

        Pageable pageable = new PageRequest(page - 1, rows, new Sort(new Order(sortDirection, sort)));
        PageResp<FinanceAccountResp> rep = this.accountInfoService.list(spec, pageable, phone, null);
        return new ResponseEntity<PageResp<FinanceAccountResp>>(rep, HttpStatus.OK);
    }

    /**
     * 账户新增审核通过
     */
    @RequestMapping(value = "/add/approve", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<BaseResp> addApprove(@RequestParam(required = true) String oid, @RequestParam(required = false) String auditComment) {
        BaseResp repponse = this.accountInfoService.addApprove(oid, auditComment);
        return new ResponseEntity<BaseResp>(repponse, HttpStatus.OK);
    }

    /**
     * 账户新增审核不通过
     */
    @RequestMapping(value = "/add/reject", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<BaseResp> addReject(@RequestParam(required = true) String oid, @RequestParam(required = true) String auditComment) {
        BaseResp repponse = this.accountInfoService.addReject(oid, auditComment);
        return new ResponseEntity<BaseResp>(repponse, HttpStatus.OK);
    }

    /**
     * 账户修改审核通过
     */
    @RequestMapping(value = "/update/approve", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<BaseResp> updateApprove(@RequestParam(required = true) String oid, @RequestParam(required = false) String auditComment) {
        BaseResp repponse = this.accountInfoService.updateApprove(oid, auditComment);
        return new ResponseEntity<BaseResp>(repponse, HttpStatus.OK);
    }

    /**
     * 账户修改审核不通过
     */
    @RequestMapping(value = "/update/reject", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<BaseResp> updateReject(@RequestParam(required = true) String oid, @RequestParam(required = true) String auditComment) {
        BaseResp repponse = this.accountInfoService.updateReject(oid, auditComment);
        return new ResponseEntity<BaseResp>(repponse, HttpStatus.OK);
    }

    /**
     * 账户封存审核通过
     */
    @RequestMapping(value = "/seal/approve", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<BaseResp> sealApprove(@RequestParam(required = true) String oid, @RequestParam(required = false) String auditComment) {
        BaseResp repponse = this.accountInfoService.sealApprove(oid, auditComment);
        return new ResponseEntity<BaseResp>(repponse, HttpStatus.OK);
    }

    /**
     * 账户封存审核不通过
     */
    @RequestMapping(value = "/seal/reject", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<BaseResp> sealReject(@RequestParam(required = true) String oid, @RequestParam(required = true) String auditComment) {
        BaseResp repponse = this.accountInfoService.sealReject(oid, auditComment);
        return new ResponseEntity<BaseResp>(repponse, HttpStatus.OK);
    }

    /**
     * 账户冻结审核通过
     */
    @RequestMapping(value = "/frozen/approve", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<BaseResp> frozenApprove(@RequestParam(required = true) String oid, @RequestParam(required = false) String auditComment) {
        BaseResp repponse = this.accountInfoService.frozenApprove(oid, auditComment);
        return new ResponseEntity<BaseResp>(repponse, HttpStatus.OK);
    }

    /**
     * 账户冻结审核不通过
     */
    @RequestMapping(value = "/frozen/reject", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<BaseResp> frozenReject(@RequestParam(required = true) String oid, @RequestParam(required = true) String auditComment) {
        BaseResp repponse = this.accountInfoService.frozenReject(oid, auditComment);
        return new ResponseEntity<BaseResp>(repponse, HttpStatus.OK);
    }

    /**
     * 账户解冻审核通过
     */
    @RequestMapping(value = "/thaw/approve", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<BaseResp> thawApprove(@RequestParam(required = true) String oid, @RequestParam(required = false) String auditComment) {
        BaseResp repponse = this.accountInfoService.thawApprove(oid, auditComment);
        return new ResponseEntity<BaseResp>(repponse, HttpStatus.OK);
    }

    /**
     * 账户解冻审核不通过
     */
    @RequestMapping(value = "/thaw/reject", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<BaseResp> thawReject(@RequestParam(required = true) String oid, @RequestParam(required = true) String auditComment) {
        BaseResp repponse = this.accountInfoService.thawReject(oid, auditComment);
        return new ResponseEntity<BaseResp>(repponse, HttpStatus.OK);
    }

    /**
     * 根据用户id获取账户余额
     */
    @RequestMapping(value = "/getAccountBalanceByUserOid", method = RequestMethod.POST)
    public AccountBalanceResponse getAccountBalanceByUserOid(@RequestBody InteractiveRequest req) {
        AccountBalanceResponse resp = accountInfoService.getAccountBalanceByUserOid(req.getUserOid());
        return resp;
    }

    /**
     * 根据用户id和账户类型获取发现行人账户余额
     */
    @RequestMapping(value = "/getPublisherAccountBalanceByUserOid", method = RequestMethod.POST)
    public PublisherAccountBalanceResponse getPublisherAccountBalanceByUserOid(@RequestBody PublisherAccountQueryRequest req) {
        PublisherAccountBalanceResponse resp = accountInfoService.getPublisherAccountBalanceByUserOid(req.getUserOid(), req.getAccountType());
        return resp;
    }
    
    /**
     * 创建产品户
     * @param req
     * @return
     */
	@RequestMapping(value = "/add/product", method = RequestMethod.POST)
	public CreateAccountResponse createProductAccount(@RequestBody CreateAccountRequest req) {
		log.info("创建产品户接口请求参数：CreateAccountRequest = {}" + req);
		CreateAccountResponse result = valCreateProductAccount(req);
		if (!Constant.SUCCESS.equals(result.getReturnCode())) {
			log.debug(result.getErrorMessage());
			return result;
		}
		req.setAccountType(AccountTypeEnum.PRODUCT.getCode());
		req.setUserType(UserTypeEnum.PUBLISHER.getCode());
		return accountInfoService.addAccount(req);
	}
    
	/**
	 * 查询产品户可用余额
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/queryProductAccountBalance", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<ProductAccountListResponse> queryProductAccountBalance(
			@RequestBody ProductAccountRequest req) {
		log.info("查询产品户可用余额接口入参：productAccountRequest = {}", req);
		ProductAccountListResponse result = this.accountInfoService.queryProductAccountBalance(req);
		return new ResponseEntity<ProductAccountListResponse>(result, HttpStatus.OK);
	}
	
	/**
	 * 查询平台户基本信息
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/platformAccountInfo", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<PlatformAccountInfoResponse> platformAccountInfo(
			@RequestBody PlatformAccountInfoRequest req) {
		log.info("查询平台户基本信息，请求参数: {}", req);
		PlatformAccountInfoResponse result = this.accountInfoService.platformAccountInfo(req);
		log.info("查询平台户基本信息，返回结果: {}", result);
		return new ResponseEntity<PlatformAccountInfoResponse>(result, HttpStatus.OK);
	}
	
	/**
	 * 平台备付金详情查询
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/platformReservedAccountDetail", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<PlatformReservedAccountDetailResponse> platformReservedAccountDetail(
			@RequestBody PlatformReservedAccountDetailRequest req) {
		log.info("平台备付金详情查询，请求参数: {}", req);
		PlatformReservedAccountDetailResponse result = this.accountInfoService.platformReservedAccountDetail(req);
		log.info("平台备付金详情查询，返回结果: {}", result);
		return new ResponseEntity<PlatformReservedAccountDetailResponse>(result, HttpStatus.OK);
	}
	
	/**
	 * 查询发行人账户信息
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/publisherAccountInfo", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<PublisherAccountInfoResponse> publisherAccountInfo(
			@RequestBody PublisherAccountInfoRequest req) {
		log.info("查询发行人账户信息，请求参数: {}", req);
		PublisherAccountInfoResponse result = this.accountInfoService.publisherAccountInfo(req);
		log.info("查询发行人账户信息，返回结果: {}", result);
		return new ResponseEntity<PublisherAccountInfoResponse>(result, HttpStatus.OK);
	}
	
}