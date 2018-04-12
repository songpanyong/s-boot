package com.guohuai.seetlement.task;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.component.collectinfo.CollectInfoSdk;
import com.guohuai.payadapter.control.Channel;
import com.guohuai.payadapter.control.ChannelDao;
import com.guohuai.payadapter.redeem.ConcurrentManager;
import com.guohuai.payadapter.redeem.ConcurrentManagerDao;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @ClassName: CollectTaskJob
 * @date 2016年11月26日 下午5:38:53
 *
 */
@Slf4j
@Component
public class CollectTaskJob {

	@Autowired
	ConcurrentManagerDao concurrentManagerDao;

	@Value("${server.host}")
	private String host;

	@Autowired
	private ChannelDao channelDao;

	@Scheduled(cron = "${jobs.collectTask.schedule:0 0 */3 * * ?}")
	public void find() {
		try {
			ConcurrentManager concurrentManager = concurrentManagerDao.findByHostAndTaskTag(host, "tradeMinCallback");
			if (null != concurrentManager) {
				List<Channel> channels = channelDao.findAll();
				if (!CollectionUtils.isEmpty(channels)) {
					CollectInfoSdk.collectInfo(JSONObject.toJSONString(channels));
				}
			}
		} catch (Exception e) {
			log.error("定时CollectTaskJob异常{}", e);
		}
	}

}
