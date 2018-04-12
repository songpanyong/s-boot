package com.guohuai.seetlement.task;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.guohuai.boot.account.dao.AccountEventChangeRecordsDao;
import com.guohuai.boot.account.dao.AccountEventChildDao;
import com.guohuai.boot.account.dao.AccountEventDao;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.entity.AccountEventChangeRecordsEntity;
import com.guohuai.boot.account.entity.AccountEventChildEntity;
import com.guohuai.boot.account.entity.AccountEventEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.component.util.Constant;
import com.guohuai.payadapter.redeem.ConcurrentManager;
import com.guohuai.payadapter.redeem.ConcurrentManagerDao;
import com.guohuai.settlement.api.response.BaseResponse;

/**
 * 
 * @ClassName: AccountEventChangeTaskJob
 * @date 2016年12月18日 下午7:44:21
 *
 */
@Slf4j
@Component
public class AccountEventChangeTaskJob {

	@Autowired
	ConcurrentManagerDao concurrentManagerDao;

	@Value("${server.host}")
	private String host;

	@Autowired
	private AccountEventChangeRecordsDao accountEventChangeRecordsDao;
	@Autowired
	private AccountEventDao accountEventDao;
	@Autowired
	private AccountEventChildDao accountEventChildDao;
	@Autowired
	private AccountInfoDao accountInfoDao;
	

	@Scheduled(cron = "${jobs.accountEventChangeTask.schedule:0 0 0 * * ?}")
	public void find() {
		try {
			ConcurrentManager concurrentManager = concurrentManagerDao.findByHostAndTaskTag(host, "tradeMinCallback");
			if (null != concurrentManager) {
				List<AccountEventChangeRecordsEntity> records = accountEventChangeRecordsDao.findNeedChangeList();
				if (!CollectionUtils.isEmpty(records)) {
					BaseResponse resp = new BaseResponse();
					for (AccountEventChangeRecordsEntity record : records) {
						try {
							resp = this.effectiveEventChange(record);
							log.info("处理登账事件变更结果：{},{}",resp.getReturnCode(), resp.getErrorMessage());
						} catch (Exception e) {
							log.error("处理登账事件变更异常,ex={}", e);
							continue;
						}
						
					}
					
				}
			}
		} catch (Exception e) {
			log.error("定时AccountEventChangeTaskJob异常{}", e);
		}
	}
	
	/**
	 * 生效登账事件修改
	 * @param eventChangeRecordsEntity 变更记录
	 * @return 变更结果
	 */
	public BaseResponse effectiveEventChange(
			AccountEventChangeRecordsEntity eventChangeRecordsEntity) {
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		AccountEventChildEntity accountEventChildEntity = accountEventChildDao.findOne(eventChangeRecordsEntity.getEventChildOid());
		AccountInfoEntity inputEntity = accountInfoDao.findByAccountNo(eventChangeRecordsEntity.getNewIntputAccountNo());
		AccountInfoEntity outputEntity = accountInfoDao.findByAccountNo(eventChangeRecordsEntity.getNewOutputAccountNo());
		if(inputEntity == null || outputEntity == null){
			resp.setReturnCode(Constant.FAIL);
			resp.setReturnCode("获取更改账户信息异常");
			return resp;
		}
		accountEventChildEntity.setInputAccountName(inputEntity.getAccountName());
		accountEventChildEntity.setInputAccountNo(inputEntity.getAccountNo());
		accountEventChildEntity.setInputAccountType(inputEntity.getAccountType());
		accountEventChildEntity.setOutputAccountName(outputEntity.getAccountName());
		accountEventChildEntity.setOutputAccountNo(outputEntity.getAccountNo());
		accountEventChildEntity.setOutputUserType(outputEntity.getAccountType());
		accountEventChildDao.saveAndFlush(accountEventChildEntity);
		//事件更新为已生效
		accountEventDao.changeEventStatusByOid(eventChangeRecordsEntity.getEventOid(), AccountEventEntity.STATUS_YES);
		return resp;
	}

}
