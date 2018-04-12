package com.guohuai.boot.pay.controller;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.component.ext.web.Response;
import com.guohuai.boot.pay.dao.ComOrderDao;
import com.guohuai.boot.pay.form.OrderForm;
import com.guohuai.boot.pay.res.OrderVoRes;
import com.guohuai.boot.pay.service.ComOrderBootService;
import com.guohuai.boot.pay.service.ComOrderConfirmService;
import com.guohuai.boot.pay.service.ComOrderService;
import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.component.common.TemplateQueryController;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.ExcelUtil;
import com.guohuai.settlement.api.request.CheckInAccountOrderRequest;
import com.guohuai.settlement.api.request.DepositConfirmRequest;
import com.guohuai.settlement.api.request.OrderAccountRequest;
import com.guohuai.settlement.api.request.OrderRequest;
import com.guohuai.settlement.api.response.BaseResponse;
import com.guohuai.settlement.api.response.DepositConfirmResponse;
import com.guohuai.settlement.api.response.OrderAccountResponse;
import com.guohuai.settlement.api.response.OrderResponse;

/**
 * @author xueyunlong
 * @ClassName: ComOrderController
 * @Description: 申购、赎回定单
 * @date 2016年11月28日 下午3:21:26
 */
@Slf4j
@RestController
@RequestMapping(value = "/settlement/order")
public class ComOrderController extends TemplateQueryController<OrderVo, ComOrderDao> {

    @Autowired
    private ComOrderService orderService;
    @Autowired
    private ComOrderBootService comOrderBootService;
    @Autowired
    private ComOrderConfirmService orderConfirmService;

    /**
     * 支付|付款
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/pay", method = RequestMethod.POST)
    public OrderResponse pay(@RequestBody OrderRequest req) {
        OrderResponse orderResponse = new OrderResponse();
        if ("pc".equals(req.getPaymentMethod())) {
            orderResponse = orderService.gatewayRrade(req);
        } else {
            orderResponse = orderService.trade(req);
        }
        return orderResponse;
    }

    /**
     * 提现申请
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/applyWithdrawal", method = RequestMethod.POST)
    public OrderResponse applyWithdrawal(@RequestBody OrderRequest req) {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse = orderService.applyWthdrawal(req);
        return orderResponse;
    }

    /**
     * 提现确认
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/confirmWthdrawal", method = RequestMethod.POST)
    public OrderResponse confirmWthdrawal(@RequestBody OrderRequest req) {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse = orderService.confirmWthdrawal(req);
        return orderResponse;
    }

    /**
     * 解冻
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/unforzenUserWithdrawal", method = RequestMethod.POST)
    public OrderResponse unforzenUserWithdrawals(@RequestBody OrderRequest req) {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse = orderService.unforzenUserWithdrawals(req);
        return orderResponse;
    }

    /**
     * 重新获取验证码
     */
    @RequestMapping(value = "/reValidPay", method = RequestMethod.POST)
    public OrderResponse reValidPay(@RequestBody OrderRequest req) {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse = orderService.reValidPay(req);
        return orderResponse;
    }

    /**
     * 验证码（验证支付）
     */
    @RequestMapping(value = "/validPay", method = RequestMethod.POST)
    public OrderResponse validPay(@RequestBody OrderRequest req) {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse = orderService.validPay(req);
        return orderResponse;
    }

