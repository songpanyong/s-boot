package com.guohuai.seetlement.task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.guohuai.boot.pay.dao.ComChannelDao;
import com.guohuai.boot.pay.service.OutsideReconciliationService;
import com.guohuai.boot.pay.service.ReconciliationPassService;
import com.guohuai.boot.pay.service.ReconciliationRecordsService;
import com.guohuai.boot.pay.vo.ChannelVo;
import com.guohuai.boot.pay.vo.ReconciliationRecordsVo;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.DateUtil;
import com.guohuai.component.util.PayTwoRedisUtil;
import com.guohuai.payadapter.component.TradeChannel;
import com.guohuai.payadapter.redeem.ConcurrentManager;
import com.guohuai.payadapter.redeem.ConcurrentManagerDao;
import com.guohuai.settlement.api.response.BaseResponse;

/**
 * 
 * @ClassName: ReconciliationTaskJob
 * @Description: 自动获取对账数据并自动对账
 * @author chendonghui
 * @date 2018年1月10日 上午9:48:58
 *
 */
@Slf4j
@Component
public class ReconciliationTaskJob {

	@Autowired
	OutsideReconciliationService outsideReconciliationService;
	@Autowired
	ReconciliationPassService reconciliationPassService;
	@Autowired
	ReconciliationRecordsService reconciliationRecordsService;
    @Autowired
    ComChannelDao comChannelDao;
	@Autowired
	ConcurrentManagerDao concurrentManagerDao;
	@Value("${server.host}")
	private String host;
	@Autowired
	private PayTwoRedisUtil payTwoRedisUtil;

    @Scheduled(cron = "${jobs.reconciliationTaskJob.schedule:0 0,20,40,59 9 * * ? }")
	public void reconciliation() {
		try {
			ConcurrentManager concurrentManager = concurrentManagerDao.findByHostAndTaskTag(host,"tradeMinCallback");
			if (null!=concurrentManager) {
			    //查询开启的支付通道
                ChannelVo channel = comChannelDao.findBindChannel();
                if(null == channel){
                	log.error("支付通道未开启，无需自动获取对账数据");
                	return;
                }
                //获取对账时间
                Date now = new Date();
                Date lastDate = DateUtil.getLastDay(now);
                SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd");
                String lastDateStr=sdf.format(lastDate); 
                //放入redis禁止自动或手动再次操作对账
        		log.info("自动对账，增加redis缓存，防止重复对账，对账日期:{}", lastDateStr);
        		Long check = payTwoRedisUtil.setRedisByTime("reconciliation_redis_tag" + lastDateStr, lastDateStr);
        		if (check.intValue() == 0) {
        			log.error("对账正在进行中，不能重复对账");
        			return;
        		}
                BaseResponse baseResp = new BaseResponse();
                if(TradeChannel.baofoopay.getValue().equals(channel.getChannelNo())){
                	baseResp = this.getBaofooPayReconData(lastDateStr);
                }else if(("19").equals(channel.getChannelNo())){
                	baseResp = this.getYeePayReconData(lastDateStr);
                }else{
                	log.error("支付通道不支持自动对账，无需自动获取对账数据");
                	log.info("自动对账失败删除申请时redis该对账日期{}缓存",lastDateStr);
     				payTwoRedisUtil.delRedis("reconciliation_redis_tag" + lastDateStr);
                	return;
                }
                if(!Constant.SUCCESS.equals(baseResp.getReturnCode())){
                	log.error("支付通道自动获取对账数据失败，需手动获取或等待下次定时对账");
                	log.info("自动对账失败删除申请时redis该对账日期{}缓存",lastDateStr);
     				payTwoRedisUtil.delRedis("reconciliation_redis_tag" + lastDateStr);
                	return;
                }
                //自动对账
                if(TradeChannel.baofoopay.getValue().equals(channel.getChannelNo())){
                	baseResp = this.baofooPayReconciliation(lastDateStr);
                }else if(("19").equals(channel.getChannelNo())){
                	baseResp = this.yeePayReconciliation(lastDateStr);
                }
                log.info("自动对账完成删除申请时redis该对账日期{}缓存",lastDateStr);
				payTwoRedisUtil.delRedis("reconciliation_redis_tag" + lastDateStr);
			}
		} catch (Exception e) {
			log.error("定时对账异常{}",e);
		}
	}

