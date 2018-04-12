package com.guohuai.boot.account.listener.event.transfer;

import java.math.BigDecimal;

import com.guohuai.component.common.AbsEvent;

import lombok.Data;
import lombok.EqualsAndHashCode;

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
	/** 平台id */
	private String userOid;
	/** 事件名称 */
	private String eventName;
	/** 交易金额 */
	private BigDecimal transAmount;
}