package com.guohuai.boot.account.service.platformchangerecords;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.response.CreateTransResponse;
import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.*;
import com.guohuai.boot.account.entity.*;
import com.guohuai.boot.account.form.PlatformChangeRecordsForm;
import com.guohuai.boot.account.service.AccountInfoService;
import com.guohuai.boot.account.service.TransService;
import com.guohuai.component.exception.SETException;
import com.guohuai.component.util.*;
import com.guohuai.settlement.api.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @Description: TODO
 * @author ZJ
 * @date 2018年1月19日 下午6:19:55
 * @version V1.0
 */
@Slf4j
@Service
public class PlatformChangeRecordsBaseService {
	@Autowired
	private PlatformChangeRecordsDao platformChangeRecordsDao;
	@Autowired
	private AccountInfoService accountInfoService;
	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private UserInfoDao userInfoDao;
	@Autowired
	private PlatformInfoDao platformInfoDao;
	@Autowired
	private PlatformAccountInfoDao platformAccountInfoDao;
	@Autowired
	private AccountEventDao accountEventDao;
	@Autowired
	private AccountEventChildDao accountEventChildDao;
	@Autowired
	private AccountEventChangeRecordsDao accountEventChangeRecordsDao;
	@Autowired
	private TransService transService;
	@Autowired
	private SeqGenerator seqGenerator;

	/**
	 * 新增审核修改记录
	 * @param changeRecordsForm 审核记录参数
	 * @return 新增结果
	 */
	@Transactional
	public BaseResponse addChangeRecords(PlatformChangeRecordsForm changeRecordsForm) {
		log.info("新增审核修改记录,PlatformInfoAuditForm:{}", JSON.toJSONString(changeRecordsForm));
		// 参数校验
		BaseResponse baseResp = this.addChangeRecordsParamCheck(changeRecordsForm);
		if (!Constant.SUCCESS.equals(baseResp.getReturnCode())) {
			log.info("新增审核修改记录,参数校验返回结果:{}", JSON.toJSONString(baseResp));
			return baseResp;
		}
		PlatformChangeRecordsEntity entity = new PlatformChangeRecordsEntity();
		Timestamp nowTime = new Timestamp(System.currentTimeMillis());
		// 传入参数转换
		BeanUtils.copyProperties(changeRecordsForm, entity);
		entity.setCreateTime(nowTime);
		entity.setUpdateTime(nowTime);
		platformChangeRecordsDao.save(entity);
		// 登账事件审核申请，将事件变为审核中状态
		if (ApplyAuditTypeEnum.CHANGE_EVENT.getCode().equals(changeRecordsForm.getChangeType())) {
			AccountEventEntity event = accountEventDao.findOne(changeRecordsForm.getEventOid());
			event.setSetUpStatus(AccountEventEntity.STATUS_AUDIT);
			accountEventDao.saveAndFlush(event);
		}
		log.info("新增审核修改记录,返回结果:{}", JSON.toJSONString(baseResp));
		return baseResp;
	}

