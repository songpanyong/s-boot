package com.guohuai.boot.pay.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.guohuai.boot.pay.dao.BankHistoryDao;
import com.guohuai.boot.pay.dao.PaymentDao;
import com.guohuai.boot.pay.dao.ReconciliationPassDao;
import com.guohuai.boot.pay.vo.BankHistoryVo;
import com.guohuai.boot.pay.vo.PaymentVo;
import com.guohuai.boot.pay.vo.ReconciliationPassVo;
import com.guohuai.component.util.CheckConstant;
import com.guohuai.component.util.DateUtil;
/**
 * 对账（第三方和银行）
 * @author chendonghui
 *
 */
@Service
public class OrderReconciliationService {
	private final static Logger log = LoggerFactory.getLogger(OrderReconciliationService.class);
	
	@Autowired
	private PaymentDao paymentDao;
	
	@Autowired
	private ReconciliationPassDao reconciliationPassDao;
	
	@Autowired
	private BankHistoryDao bankHistoryDao;
	
	@Autowired
	private BankCallbackService bankCallbackService;
	
	public Map<String,Object> orderReconciliation(String checkTime, String channel){
		log.info("{},对账,{},","receiveTime="+checkTime);
		//根据日期获取时间区间
		Date date = DateUtil.stringToDate(checkTime);
//	    Timestamp startDate = new Timestamp(date.getTime());//保留，启用日期区间
	    Timestamp endDate = new Timestamp((DateUtil.getNextDay(date)).getTime());
	    Date now = new Date();
	    Timestamp updateDate = new Timestamp(now.getTime());//当前时间
	    
		Map<String,Object> resultMap = new HashMap<String,Object>();
		//查询订单List(默认查非失败状态订单和未对帐的订单)
		//2017.01.04修改查询当日之前订单状态的所有状态订单并且对账状态为未对帐和对账失败订单
		//2017.01.06修改查询新增条件:不查询未发送订单
		log.info("查询订单List Start**Parameter:endDate="+endDate+",channel="+channel);
		List<PaymentVo> orderList = paymentDao.findByStatusAndTime(endDate, channel);//查询该渠道下需对帐的订单List
		List<PaymentVo> checkOrderList = new ArrayList<PaymentVo>();//修改过状态的订单List
		List<ReconciliationPassVo> checkReconciliationList = new ArrayList<ReconciliationPassVo>();//渠道下修改过状态的订单List
		List<BankHistoryVo> checkBankHistoryList = new ArrayList<BankHistoryVo>();//银行修改过状态的订单List
		List<PaymentVo> needCallBackList = new ArrayList<PaymentVo>();//网关支付需要回调的订单
		//判断对账渠道
		if("7".equals(channel)||"8".equals(channel) ||"10".equals(channel) ||"11".equals(channel) || "12".equals(channel)){//7金运通代付8金运通代扣 10宝付认证支付 11宝付代付 12宝付代扣
			//查询快付通或先锋支付对账List(默认查询成功的订单)
			//2017.01.04修改查询当日之前所有订单并且对账状态为未对帐和对账失败订单
			log.info("查询第三方对账List Start**Parameter:endDate="+endDate+",channel="+channel);
			List<ReconciliationPassVo> reconciliationList = reconciliationPassDao.findByChannelAndTime(channel,endDate);//查询需对账的订单
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
								}else{
									reconciliationVo.setReconStatus(CheckConstant.CHECK_FAIL);//金额不匹配
									reconciliationVo.setCheckMark(CheckConstant.AMOUNT_NOT_CONFORM_DESC);
									reconciliationVo.setUpdateTime(updateDate);
									paymentVo.setReconStatus(CheckConstant.CHECK_FAIL);
									paymentVo.setReconciliationMark(CheckConstant.AMOUNT_NOT_CONFORM_DESC);
									paymentVo.setUpdateTime(updateDate);
								}
							}else {
								reconciliationVo.setReconStatus(CheckConstant.CHECK_FAIL);//第三方状态不匹配与订单交易状态
								reconciliationVo.setCheckMark(CheckConstant.STATUS_NOT_CONFORM_DESC);
								reconciliationVo.setUpdateTime(updateDate);
								paymentVo.setReconStatus(CheckConstant.CHECK_FAIL);
								paymentVo.setReconciliationMark(CheckConstant.STATUS_NOT_CONFORM_DESC);
								paymentVo.setUpdateTime(updateDate);
							}
							checkOrderList.add(paymentVo);
						}else if("短信验证码输入错误，请重新输入".equals(paymentVo.getFailDetail())){//20170310订单失败的原因是验证码的,为对账成功
							paymentVo.setReconStatus(CheckConstant.CHECK_SUCCESS);
							paymentVo.setReconciliationMark("");
							paymentVo.setUpdateTime(updateDate);
							checkOrderList.add(paymentVo);
						}
					}
					if(reconciliationVo.getReconStatus()==0){
						reconciliationVo.setReconStatus(CheckConstant.CHECK_FAIL);//处理第三方多单情况
						reconciliationVo.setCheckMark(CheckConstant.BANK_MORETHAN_ORDER_DESC);
						reconciliationVo.setUpdateTime(updateDate);
					}
					checkReconciliationList.add(reconciliationVo);
				}
			}
		}else if("9".equals(channel)){//9金运通网关
			log.info("查询金运通网关对账List Start**Parameter:endDate="+endDate+"channel="+channel);
			List<ReconciliationPassVo> reconciliationList = reconciliationPassDao.findByChannelAndTime(channel,endDate);//查询需对账的订单
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
								}else{
									reconciliationVo.setReconStatus(CheckConstant.CHECK_FAIL);//金额不匹配
									reconciliationVo.setCheckMark(CheckConstant.AMOUNT_NOT_CONFORM_DESC);
									reconciliationVo.setUpdateTime(updateDate);
									paymentVo.setReconStatus(CheckConstant.CHECK_FAIL);
									paymentVo.setReconciliationMark(CheckConstant.AMOUNT_NOT_CONFORM_DESC);
									paymentVo.setUpdateTime(updateDate);
								}
							}else {
								reconciliationVo.setReconStatus(CheckConstant.CHECK_FAIL);//第三方状态不匹配与订单交易状态
								reconciliationVo.setCheckMark(CheckConstant.STATUS_NOT_CONFORM_DESC);
								reconciliationVo.setUpdateTime(updateDate);
								paymentVo.setReconStatus(CheckConstant.CHECK_FAIL);
								paymentVo.setReconciliationMark(CheckConstant.STATUS_NOT_CONFORM_DESC);
								paymentVo.setUpdateTime(updateDate);
							}
							checkOrderList.add(paymentVo);
						}else if("短信验证码输入错误，请重新输入".equals(paymentVo.getFailDetail())){//20170310订单失败的原因是验证码的,为对账成功
							paymentVo.setReconStatus(CheckConstant.CHECK_SUCCESS);
							paymentVo.setReconciliationMark("");
							paymentVo.setUpdateTime(updateDate);
							checkOrderList.add(paymentVo);
						}
					}
					if(reconciliationVo.getReconStatus()==0&&"2".equals(reconciliationVo.getTradStatus())){//交易状态为失败的并且对账状态为未对帐的
						//看payment表是否存在该订单
						PaymentVo needCallBackVo = paymentDao.findByPayNo(reconciliationVo.getOrderId());
						if(null != needCallBackVo){
							reconciliationVo.setReconStatus(CheckConstant.CHECK_SUCCESS);
							reconciliationVo.setCheckMark("");//对账成功
							reconciliationVo.setUpdateTime(updateDate);
							needCallBackVo.setReconStatus(CheckConstant.CHECK_SUCCESS);
							needCallBackVo.setReconciliationMark("");
							needCallBackVo.setUpdateTime(updateDate);
							//回调List
							needCallBackList.add(needCallBackVo);
						}else{
							reconciliationVo.setReconStatus(CheckConstant.CHECK_FAIL);//处理第三方多单情况
							reconciliationVo.setCheckMark(CheckConstant.BANK_MORETHAN_ORDER_DESC);
						}
					}else if(reconciliationVo.getReconStatus()==0){
						reconciliationVo.setReconStatus(CheckConstant.CHECK_FAIL);//处理第三方多单情况
						reconciliationVo.setCheckMark(CheckConstant.BANK_MORETHAN_ORDER_DESC);
					}
					checkReconciliationList.add(reconciliationVo);
				}
			}
		}else{
			resultMap.put("responseCode", CheckConstant.FAIL);
			resultMap.put("responseMsg", "无渠道信息");
			return resultMap;
		}
		//处理orderList 
		checkOrder(orderList,checkOrderList,updateDate);
		if(checkReconciliationList.size()>0){
			//修改快付通对账表状态
			reconciliationPassDao.save(checkReconciliationList);
			resultMap.put("responseCode", CheckConstant.SUCCESS);
		}else if(checkBankHistoryList.size()>0){
			//修改银行对账表状态
			bankHistoryDao.save(checkBankHistoryList);
			resultMap.put("responseCode", CheckConstant.SUCCESS);
		}else {
			resultMap.put("responseCode", CheckConstant.FAIL);
			resultMap.put("responseMsg", "无对账账单");
		}
		if(null != needCallBackList&&needCallBackList.size()>0){//回调
			paymentDao.save(needCallBackList);//保存对账状态
			bankCallbackService.gatewayCallBackByServerItself(needCallBackList);//回调
		}
		return resultMap;
	}
	
	/**
	 * 订单存在，对账不存在该订单，对账状态置为对账失败，原因订单多单
	 * @param oederList
	 * @param checkOederList
	 */
	public void checkOrder(List<PaymentVo> orderList, List<PaymentVo> checkOrderList, Timestamp updateDate){
		List<PaymentVo> updateOrderList = new ArrayList<PaymentVo>();
		if(checkOrderList.size()>0){
			for(PaymentVo paymentVo : orderList){
				//未对账订单,并且成功的订单
//				if(paymentVo.getReconStatus()==0&&"1".equals(paymentVo.getCommandStatus())){
				//2017.01.05修改为未对帐订单置为订单多单
				if(paymentVo.getReconStatus()==0){
					paymentVo.setReconStatus(CheckConstant.CHECK_FAIL);//订单多单
					paymentVo.setReconciliationMark(CheckConstant.ORDER_MORETHAN_BANK_DESC);
					paymentVo.setUpdateTime(updateDate);
					for(PaymentVo checkPaymentVo : checkOrderList){
						if(paymentVo.getOrderNo().equals(checkPaymentVo.getOrderNo())){
							paymentVo.setReconStatus(checkPaymentVo.getReconStatus());
							paymentVo.setReconciliationMark(checkPaymentVo.getReconciliationMark());
						}
					}
				}
//				else if(paymentVo.getReconStatus()==0){//未对账订单,非成功状态的订单
//					paymentVo.setReconStatus(CheckConstant.CHECK_SUCCESS);//对账成功
//					paymentVo.setUpdateTime(updateDate);
//					for(PaymentVo checkPaymentVo : checkOrderList){
//						if(paymentVo.getOrderNo().equals(checkPaymentVo.getOrderNo())){
//							paymentVo.setReconStatus(checkPaymentVo.getReconStatus());
//						}
//					}
//				}
				updateOrderList.add(paymentVo);
			}
			paymentDao.save(updateOrderList);
		}
	}

}