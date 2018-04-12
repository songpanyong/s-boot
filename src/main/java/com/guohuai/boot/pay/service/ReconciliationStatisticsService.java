package com.guohuai.boot.pay.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.guohuai.basic.common.SeqGenerator;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.dao.TransDao;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.entity.TransEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.boot.pay.dao.ComChannelDao;
import com.guohuai.boot.pay.dao.ReconciliationStatisticsDao;
import com.guohuai.boot.pay.res.ReconciliationStatisticsVoRes;
import com.guohuai.boot.pay.vo.ChannelVo;
import com.guohuai.boot.pay.vo.ReconciliationStatisticsVo;
import com.guohuai.component.util.OrderTypeEnum;
import com.guohuai.payadapter.component.Constant;
import com.guohuai.payadapter.component.TradeChannel;
import com.guohuai.settlement.api.SettlementSdk;
import com.guohuai.settlement.api.request.InteractiveRequest;
import com.guohuai.settlement.api.response.BaseResponse;

@Service
public class ReconciliationStatisticsService {
	private final static Logger log = LoggerFactory.getLogger(ReconciliationStatisticsService.class);
	@Autowired
	private ReconciliationStatisticsDao reconciliationStatisticsDao;
	@Autowired
	private TransDao transDao;
	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private ReconciliationErrorRecordsService reconciliationErrorRecordsService;
	@Autowired
	SettlementSdk settlementSdk;
	@Autowired
	private SeqGenerator seqGenerator;
	@Autowired
	private UserInfoDao userInfoDao;
	@Autowired
	private ComChannelDao comChannelDao;
	
	@Value("${needRechargeFrozenAccount:N}")
	private String needRechargeFrozenAccount;
	
	/**
	 * 查询渠道对账统计信息
	 * @param channleNo
	 * @param outsideDate
	 */
	public ReconciliationStatisticsVoRes findReconciliationStatisticsByChannleAndDate(String channelNo, Timestamp outsideDate){
		log.info("查询对账统计信息,日期：{},渠道：{}", outsideDate, channelNo);
		ReconciliationStatisticsVoRes reconciliationStatisticsVoRes = new ReconciliationStatisticsVoRes();
		ReconciliationStatisticsVo reconciliationStatisticsVo = null;
		reconciliationStatisticsVo = reconciliationStatisticsDao.findByChannelNoAndOutsideDate(channelNo, outsideDate);
		if(reconciliationStatisticsVo != null){
			reconciliationStatisticsVoRes.setErrorMessage("成功");
			reconciliationStatisticsVoRes.setReturnCode("SUCCESS");
			reconciliationStatisticsVoRes.setChannelNo(reconciliationStatisticsVo.getChannelNo());
			reconciliationStatisticsVoRes.setChannelName(reconciliationStatisticsVo.getChannelName());
			reconciliationStatisticsVoRes.setOutsideAmount(reconciliationStatisticsVo.getOutsideAmount());
			reconciliationStatisticsVoRes.setConfirmDate(reconciliationStatisticsVo.getConfirmDate());
			reconciliationStatisticsVoRes.setErrorAmount(reconciliationStatisticsVo.getErrorAmount());
			reconciliationStatisticsVoRes.setErrorCount(reconciliationStatisticsVo.getErrorCount());
			reconciliationStatisticsVoRes.setOutsideCount(reconciliationStatisticsVo.getOutsideCount());
			reconciliationStatisticsVoRes.setOutsideDate(reconciliationStatisticsVo.getOutsideDate());
			reconciliationStatisticsVoRes.setReconciliationDate(reconciliationStatisticsVo.getReconciliationDate());
			reconciliationStatisticsVoRes.setReconciliationStatus(reconciliationStatisticsVo.getReconciliationStatus());
			reconciliationStatisticsVoRes.setSystemAmount(reconciliationStatisticsVo.getSystemAmount());
			reconciliationStatisticsVoRes.setSystemCount(reconciliationStatisticsVo.getSystemCount());
		}else{
			reconciliationStatisticsVoRes.setErrorMessage("无记录");
			reconciliationStatisticsVoRes.setReturnCode("FAILE");
		}
		return reconciliationStatisticsVoRes;
	}

