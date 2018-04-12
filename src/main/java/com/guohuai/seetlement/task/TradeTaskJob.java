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
 * 
 * @ClassName: TradeTaskJob
 * @Description: 交易推送业务系统
 * @author xueyunlong
 * @date 2016年11月26日 下午5:38:53
 *
 */
@Slf4j
@Component
public class TradeTaskJob {

	@Autowired
	CallBackDao callbackDao;
	@Autowired
	ConcurrentManagerDao concurrentManagerDao;
	@Autowired
	CallbackSyncService callbackSyncService;
	@Value("${server.host}")
	private String host;

	@Scheduled( fixedDelayString = "${jobs.tradetask.schedule:2000}")
	public void find() {
		try {
			ConcurrentManager concurrentManager = concurrentManagerDao.findByHostAndTaskTag(host,"tradeMinCallback");
			if (null!=concurrentManager) {
				List<CallBackInfo> list = callbackDao.querySettlementCallBackAll("settlement");
				for (CallBackInfo callBackInfo : list) {
					callbackSyncService.callbackSync(callBackInfo,"1");
				}
			}
		} catch (Exception e) {
			log.error("定时callback异常{}",e);
		}
	}

}
