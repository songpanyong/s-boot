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

import com.guohuai.account.api.request.OrderQueryRequest;
import com.guohuai.account.api.response.AccountReconciliationDataResponse;
import com.guohuai.account.api.response.OrderListResponse;
import com.guohuai.account.api.response.OrderQueryResponse;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.res.AccountOrderPageResponse;
import com.guohuai.boot.account.res.AccountOrderResponse;
import com.guohuai.boot.account.service.AccOrderService;
import com.guohuai.settlement.api.request.OrderAccountRequest;
import com.guohuai.settlement.api.response.OrderAccountResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "/account/order")
public class AccOrderController {

	@Autowired
	private AccOrderService orderService;
	
	
	@RequestMapping(value = "/list",method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<OrderListResponse> orderQueryList(
			OrderQueryRequest req){
		OrderListResponse resp = orderService.orderQueryList(req);
		return new ResponseEntity<OrderListResponse>(resp, HttpStatus.OK);
	}
	
	/**
	 * 获取订单对账数据
	 */
	@RequestMapping(value = "/getAccountReconciliationData",method = RequestMethod.POST)
	public List<OrderAccountResponse>  getAccountReconciliationData(@RequestBody OrderAccountRequest req) {
		List<OrderAccountResponse> orderAccountResponse=orderService.getAccountReconciliationData(req);
		return orderAccountResponse;
	}
	
	/**
	 * 获取订单对账数据
	 */
	@RequestMapping(value = "/getAccountOrderByOrderCode",method = RequestMethod.POST)
	public OrderQueryResponse  getAccountOrderByOrderCode(@RequestBody OrderAccountRequest accountRequest) {
		OrderQueryResponse orderQueryResponse = orderService.getOrderByNoResp(accountRequest.getOrderCode());
		return orderQueryResponse;
	}
	
	
	/**
	 * 获取订单对账数据（最新）
	 */
	@RequestMapping(value = "/getAccountAlreadyReconciliationData",method = RequestMethod.POST)
	public AccountReconciliationDataResponse  getAccountAlreadyReconciliationData(@RequestBody OrderAccountRequest req) {
		AccountReconciliationDataResponse orderAccountResponse= orderService.getAccountAlreadyReconciliationData(req);
		return orderAccountResponse;
	}
	
	/**
	 * 订单日志分页查询
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/orderPage",method = RequestMethod.POST)
	public ResponseEntity<AccountOrderPageResponse> orderPage(OrderQueryRequest req){
		AccountOrderPageResponse resp = orderService.page(req);
		return new ResponseEntity<AccountOrderPageResponse>(resp, HttpStatus.OK);
	}
	
	/**
	 * 查询订单详情
	 * @param orderNo
	 * @return
	 */
	@RequestMapping(value = "/orderDetail",method = RequestMethod.POST)
	public ResponseEntity<AccountOrderResponse> orderDetail(String orderNo){
		if(StringUtil.isEmpty(orderNo)){
			log.info("根据订单号查询订单详情,订单号不能为空", orderNo);
			return null;
		}
		AccountOrderResponse resp = orderService.findAccOrderDetails(orderNo);
		return new ResponseEntity<AccountOrderResponse>(resp, HttpStatus.OK);
	}
}
