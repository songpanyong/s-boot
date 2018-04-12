/**
 * 
 */
package com.guohuai.boot.account.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import com.guohuai.account.api.response.BaseResponse;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountTransRequest;
import com.guohuai.account.api.request.CreateAccountRequest;
import com.guohuai.account.api.request.CreateTransRequest;
import com.guohuai.account.api.request.EnterAccountRequest;
import com.guohuai.account.api.request.TransPublishRequest;
import com.guohuai.account.api.request.TransferAccountRequest;
import com.guohuai.account.api.response.CreateAccountResponse;
import com.guohuai.account.api.response.CreateTransResponse;
import com.guohuai.account.api.response.TransBalanceResponse;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.basic.component.exception.GHException;
import com.guohuai.boot.account.dao.AccountInfoDao;
import com.guohuai.boot.account.dao.TransDao;
import com.guohuai.boot.account.dao.UserInfoDao;
import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.boot.account.entity.AccountInfoEntity;
import com.guohuai.boot.account.entity.TransEntity;
import com.guohuai.boot.account.entity.UserInfoEntity;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.EnterOrderTypeEnum;
import com.guohuai.component.util.OrderTypeEnum;
import com.guohuai.component.util.UserTypeEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Thinkpad
 *
 */
@Slf4j
@Service
@Transactional
public class AccountTradeService {

	@Autowired
	private TransDao transDao;
	@Autowired
	private AccountInfoDao accountInfoDao;
	@Autowired
	private AccOrderService orderServive;
	@Autowired
	private AccountInfoService accountInfoService;
	
	@Autowired
	private UserInfoDao userInfoDao;
	
	@Autowired
	private EntityManager entityManager;
	
	@Value("${needRechargeFrozenAccount:N}")
	private String needRechargeFrozenAccount;
	
	/**
	 * 会员账户交易 余额操作和交易流水用同一个事物处理 @Title:
	 * addAccountTrans @Description: @param @param accountNo @param @param
	 * orderOid @param @param req @param @return @return
	 * CreateTransResponse @throws
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public CreateTransResponse addAccountTrans(String accountNo, String orderOid, AccountTransRequest req,
			String accountType) throws GHException, Exception {
		CreateTransResponse resp = new CreateTransResponse();
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("");
			String orderNo = req.getOrderNo();
			log.info("会员账户交易 余额操作和记录交易流水 orderOid={},accountNo={},orderNo={}", orderOid, accountNo, orderNo);
			AccountInfoEntity account = accountInfoDao.findByAccountNo(accountNo);
			
			String orderType = req.getOrderType();

			if (account != null) {
				
				
				log.info("交易账户 AccountInfoEntity={},锁定账户", JSONObject.toJSON(account));
				account=accountInfoDao.findByOidForUpdate(account.getOid());

				log.info("同步hibernate缓存，account");
				entityManager.refresh(account);
				
				CreateTransResponse transResp = null;
				/**
				 * 更新发行人或者平台账户余额,20170526新增充值冻结户
				 */
				AccountInfoEntity otherAccount = null;
				AccountInfoEntity otherAccount2 = null;
				// 获取对应账户
				log.info("accountType={},RelationProductNo={}", accountType, req.getRelationProductNo());
				if (StringUtil.in(orderType, OrderTypeEnum.GIVEFREEMONEY.getCode(),
						OrderTypeEnum.EXPIREMONEY.getCode())) {
					// 如果是投资人是体验金账户,获取平台体验金账户
					otherAccount = accountInfoDao.findByUserTypeAndAccountType(UserTypeEnum.PLATFORMER.getCode(),
							accountType);
				} else if(OrderTypeEnum.APPLY.getCode().equals(orderType)){
					// 申购 获取发行人账户、20170526获取充值冻结户
					otherAccount = accountInfoDao.findByUserAccountProduct(UserTypeEnum.PUBLISHER.getCode(),
							accountType, req.getRelationProductNo());
					otherAccount2 = null;
					//断定是否需要充值冻结户 如果不需要，查询时充值冰结户设置为0
					if ("Y".equals(needRechargeFrozenAccount)) {
						otherAccount2 = accountInfoDao.findAccountByAccountTypeAndUserOid(AccountTypeEnum.RECHARGEFROZEN.getCode(),
								req.getUserOid());
					}
					
				} else if (StringUtil.in(orderType, OrderTypeEnum.REDEEM.getCode(),
						OrderTypeEnum.DIVIDEND.getCode())) {
					// 赎回、派息 获取发行人账户
					otherAccount = accountInfoDao.findByUserAccountProduct(UserTypeEnum.PUBLISHER.getCode(),
							accountType, req.getRelationProductNo());
				} else if(OrderTypeEnum.RECHARGE.getCode().equals(orderType)){
					// 充值需要更新用户充值冻结户
					if ("Y".equals(needRechargeFrozenAccount)) {
						otherAccount = accountInfoDao.findAccountByAccountTypeAndUserOid(AccountTypeEnum.RECHARGEFROZEN.getCode(),
								req.getUserOid());
					}else{
						otherAccount = account;
					}
				} else if (StringUtil.in(orderType,
						OrderTypeEnum.WITHDRAWALS.getCode(),OrderTypeEnum.OFFSETPOSITIVE.getCode(),OrderTypeEnum.OFFSETNEGATIVE.getCode(),
						OrderTypeEnum.REDENVELOPE.getCode())) {
					// 当充值或者提现时不需要更新 otherAccount ，这里设置为自身账户
					// 20170324新增冲正冲负红包
					// 20170329新增冻结账户otherAccount，为冻结账户
					otherAccount = account;
				} else if (StringUtil.in(orderType, OrderTypeEnum.CURRENTTOREGULAR.getCode(),
						OrderTypeEnum.REGULARTOCURRENT.getCode())) {
					// 获取转出户类别 转出为定期时 accountType 为定期
					accountType = orderType.equals(OrderTypeEnum.CURRENTTOREGULAR.getCode())
							? AccountTypeEnum.REGULAR.getCode() : AccountTypeEnum.CURRENT.getCode();
					// 转换时查询转出产品户
					List<AccountInfoEntity> accountInfoEntities = accountInfoDao
							.findByUserOidAndAccountTypeAndProductNo(req.getUserOid(), req.getUserType(),
									req.getOutputRelationProductNo(), accountType);
					if (null == accountInfoEntities || accountInfoEntities.size() < 1) {
						log.info("产品转换，发行人转出或转入产品账户不存在，创建账户 accountType={} ,AccountTransRequest:{}", accountType,
								JSONObject.toJSON(req));
						CreateAccountRequest accreq = new CreateAccountRequest();
						accreq.setUserOid(req.getUserOid());
						accreq.setRelationProduct(req.getOutputRelationProductNo());
						accreq.setAccountType(accountType);
						accreq.setUserType(req.getUserType());
						CreateAccountResponse accountResponse = accountInfoService.addAccount(accreq);
						otherAccount = accountInfoService.getAccountByNo(accountResponse.getAccountNo());
					} else {
						log.info("转出户size:{}", accountInfoEntities.size());
						otherAccount = accountInfoEntities.get(0);
					}
				}

