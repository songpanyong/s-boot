package com.guohuai.boot.pay.controller;

import java.math.BigDecimal;
import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.guohuai.account.api.response.CardQuotaQueryResponse;
import com.guohuai.basic.component.ext.web.Response;
import com.guohuai.boot.pay.dao.ComChannelDao;
import com.guohuai.boot.pay.form.BankInfo;
import com.guohuai.boot.pay.form.ChannelBankForm;
import com.guohuai.boot.pay.form.ChannelBankInfo;
import com.guohuai.boot.pay.res.ChannelBankVoRes;
import com.guohuai.boot.pay.service.ComChannelBankService;
import com.guohuai.boot.pay.vo.ChannelVo;
import com.guohuai.component.common.TemplateQueryController;
import com.guohuai.component.util.Constant;
import com.guohuai.payadapter.bankutil.BankUtilEntity;
import com.guohuai.payadapter.bankutil.BankUtilService;
import com.guohuai.settlement.api.request.InteractiveRequest;
import com.guohuai.settlement.api.response.BankInfoResponse;

/**
 * 
 * @ClassName: ComChannelController
 * @Description: 渠道支持银行配置管理
 * @author xueyunlong
 * @date 2016年11月28日 下午3:21:11
 *
 */
@RestController
@RequestMapping(value = "/settlement/channelBank")
public class ComChannelBankController extends TemplateQueryController<ChannelVo, ComChannelDao> {
	private final static Logger log = LoggerFactory.getLogger(ComChannelBankController.class);

	@Autowired
	private ComChannelBankService comChannelBankService;

	@Autowired
	BankUtilService bankUtilService;

	@RequestMapping(value = "/save", method = { RequestMethod.POST, RequestMethod.GET })
	public @ResponseBody ResponseEntity<Response> save(@Valid ChannelBankForm req) {
		// String operator=this.getLoginAdmin();
		// req.setUserOid(operator);
		comChannelBankService.save(req);
		Response r = new Response();
		r.with("result", "SUCCESS");
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}

	@RequestMapping(value = "/update", method = { RequestMethod.POST, RequestMethod.GET })
	public @ResponseBody ResponseEntity<Response> update(@Valid ChannelBankForm req) {
		// String operator=this.getLoginAdmin();
		// req.setUserOid(operator);
		comChannelBankService.update(req);
		Response r = new Response();
		r.with("result", "SUCCESS");
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}

	@RequestMapping(value = "/delete", method = { RequestMethod.POST, RequestMethod.GET })
	public @ResponseBody ResponseEntity<Response> delete(@Valid ChannelBankForm req) {
		// String operator=this.getLoginAdmin();
		// req.setUserOid(operator);
		// log.info("{},渠道支持银行删除,{},", JSONObject.toJSONString(req.getOid()));
		comChannelBankService.delete(req.getOid());
		Response r = new Response();
		r.with("result", "SUCCESS");
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}

	// 查询结算渠道下的银行信息
	@RequestMapping(value = "/page", method = { RequestMethod.POST, RequestMethod.GET })
	public @ResponseBody ResponseEntity<ChannelBankVoRes> page(ChannelBankForm req) {
		ChannelBankVoRes rows = comChannelBankService.page(req);
		return new ResponseEntity<ChannelBankVoRes>(rows, HttpStatus.OK);
	}

	// 查询银行名称是否存在
//	@RequestMapping(value = "/findBank", method = { RequestMethod.POST, RequestMethod.GET })
//	public @ResponseBody ResponseEntity<Response> findBankName(ChannelBankForm req) {
//		String standardCode = comChannelBankService.findBank(req.getChannelbankName());
//		log.info("银行名称:{},银行代码:{},渠道:{},", req.getChannelbankName(), standardCode, req.getChannelNo());
//		Response r = new Response();
//		if (standardCode != null) {
//			int listSize = comChannelBankService.findBankReap(req.getChannelNo(), standardCode);
//			// log.info("重复银行数量,{}", listSize);
//			if (listSize > 0) {
//				r.with("result", "通道已存在该银行!");
//			} else {
//				r.with("result", "SUCCESS");
//			}
//		} else {
//			r.with("result", "银行名称输入有误或暂不支持该银行!建议输入银行全称!");
//		}
//		return new ResponseEntity<Response>(r, HttpStatus.OK);
//	}

	@RequestMapping(value = "/whetherSupportBank", method = { RequestMethod.POST, RequestMethod.GET })
	public @ResponseBody ResponseEntity<Response> whetherSupportBank(@RequestParam String bankCard) {
		boolean whetherSupport = comChannelBankService.whetherSupportBank(bankCard);
		Response r = new Response();
		if (whetherSupport) {
			r.with("result", "TRUE");
		} else {
			r.with("result", "FALSE");
		}
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}

