//package com.guohuai.seetlement.task;
//
//import java.util.List;
//
//import org.apache.commons.collections.CollectionUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import com.guohuai.boot.account.dao.AccountInfoDao;
//import com.guohuai.boot.account.entity.AccountInfoEntity;
//import com.guohuai.component.util.AccountTypeEnum;
//import com.guohuai.component.util.Constant;
//import com.guohuai.component.util.DateUtil;
//import com.guohuai.component.util.UserTypeEnum;
//import com.guohuai.payadapter.redeem.ConcurrentManager;
//import com.guohuai.payadapter.redeem.ConcurrentManagerDao;
//import com.guohuai.settlement.api.SettlementSdk;
//import com.guohuai.settlement.api.request.InteractiveRequest;
//
//import lombok.extern.slf4j.Slf4j;
//
///**
// * 
// * @ClassName: CollectTaskJob
// * @date 2016年11月26日 下午5:38:53
// *
// */
//@Slf4j
//@Component
//public class UnfreezeAccountTaskJob {
//
//	@Autowired
//	ConcurrentManagerDao concurrentManagerDao;
//
//	@Value("${server.host}")
//	private String host;
//
//	@Autowired
//	private AccountInfoDao accountInfoDao;
//	
//	@Autowired
//	SettlementSdk settlementSdk;
//
//	@Scheduled(cron = "${jobs.unfreezeAccountTask.schedule:0 0 0 * * ?}")
//	public void find() {
//		try {
//			ConcurrentManager concurrentManager = concurrentManagerDao.findByHostAndTaskTag(host, "tradeMinCallback");
//			if (null != concurrentManager) {
//				String date=DateUtil.getLastDay(Constant.fomatNo);
////				List<AccountInfoEntity> infos = accountInfoDao.findFreezeAccount(UserTypeEnum.INVESTOR.getCode(),
////						AccountTypeEnum.RECHARGEFROZEN.getCode(), date+" 00:00:00", date+" 23:59:59");
//				//20170810增加发行人的解冻
//				List<AccountInfoEntity> infos = accountInfoDao.findFreezeAccount(AccountTypeEnum.RECHARGEFROZEN.getCode(),
//						date+" 00:00:00", date+" 23:59:59");
//				log.info("查询{}日内冻结账户",date);
//				if (!CollectionUtils.isEmpty(infos)) {
//					
//					log.info("查询{}日内冻结账户数量{}",date,infos.size());
//					InteractiveRequest req=null;
//					for (AccountInfoEntity accountInfoEntity : infos) {
//						try {
//							accountInfoDao.subtractBalance(accountInfoEntity.getBalance(),accountInfoEntity.getAccountNo());
//							log.info("用户{}，冻结金额{}更新成功",accountInfoEntity.getUserOid(),accountInfoEntity.getBalance());
//						} catch (Exception e) {
//							log.error("更新余额异常 accountNo={} balance={},ex={}", accountInfoEntity.getAccountNo(),accountInfoEntity.getBalance(), e);
//							continue;
//						}
//						
//						//回调
//						req=new InteractiveRequest();
//						req.setUserOid(accountInfoEntity.getUserOid());
//						req.setUserType(accountInfoEntity.getUserType());
//						boolean result=false;
//						try {
//							result = settlementSdk.notifyChangeAccountBalance(req);
//						} catch (Exception e) {
//							log.error("用户{}，冻结金额{}，回调异常={}",accountInfoEntity.getUserOid(),accountInfoEntity.getBalance(),e);
//						}
//						log.info("用户{}，冻结金额{}，回调结果:{}",accountInfoEntity.getUserOid(),accountInfoEntity.getBalance(),result);
//						req=null;
//						
//					}
//					
//				}
//			}
//		} catch (Exception e) {
//			log.error("定时UnfreezeAccountTaskJob异常{}", e);
//		}
//	}
//
//}
