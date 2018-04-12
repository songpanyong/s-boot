package com.guohuai.boot.account.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.guohuai.boot.account.entity.PlatformChangeRecordsEntity;
import com.guohuai.boot.account.form.AccountEventForm;
import com.guohuai.boot.account.form.PlatformInfoAuditForm;
import com.guohuai.boot.account.res.PlatformInfoAuditResponse;
import com.guohuai.boot.account.service.PlatformInfoAuditService;
import com.guohuai.settlement.api.response.BaseResponse;

@RestController
@RequestMapping(value = "/account/platformAudit")
public class PlatformInfoAuditController {

	@Autowired
	private PlatformInfoAuditService platformInfoAuditService;

	/**
	 * 提交审核申请
	 * @param auditForm 审核申请参数
	 * @return 提交审核申请结果
	 */
	@RequestMapping(value = "/addAudit",method = RequestMethod.POST)
	public BaseResponse addAudit(PlatformInfoAuditForm auditForm) {
		BaseResponse resp = platformInfoAuditService.addAudit(auditForm); 
		return resp;
	}
	
	/**
	 * 审核申请记录
	 * @param auditForm 审核参数
	 * @return 审核结果
	 */
	@RequestMapping(value = "/audit",method = RequestMethod.POST)
	public BaseResponse audit(PlatformInfoAuditForm auditForm) {
		BaseResponse resp = platformInfoAuditService.audit(auditForm); 
		return resp;
	}
	
	/**
	 * 查询审核记录
	 * @param auditForm 查询条件
	 * @return 查询结果
	 */
	@RequestMapping(value = "/page", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<PlatformInfoAuditResponse> page(PlatformInfoAuditForm auditForm) {
		PlatformInfoAuditResponse rows = platformInfoAuditService.page(auditForm);
		return new ResponseEntity<PlatformInfoAuditResponse>(rows, HttpStatus.OK);
	}
	
	/**
	 * 修改记录查询
	 * @param auditForm 审核oid
	 * @return 修改记录
	 */
	@RequestMapping(value = "/changeRecords", method = {RequestMethod.POST,RequestMethod.GET})
	public List<PlatformChangeRecordsEntity> changeRecords(PlatformInfoAuditForm auditForm) {
		List<PlatformChangeRecordsEntity> list = platformInfoAuditService.getChangeRecords(auditForm.getOid());
		return list;
	}
	
	/**
	 * 平台首页展示变更记录
	 * @param auditForm 平台用户UserOid
	 * @return 变更记录
	 */
	@RequestMapping(value = "/paltformChangeRecords", method = {RequestMethod.POST,RequestMethod.GET})
	public List<PlatformChangeRecordsEntity> paltformChangeRecords(PlatformInfoAuditForm auditForm) {
		List<PlatformChangeRecordsEntity> list = platformInfoAuditService.paltformChangeRecords(auditForm.getUserOid());
		return list;
	}
	
	/**
	 * 撤销登账事件审核
	 * @param auditForm 登账事件oid
	 * @return 撤销结果
	 */
	@RequestMapping(value = "/revoke",method = RequestMethod.POST)
	public BaseResponse revoke(AccountEventForm eventForm) {
		BaseResponse resp = platformInfoAuditService.revoke(eventForm); 
		return resp;
	}
}
