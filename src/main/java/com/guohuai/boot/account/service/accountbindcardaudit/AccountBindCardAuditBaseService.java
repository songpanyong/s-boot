package com.guohuai.boot.account.service.accountbindcardaudit;

import java.sql.Timestamp;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.PayEnum;
import com.guohuai.boot.account.dao.AccountBindCardAuditDao;
import com.guohuai.boot.account.dao.PlatformInfoDao;
import com.guohuai.boot.account.entity.AccountBindCardAuditEntity;
import com.guohuai.boot.account.entity.PlatformInfoEntity;
import com.guohuai.boot.account.form.AccountBindCardAuditForm;
import com.guohuai.boot.pay.dao.ProtocolDao;
import com.guohuai.boot.pay.service.ElementValidationService;
import com.guohuai.boot.pay.service.EnterpriseWithholdingService;
import com.guohuai.boot.pay.vo.ProtocolVo;
import com.guohuai.component.util.BindBankCardEnum;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.DesPlus;
import com.guohuai.settlement.api.request.ElementValidationRequest;
import com.guohuai.settlement.api.response.BaseResponse;
import com.guohuai.settlement.api.response.ElementValidaResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: TODO
 * @author ZJ
 * @date 2018年1月19日 下午6:15:02
 * @version V1.0
 */
@Slf4j
@Component
public class AccountBindCardAuditBaseService {
	@Autowired
	private AccountBindCardAuditDao accountBindCardAuditDao;
	@Autowired
	private PlatformInfoDao platformInfoDao;
	@Autowired
	private ProtocolDao protocolDao;
	@Autowired
	private SeqGenerator seqGenerator;
	@Autowired
	private EnterpriseWithholdingService enterpriseWithholdingService;
	@Autowired
	private ElementValidationService elementValidationService;

