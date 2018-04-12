package com.guohuai.boot.account.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.guohuai.boot.account.dao.AccountEventTransDao;
import com.guohuai.boot.account.entity.AccountEventTransEntity;

/**
 * @ClassName: AccountEventTransService
 * @Description: 账户事件流水
 * @author chendonghui
 * @date 2018年1月24日11:12:37
 */
@Service
public class AccountEventTransService {
	
	@Autowired
	private AccountEventTransDao accountEventTransDao;

	@Transactional(propagation = Propagation.REQUIRES_NEW) 
	public void saveEventTransList(
			List<AccountEventTransEntity> eventTransEntityList) {
		accountEventTransDao.save(eventTransEntityList);
	}

	@Transactional
	public void updateEventTransStatus(String orderNo, String orderStatus) {
		accountEventTransDao.updateEventTransStatus(orderNo, orderStatus);
	}
	
	/**
	 * 根据订单号查询登账明细
	 * @param orderNo
	 * @return
	 */
	public List<AccountEventTransEntity> findEventTransByOrderNo(String orderNo){
		return accountEventTransDao.findByOrderNo(orderNo);
	}
	
}