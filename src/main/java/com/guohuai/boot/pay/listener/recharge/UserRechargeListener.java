package com.guohuai.boot.pay.listener.recharge;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.guohuai.boot.pay.listener.event.recharge.PayRechargeEvent;

import lombok.extern.slf4j.Slf4j;

/**   
 * @Description: 用户充值监听
 * @author ZJ   
 * @date 2018年1月17日 下午2:18:00 
 * @version V1.0   
 */
@Component
@Slf4j
public class UserRechargeListener {
	/**
	 * 充值
	 * @param req
	 * @throws Exception
	 */
	@EventListener(condition = "#event.transType =='50' && #event.eventType == '01'")
	public void recharge(PayRechargeEvent event) throws Exception {
		log.info("用户充值请求参数：payRechargeEvent = {}", event);
	}
}