    /**
     * 易宝自动对账
     * @param lastDateStr 对账日期
     * @return 对账结果
     */
    private BaseResponse yeePayReconciliation(String lastDateStr) {
    	BaseResponse baseResp = new BaseResponse();
		baseResp.setErrorMessage("对账完成");
		baseResp.setReturnCode(Constant.SUCCESS);
		// 易宝绑卡支付、易宝代付
		log.info("定时自动处理易宝绑卡支付对账，通道{}", "19");
    	Map<String,Object> returnMap = new HashMap<String,Object>();
		returnMap = outsideReconciliationService.orderReconciliation(lastDateStr, "19");
		if(Constant.SUCCESS.equals(returnMap.get("responseCode"))){
			log.info("自动对账完成易宝绑卡支付对账");
		}else{
			log.error("易宝绑卡支付自动对账失败，失败原因：{}", returnMap.get("responseMsg"));
		}
		log.info("定时自动处理易宝代付对账，通道{}", "20");
		returnMap = outsideReconciliationService.orderReconciliation(lastDateStr, "20");
		if(Constant.SUCCESS.equals(returnMap.get("responseCode"))){
			log.info("自动对账完成易宝代付对账");
		}else{
			log.error("易宝代付自动对账失败，失败原因：{}", returnMap.get("responseMsg"));
		}
		return baseResp;
	}

    /**
     * 宝付对账
     * @param lastDateStr 对账日期
     * @return 对账结果
     */
	private BaseResponse baofooPayReconciliation(String lastDateStr) {
		BaseResponse baseResp = new BaseResponse();
		baseResp.setErrorMessage("自动对账完成");
		baseResp.setReturnCode(Constant.SUCCESS);
		// 宝付认证支付、宝付代付、宝付网关支付
		log.info("定时自动处理宝付认证支付对账，通道{}", TradeChannel.baofoopay.getValue());
		Map<String,Object> returnMap = new HashMap<String,Object>();
		returnMap = outsideReconciliationService.orderReconciliation(lastDateStr, TradeChannel.baofoopay.getValue());
		if(Constant.SUCCESS.equals(returnMap.get("responseCode"))){
			log.info("自动对账完成宝付认证支付对账");
		}else{
			log.error("宝付认证支付自动对账失败，失败原因：{}", returnMap.get("responseMsg"));
		}
		log.info("定时自动处理宝付代付对账，通道{}", TradeChannel.baofoopayee.getValue());
		returnMap = outsideReconciliationService.orderReconciliation(lastDateStr, TradeChannel.baofoopayee.getValue());
		if(Constant.SUCCESS.equals(returnMap.get("responseCode"))){
			log.info("自动对账完成宝付代付对账");
		}else{
			log.error("宝付代付自动对账失败，失败原因：{}", returnMap.get("responseMsg"));
		}
		log.info("定时自动处理宝付网关支付对账，通道{}", TradeChannel.baofooGateway.getValue());
		returnMap = outsideReconciliationService.orderReconciliation(lastDateStr, TradeChannel.baofooGateway.getValue());
		if(Constant.SUCCESS.equals(returnMap.get("responseCode"))){
			log.info("自动对账完成宝付网关支付对账");
		}else{
			log.error("宝付网关支付自动对账失败，失败原因：{}", returnMap.get("responseMsg"));
		}
		return baseResp;
	}

	/**
     * 获取易宝对账数据
     * @param lastDateStr 对账日期
     * @return 获取结果
     */
	private BaseResponse getYeePayReconData(String lastDateStr) {
		BaseResponse baseResp = new BaseResponse();
		baseResp.setErrorMessage("获取成功");
		baseResp.setReturnCode(Constant.SUCCESS);
		//获取对账数据
		Map<String,String> returnMap = new HashMap<String,String>();
//        log.info("定时自动获取易宝绑卡支付对账数据，通道{}", TradeChannel.yeePayBindPay.getValue());
		log.info("定时自动获取易宝绑卡支付对账数据，通道{}", "19");
		// 判断是否以获取完成
        ReconciliationRecordsVo payVo = reconciliationRecordsService.findByDate(lastDateStr,"19");
		if(payVo == null){//未导入对账文件
			returnMap = reconciliationPassService.getYeePayRecon(lastDateStr, "19");
		}else if("1".equals(payVo.getReconStatus())){
			returnMap.put("responseCode", "SUCCESS");
			returnMap.put("responseMsg", "已导入过该日期对账文件，无需再次获取");
			log.info("已导入过该日期对账文件，无需再次获取");
		}
		if(!"SUCCESS".equals(returnMap.get("responseCode"))){
			baseResp.setErrorMessage(returnMap.get("responseCode"));
			baseResp.setReturnCode(Constant.FAIL);
			return baseResp;
		}
//		log.info("定时自动获取易宝提现对账数据，通道{}", TradeChannel.yeePayWithdraw.getValue());
		log.info("定时自动获取易宝提现对账数据，通道{}", "20");
		// 判断是否以获取完成
        ReconciliationRecordsVo payeeVo = reconciliationRecordsService.findByDate(lastDateStr,"20");
		if(payeeVo == null){//未导入对账文件
			returnMap = reconciliationPassService.getYeePayRecon(lastDateStr, "20");
		}else if("1".equals(payVo.getReconStatus())){
			returnMap.put("responseCode", "SUCCESS");
			returnMap.put("responseMsg", "已导入过该日期对账文件，无需再次获取");
			log.info("已导入过该日期对账文件，无需再次获取");
		}
		if(!"SUCCESS".equals(returnMap.get("responseCode"))){
			baseResp.setErrorMessage(returnMap.get("responseCode"));
			baseResp.setReturnCode(Constant.FAIL);
			return baseResp;
		}
		return baseResp;
	}

