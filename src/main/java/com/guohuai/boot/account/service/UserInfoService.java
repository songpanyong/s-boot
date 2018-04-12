package com.guohuai.boot.account.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.*;
import com.guohuai.account.api.response.*;
import com.guohuai.account.api.response.entity.UserDto;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.basic.component.ext.web.PageResp;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.util.MD5Utils;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.CodeConstants;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.UserTypeEnum;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.util.*;

@Service
public class UserInfoService {

	private final static Logger logger = LoggerFactory.getLogger(UserInfoService.class);

	@Autowired
	private UserInfoDao userInfoDao;
	@Autowired
	private SeqGenerator seqGenerator;
	@Autowired
	private AccountInfoService accountInfoService;

	/**
	 * 新增用户
     * 返回默认发行人产品户
	 */
	@Transactional
	public CreateUserResponse addUser(CreateUserRequest createUserReq) {
		logger.info("新增用户参数,CreatePasswordRequest:{}", JSON.toJSONString(createUserReq));
		//返回参数
		CreateUserResponse resp = new CreateUserResponse();
		resp.setReturnCode(Constant.FAIL);
			//验证参数
		if (StringUtil.isEmpty(createUserReq.getSystemUid())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("用户业务系统ID不能为空");
			return resp;
		}
		if (StringUtil.isEmpty(createUserReq.getSystemSource())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("来源系统类型不能为空");
			return resp;
		}
		if(StringUtil.isEmpty(UserTypeEnum.getEnumName(createUserReq.getUserType()))){
			//用户类型不存在
			resp.setReturnCode(Constant.USERTYPENOTEXISTS);
			resp.setErrorMessage("用户类型不存在！");
			logger.error("用户类型不存在，[userType=" + createUserReq.getUserType() + "]");
			return resp;
		}

		resp.setUserType(createUserReq.getUserType());
		resp.setStatus(AccountInfoEntity.STATUS_SUBMIT);

		//同一个系统来源的平台只能创建一个
		if(UserTypeEnum.PLATFORMER.getCode().equals(createUserReq.getUserType())) {
			List<UserInfoEntity> uies = userInfoDao.findByUserTypeSystemSource(createUserReq.getUserType(), createUserReq.getSystemSource());
			if(uies!=null && uies.size()>0) {
				logger.info("用户已存在，不需要新建 ,userOid{}",JSON.toJSONString(uies.get(0)));
				resp.setReturnCode(Constant.SUCCESS);
				resp.setErrorMessage("成功");
				resp.setUserOid(uies.get(0).getUserOid());
				return resp;
			}
		}

		Timestamp nowTime = new Timestamp(System.currentTimeMillis());
		UserInfoEntity entity = new UserInfoEntity();
		//传入参数转换
		BeanUtils.copyProperties(createUserReq, entity);
		entity.setOid(StringUtil.uuid());
		entity.setUserType(createUserReq.getUserType());

		//20170615修改用户ID使用业务系统ID
//		String userOid = this.seqGenerator.next(CodeConstants.USER_OID_PREFIX);
		String userOid = createUserReq.getSystemUid();
		//查询是否已存在
		UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(userOid);
		if(userInfoEntity != null){
			logger.info("用户已存在，不需要新建 ,userOid{}", userOid);
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("用户已存在");
			resp.setUserOid(userOid);
			return resp;
		}
		entity.setUserOid(userOid);
		entity.setCreateTime(nowTime);
		entity.setUpdateTime(nowTime);
		userInfoDao.save(entity);

		//创建默认账户
		if(UserTypeEnum.INVESTOR.getCode().equals(createUserReq.getUserType())) {
			AccountInfoEntity account = accountInfoService.addBaseAccount(userOid, createUserReq.getRemark(),UserTypeEnum.INVESTOR);
			resp.setAccountName(account.getAccountName());
			resp.setAccountNo(account.getAccountNo());
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("创建成功");
		}
		//发行人创建默认账户
		if (UserTypeEnum.PUBLISHER.getCode().equals(createUserReq.getUserType())) {
			// 基本户
			AccountInfoEntity account = accountInfoService.addBaseAccount(userOid, createUserReq.getRemark(),UserTypeEnum.PUBLISHER);
			CreateAccountRequest req = new CreateAccountRequest();
			req.setUserOid(userOid);
			req.setUserType(UserTypeEnum.PUBLISHER.getCode());
			// 归集清算户
			req.setAccountType(AccountTypeEnum.COLLECTION_SETTLEMENT.getCode());
			accountInfoService.saveAccount(req, AccountInfoEntity.STATUS_SUBMIT);
			// 冻结资金户
			req.setAccountType(AccountTypeEnum.REDEEMFROZEN.getCode());
			accountInfoService.saveAccount(req, AccountInfoEntity.STATUS_SUBMIT);
			// 体现冻结户
			req.setAccountType(AccountTypeEnum.FROZEN.getCode());
			accountInfoService.saveAccount(req, AccountInfoEntity.STATUS_SUBMIT);
			// 发行人默认产品户
			req.setAccountType(AccountTypeEnum.PRODUCT.getCode());
			req.setRelationProductName("default");
			CreateAccountResponse createAccountResponse=accountInfoService.saveAccount(req, AccountInfoEntity.STATUS_SUBMIT);
			if(BaseResponse.isSuccess(createAccountResponse)){
             resp.setAccountNo(createAccountResponse.getAccountNo());
             resp.setReturnCode(Constant.SUCCESS);
             resp.setAccountName("默认发行人产品户");
			}
		}
		resp.setUserOid(userOid);
		return resp;
	}


