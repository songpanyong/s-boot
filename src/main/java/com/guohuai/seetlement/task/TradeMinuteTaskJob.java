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

@Slf4j
@Component
public class TradeMinuteTaskJob {

	@Autowired
	CallBackDao callbackDao;
	@Autowired
	CallbackSyncService callbackSyncService;
	@Autowired
	ConcurrentManagerDao concurrentManagerDao;
	@Value("${server.host}")
	private String host;

	//交易推送业务系统每分钟
	@Scheduled( fixedDelayString = "${jobs.tradeMintask.schedule:120000}")
	public void find() {
		try {
			ConcurrentManager concurrentManager = concurrentManagerDao.findByHostAndTaskTag(host,"tradeMinCallback");
			if (null!=concurrentManager) {
				List<CallBackInfo> list = callbackDao.querySettlementCallBackAll("settlement");
				for (CallBackInfo callBackInfo : list) {
					callbackSyncService.callbackSync(callBackInfo,"2");
				}
			}
		} catch (Exception e) {
			log.error("定时callback异常{}",e);
		}
	}

}
