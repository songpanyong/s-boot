package com.guohuai.boot.account.service.accountinfo;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.CreateAccountRequest;
import com.guohuai.account.api.request.UpdateAccountForm;
import com.guohuai.account.api.response.CreateAccountResponse;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.basic.component.ext.web.BaseResp;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.service.UserInfoService;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.CodeConstants;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.UserTypeEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 账户基本操作
 * @author ZJ
 * @date 2018年1月19日 下午3:15:37
 * @version V1.0
 */
@Slf4j
@Service
public class AccountInfoBaseService {
	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private UserInfoDao userInfoDao;
	@Autowired
	private UserInfoService userInfoService;
	@Autowired
	private SeqGenerator seqGenerator;
	@Autowired
	private AccountInfoQueryService accountInfoQueryService;

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
		// account.setRelationProduct(relationProduct);
		account.setAccountName(userType.getName() + AccountTypeEnum.BASICER.getName());
		account.setOpenTime(nowTime);
		// account.setOpenOperator(openOperator);
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
	 * 新增账户(创建子账户)
	 */
	@Transactional
	public CreateAccountResponse saveAccount(CreateAccountRequest req, String status) {
		log.info("新增账户:[" + JSONObject.toJSONString(req) + "]");

		CreateAccountResponse resp = new CreateAccountResponse();

		resp = this.accountInfoQueryService.checkAccountType(req);
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
		account.setAccountName(
				UserTypeEnum.getEnumName(req.getUserType()) + AccountTypeEnum.getEnumName(req.getAccountType()));
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
	 * 更新账户
	 * @param form
	 * @return
	 */
	@Transactional
	public BaseResp edit(UpdateAccountForm form) {
		BaseResp response = new BaseResp();
		log.info("编辑账户参数,form:{}", JSON.toJSONString(form));

		// 验证参数
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

		if (a != null
				&& (a.getAuditStatus() == null || !a.getAuditStatus().equals(AccountInfoEntity.AUDIT_STATUS_SUBMIT))) {

			List<AccountInfoEntity> aies = null;
			if (StringUtil.isEmpty(form.getRelationProduct())) {
				aies = accountInfoDao.findByUserOidAndAccountTypeAndUserTypeNoProduct(a.getUserOid(), a.getUserType(),
						a.getAccountType());
			} else {
				aies = accountInfoDao.findByUserOidAndAccountTypeAndProductNo(a.getUserOid(), a.getUserType(),
						form.getRelationProduct(), a.getAccountType());
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
			// a.setRelationProductName(form.getRelationProductName());
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
	 * @param form
	 * @return
	 */
	@Transactional
	public BaseResp update(UpdateAccountForm form) {
		BaseResp response = new BaseResp();
		log.info("更新账户参数,form:{}", JSON.toJSONString(form));

		// 验证参数
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
				aies = accountInfoDao.findByUserOidAndAccountTypeAndUserTypeNoProduct(a.getUserOid(), a.getUserType(),
						form.getAccountType());
			} else {
				aies = accountInfoDao.findByUserOidAndAccountTypeAndProductNo(a.getUserOid(), a.getUserType(),
						form.getRelationProduct(), form.getAccountType());
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
		log.info("addApprove,oid:{},auditComment:{}", new Object[] { oid, auditComment });
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
		log.info("addReject,oid:{},auditComment:{}", new Object[] { oid, auditComment });
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
		log.info("updateApprove,oid:{},auditComment:{}", new Object[] { oid, auditComment });
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
		log.info("updateReject,oid:{},auditComment:{}", new Object[] { oid, auditComment });
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
		log.info("sealApprove,oid:{},auditComment:{}", new Object[] { oid, auditComment });
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
		log.info("sealReject,oid:{},auditComment:{}", new Object[] { oid, auditComment });
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
		log.info("thawApprove,oid:{},auditComment:{}", new Object[] { oid, auditComment });
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
		log.info("thawReject,oid:{},auditComment:{}", new Object[] { oid, auditComment });
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
		log.info("frozenApprove,oid:{},auditComment:{}", new Object[] { oid, auditComment });
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
		log.info("frozenReject,oid:{},auditComment:{}", new Object[] { oid, auditComment });
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
}