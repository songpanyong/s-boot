package com.guohuai.boot.pay.controller;

import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.pay.form.ReconciliationErrorRecordsForm;
import com.guohuai.boot.pay.res.ReconciliationErrorRecordsVoRes;
import com.guohuai.boot.pay.service.ReconciliationErrorRecordsService;
import com.guohuai.component.util.Constant;
import com.guohuai.settlement.api.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 异常单据管理
 *
 * @author chendonghui
 * @date 2017年8月4日 下午1:21:11
 */
@Slf4j
@RestController
@RequestMapping(value = "/settlement/reconciliationErrorRecords")
public class ReconciliationErrorRecordsController {

    @Autowired
    private ReconciliationErrorRecordsService reconciliationErrorRecordsService;

    @PostMapping(value = "/page")
    public @ResponseBody
    ResponseEntity<ReconciliationErrorRecordsVoRes> page(ReconciliationErrorRecordsForm req) {
        ReconciliationErrorRecordsVoRes rows = reconciliationErrorRecordsService.page(req);
        return new ResponseEntity<ReconciliationErrorRecordsVoRes>(rows, HttpStatus.OK);
    }

    /**
     * 复合完成
     */
    @PostMapping(value = "/composite")
    public BaseResponse ArtificialComposite(ReconciliationErrorRecordsForm req) {
        log.info("复合完成 请求参数：{}", req);
        BaseResponse response = checkRequest(req);

        if (!response.getReturnCode().equals(Constant.SUCCESS)) {
            log.info("复合完成 响应：{}", response);
            return response;
        }
        try {
            response = reconciliationErrorRecordsService.artificialComposite(req);
        } catch (Exception e) {
            log.error("复合完成 未知异常：", e);
            response.setReturnCode(Constant.FAIL);
            response.setErrorMessage("未知异常");
        }
        log.info("复合完成 响应：{}", response);
        return response;
    }

    /**
     * 检查请求参数
     */
    private BaseResponse checkRequest(ReconciliationErrorRecordsForm req) {
        BaseResponse resp = new BaseResponse();
        resp.setReturnCode(Constant.SUCCESS);
        resp.setErrorMessage("成功");

        if (StringUtil.isEmpty(req.getOid())) {
            resp.setReturnCode(Constant.FAIL);
            resp.setErrorMessage("主键为空！");
            return resp;
        }
        return resp;
    }

    /**
     * 确认成功
     */
    @PostMapping(value = "/success")
    public BaseResponse sureSuccess(ReconciliationErrorRecordsForm req) {

        log.info("确认成功 请求参数：{}", req);
        BaseResponse response = checkRequest(req);

        if (!response.getReturnCode().equals(Constant.SUCCESS)) {
            log.info("确认成功 响应：{}", response);
            return response;
        }
        try {
            response = reconciliationErrorRecordsService.sureSuccess(req);
        } catch (Exception e) {
            log.error("确认成功 未知异常：", e);
            response.setReturnCode(Constant.FAIL);
            response.setErrorMessage("未知异常");
        }
        log.info("确认成功 响应：{}", response);
        return response;
    }

    /**
     * 确认失败（尝试扣款）
     * 对于充值先成功，后失败，系统对账时，尝试扣用户款。
     */
    @PostMapping(value = "/failedWithTryToCharge")
    public BaseResponse sureFailed1(ReconciliationErrorRecordsForm req) {

        log.info("确认失败（尝试扣款） 请求参数：{}", req);
        BaseResponse response = checkRequest(req);

        if (!response.getReturnCode().equals(Constant.SUCCESS)) {
            log.info("确认失败（尝试扣款） 响应：{}", response);
            return response;
        }
        try {
            response = reconciliationErrorRecordsService.sureFailed1(req);
        } catch (Exception e) {
            log.error("确认失败（尝试扣款） 未知异常：", e);
            response.setReturnCode(Constant.FAIL);
            response.setErrorMessage("未知异常");
        }
        log.info("确认失败（尝试扣款） 响应：{}", response);
        return response;
    }

    /**
     * 确认失败（不尝试扣款）
     * 在次确认失败，不尝试扣款，把定单状态改为失败
     */
    @PostMapping(value = "/failedWithNoCharge")
    public BaseResponse sureFailed2(ReconciliationErrorRecordsForm req) {

        log.info("确认失败（不尝试扣款） 请求参数：{}", req);
        BaseResponse response = checkRequest(req);

        if (!response.getReturnCode().equals(Constant.SUCCESS)) {
            log.info("确认失败（不尝试扣款） 响应：{}", response);
            return response;
        }
        try {
            response = reconciliationErrorRecordsService.sureFailed2(req);
        } catch (Exception e) {
            log.error("确认失败（不尝试扣款） 未知异常：", e);
            response.setReturnCode(Constant.FAIL);
            response.setErrorMessage("未知异常");
        }
        log.info("确认失败（不尝试扣款） 响应：{}", response);
        return response;
    }

}
