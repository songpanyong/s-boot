package com.guohuai.seetlement.task;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.guohuai.payadapter.redeem.ConcurrentManager;
import com.guohuai.payadapter.redeem.ConcurrentManagerDao;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @ClassName: TradeTaskJob
 * @Description: 并发锁检查
 * @author xueyunlong
 * @date 2016年11月26日 下午5:38:53
 *
 */
@Slf4j
@Component
public class ConcurrentCheckTask {

	@Autowired
	ConcurrentManagerDao concurrentManagerDao;

	@Value("${server.host}")
	private String host;

	@Scheduled(cron = "${jobs.concurrentCheck.schedule}")
	public void find() {
		try {
			// 查询超过两分钟未更新的，本机更新为master
			List<ConcurrentManager> concurrentManagerList = concurrentManagerDao.findAll();
			for (ConcurrentManager concurrentManager : concurrentManagerList) {
				Timestamp updateTime = concurrentManager.getUpdateTime();
				if (concurrentManager.getHost().equals(host)) {
					concurrentManagerDao.updateTime(concurrentManager.getOid());
				} else {
					// 如果更新时间超过2分钟表明处理超时，把状态更新为master
					if (checkUpdateTime(updateTime)) {
						concurrentManager.setUpdateTime(new Timestamp(System.currentTimeMillis()));
						concurrentManager.setHost(host);
						concurrentManagerDao.save(concurrentManager);
					}
				}
			}
		} catch (Exception e) {
			log.error("并发控制定时异常{}", e);
		}

	}

	private boolean checkUpdateTime(Timestamp upateTime) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, -5);
		Timestamp time = new Timestamp(calendar.getTimeInMillis());
		if (time.after(upateTime)) {
			log.info("切换机器为：{}",host);
			return true;
		}
		return false;
	}
}
