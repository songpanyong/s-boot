package com.guohuai.boot.account.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.boot.account.entity.AccountBindCardAuditEntity;
import com.guohuai.boot.account.form.AccountBindCardAuditForm;
import com.guohuai.boot.account.res.AccountBindCardAuditPageResponse;
import com.guohuai.boot.account.service.AccountBindCardAuditService;
import com.guohuai.boot.account.service.PlatformInfoService;
import com.guohuai.boot.pay.service.ElementValidationService;
import com.guohuai.component.util.BindBankCardEnum;
import com.guohuai.component.util.Constant;
import com.guohuai.settlement.api.request.ElementValidationRequest;
import com.guohuai.settlement.api.response.BaseResponse;
import com.guohuai.settlement.api.response.ElementValidaResponse;

@RestController
@RequestMapping(value = "/account/bindCardAudit")
public class PlatformBindCardAuditController {

	@Autowired
	private AccountBindCardAuditService accountBindCardAuditService;
	@Autowired
	private ElementValidationService elementValidationService;
	@Autowired
	private PlatformInfoService platformInfoService;
	@Autowired
	private SeqGenerator seqGenerator;

	/**
	 * 根据用户userOid查询绑卡审核信息
	 * @param req 用户userOid
	 * @return 查询结果
	 */
	@RequestMapping(value = "/auditInfo", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<AccountBindCardAuditEntity> platformInfo(AccountBindCardAuditForm req) {
		AccountBindCardAuditEntity resp = accountBindCardAuditService.auditInfo(req.getUserOid());
		return new ResponseEntity<AccountBindCardAuditEntity>(resp, HttpStatus.OK);
	}
	
	/**
	 * 平台绑卡审核分页查询
	 * @param req 查询请求参数
	 * @return 查询结果
	 */
	@RequestMapping(value = "/page", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<AccountBindCardAuditPageResponse> page(AccountBindCardAuditForm req) {
		AccountBindCardAuditPageResponse rows=accountBindCardAuditService.page(req);
		return new ResponseEntity<AccountBindCardAuditPageResponse>(rows, HttpStatus.OK);
	}
	
	/**
	 * 绑卡申请、换绑申请
	 * @param req 绑卡信息
	 * @return 申请结果
	 */
	@RequestMapping(value = "/apply", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<BaseResponse> apply(AccountBindCardAuditForm req) {
		BaseResponse resp = accountBindCardAuditService.apply(req);
		return new ResponseEntity<BaseResponse>(resp, HttpStatus.OK);
	}
	
	/**
	 * 绑卡信息审核
	 * @param req 审核oid
	 * @return 审核处理结果
	 */
	@RequestMapping(value = "/audit", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<BaseResponse> audit(AccountBindCardAuditForm req) {
		BaseResponse resp = accountBindCardAuditService.audit(req);
		return new ResponseEntity<BaseResponse>(resp, HttpStatus.OK);
	}
	
	/**
	 * 手动尝试解绑
	 * @param req 平台userOid
	 * @return 解绑结果
	 */
	@RequestMapping(value = "/unlock", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<BaseResponse> unlock(AccountBindCardAuditForm req) {
		BaseResponse resp = accountBindCardAuditService.unlock(req.getUserOid());
		return new ResponseEntity<BaseResponse>(resp, HttpStatus.OK);
	}
	
	/**
	 * 平台绑卡（个人卡）发送验证码
	 * 调用原绑卡申请接口
	 * @param req 绑卡信息
	 * @return 绑卡申请返回结果
	 */
	@RequestMapping(value = "/bindApply",method = RequestMethod.POST)
	public ElementValidaResponse  bindApply(ElementValidationRequest req) {
		//生成请求流水号
		req.setRequestNo(seqGenerator.next("T3BAN"));
		ElementValidaResponse elementValidaResponse=new ElementValidaResponse();
		elementValidaResponse=elementValidationService.bindApply(req);
		return elementValidaResponse;
	}
	
	/**
	 * 绑卡（个人卡）确认
	 * 调用原绑卡接口
	 * @param req 确认参数
	 * @return 绑卡结果
	 */
	@RequestMapping(value = "/bindConfrim",method = RequestMethod.POST)
	public ElementValidaResponse  bindConfrim(ElementValidationRequest req) {
		//生成请求流水号
		req.setRequestNo(seqGenerator.next("T3BAN"));
		ElementValidaResponse elementValidaResponse=new ElementValidaResponse();
		elementValidaResponse=elementValidationService.bindConfrim(req);
		if(Constant.SUCCESS.equals(elementValidaResponse.getReturnCode())){
			//绑定成功，去修改平台绑卡状态
			platformInfoService.changeBindStatus(req.getUserOid(), BindBankCardEnum.BIND_STATUS_1.getCode());
		}
		return elementValidaResponse;
	}
  
}