	/**
	 * 平台信息修参数校验
	 * @param changeRecordsForm 平台信息参数
	 * @return 校验结果
	 */
	private BaseResponse addChangeRecordsParamCheck(PlatformChangeRecordsForm changeRecordsForm) {
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");
		// 验证参数
		if (StringUtil.isEmpty(changeRecordsForm.getAuditOid())
				|| StringUtil.isEmpty(changeRecordsForm.getChangeType())) {// 审核记录oid、变更类型
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("系统异常");
			return resp;
		}
		// 根据不同的修改类型，校验不同的参数
		if (ApplyAuditTypeEnum.CHANGE_CREDIT.getCode().equals(changeRecordsForm.getChangeType())) {// 修改授信额度
			if (StringUtil.isEmpty(changeRecordsForm.getAccountNo())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("变更账户号不能为空");
				return resp;
			}
			if (changeRecordsForm.getOldBalance() == null) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("变更前额度不能为空");
				return resp;
			}
			if (changeRecordsForm.getNewBalance() == null) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("变更后额度不能为空");
				return resp;
			}
			// 查询账户
			AccountInfoEntity account = accountInfoService.getAccountByNo(changeRecordsForm.getAccountNo());
			BigDecimal balance = account.getBalance();
			BigDecimal lineOfCredit = account.getLineOfCredit();
			BigDecimal usedBalance = BigDecimal.ZERO;
			// 调整授信额度不能低于已使用授信额度
			if (balance.compareTo(lineOfCredit) < 0) {
				usedBalance = lineOfCredit.subtract(balance);
			}
			if (changeRecordsForm.getNewBalance().compareTo(usedBalance) < 0) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("授信额度调整申请失败,授信额度不能低于已使用授信额度");
				return resp;
			}
			// 判断是否已存在待审核的记录
			int i = platformChangeRecordsDao.findReadyAuditRecords(changeRecordsForm.getAccountNo(),
					changeRecordsForm.getChangeType());
			if (i > 0) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("授信额度调整申请失败,该账户已存在待审核授信额度调整");
				return resp;
			}
		} else if (ApplyAuditTypeEnum.CHANGE_NAME1.getCode().equals(changeRecordsForm.getChangeType())) {// 平台名称更改
			if (StringUtil.isEmpty(changeRecordsForm.getOldName())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("原名称不能为空");
				return resp;
			}
			if (StringUtil.isEmpty(changeRecordsForm.getNewName())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("新名称不能为空");
				return resp;
			}
		} else if (ApplyAuditTypeEnum.CHANGE_NAME2.getCode().equals(changeRecordsForm.getChangeType())) {// 账户名称更改
			if (StringUtil.isEmpty(changeRecordsForm.getOldName())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("原名称不能为空");
				return resp;
			}
			if (StringUtil.isEmpty(changeRecordsForm.getNewName())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("新名称不能为空");
				return resp;
			}
			if (StringUtil.isEmpty(changeRecordsForm.getAccountNo())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("更改账户号不能为空");
				return resp;
			}
		} else if (ApplyAuditTypeEnum.DISABLE_PLATFORM.getCode().equals(changeRecordsForm.getChangeType())) {// 平台停用
			// 查询原平台是否为停用
			PlatformInfoEntity platformInfo = platformInfoDao.findByUserOid(changeRecordsForm.getUserOid());
			if (PlatformInfoEntity.STATUS_STOP.equals(platformInfo.getPlatformStatus())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("原平台为停用状态，不能再次停用");
				return resp;
			}
		} else if (ApplyAuditTypeEnum.ENABLE_PLATFORM.getCode().equals(changeRecordsForm.getChangeType())) {// 平台启用
			// 查询原平台是否为启用
			PlatformInfoEntity platformInfo = platformInfoDao.findByUserOid(changeRecordsForm.getUserOid());
			if (PlatformInfoEntity.STATUS_START.equals(platformInfo.getPlatformStatus())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("原平台为启用状态，不能再次启用");
				return resp;
			}
		} else if (ApplyAuditTypeEnum.BUILD_PROVISION_ACCOUNT.getCode().equals(changeRecordsForm.getChangeType())) {// 新建备付金账户
			if (StringUtil.isEmpty(changeRecordsForm.getNewName())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("新名称不能为空");
				return resp;
			}
		} else if (ApplyAuditTypeEnum.DISABLE_PROVISION_ACCOUNT.getCode().equals(changeRecordsForm.getChangeType())
				|| ApplyAuditTypeEnum.ENABLE_PROVISION_ACCOUNT.getCode().equals(changeRecordsForm.getChangeType())) {// 停启用备付金账户
			if (StringUtil.isEmpty(changeRecordsForm.getAccountNo())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("账户号不能为空");
				return resp;
			}
			if (StringUtil.isEmpty(changeRecordsForm.getOldName())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("停启用账户名称不能为空");
				return resp;
			}
		} else if (ApplyAuditTypeEnum.CHANGE_EVENT.getCode().equals(changeRecordsForm.getChangeType())) {// 设置登账事件
			if (StringUtil.isEmpty(changeRecordsForm.getEventOid())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("事件oid不能为空");
				return resp;
			}
			if (StringUtil.isEmpty(changeRecordsForm.getEventName())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("登账事件名称不能为空");
				return resp;
			}
			if (StringUtil.isEmpty(changeRecordsForm.getEffectiveTimeType())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("生效时间类型不能为空");
				return resp;
			}
			if (StringUtil.isEmpty(changeRecordsForm.getOldOutputAccountName())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("变更前出账账户名称不能为空");
				return resp;
			}
			// if (StringUtil.isEmpty(changeRecordsForm.getOldOutputAccountNo())) {
			// resp.setReturnCode(Constant.FAIL);
			// resp.setErrorMessage("变更前出账账户号不能为空");
			// return resp;
			// }
			if (StringUtil.isEmpty(changeRecordsForm.getNewOutputAccountName())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("变更后出账账户名称不能为空");
				return resp;
			}
			// if (StringUtil.isEmpty(changeRecordsForm.getNewOutputAccountNo())) {
			// resp.setReturnCode(Constant.FAIL);
			// resp.setErrorMessage("变更后出账账户号不能为空");
			// return resp;
			// }
			// if (StringUtil.isEmpty(changeRecordsForm.getOldIntputAccountName())) {
			// resp.setReturnCode(Constant.FAIL);
			// resp.setErrorMessage("变更前入账账户名称不能为空");
			// return resp;
			// }
			// if (StringUtil.isEmpty(changeRecordsForm.getNewOutputAccountNo())) {
			// resp.setReturnCode(Constant.FAIL);
			// resp.setErrorMessage("变更后出账账户号不能为空");
			// return resp;
			// }
			// if (StringUtil.isEmpty(changeRecordsForm.getOldInputAccountNo())) {
			// resp.setReturnCode(Constant.FAIL);
			// resp.setErrorMessage("变更前入账账户号不能为空");
			// return resp;
			// }
			// if (StringUtil.isEmpty(changeRecordsForm.getNewInputAccountName())) {
			// resp.setReturnCode(Constant.FAIL);
			// resp.setErrorMessage("变更后入账账户名称不能为空");
			// return resp;
			// }
			// if (StringUtil.isEmpty(changeRecordsForm.getNewInputAccountNo())) {
			// resp.setReturnCode(Constant.FAIL);
			// resp.setErrorMessage("变更后入账账户号不能为空");
			// return resp;
			// }
		} else {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("未知信息");
			return resp;
		}
		return resp;
	}

	/**
	 * 处理修改记录
	 * @param changeRecordsEntity 修改记录
	 * @return 修改结果
	 */
	public BaseResponse dealWithChange(PlatformChangeRecordsEntity changeRecordsEntity) {
		log.info("处理审核修改记录,PlatformChangeRecordsEntity:{}", JSON.toJSONString(changeRecordsEntity));
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");
		// 修改类型
		String changeType = changeRecordsEntity.getChangeType();
		// 根据修改的类型，处理不同类型的审核
		if (ApplyAuditTypeEnum.CHANGE_CREDIT.getCode().equals(changeType)) {
			// 处理授信额度调整
			resp = this.changeCredit(changeRecordsEntity);
		} else if (ApplyAuditTypeEnum.CHANGE_NAME2.getCode().equals(changeType)) {
			// 处理账户名称修改
			resp = this.changeAccountName(changeRecordsEntity);
		} else if (ApplyAuditTypeEnum.CHANGE_NAME1.getCode().equals(changeType)) {
			// 处理平台名称修改
			resp = this.changePlatformName(changeRecordsEntity);
		} else if (ApplyAuditTypeEnum.BUILD_PROVISION_ACCOUNT.getCode().equals(changeType)) {
			// 处理新增备付金账户
			resp = this.addProvisonAccount(changeRecordsEntity);
		} else if (ApplyAuditTypeEnum.DISABLE_PROVISION_ACCOUNT.getCode().equals(changeType)) {
			// 停用备付金账户
			resp = this.disableProvisionAccount(changeRecordsEntity);
		} else if (ApplyAuditTypeEnum.ENABLE_PROVISION_ACCOUNT.getCode().equals(changeType)) {
			// 启用备付金账户
			resp = this.enableProvisionAccount(changeRecordsEntity);
		} else if (ApplyAuditTypeEnum.DISABLE_PLATFORM.getCode().equals(changeType)) {
			// 停用平台
			resp = this.disablePlatform(changeRecordsEntity);
		} else if (ApplyAuditTypeEnum.ENABLE_PLATFORM.getCode().equals(changeType)) {
			// 启用平台
			resp = this.enablePlatform(changeRecordsEntity);
		} else if (ApplyAuditTypeEnum.CHANGE_EVENT.getCode().equals(changeType)) {
			// 修改登账事件账户
			resp = this.changeEvent(changeRecordsEntity);
		} else {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("未知审核处理类型");
		}
		log.info("处理审核修改记录,处理结果:{}", JSON.toJSONString(resp));
		return resp;
	}

	/**
	 * 修改登账事件账户
	 * @param changeRecordsEntity 修改参数
	 * @return 修改登账事件结果
	 */
	@Transactional
	private BaseResponse changeEvent(PlatformChangeRecordsEntity changeRecordsEntity) {
		log.info("修改登账事件账户:{}", changeRecordsEntity);
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		// 修改登账事件oid
		String childEventOid = changeRecordsEntity.getEventChildOid();
		String eventOid = changeRecordsEntity.getEventOid();
		// 查询该事件
		AccountEventEntity event = accountEventDao.findOne(eventOid);
		AccountEventChildEntity childEvent = accountEventChildDao.findOne(childEventOid);
		if (event == null || childEvent == null) {
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("获取登账事件信息异常");
		}
		String effevtiveStatus = AccountEventChangeRecordsEntity.STATUS_NOT;
		Timestamp nowTime = new Timestamp(System.currentTimeMillis());
		Timestamp effectiveTime;
		// 即时生效
		if ("01".equals(changeRecordsEntity.getEffectiveTimeType())) {
			// 生效时间为当前时间
			effevtiveStatus = AccountEventChangeRecordsEntity.STATUS_YES;
			effectiveTime = nowTime;
		} else if ("02".equals(changeRecordsEntity.getEffectiveTimeType())) {
			// 计算生效时间
			Date now = new Date();
			Date nextDay = DateUtil.getNextDay(now);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			String dateNowStr = sdf.format(nextDay);
			effectiveTime = Timestamp.valueOf(dateNowStr);
		} else {
			// 计算生效时间
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.DAY_OF_MONTH, 1);
			calendar.add(Calendar.MONTH, 1);
			Date nextMonthFirstDate = calendar.getTime();
			effectiveTime = new Timestamp(nextMonthFirstDate.getTime());
		}
		AccountEventChangeRecordsEntity eventChangeRecordsEntity = new AccountEventChangeRecordsEntity();
		eventChangeRecordsEntity.setAuditOid(changeRecordsEntity.getAuditOid());
		eventChangeRecordsEntity.setEffevtiveStatus(effevtiveStatus);
		eventChangeRecordsEntity.setEventChildOid(changeRecordsEntity.getEventChildOid());
		eventChangeRecordsEntity.setEventOid(eventOid);
		eventChangeRecordsEntity.setNewIntputAccountNo(changeRecordsEntity.getNewInputAccountName());
		eventChangeRecordsEntity.setNewOutputAccountNo(changeRecordsEntity.getNewOutputAccountNo());
		eventChangeRecordsEntity.setOldIntputAccountNo(changeRecordsEntity.getOldIntputAccountName());
		eventChangeRecordsEntity.setOldOutputAccountNo(changeRecordsEntity.getOldOutputAccountNo());
		eventChangeRecordsEntity.setUpdateTime(nowTime);
		eventChangeRecordsEntity.setCreateTime(nowTime);
		eventChangeRecordsEntity.setEffectiveTime(effectiveTime);
		if (AccountEventChangeRecordsEntity.STATUS_YES.equals(eventChangeRecordsEntity.getEffevtiveStatus())) {
			resp = this.effectiveEventChange(eventChangeRecordsEntity);
		} else {
			// 事件更新为生效中
			accountEventDao.changeEventStatusByOid(eventChangeRecordsEntity.getEventOid(),
					AccountEventEntity.STATUS_NOT);
		}
		// 放入定时任务
		accountEventChangeRecordsDao.save(eventChangeRecordsEntity);
		return resp;
	}

	/**
	 * 生效登账事件修改
	 * @param eventChangeRecordsEntity 变更记录
	 * @return 变更结果
	 */
	@Transactional
	public BaseResponse effectiveEventChange(AccountEventChangeRecordsEntity eventChangeRecordsEntity) {
		log.info("生效登账事件修改:{}", eventChangeRecordsEntity);
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		AccountEventChildEntity accountEventChildEntity = accountEventChildDao
				.findOne(eventChangeRecordsEntity.getEventChildOid());
		AccountInfoEntity inputEntity = accountInfoDao
				.findByAccountNo(eventChangeRecordsEntity.getNewIntputAccountNo());
		AccountInfoEntity outputEntity = accountInfoDao
				.findByAccountNo(eventChangeRecordsEntity.getNewOutputAccountNo());
		if (inputEntity == null || outputEntity == null) {
			resp.setReturnCode(Constant.FAIL);
			resp.setReturnCode("获取更改账户信息异常");
			return resp;
		}
		accountEventChildEntity.setInputAccountName(inputEntity.getAccountName());
		accountEventChildEntity.setInputAccountNo(inputEntity.getAccountNo());
		accountEventChildEntity.setInputAccountType(inputEntity.getAccountType());
		accountEventChildEntity.setOutputAccountName(outputEntity.getAccountName());
		accountEventChildEntity.setOutputAccountNo(outputEntity.getAccountNo());
		accountEventChildEntity.setOutputUserType(outputEntity.getAccountType());
		accountEventChildDao.saveAndFlush(accountEventChildEntity);
		// 事件更新为已生效
		accountEventDao.changeEventStatusByOid(eventChangeRecordsEntity.getEventOid(), AccountEventEntity.STATUS_YES);
		return resp;
	}

	/**
	 * 启用平台
	 * @param changeRecordsEntity 修改参数
	 * @return 启用结果
	 */
	@Transactional
	private BaseResponse enablePlatform(PlatformChangeRecordsEntity changeRecordsEntity) {
		log.info("启用平台:{}", changeRecordsEntity);
		// 平台启用，该平台的基本户、提现冻结户、备付金户启用
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		platformAccountInfoDao.disOrEnablePlatformAccount(PlatformAccountInfoEntity.STATUS_RUN,
				changeRecordsEntity.getUserOid(), UserTypeEnum.PLATFORMER.getCode());
		return resp;
	}

	/**
	 * 停用平台
	 * @param changeRecordsEntity 修改参数
	 * @return 停用结果
	 */
	@Transactional
	private BaseResponse disablePlatform(PlatformChangeRecordsEntity changeRecordsEntity) {
		log.info("停用平台:{}", changeRecordsEntity);
		// 平台停用，该平台的基本户、提现冻结户、备付金户停用
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		platformAccountInfoDao.disOrEnablePlatformAccount(PlatformAccountInfoEntity.STATUS_STOP,
				changeRecordsEntity.getUserOid(), UserTypeEnum.PLATFORMER.getCode());
		return resp;
	}

	/**
	 * 启用备付金账户
	 * @param changeRecordsEntity 修改记录
	 * @return 启用结果
	 */
	@Transactional
	private BaseResponse enableProvisionAccount(PlatformChangeRecordsEntity changeRecordsEntity) {
		log.info("启用备付金账户:{}", changeRecordsEntity);
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		// 获取账户信息
		PlatformAccountInfoEntity accountInfo = platformAccountInfoDao
				.findByAccountNo(changeRecordsEntity.getAccountNo());
		if (accountInfo == null) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("账户信息不存在");
			return resp;
		}
		accountInfo.setAccountStatus(PlatformAccountInfoEntity.STATUS_RUN);
		platformAccountInfoDao.saveAndFlush(accountInfo);
		return resp;
	}

	/**
	 * 停用备付金账户
	 * @param changeRecordsEntity 修改记录
	 * @return 停用结果
	 */
	@Transactional
	private BaseResponse disableProvisionAccount(PlatformChangeRecordsEntity changeRecordsEntity) {
		log.info("停用备付金账户:{}", changeRecordsEntity);
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		// 获取账户信息
		PlatformAccountInfoEntity accountInfo = platformAccountInfoDao
				.findByAccountNo(changeRecordsEntity.getAccountNo());
		if (accountInfo == null) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("账户信息不存在");
			return resp;
		}
		accountInfo.setAccountStatus(PlatformAccountInfoEntity.STATUS_STOP);
		platformAccountInfoDao.saveAndFlush(accountInfo);
		return resp;
	}

	/**
	 * 新增备付金账户
	 * @param changeRecordsEntity 修改记录
	 * @return 新增结果
	 */
	@Transactional
	private BaseResponse addProvisonAccount(PlatformChangeRecordsEntity changeRecordsEntity) {
		log.info("新增备付金账户:{}", changeRecordsEntity);
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		// 新增账户
		AccountInfoEntity account = new AccountInfoEntity();
		account.setOid(StringUtil.uuid());
		Timestamp nowTime = new Timestamp(System.currentTimeMillis());
		String accountNo = this.seqGenerator.next(CodeConstants.ACCOUNT_NO_PREFIX);
		account.setAccountNo(accountNo);
		account.setUserOid(changeRecordsEntity.getUserOid());
		account.setUserType(UserTypeEnum.PLATFORMER.getCode());
		account.setAccountType(AccountTypeEnum.RESERVE.getCode());
		account.setAccountName(changeRecordsEntity.getNewName());
		account.setOpenTime(nowTime);
		account.setBalance(BigDecimal.ZERO);
		account.setLineOfCredit(BigDecimal.ZERO);
		account.setStatus(AccountInfoEntity.STATUS_SUBMIT);
		account.setFrozenStatus(AccountInfoEntity.FROZENSTATUS_NORMAL);
		account.setCreateTime(nowTime);
		account.setUpdateTime(nowTime);
		account = accountInfoDao.save(account);
		log.info("创建用户账户：{}", account);
		// 新增账户信息
		PlatformAccountInfoEntity accountInfo = new PlatformAccountInfoEntity();
		accountInfo.setAccountName(account.getAccountName());
		accountInfo.setAccountNo(accountNo);
		accountInfo.setAccountStatus(PlatformAccountInfoEntity.STATUS_RUN);
		accountInfo.setSettleStatus(PlatformAccountInfoEntity.STATUS_RUN);
		accountInfo.setAccountType(account.getAccountType());
		accountInfo.setUserType(account.getUserType());
		accountInfo.setCreateTime(nowTime);
		accountInfo.setUpdateTime(nowTime);
		accountInfo.setUserOid(account.getUserOid());
		platformAccountInfoDao.save(accountInfo);
		return resp;
	}

	/**
	 * 平台名称修改
	 * @param changeRecordsEntity 修改记录
	 * @return 修改结果
	 */
	@Transactional
	private BaseResponse changePlatformName(PlatformChangeRecordsEntity changeRecordsEntity) {
		log.info("平台名称修改:{}", changeRecordsEntity);
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		// 查询平台信息
		PlatformInfoEntity platformInfo = platformInfoDao.findByUserOid(changeRecordsEntity.getUserOid());
		if (platformInfo == null) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("平台信息不存在");
			return resp;
		}
		// 更改平台名称
		platformInfo.setPlatformName(changeRecordsEntity.getNewName());
		platformInfoDao.saveAndFlush(platformInfo);
		// 更改平台用户信息名称
		UserInfoEntity userInfo = userInfoDao.findByUserOid(changeRecordsEntity.getUserOid());
		userInfo.setName(changeRecordsEntity.getNewName());
		userInfoDao.saveAndFlush(userInfo);
		// 暂不通知业务系统改名称
		return resp;
	}

	/**
	 * 处理账户名称更改
	 * @param changeRecordsEntity 修改记录
	 * @return 修改结果
	 */
	@Transactional
	private BaseResponse changeAccountName(PlatformChangeRecordsEntity changeRecordsEntity) {
		log.info("账户名称更改:{}", changeRecordsEntity);
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		// 查询平台账户信息
		PlatformAccountInfoEntity platformAccountInfo = platformAccountInfoDao
				.findByAccountNo(changeRecordsEntity.getAccountNo());
		// 修改平台账户名称
		platformAccountInfo.setAccountName(changeRecordsEntity.getNewName());
		platformAccountInfoDao.saveAndFlush(platformAccountInfo);
		// 查询账户信息
		AccountInfoEntity accountInfo = accountInfoDao.findByAccountNo(changeRecordsEntity.getAccountNo());
		// 修改账户信息名称
		accountInfo.setAccountName(changeRecordsEntity.getNewName());
		accountInfoDao.saveAndFlush(accountInfo);
		return resp;
	}

	/**
	 * 调整授信额度
	 * @param changeRecordsEntity 修改记录
	 * @return 调整结果
	 */
	@Transactional
	private BaseResponse changeCredit(PlatformChangeRecordsEntity changeRecordsEntity) {
		log.info("调整授信额度:{}", changeRecordsEntity);
		BaseResponse resp = new BaseResponse();
		// 查询账户
		AccountInfoEntity account = accountInfoService.getAccountByNo(changeRecordsEntity.getAccountNo());
		BigDecimal balance = account.getBalance();
		BigDecimal lineOfCredit = account.getLineOfCredit();
		// 计算已使用额度
		BigDecimal usedBalance = BigDecimal.ZERO;
		BigDecimal afterBalance = BigDecimal.ZERO;
		BigDecimal differBalance = BigDecimal.ZERO;
		// 调整授信额度不能低于已使用授信额度
		if (balance.compareTo(lineOfCredit) < 0) {
			usedBalance = lineOfCredit.subtract(balance);
		}
		if (changeRecordsEntity.getNewBalance().compareTo(usedBalance) < 0) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("授信额度调整失败,授信额度不能低于已使用授信额度");
			return resp;
		}
		CreateTransResponse transResp = null;
		// 处理授信额度调增
		if (changeRecordsEntity.getNewBalance().compareTo(changeRecordsEntity.getOldBalance()) > 0) {
			differBalance = changeRecordsEntity.getNewBalance().subtract(changeRecordsEntity.getOldBalance());
			log.info("调整授信额度增加账户余额 accountNo={}，transferBalance={}", account.getAccountNo(), differBalance);
			account = accountInfoDao.findByOidForUpdate(account.getOid());
			// 刷新缓存
			// entityManager.refresh(account);
			int result = 0;
			result = accountInfoDao.addCreditBalance(differBalance, account.getAccountNo());
			if (result == 0) {
				resp.setReturnCode(Constant.BALANCELESS);
				resp.setErrorMessage("转入交易异常");
				log.info("转入交易异常，账户号：{}", account.getAccountNo());
				throw new SETException(resp.getReturnCode());
			}
			afterBalance = balance.add(differBalance);
			String orderDesc = "增加授信额度记录账户明细";
			String order = seqGenerator.next("LOC");
			transResp = transService.addTransferTrans(order, order, OrderTypeEnum.QUOTAADJUSTMENT.getCode(),
					account.getAccountNo(), "", differBalance, afterBalance, orderDesc, "01", account,"");
			if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
				resp.setReturnCode(Constant.SUCCESS);
				resp.setErrorMessage("成功");
			} else {
				resp.setReturnCode(transResp.getReturnCode());
				resp.setErrorMessage(transResp.getErrorMessage());
				log.info("增加授信额度失败,resp={}", JSONObject.toJSON(resp));
				throw new SETException(9015);
			}
			return resp;
		} else {
			// 处理授信额度调低
			differBalance = changeRecordsEntity.getOldBalance().subtract(changeRecordsEntity.getNewBalance());
			log.info("调整授信额度扣减转入账户余额 accountNo={}，transferBalance={}", account.getAccountNo(), differBalance);
			account = accountInfoDao.findByOidForUpdate(account.getOid());
			// 刷新缓存
			// entityManager.refresh(account);

			int result = 0;
			result = accountInfoDao.subtractCreditBalance(differBalance, account.getAccountNo());
			if (result == 0) {
				resp.setReturnCode(Constant.BALANCELESS);
				resp.setErrorMessage("调整授信额度扣除转出账户余额失败，转出账户余额不足");
				log.info("调整授信额度扣除转出账户余额失败，转账人{}转出账户{}余额不足", account.getUserOid(), account.getAccountNo());
				return resp;
			}

			afterBalance = balance.subtract(differBalance);
			String orderDesc = "减少授信额度记录账户明细";
			String order = seqGenerator.next("LOC");
			transResp = transService.addTransferTrans(order, order, OrderTypeEnum.QUOTAADJUSTMENT.getCode(),
					account.getAccountNo(), "", differBalance, afterBalance, orderDesc, "02", account,"");
			if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
				resp.setReturnCode(Constant.SUCCESS);
				resp.setErrorMessage("成功");
			} else {
				resp.setReturnCode(transResp.getReturnCode());
				resp.setErrorMessage(transResp.getErrorMessage());
				log.info("减少授信额度失败,resp={}", JSONObject.toJSON(resp));
				throw new SETException(9015);
			}
		}
		return resp;
	}
}