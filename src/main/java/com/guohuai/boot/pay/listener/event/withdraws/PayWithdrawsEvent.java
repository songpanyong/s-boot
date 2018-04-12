package com.guohuai.boot.pay.listener.event.withdraws;

import com.guohuai.component.common.AbsEvent;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**   
 * @Description: 支付提现对象  
 * @author ZJ   
 * @date 2018年1月17日 下午5:34:22 
 * @version V1.0   
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PayWithdrawsEvent extends AbsEvent {
	private static final long serialVersionUID = 8494756057006764773L;
}