	/**
	 * 设置密码
	 *
	 * @param @return
	 * @return NewPasswordResponse
	 * @throws
	 * @Title: setPassword
	 * @Description:
	 */
	public CreatePasswordResponse setPassword(CreatePasswordRequest req) {
		//返回参数
		CreatePasswordResponse resp = new CreatePasswordResponse();
		try {
			logger.info("设置密码参数,userOid:{},md5Password:{}", req.getUserOid(), MD5Utils.encryptByMd5(req.getPassword()));

			String userOid = req.getUserOid();
			String password = req.getPassword();

			if (StringUtil.isEmpty(userOid)) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("userOid不能为空");
				return resp;
			}
			if (StringUtil.isEmpty(password)) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("密码不能为空");
				return resp;
			}

			UserInfoEntity entity = userInfoDao.findByUserOid(userOid);
			if (entity != null) {
				//加密
				String encryptPassword = MD5Utils.encryptByMd5(password);
				userInfoDao.updatePassword(entity.getOid(), encryptPassword);

				resp.setUserOid(userOid);
				resp.setReturnCode(Constant.SUCCESS);
				resp.setErrorMessage("成功");
				return resp;
			} else {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("userOid不存在");
				return resp;
			}
		} catch (Exception e) {
			logger.error("设置密码异常",e);
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("设置密码异常");
			return resp;
		}

	}

	/**
	 * 修改密码
	 *
	 * @param @param  req
	 * @param @return
	 * @return ModifyPasswordResponse
	 * @throws
	 * @Title: modifyPassword
	 * @Description:
	 */
	public ModifyPasswordResponse modifyPassword(ModifyPasswordRequest req) {
		//返回参数
		ModifyPasswordResponse resp = new ModifyPasswordResponse();
		try {
			logger.info("修改密码参数,userOid:{},md5oldPassword:{},md5newPassword:{}", req.getUserOid(), MD5Utils.encryptByMd5(req.getOldPassword()), MD5Utils.encryptByMd5(req.getNewPassword()));

			String userOid = req.getUserOid();
			String oldPassword = req.getOldPassword();
			String newPassword = req.getNewPassword();

			if (StringUtil.isEmpty(userOid)) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("userOid不能为空");
				return resp;
			}
			if (StringUtil.isEmpty(oldPassword)) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("原密码不能为空");
				return resp;
			}
			if (StringUtil.isEmpty(newPassword)) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("新密码不能为空");
				return resp;
			}

			UserInfoEntity entity = userInfoDao.findByUserOid(userOid);
			if (entity != null) {
				//原密码加密和数据库的密码比较
				String encryptOldPassword = MD5Utils.encryptByMd5(oldPassword);
				if (encryptOldPassword.equals(entity.getPassword())) {
					//加密
					String encryptPassword = MD5Utils.encryptByMd5(newPassword);
					userInfoDao.updatePassword(entity.getOid(), encryptPassword);

					resp.setUserOid(userOid);
					resp.setReturnCode(Constant.SUCCESS);
					resp.setErrorMessage("成功");
					return resp;
				} else {
					resp.setReturnCode(Constant.FAIL);
					resp.setErrorMessage("原密码不对,不能修改密码");
					return resp;
				}
			} else {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("userOid不存在");
				return resp;
			}
		} catch (Exception e) {
			logger.error("修改密码异常",e);
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("修改密码异常");
			return resp;
		}
	}


	/**
	 * 校验密码
	 *
	 * @param @param  req
	 * @param @return
	 * @return ValidatePasswordResponse
	 * @throws
	 * @Title: validatePassword
	 * @Description:
	 */
	public ValidatePasswordResponse validatePassword(ValidatePasswordRequest req) {
		//返回参数
		ValidatePasswordResponse resp = new ValidatePasswordResponse();
		try {
			logger.info("校验密码参数,userOid:{},md5Password:{}", req.getUserOid(), MD5Utils.encryptByMd5(req.getPassword()));

			String userOid = req.getUserOid();
			String password = req.getPassword();

			if (StringUtil.isEmpty(userOid)) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("userOid不能为空");
				return resp;
			}
			if (StringUtil.isEmpty(password)) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("密码不能为空");
				return resp;
			}

			UserInfoEntity entity = userInfoDao.findByUserOid(userOid);
			if (entity != null) {
				//原密码加密和数据库的密码比较
				String encryptPassword = MD5Utils.encryptByMd5(password);
				if (encryptPassword.equals(entity.getPassword())) {
					resp.setUserOid(userOid);
					resp.setReturnCode(Constant.SUCCESS);
					resp.setErrorMessage("密码校验成功");
					return resp;
				} else {
					resp.setReturnCode(Constant.FAIL);
					resp.setErrorMessage("密码校验失败");
					return resp;
				}
			} else {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("userOid不存在");
				return resp;
			}
		} catch (Exception e) {
			logger.error("校验密码异常",e);
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("校验密码异常");
			return resp;
		}
	}


	/**
	 * 根据userOid获取用户
	 *
	 * @param @param  userOid
	 * @param @return
	 * @return UserInfoEntity
	 * @throws
	 * @Title: getAccountUserByUserOid
	 * @Description:
	 */
	public UserInfoEntity getAccountUserByUserOid(String userOid) {
		UserInfoEntity entity = userInfoDao.findByUserOid(userOid);
		return entity;
	}

	/**
	 * 查询用户
	 *
	 * @return
	 */
	public UserListResponse userQueryList(Specification<UserInfoEntity> spec) {
		List<UserInfoEntity> us = null;
		if (spec != null) {
			us = this.userInfoDao.findAll(spec);
		} else {
			us = this.userInfoDao.findAll();
		}

		if (null != us && us.size() != 0) {
			List<UserDto> infos = new ArrayList<UserDto>();
			for (UserInfoEntity entity : us) {
				UserDto tempEntity = new UserDto();
				BeanUtils.copyProperties(entity, tempEntity, new String[]{"createTime"});
				tempEntity.setCreateTime(DateUtil.format(entity.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
				infos.add(tempEntity);
			}
			return new UserListResponse(infos);
		}
		return new UserListResponse();
	}

	/**
	 * 新加用户
	 *
	 * @param form
	 * @return
	 */
	@Transactional
	public CreateUserResponse save(SaveUserForm form) {
		logger.info("新加用户参数,form:{}", JSON.toJSONString(form));

		CreateUserResponse resp = new CreateUserResponse();

		//验证参数
		if (StringUtil.isEmpty(form.getName())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("用户名称不能为空");
			return resp;
		}
		if (!UserTypeEnum.PUBLISHER.getCode().equals(form.getUserType()) && !UserTypeEnum.PLATFORMER.getCode().equals(form.getUserType())) {
			resp.setReturnCode(Constant.USERTYPENOTEXISTS);
			resp.setErrorMessage("用户类型不存在！");
			return resp;
		}

		Timestamp nowTime = new Timestamp(System.currentTimeMillis());

		UserInfoEntity userEntity = new UserInfoEntity();
		userEntity.setOid(StringUtil.uuid());
		userEntity.setSystemSource(Constant.SYSTEM_SOURCE);
		userEntity.setUserType(form.getUserType());
		userEntity.setName(form.getName());
		userEntity.setPhone(form.getPhone());
		userEntity.setRemark(form.getRemark());
		String userOid = this.seqGenerator.next(CodeConstants.USER_OID_PREFIX);
		userEntity.setUserOid(userOid);
		userEntity.setCreateTime(nowTime);
		userEntity.setUpdateTime(nowTime);
		userInfoDao.save(userEntity);

		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");
		return resp;
	}

	/**
	 * 更新用户
	 *
	 * @param form
	 * @return
	 */
	@Transactional
	public CreateUserResponse update(SaveUserForm form) {
		logger.info("更新用户参数,form:{}", JSON.toJSONString(form));

		CreateUserResponse resp = new CreateUserResponse();

		//验证参数
		if (StringUtil.isEmpty(form.getOid())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("OID不能为空");
			return resp;
		}
		if (StringUtil.isEmpty(form.getName())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("用户名称不能为空");
			return resp;
		}

		UserInfoEntity userEntity = this.userInfoDao.findOne(form.getOid());

		userEntity.setName(form.getName());
		userEntity.setPhone(form.getPhone());
		userEntity.setRemark(form.getRemark());
		userEntity.setUpdateTime(new Timestamp(System.currentTimeMillis()));

		userInfoDao.saveAndFlush(userEntity);

		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");
		return resp;
	}

	public FinanceUserResp read(String oid) {
		logger.info("获取用户详情,oid:{}", oid);
		UserInfoEntity userEntity = this.userInfoDao.findOne(oid);
		logger.info("获取用户详情,UserInfoEntity:{}", JSON.toJSONString(userEntity));
		FinanceUserResp fur = userEntityTranforToResp(userEntity);
		fur.setReturnCode(Constant.SUCCESS);
		logger.info("转化后用户详情,FinanceUserResp:{}", JSON.toJSONString(fur));

		return fur;
	}

	private FinanceUserResp userEntityTranforToResp(UserInfoEntity userEntity) {
		FinanceUserResp fur = new FinanceUserResp();
		fur.setOid(userEntity.getOid());
		fur.setUserType(userEntity.getUserType());
		fur.setUserOid(userEntity.getUserOid()); // 用户ID
		fur.setSystemUid(userEntity.getSystemUid()); // 用户业务系统ID
		fur.setSystemSource(userEntity.getSystemSource()); // 来源系统
		fur.setName(userEntity.getName()); // 姓名
		fur.setIdCard(userEntity.getIdCard()); // 身份证号
		fur.setBankName(userEntity.getBankName()); // 开户行
		fur.setCardNo(userEntity.getCardNo()); // 银行账号
		fur.setPhone(userEntity.getPhone()); // 手机号
		fur.setRemark(userEntity.getRemark()); // 备注
		fur.setUpdateTime(userEntity.getUpdateTime()); // 更新时间
		fur.setCreateTime(userEntity.getCreateTime()); // 创建时间
		return fur;
	}

	/**
	 * 获取用户列表
	 *
	 * @param spec
	 * @param pageable
	 * @return 如果返回的errCode属性等于0表示成功，否则表示失败，失败原因在errMessage里面体现
	 */
	public PageResp<FinanceUserResp> list(Specification<UserInfoEntity> spec, Pageable pageable) {
		logger.info("获取用户列表,spec:{},pageable:{}", new Object[]{JSON.toJSONString(spec), JSON.toJSONString(pageable)});
		Page<UserInfoEntity> cas = this.userInfoDao.findAll(spec, pageable);
		PageResp<FinanceUserResp> pagesRep = new PageResp<FinanceUserResp>();
		if (cas != null && cas.getContent() != null && cas.getTotalElements() > 0) {
			List<FinanceUserResp> rows = new ArrayList<FinanceUserResp>();
			Object[] users = userInfoDao.findAllUserName();
			for (UserInfoEntity u : cas) {
				FinanceUserResp queryRep = userEntityTranforToResp(u);
				String name = u.getName();
				if(StringUtils.isEmpty(u.getName())){
					for(Object user : users){
						Object[] aname = (Object[])user;
						String uid = aname[0].toString();
						String uname = aname[1].toString();
						if(!StringUtils.isEmpty(u.getUserOid()) && u.getUserOid().equals(uid)){
							u.setName(uname);
						}
					}
				}
				queryRep.setName(name);
				rows.add(queryRep);
			}
			pagesRep.setRows(rows);
		}
		pagesRep.setTotal(cas.getTotalElements());
		return pagesRep;
	}

	public List<JSONObject> getUsers() {
		List<JSONObject> jsonObjList = new ArrayList<JSONObject>();
		List<UserInfoEntity> us = this.userInfoDao.findAll();
		if (us != null && us.size() > 0) {
			JSONObject jsonObj = null;
			for (UserInfoEntity u : us) {
				jsonObj = new JSONObject();
				jsonObj.put("oid", u.getOid());
				jsonObj.put("name", u.getName());
				jsonObjList.add(jsonObj);
			}
		}
		return jsonObjList;
	}

	public Map<String, UserInfoEntity> findUsers(final Set<String> userOids) {
		//查询标签
		Specification<UserInfoEntity> spec = new Specification<UserInfoEntity>() {
			@Override
			public Predicate toPredicate(Root<UserInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				In<String> inUserOids = cb.in(root.get("userOid").as(String.class));
				for (String userOid : userOids) {
					inUserOids.value(userOid);
				}
				return inUserOids;
			}
		};
		spec = Specifications.where(spec);
		List<UserInfoEntity> us = userInfoDao.findAll(spec);
		Map<String, UserInfoEntity> userMap = new HashMap<String, UserInfoEntity>();
		if (us != null && us.size() > 0) {
			for (UserInfoEntity u : us) {
				userMap.put(u.getUserOid(), u);
			}
		}
		return userMap;
	}

	public List<UserInfoEntity> getUsers(final String phone) {
		Specification<UserInfoEntity> spec = new Specification<UserInfoEntity>() {
			@Override
			public Predicate toPredicate(Root<UserInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.like(root.get("phone").as(String.class), phone);
			}
		};
		spec = Specifications.where(spec);
		List<UserInfoEntity> us = userInfoDao.findAll(spec);

		return us;
	}
	
	/**
	 * 根据用户手机号获取用户信息
	 * @param phone
	 * @return
	 */
	public UserInfoEntity getAccountUserByPhone(String phone) {
		UserInfoEntity entity = userInfoDao.findByPhone(phone);
		return entity;
	}

	/**
	 * 修改手机号
	 *
	 * @param @param  req
	 * @param @return
	 * @return ModifyPasswordResponse
	 * @throws
	 * @Title: modifyPassword
	 * @Description:
	 */
	@SuppressWarnings("unused")
	public CreateUserResponse modifyPhone(CreateUserRequest req) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		logger.info("修改用户手机号,{}", req.getUserOid(), JSONObject.toJSONString(req));
		//返回参数
		CreateUserResponse resp = new CreateUserResponse();
		BeanUtils.copyProperties(req, resp);
		try {
			if(StringUtil.isEmpty(req.getUserOid())&&StringUtil.isEmpty(req.getPhone())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("请输入要修改手机号的用户和手机号");
				logger.info("修改手机号返回回页面端数据：{}", JSONObject.toJSONString(resp));
				return resp;
			}
			UserInfoEntity entity = userInfoDao.findByUserOid(req.getUserOid());
			if (entity != null) {
				String remark="修改用户的手机号";
				userInfoDao.updatePhone(req.getPhone(),remark,entity.getOid());
				resp.setUserOid(req.getUserOid());
				resp.setReturnCode(Constant.SUCCESS);
				resp.setErrorMessage("修改成功");
				logger.info("修改手机号返回回页面端数据：{}", JSONObject.toJSONString(resp));
				return resp;
			} else {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("userOid不存在，无法修改");
				logger.info("修改手机号返回回页面端数据：{}", JSONObject.toJSONString(resp));
				return resp;
			}
		} catch (Exception e) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("修改手机号异常");
			logger.error("修改手机号异常", e);
			return resp;
		}
	}

}