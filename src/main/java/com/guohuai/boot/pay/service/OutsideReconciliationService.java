package com.guohuai.boot.pay.service;

import com.guohuai.boot.pay.dao.PaymentDao;
import com.guohuai.boot.pay.dao.ReconciliationErrorRecordsDao;
import com.guohuai.boot.pay.dao.ReconciliationPassDao;
import com.guohuai.boot.pay.dao.ReconciliationStatisticsDao;
import com.guohuai.boot.pay.vo.PaymentVo;
import com.guohuai.boot.pay.vo.ReconciliationErrorRecordsVo;
import com.guohuai.boot.pay.vo.ReconciliationPassVo;
import com.guohuai.boot.pay.vo.ReconciliationStatisticsVo;
import com.guohuai.component.util.CheckConstant;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.DateUtil;
import com.guohuai.component.util.TradeTypeEnum;
import com.guohuai.payadapter.component.TradeChannel;
import com.guohuai.settlement.api.response.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
/**
 * 对账
 * @author chendonghui
 *
 */
@Service
public class OutsideReconciliationService {
	private final static Logger log = LoggerFactory.getLogger(OutsideReconciliationService.class);
	
	@Autowired
	private PaymentDao paymentDao;
	
	@Autowired
	private ReconciliationPassDao reconciliationPassDao;
	
	@Autowired
	private ReconciliationErrorRecordsDao reconciliationErrorRecordsDao;
	
	@Autowired
	private ReconciliationWithdrawalsService reconciliationWithdrawalsService;
	
	@Autowired
	private ReconciliationRechargeService reconciliationRechargeService;
	
	@Autowired
	private ReconciliationStatisticsDao reconciliationStatisticsDao;
	
	@Value("${baofoo.memberId:100000178}")
	String memberId;
	