	/**
	 * 绑卡申请、换绑申请
	 * @param req 绑卡参数
	 * @return 申请结果
	 */
	@Transactional
	public BaseResponse apply(AccountBindCardAuditForm req) {
		log.info("绑卡申请，请求参数：{}", JSONObject.toJSONString(req));
		BaseResponse resp = this.checkBindCardApply(req);
		if (!Constant.SUCCESS.equals(resp.getReturnCode())) {
			log.info("绑卡申请参数校验失败：{}", resp.getErrorMessage());
			return resp;
		}
		// 已存在绑卡待审核信息
		AccountBindCardAuditEntity entity = accountBindCardAuditDao.findAuditingByUserOid(req.getUserOid());
		if (entity == null) {
			entity = new AccountBindCardAuditEntity();
		} else {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("已存在待审核的绑卡信息");
			log.info("已存在待审核的绑卡信息");
			return resp;
		}
		Timestamp nowTime = new Timestamp(System.currentTimeMillis());
		// 传入参数转换
		BeanUtils.copyProperties(req, entity);
		entity.setCreateTime(nowTime);
		entity.setUpdateTime(nowTime);
		entity.setCardNo(DesPlus.encrypt(req.getCardNo()));
		entity.setCertificateNo(DesPlus.encrypt(req.getCertificateNo()));
		entity.setAuditStatus(Constant.AUDIT);// 待审核
		// 修改平台绑卡状态
		String bindCardStatus = req.getBindCardStatus();
		PlatformInfoEntity platformInfo = platformInfoDao.findByUserOid(req.getUserOid());
		if (platformInfo == null) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("平台信息不存在");
			log.info("平台信息不存在");
			return resp;
		}
		if (!bindCardStatus.equals(platformInfo.getBindCardStatus())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("绑卡状态异常");
			log.info("绑卡状态异常");
			return resp;
		}
		// 保存绑卡审核信息
		accountBindCardAuditDao.save(entity);
		if (BindBankCardEnum.BIND_STATUS_0.getCode().equals(bindCardStatus)
				|| BindBankCardEnum.BIND_STATUS_3.getCode().equals(bindCardStatus)) {
			bindCardStatus = BindBankCardEnum.BIND_STATUS_2.getCode();// 未绑卡绑卡申请
		} else {
			bindCardStatus = BindBankCardEnum.BIND_STATUS_4.getCode();// 已绑卡换绑卡申请
		}
		platformInfoDao.updateBindCardStatus(bindCardStatus, req.getUserOid());
		log.info("绑卡申请，返回结果：{}", JSONObject.toJSONString(resp));
		return resp;
	}

	/**
	 * 绑卡申请参数校验
	 * @param req 绑卡申请参数
	 * @return 校验结果
	 */
	private BaseResponse checkBindCardApply(AccountBindCardAuditForm req) {
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");
		// 验证参数
		if (StringUtil.isEmpty(req.getUserOid())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("系统异常");
			return resp;
		}
		if (StringUtil.isEmpty(req.getPlatformName())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("平台名称不能为空");
			return resp;
		}
		if (StringUtil.isEmpty(req.getAccountBankType())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("绑卡类型不能为空");
			return resp;
		}
		if (StringUtil.isEmpty(req.getRealName())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("账户名称不能为空");
			return resp;
		}
		if (StringUtil.isEmpty(req.getCardNo())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("银行账号不能为空");
			return resp;
		}
		if (StringUtil.isEmpty(req.getBankName())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("开户行名称不能为空");
			return resp;
		}
		if (BindBankCardEnum.ENTERPRISE.getCode().equals(req.getAccountBankType())) {// 企业
			if (StringUtil.isEmpty(req.getBankBranch())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("开户支行不能为空");
				return resp;
			}
			if (StringUtil.isEmpty(req.getProvince())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("开户省份不能为空");
				return resp;
			}
			if (StringUtil.isEmpty(req.getCity())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("开户城市不能为空");
				return resp;
			}
		} else {// 个人
			if (StringUtil.isEmpty(req.getCertificateNo())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("身份证号不能为空");
				return resp;
			}
			if (StringUtil.isEmpty(req.getPhone())) {
				resp.setReturnCode(Constant.FAIL);
				resp.setErrorMessage("手机号不能为空");
				return resp;
			}
		}
		if (StringUtil.isEmpty(req.getApplicantId())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("申请人id不能为空");
			return resp;
		}
		if (StringUtil.isEmpty(req.getApplicantName())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("申请人名称不能为空");
			return resp;
		}
		return resp;
	}

	/**
	 * 绑卡、换绑审核
	 * @param oid 审核信息oid
	 * @param auditStatus 审核状态
	 * @return 审核结果
	 */
	@Transactional
	public BaseResponse audit(AccountBindCardAuditForm req) {
		log.info("审核绑卡信息{}，审核状态{}，审核人信息{}{}", req.getOid(), req.getAuditStatus(), req.getOperatorName(),
				req.getOperatorId());
		Timestamp nowTime = new Timestamp(System.currentTimeMillis());
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("审核完成");
		String bindCardStatus = "";// 绑卡状态
		if (req.getOid() == null || req.getAuditStatus() == null || req.getOperatorId() == null
				|| req.getOperatorName() == null) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("请求参数异常");
			log.info("审核绑卡信息，返回结果", JSONObject.toJSONString(resp));
			return resp;
		}
		AccountBindCardAuditEntity entity = accountBindCardAuditDao.findOne(req.getOid());
		if (entity == null) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("待审核信息不存在");
			log.info("审核绑卡信息，返回结果", JSONObject.toJSONString(resp));
			return resp;
		}
		if (!Constant.AUDIT.equals(entity.getAuditStatus())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("待审核信息异常");
			log.info("审核绑卡信息，返回结果", JSONObject.toJSONString(resp));
			return resp;
		}
		// 修改平台绑卡状态
		PlatformInfoEntity platformInfo = platformInfoDao.findByUserOid(entity.getUserOid());
		if (platformInfo == null) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("平台信息不存在");
			log.info("审核绑卡信息，返回结果", JSONObject.toJSONString(resp));
			return resp;
		} else {
			bindCardStatus = platformInfo.getBindCardStatus();
		}
		if (Constant.PASS.equals(req.getAuditStatus())) {
			log.info("绑卡审核通过，审核绑卡信息{},", entity);
			if (BindBankCardEnum.PERSONAL.getCode().equals(entity.getAccountBankType())) {// 个人卡
				if (BindBankCardEnum.BIND_STATUS_2.getCode().equals(bindCardStatus)) {// 未绑卡绑卡申请
					bindCardStatus = BindBankCardEnum.BIND_STATUS_3.getCode();// 绑卡申请审核通过
				} else {// 已绑卡换绑卡申请
						// 解绑原卡
					resp = this.bindEnterprise(bindCardStatus, BindBankCardEnum.PERSONAL.getCode(), entity);
					if (Constant.SUCCESS.equals(resp.getReturnCode())) {
						bindCardStatus = BindBankCardEnum.BIND_STATUS_3.getCode();// 绑卡审核通过
					} else {
						bindCardStatus = BindBankCardEnum.BIND_STATUS_6.getCode();// 已绑卡换绑解绑失败（个人卡）
					}
				}
			} else {// 企业卡
				resp = this.bindEnterprise(bindCardStatus, BindBankCardEnum.ENTERPRISE.getCode(), entity);
				if (Constant.SUCCESS.equals(resp.getReturnCode())) {
					bindCardStatus = BindBankCardEnum.BIND_STATUS_1.getCode();// 已绑卡
				} else {
					bindCardStatus = BindBankCardEnum.BIND_STATUS_6.getCode();// 已绑卡换绑解绑失败（个人卡）
				}
			}
		} else {
			log.info("绑卡审核驳回，审核绑卡信息{},", entity);
			if (BindBankCardEnum.BIND_STATUS_2.getCode().equals(bindCardStatus)
					|| BindBankCardEnum.BIND_STATUS_4.getCode().equals(bindCardStatus)) {// 未绑卡绑卡申请
				bindCardStatus = BindBankCardEnum.BIND_STATUS_0.getCode();// 未绑卡
			} else {// 已绑卡换绑卡申请
				bindCardStatus = BindBankCardEnum.BIND_STATUS_1.getCode();// 已绑卡
			}
		}
		entity.setAuditStatus(req.getAuditStatus());
		entity.setUpdateTime(nowTime);
		entity.setOperatorId(req.getOperatorId());
		entity.setOperatorName(req.getOperatorName());
		// 变更审核结果
		accountBindCardAuditDao.save(entity);
		// 变更绑卡状态
		platformInfoDao.updateBindCardStatus(bindCardStatus, platformInfo.getUserOid());
		log.info("审核绑卡信息，返回结果", JSONObject.toJSONString(resp));
		return resp;
	}

	/**
	 * 企业绑卡、换绑
	 * @param bindCardStatus 绑卡状态
	 * @param entity 绑卡申请参数
	 * @return 企业绑卡、换绑结果
	 */
	private BaseResponse bindEnterprise(String bindCardStatus, String bindCardType, AccountBindCardAuditEntity entity) {
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		log.info("新绑卡审核卡号{}",entity.getCardNo());
		String newCardNo = DesPlus.decrypt(entity.getCardNo());
		log.info("新绑卡审核卡号解密{}",newCardNo);
		// 组装企业绑卡、企业解绑卡参数
		ElementValidationRequest req = new ElementValidationRequest();
		String orderNo = seqGenerator.next("CARD");
		req.setUserOid(entity.getUserOid());
		req.setRequestNo(orderNo);
		req.setBankName(entity.getBankName());
		req.setProvince(entity.getBankAddress());
		req.setCity(entity.getCity());
		req.setBranch(entity.getBankBranch());
		req.setRealName(entity.getRealName());
		req.setPhone(entity.getPhone());
		req.setCertificateNo(entity.getCertificateNo());
		req.setSystemSource("momosa");
		ElementValidaResponse elementValidaResponse = new ElementValidaResponse();
		if (BindBankCardEnum.BIND_STATUS_4.getCode().equals(bindCardStatus)) {// 换绑
			// 解绑原卡
			// 查询原绑卡信息
			ProtocolVo protocol = protocolDao.findOneByUserOidAndStatus(req.getUserOid(), PayEnum.ERRORCODE1.getCode());
			if (protocol != null && "企业".equals(protocol.getCardType())) {
				// 解绑企业卡
				req.setCardNo(DesPlus.decrypt(protocol.getCardNo()));
				log.info("解绑企业卡");
				elementValidaResponse = enterpriseWithholdingService.unbundling(req);
			} else {
				// 解绑个人卡
				req.setCardNo(DesPlus.decrypt(protocol.getCardNo()));
				log.info("解绑个人卡");
				elementValidaResponse = elementValidationService.unLock(req);
			}
		} else if(BindBankCardEnum.BIND_STATUS_2.getCode().equals(bindCardStatus)){
			elementValidaResponse.setReturnCode(Constant.SUCCESS);
		} else {
			elementValidaResponse.setReturnCode(Constant.FAIL);
			elementValidaResponse.setErrorMessage("绑卡状态异常");
		}
		if (Constant.SUCCESS.equals(elementValidaResponse.getReturnCode())
				&& BindBankCardEnum.ENTERPRISE.getCode().equals(bindCardType)) {
			// 绑定新卡
			req.setCardNo(newCardNo);
			log.info("绑定新卡");
			elementValidaResponse = enterpriseWithholdingService.bindCard(req);
		}
		if (!Constant.SUCCESS.equals(elementValidaResponse.getReturnCode())) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage(elementValidaResponse.getErrorMessage());
		}
		return resp;
	}

	/**
	 * 审核通过解绑失败，手动解绑
	 * @param userOid 用户id
	 * @return 解绑结果
	 */
	public BaseResponse unlock(String userOid) {
		log.info("平台手动尝试解绑,请求参数：{}", userOid);
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		AccountBindCardAuditEntity entity = accountBindCardAuditDao.findNewByUserOid(userOid);
		if (entity == null) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("审核绑卡信息不存在");
			log.info("平台手动尝试解绑,返回结果：{}", JSONObject.toJSONString(resp));
			return resp;
		}
		String bindCardStatus = "";
		PlatformInfoEntity platformInfo = platformInfoDao.findByUserOid(entity.getUserOid());
		if (platformInfo == null) {
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("平台信息不存在");
			log.info("平台手动尝试解绑,返回结果：{}", JSONObject.toJSONString(resp));
			return resp;
		} else {
			bindCardStatus = platformInfo.getBindCardStatus();
		}
		resp = bindEnterprise(bindCardStatus, BindBankCardEnum.ENTERPRISE.getCode(), entity);
		if (Constant.SUCCESS.equals(resp.getReturnCode())) {
			bindCardStatus = BindBankCardEnum.BIND_STATUS_1.getCode();// 已绑卡
			// 变更绑卡状态
			platformInfoDao.updateBindCardStatus(bindCardStatus, userOid);
		}
		log.info("平台手动尝试解绑,返回结果：{}", JSONObject.toJSONString(resp));
		return resp;
	}
}