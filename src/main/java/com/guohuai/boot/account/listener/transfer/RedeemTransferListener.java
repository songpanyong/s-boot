package com.guohuai.boot.account.listener.transfer;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.guohuai.boot.account.listener.event.transfer.AccountTransferEvent;
import static com.guohuai.boot.account.validate.listener.TransferListenerVal.valTransfer;

import lombok.extern.slf4j.Slf4j;

/**   
 * @Description: 赎回转账监听  
 * @author ZJ   
 * @date 2018年1月17日 上午11:50:49 
 * @version V1.0   
 */
@Component
@Slf4j
public class RedeemTransferListener {
	/**
	 * 转账
	 * @param req
	 * @throws Exception
	 */
	@EventListener(condition = "#event.transType =='02' && #event.eventType == '03'")
	public void transfer(AccountTransferEvent event) throws Exception {
		log.info("赎回转账请求参数：accountTransferEvent = {}", event);

		// 验证入参
		AccountTransferEvent result = valTransfer(event);
		if (null != result) {
			log.debug(result.getErrorMessage());
			event.setError(result.getReturnCode());
			return;
		}
	}
}