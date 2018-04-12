package com.guohuai.boot.account.validate.listener;

import com.guohuai.boot.account.listener.event.transfer.AccountTransferEvent;
import com.guohuai.component.util.ErrorEnum;

/**   
 * @Description: 转账监听验证 
 * @author ZJ   
 * @date 2018年1月18日 下午4:12:53 
 * @version V1.0   
 */
public class TransferListenerVal {
	/**
	 * 验证转账入参
	 * @param event
	 * @return
	 */
	public static AccountTransferEvent valTransfer(AccountTransferEvent event) {
		AccountTransferEvent result = new AccountTransferEvent();

		if (null == event) {
			result.setError("9051");// 转账对象为空
			return result;
		}
		if (null == event.getTransAmount() || 0 == event.getTransAmount().intValue()) {
			result.setError("9052");// 交易金额为空
			return result;
		}
		if (event.getTransAmount().intValue() < 0) {
			result.setError("9053");// 交易金额为负数
			return result;
		}

		result.setError(ErrorEnum.SUCCESS.getCode());// 成功
		return result;
	}
}