package com.guohuai.boot.account.service.platformInfoaudit;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.AccountEventChangeRecordsDao;
import com.guohuai.boot.account.dao.AccountEventDao;
import com.guohuai.boot.account.dao.PlatformInfoAuditDao;
import com.guohuai.boot.account.entity.AccountEventChangeRecordsEntity;
import com.guohuai.boot.account.entity.AccountEventEntity;
import com.guohuai.boot.account.entity.PlatformChangeRecordsEntity;
import com.guohuai.boot.account.entity.PlatformInfoAuditEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.account.form.AccountEventForm;
import com.guohuai.boot.account.form.PlatformChangeRecordsForm;
import com.guohuai.boot.account.form.PlatformInfoAuditForm;
import com.guohuai.boot.account.service.PlatformChangeRecordsService;
import com.guohuai.boot.account.service.UserInfoService;
import com.guohuai.component.util.ApplyAuditTypeEnum;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.PayTwoRedisUtil;
import com.guohuai.component.util.UserTypeEnum;
import com.guohuai.settlement.api.response.BaseResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: TODO
 * @author ZJ
 * @date 2018年1月19日 下午6:22:10
 * @version V1.0
 */
@Slf4j
@Service
public class PlatformInfoAuditBaseService {
	@Autowired
	private PlatformInfoAuditDao plarformInfoAuditDao;
	@Autowired
	private AccountEventDao accountEventDao;
	@Autowired
	private AccountEventChangeRecordsDao accountEventChangeRecordsDao;
	@Autowired
	private PlatformChangeRecordsService platformChangeRecordsService;
	@Autowired
	private UserInfoService userInfoService;
	@Autowired
	private PayTwoRedisUtil payTwoRedisUtil;

	/**
	 * 新增审核信息
	 * @param auditForm 新增待审核信息
	 * @return 新增审核记录记过
	 */
	@Transactional
	public BaseResponse addAudit(PlatformInfoAuditForm auditForm) {
		log.info("新增审核参数,PlatformInfoAuditForm:{}", JSON.toJSONString(auditForm));
		BaseResponse baseResp = new BaseResponse();
		// 查询用户信息
		UserInfoEntity userInfo = userInfoService.getAccountUserByUserOid(auditForm.getUserOid());
		log.info("用户信息：{}", userInfo);
		if (userInfo == null) {
			baseResp.setReturnCode(Constant.FAIL);
			baseResp.setErrorMessage("用户不存在");
		}
		auditForm.setUserName(userInfo.getName());
		auditForm.setUserType(userInfo.getUserType());
		auditForm.setUserStatus("1");// 默认用户是启用状态，后续扩展
		auditForm.setUserTypeName(UserTypeEnum.getEnumName(auditForm.getUserType()));
		auditForm.setApplyTypeName(ApplyAuditTypeEnum.getEnumName(auditForm.getApplyType()));
		// 校验参数
		baseResp = this.addAuditParamCheck(auditForm);
		if (!Constant.SUCCESS.equals(baseResp.getReturnCode())) {
			return baseResp;
		}
		PlatformInfoAuditEntity entity = new PlatformInfoAuditEntity();
		Timestamp nowTime = new Timestamp(System.currentTimeMillis());
		// 传入参数转换
		BeanUtils.copyProperties(auditForm, entity);
		entity.setCreateTime(nowTime);
		entity.setUpdateTime(nowTime);
		entity.setAuditStatus(Constant.AUDIT);// 待审核
		entity.setOid(UUID.randomUUID().toString().replaceAll("-", ""));
		// 保存审核记录相关修改记录
		log.info("审核信息entity{}", JSON.toJSONString(entity));
		List<PlatformChangeRecordsForm> changeRecordsList = (List<PlatformChangeRecordsForm>) JSONArray
				.parseArray(auditForm.getChangeRecordsList(), PlatformChangeRecordsForm.class);
		for (PlatformChangeRecordsForm changeRecordsForm : changeRecordsList) {
			changeRecordsForm.setAuditOid(entity.getOid());
			changeRecordsForm.setUserOid(entity.getUserOid());
			// 保存修改记录
			baseResp = platformChangeRecordsService.addChangeRecords(changeRecordsForm);
		}
		if (Constant.SUCCESS.equals(baseResp.getReturnCode())) {
			// 保存审核记录
			plarformInfoAuditDao.save(entity);
		}
		return baseResp;
	}