	public Map<String,Object> orderReconciliation(String checkTime, String channel){
		log.info("{},对账,{},","receiveTime="+checkTime);
		//根据日期获取时间区间
		Date date = DateUtil.stringToDate(checkTime);
	    Timestamp startDate = new Timestamp(date.getTime());
	    Timestamp endDate = new Timestamp((DateUtil.getNextDay(date)).getTime());
	    Date now = new Date();
	    Timestamp updateDate = new Timestamp(now.getTime());//当前时间
	    Map<String,Object> resultMap = new HashMap<String,Object>();
	    //判断今日是否已经对账
	    int recon = reconciliationStatisticsDao.getReconciliationCount(startDate, channel);
	    if(recon>0){
	    	resultMap.put("responseCode", CheckConstant.FAIL);
			resultMap.put("responseMsg", "当日已对账,无需再次对账");
			return resultMap;
	    }
		//查询结算系统当日未对账订单
		log.info("查询订单List Start**Parameter:startDate="+startDate+"endDate="+endDate+",channel="+channel);
		ReconciliationStatisticsVo reconciliationStatisticsVo = new ReconciliationStatisticsVo();
		int errorCount = 0;
		BigDecimal errorAmount = BigDecimal.ZERO;
		List<PaymentVo> orderList = paymentDao.findReconciliationByStatusAndTime(startDate, endDate, channel);//查询该渠道下需对帐的订单List
		String systemAmount = paymentDao.findReconciliationAmountByStatusAndTime(startDate, endDate, channel);//查询该渠道下需对帐的订单总金额
		List<PaymentVo> checkOrderList = new ArrayList<PaymentVo>();//修改过状态的订单List
		List<ReconciliationPassVo> checkReconciliationList = new ArrayList<ReconciliationPassVo>();//渠道下修改过状态的订单List
		//判断对账渠道
		if("10".equals(channel) ||"11".equals(channel) || "12".equals(channel) || "14".equals(channel) || "19".equals(channel) 
				|| "20".equals(channel) || "16".equals(channel) || "17".equals(channel) || "18".equals(channel)){//10宝付认证支付 11宝付代付 12宝付代扣14网关161718先锋1920易宝
			//查询对账数据当日未对账订单
			log.info("查询第三方对账List Start**Parameter:startDate="+startDate+"endDate="+endDate+",channel="+channel);
			List<ReconciliationPassVo> reconciliationList = reconciliationPassDao.findReconciliationByChannelAndTime(channel,startDate,endDate);//查询需对账的订单
			String outsideAmount = reconciliationPassDao.findReconciliationAmountByChannelAndTime(channel,startDate,endDate);//查询需对账的订单
			reconciliationStatisticsVo.setOutsideCount(reconciliationList.size());
			reconciliationStatisticsVo.setOutsideAmount(outsideAmount);
			for(ReconciliationPassVo reconciliationVo : reconciliationList){
				if(reconciliationVo.getReconStatus()==0||reconciliationVo.getReconStatus()==2){//未对账或者对账失败
					for(PaymentVo paymentVo : orderList){
						if(reconciliationVo.getOrderId().equals(paymentVo.getPayNo())){//订单号匹配
							//处理订单状态和第三方状态是否匹配
							if(reconciliationVo.getTradStatus().equals(paymentVo.getCommandStatus())){//交易状态匹配
								if(0==reconciliationVo.getTransactionAmount().compareTo(paymentVo.getAmount())){//比较金额
									reconciliationVo.setReconStatus(CheckConstant.CHECK_SUCCESS);
									reconciliationVo.setCheckMark("");//对账成功
									reconciliationVo.setUpdateTime(updateDate);
									paymentVo.setReconStatus(CheckConstant.CHECK_SUCCESS);
									paymentVo.setReconciliationMark("");
									paymentVo.setUpdateTime(updateDate);
									//提现订单状态匹配,并且为失败的订单,自动去解冻冻结户余额
									log.info("订单类型：{}，订单号：{}，对账成功！，订单状态：{}，三方订单状态：{}",paymentVo.getType(),
											paymentVo.getOrderNo(),paymentVo.getCommandStatus(),reconciliationVo.getTradStatus());
									if(TradeTypeEnum.trade_payee.getCode().equals(paymentVo.getType())&&"2".equals(paymentVo.getCommandStatus())){
										log.info("订单号：{}，提现订单状态匹配,并且为失败的订单,自动去解冻冻结户余额。",paymentVo.getOrderNo());
										OrderResponse resp = reconciliationWithdrawalsService.withdrawalsRecon(
												paymentVo.getOrderNo(), reconciliationVo.getTradStatus(), paymentVo.getCommandStatus());
										if(Constant.SUCCESS.equals(resp.getReturnCode())){
											log.info("订单号：{}，失败提现订单对账成功，自动处理，解冻账户余额成功。",paymentVo.getOrderNo());
										}else{
											log.error("订单号：{}，失败提现订单对账成功，自动处理，解冻账户余额失败。",paymentVo.getOrderNo());
										}
									}
								}else{
									reconciliationVo.setReconStatus(CheckConstant.CHECK_FAIL);//金额不匹配
									reconciliationVo.setCheckMark(CheckConstant.AMOUNT_NOT_CONFORM_DESC);
									reconciliationVo.setRepairStatus("N");
									reconciliationVo.setUpdateTime(updateDate);
									paymentVo.setReconStatus(CheckConstant.CHECK_FAIL);
									paymentVo.setReconciliationMark(CheckConstant.AMOUNT_NOT_CONFORM_DESC);
									paymentVo.setUpdateTime(updateDate);
									errorCount++;
									errorAmount = errorAmount.add(reconciliationVo.getTransactionAmount());
									//保存金额不匹配订单
									saveAmountNotConformErrorOrder(paymentVo, reconciliationVo);
								}
							}else {
								reconciliationVo.setReconStatus(CheckConstant.CHECK_FAIL);//第三方状态不匹配与订单交易状态
								reconciliationVo.setCheckMark(CheckConstant.STATUS_NOT_CONFORM_DESC);
								reconciliationVo.setUpdateTime(updateDate);
								paymentVo.setReconStatus(CheckConstant.CHECK_FAIL);
								paymentVo.setReconciliationMark(CheckConstant.STATUS_NOT_CONFORM_DESC);
								paymentVo.setUpdateTime(updateDate);
								//处理状态不匹配订单
								automateErrorOrder(paymentVo,reconciliationVo);
								errorCount++;
								errorAmount = errorAmount.add(reconciliationVo.getTransactionAmount());
							}
							checkOrderList.add(paymentVo);
						}else if("短信验证码输入错误，请重新输入".equals(paymentVo.getFailDetail())){//订单失败的原因是验证码的,为对账成功
							paymentVo.setReconStatus(CheckConstant.CHECK_SUCCESS);
							paymentVo.setReconciliationMark("");
							paymentVo.setUpdateTime(updateDate);
							checkOrderList.add(paymentVo);
						}
					}
					if(reconciliationVo.getReconStatus()==0){
						reconciliationVo.setReconStatus(CheckConstant.CHECK_FAIL);//处理第三方多单情况
						reconciliationVo.setCheckMark(CheckConstant.BANK_MORETHAN_ORDER_DESC);
						reconciliationVo.setRepairStatus("N");
						reconciliationVo.setUpdateTime(updateDate);
						//保存三方多单
						saveBankMoreErrorOrder(reconciliationVo);
						errorCount++;
						errorAmount = errorAmount.add(reconciliationVo.getTransactionAmount());
					}
					checkReconciliationList.add(reconciliationVo);
				}
			}
		}else{
			resultMap.put("responseCode", CheckConstant.FAIL);
			resultMap.put("responseMsg", "未知渠道");
			return resultMap;
		}
		//处理orderList
		Map<String,Object> map = checkOrder(orderList,checkOrderList,updateDate,errorCount,errorAmount);
		errorCount = (int) map.get("errorCount");
		errorAmount = (BigDecimal) map.get("errorAmount");
		reconciliationStatisticsVo.setErrorCount(errorCount);
		reconciliationStatisticsVo.setErrorAmount(errorAmount.toString());
		reconciliationStatisticsVo.setReconciliationDate(updateDate);
		reconciliationStatisticsVo.setOutsideDate(startDate);
		reconciliationStatisticsVo.setSystemCount(orderList.size());
		reconciliationStatisticsVo.setSystemAmount(systemAmount);
		reconciliationStatisticsVo.setConfirmDate(startDate);
		reconciliationStatisticsVo.setChannelNo(channel);
		reconciliationStatisticsVo.setReconciliationStatus("0");
		String channelName = "";
		if(TradeChannel.baofoopay.getValue().equals(channel)){
			channelName="宝付（认证支付）";
		}else if(TradeChannel.baofoopayee.getValue().equals(channel)){
			channelName="宝付（代付）";
		}else if(TradeChannel.baofooDkWithoiding.getValue().equals(channel)){
			channelName="宝付（代扣）";
		}else if(TradeChannel.baofooGateway.getValue().equals(channel)){
			channelName="宝付（网关支付）";
		}else if("19".equals(channel)){
			channelName="易宝（绑卡支付）";
		}else if("20".equals(channel)){
			channelName="易宝（代付）";
		}else if("16".equals(channel)){
			channelName="先锋支付（认证支付）";
		}else if("17".equals(channel)){
			channelName="先锋支付（代扣）";
		}else if("18".equals(channel)){
			channelName="易先锋支付（代付）";
		}else{
			channelName="未知";
		}
		reconciliationStatisticsVo.setChannelName(channelName);
		reconciliationStatisticsDao.save(reconciliationStatisticsVo);
		if(checkReconciliationList.size()>0){
			//修改对账表状态
			reconciliationPassDao.save(checkReconciliationList);
		}
		resultMap.put("responseCode", CheckConstant.SUCCESS);
		return resultMap;
	}
	
