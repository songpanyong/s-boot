package com.guohuai.boot.account.listener.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.guohuai.component.common.AbsEvent;

/**   
 * @Description: 账户转账对象 
 * @author ZJ   
 * @date 2018年1月17日 下午5:20:55 
 * @version V1.0   
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AccountTransferEvent extends AbsEvent {
	private static final long serialVersionUID = -301490294761266669L;
	/** 订单号*/
	private String orderNo;
}