	/**
	 * 确认完成对账
	 * @param channleNo
	 * @param outsideDate
	 */
	public BaseResponse confirmCompleteReconciliation(String channleNo, Timestamp outsideDate) {
		log.info("确认完成对账,日期：{},渠道：{}", outsideDate, channleNo);
		BaseResponse resp = new BaseResponse();
		//判断今日是否已经对账
	    int reconCount = reconciliationStatisticsDao.findCompleteReconciliation(channleNo, outsideDate,"1");
	    //已确认完成对账不可再次完成
	    if(reconCount>0){
	    	resp.setReturnCode("FAIL");
			resp.setErrorMessage("已确认完成对账，不能再次确认完成对账");
			return resp;
	    }
	    reconCount = reconciliationStatisticsDao.findCompleteReconciliation(channleNo, outsideDate,"0");
	    if(reconCount==0){
	    	resp.setReturnCode("FAIL");
			resp.setErrorMessage("尚未对账，不能确认完成对账");
			return resp;
	    }
		//查询是否存在未处理的异常单，若有将阻止确认完成对账
		boolean completeReconciliation = reconciliationErrorRecordsService.completeReconciliation(channleNo, outsideDate);
		if(completeReconciliation){
			//去解冻重置冻结户账户余额
			if ("Y".equals(needRechargeFrozenAccount)) {
				resp = unfreezeAccount(outsideDate);
			    if(!Constant.SUCCESS.equals(resp.getReturnCode())){
			    	resp.setReturnCode("FAIL");
					resp.setErrorMessage(resp.getErrorMessage());
					return resp;
			    }
			}
			try {
				resp.setReturnCode("SUCCESS");
				resp.setErrorMessage("成功");
				reconciliationStatisticsDao.confirmCompleteReconciliation(channleNo, outsideDate);
			} catch (Exception e) {
				log.error("确认完成对账异常:" + e);
				resp.setReturnCode("FAIL");
				resp.setErrorMessage("确认完成对账失败");
				return resp;
			}
		}else{
			resp.setReturnCode("FAIL");
			resp.setErrorMessage("尚有未处理的异常订单，确认完成对账失败");
			return resp;
		}
		return resp;
	}
	
	/**
	 * 解冻充值冻结
	 * @return
	 */
	private BaseResponse unfreezeAccount(Timestamp outsideDate) {
		BaseResponse resp = new BaseResponse();
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("解冻成功");
		//查询需解冻充值冻结的个数，超3000，分批次处理
		int transEntityCount = transDao.getNeedUnfreezeAccountCount(outsideDate);
		if(transEntityCount==0){
			log.info("无需解冻的充值冻结账户");
			return resp;
		}
		int oneCount = 3000;
		int times = transEntityCount/oneCount;
		InteractiveRequest req=null;
		try{
			for(int i =0;i<=times;i++){
				int star = i*oneCount;
				int end = (i+1)*oneCount;
				log.info("解冻用户充值冻结户，共{}条，批次{}，本次处理{}到{}条",transEntityCount,times+1,star,end);
				//查询未解冻的充值账户list
				List<TransEntity> transEntityList = transDao.getNeedUnfreezeAccountList(outsideDate,star,end);
				for(TransEntity transEntity : transEntityList){
					//查询真正需解冻的记录
					transEntity = transDao.getNeedUnfreezeAccount(transEntity.getAccountOid(), outsideDate);
					BigDecimal unfrozeenBalance = transEntity.getBalance();
					//根据accountNo查询用户冻结户
					AccountInfoEntity accountInfoEntity = accountInfoDao.findByAccountNo(transEntity.getAccountOid());
					//冻结额为0或者冻结户金额小于昨日剩余冻结金额
					if(unfrozeenBalance.compareTo(BigDecimal.ZERO)==0||unfrozeenBalance.compareTo(accountInfoEntity.getBalance())>0){
						log.info("用户{}，冻结金额{}，充值冻结户小于可解冻金额或余额为0，不进行解冻",accountInfoEntity.getUserOid(),unfrozeenBalance);
						continue;
					}
					try {
						accountInfoDao.subtractBalance(unfrozeenBalance,transEntity.getAccountOid());
						log.info("用户{}，冻结金额{}更新成功",accountInfoEntity.getUserOid(),unfrozeenBalance);
						//20171103记录解冻流水
						this.addAccountTrans(unfrozeenBalance,transEntity,accountInfoEntity);
					} catch (Exception e) {
						log.error("更新余额异常 accountNo={} balance={},ex={}", accountInfoEntity.getAccountNo(),unfrozeenBalance, e);
						continue;
					}
					//回调业务同步余额
					req=new InteractiveRequest();
					req.setUserOid(accountInfoEntity.getUserOid());
					req.setUserType(accountInfoEntity.getUserType());
					boolean result=false;
					try {
						result = settlementSdk.notifyChangeAccountBalance(req);
					} catch (Exception e) {
						log.error("用户{}，冻结金额{}，回调异常={}",accountInfoEntity.getUserOid(),accountInfoEntity.getBalance(),e);
					}
					log.info("用户{}，冻结金额{}，回调结果:{}",accountInfoEntity.getUserOid(),accountInfoEntity.getBalance(),result);
				}
			}
		}catch(Exception e){
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("系统异常");
		}
		return resp;
	}