	/**
	 * 自动处理订单状态不匹配异常订单
	 * @param paymentVo
	 * @param reconciliationVo
	 * @return
	 */
	private void automateErrorOrder(
			PaymentVo paymentVo, ReconciliationPassVo reconciliationVo){
		ReconciliationErrorRecordsVo reconciliationErrorRecordsVo = new ReconciliationErrorRecordsVo();
		//自动处理
		OrderResponse resp = null;
		String errorResult = null;
		String errorStatus = null;
		String channelName = null;
		String channelNo = null;
		if(TradeTypeEnum.trade_pay.getCode().equals(paymentVo.getType())){
			resp = reconciliationRechargeService.rechargeRecon(
					paymentVo.getOrderNo(), reconciliationVo.getTradStatus(), paymentVo.getCommandStatus());
			if("1".equals(paymentVo.getCommandStatus())&&"2".equals(reconciliationVo.getTradStatus())){
				if(Constant.SUCCESS.equals(resp.getReturnCode())){
					errorStatus = CheckConstant.DEAL_BY_SYSTEM;//系统自动处理成功
					errorResult = CheckConstant.STANDARD_RESULT_1001;//系统自动扣减用户账户余额成功并变更订单状态为“交易失败”
					reconciliationErrorRecordsVo.setApplyRecord("Y");
					reconciliationVo.setRepairStatus("Y");
				}else if(Constant.BALANCELESS.equals(resp.getReturnCode())){
					errorStatus = CheckConstant.WAIT_FOR_DEAL;//等待人工处理
					errorResult = CheckConstant.STANDARD_RESULT_1002;//系统自动扣减用户账户余额失败，需人工确认
					reconciliationErrorRecordsVo.setApplyRecord("Y");
					reconciliationVo.setRepairStatus("N");
				}else{
					errorStatus = CheckConstant.DEAL_FAIL;//系统处理失败
					errorResult = "系统自动处理失败,请联系运维人员";
					reconciliationErrorRecordsVo.setApplyRecord("N");
					reconciliationVo.setRepairStatus("N");
				}
			}else if("1".equals(reconciliationVo.getTradStatus())){
				if(Constant.SUCCESS.equals(resp.getReturnCode())){
					errorStatus = CheckConstant.DEAL_BY_SYSTEM;//系统自动处理成功
					errorResult = CheckConstant.STANDARD_RESULT_1005;//系统变更订单状态为“交易成功”，并增加账户余额成功
					reconciliationErrorRecordsVo.setApplyRecord("Y");
					reconciliationVo.setRepairStatus("Y");
				}else{
					errorStatus = CheckConstant.DEAL_FAIL;//系统处理失败
					errorResult = "系统自动处理失败,请联系运维人员";
					reconciliationErrorRecordsVo.setApplyRecord("N");
					reconciliationVo.setRepairStatus("N");
				}
			}else if("2".equals(reconciliationVo.getTradStatus())){
				if(Constant.SUCCESS.equals(resp.getReturnCode())){
					errorStatus = CheckConstant.DEAL_BY_SYSTEM;//系统自动处理成功
					errorResult = CheckConstant.STANDARD_RESULT_1006;//系统变更订单状态为“交易失败”
					reconciliationErrorRecordsVo.setApplyRecord("Y");
					reconciliationVo.setRepairStatus("Y");
				}else{
					errorStatus = CheckConstant.DEAL_FAIL;//系统处理失败
					errorResult = "系统自动处理失败,请联系运维人员";
					reconciliationErrorRecordsVo.setApplyRecord("N");
					reconciliationVo.setRepairStatus("N");
				}
			}
		}else{
			resp = reconciliationWithdrawalsService.withdrawalsRecon(
					paymentVo.getOrderNo(), reconciliationVo.getTradStatus(), paymentVo.getCommandStatus());
			if("2".equals(reconciliationVo.getTradStatus())){
				if(Constant.SUCCESS.equals(resp.getReturnCode())){
					errorStatus = CheckConstant.DEAL_BY_SYSTEM;//系统自动处理成功
					errorResult = CheckConstant.STANDARD_RESULT_1007;//系统变更订单状态为“交易失败”，并增加账户余额成功
					reconciliationErrorRecordsVo.setApplyRecord("N");
					reconciliationVo.setRepairStatus("Y");
				}else if(Constant.BALANCELESS.equals(resp.getReturnCode())){
					errorStatus = CheckConstant.DEAL_FAIL;//系统处理失败
					errorResult = "系统自动处理失败,请联系运维人员";
					reconciliationErrorRecordsVo.setApplyRecord("N");
					reconciliationVo.setRepairStatus("N");
				}
			}else {
				if(Constant.SUCCESS.equals(resp.getReturnCode())){
					errorStatus = CheckConstant.DEAL_BY_SYSTEM;//系统自动处理成功
					errorResult = CheckConstant.STANDARD_RESULT_1008;//系统变更订单状态为“交易成功”，并扣减账户余额成功
					reconciliationErrorRecordsVo.setApplyRecord("N");
					reconciliationVo.setRepairStatus("Y");
				}else{
					errorStatus = CheckConstant.DEAL_FAIL;//系统处理失败
					errorResult = "系统自动处理失败,请联系运维人员";
					reconciliationErrorRecordsVo.setApplyRecord("N");
					reconciliationVo.setRepairStatus("N");
				}
			}
		}
		
		channelNo = reconciliationVo.getChannelId();
		if(channelNo != null&&TradeChannel.baofoopay.getValue().equals(channelNo)){
			channelName="宝付（认证支付）";
		}else if(channelNo != null&&TradeChannel.baofoopayee.getValue().equals(channelNo)){
			channelName="宝付（代付）";
		}else if(channelNo != null&&TradeChannel.baofooDkWithoiding.getValue().equals(channelNo)){
			channelName="宝付（代扣）";
		}else if(channelNo != null&&TradeChannel.baofooGateway.getValue().equals(channelNo)){
			channelName="宝付（网关支付）";
		}else if(channelNo != null&&"19".equals(channelNo)){
			channelName = "易宝（绑卡支付）";
		}else if(channelNo != null&&"20".equals(channelNo)){
			channelName = "易宝（代付）";
		}else if(channelNo != null&&"16".equals(channelNo)){
			channelName="先锋支付（认证支付）";
		}else if(channelNo != null&&"17".equals(channelNo)){
			channelName="先锋支付（代扣）";
		}else if(channelNo != null&&"18".equals(channelNo)){
			channelName="易先锋支付（代付）";
		}else{
			channelName="未知";
		}
		Timestamp nowTime = new Timestamp(System.currentTimeMillis());
		reconciliationErrorRecordsVo.setChannelName(channelName);
		reconciliationErrorRecordsVo.setChannelNo(paymentVo.getChannelNo());
		reconciliationErrorRecordsVo.setCreateTime(nowTime);
		reconciliationErrorRecordsVo.setErrorResult(errorResult);
		reconciliationErrorRecordsVo.setErrorStatus(errorStatus);
		reconciliationErrorRecordsVo.setErrorType(CheckConstant.STATUS_NOT_CONFORM_DESC);
		reconciliationErrorRecordsVo.setErrorSort(CheckConstant.WAIT_FOR_DEAL.equals(errorStatus)?CheckConstant.ERRORSORT_WAIT_FOR_DEAL:CheckConstant.ERRORSORT_FINISH);
		reconciliationErrorRecordsVo.setMemberId(reconciliationVo.getMemberId());
		reconciliationErrorRecordsVo.setOrderNo(paymentVo.getOrderNo());
		reconciliationErrorRecordsVo.setOrderStatus(paymentVo.getCommandStatus());
		reconciliationErrorRecordsVo.setOrderTime(paymentVo.getUpTime());
		reconciliationErrorRecordsVo.setOrderType(paymentVo.getType());
		reconciliationErrorRecordsVo.setAmount(paymentVo.getAmount());
		reconciliationErrorRecordsVo.setOutsideAmount(reconciliationVo.getTransactionAmount());
		reconciliationErrorRecordsVo.setOutsideOrderNo(reconciliationVo.getOutsideOrderNo());
		reconciliationErrorRecordsVo.setOutsideOrderStatus(reconciliationVo.getTradStatus());
		reconciliationErrorRecordsVo.setPayNo(reconciliationVo.getOrderId());
//		reconciliationErrorRecordsVo.setRemark(remark);
		reconciliationErrorRecordsVo.setUpdateTime(nowTime);
		reconciliationErrorRecordsVo.setUserName(paymentVo.getRealName());
		reconciliationErrorRecordsVo.setUserOid(paymentVo.getUserOid());
		reconciliationErrorRecordsVo.setUserPhone(paymentVo.getPhone());
		reconciliationErrorRecordsDao.saveAndFlush(reconciliationErrorRecordsVo);
	}
	
