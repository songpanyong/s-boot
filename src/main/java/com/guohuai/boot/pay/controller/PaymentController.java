package com.guohuai.boot.pay.controller;

import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.guohuai.basic.component.ext.web.BaseController;
import com.guohuai.basic.component.ext.web.Response;
import com.guohuai.boot.pay.form.PaymentForm;
import com.guohuai.boot.pay.res.PaymentVoRes;
import com.guohuai.boot.pay.service.PaymentService;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.DateUtil;
import com.guohuai.settlement.api.request.OrderRequest;
import com.guohuai.settlement.api.response.OrderResponse;


@RestController 
@RequestMapping(value = "/settlement/payment"/*, produces = "application/json;charset=utf-8"*/)
public class PaymentController extends BaseController{

	@Autowired
	private PaymentService paymentService;

	@RequestMapping(value = "/page", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<PaymentVoRes> page(PaymentForm req) {
		PaymentVoRes rows=paymentService.page(req);
		return new ResponseEntity<PaymentVoRes>(rows, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/page2", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<PaymentVoRes> page2(PaymentForm req) {
		PaymentVoRes rows=paymentService.page2(req);
		return new ResponseEntity<PaymentVoRes>(rows, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/update", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<OrderResponse> update(PaymentForm req) {
		String userOid=this.getLoginUser();
		req.setUserOid(userOid);
		req.setAuditStatus("2");//提交审核状态20170214
		OrderResponse res=paymentService.update(req); 
		return new ResponseEntity<OrderResponse>(res, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/audit", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<OrderResponse> audit(PaymentForm req) {
		String userOid=this.getLoginUser();
		req.setUserOid(userOid);
		OrderResponse res = new OrderResponse();
		String sessionOrderNo = (String) session.getAttribute(req.getOrderNo());
		if(sessionOrderNo != null&&"".equals(sessionOrderNo)){
			res.setReturnCode(Constant.FAIL);
			res.setErrorMessage("该订单已在操作！");
		}else{
			session.setAttribute(req.getOrderNo(), req.getOrderNo());
			res = paymentService.audit(req);
			session.removeAttribute(req.getOrderNo());
		}
		return new ResponseEntity<OrderResponse>(res, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/updateBath", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<OrderResponse> updateBath(PaymentForm req) {
		OrderResponse res=paymentService.updateBath(req); 
		return new ResponseEntity<OrderResponse>(res, HttpStatus.OK);
	}
	
/*	@RequestMapping(value = "/auditBath", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<OrderResponse> auditBath(PaymentForm req) {
		OrderResponse res=paymentService.auditBath(req); 
		return new ResponseEntity<OrderResponse>(res, HttpStatus.OK);
	}*/
	
	@RequestMapping(value = "/pageBath", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<PaymentVoRes> pageBath(PaymentForm req) {
		PaymentVoRes rows=paymentService.pageBath(req);
		return new ResponseEntity<PaymentVoRes>(rows, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/findReconc", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> findReconc(PaymentForm req) throws Exception {
		String url=paymentService.findPaymentWithReconc(req); 
		Response r = new Response();
		r.with("result",url);
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/findStatus",method = RequestMethod.POST)
	public OrderResponse  findPayStatus(@RequestBody OrderRequest req) {
		OrderResponse orderResponse=paymentService.findPayStatus(req); 
		return orderResponse;
	}
	
	/**
	 * 异常单据查询
	 * @param startDate
	 * @param endDate
	 * @param reconciliationMark
	 * @param page
	 * @param row
	 * @return
	 */
	@RequestMapping(value = "/pageEX", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<PaymentVoRes> pageEX(@RequestParam String channelNo,
			@RequestParam String startDate,@RequestParam String endDate,@RequestParam String reconciliationMark,
			@RequestParam int page,@RequestParam int row) {
		if("noDate".equals(startDate)){
			PaymentVoRes rows = new PaymentVoRes();
			return new ResponseEntity<PaymentVoRes>(rows, HttpStatus.OK);
		}
		Timestamp sDate = new Timestamp(DateUtil.stringToDate(startDate).getTime());
		Timestamp edate = new Timestamp(DateUtil.getNextDay(DateUtil.stringToDate(endDate)).getTime());
		PaymentVoRes rows = paymentService.pageEX(channelNo, sDate, edate, reconciliationMark, page, row);
		return new ResponseEntity<PaymentVoRes>(rows, HttpStatus.OK);
	}
	
	/**
	 * 忽略对账
	 * @param oids
	 * @return
	 */
	@RequestMapping(value = "/ignoreRecon", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> ignoreRecon(@RequestParam String[] oids) {
		paymentService.ignoreRecon(oids);
		Response r = new Response();
		r.with("result","SUCCESS");
		return new ResponseEntity<Response>(HttpStatus.OK);
	}
	
	/**
	 * 批量指令管理
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/pageLargeAmount", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<PaymentVoRes> pageLargeAmount(PaymentForm req) {
		PaymentVoRes rows=paymentService.pageLargeAmount(req);
		return new ResponseEntity<PaymentVoRes>(rows, HttpStatus.OK);
	}
	
	/**
	 * 批量申请
	 * @param oids
	 * @param batchOperatorStatus
	 * @param batchOperatorReson
	 * @return
	 */
	@RequestMapping(value = "/batchUpdate", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> batchUpdate(@RequestParam String[] oids,
			@RequestParam String batchOperatorStatus, @RequestParam String batchOperatorReson) {
		int successPay = paymentService.batchUpdate(oids,batchOperatorStatus,batchOperatorReson);
		Response r = new Response();
		r.with("result","SUCCESS");
		r.with("succCount",successPay);
		r.with("totle",oids.length);
		return new ResponseEntity<Response>(r,HttpStatus.OK);
	}
	
	/**
	 * 批量审核
	 * @param oids
	 * @param orderNos
	 * @param reson
	 * @param auditStatus
	 * @param operatorTypes
	 * @param operatorStatus
	 * @param updateStatus
	 * @return
	 */
	@RequestMapping(value = "/batchAudit", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> batchAudit(@RequestParam String[] oids,@RequestParam String[] orderNos,
			@RequestParam String reson,@RequestParam String auditStatus,@RequestParam String[] operatorTypes,@RequestParam String[] operatorStatus,
			@RequestParam String[] updateStatus) {
		int successPay = paymentService.batchAudit(oids,orderNos,reson,auditStatus,operatorTypes,operatorStatus,updateStatus);
		Response r = new Response();
		r.with("result","SUCCESS");
		r.with("succCount",successPay);
		r.with("totle",oids.length);
		return new ResponseEntity<Response>(r,HttpStatus.OK);
	}
	
	/**
	 * 修改订单状态
	 * @param req
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/changeStatus", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> changeStatus(PaymentForm req) throws Exception {
		boolean change = paymentService.changeStatus(req);
		Response r = new Response();
		if(change){
			r.with("result","SUCCESS");
		}else{
			r.with("result","FAIL");
		}
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
	/**
	 * 查询提现待审核订单总金额
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/getAuditBalance", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<String> getAuditBalance() {
		String balance = paymentService.getMemBalance();
		return new ResponseEntity<String>(balance, HttpStatus.OK);
	}
}