	/**
	 * 解冻充值冻结户记录账户流水
	 * @param unfrozeenBalance
	 * @param transEntity
	 * @param accountInfoEntity
	 */
	private void addAccountTrans(BigDecimal unfrozeenBalance,
			TransEntity transEntity, AccountInfoEntity accountInfoEntity) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		TransEntity entity = new TransEntity();
		String orderNo = seqGenerator.next("UNFROZEN");//解冻无订单号，生成特殊标记的订单号
		entity.setAccountOid(accountInfoEntity.getAccountNo());
		entity.setUserOid(accountInfoEntity.getUserOid());
		entity.setUserType(accountInfoEntity.getUserType());
		entity.setRequestNo(orderNo);
		entity.setAccountOrderOid(orderNo);
		entity.setOrderType(OrderTypeEnum.UNFROZEN.getCode());
		entity.setSystemSource("settlement");
		entity.setOrderNo(orderNo);
		entity.setDirection("02");
		entity.setOrderBalance(unfrozeenBalance);
		entity.setRamark("充值冻结解冻");
		entity.setOrderDesc("解冻");
		entity.setAccountName("充值冻结户");
		entity.setTransTime(time);
		entity.setDataSource("settlement");
		entity.setBalance(accountInfoEntity.getBalance().subtract(unfrozeenBalance));
		entity.setIsDelete("N");
		// 入账，出账用户
		entity.setInputAccountNo("");
		entity.setOutpuptAccountNo(accountInfoEntity.getAccountNo());
		entity.setCreateTime(time);
		entity.setUpdateTime(time);
		entity.setVoucher(BigDecimal.ZERO);
		UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(entity.getUserOid());
		if(null!=userInfoEntity){
			entity.setPhone(userInfoEntity.getPhone());
		}
		transDao.save(entity);
	}

	/**
	 * 新增对账统计信息
	 * @param vo
	 */
	public void saveReconciliationStatistics(ReconciliationStatisticsVo vo){
		if(vo != null){
			reconciliationStatisticsDao.save(vo);
		}
	}
	
	/**
	 * 判断是否完成对账
	 * @param outsideDate
	 * @return
	 */
	public boolean completeReconciliation(Timestamp outsideDate){
		boolean completeReconciliation = false;//是否完成对账
		int needCompleteCount = 2;//需完成对账数
		int completeCount = reconciliationStatisticsDao.getcompleteCount(outsideDate);
		//获取开启的绑卡通道
		ChannelVo channel = comChannelDao.findBindChannel();
		if(channel == null){
			return completeReconciliation;
		}
		if(TradeChannel.baofoopay.getValue().equals(channel.getChannelNo())){
			needCompleteCount = 3;
		}else if(TradeChannel.ucfPayCertPay.getValue().equals(channel.getChannelNo())){
			needCompleteCount = 2;
		}else{
			return completeReconciliation;
		}
		if(completeCount == needCompleteCount){
			completeReconciliation = true;
		}
		return completeReconciliation;
	}
	
}
