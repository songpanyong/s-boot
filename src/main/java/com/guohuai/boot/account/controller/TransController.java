package com.guohuai.boot.account.controller;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountBatchRedeemRequest;
import com.guohuai.account.api.request.AccountBatchTransferFrozenRequest;
import com.guohuai.account.api.request.AccountBatchTransferRequest;
import com.guohuai.account.api.request.AccountSettlementRequest;
import com.guohuai.account.api.request.AccountSingleTransferRequest;
import com.guohuai.account.api.request.AccountTransRequest;
import com.guohuai.account.api.request.AccountUnFrozenTransferRequest;
import com.guohuai.account.api.request.CreateOrderRequest;
import com.guohuai.account.api.request.EnterAccountRequest;
import com.guohuai.account.api.request.PurchaseTransCancelRequest;
import com.guohuai.account.api.request.TransDetailQueryRequest;
import com.guohuai.account.api.request.TransPublishRequest;
import com.guohuai.account.api.request.TransferAccountRequest;
import com.guohuai.account.api.response.AccountBatchRedeemResponse;
import com.guohuai.account.api.response.AccountSettlementResponse;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.account.api.response.AccountTransferResponse;
import com.guohuai.account.api.response.CreateOrderResponse;
import com.guohuai.account.api.response.EnterAccountResponse;
import com.guohuai.account.api.response.PurchaseTransCancelResponse;
import com.guohuai.account.api.response.TransDetailListResponse;
import com.guohuai.account.api.response.TransferAccountResponse;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.basic.component.ext.web.Response;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.service.AccOrderService;
import com.guohuai.boot.account.service.AccountContinuedService;
import com.guohuai.boot.account.service.AccountDividendOrderService;
import com.guohuai.boot.account.service.AccountNettingService;
import com.guohuai.boot.account.service.AccountRedeemService;
import com.guohuai.boot.account.service.AccountTransferService;
import com.guohuai.boot.account.service.AdapterService;
import com.guohuai.boot.account.service.TransService;
import com.guohuai.boot.pay.res.CreateBatchAccountOrderRes;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.OrderTypeEnum;

@Slf4j
@RestController
@RequestMapping(value = "/account/trans")
public class TransController {
	@Autowired
	private TransService transService;
	@Autowired
	private AccountDividendOrderService dividendOrderService;
	@Autowired
	private AdapterService adapterService;
	@Autowired
	private AccOrderService accOrderService;
	@Autowired
	private AccountRedeemService redeemService;
	@Autowired
	private AccountNettingService accountNettingService;
	@Autowired
	private AccountRedeemService accountRedeemService;
	@Autowired
	private AccountTransferService accountTransferService;
	@Autowired
	private AccountContinuedService accountContinuedService;

    @RequestMapping(value = "/trade", method = RequestMethod.POST)
    public AccountTransResponse add(@RequestBody AccountTransRequest req) {
        log.info("账户交易:请求参数[" + JSONObject.toJSONString(req) + "]");
        AccountTransResponse resp = new AccountTransResponse();
        BeanUtils.copyProperties(req, resp);
        // 派息拦截流程
        if (OrderTypeEnum.DIVIDEND.getCode().equals(req.getOrderType())) {
            resp = dividendOrderService.addDividendOrder(req);
            // 20170526 申购增加适配器处理
        } else if (StringUtil.in(req.getOrderType(), OrderTypeEnum.APPLY.getCode())) {
            // 接收定单
            CreateOrderResponse cResp = accOrderService.acceptOrder(req);
            if (Constant.SUCCESS.equals(cResp.getReturnCode())) {
                // 适配器
                final AccountTransRequest adapter = adapterService.adapter(req);
                resp.setErrorMessage(adapter.getErrorMessage());
                resp.setReturnCode(adapter.getReturnCode());
                // 更新收单结果
                if (Constant.SUCCESS.equals(adapter.getReturnCode())) {
                    accOrderService.updateOrderStatus(req.getOrderNo(), AccOrderEntity.ORDERSTATUS_SUCCESS);
                } else {
                    accOrderService.updateOrderStatus(req.getOrderNo(), AccOrderEntity.ORDERSTATUS_FAIL);
                }
            } else {
                resp.setErrorMessage(cResp.getErrorMessage());
                resp.setReturnCode(cResp.getReturnCode());
            }
        } else if(OrderTypeEnum.REDEEM.getCode().equals(req.getOrderType())){
    	    //赎回申请
			resp = redeemService.receiveRedeemApply(req);
		}else {
            // 其他情况走原来的业务逻辑
            resp = transService.trade(req);
        }
        log.info("tradeResp={}", JSONObject.toJSON(resp));
        return resp;
    }
    
    
    /**
	 * 发行人放款，收款
	 * 收款,orderType传58
	 * 放款,orderType传57
	* @param @param req
	* @param @return 
	* @return AccountTransResponse
	* @throws
	 */
	@RequestMapping(value = "/publishertrans", method = RequestMethod.POST)
	public AccountTransResponse publisherTrans(@RequestBody AccountTransRequest req) {
		AccountTransResponse resp = transService.publisherTransAndSaveOrder(req);
		return resp;
	}

