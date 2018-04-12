package com.guohuai.boot.account.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.guohuai.account.api.request.AccountTransRequest;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.basic.component.exception.GHException;
import com.guohuai.boot.account.service.PurchaseTransService;
import com.guohuai.component.util.Constant;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TransListener {

    @Autowired
    private PurchaseTransService purchaseTransService;

    /**
     * 申购
     */
    @EventListener(condition = "#event.userType == 'T1' && #event.orderType == '01'")
    public void purchase(AccountTransRequest event) {
        try {
            AccountTransResponse transResponse = purchaseTransService.purchaseTrans(event);
            event.setErrorMessage(transResponse.getErrorMessage());
            event.setReturnCode(transResponse.getReturnCode());
        } catch (GHException e) {
        	  log.error("申购异常：{}", e);
            event.setErrorMessage(e.getMessage());
            event.setReturnCode(Integer.toString(e.getCode()));
        } catch (Exception e) {
            log.error("申购异常：{}", e);
            event.setErrorMessage("未知错误");
            event.setReturnCode(Constant.FAIL);
        }
    }

}