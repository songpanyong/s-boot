package com.guohuai.boot.pay.controller;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.guohuai.basic.component.ext.web.Response;
import com.guohuai.boot.pay.dao.ReconciliationPassDao;
import com.guohuai.boot.pay.res.ReconciliationPassVoRes;
import com.guohuai.boot.pay.service.OutsideReconciliationService;
import com.guohuai.boot.pay.service.ReconciliationPassService;
import com.guohuai.boot.pay.service.ReconciliationRecordsService;
import com.guohuai.boot.pay.vo.ReconciliationPassVo;
import com.guohuai.boot.pay.vo.ReconciliationRecordsVo;
import com.guohuai.component.common.TemplateQueryController;
import com.guohuai.component.util.DateUtil;
import com.guohuai.component.util.PayTwoRedisUtil;
import com.guohuai.payadapter.component.TradeChannel;

@Slf4j
@RestController
@RequestMapping(value = "/settlement/pass"/*, produces = "application/json;charset=utf-8"*/)
public class ReconciliationPassController extends TemplateQueryController<ReconciliationPassVo,ReconciliationPassDao>{

	@Autowired
	private ReconciliationPassService reconciliationPassService;
	
	@Autowired
	private ReconciliationRecordsService reconciliationRecordsService;
	
	@Autowired
	private OutsideReconciliationService outsideReconciliationService;
	@Autowired
	private PayTwoRedisUtil payTwoRedisUtil;
	