	/**
	 * 保存三方多单订单
	 * @param reconciliationVo
	 */
	private void saveBankMoreErrorOrder(ReconciliationPassVo reconciliationVo){
		ReconciliationErrorRecordsVo reconciliationErrorRecordsVo = new ReconciliationErrorRecordsVo();
		String errorResult = CheckConstant.STANDARD_RESULT_1009;
		String errorStatus = CheckConstant.WAIT_FOR_DEAL;
		String errorType = CheckConstant.BANK_MORETHAN_ORDER_DESC;
		String channelName = null;
		String channelNo = null;
		String tradeType = null;
		channelNo = reconciliationVo.getChannelId();
		if(channelNo != null&&TradeChannel.baofoopay.getValue().equals(channelNo)){
			channelName = "宝付（认证支付）";
			tradeType = TradeTypeEnum.trade_pay.getCode();
		}else if(channelNo != null&&TradeChannel.baofoopayee.getValue().equals(channelNo)){
			channelName = "宝付（代付）";
			tradeType = TradeTypeEnum.trade_payee.getCode();
		}else if(channelNo != null&&TradeChannel.baofooDkWithoiding.getValue().equals(channelNo)){
			channelName = "宝付（代扣）";
			tradeType = TradeTypeEnum.trade_pay.getCode();
		}else if(channelNo != null&&TradeChannel.baofooGateway.getValue().equals(channelNo)){
			channelName = "宝付（网关支付）";
			tradeType = TradeTypeEnum.trade_pay.getCode();
		}else if(channelNo != null&&"19".equals(channelNo)){
			channelName = "易宝（绑卡支付）";
			tradeType = TradeTypeEnum.trade_pay.getCode();
		}else if(channelNo != null&&"20".equals(channelNo)){
			channelName = "易宝（代付）";
			tradeType = TradeTypeEnum.trade_payee.getCode();
		}else if(channelNo != null&&"16".equals(channelNo)){
			channelName="先锋支付（认证支付）";
			tradeType = TradeTypeEnum.trade_pay.getCode();
		}else if(channelNo != null&&"17".equals(channelNo)){
			channelName="先锋支付（代扣）";
			tradeType = TradeTypeEnum.trade_pay.getCode();
		}else if(channelNo != null&&"18".equals(channelNo)){
			channelName="易先锋支付（代付）";
			tradeType = TradeTypeEnum.trade_payee.getCode();
		}
		Timestamp nowTime = new Timestamp(System.currentTimeMillis());
		reconciliationErrorRecordsVo.setChannelName(channelName);
		reconciliationErrorRecordsVo.setChannelNo(channelNo);
		reconciliationErrorRecordsVo.setCreateTime(nowTime);
		reconciliationErrorRecordsVo.setErrorResult(errorResult);
		reconciliationErrorRecordsVo.setErrorStatus(errorStatus);
		reconciliationErrorRecordsVo.setErrorType(errorType);
		reconciliationErrorRecordsVo.setErrorSort(CheckConstant.WAIT_FOR_DEAL.equals(errorStatus)?CheckConstant.ERRORSORT_WAIT_FOR_DEAL:CheckConstant.ERRORSORT_FINISH);
		reconciliationErrorRecordsVo.setMemberId(reconciliationVo.getMemberId());
//		reconciliationErrorRecordsVo.setOrderNo();
//		reconciliationErrorRecordsVo.setOrderStatus(paymentVo.getCommandStatus());
		reconciliationErrorRecordsVo.setOrderTime(reconciliationVo.getTransactionTime());
		reconciliationErrorRecordsVo.setOrderType(tradeType);
		reconciliationErrorRecordsVo.setOutsideAmount(reconciliationVo.getTransactionAmount());
		reconciliationErrorRecordsVo.setOutsideOrderNo(reconciliationVo.getOutsideOrderNo());
		reconciliationErrorRecordsVo.setOutsideOrderStatus(reconciliationVo.getTradStatus());
		reconciliationErrorRecordsVo.setPayNo(reconciliationVo.getOrderId());
//		reconciliationErrorRecordsVo.setRemark(remark);
		reconciliationErrorRecordsVo.setUpdateTime(nowTime);
//		reconciliationErrorRecordsVo.setUserName(paymentVo.getRealName());
//		reconciliationErrorRecordsVo.setUserOid(paymentVo.getUserOid());
//		reconciliationErrorRecordsVo.setUserPhone(paymentVo.getPhone());
		reconciliationErrorRecordsVo.setApplyRecord("N");
		reconciliationErrorRecordsDao.saveAndFlush(reconciliationErrorRecordsVo);
	}
	
