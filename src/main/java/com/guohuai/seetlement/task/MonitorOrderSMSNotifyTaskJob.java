package com.guohuai.seetlement.task;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.guohuai.basic.component.ext.web.Response;
import com.guohuai.boot.account.entity.AccFailOrderNotifyEntity;
import com.guohuai.boot.account.service.AccFailOrderNotifyService;
import com.guohuai.boot.pay.form.BankCallbackForm;
import com.guohuai.boot.pay.service.SendMsgService;
import com.guohuai.payadapter.redeem.ConcurrentManager;
import com.guohuai.payadapter.redeem.ConcurrentManagerDao;

/**
 * 
 * @ClassName: MonitorOrderSMSNotifyTaskJob
 * @Description: 订单监控短信通知
 * @author chendonghui
 * @date 2017年3月15日 上午10:12:53
 *
 */
@Slf4j
@Component
@RequestMapping(value = "/settlement/task")
public class MonitorOrderSMSNotifyTaskJob {

	@Autowired
	private SendMsgService sendMsgService;
	
	@Autowired
	private AccFailOrderNotifyService accFailOrderNotifyService;

	@Autowired
	ConcurrentManagerDao concurrentManagerDao;
	
	@Value("${server.host}")
	private String host;
	
	@Value("${account.order.sendNotifySMSTimeInterval:2}")
	private int sendNotifySMSTimeInterval;
	
	@Scheduled(cron="${jobs.sendNotifySMS.schedule:0 0/1 * * * ?}")
	public void sendNotifySMS() {
		try {
			ConcurrentManager concurrentManager = concurrentManagerDao.findByHostAndTaskTag(host,"tradeMinCallback");
			if (null!=concurrentManager) {
				boolean sendResult = false;
				String mesParam = "订单异常通知！订单异常信息：";
				
				List<AccFailOrderNotifyEntity> needSendMsgList = accFailOrderNotifyService.getNeedSendMsgList(sendNotifySMSTimeInterval);
				if(needSendMsgList != null&& !needSendMsgList.isEmpty()){
					//组装短信
					int i = 1;
					for(AccFailOrderNotifyEntity entity : needSendMsgList){
						entity.setNotified("Y");//发送短信成功
						mesParam = mesParam +i+"、"+ entity.getOrderDesc();
						i++;
					}
					//发送短信
					sendResult = sendMsgService.sendMonitorNotifyMsg(mesParam);
					if(sendResult){
						accFailOrderNotifyService.update(needSendMsgList);
					}
				}else{
					log.info("无需发送短信通知");
				}
			}
		} catch (Exception e) {
			log.error("定时订单监控短信通知异常{}",e);
		}
	}
	
	//增加回调次数
		@RequestMapping(value = "/sendNotifySMS", method = {RequestMethod.POST,RequestMethod.GET})
		public @ResponseBody ResponseEntity<Response> handCallBack(BankCallbackForm req) {
			sendNotifySMS();
			Response r = new Response();
			r.with("result","SUCCESS");
			return new ResponseEntity<Response>(r, HttpStatus.OK);
		}

}