	@RequestMapping(value = "/recon", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> save() {
		reconciliationPassService.reconciliation();
		Response r = new Response();
		r.with("result","SUCCESS");
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
	/**
	 * 查询对账结果
	 * @param checkDate
	 * @param reconStatus
	 * @return
	 */
	@RequestMapping(value = "/page", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<ReconciliationPassVoRes> page(@RequestParam String channelId,
			@RequestParam String checkDate,@RequestParam int reconStatus,
			@RequestParam int page,@RequestParam int row) {
		//根据日期获取时间区间
		Date date = DateUtil.stringToDate(checkDate);
	    Timestamp startDate = new Timestamp(date.getTime());
	    Timestamp endDate = new Timestamp((DateUtil.getNextDay(date)).getTime()); 
	    
		ReconciliationPassVoRes rows=reconciliationPassService.page(channelId, startDate, endDate, reconStatus, page, row);
		return new ResponseEntity<ReconciliationPassVoRes>(rows, HttpStatus.OK);
	}
	
	/**
	 * 金运通导入对账文件
	 * @param checkDate
	 * @param localUrl
	 * @param channelId
	 * @return
	 */
	@RequestMapping(value = "/uploadJytRecon", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> uploadJytRecon(@RequestParam String checkDate,@RequestParam String channelId,
			@RequestParam(value = "file") MultipartFile file) {
//		String operator=this.getLoginAdmin();//可添加导入人
		Map<String,String> returnMap = new HashMap<String,String>();
		//判断日期是否已上传过对账文件或已完成对账
		ReconciliationRecordsVo vo = reconciliationRecordsService.findByDate(checkDate,channelId);
		if(vo == null){//未导入对账文件
			returnMap = reconciliationPassService.uploadJytRecon(checkDate, channelId,file);
		}else if("1".equals(vo.getReconStatus())){
			returnMap.put("responseCode", "FAIL");
	        returnMap.put("responseMsg", "已导入过该日期对账文件，无需再次导入");
		}
		Response r = new Response();
		r.with("result",returnMap.get("responseCode"));
		r.with("resultDetial",returnMap.get("responseMsg"));
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
	/**
	 * 宝付获取对账流水
	 * @param checkDate
	 * @param localUrl
	 * @param channelId
	 * @return
	 */
	@RequestMapping(value = "/uploadBaofooRecon", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> uploadBaofooRecon(@RequestParam String checkDate,
			@RequestParam String channelId) {
		Response r = new Response();
		Map<String,String> returnMap = new HashMap<String,String>();
//		String operator=this.getLoginAdmin();//可添加导入人
		//放入redis禁止自动或手动再次操作对账
		log.info("手动对账，增加redis缓存，防止重复对账，对账日期:{}", checkDate);
		Long check = payTwoRedisUtil.setRedisByTime("reconciliation_redis_tag" + checkDate, checkDate);
		if (check.intValue() == 0) {
			returnMap.put("responseCode", "FAIL");
	        returnMap.put("responseMsg", "对账正在进行中，不能重复对账");
			log.error("对账正在进行中，不能重复对账");
		}else{
			//判断日期是否已上传过对账文件或已完成对账
			ReconciliationRecordsVo vo = reconciliationRecordsService.findByDate(checkDate,channelId);
			if(vo == null){//未导入对账文件
				returnMap = reconciliationPassService.getBaofooRecon(checkDate, channelId);
			}else if("1".equals(vo.getReconStatus())){
				returnMap.put("responseCode", "FAIL");
		        returnMap.put("responseMsg", "已导入过该日期对账文件，无需再次导入");
			}
		}
		log.info("获取对账流水完成删除redis该对账日期{}缓存",checkDate);
		payTwoRedisUtil.delRedis("reconciliation_redis_tag" + checkDate);
		r.with("result",returnMap.get("responseCode"));
		r.with("resultDetial",returnMap.get("responseMsg"));
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/uploadRecon", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> uploadYeePayRecon(@RequestParam String checkDate,
			@RequestParam String channelId) {
//		String operator=this.getLoginAdmin();//可添加导入人
		Response r = new Response();
		Map<String,String> returnMap = new HashMap<String,String>();
		log.info("手动对账，增加redis缓存，防止重复对账，对账日期:{}", checkDate);
		Long check = payTwoRedisUtil.setRedisByTime("reconciliation_redis_tag" + checkDate, checkDate);
		if (check.intValue() == 0) {
			returnMap.put("responseCode", "FAIL");
	        returnMap.put("responseMsg", "对账正在进行中，不能重复对账");
			log.error("对账正在进行中，不能重复对账");
		}else{
			//判断日期是否已上传过对账文件或已完成对账
			ReconciliationRecordsVo vo = reconciliationRecordsService.findByDate(checkDate,channelId);
			if(vo != null&&"1".equals(vo.getReconStatus())){
				returnMap.put("responseCode", "FAIL");
		        returnMap.put("responseMsg", "已导入过该日期对账文件，无需再次导入");
			}else{
				if(TradeChannel.baofooDkWithoiding.getValue().equals(channelId)
						||TradeChannel.baofooGateway.getValue().equals(channelId)
						||TradeChannel.baofoopay.getValue().equals(channelId)
						||TradeChannel.baofoopayee.getValue().equals(channelId)){
					returnMap = reconciliationPassService.getBaofooRecon(checkDate, channelId);
				}else if("19".equals(channelId)||"20".equals(channelId)){
					returnMap = reconciliationPassService.getYeePayRecon(checkDate, channelId);
				}else{
					returnMap.put("responseCode", "FAIL");
			        returnMap.put("responseMsg", "该支付通道不支持获取流水，请手动上传");
				}
			}
		}
		log.info("获取对账流水完成删除redis该对账日期{}缓存",checkDate);
		payTwoRedisUtil.delRedis("reconciliation_redis_tag" + checkDate);
		r.with("result",returnMap.get("responseCode"));
		r.with("resultDetial",returnMap.get("responseMsg"));
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
	/**
	 * 对账
	 * @param checkDate
	 * @param channelId
	 * @return
	 */
	@RequestMapping(value = "/ucfRecon", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> ucfCheck(@RequestParam String checkDate,
			@RequestParam String channelId) {

		ReconciliationRecordsVo vo = new ReconciliationRecordsVo();
//		String operator=this.getLoginAdmin();
//		req.setUserOid(operator);
		Response r = new Response();
		log.info("手动对账，增加redis缓存，防止重复对账，对账日期:{}", checkDate);
		Long check = payTwoRedisUtil.setRedisByTime("reconciliation_redis_tag" + checkDate, checkDate);
		if (check.intValue() == 0) {
			r.with("result","FAIL");
			r.with("resultDetial", "对账正在进行中，不能重复对账");
			log.error("对账正在进行中，不能重复对账");
			return new ResponseEntity<Response>(r, HttpStatus.OK);
		}
		//对账时先判断该日期是否已导入对账文件
		if(checkDate != null){
			//查询是否导入对账文件
			vo = reconciliationRecordsService.findByDate(checkDate,channelId);
			if(vo != null){//存在
				if("1".equals(vo.getReconStatus())){//已成功导入对账
					//对账
					Map<String,Object> returnMap = new HashMap<String,Object>();
					returnMap = outsideReconciliationService.orderReconciliation(checkDate, channelId);
					if("0000".equals(returnMap.get("responseCode"))){//对账成功
						r.with("result","SUCCESS");
					}else{
						r.with("result","FAIL");
						r.with("resultDetial", returnMap.get("responseMsg"));
					}
				}else{//未成功导入对账
					r.with("result","FAIL");
					r.with("resultDetial","未成功导入该日期对账文件，请先导入对账文件");
				}
			}else {//不存在
				r.with("result","FAIL");
				r.with("resultDetial","未导入该日期对账文件，请先导入对账文件");
			}
		}
		log.info("手动对账完成删除redis该对账日期{}缓存",checkDate);
		payTwoRedisUtil.delRedis("reconciliation_redis_tag" + checkDate);
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}

	/**
	 * 对账忽略
	 * @param oids
	 * @return
	 */
	@RequestMapping(value = "/ignoreRecon",method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> ignore(@RequestParam String[] oids) {
//		String operator=this.getLoginAdmin();
//		form.setUserOid(operator);
		Response r = new Response();
		reconciliationPassService.ignore(oids);
		r.with("result", "SUCCESS");
		return new ResponseEntity<Response>(r,HttpStatus.OK);
	}
	
	/**
	 * 导入先锋支付对账文件
	 * @param checkDate 对账文件时间
	 * @param channelId 通道id
	 * @param file 对账文件
	 * @return 导入结果
	 */
	@RequestMapping(value = "/uploadUcfRecon", method = {RequestMethod.POST,RequestMethod.GET})
	public @ResponseBody ResponseEntity<Response> uploadUcfRecon(@RequestParam String checkDate,
			@RequestParam String channelId,@RequestParam(value = "file") MultipartFile file) {
//		String operator=this.getLoginAdmin();//可添加导入人
		Map<String,String> returnMap = new HashMap<String,String>();
		//判断日期是否已上传过对账文件或已完成对账
		ReconciliationRecordsVo vo = reconciliationRecordsService.findByDate(checkDate,channelId);
		if(vo == null){//未导入对账文件
			returnMap = reconciliationPassService.uploadUcfRecon(checkDate,channelId,file);
		}else if("1".equals(vo.getReconStatus())){
			returnMap.put("responseCode", "FAIL");
	        returnMap.put("responseMsg", "已导入过该日期对账文件，无需再次导入");
		}
		Response r = new Response();
		r.with("result",returnMap.get("responseCode"));
		r.with("resultDetial",returnMap.get("responseMsg"));
		return new ResponseEntity<Response>(r, HttpStatus.OK);
	}
	
}
