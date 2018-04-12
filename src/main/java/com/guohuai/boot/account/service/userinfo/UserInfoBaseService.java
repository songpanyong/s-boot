package com.guohuai.boot.account.service.userinfo;

import java.sql.Timestamp;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.CreateAccountRequest;
import com.guohuai.account.api.request.CreatePasswordRequest;
import com.guohuai.account.api.request.CreateUserRequest;
import com.guohuai.account.api.request.ModifyPasswordRequest;
import com.guohuai.account.api.request.SaveUserForm;
import com.guohuai.account.api.response.CreatePasswordResponse;
import com.guohuai.account.api.response.CreateUserResponse;
import com.guohuai.account.api.response.ModifyPasswordResponse;
import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.service.AccountInfoService;
import com.guohuai.boot.account.util.MD5Utils;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.CodeConstants;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.UserTypeEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 用户信息基本服务
 * @author ZJ
 * @date 2018年1月19日 下午5:49:24
 * @version V1.0
 */
@Slf4j
@Service
@Transactional
public class UserInfoBaseService {
	@Autowired
	private UserInfoDao userInfoDao;
	@Autowired
	private SeqGenerator seqGenerator;
	@Autowired
	private AccountInfoService accountInfoService;

	/**
	 * 新增用户
	 */
	public CreateUserResponse addUser(CreateUserRequest createUserReq) {
		log.info("新增用户参数,CreatePasswordRequest:{}", JSON.toJSONString(createUserReq));
		// 返回参数
		CreateUserResponse resp = new CreateUserResponse();
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");

		// 验证参数
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
		if (StringUtil.isEmpty(UserTypeEnum.getEnumName(createUserReq.getUserType()))) {
			// 用户类型不存在
			resp.setReturnCode(Constant.USERTYPENOTEXISTS);
			resp.setErrorMessage("用户类型不存在！");
			log.error("用户类型不存在，[userType=" + createUserReq.getUserType() + "]");
			return resp;
		}

		resp.setUserType(createUserReq.getUserType());
		resp.setStatus(AccountInfoEntity.STATUS_SUBMIT);

		if (UserTypeEnum.PLATFORMER.getCode().equals(createUserReq.getUserType())) {// 同一个系统来源的平台只能创建一个
			List<UserInfoEntity> uies = userInfoDao.findByUserTypeSystemSource(createUserReq.getUserType(),
					createUserReq.getSystemSource());
			if (uies != null && uies.size() > 0) {
				log.info("用户已存在，不需要新建 ,userOid{}", JSON.toJSONString(uies.get(0)));
				resp.setReturnCode(Constant.SUCCESS);
				resp.setErrorMessage("成功");
				resp.setUserOid(uies.get(0).getUserOid());
				return resp;
			}
		}

		Timestamp nowTime = new Timestamp(System.currentTimeMillis());
		UserInfoEntity entity = new UserInfoEntity();
		// 传入参数转换
		BeanUtils.copyProperties(createUserReq, entity);
		entity.setOid(StringUtil.uuid());
		entity.setUserType(createUserReq.getUserType());

		// 20170615修改用户ID使用业务系统ID
		// String userOid = this.seqGenerator.next(CodeConstants.USER_OID_PREFIX);
		String userOid = createUserReq.getSystemUid();
		// 查询是否已存在
		UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(userOid);
		if (userInfoEntity != null) {
			log.info("用户已存在，不需要新建 ,userOid{}", userOid);
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("用户已存在");
			resp.setUserOid(userOid);
			return resp;
		}
		entity.setUserOid(userOid);
		entity.setCreateTime(nowTime);
		entity.setUpdateTime(nowTime);
		userInfoDao.save(entity);

		// 创建默认账户
		if (UserTypeEnum.INVESTOR.getCode().equals(createUserReq.getUserType())) {
			AccountInfoEntity account = accountInfoService.addBaseAccount(userOid, createUserReq.getRemark(),
					UserTypeEnum.INVESTOR);
			resp.setAccountName(account.getAccountName());
			resp.setAccountNo(account.getAccountNo());
		}
		// 发行人创建默认账户
		if (UserTypeEnum.PUBLISHER.getCode().equals(createUserReq.getUserType())) {
			// 基本户
			AccountInfoEntity account = accountInfoService.addBaseAccount(userOid, createUserReq.getRemark(),
					UserTypeEnum.PUBLISHER);
			resp.setAccountName(account.getAccountName());
			resp.setAccountNo(account.getAccountNo());
			CreateAccountRequest req = new CreateAccountRequest();
			req.setUserOid(userOid);
			req.setUserType(UserTypeEnum.PUBLISHER.getCode());
			// 归集清算户
			req.setAccountType(AccountTypeEnum.COLLECTION_SETTLEMENT.getCode());
			accountInfoService.saveAccount(req, AccountInfoEntity.STATUS_SUBMIT);
			// 冻结资金户
			req.setAccountType(AccountTypeEnum.REDEEMFROZEN.getCode());
			accountInfoService.saveAccount(req, AccountInfoEntity.STATUS_SUBMIT);
			// 可用金户
			req.setAccountType(AccountTypeEnum.AVAILABLE_AMOUNT.getCode());
			accountInfoService.saveAccount(req, AccountInfoEntity.STATUS_SUBMIT);
		}
		resp.setUserOid(userOid);
		return resp;
	}