	/**
	 * 审核
	 * @param auditForm 待审核记录
	 * @return 审核结果
	 */
	public BaseResponse audit(PlatformInfoAuditForm auditForm) {
		log.info("审核修改,oid:{},auditStatus:{}", auditForm.getOid(), auditForm.getAuditStatus());
		BaseResponse resp = new BaseResponse();
		// 将提现审核信息放入redis中
		Long check = payTwoRedisUtil.setRedisByTime("audit_redis_tag" + auditForm.getOid(), auditForm.getOid());
		if (check.intValue() == 0) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("该审核处理中，请勿重复提交");
			return resp;
		}
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");
		String auditStatus = auditForm.getAuditStatus();
		// 查询审核记录
		PlatformInfoAuditEntity auditEntity = plarformInfoAuditDao.findOne(auditForm.getOid());
		if (auditEntity == null) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("审核信息不存在");
			log.info("审核修改,返回结果:{}", resp);
			return resp;
		}
		if (!Constant.AUDIT.equals(auditEntity.getAuditStatus())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("已审核，请勿再次审核");
			payTwoRedisUtil.delRedis("audit_redis_tag" + auditForm.getOid());
			return resp;
		}
		// 审核通过
		if (Constant.PASS.equals(auditStatus)) {
			log.info("审核通过");
			List<PlatformChangeRecordsEntity> changeRecordsList = platformChangeRecordsService
					.findByAuditOid(auditEntity.getOid());
			for (PlatformChangeRecordsEntity changeRecordsEntity : changeRecordsList) {
				// 处理审核
				resp = platformChangeRecordsService.dealWithChange(changeRecordsEntity);
				if (!Constant.SUCCESS.equals(resp.getReturnCode())) {
					break;
				}
			}
		} else {
			log.info("审核驳回");
		}
		if (Constant.SUCCESS.equals(resp.getReturnCode())) {
			// 修改审核记录
			auditEntity.setAuditStatus(auditStatus);
			auditEntity.setAuditReason(auditForm.getAuditReason());
			auditEntity.setOperatorId(auditForm.getOperatorId());
			auditEntity.setOperatorName(auditForm.getOperatorName());
			plarformInfoAuditDao.saveAndFlush(auditEntity);
		}
		payTwoRedisUtil.delRedis("audit_redis_tag" + auditForm.getOid());
		log.info("审核修改,返回结果:{}", resp);
		return resp;
	}

	/**
	 * 平台信息修参数校验
	 * @param auditForm 平台信息参数
	 * @return 校验结果
	 */
	private BaseResponse addAuditParamCheck(PlatformInfoAuditForm auditForm) {
		log.info("审核参数校验,PlatformInfoAuditForm:{}", JSON.toJSONString(auditForm));
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("提交成功");
		// 验证参数
		if (StringUtil.isEmpty(auditForm.getUserOid())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("用户ID不能为空");
			return resp;
		}
		if (StringUtil.isEmpty(auditForm.getUserType())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("用户类型不能为空");
			return resp;
		}
		if (StringUtil.isEmpty(UserTypeEnum.getEnumName(auditForm.getUserType()))) {
			// 用户类型不存在
			resp.setReturnCode(Constant.USERTYPENOTEXISTS);
			resp.setErrorMessage("用户类型不存在！");
			log.error("用户类型不存在，[userType=" + auditForm.getUserType() + "]");
			return resp;
		}
		if (StringUtil.isEmpty(auditForm.getUserStatus())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("用户状态不能为空");
			return resp;
		}
		if (StringUtil.isEmpty(auditForm.getApplyType())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("申请原因类型不能为空");
			return resp;
		}
		// if (StringUtil.isEmpty(auditForm.getApplyReason())) {
		// resp.setReturnCode(Constant.FAIL);
		// resp.setErrorMessage("申请原因不能为空");
		// return resp;
		// }
		if (StringUtil.isEmpty(auditForm.getApplicantId())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("申请人id不能为空");
			return resp;
		}
		if (StringUtil.isEmpty(auditForm.getApplicantName())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("申请人姓名不能为空");
			return resp;
		}
		return resp;
	}

	/**
	 * 撤回待审核或审核通过待生效的登账事件变更
	 * @param eventForm 登账事件信息
	 * @return 撤回结果
	 */
	@Transactional
	public BaseResponse revoke(AccountEventForm eventForm) {
		log.info("撤销登账事件变更:{}", JSONObject.toJSONString(eventForm));
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("撤销成功");
		// 查询事件信息
		AccountEventEntity event = accountEventDao.findOne(eventForm.getOid());
		if (event == null) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("获取登账信息异常");
			log.info("撤销登账事件变更，返回结果：{}", JSONObject.toJSONString(resp));
			return resp;
		}
		// 查询变更审核记录
		PlatformInfoAuditEntity audit = plarformInfoAuditDao.findByEventOid(eventForm.getOid());
		if (audit == null) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("改修改不存在或已被驳回");
			log.info("撤销登账事件变更，返回结果：{}", JSONObject.toJSONString(resp));
			return resp;
		}
		if (Constant.PASS.equals(audit.getAuditStatus())) {// 已待生效中
			// 处理待生效任务
			List<AccountEventChangeRecordsEntity> changeRecords = accountEventChangeRecordsDao
					.findByAuditOid(audit.getOid());
			for (AccountEventChangeRecordsEntity record : changeRecords) {
				record.setEffevtiveStatus(AccountEventChangeRecordsEntity.STATUS_REVOKE);
			}
			accountEventChangeRecordsDao.save(changeRecords);
		} else if (Constant.AUDIT.equals(audit.getAuditStatus())) {// 未审核
			// 审核信息状态修改为撤回
			audit.setAuditStatus(Constant.REVOKE);
			plarformInfoAuditDao.saveAndFlush(audit);
		}
		// 将事件信息变为已生效
		event.setSetUpStatus(AccountEventEntity.STATUS_YES);
		accountEventDao.saveAndFlush(event);
		log.info("撤销登账事件变更，返回结果：{}", JSONObject.toJSONString(resp));
		return resp;
	}
}