	/**
	 * 保存订单多单订单
	 * @param paymentVo
	 */
	private void saveOrderMoreErrorOrder(PaymentVo paymentVo, ReconciliationPassVo reconciliationVo) {
		ReconciliationErrorRecordsVo reconciliationErrorRecordsVo = new ReconciliationErrorRecordsVo();
		String errorResult = CheckConstant.STANDARD_RESULT_1011;
		String errorStatus = CheckConstant.WAIT_FOR_DEAL;
		String errorType = CheckConstant.ORDER_MORETHAN_BANK_DESC;
		//自动处理平台提现失败的订单
		OrderResponse resp = null;
		if(TradeTypeEnum.trade_payee.getCode().equals(paymentVo.getType())&&"2".equals(paymentVo.getCommandStatus())){
			resp = reconciliationWithdrawalsService.withdrawalsRecon(
					paymentVo.getOrderNo(), paymentVo.getCommandStatus(), paymentVo.getCommandStatus());
			if(Constant.SUCCESS.equals(resp.getReturnCode())){
				errorStatus = CheckConstant.DEAL_BY_SYSTEM;//系统自动处理成功
				errorResult = CheckConstant.STANDARD_RESULT_1007;//系统变更订单状态为“交易失败”，并增加账户余额成功
				reconciliationVo.setRepairStatus("Y");
			}else if(Constant.BALANCELESS.equals(resp.getReturnCode())){
				errorStatus = CheckConstant.DEAL_FAIL;//系统处理失败
				errorResult = "系统自动处理失败,请联系运维人员";
				reconciliationVo.setRepairStatus("N");
			}
		}
		String channelNo = paymentVo.getChannelNo();
		String channelName =null;
		if(channelNo != null&&TradeChannel.baofoopay.getValue().equals(channelNo)){
			channelName="宝付（认证支付）";
		}else if(channelNo != null&&TradeChannel.baofoopayee.getValue().equals(channelNo)){
			channelName="宝付（代付）";
		}else if(channelNo != null&&TradeChannel.baofooDkWithoiding.getValue().equals(channelNo)){
			channelName="宝付（代扣）";
		}else if(channelNo != null&&TradeChannel.baofooGateway.getValue().equals(channelNo)){
			channelName="宝付（网关支付）";
		}else if(channelNo != null&&"19".equals(channelNo)){
			channelName = "易宝（绑卡支付）";
		}else if(channelNo != null&&"20".equals(channelNo)){
			channelName = "易宝（代付）";
		}else if(channelNo != null&&"16".equals(channelNo)){
			channelName="先锋支付（认证支付）";
		}else if(channelNo != null&&"17".equals(channelNo)){
			channelName="先锋支付（代扣）";
		}else if(channelNo != null&&"18".equals(channelNo)){
			channelName="易先锋支付（代付）";
		}else{
			channelName="未知";
		}
		Timestamp nowTime = new Timestamp(System.currentTimeMillis());
		reconciliationErrorRecordsVo.setChannelName(channelName);
		reconciliationErrorRecordsVo.setChannelNo(channelNo);
		reconciliationErrorRecordsVo.setCreateTime(nowTime);
		reconciliationErrorRecordsVo.setErrorResult(errorResult);
		reconciliationErrorRecordsVo.setErrorStatus(errorStatus);
		reconciliationErrorRecordsVo.setErrorSort(CheckConstant.WAIT_FOR_DEAL.equals(errorStatus)?CheckConstant.ERRORSORT_WAIT_FOR_DEAL:CheckConstant.ERRORSORT_FINISH);
		reconciliationErrorRecordsVo.setErrorType(errorType);
		reconciliationErrorRecordsVo.setMemberId(memberId);
		reconciliationErrorRecordsVo.setOrderNo(paymentVo.getOrderNo());
		reconciliationErrorRecordsVo.setOrderStatus(paymentVo.getCommandStatus());
		reconciliationErrorRecordsVo.setOrderTime(paymentVo.getUpTime());
		reconciliationErrorRecordsVo.setOrderType(paymentVo.getType());
		reconciliationErrorRecordsVo.setAmount(paymentVo.getAmount());
//		reconciliationErrorRecordsVo.setOutsideAmount(reconciliationVo.getTransactionAmount());
//		reconciliationErrorRecordsVo.setOutsideOrderNo(reconciliationVo.getBeneficiaryBankNo());
//		reconciliationErrorRecordsVo.setOutsideOrderStatus(reconciliationVo.getTradStatus());
		
		reconciliationErrorRecordsVo.setPayNo(paymentVo.getPayNo());
//		reconciliationErrorRecordsVo.setRemark(remark);
		reconciliationErrorRecordsVo.setUpdateTime(nowTime);
		reconciliationErrorRecordsVo.setUserName(paymentVo.getRealName());
		reconciliationErrorRecordsVo.setUserOid(paymentVo.getUserOid());
		reconciliationErrorRecordsVo.setUserPhone(paymentVo.getPhone());
		if("1".equals(paymentVo.getCommandStatus())&&TradeTypeEnum.trade_pay.getCode().equals(paymentVo.getType())){
			reconciliationErrorRecordsVo.setApplyRecord("Y");
		}else{
			reconciliationErrorRecordsVo.setApplyRecord("N");
		}
		reconciliationErrorRecordsDao.saveAndFlush(reconciliationErrorRecordsVo);
		reconciliationPassDao.save(reconciliationVo);
	}
	
