package com.guohuai.seetlement.task;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.guohuai.payadapter.redeem.CallBackDao;
import com.guohuai.payadapter.redeem.CallBackInfo;
import com.guohuai.payadapter.redeem.ConcurrentManager;
import com.guohuai.payadapter.redeem.ConcurrentManagerDao;

import lombok.extern.slf4j.Slf4j;

/**
 *银行回调
 * 
 * @author xyl
 *
 */

@Slf4j
@Component
public class BankCallbackTaskJob {

	@Autowired
	CallBackDao callbackDao;

	@Autowired
	ConcurrentManagerDao concurrentManagerDao;
	
	@Autowired
	BankCallSyncBackService bankCallSyncBackService;
	
	@Value("${server.host}")
	private String host;
	

	@Scheduled( fixedDelayString = "${jobs.bankCallback.schedule:10000}")
	public void find() {
		try {
			ConcurrentManager concurrentManager = concurrentManagerDao.findByHostAndTaskTag(host, "tradeCallback");
			if (null != concurrentManager) {
				List<CallBackInfo> list = callbackDao.queryCallBackAll("bank");// 查询金运通所有未处理的订单回调信息
				for (CallBackInfo callBackInfo : list) {
					bankCallSyncBackService.bankCallBack(callBackInfo,"1");
				}
			}
		} catch (Exception e) {
			log.error("定时查询定单状任务，执行异常{}", e);
		}
	}

	
	
}