				if (otherAccount == null) {
					log.info("平台或发行人账户不存在 AccountTransRequest:{}", JSONObject.toJSON(req));
					resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
					resp.setErrorMessage("平台或发行人账户不存在!");
					return resp;
				}
				
				log.info("同步hibernate缓存，otherAccount");
				entityManager.refresh(otherAccount);

				log.info("otherAccount={}", JSONObject.toJSON(otherAccount));
				String otherAccountNo = otherAccount.getAccountNo();

				BigDecimal realOrderMoney = req.getBalance();// 扣除代金券后的实际金额订单
				BigDecimal voucher = req.getVoucher();// 代金券的金额
				BigDecimal orderMoney = realOrderMoney;
				if (req.getOrderType().equals(OrderTypeEnum.APPLY.getCode())) {
					if (null != req.getVoucher()) {
						orderMoney = realOrderMoney.add(voucher);// 增加代金券金额
					}
				}
				BigDecimal accBalance = account.getBalance();
				BigDecimal afterBalance = BigDecimal.ZERO;
				BigDecimal accOtherBalance = BigDecimal.ZERO;
				BigDecimal afterOtherBalance = BigDecimal.ZERO;

				accOtherBalance = otherAccount.getBalance();
				otherAccountNo = otherAccount.getAccountNo();
				int result = 0;
				if (orderType.equals(OrderTypeEnum.APPLY.getCode())) {
					// 如果订单类型为申购活期,贷：投资人活期账户余额增加 //发行人账户余额不做处理
					AccountInfoEntity basicAccoun = accountInfoDao
							.findByUserOidAndAccountType(req.getUserOid(), AccountTypeEnum.BASICER.getCode()).get(0);
					log.info("同步hibernate缓存前 balance:{}",basicAccoun.getBalance());
					entityManager.refresh(basicAccoun);
					log.info("同步hibernate缓存后 balance:{}",basicAccoun.getBalance());
					result = accountInfoDao.subtractBalance(realOrderMoney, basicAccoun.getAccountNo());
					log.info("申购扣除基本户余额结果={}", result);
					if (result == 0) {
						resp.setReturnCode(Constant.BALANCELESS);
						resp.setErrorMessage("申购扣除基本户余额失败，用户基本户余额不足");
						return resp;
					}
					afterBalance = basicAccoun.getBalance().subtract(realOrderMoney);
					log.info("申购记录基本户明细 accountNo={},orderOid={},orderMoney={},orderNo={},afterBalance", basicAccoun.getAccountNo(),
							orderOid, realOrderMoney, orderNo,afterBalance);
					req.setOrderDesc("申购记录基本户明细");
					req.setVoucher(BigDecimal.ZERO);
					transResp = this.addTrans(req, orderOid, basicAccoun.getAccountNo(), accountNo, req.getUserOid(),
							req.getUserType(), afterBalance, "02", AccountTypeEnum.BASICER.getCode(), basicAccoun.getAccountNo());

					//20170526申购可以使用充值冻结余额，并优先使用充值冻结余额 balance1.compareTo(balance2) != -1){//-1 小于  0 等于 1 大于
					if(otherAccount2 != null&&otherAccount2.getBalance().compareTo(BigDecimal.ZERO) !=0){
						if(realOrderMoney.compareTo(otherAccount2.getBalance())  == -1){
							otherAccount2=accountInfoDao.findByOidForUpdate(otherAccount2.getOid());
							//同步hibernate缓存
							entityManager.refresh(otherAccount2);
							
							result = accountInfoDao.subtractBalance(realOrderMoney, otherAccount2.getAccountNo());
							afterBalance = otherAccount2.getBalance().subtract(realOrderMoney);
						}else{
							result = accountInfoDao.subtractBalance(otherAccount2.getBalance(), otherAccount2.getAccountNo());
							afterBalance = BigDecimal.ZERO;
						}
						log.info("申购扣除充值冻结户余额结果={}", result);
						if (result == 0) {
							resp.setReturnCode(Constant.BALANCELESS);
							resp.setErrorMessage("申购扣除充值冻结户余额失败，用户充值冻结户余额余额不足，请重新申购");
							return resp;
						}
						log.info("申购记录充值冻结户明细 accountNo={},orderOid={},orderMoney={},orderNo={}", otherAccount2.getAccountNo(),
								orderOid, realOrderMoney, orderNo);
						req.setOrderDesc("申购记录充值冻结户明细");
						req.setVoucher(BigDecimal.ZERO);
						transResp = this.addTrans(req, orderOid, otherAccount2.getAccountNo(), accountNo, req.getUserOid(),
								req.getUserType(), afterBalance, "02" , AccountTypeEnum.RECHARGEFROZEN.getCode(), otherAccount2.getAccountNo());
					}

					// 记录投资人交易明细，发行的明细及余额更新，等份额确认后更新 所以申购不记录发行人交易流水
//					log.info("申购增加投资人产品户余额 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
//							orderMoney, orderNo);
//					accountInfoDao.addBalance(orderMoney, accountNo);
//					afterBalance = accBalance.add(orderMoney);
//					log.info("申购记录投资人产品户明细 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
//							orderMoney, orderNo);
//					req.setOrderDesc("申购记录投资人产品户明细");
//					req.setVoucher(voucher);
//					transResp = this.addTrans(req, orderOid, accountNo, otherAccount.getAccountNo(), req.getUserOid(),
//							req.getUserType(), afterBalance, "01");
				} else if (orderType.equals(OrderTypeEnum.REDEEM.getCode())) {
					// 如果订单类型为赎回 借：投资人活期户（减少投资人活期户余额） 贷：发行人产品户（减少发行人产品户金额）
//					if (accBalance.compareTo(orderMoney) < 0) {
//						resp.setReturnCode(Constant.BALANCELESS);
//						resp.setErrorMessage("余额不足！");
//						log.debug("子账户余额不足，[accountNo=" + accountNo + "]");
//						return resp;
//					}

					log.info("查询用户基本户");
					AccountInfoEntity basicAccoun = accountInfoDao
							.findByUserOidAndAccountType(req.getUserOid(), AccountTypeEnum.BASICER.getCode()).get(0);
					basicAccoun=accountInfoDao.findByOidForUpdate(basicAccoun.getOid());
					//同步hibernate缓存
					entityManager.refresh(basicAccoun);
					
					result = accountInfoDao.addBalance(orderMoney, basicAccoun.getAccountNo());
					log.info("赎回增加基本户余额结果={}", result);
					if (result == 0) {
						// 上面已更新数据库，这里需要回滚事务
						throw new GHException(Integer.parseInt(Constant.BALANCELESS), "赎回增加基本户余额失败，用户基本户余额不足");
					}
					afterBalance = basicAccoun.getBalance().add(orderMoney);
					log.info("赎回记录基本户交易明细accountNo={},orderOid={},orderMoney={},orderNo={}", basicAccoun.getAccountNo(),
							orderOid, orderMoney, orderNo);
					req.setOrderDesc("赎回记录基本户明细");
					transResp = this.addTrans(req, orderOid, basicAccoun.getAccountNo(), accountNo, req.getUserOid(),
							req.getUserType(), afterBalance, "01",AccountTypeEnum.BASICER.getCode(), basicAccoun.getAccountNo());
				} else if (orderType.equals(OrderTypeEnum.EXPIREMONEY.getCode())) {
					// 如果订单类型为体验金到期,借：投资人体验金账户（减少投资人体验金）贷：平台体验金账户（减少平台体验金）
					if (accBalance.compareTo(orderMoney) < 0) {
						resp.setReturnCode(Constant.BALANCELESS);
						resp.setErrorMessage("余额不足！");
						log.debug("子账户余额不足，[accountNo=" + accountNo + "]");
						return resp;
					}
					// 更新投资人账户，记录交易流水
					result = accountInfoDao.subtractBalance(orderMoney, accountNo);
					if (result == 0) {
						resp.setReturnCode(Constant.BALANCELESS);
						resp.setErrorMessage("余额不足！");
						log.debug("子账户余额不足，[accountNo=" + accountNo + "]");
						return resp;
					}
					
					afterBalance = accBalance.subtract(orderMoney);
					req.setOrderDesc("体验金过期记录投资人账户明细");
					transResp = this.addTrans(req, orderOid, accountNo, otherAccountNo, account.getUserOid(),
							account.getUserType(), afterBalance, "02", AccountTypeEnum.EXPERIENCE.getCode(), otherAccountNo);
					// 更新平台体验金账户，记录交易流水
					accountInfoDao.addBalance(orderMoney.abs().multiply(new BigDecimal(-1)), otherAccountNo);
					afterOtherBalance = accOtherBalance.subtract(orderMoney);
					req.setOrderDesc("体验金过期记录平台账户明细");
					transResp = this.addTrans(req, orderOid, otherAccountNo, accountNo, account.getUserOid(),
							otherAccount.getUserType(), afterOtherBalance, "02",AccountTypeEnum.EXPERIENCE.getCode(), otherAccountNo);
				} else if (orderType.equals(OrderTypeEnum.GIVEFREEMONEY.getCode())) {
					// 如果订单类型为赠送体验金 借：平台体验金户（增加平台体验金户余额）
					// 贷：投资人体验金账户（增加体验金账户余额）
					log.info("赠送体验金 更新账户余额 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					accountInfoDao.addBalance(orderMoney, accountNo);
					afterBalance = accBalance.add(orderMoney);
					req.setOrderDesc("赠送体验金记录投资人账户明细");
					transResp = this.addTrans(req, orderOid, accountNo, otherAccountNo, account.getUserOid(),
							account.getUserType(), afterBalance, "01",AccountTypeEnum.EXPERIENCE.getCode(), otherAccountNo);

					log.info("赠送体验金 更新平台账户余额 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					accountInfoDao.addBalance(orderMoney, otherAccountNo);
					afterOtherBalance = accOtherBalance.add(orderMoney);
					req.setOrderDesc("赠送体验金记录平台账户明细");
					transResp = this.addTrans(req, orderOid, otherAccountNo, accountNo, account.getUserOid(),
							otherAccount.getUserType(), afterOtherBalance, "01",AccountTypeEnum.EXPERIENCE.getCode(), otherAccountNo);
				} else if (orderType.equals(OrderTypeEnum.DIVIDEND.getCode())) {
					// 如果订单类型为派息, 借：发行人产品利息户 贷：投资人利息户余额增加
					// 增加投资人利息账户余额
					log.info("派息 更新投资人利息账户余额 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					accountInfoDao.addBalance(orderMoney, accountNo);
					afterBalance = accBalance.add(orderMoney);
					log.info("派息 记录交易明细 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					req.setOrderDesc("派息记录投资人产品利息户账户明细");
					transResp = this.addTrans(req, orderOid, accountNo, otherAccountNo, account.getUserOid(),
							account.getUserType(), afterBalance, "01",AccountTypeEnum.CURRENTINTEREST.getCode(), otherAccountNo);

					// 发行人产品利息户
					log.info("派息 更新发行人产品利息户 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					accountInfoDao.addBalance(orderMoney, otherAccountNo);
					afterOtherBalance = accOtherBalance.add(orderMoney);
					req.setOrderDesc("派息记录发行人产品利息户账户明细");
					transResp = this.addTrans(req, orderOid, otherAccountNo, accountNo, account.getUserOid(),
							otherAccount.getUserType(), afterOtherBalance, "01",AccountTypeEnum.CURRENTINTEREST.getCode(), otherAccountNo);

					// 利息增加到对应发行人产品户
					AccountInfoEntity productPubLisherAccount = accountInfoDao.findByUserAccountProduct(
							UserTypeEnum.PUBLISHER.getCode(), req.getProductType(), req.getRelationProductNo());
					log.info("派息利息增加到对应发行人产品户 accountNo={},orderOid={},orderMoney={}",
							productPubLisherAccount.getAccountNo(), orderOid, orderMoney);
					accountInfoDao.addBalance(orderMoney, productPubLisherAccount.getAccountNo());
					accOtherBalance = productPubLisherAccount.getBalance();
					afterOtherBalance = accOtherBalance.add(orderMoney);
					req.setOrderDesc("派息记录发行人产品户账户明细");
					transResp = this.addTrans(req, orderOid, productPubLisherAccount.getAccountNo(), accountNo,
							account.getUserOid(), productPubLisherAccount.getUserType(), afterOtherBalance, "01", 
							AccountTypeEnum.CURRENTINTEREST.getCode(), productPubLisherAccount.getAccountNo());

					// 利息增加到对应投资人产品户
					log.info("派息 利息增加到投资人产品户（利息加到对应产品账户） userOid={},userType={},getRelationProductNo={},ProductType={}",
							req.getUserOid(), UserTypeEnum.INVESTOR.getCode(), req.getRelationProductNo(),
							req.getProductType());
					List<AccountInfoEntity> acountInfoEntitys = accountInfoDao.findByUserOidAndAccountTypeAndProductNo(
							req.getUserOid(), UserTypeEnum.INVESTOR.getCode(), req.getRelationProductNo(),
							req.getProductType());
					AccountInfoEntity productAccount = null;
					if (null != acountInfoEntitys && acountInfoEntitys.size() > 0) {
						productAccount = acountInfoEntitys.get(0);
					}
					accountInfoDao.addBalance(orderMoney, productAccount.getAccountNo());
					accOtherBalance = productAccount.getBalance();
					afterOtherBalance = accOtherBalance.add(orderMoney);
					req.setOrderDesc("派息记录投资人产品户账户明细");
					transResp = this.addTrans(req, orderOid, productAccount.getAccountNo(), accountNo,
							account.getUserOid(), productAccount.getUserType(), afterOtherBalance, "01", 
							AccountTypeEnum.CURRENTINTEREST.getCode(), productAccount.getAccountNo());

				} else if (orderType.equals(OrderTypeEnum.RECHARGE.getCode())) {
					// 如果订单类型为充值更新用户基本账户
					log.info("充值增加基本户余额 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					accountInfoDao.addBalance(orderMoney, accountNo);
					afterBalance = accBalance.add(orderMoney);
					// 记录投资人交易明细，发行的明细及余额更新
					log.info("充值 记录交易明细accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					req.setOrderDesc("充值记录"+UserTypeEnum.getEnumName(req.getUserType())+"基本户账户明细");
					transResp = this.addTrans(req, orderOid, accountNo, "", req.getUserOid(),
							req.getUserType(), afterBalance, "01", AccountTypeEnum.BASICER.getCode(), accountNo);
					if ("Y".equals(needRechargeFrozenAccount)) {
						//20170526充值记录用户充值冻结户
						log.info("充值增加充值冻结户余额 accountNo={},orderOid={},orderMoney={},orderNo={}", otherAccountNo, orderOid,
								orderMoney, orderNo);
						accountInfoDao.addBalance(orderMoney, otherAccountNo);
						afterBalance = otherAccount.getBalance().add(orderMoney);
						// 记录投资人交易明细，发行的明细及余额更新
						log.info("充值 记录充值冻结户交易明细accountNo={},orderOid={},orderMoney={},orderNo={}", otherAccountNo, orderOid,
								orderMoney, orderNo);
						req.setOrderDesc("充值记录"+UserTypeEnum.getEnumName(req.getUserType())+"充值冻结户明细");
						transResp = this.addTrans(req, orderOid, otherAccountNo, "", req.getUserOid(),
								req.getUserType(), afterBalance, "01",AccountTypeEnum.RECHARGEFROZEN.getCode(), otherAccountNo);
					}
				} else if (orderType.equals(OrderTypeEnum.WITHDRAWALS.getCode())) {
					//20170329新增提现冻结账户操作逻辑
					// 如果操作账户为提现冻结户，1：第一次记账，基本户余额不变，提现冻结户余额增加；2：第二次记账，撤单操作
					if(AccountTypeEnum.FROZEN.getCode().equals(accountType)){
						//撤单标记
						if(Constant.KILLORDER.equals(req.getRemark())){
							//查询该订单记账状态
							String accountingTimes = getWithdrawalsAccountStatus(req.getUserOid(), orderNo);
							if("1".equals(accountingTimes)){//只有提现冻结户记账，撤单减少提现冻结户余额
								log.info("提现撤单减少提现冻结户余额 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
										orderMoney, orderNo);
								result = accountInfoDao.subtractBalance(orderMoney, accountNo);
								if (result == 0) {
									resp.setReturnCode(Constant.BALANCELESS);
									resp.setErrorMessage("提现冻结户户余额不足，提现撤单失败！");
									log.debug("提现冻结户余额不足，[accountNo=" + accountNo + "]");
									return resp;
								}
								afterBalance = accBalance.subtract(orderMoney);
								// 记录投资人交易明细余额更新
								log.info("提现撤单 记录交易明细accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
										orderMoney, orderNo);
								req.setOrderDesc("提现撤单记录投资人提现冻结户账户明细");
								transResp = this.addTrans(req, orderOid, accountNo, otherAccount.getAccountNo(), req.getUserOid(),
										req.getUserType(), afterBalance, "02",AccountTypeEnum.FROZEN.getCode(), otherAccount.getAccountNo());
							}else if("2".equals(accountingTimes)){//基本户已记账，撤单增加基本户余额
								AccountInfoEntity baseAccount = accountInfoDao.findBasicAccountByUserOid(req.getUserOid());
								
								log.info("提现撤单增加基本户余额 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
										orderMoney, orderNo);
								accountInfoDao.addBalance(orderMoney, baseAccount.getAccountNo());
								afterBalance = baseAccount.getBalance().add(orderMoney);
								req.setOrderDesc("提现撤单记录投资人基本户账户明细");
								transResp = this.addTrans(req, orderOid, accountNo, baseAccount.getAccountNo(), req.getUserOid(),
										req.getUserType(), afterBalance, "01",AccountTypeEnum.BASICER.getCode(), baseAccount.getAccountNo());
							}
						}else{
							//查询基本户的余额判断是否够冻结
							AccountInfoEntity baseAccount = accountInfoDao.findBasicAccountByUserOid(req.getUserOid());
							//同步hibernate缓存
							entityManager.refresh(baseAccount);
							boolean isExcess = isExcess(baseAccount.getBalance(),orderMoney);
							if(isExcess){
								log.info("提现增加提现冻结户余额 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
										orderMoney, orderNo);
								accountInfoDao.addBalance(orderMoney, accountNo);
								afterBalance = accBalance.add(orderMoney);
								req.setOrderDesc("提现记录投资人提现冻结户账户明细");
								transResp = this.addTrans(req, orderOid, accountNo, otherAccount.getAccountNo(), req.getUserOid(),
										req.getUserType(), afterBalance, "01",AccountTypeEnum.FROZEN.getCode(), otherAccount.getAccountNo());
							}else{
								resp.setReturnCode(Constant.BALANCELESS);
								resp.setErrorMessage("基本户余额不足，提现失败！");
								log.debug("基本户余额不足，[accountNo=" + baseAccount.getAccountNo() + "]");
								return resp;
							}
						}
					}else{
						// 如果订单类型为提现更新用户基本账户和冻结账户
						// 查询该用户名下提现冻结户
						AccountInfoEntity frozenAccount = accountInfoDao.findFrozenAccountByUserOid(req.getUserOid());
						if(frozenAccount != null){
							frozenAccount=accountInfoDao.findByOidForUpdate(frozenAccount.getOid());
							entityManager.refresh(frozenAccount);
							log.info("提现减少体现冻结户余额 accountNo={},orderOid={},orderMoney={},orderNo={}", frozenAccount.getAccountNo(), orderOid,
									orderMoney, orderNo);
							result = accountInfoDao.subtractBalance(orderMoney, frozenAccount.getAccountNo());
							if (result == 0) {
								resp.setReturnCode(Constant.BALANCELESS);
								resp.setErrorMessage("提现冻结户余额不足，提现失败！");
								log.debug("提现冻结户余额不足，[accountNo=" + frozenAccount.getAccountNo() + "]");
								return resp;
							}
							log.info("提现减少基本户余额 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
									orderMoney, orderNo);
							result = accountInfoDao.subtractBalance(orderMoney, accountNo);
							if (result == 0) {
								resp.setReturnCode(Constant.BALANCELESS);
								resp.setErrorMessage("基本户余额不足，提现失败！");
								log.debug("基本户余额不足，[accountNo=" + accountNo + "]");
								return resp;
							}
							BigDecimal frozenAfterBalance = frozenAccount.getBalance().subtract(orderMoney);
							afterBalance = accBalance.subtract(orderMoney);
							// 记录投资人交易明细及余额更新
							log.info("提现 记录交易明细accountNo={},orderOid={},orderMoney={},orderNo={}", frozenAccount.getAccountNo(), orderOid,
									orderMoney, orderNo);
							req.setOrderDesc("提现记录投资人提现冻结户账户明细");
							transResp = this.addTrans(req, orderOid, frozenAccount.getAccountNo(), frozenAccount.getAccountNo(), req.getUserOid(),
									req.getUserType(), frozenAfterBalance, "02", AccountTypeEnum.FROZEN.getCode(), frozenAccount.getAccountNo());
							// 记录投资人交易明细，发行的明细及余额更新
							log.info("提现 记录交易明细accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
									orderMoney, orderNo);
							req.setOrderDesc("提现记录投资人基本户账户明细");
							transResp = this.addTrans(req, orderOid, accountNo, otherAccount.getAccountNo(), req.getUserOid(),
									req.getUserType(), afterBalance, "02", AccountTypeEnum.BASICER.getCode(), accountNo);
						}else{
							resp.setReturnCode(Constant.BALANCELESS);
							resp.setErrorMessage("提现冻结户不存在，提现失败！");
							log.error("提现冻结户不存在，[userOid=" + req.getUserOid() + "]");
							return resp;
						}

					}
				} else if (StringUtil.in(orderType, OrderTypeEnum.CURRENTTOREGULAR.getCode(),
						OrderTypeEnum.REGULARTOCURRENT.getCode())) {
					// 如果订单类型为提现更新用户基本账户
					log.info("产品转换，当前产品户余额减少 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					result = accountInfoDao.subtractBalance(orderMoney, accountNo);
					if (result == 0) {
						resp.setReturnCode(Constant.BALANCELESS);
						resp.setErrorMessage("产品户余额不足，转出失败！");
						log.debug("产品户余额不足，转出失败 [accountNo=" + accountNo + "]");
						return resp;
					}
					afterBalance = accBalance.subtract(orderMoney);
					// 记录投资人交易明细，发行的明细及余额更新
					log.info("产品转换记录投资人转出账户明细 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					req.setOrderDesc("产品转换记录投资人转出账户明细");
					transResp = this.addTrans(req, orderOid, accountNo, otherAccount.getAccountNo(), req.getUserOid(),
							req.getUserType(), afterBalance, "02",AccountTypeEnum.PRODUCT.getCode(), otherAccount.getAccountNo());

					log.info("产品转换，转出对应产品户余额增加 accountNo={},orderOid={},orderMoney={},orderNo={}",
							otherAccount.getAccountNo(), orderOid, orderMoney, orderNo);
					accountInfoDao.addBalance(orderMoney, otherAccount.getAccountNo());
					afterBalance = otherAccount.getBalance().add(orderMoney);
					log.info("产品转换记录投资人转入账户明细 accountNo={},orderOid={},orderMoney={},orderNo={}",
							otherAccount.getAccountNo(), orderOid, orderMoney, orderNo);
					req.setOrderDesc("产品转换记录投资人转入账户明细");
					// 记录明细时，需要记录转出产品编号
					req.setRelationProductNo(req.getOutputRelationProductNo());
					transResp = this.addTrans(req, orderOid, otherAccount.getAccountNo(), accountNo,
							otherAccount.getUserOid(), otherAccount.getUserType(), afterBalance, "01", 
							AccountTypeEnum.PRODUCT.getCode(), otherAccount.getAccountNo());
				}else if (orderType.equals(OrderTypeEnum.OFFSETPOSITIVE.getCode())) {
					// 如果订单类型为冲正更新用户基本账户
					log.info("冲正增加基本户余额 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					accountInfoDao.addBalance(orderMoney, accountNo);
					afterBalance = accBalance.add(orderMoney);
					// 记录投资人交易明细，发行的明细及余额更新
					log.info("冲正 记录交易明细accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					req.setOrderDesc("冲正 记录投资人基本户账户明细");
					transResp = this.addTrans(req, orderOid, accountNo, "", req.getUserOid(),
							req.getUserType(), afterBalance, "01",AccountTypeEnum.BASICER.getCode(), accountNo);
				} else if (orderType.equals(OrderTypeEnum.OFFSETNEGATIVE.getCode())) {
					// 如果订单类型为冲负更新用户基本账户
					log.info("冲负减少基本户余额 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					result = accountInfoDao.subtractBalance(orderMoney, accountNo);
					if (result == 0) {
						resp.setReturnCode(Constant.BALANCELESS);
						resp.setErrorMessage("基本户余额不足，冲负失败！");
						log.debug("基本户余额不足，[accountNo=" + accountNo + "]");
						return resp;
					}
					afterBalance = accBalance.subtract(orderMoney);
					// 记录投资人交易明细，发行的明细及余额更新
					log.info("冲负 记录交易明细accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					req.setOrderDesc("冲负 记录投资人基本户账户明细");
					transResp = this.addTrans(req, orderOid, accountNo, otherAccount.getAccountNo(), req.getUserOid(),
							req.getUserType(), afterBalance, "02", AccountTypeEnum.BASICER.getCode(), accountNo);
				} else if (orderType.equals(OrderTypeEnum.REDENVELOPE.getCode())) {
					// 如果订单类型为红包更新用户基本账户
					log.info("红包 增加基本户余额 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					accountInfoDao.addBalance(orderMoney, accountNo);
					afterBalance = accBalance.add(orderMoney);
					// 记录投资人交易明细，发行的明细及余额更新
					log.info("红包 记录交易明细accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					req.setOrderDesc("红包记录投资人基本户账户明细");
					transResp = this.addTrans(req, orderOid, accountNo, "", req.getUserOid(),
							req.getUserType(), afterBalance, "01", AccountTypeEnum.BASICER.getCode(), accountNo);
				}

				if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
					resp.setReturnCode(Constant.SUCCESS);
					resp.setErrorMessage("成功");
				} else {
					resp.setReturnCode(transResp.getReturnCode());
					resp.setErrorMessage(transResp.getErrorMessage());
					log.info("交易失败 orderNo={},resp={}", orderNo, JSONObject.toJSON(resp));
				}
			} else {
				resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
				resp.setErrorMessage("账户不存在!");
				log.info("账户不存在!accountNo = " + accountNo);
				return resp;
			}

		log.info("会员账户交易 余额操作和记录交易流水完成 orderOid={},resp={}", orderOid, JSONObject.toJSON(resp));
		return resp;
	}

	/**
	 * 对账补单（用户基本户、基本户交易流水）
	 */
	public CreateTransResponse addAccountTransReconciliation(String accountNo, String orderOid, AccountTransRequest req, String accountType)  {
		CreateTransResponse resp = new CreateTransResponse();
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("");
			String orderNo = req.getOrderNo();
			log.info("会员账户交易 余额操作和记录交易流水 orderOid={},accountNo={},orderNo={}", orderOid, accountNo, orderNo);
			AccountInfoEntity account = accountInfoDao.findByAccountNo(accountNo);

			if (account == null) {
				resp.setReturnCode(Constant.ACCOUNTNOTEXISTS);
				resp.setErrorMessage("账户不存在!");
				log.info("账户不存在!accountNo = " + accountNo);
				return resp;
			}
				log.info("交易账户 AccountInfoEntity={},锁定账户", JSONObject.toJSON(account));
				account=accountInfoDao.findByOidForUpdate(account.getOid());
				CreateTransResponse transResp = null;
				// 获取对应账户
				log.info("accountType={},RelationProductNo={}", accountType, req.getRelationProductNo());

				BigDecimal realOrderMoney = req.getBalance();// 扣除代金券后的实际金额订单
				BigDecimal voucher = req.getVoucher();// 代金券的金额
				BigDecimal orderMoney = realOrderMoney;
				if (req.getOrderType().equals(OrderTypeEnum.APPLY.getCode())) {
					if (null != req.getVoucher()) {
						orderMoney = realOrderMoney.add(voucher);// 增加代金券金额
					}
				}
				BigDecimal accBalance = account.getBalance();
				BigDecimal afterBalance = BigDecimal.ZERO;

					// 如果订单类型为充值更新用户基本账户
					log.info("充值增加基本户余额 accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					accountInfoDao.addBalance(orderMoney, accountNo);
					afterBalance = accBalance.add(orderMoney);
					// 记录投资人交易明细，发行的明细及余额更新
					log.info("充值 记录交易明细accountNo={},orderOid={},orderMoney={},orderNo={}", accountNo, orderOid,
							orderMoney, orderNo);
					req.setOrderDesc("充值记录投资人基本户账户明细");
					transResp = this.addTrans(req, orderOid, accountNo, "", req.getUserOid(),
							req.getUserType(), afterBalance, "01", AccountTypeEnum.BASICER.getCode(), accountNo);

				if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
					resp.setReturnCode(Constant.SUCCESS);
					resp.setErrorMessage("成功");
				} else {
					resp.setReturnCode(transResp.getReturnCode());
					resp.setErrorMessage(transResp.getErrorMessage());
					log.info("交易失败 orderNo={},resp={}", orderNo, JSONObject.toJSON(resp));
				}

		log.info("会员账户交易 余额操作和记录交易流水完成 orderOid={},resp={}", orderOid, JSONObject.toJSON(resp));
		return resp;
	}

	/**
	 * 新增交易流水
	 */
	@Transactional
	public CreateTransResponse addTrans(AccountTransRequest req, String orderOid, String inputAccountNo,
			String outputAccountNo, String userOid, String userType, BigDecimal afterBalance, 
			String direction, String accountType, String accountNo) {
		CreateTransRequest transRequest = new CreateTransRequest();
		transRequest.setUserOid(userOid);
		transRequest.setAccountOid(accountNo);
		transRequest.setUserType(userType);
		transRequest.setRequestNo(req.getRequestNo());
		transRequest.setAccountOrderOid(orderOid);
		transRequest.setOrderType(req.getOrderType());
		transRequest.setSystemSource(req.getSystemSource());
		transRequest.setOrderNo(req.getOrderNo());
		transRequest.setRelationProductNo(req.getRelationProductNo());
		transRequest.setDirection(direction);
		transRequest.setInputAccountNo(inputAccountNo);
		transRequest.setOutpuptAccountNo(outputAccountNo);
		transRequest.setOrderBalance(req.getBalance());
		transRequest.setVoucher(req.getVoucher());
		transRequest.setTransTime(new Timestamp(System.currentTimeMillis()));
		transRequest.setDataSource(req.getSystemSource());
		transRequest.setBalance(afterBalance);
		transRequest.setRamark(req.getRemark());
		transRequest.setOrderDesc(req.getOrderDesc());
		transRequest.setAccountType(accountType);
		// 财务入账标志
		return this.addAccountTrans(transRequest);
	}

	/**
	 * 新增交易流水
	 */
	@Transactional
	public BaseResponse addTrans(TransEntity entity, String orderOid, String accountNo,
								 String outputAccountNo, String userOid, String userType, BigDecimal afterBalance, String direction,String remark,String orderDesc,String accountType) {
		CreateTransRequest transRequest = new CreateTransRequest();
		transRequest.setUserOid(userOid);
		transRequest.setAccountOid(accountNo);
		transRequest.setUserType(userType);
		transRequest.setRequestNo(entity.getRequestNo());
		transRequest.setAccountOrderOid(orderOid);
		transRequest.setOrderType(entity.getOrderType());
		transRequest.setSystemSource(entity.getSystemSource());
		transRequest.setOrderNo(entity.getOrderNo());
		transRequest.setRelationProductNo(entity.getRelationProductNo());
		transRequest.setDirection(direction);
		transRequest.setInputAccountNo(accountNo);
		transRequest.setOutpuptAccountNo(outputAccountNo);
		transRequest.setOrderBalance(entity.getBalance());
		transRequest.setVoucher(entity.getVoucher());
		transRequest.setTransTime(new Timestamp(System.currentTimeMillis()));
		transRequest.setDataSource(entity.getSystemSource());
		transRequest.setBalance(afterBalance);
		transRequest.setRamark(remark);
		transRequest.setOrderDesc(orderDesc);
		
		transRequest.setAccountType(accountType);
		// 财务入账标志
		return this.addAccountTrans(transRequest);
	}
	@Transactional
	public CreateTransResponse addAccountTrans(String accountNo, String orderNo, TransPublishRequest req) {
		CreateTransResponse resp = new CreateTransResponse();
		resp.setReturnCode(Constant.SUCCESS);
		AccountInfoEntity account = accountInfoDao.findByAccountNo(accountNo);
		BigDecimal accBalance = BigDecimal.ZERO;
		BigDecimal afterBalance = BigDecimal.ZERO;
		BigDecimal orderMoney = req.getBalance();
		try {
			if (account != null) {
				accBalance = account.getBalance();
				CreateTransResponse transResp = null;
				log.info("增加账户{},发行额：{}", accountNo, req.getBalance());
				accountInfoDao.addBalance(req.getBalance(), accountNo);
				afterBalance = accBalance.add(orderMoney);

				/**
				 * 交易流水
				 */
				CreateTransRequest transReq = new CreateTransRequest();
				transReq.setAccountOid(req.getAccountNo());
				transReq.setUserType(UserTypeEnum.PUBLISHER.getCode());
				transReq.setRequestNo(req.getRequestNo());
				transReq.setAccountOrderOid(orderNo);
				transReq.setOrderType(req.getOrderType());
				transReq.setSystemSource(req.getSystemSource());
				transReq.setDataSource(req.getSystemSource());
				transReq.setOrderNo(req.getOrderNo());
				transReq.setRelationProductNo(req.getRelationProductNo());
				transReq.setInputAccountNo(accountNo);
				transReq.setDirection("01");
				transReq.setOrderBalance(req.getBalance());
				transReq.setDataSource("");
				transReq.setBalance(afterBalance);
				// 财务入账标志
				transReq.setFinanceMark("");
				transReq.setOrderDesc("增加发行人申购发行额");
				transResp = this.addAccountTrans(transReq);
				if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
					resp.setReturnCode(Constant.SUCCESS);
					resp.setErrorMessage("成功");
				} else {
					resp.setReturnCode(transResp.getReturnCode());
					resp.setErrorMessage(transResp.getErrorMessage());
				}

			}

		} catch (Exception e) {
			log.error("账户交易失败，失败原因:", e);
			throw new GHException(Integer.parseInt(Constant.FAIL), resp.getErrorMessage());
		}
		return resp;
	}

	/**
	 * 平台、投资人转账, 余额操作和交易流水用同一个事物处理 @Title:
	 * addAccountTrans @Description: @param @param accountNo @param @param
	 * orderNo @param @param req @param @return @return
	 * CreateTransResponse @throws
	 */
	@Transactional
	public CreateTransResponse addAccountTrans(String orderNo, TransferAccountRequest req) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		CreateTransResponse resp = new CreateTransResponse();
		resp.setReturnCode(Constant.SUCCESS);
		try {
			AccountInfoEntity outAccount = accountInfoDao.findByAccountNo(req.getOutpuptAccountNo());
			AccountInfoEntity inAccount = accountInfoDao.findByAccountNo(req.getInputAccountNo());
			// 借贷标记
			if (outAccount != null && inAccount != null) {
				/**
				 * 更新账户余额
				 */
				// 给转出账户扣款

				TransBalanceResponse transBalanceResp = null;

				transBalanceResp = this.updateBalance(req.getBalance(), TransEntity.TRANSFER, req.getOutpuptAccountNo(),
						req.getInputAccountNo());
				if (!transBalanceResp.getReturnCode().equals(Constant.SUCCESS)) {
					resp.setReturnCode(transBalanceResp.getReturnCode());
					resp.setErrorMessage(transBalanceResp.getErrorMessage());
					// 把订单状态改为失败
					AccOrderEntity order = orderServive.getOrderByNo(orderNo);
					order.setOrderStatus(AccOrderEntity.ORDERSTATUS_INIT);
					return resp;
				}

				/**
				 * 交易流水
				 */
				// 转出账户交易流水
				CreateTransRequest outTransReq = new CreateTransRequest();
				outTransReq.setAccountOid(req.getOutpuptAccountNo());
				outTransReq.setOutpuptAccountNo(req.getOutpuptAccountNo());
				outTransReq.setInputAccountNo(req.getInputAccountNo());
				outTransReq.setRequestNo(req.getRequestNo());
				outTransReq.setAccountOrderOid(orderNo);
				outTransReq.setOrderType(req.getOrderType());
				outTransReq.setOrderNo(req.getOrderNo());
				outTransReq.setDirection("");
				outTransReq.setOrderBalance(req.getBalance());
				outTransReq.setTransTime(time);
				outTransReq.setDataSource("");
				outTransReq.setBalance(transBalanceResp.getBalance());
				// 财务入账标志
				outTransReq.setFinanceMark("");
				CreateTransResponse outTransResp = this.addAccountTrans(outTransReq);

				// 转入账户交易流水
				CreateTransRequest inTransReq = new CreateTransRequest();
				inTransReq.setAccountOid(req.getInputAccountNo());
				inTransReq.setInputAccountNo(req.getInputAccountNo());
				inTransReq.setOutpuptAccountNo(req.getOutpuptAccountNo());
				inTransReq.setRequestNo(req.getRequestNo());
				inTransReq.setAccountOrderOid(orderNo);
				inTransReq.setOrderType(req.getOrderType());
				inTransReq.setOrderNo(req.getOrderNo());
				inTransReq.setDirection("");
				inTransReq.setOrderBalance(req.getBalance());
				inTransReq.setTransTime(time);
				inTransReq.setDataSource("");
				inTransReq.setBalance(transBalanceResp.getOtherBalance());
				// 财务入账标志
				inTransReq.setFinanceMark("");
				CreateTransResponse inTransResp = this.addAccountTrans(inTransReq);

				// 转入账户
				if (inTransResp.getReturnCode().equals(Constant.SUCCESS)
						&& outTransResp.getReturnCode().equals(Constant.SUCCESS)) {
					resp.setReturnCode(Constant.SUCCESS);
					resp.setErrorMessage("成功");
				} else {
					resp.setReturnCode(inTransResp.getReturnCode());
					resp.setErrorMessage(inTransResp.getErrorMessage());
				}
			}
		} catch (Exception e) {
			log.error("账户交易失败，失败原因:", e);
			throw new GHException(Integer.parseInt(Constant.FAIL), resp.getErrorMessage());
		}
		return resp;
	}

	/**
	 * 平台、投资人入账，新增交易流水 @Title: addAccountTrans @Description: @param @param
	 * orderNo @param @param req @param @return @return
	 * CreateTransResponse @throws
	 */
	@Transactional
	public CreateTransResponse addAccountTrans(String orderNo, EnterAccountRequest req) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		CreateTransResponse resp = new CreateTransResponse();
		resp.setReturnCode(Constant.SUCCESS);
		try {
			AccountInfoEntity account = accountInfoDao.findByAccountNo(req.getInputAccountNo());
			if (account != null) {
				TransBalanceResponse relustResp = new TransBalanceResponse();
				// 更新账户余额
				relustResp = this.updateBalance(req.getBalance(), req.getOrderType(), req.getInputAccountNo(), "");
				if (!relustResp.getReturnCode().equals(Constant.SUCCESS)) {
					resp.setReturnCode(relustResp.getReturnCode());
					resp.setErrorMessage(relustResp.getErrorMessage());
					// 把订单状态改为失败
					AccOrderEntity order = orderServive.getOrderByNo(orderNo);
					order.setOrderStatus(AccOrderEntity.ORDERSTATUS_INIT);
					return resp;
				}

				/**
				 * 交易流水
				 */
				CreateTransRequest transReq = new CreateTransRequest();
				transReq.setAccountOid(req.getInputAccountNo());
				transReq.setRequestNo(req.getRequestNo());
				transReq.setAccountOrderOid(orderNo);
				transReq.setOrderType(req.getOrderType());
				transReq.setOrderNo(req.getOrderNo());
				transReq.setInputAccountNo(req.getInputAccountNo());
				transReq.setDirection("");
				transReq.setOrderBalance(req.getBalance());
				transReq.setTransTime(time);
				transReq.setDataSource("");
				transReq.setBalance(relustResp.getBalance());
				// 财务入账标志
				transReq.setFinanceMark("");
				CreateTransResponse transResp = this.addAccountTrans(transReq);
				if (transResp.getReturnCode().equals(Constant.SUCCESS)) {
					resp.setReturnCode(Constant.SUCCESS);
					resp.setErrorMessage("成功");
				} else {
					resp.setReturnCode(transResp.getReturnCode());
					resp.setErrorMessage(transResp.getErrorMessage());
				}
			}
		} catch (Exception e) {
			log.error("账户交易失败，失败原因:", e);
			throw new GHException(Integer.parseInt(Constant.FAIL), resp.getErrorMessage());
		}
		return resp;
	}

	/**
	 * 新增交易流水 @Title: addAccountTrans @Description: @param @param
	 * req @param @return @return CreateTransResponse @throws
	 */
	@Transactional
	public CreateTransResponse addAccountTrans(CreateTransRequest req) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		CreateTransResponse resp = new CreateTransResponse();
		resp.setReturnCode(Constant.SUCCESS);
		try {
			TransEntity entity = new TransEntity();
			entity.setAccountOid(req.getAccountOid());
			entity.setUserOid(req.getUserOid());
			entity.setUserType(req.getUserType());
			entity.setRequestNo(req.getRequestNo());
			entity.setAccountOrderOid(req.getAccountOrderOid());
			entity.setOrderType(req.getOrderType());
			entity.setSystemSource(req.getSystemSource());
			entity.setOrderNo(req.getOrderNo());
			entity.setRelationProductNo(req.getRelationProductNo());
			entity.setDirection(req.getDirection());
			entity.setOrderBalance(req.getOrderBalance());
			entity.setRamark(req.getRamark());
			entity.setOrderDesc(req.getOrderDesc());
			entity.setAccountName(req.getAccountName());
			entity.setTransTime(time);
			entity.setDataSource(req.getDataSource());
			entity.setBalance(req.getBalance());
			entity.setIsDelete(req.getIsDelete());
			entity.setCurrency(req.getCurrency());
			// 入账，出账用户
			entity.setInputAccountNo(req.getInputAccountNo());
			entity.setOutpuptAccountNo(req.getOutpuptAccountNo());
			// 财务入账标志
			entity.setFinanceMark(req.getFinanceMark());
			entity.setCreateTime(time);
			entity.setUpdateTime(time);
			entity.setVoucher(req.getVoucher());
			entity.setAccountType(req.getAccountType());
			UserInfoEntity userInfoEntity = userInfoDao.findByUserOid(entity.getUserOid());
			if(null!=userInfoEntity){
				entity.setPhone(userInfoEntity.getPhone());
			}
			Object result = transDao.save(entity);
			if (result != null) {
				resp.setReturnCode(Constant.SUCCESS);
				resp.setErrorMessage("成功");
			}
		} catch (Exception e) {
			log.error("账户交易失败，失败原因:", e);
			throw new GHException(Integer.parseInt(Constant.FAIL), resp.getErrorMessage());
		}
		return resp;

	}

	/**
	 * 对余额进行处理 @Title: updateBalance @Description: @param @param
	 * balance @param @param type @param @param accountNo @param @return @return
	 * AccountTransResponse @throws
	 */
	@Transactional
	public TransBalanceResponse updateBalance(BigDecimal balance, String type, String accountNo,
			String otherAccountNo) {
		TransBalanceResponse resp = new TransBalanceResponse();
		resp.setReturnCode(Constant.SUCCESS);
		log.info("order money: " + balance);
		BigDecimal dr = balance.abs();
		// BigDecimal cr = dr.multiply(new BigDecimal(-1));

		AccountInfoEntity account = accountInfoDao.findByAccountNo(accountNo);
		AccountInfoEntity otherAccount = null;
		BigDecimal accOtherBalance = BigDecimal.ZERO;
		if (!StringUtil.isEmpty(otherAccountNo)) {
			otherAccount = accountInfoDao.findByAccountNo(otherAccountNo);
			accOtherBalance = otherAccount.getBalance();
		}
		BigDecimal accBalance = account.getBalance();
		BigDecimal afterBalance = BigDecimal.ZERO;
		BigDecimal afterOtherBalance = BigDecimal.ZERO;

		if (type.equals(TransEntity.TRANSFER)) {
			// 如果转账标记，先判断转出账户余额是否足够，不足报错
			if (accBalance.compareTo(dr) == -1) {
				resp.setReturnCode(Constant.BALANCELESS);
				resp.setErrorMessage("余额不足！");
				log.debug("子账户余额不足，[accountNo=" + accountNo + "]");
				return resp;
			} else {
				accountInfoDao.subtractBalance(dr, accountNo);
				afterBalance = accBalance.subtract(balance);

				accountInfoDao.addBalance(dr, otherAccountNo);
				afterOtherBalance = accOtherBalance.subtract(balance);
			}
		} else {
			if (type.equals(EnterOrderTypeEnum.ENTERADD.getCode())) {
				// 调增：20
				accountInfoDao.addBalance(dr, accountNo);
				afterBalance = accBalance.add(balance);
			} else {
				// 调减：30
				accountInfoDao.subtractBalance(dr, accountNo);
				afterBalance = accBalance.subtract(balance);
			}

		}
		resp.setBalance(afterBalance);
		resp.setOtherBalance(afterOtherBalance);
		return resp;

	}
	
	/**
	 * 判断是否超额
	 * @param balance1
	 * @param balance2
	 * @return
	 */
	private boolean isExcess(BigDecimal balance1, BigDecimal balance2) {
		boolean isExcess = false;
		if(balance1.compareTo(balance2) != -1){//-1 小于  0 等于 1 大于
			isExcess = true;
		}
		return isExcess;
	}
	
	/**
	 * 根据UserOid,orderNo查询提现冻结户、基本户是否记账
	 * 0未记账 1 提现冻结户记账 2 基本户记账
	 * @param userOid
	 * @param orderNo
	 * @return
	 */
	public String getWithdrawalsAccountStatus(String userOid, String orderNo) {
		Map<String, String> map=getAccountStatus(userOid, orderNo);
		if("1".equals(map.get("isAccount").toString())){
			if(null!=map.get(AccountTypeEnum.BASICER.getCode())){
				return Constant.BASICERACCTING;
			}
			
			if(null!=map.get(AccountTypeEnum.FROZEN.getCode())){
				return Constant.FROZENACCTING;
			}
		}
		return Constant.NOACCTING;
	}
	
	/**
	 * 根据UserOid,orderNo查询交易记录
	 * @param userOid
	 * @param orderNo
	 * @return
	 */
	public Map<String, String> getAccountStatus(String userOid, String orderNo) {
		Map<String, String> map = new HashMap<>();
		map.put("isAccount", "0");
		if (StringUtil.isEmpty(userOid) || StringUtil.isEmpty(orderNo)) {
			return map;
		}
		List<TransEntity> trans = null;
		trans = transDao.findByUserOidAndOrderNo(userOid, orderNo);
		if (!CollectionUtils.isEmpty(trans)) {
			map.put("isAccount", "1");
			for (TransEntity tran : trans) {
				AccountInfoEntity accountInfoEntity = accountInfoDao.findByAccountNo(tran.getInputAccountNo());
				map.put(accountInfoEntity.getAccountType(), accountInfoEntity.getAccountNo());
			}

		}
		log.info("根据UserOid:{},orderNo:{}查询交易记录:{}", userOid, orderNo, map.toString());
		return map;
	}
	
	

}