	/**
	 * 保存金额不匹配异常订单
	 * @param paymentVo
	 * @param reconciliationVo
	 */
	private void saveAmountNotConformErrorOrder(
			PaymentVo paymentVo, ReconciliationPassVo reconciliationVo){
		ReconciliationErrorRecordsVo reconciliationErrorRecordsVo = new ReconciliationErrorRecordsVo();
		String errorResult = CheckConstant.STANDARD_RESULT_1014;
		String errorStatus = CheckConstant.WAIT_FOR_DEAL;
		String errorType = CheckConstant.AMOUNT_NOT_CONFORM_DESC;
		String channelName = null;
		String channelNo = null;
		
		channelNo = reconciliationVo.getChannelId();
		if(channelNo != null&&TradeChannel.baofoopay.getValue().equals(channelNo)){
			channelName="宝付（认证支付）";
		}else if(channelNo != null&&TradeChannel.baofoopayee.getValue().equals(channelNo)){
			channelName="宝付（代付）";
		}else if(channelNo != null&&TradeChannel.baofooDkWithoiding.getValue().equals(channelNo)){
			channelName="宝付（代扣）";
		}else if(channelNo != null&&TradeChannel.baofooGateway.getValue().equals(channelNo)){
			channelName="宝付（网关支付）";
		}else if(channelNo != null&&"19".equals(channelNo)){
			channelName = "易宝（绑卡支付）";
		}else if(channelNo != null&&"20".equals(channelNo)){
			channelName = "易宝（代付）";
		}else if(channelNo != null&&"16".equals(channelNo)){
			channelName="先锋支付（认证支付）";
		}else if(channelNo != null&&"17".equals(channelNo)){
			channelName="先锋支付（代扣）";
		}else if(channelNo != null&&"18".equals(channelNo)){
			channelName="易先锋支付（代付）";
		}else{
			channelName="未知";
		}
		Timestamp nowTime = new Timestamp(System.currentTimeMillis());
		reconciliationErrorRecordsVo.setChannelName(channelName);
		reconciliationErrorRecordsVo.setChannelNo(paymentVo.getChannelNo());
		reconciliationErrorRecordsVo.setCreateTime(nowTime);
		reconciliationErrorRecordsVo.setErrorResult(errorResult);
		reconciliationErrorRecordsVo.setErrorStatus(errorStatus);
		reconciliationErrorRecordsVo.setErrorType(errorType);
		reconciliationErrorRecordsVo.setErrorSort(CheckConstant.WAIT_FOR_DEAL.equals(errorStatus)?CheckConstant.ERRORSORT_WAIT_FOR_DEAL:CheckConstant.ERRORSORT_FINISH);
		reconciliationErrorRecordsVo.setMemberId(reconciliationVo.getMemberId());
		reconciliationErrorRecordsVo.setOrderNo(paymentVo.getOrderNo());
		reconciliationErrorRecordsVo.setOrderStatus(paymentVo.getCommandStatus());
		reconciliationErrorRecordsVo.setOrderTime(paymentVo.getUpTime());
		reconciliationErrorRecordsVo.setOrderType(paymentVo.getType());
		reconciliationErrorRecordsVo.setAmount(paymentVo.getAmount());
		reconciliationErrorRecordsVo.setOutsideAmount(reconciliationVo.getTransactionAmount());
		reconciliationErrorRecordsVo.setOutsideOrderNo(reconciliationVo.getOutsideOrderNo());
		reconciliationErrorRecordsVo.setOutsideOrderStatus(reconciliationVo.getTradStatus());
		reconciliationErrorRecordsVo.setPayNo(reconciliationVo.getOrderId());
//		reconciliationErrorRecordsVo.setRemark(remark);
		reconciliationErrorRecordsVo.setUpdateTime(nowTime);
		reconciliationErrorRecordsVo.setUserName(paymentVo.getRealName());
		reconciliationErrorRecordsVo.setUserOid(paymentVo.getUserOid());
		reconciliationErrorRecordsVo.setUserPhone(paymentVo.getPhone());
		reconciliationErrorRecordsVo.setApplyRecord("N");
		reconciliationErrorRecordsDao.saveAndFlush(reconciliationErrorRecordsVo);
	}

