package com.guohuai.boot.account.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.entity.AccountEventTransEntity;
import com.guohuai.boot.account.form.AccountEventForm;
import com.guohuai.boot.account.res.AccountEventEffectInfoResponse;
import com.guohuai.boot.account.res.AccountEventPageResponse;
import com.guohuai.boot.account.service.AccountEventService;
import com.guohuai.boot.account.service.AccountEventTransService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "/account/event")
public class AccountEventController {

	@Autowired
	private AccountEventService accountEventService;
	
	@Autowired
	private AccountEventTransService accountEventTransService;

	/**
	 * 登账事件分页查询
	 * @param req 查询参数
	 * @return 查询结果
	 */
	@RequestMapping(value = "/page", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<AccountEventPageResponse> page(AccountEventForm req) {
		AccountEventPageResponse rows = accountEventService.page(req);
		return new ResponseEntity<AccountEventPageResponse>(rows, HttpStatus.OK);
	}
	/**
	 * 查看登账事件设置生效状态
	 * @param req 登账事件oid
	 * @return 查询结果
	 */
	@RequestMapping(value = "/getEffectInfo", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<AccountEventEffectInfoResponse> pagetEffectInfoge(AccountEventForm req) {
		AccountEventEffectInfoResponse resp = accountEventService.getEffectInfo(req);
		return new ResponseEntity<AccountEventEffectInfoResponse>(resp, HttpStatus.OK);
	}
  
	/**
	 * 根据订单号查询登账明细
	 */
	@RequestMapping(value = "/getEventTransList", method = RequestMethod.POST)
	public List<AccountEventTransEntity> getEventTransList(String orderNo) {
		if(StringUtil.isEmpty(orderNo)){
			log.info("根据订单号查询登账明细,订单号不能为空", orderNo);
			return null;
		}
		List<AccountEventTransEntity> orderAccountResponse = accountEventTransService.findEventTransByOrderNo(orderNo);
		return orderAccountResponse;
	}
}
