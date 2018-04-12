package com.guohuai.boot.pay.listener.event.recharge;

import com.guohuai.component.common.AbsEvent;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**   
 * @Description: 支付充值对象   
 * @author ZJ   
 * @date 2018年1月17日 下午5:32:16 
 * @version V1.0   
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PayRechargeEvent extends AbsEvent {
	private static final long serialVersionUID = 6266546809213203637L;
}