	/**
	 *  订单存在，对账不存在该订单，对账状态置为对账失败，原因订单多单
	 * @param orderList
	 * @param checkOrderList
	 * @param updateDate
	 * @param errorCount
	 * @param errorAmount
	 * @return
	 */
	public Map<String,Object> checkOrder(List<PaymentVo> orderList, List<PaymentVo> checkOrderList,
			Timestamp updateDate, int errorCount, BigDecimal errorAmount){
		Map<String,Object> map = new HashMap<String,Object>();
		List<PaymentVo> updateOrderList = new ArrayList<PaymentVo>();
		if(checkOrderList.size()==0){
			log.info("对账文件无数据");
		}
		if(orderList.size()>0){
			for(PaymentVo paymentVo : orderList){
				//未对帐订单置为订单多单
				if(paymentVo.getReconStatus()==0){
					paymentVo.setReconStatus(CheckConstant.CHECK_FAIL);//平台订单多单
					paymentVo.setReconciliationMark(CheckConstant.ORDER_MORETHAN_BANK_DESC);
					paymentVo.setUpdateTime(updateDate);
					//将平台多单数据插入到对账文件数据中
					ReconciliationPassVo reconciliationVo = saveOrderMoreToReconciliationPass(paymentVo);
					//保存平台多单
					saveOrderMoreErrorOrder(paymentVo, reconciliationVo);
					errorCount++;
					errorAmount = errorAmount.add(paymentVo.getAmount());
					for(PaymentVo checkPaymentVo : checkOrderList){
						if(paymentVo.getOrderNo().equals(checkPaymentVo.getOrderNo())){
							paymentVo.setReconStatus(checkPaymentVo.getReconStatus());
							paymentVo.setReconciliationMark(checkPaymentVo.getReconciliationMark());
						}
					}
				}
				updateOrderList.add(paymentVo);
			}
			paymentDao.save(updateOrderList);
		}
		map.put("errorCount", errorCount);
		map.put("errorAmount", errorAmount);
		return map;
	}

