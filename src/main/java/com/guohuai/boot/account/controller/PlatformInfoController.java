package com.guohuai.boot.account.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.guohuai.boot.account.entity.PlatformAccountInfoEntity;
import com.guohuai.boot.account.entity.PlatformInfoEntity;
import com.guohuai.boot.account.form.AccountInfoForm;
import com.guohuai.boot.account.form.PlatformInfoForm;
import com.guohuai.boot.account.res.PlatformInfoPageResponse;
import com.guohuai.boot.account.res.PlatformInfoResponse;
import com.guohuai.boot.account.service.PlatformInfoService;

@RestController
@RequestMapping(value = "/account/platform")
public class PlatformInfoController {

	@Autowired
	private PlatformInfoService platformInfoService;

	/**
	 * 根据用户id获取平台信息
	 * @param userOid 平台id
	 * @return 平台信息
	 */
	@RequestMapping(value = "/getPlatfromInfoByUserOid",method = RequestMethod.POST)
	public PlatformInfoEntity getPlatfromInfoByUserOid(@RequestBody String userOid) {
		PlatformInfoEntity entity = platformInfoService.getPlatformInfoByUserOid(userOid); 
		return entity;
	}
	
	/**
	 * 平台首页展示平台信息及账户信息
	 * @param req 平台userOid
	 * @return 平台信息及账户信息
	 */
	@RequestMapping(value = "/platformInfo", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<PlatformInfoResponse> platformInfo(PlatformInfoForm req) {
		PlatformInfoResponse resp = platformInfoService.platformInfo(req.getUserOid());
		return new ResponseEntity<PlatformInfoResponse>(resp, HttpStatus.OK);
	}
	
	/**
	 * 平台信息分页查询
	 * @param req 分页查询参数
	 * @return 查询结果
	 */
	@RequestMapping(value = "/page", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<PlatformInfoPageResponse> page(PlatformInfoForm req) {
		PlatformInfoPageResponse rows=platformInfoService.page(req);
		return new ResponseEntity<PlatformInfoPageResponse>(rows, HttpStatus.OK);
	}
	
	/**
	 * 查询所有平台List
	 * @return 所有平台List
	 */
	@RequestMapping(value = "/platfromInfoList",method = {RequestMethod.POST,RequestMethod.GET})
	public List<PlatformInfoEntity> platfromInfoList() {
		List<PlatformInfoEntity> platformList = platformInfoService.getPlatformInfoList(); 
		return platformList;
	}
	
	/**
	 * 可更换账户下拉列表查询
	 * @param req 求情参数
	 * @return 可更换账户下拉列表
	 */
	@RequestMapping(value = "/platfromAccountInfoList",method = RequestMethod.POST)
	public List<PlatformAccountInfoEntity> platfromAccountInfoList(AccountInfoForm req) {
		List<PlatformAccountInfoEntity> platformList = platformInfoService.getChangeAccountInfoList(req); 
		return platformList;
	}
	
}
