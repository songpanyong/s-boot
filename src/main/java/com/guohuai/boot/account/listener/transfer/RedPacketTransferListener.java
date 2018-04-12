package com.guohuai.boot.account.listener.transfer;

import java.math.BigDecimal;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.dto.AccountEventResDTO;
import com.guohuai.boot.account.entity.AccountEventChildEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.listener.event.transfer.AccountTransferEvent;
import com.guohuai.boot.account.service.AccountEventService;
import com.guohuai.component.util.ErrorEnum;

import static com.guohuai.boot.account.validate.listener.TransferListenerVal.valTransfer;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 红包转账监听
 * @author ZJ
 * @date 2018年1月17日 上午11:30:32
 * @version V1.0
 */
@Component
@Slf4j
public class RedPacketTransferListener {
	// 登账事件服务
	@Autowired
	private AccountEventService accountEventService;

	// 账户信息数据库操作
	@Autowired
	private AccountInfoDao accountInfoDao;

	@Autowired
	private EntityManager entityManager;

	/**
	 * 转账
	 * @param req
	 * @throws Exception
	 */
	@EventListener(condition = "#event.transType =='56' && #event.eventType == '03'")
	public void transfer(AccountTransferEvent event) throws Exception {
		log.info("红包转账请求参数：accountTransferEvent = {}", event);

		// 验证入参
		AccountTransferEvent result = valTransfer(event);
		if (!StringUtils.equals(ErrorEnum.SUCCESS.getCode(), result.getReturnCode())) {
			log.debug(result.getErrorMessage());
			event.setError(result.getReturnCode());
			return;
		}

		// 根据条件查询登账事件信息
		AccountEventResDTO accountEventResDTO = this.accountEventService.queryAccountEventInfo(event.getUserOid(),
				event.getTransType(), event.getEventType());
		if (!StringUtils.equals(ErrorEnum.SUCCESS.getCode(), accountEventResDTO.getReturnCode())) {
			log.debug("根据条件查询登账事件信息异常：{}", accountEventResDTO.getErrorMessage());
			event.setError(accountEventResDTO.getReturnCode());
			return;
		}

		// 转账操作
		transferDetail(accountEventResDTO, event);
	}

	/**
	 * 转账详情
	 * @param accountEventResDTO
	 * @param event
	 * @return
	 */
	private AccountTransferEvent transferDetail(AccountEventResDTO accountEventResDTO, AccountTransferEvent event) {
		try {
			List<AccountEventChildEntity> accountEventChildEntitys = accountEventResDTO.getAccountEventChildEntitys();
			for (AccountEventChildEntity accountEventChildEntity : accountEventChildEntitys) {
				AccountTransferEvent accountTransferEvent = transferForItem(accountEventChildEntity);
				if (!StringUtils.equals(ErrorEnum.SUCCESS.getCode(), accountTransferEvent.getReturnCode())) {
					event.setError(accountTransferEvent.getReturnCode());
					return accountTransferEvent;
				}
			}
		} catch (Exception e) {
			log.error("转账异常", e);
			event.setError("9060");
			return event;
		}
		event.setError(ErrorEnum.SUCCESS.getCode());
		return event;
	}

	/**
	 * 转账逐条处理
	 * @param accountEventChildEntity
	 * @throws Exception
	 */
	private AccountTransferEvent transferForItem(AccountEventChildEntity accountEventChildEntity) {
		AccountTransferEvent event = new AccountTransferEvent();

		// 验证出账账户号
		AccountInfoEntity outAccountInfoEntity = this.accountInfoDao
				.findByAccountNo(accountEventChildEntity.getOutputAccountNo());
		if (null == outAccountInfoEntity) {
			log.debug("根据出账账号查询账户信息为空：outputAccountNo = {}", accountEventChildEntity.getOutputAccountNo());
			event.setError("9061");
			return event;
		}
		// 验证出账账户余额
		BigDecimal balance = outAccountInfoEntity.getBalance().subtract(event.getTransAmount());
		if (balance.intValue() < 0) {
			log.debug("出账账户余额不足：balance = {}", balance);
			event.setError("9062");
			return event;
		}

		// 验证入账账户号
		AccountInfoEntity inAccountInfoEntity = this.accountInfoDao
				.findByAccountNo(accountEventChildEntity.getInputAccountNo());
		if (null == inAccountInfoEntity) {
			log.debug("根据入账账号查询账户信息为空：inputAccountNo = {}", accountEventChildEntity.getInputAccountNo());
			event.setError("9061");
			return event;
		}

		// 转账
		// 出账账户减钱
		outAccountInfoEntity = this.accountInfoDao.findByOidForUpdate(outAccountInfoEntity.getOid());
		this.entityManager.refresh(outAccountInfoEntity);
		int num = this.accountInfoDao.subtractBalance(event.getTransAmount(),
				accountEventChildEntity.getOutputAccountNo());
		if (num <= 0) {
			log.debug("出账账户扣款失败：outputAccountNo = {}", accountEventChildEntity.getOutputAccountNo());
			event.setError("9063");
			return event;
		}

		// 入账账户加钱
		inAccountInfoEntity = this.accountInfoDao.findByOidForUpdate(inAccountInfoEntity.getOid());
		this.entityManager.refresh(inAccountInfoEntity);
		num = this.accountInfoDao.addBalance(event.getTransAmount(), accountEventChildEntity.getInputAccountNo());
		if (num <= 0) {
			log.debug("入账账户入款失败：inputAccountNo = {}", accountEventChildEntity.getInputAccountNo());
			event.setError("9063");
			return event;
		}

		event.setError(ErrorEnum.SUCCESS.getCode());
		return event;
	}
}