	/**
	 * 获取宝付对账数据
	 * @param lastDateStr 对账时间
	 * @return 获取结果
	 */
	private BaseResponse getBaofooPayReconData(String lastDateStr) {
		BaseResponse baseResp = new BaseResponse();
		baseResp.setErrorMessage("获取成功");
		baseResp.setReturnCode(Constant.SUCCESS);
		//获取对账数据
		Map<String,String> returnMap = new HashMap<String,String>();
        log.info("定时自动获取宝付认证支付对账数据，通道{}", TradeChannel.baofoopay.getValue());
        // 判断是否以获取完成
        ReconciliationRecordsVo payVo = reconciliationRecordsService.findByDate(lastDateStr,TradeChannel.baofoopay.getValue());
		if(payVo == null){//未导入对账文件
			returnMap = reconciliationPassService.getBaofooRecon(lastDateStr, TradeChannel.baofoopay.getValue());
		}else if("1".equals(payVo.getReconStatus())){
			returnMap.put("responseCode", "SUCCESS");
			returnMap.put("responseMsg", "已导入过该日期对账文件，无需再次获取");
			log.info("已导入过该日期对账文件，无需再次获取");
		}
		if(!"SUCCESS".equals(returnMap.get("responseCode"))){
			baseResp.setErrorMessage(returnMap.get("responseCode"));
			baseResp.setReturnCode(Constant.FAIL);
			return baseResp;
		}
		log.info("定时自动获取宝付代付对账数据，通道{}",TradeChannel.baofoopayee.getValue());
		// 判断是否以获取完成
        ReconciliationRecordsVo payeeVo = reconciliationRecordsService.findByDate(lastDateStr,TradeChannel.baofoopayee.getValue());
		if(payeeVo == null){//未导入对账文件
			returnMap = reconciliationPassService.getBaofooRecon(lastDateStr, TradeChannel.baofoopayee.getValue());
		}else if("1".equals(payeeVo.getReconStatus())){
			returnMap.put("responseCode", "SUCCESS");
			returnMap.put("responseMsg", "已导入过该日期对账文件，无需再次获取");
			log.info("已导入过该日期对账文件，无需再次获取");
		}
		if(!"SUCCESS".equals(returnMap.get("responseCode"))){
			baseResp.setErrorMessage(returnMap.get("responseCode"));
			baseResp.setReturnCode(Constant.FAIL);
			return baseResp;
		}
		log.info("定时自动获取宝付网关支付对账数据，通道{}",TradeChannel.baofooGateway.getValue());
		// 判断是否以获取完成
        ReconciliationRecordsVo gatewayVo = reconciliationRecordsService.findByDate(lastDateStr,TradeChannel.baofooGateway.getValue());
		if(gatewayVo == null){//未导入对账文件
			returnMap = reconciliationPassService.getBaofooRecon(lastDateStr, TradeChannel.baofooGateway.getValue());
		}else if("1".equals(gatewayVo.getReconStatus())){
			returnMap.put("responseCode", "SUCCESS");
			returnMap.put("responseMsg", "已导入过该日期对账文件，无需再次获取");
			log.info("已导入过该日期对账文件，无需再次获取");
		}
		if(!"SUCCESS".equals(returnMap.get("responseCode"))){
			baseResp.setErrorMessage(returnMap.get("responseCode"));
			baseResp.setReturnCode(Constant.FAIL);
			return baseResp;
		}
		return baseResp;
	}

}