	/**
	 * 设置密码
	 * @param @return
	 * @return NewPasswordResponse
	 * @throws @Title:
	 *             setPassword
	 * @Description:
	 */
	public CreatePasswordResponse setPassword(CreatePasswordRequest req) {
		// 返回参数
		CreatePasswordResponse resp = new CreatePasswordResponse();
		try {
			log.info("设置密码参数,userOid:{},md5Password:{}", req.getUserOid(), MD5Utils.encryptByMd5(req.getPassword()));

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
				// 加密
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
			log.error("设置密码异常", e);
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("设置密码异常");
			return resp;
		}

	}

	/**
	 * 修改密码
	 * @param @param
	 *            req
	 * @param @return
	 * @return ModifyPasswordResponse
	 * @throws @Title:
	 *             modifyPassword
	 * @Description:
	 */
	public ModifyPasswordResponse modifyPassword(ModifyPasswordRequest req) {
		// 返回参数
		ModifyPasswordResponse resp = new ModifyPasswordResponse();
		try {
			log.info("修改密码参数,userOid:{},md5oldPassword:{},md5newPassword:{}", req.getUserOid(),
					MD5Utils.encryptByMd5(req.getOldPassword()), MD5Utils.encryptByMd5(req.getNewPassword()));

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
				// 原密码加密和数据库的密码比较
				String encryptOldPassword = MD5Utils.encryptByMd5(oldPassword);
				if (encryptOldPassword.equals(entity.getPassword())) {
					// 加密
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
			log.error("修改密码异常", e);
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("修改密码异常");
			return resp;
		}
	}

	/**
	 * 新加用户
	 * @param form
	 * @return
	 */
	@Transactional
	public CreateUserResponse save(SaveUserForm form) {
		log.info("新加用户参数,form:{}", JSON.toJSONString(form));

		CreateUserResponse resp = new CreateUserResponse();

		// 验证参数
		if (StringUtil.isEmpty(form.getName())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("用户名称不能为空");
			return resp;
		}
		if (!UserTypeEnum.PUBLISHER.getCode().equals(form.getUserType())
				&& !UserTypeEnum.PLATFORMER.getCode().equals(form.getUserType())) {
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
	 * @param form
	 * @return
	 */
	@Transactional
	public CreateUserResponse update(SaveUserForm form) {
		log.info("更新用户参数,form:{}", JSON.toJSONString(form));

		CreateUserResponse resp = new CreateUserResponse();

		// 验证参数
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

	/**
	 * 修改手机号
	 * @param @param
	 *            req
	 * @param @return
	 * @return ModifyPasswordResponse
	 * @throws @Title:
	 *             modifyPassword
	 * @Description:
	 */
	@SuppressWarnings("unused")
	public CreateUserResponse modifyPhone(CreateUserRequest req) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		log.info("修改用户手机号,{}", req.getUserOid(), JSONObject.toJSONString(req));
		// 返回参数
		CreateUserResponse resp = new CreateUserResponse();
		BeanUtils.copyProperties(req, resp);
		try {
			if (StringUtil.isEmpty(req.getUserOid()) && StringUtil.isEmpty(req.getPhone())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("请输入要修改手机号的用户和手机号");
				log.info("修改手机号返回回页面端数据：{}", JSONObject.toJSONString(resp));
				return resp;
			}
			UserInfoEntity entity = userInfoDao.findByUserOid(req.getUserOid());
			if (entity != null) {
				String remark = "修改用户的手机号";
				userInfoDao.updatePhone(req.getPhone(), remark, entity.getOid());
				resp.setUserOid(req.getUserOid());
				resp.setReturnCode(Constant.SUCCESS);
				resp.setErrorMessage("修改成功");
				log.info("修改手机号返回回页面端数据：{}", JSONObject.toJSONString(resp));
				return resp;
			} else {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("userOid不存在，无法修改");
				log.info("修改手机号返回回页面端数据：{}", JSONObject.toJSONString(resp));
				return resp;
			}
		} catch (Exception e) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("修改手机号异常");
			log.error("修改手机号异常", e);
			return resp;
		}
	}
}