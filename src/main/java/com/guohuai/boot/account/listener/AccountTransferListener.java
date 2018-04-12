package com.guohuai.boot.account.listener;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.AccOrderDao;
import com.guohuai.boot.account.dao.AccountEventDao;
import com.guohuai.boot.account.dao.AccountEventTransDao;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.AccountEventTransEntity;
import com.guohuai.boot.account.listener.event.AccountTransferEvent;
import com.guohuai.boot.account.res.AccountEventResponse;
import com.guohuai.boot.account.service.AccountEventService;
import com.guohuai.boot.account.service.AccountTransferService;
import com.guohuai.component.exception.SETException;
import com.guohuai.component.util.Constant;
import com.guohuai.settlement.api.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
public class AccountTransferListener {

	@Autowired
	private AccountEventDao accountEventDao;
	@Autowired
	private AccOrderDao accOrderDao;
	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private AccountEventService accountEventService;
	@Autowired
	private AccountTransferService accountTransferService;
	@Autowired
	private AccountEventTransDao accountEventTransDao;
	
	/**
	 * 等账事件监听
	 * @param event 事件
	 */
    @EventListener
    public void transfer(AccountTransferEvent event) {
    	log.info("事件监听，请求参数{}",event);
    	// 获取订单信息
    	AccOrderEntity orderEntity = accOrderDao.findByOrderNo(event.getOrderNo());
    	if(orderEntity == null){
    		log.error("事件监听，获取订单信息异常");
    		event.setReturnCode(Constant.FAIL);
    		event.setErrorMessage("系统异常");
    		return;
    	}
    	// 获取事件流水
    	List<AccountEventTransEntity> eventTransEntityList = accountEventTransDao
    			.findPendingByOrderNoAndRequestNo(orderEntity.getOrderNo(), orderEntity.getRequestNo());
    	if(CollectionUtils.isEmpty(eventTransEntityList)){
    		log.error("事件监听，获取事件流水信息异常");
    		event.setReturnCode(Constant.FAIL);
    		event.setErrorMessage("系统异常");
    		return;
    	}
    	AccountEventResponse accountEventResp = null;
    	try{
    		accountEventResp = this.transfer(eventTransEntityList);
    	}catch (SETException sETException){
    		log.error("事件监听，处理账户转账异常",sETException);
    		event.setReturnCode(Constant.FAIL);
    		event.setErrorMessage("系统繁忙");
    		return;
    	}catch (Exception e){
    		log.error("事件监听，处理账户转账异常{}",e);
    		event.setReturnCode(Constant.FAIL);
    		event.setErrorMessage("系统异常");
    		return;
    	}
    	event.setReturnCode(accountEventResp.getReturnCode());
		event.setErrorMessage(accountEventResp.getErrorMessage());
    	log.info("事件监听，处理结果{}",JSONObject.toJSONString(event));
    }

	/**
	 *  获取出入款账户并转账
	 * @param eventTransEntityList
	 * @return
	 */
	@Transactional
	private AccountEventResponse transfer(List<AccountEventTransEntity> eventTransEntityList) {
		AccountEventResponse accountEventResp = new AccountEventResponse();
    	// 循环处理登帐事件
    	for(AccountEventTransEntity accountEventTransEntity : eventTransEntityList) {
			// 调用底层转账
			if (!StringUtil.isEmpty(accountEventTransEntity.getOutputAccountNo())) {
				BaseResponse subtractResp = accountTransferService.subtractBalance(accountEventTransEntity.getOutputAccountNo(),
						accountEventTransEntity.getInputAccountNo(), accountEventTransEntity.getBalance(),
						accountEventTransEntity.getChildEventType(), accountEventTransEntity.getOrderNo(),
						accountEventTransEntity.getRequestNo(), accountEventTransEntity.getTransNo());
				if (!BaseResponse.isSuccess(subtractResp)) {
					log.error("{}操作事件转账-扣减账户余额：{}, baseResponse= {} ", accountEventTransEntity.getChildEventType(),
							accountEventTransEntity.getOrderNo(), JSONObject.toJSON(subtractResp));
					throw new SETException(subtractResp.getReturnCode());
				}
			}
			if (!StringUtil.isEmpty(accountEventTransEntity.getInputAccountNo())) {
				BaseResponse addResp = accountTransferService.addBalance(accountEventTransEntity.getOutputAccountNo(),
						accountEventTransEntity.getInputAccountNo(), accountEventTransEntity.getBalance(),
						accountEventTransEntity.getChildEventType(), accountEventTransEntity.getOrderNo(),
						accountEventTransEntity.getRequestNo(), accountEventTransEntity.getTransNo());
				if (!BaseResponse.isSuccess(addResp)) {
					log.error("{}操作事件转账-增加账户余额：{}, baseResponse= {} ", accountEventTransEntity.getChildEventType(),
							accountEventTransEntity.getOrderNo(), JSONObject.toJSON(addResp));
					throw new SETException(addResp.getReturnCode());
				}
			}
		}
    	accountEventResp.setReturnCode(Constant.SUCCESS);
    	accountEventResp.setErrorMessage("交易成功");
		return accountEventResp;
	}

}