	// 查询所有支持银行
	@RequestMapping(value = "/findAllBank", method = { RequestMethod.POST, RequestMethod.GET })
	public @ResponseBody ResponseEntity<Response> findAllBank() {
		List<String> bankList = null;
		Response r = new Response();
		bankList = comChannelBankService.findAllBankName();
		r.with("bankList", bankList);
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}

	// 查询所有支持银行
	@RequestMapping(value = "/findAllBankInfo", method = { RequestMethod.POST, RequestMethod.GET })
	public @ResponseBody ResponseEntity<Response> findAllBankInfo() {
		List<BankInfo> bankList = null;
		Response r = new Response();
		bankList = comChannelBankService.findAllBank();
		r.with("bankList", bankList);
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
	// 查询银行基础表中所有的银行
	@RequestMapping(value = "/findAllBankName", method = { RequestMethod.POST, RequestMethod.GET })
	public @ResponseBody ResponseEntity<Response> findAllBankName() {
		List<BankInfo> bankList = null;
		Response r = new Response();
		bankList = comChannelBankService.findAllBankNameAndBankCode();
		r.with("bankList", bankList);
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}

	// 通过卡号查询银行名称
	@RequestMapping(value = "/findBankByCard", method = { RequestMethod.POST, RequestMethod.GET })
	public @ResponseBody ResponseEntity<Response> findBankByCode(@RequestParam String bankCard) {
		String bankName = comChannelBankService.findBankName(bankCard);
		Response r = new Response();
		r.with("bankName", bankName);
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}

	// 通过卡号查询银行名称
//	@RequestMapping(value = "/findBankInfoByCard", method = { RequestMethod.POST, RequestMethod.GET })
//	public @ResponseBody ResponseEntity<Response> findBankInfoByCode(@RequestParam String bankCard) {
//		Response r = new Response();
//		log.info("根据卡号查银行，bankCard={}",bankCard);
//		BankUtilEntity bankInfo=null;
//		bankInfo = bankUtilService.getBankByCard(bankCard);
//		log.info("bankInfo={}",bankInfo);
//		r.with("bankInfo",bankInfo);
//		return new ResponseEntity<Response>(r, HttpStatus.OK);
//	}

	/**
	 * 根据银行卡号获取该银行的单笔限额（为0代表不支持该银行，或该银行的渠道关闭）
	 * 
	 * @param bankCard
	 * @return
	 */
	@RequestMapping(value = "/getSingleQuota", method = { RequestMethod.POST, RequestMethod.GET })
	public @ResponseBody ResponseEntity<Response> getSingleQuota(@RequestParam String bankCard) {
		BigDecimal singleQuota = comChannelBankService.getSingleQuota(bankCard);
		Response r = new Response();
		r.with("singleQuota", singleQuota);
		r.with("result", "SUCCESS");
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
	/**
	 * 根据渠道查询渠道银行限额信息(没用到)
	 * @param channelNo
	 * @return
	 */
	@RequestMapping(value = "/getChannelBank", method = { RequestMethod.POST, RequestMethod.GET })
	public @ResponseBody ResponseEntity<Response> getChannelBank(@RequestParam String channelNo) {
		List<ChannelBankInfo> bankList = null;
		Response r = new Response();
		bankList = comChannelBankService.getChannelBank(channelNo);
		r.with("bankList", bankList);
		r.with("result", "SUCCESS");
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/getBanKQuotaByBankCard", method = { RequestMethod.POST, RequestMethod.GET })
	public CardQuotaQueryResponse getBanKQuotaByBankCard(@RequestBody InteractiveRequest req) {
		CardQuotaQueryResponse resp = comChannelBankService.getBanKQuotaByBankCard(req.getBankCardNo());
		return resp;
	}
	
	// 通过卡号查询银行名称
	@RequestMapping(value = "/findBankInfoByCard", method = { RequestMethod.POST, RequestMethod.GET })
	public BankInfoResponse findBankInfoByCode(@RequestBody InteractiveRequest req) {
		BankInfoResponse resp = new BankInfoResponse();
		log.info("根据卡号查银行，bankCard={}",req.getBankCardNo());
		BankUtilEntity bankInfo=null;
		bankInfo = bankUtilService.getBankByCard(req.getBankCardNo());
		if(bankInfo != null){
			log.info("根据卡号查银行信息：{}",bankInfo);
			resp.setBankBin(bankInfo.getBankBin());
			resp.setBankCode(bankInfo.getBankCode());
			resp.setBankName(bankInfo.getBankName());
			resp.setBINLength(bankInfo.getBINLength());
			resp.setCardName(bankInfo.getCardName());
			resp.setKindOfCard(bankInfo.getKindOfCard());
			resp.setCardLength(bankInfo.getCardLength());
			resp.setReturnCode(Constant.SUCCESS);
			resp.setErrorMessage("成功");
		}else{
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("未知银行卡信息");
		}
		return resp;
	}
	
}
