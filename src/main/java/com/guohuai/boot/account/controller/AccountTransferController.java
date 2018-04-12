package com.guohuai.boot.account.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.guohuai.account.api.request.AccountTransferRequest;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.boot.account.service.AccountAcceptOrderService;
import com.guohuai.boot.account.service.TradeOrderService;

@Slf4j
@RestController
@RequestMapping(value = "/account/transfer")
public class AccountTransferController {
	
	@Autowired
	private AccountAcceptOrderService accountAcceptOrderService;

	@Autowired
	private TradeOrderService tradeOrderService;
    
	@RequestMapping(value = "/investT0", method = RequestMethod.POST)
	public AccountTransResponse investT0(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.investT0(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	@RequestMapping(value = "/investT1", method = RequestMethod.POST)
	public AccountTransResponse investT1(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.investT1(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	@RequestMapping(value = "/redeemT0", method = RequestMethod.POST)
	public AccountTransResponse redeemT0(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.redeemT0(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}

	@RequestMapping(value = "/redeemT1", method = RequestMethod.POST)
	public AccountTransResponse redeemT1(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.redeemT1(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	@RequestMapping(value = "/reFundInvestT0", method = RequestMethod.POST)
	public AccountTransResponse reFundInvestT0(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.reFundInvestT0(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	@RequestMapping(value = "/reFundInvestT1", method = RequestMethod.POST)
	public AccountTransResponse reFundInvestT1(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.reFundInvestT1(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	@RequestMapping(value = "/useRedPacket", method = RequestMethod.POST)
	public AccountTransResponse useRedPacket(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.useRedPacket(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	@RequestMapping(value = "/rebate", method = RequestMethod.POST)
	public AccountTransResponse rebate(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.rebate(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	@RequestMapping(value = "/redeemT0Change", method = RequestMethod.POST)
	public AccountTransResponse redeemT0Change(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.redeemT0Change(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}

	@RequestMapping(value = "/investT0Change", method = RequestMethod.POST)
	public AccountTransResponse investT0Change(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.investT0Change(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	@RequestMapping(value = "/redeemT0Continued", method = RequestMethod.POST)
	public AccountTransResponse redeemT0Continued(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.redeemT0Continued(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	@RequestMapping(value = "/redeemT1Continued", method = RequestMethod.POST)
	public AccountTransResponse redeemT1Continued(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.redeemT1Continued(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	@RequestMapping(value = "/investT0Continued", method = RequestMethod.POST)
	public AccountTransResponse investT0Continued(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.investT0Continued(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	@RequestMapping(value = "/investT1Continued", method = RequestMethod.POST)
	public AccountTransResponse investT1Continued(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.investT1Continued(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	@RequestMapping(value = "/unfreezeContinued", method = RequestMethod.POST)
	public AccountTransResponse unfreezeContinued(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.unfreezeContinued(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	/**
	 * 平台基本户转账
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/transferPlatformBasic", method = RequestMethod.POST)
	public AccountTransResponse transferPlatformBasic(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.transferPlatformBasic(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	/**
	 * 平台备付金转账
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/transferPlatformPayment", method = RequestMethod.POST)
	public AccountTransResponse transferPlatformPayment(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.transferPlatformPayment(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	/**
	 * 发行人基本户转账
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/transferPublisherBasic", method = RequestMethod.POST)
	public AccountTransResponse transferPublisherBasic(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.transferPublisherBasic(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	/**
	 * 发行人产品户转账
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/transferPublisherProduct", method = RequestMethod.POST)
	public AccountTransResponse transferPublisherProduct(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.transferPublisherProduct(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	/**
	 * 轧差-入款
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/nettingIncome", method = RequestMethod.POST)
	public AccountTransResponse nettingIncome(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.nettingIncome(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
	
	/**
	 * 轧差-出款
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/nettingOutcome", method = RequestMethod.POST)
	public AccountTransResponse nettingOutcome(@RequestBody AccountTransferRequest req) {
		log.info("账户接收到订单，请求参数：{}", req);
		AccountTransResponse resp = tradeOrderService.nettingOutcome(req);
		log.info("账户处理完成订单，返回参数：{}", resp);
		return resp;
	}
}