	/**
	 *申购撤单
	 */
	@RequestMapping(value = "/transCancel", method = RequestMethod.POST)
	public PurchaseTransCancelResponse transCancel(@RequestBody PurchaseTransCancelRequest req) {
		log.info("申购撤单:{}",req);
		return transService.transCancel(req);

	}

	@RequestMapping(value = "/transferaccount", method = RequestMethod.POST)
	public TransferAccountResponse transferAccount(@RequestBody TransferAccountRequest req) {
		TransferAccountResponse resp = transService.transferAccount(req);
		return resp;
	}

	@RequestMapping(value = "/enteraccount", method = RequestMethod.POST)
	public EnterAccountResponse enterAccount(@RequestBody EnterAccountRequest req) {
		EnterAccountResponse resp = transService.enterAccount(req);
		return resp;
	}

	@RequestMapping(value = "/tradepublish", method = RequestMethod.POST)
	public AccountTransResponse tradepublish(@RequestBody List<TransPublishRequest> reqList) {
		AccountTransResponse resp = transService.trade(reqList);
		return resp;
	}

	@RequestMapping(value = "/detaillist", method = RequestMethod.POST)
	public @ResponseBody
	ResponseEntity<TransDetailListResponse> tansDetailQueryList(
			TransDetailQueryRequest req) {
		TransDetailListResponse resp = transService.tansDetailQueryList(req);
		return new ResponseEntity<TransDetailListResponse>(resp, HttpStatus.OK);
	}