	/**
	 * 将平台多单数据保存到三方对账文件表中
	 * @param paymentVo
	 */
	private ReconciliationPassVo saveOrderMoreToReconciliationPass(PaymentVo paymentVo) {
		ReconciliationPassVo oldReconciliationVo = reconciliationPassDao.findByOrderId(paymentVo.getPayNo());
		if(oldReconciliationVo == null){
			ReconciliationPassVo reconciliationVo = new ReconciliationPassVo();
			reconciliationVo.setFee(BigDecimal.ZERO);
			reconciliationVo.setChannelId(paymentVo.getChannelNo());
			reconciliationVo.setCheckMark(paymentVo.getReconciliationMark());
			reconciliationVo.setCreateTime(paymentVo.getUpdateTime());
			reconciliationVo.setFailDetail(paymentVo.getFailDetail());
			reconciliationVo.setMemberId(memberId);
			reconciliationVo.setOrderId(paymentVo.getPayNo());
			reconciliationVo.setReconStatus(paymentVo.getReconStatus());
			reconciliationVo.setRepairStatus("N");
			reconciliationVo.setTradStatus(paymentVo.getCommandStatus());
			reconciliationVo.setTransactionAmount(paymentVo.getAmount());
			reconciliationVo.setTransactionTime(paymentVo.getCreateTime());
			reconciliationVo.setUpdateTime(paymentVo.getUpdateTime());
			reconciliationVo.setUserOid(paymentVo.getUserOid());
			
			reconciliationPassDao.save(reconciliationVo);
			return reconciliationVo;
		}
		return oldReconciliationVo;
	}

}