    /**
     * 代扣
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/withholding", method = RequestMethod.POST)
    public OrderResponse withoiding(@RequestBody OrderRequest req) {
        log.info("接收代扣请求 OrderRequest =[{}] ", JSONObject.toJSON(req));
        OrderResponse orderResponse = new OrderResponse();
        orderResponse = orderService.witholding(req);
        log.info("代扣返回orderResponse =[{}] ", JSONObject.toJSON(orderResponse));
        return orderResponse;
    }

    /**
     * 网关支付
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/gatewayPay", method = RequestMethod.POST)
    public OrderResponse gatewayPay(@RequestBody OrderRequest req) {

        OrderResponse orderResponse = orderService.gatewayRrade(req);
        log.info("gatewayPay orderResponse:{}", JSONObject.toJSON(orderResponse));
        return orderResponse;
    }

    /**
     * 收款
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/payee", method = RequestMethod.POST)
    public OrderResponse payee(@RequestBody OrderRequest req) {
        OrderResponse orderResponse = orderService.trade(req);
        return orderResponse;
    }


    @RequestMapping(value = "/queryPay", method = RequestMethod.POST)
    public OrderResponse queryPay(@RequestBody OrderRequest req) {
        OrderResponse orderResponse = orderService.queryPay(req);
        return orderResponse;
    }


    @RequestMapping(value = "/page", method = {RequestMethod.POST, RequestMethod.GET})
    public @ResponseBody
    ResponseEntity<OrderVoRes> page(OrderForm req) {
        OrderVoRes rows = comOrderBootService.page(req);
        return new ResponseEntity<OrderVoRes>(rows, HttpStatus.OK);
    }

    @RequestMapping(value = "/findOne", method = {RequestMethod.POST, RequestMethod.GET})
    public @ResponseBody
    ResponseEntity<Response> findOne(@RequestParam String oid) {
        OrderVo vo = comOrderBootService.findOne(oid);
        Response r = new Response();
        r.with("result", vo);
        return new ResponseEntity<Response>(r, HttpStatus.OK);
    }


    @RequestMapping(value = "/down", method = {RequestMethod.POST, RequestMethod.GET})
    public void down(OrderForm req) {
        try {
            List<List<String>> data = comOrderBootService.data(req);
            List<String> header = comOrderBootService.header();
            String name = "结算订单导出";
            // 生成excel
            SXSSFWorkbook book = ExcelUtil.generate(name, header, data, null);
            response.setContentType("application/ms-excel;charset=UTF-8");
//			response.setHeader("Content-Disposition",
//					"attachment;filename="+new String(name.getBytes("UTF-8"), "ISO8859-1")+".xls");
            String fileName = URLEncoder.encode(name, "UTF8");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + fileName + ".xlsx");
            OutputStream os = response.getOutputStream();
            book.write(os);
            os.flush();
            os.close();
        } catch (Exception e) {
        	log.error("结算订单导出异常",e);
        }
    }


    /**
     * 获取订单数
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/getCounNum", method = RequestMethod.POST)
    public OrderAccountResponse getCounNum(@RequestBody OrderAccountRequest req) {
        OrderAccountResponse orderAccountResponse = comOrderBootService.getCounNum(req);
        return orderAccountResponse;
    }


    /**
     * 获取订单对账数据
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/getChackCompareData", method = RequestMethod.POST)
    public List<OrderAccountResponse> getChackCompareData(@RequestBody OrderAccountRequest req) {
        List<OrderAccountResponse> orderAccountResponse = comOrderBootService.getChackCompareData(req);
        return orderAccountResponse;
    }

    /**
     * 获取商户余额(暂宝付一家渠道)
     */
    @RequestMapping(value = "/getMemBalance", method = {RequestMethod.POST, RequestMethod.GET})
    public OrderResponse getMemBalance() {
        OrderResponse orderResponse = orderService.getMemBalanceQuery();
        return orderResponse;
    }

    /**
     * 充值订单对账
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/depositConfirm", method = RequestMethod.POST)
    public DepositConfirmResponse depositConfirm(@RequestBody DepositConfirmRequest req) {
        DepositConfirmResponse orderResponse = orderConfirmService.depositConfirm(req);
        return orderResponse;
    }

    @PostMapping(value = "/checkInOrder")
    public @ResponseBody
    BaseResponse checkInOrder(OrderForm req) {
        log.info("订单手工录入：{}",req);
        BaseResponse resp = null;
        try {
            resp = comOrderBootService.checkInOrder(req);
        } catch (Exception e) {
        	log.error("系统异常：{}",e);
            resp = new BaseResponse();
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("系统繁忙！");
        }
        log.info("订单手工录入：{}",resp);
        return resp;
    }


    @PostMapping(value = "/checkInOrderAudit")
    public @ResponseBody
    BaseResponse checkInOrderAudit(OrderForm req) {
        log.info("审核订单手工录入：{}",req);
        BaseResponse resp = null;
        try {
            resp = comOrderBootService.checkInOrderAudit(req);
        } catch (Exception e) {
        	log.error("系统异常：{}",e);
            resp = new BaseResponse();
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("系統繁忙！");
        }
        log.info("审核订单手工录入：{}",resp);
        return resp;
    }
    
    @RequestMapping(value = "/checkInAccountOrder", method = RequestMethod.POST)
    public BaseResponse checkInAccountOrder(@RequestBody CheckInAccountOrderRequest checkInAccountRequest) {
        log.info("业务订单手工录入：{}",checkInAccountRequest);
        BaseResponse resp = null;
        try {
            resp = comOrderBootService.checkInAccountOrder(checkInAccountRequest);
        } catch (Exception e) {
        	log.error("系统异常：{}",e);
            resp = new BaseResponse();
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("系统繁忙！");
        }
        log.info("业务订单手工录入：{}",resp);
        return resp;
    }

}