	@RequestMapping(value = "/transFile", method = RequestMethod.POST)
	public @ResponseBody
	ResponseEntity<Response> TransFile(
			TransDetailQueryRequest req) throws Exception {
		String resp = transService.buildTransFile(req);
		Response r = new Response();
		r.with("result", resp);
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
	/**
	 * 赎回补单
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/supply", method = RequestMethod.POST)
	public AccountTransResponse supply(@RequestBody AccountTransRequest req) {
		log.info("账户赎回补单:请求参数[" + JSONObject.toJSONString(req) + "]");
		AccountTransResponse resp = new AccountTransResponse();
		req.setRemark("补单");
		BeanUtils.copyProperties(req, resp);
		resp = redeemService.receiveRedeemApply(req);
		log.info("tradeResp={}", JSONObject.toJSON(resp));
		return resp;
	}
	
	/**
	 * 赎回撤单
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/cancel", method = RequestMethod.POST)
	public AccountTransResponse cancel(@RequestBody AccountTransRequest req) {
		log.info("账户赎回撤单:请求参数[" + JSONObject.toJSONString(req) + "]");
		AccountTransResponse resp = new AccountTransResponse();
		req.setRemark("撤单");
		BeanUtils.copyProperties(req, resp);
		resp = redeemService.cancelOrder(req);
		log.info("tradeResp={}", JSONObject.toJSON(resp));
		return resp;
	}
	
	/**
	 * 赎回确认
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/redeemConfirm", method = RequestMethod.POST)
	public AccountTransResponse confirmRedeemList(@RequestBody List<CreateOrderRequest> reqOrderList) {
		log.info("账户赎回确认:请求笔数[" + reqOrderList.size() + "]");
		AccountTransResponse resp = new AccountTransResponse();
		resp = redeemService.confirmRedeemList(reqOrderList);
		log.info("tradeResp={}", JSONObject.toJSON(resp));
		return resp;
	}
	
	/**
	 * 轧差结算，批量赎回
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/nettingSettlement", method = RequestMethod.POST)
	public AccountSettlementResponse nettingSettlement(@RequestBody AccountSettlementRequest req) {
		log.info("接收轧差结算请求{}:",JSONObject.toJSON(req));
		AccountSettlementResponse resp = new AccountSettlementResponse();
		resp = accountNettingService.nettingSettlement(req);
		log.info("接收轧差结算返回{}", JSONObject.toJSON(resp));
		return resp;
	}
	
	@RequestMapping(value = "/batchRedeem", method = RequestMethod.POST)
	public AccountBatchRedeemResponse batchRedeem(@RequestBody AccountBatchRedeemRequest req) {
		log.info("批量赎回请求{}:",JSONObject.toJSON(req));
		AccountBatchRedeemResponse resp = new AccountBatchRedeemResponse();
		resp.setPublisherUserOid(req.getPublisherUserOid());
		resp.setRequestNo(req.getRequestNo());
		CreateBatchAccountOrderRes res = new CreateBatchAccountOrderRes();
		res = accountRedeemService.creatBatchAccountOrder(req);
		resp.setReturnCode(res.getReturnCode());
		resp.setErrorMessage(res.getErrorMessage());
		log.info("接收轧差结算，批量赎回返回{}", JSONObject.toJSON(resp));
		if(Constant.SUCCESS.equals(res.getReturnCode())){
			accountNettingService.batchRedeem(res.getAccOrderEntityList(),req.getRequestNo());
		}
		return resp;
	}
	
	/**
	 * 账户转账
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/transfer", method = RequestMethod.POST)
    public AccountTransResponse transfer(@RequestBody AccountTransRequest req) {
		log.info("AccountTransRequest={}", JSONObject.toJSON(req));
		AccountTransResponse resp = new AccountTransResponse();
		resp = accountTransferService.transfer(req);
		log.info("AccountTransResponse={}", JSONObject.toJSON(resp));
		return resp;
	}
	
	/**
	 * 批量转账
	 * @param req 批量转账订单
	 * @return 批量转账收单状态
	 */
	@RequestMapping(value = "/batchTransfer", method = RequestMethod.POST)
	public AccountTransferResponse batchTransfer(@RequestBody AccountBatchTransferRequest req){
		log.info("接收批量转账请求{}:",JSONObject.toJSON(req));
		AccountTransferResponse resp = new AccountTransferResponse();
		resp = accountTransferService.batchTransfer(req);
		log.info("接收批量转账返回{}", JSONObject.toJSON(resp));
		return resp;
	}

	/**
	 * 批量转账冻结
	 * @param req 冻结参数
	 * @return 冻结结果
	 */
	@RequestMapping(value = "/batchTransferFrozen", method = RequestMethod.POST)
	public AccountTransferResponse batchTransferFrozen(@RequestBody AccountBatchTransferFrozenRequest req){
		log.info("接收批量转账冻结请求{}:",JSONObject.toJSON(req));
		AccountTransferResponse resp = new AccountTransferResponse();
		resp = accountTransferService.batchTransferFrozen(req);
		log.info("接收批量转账冻结返回{}", JSONObject.toJSON(resp));
		return resp;
	}

	/**
	 * 单笔转账
	 * @param req 单笔转账参数
	 * @return 转账结果
	 */
	@RequestMapping(value = "/singleTransfer", method = RequestMethod.POST)
	public AccountTransferResponse singleTransfer(@RequestBody AccountSingleTransferRequest req){
		log.info("接收单笔转账请求:{}",JSONObject.toJSON(req));
		AccountTransferResponse resp = new AccountTransferResponse();
		resp = accountTransferService.singleTransfer(req);
		log.info("接收单笔转账返回:{}", JSONObject.toJSON(resp));
		return resp;
	}

	/**
	 * 解冻转账
	 * @param req 解冻转账参数
	 * @return 解冻结果
	 */
	@RequestMapping(value = "/unFrozenTransfer", method = RequestMethod.POST)
	public AccountTransferResponse unFrozenTransfer(@RequestBody AccountUnFrozenTransferRequest req){
		log.info("接收单笔转账请求{}:",JSONObject.toJSON(req));
		AccountTransferResponse resp = new AccountTransferResponse();
		resp = accountTransferService.unFrozenTransfer(req);
		log.info("接收单笔转账返回{}", JSONObject.toJSON(resp));
		return resp;
	}
	
	/**
	 * 活转定接口之赎回
	 * 
	 * @param req
	 * @return
	 */
    @RequestMapping(value = "/redeem", method = RequestMethod.POST)
    public AccountTransResponse transforRedeem(@RequestBody AccountTransRequest req) {
        log.info("活转定之赎回交易:请求参数[" + JSONObject.toJSONString(req) + "]");
        AccountTransResponse resp = new AccountTransResponse();
        if(OrderTypeEnum.REDEEM.getCode().equals(req.getOrderType())){
        	//赎回
        	resp = accountRedeemService.redeemApplyDay(req);
        }else{
        	resp.setReturnCode(Constant.FAIL);
        	resp.setErrorMessage("交易订单类型不对");
        	log.info("交易订单类型不对, 应该是orderType={}， 传入的错误订单类型orderType={}", OrderTypeEnum.REDEEM.getCode(), req.getOrderType());
        }
        
        log.info("活转定接口之赎回tradeResp={}", JSONObject.toJSON(resp));
        return resp;
    }
    
    /**
     * 活转定接口之续投（申购）
     * @param req
     * @return
     */
    @RequestMapping(value = "/purchase", method = RequestMethod.POST)
    public AccountTransResponse transforContinuPurchase(@RequestBody AccountTransRequest req) {
        log.info("活转定之续投交易:请求参数[" + JSONObject.toJSONString(req) + "]");
        AccountTransResponse resp = new AccountTransResponse();
        if(OrderTypeEnum.APPLY.getCode().equals(req.getOrderType())){
        	//续投（申购）
        	resp = accountContinuedService.accountContinuInvest(req);
        }else{
        	resp.setReturnCode(Constant.FAIL);
        	resp.setErrorMessage("交易订单类型不对");
        	log.info("交易订单类型不对, 应该是orderType={}， 传入的错误订单类型orderType={}", OrderTypeEnum.APPLY.getCode(), req.getOrderType());
        }
        
        log.info("活转定接口之续投（申购）tradeResp={}", JSONObject.toJSON(resp));
        return resp;
    }
    
    /**
     * 续投解冻
     * @param req
     * @return
     */
    @RequestMapping(value = "/continuUnFrozen", method = RequestMethod.POST)
    public AccountTransResponse transforContinuUnFrozen(@RequestBody AccountTransRequest req) {
        log.info("续投解冻交易:请求参数[" + JSONObject.toJSONString(req) + "]");
        AccountTransResponse resp = new AccountTransResponse();
        resp = accountContinuedService.accountContinuUnFrozen(req);
        log.info("续投解冻结果tradeResp={}", JSONObject.toJSON(resp));
        return resp;
    }
    
    /**
     * 产品户放款、收款
     * 订单类型：57放款 58收款
     * @param req
     * @return
     */
	@RequestMapping(value = "/productAccountTrans", method = RequestMethod.POST)
	public AccountTransResponse productAccountTrans(@RequestBody AccountTransRequest req) {
		log.info("产品户放款、收款入参：accountTransRequest = " + req);
		req.setTransAccountType(AccountTypeEnum.PRODUCT.getCode());
		AccountTransResponse result = transService.publisherTransAndSaveOrder(req);
		log.info("产品户放款、收款结果：" + result);
		return result;
	}
}