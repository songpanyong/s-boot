package com.guohuai.boot.pay.listener.withdraws;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.guohuai.boot.pay.listener.event.withdraws.PayWithdrawsEvent;

import lombok.extern.slf4j.Slf4j;

/**   
 * @Description: 用户提现监听
 * @author ZJ   
 * @date 2018年1月17日 下午2:19:26 
 * @version V1.0   
 */
@Component
@Slf4j
public class UserWithdrawsListener {
	/**
	 * 提现
	 * @param req
	 * @throws Exception
	 */
	@EventListener(condition = "#event.transType =='51' && #event.eventType == '02'")
	public void withdraws(PayWithdrawsEvent event) throws Exception {
		log.info("用户提现请求参数：payWithdrawsEvent = {}", event);
	}
}