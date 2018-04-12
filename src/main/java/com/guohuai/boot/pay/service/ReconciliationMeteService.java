package com.guohuai.boot.pay.service;


import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.PayEnum;
import com.guohuai.boot.pay.dao.BankHistoryDao;
import com.guohuai.boot.pay.dao.ComOrderDao;
import com.guohuai.boot.pay.dao.PaymentDao;
import com.guohuai.boot.pay.vo.BankHistoryVo;
import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.boot.pay.vo.PaymentVo;
import com.guohuai.component.util.TradeTypeEnum;

@Service
public class ReconciliationMeteService {
	private final static Logger log = LoggerFactory.getLogger(ReconciliationMeteService.class);
	@Autowired
	private BankHistoryDao bankHistoryDao;
	@Autowired
	private PaymentDao paymentDao;
	@Autowired
	private ComOrderDao comOrderDao;

	public void reconciliation() {
		// ---以指令表为基础比较快付通表数据(查询没有对过帐的数据)
		List<PaymentVo> payLists = paymentDao.findByDiffMeta(TradeTypeEnum.trade_payee.getCode());
		List<PaymentVo> mentNos = paymentDao.findByNo(TradeTypeEnum.trade_payee.getCode());
		List<BankHistoryVo> reconNos = bankHistoryDao.findByNo();
		List<OrderVo> orderNos = comOrderDao.findByNos(TradeTypeEnum.trade_payee.getCode());

		if (payLists.isEmpty() && (!mentNos.isEmpty())) {
			nii(mentNos, reconNos, orderNos);
		} else if (!payLists.isEmpty()) {
			nii(mentNos, reconNos, orderNos, payLists);
		}
	}

	public void nii(List<PaymentVo> mentNos, List<BankHistoryVo> reconNos, List<OrderVo> orderNos) {
		Timestamp time = new Timestamp(System.currentTimeMillis());

		// --多单（补单）
		log.info("平安赎回,多单,{}条,开始对账",mentNos.size());
		for (PaymentVo vo : mentNos) {
			for (OrderVo orderVo : orderNos) {
				if (vo.getOrderNo().trim().equals(orderVo.getOrderNo().trim())) {
					orderVo.setReconStatus(Integer.valueOf(PayEnum.RECONCILIATION3.getCode()));
					orderVo.setUpdateTime(time);
					if (orderVo.getStatus().equals(PayEnum.PAY1.getCode())) {
						//账户余额增加
					}
					orderVo.setStatus(PayEnum.PAY4.getCode());
				}
			}
			vo.setReconStatus(Integer.valueOf(PayEnum.RECONCILIATION3.getCode()));
			vo.setUpdateTime(time);
			if (vo.getCommandStatus().equals(PayEnum.PAY1.getCode())) {
				//账户余额增加
			}
			vo.setCommandStatus(PayEnum.PAY4.getCode());
		}
		paymentDao.save(mentNos);
		comOrderDao.save(orderNos);
	}

	public void nii(List<PaymentVo> mentNos, List<BankHistoryVo> reconNos, List<OrderVo> orderNos,
			List<PaymentVo> payLists) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		Map<String, PaymentVo> maps = new HashMap<String, PaymentVo>();

		// ---修改公共的数据状态
		log.info("平安赎回,公共单,{}条,开始对账",payLists.size());
		for (PaymentVo vo : payLists) {
			for (BankHistoryVo recon : reconNos) {
				if (vo.getPayNo().trim().equals(recon.getTransactionFlow().trim())) {
					if (!vo.getCommandStatus().trim().equals(recon.getTradStatus().trim())) {
						vo.setReconStatus(Integer.valueOf(PayEnum.RECONCILIATION2.getCode()));
						recon.setReconStatus(Integer.valueOf(PayEnum.RECONCILIATION2.getCode()));
						if (vo.getCommandStatus().trim().equals(PayEnum.PAY1.getCode())) {
							//账户余额增加
							
						} else if (recon.getTradStatus().trim().equals(PayEnum.PAY1.getCode())) {
							// 账户余额减少
							//除成功、处理中、未处理其他都需要重发赎回
						}
						String status = StringUtil.isEmpty(PayEnum.getName(recon.getTradStatus().trim()))
								? PayEnum.PAY4.getCode() : recon.getTradStatus().trim();
						vo.setCommandStatus(status);
						if(vo.getCommandStatus().equals(PayEnum.PAY2.getCode())){
							vo.setCommandStatus(PayEnum.PAY4.getCode());
						}
						
					} else {
						vo.setReconStatus(Integer.valueOf(PayEnum.RECONCILIATION1.getCode()));
						recon.setReconStatus(Integer.valueOf(PayEnum.RECONCILIATION1.getCode()));
					}
					recon.setUpdateTime(time);
					vo.setUpdateTime(time);
					maps.put(vo.getOrderNo(), vo);
					break;
				}
			}
		}

		// --多单(补单)
		if (mentNos.size() > reconNos.size()) {
			log.info("平安赎回,公共单,多单,开始对账");
			for (PaymentVo vo : mentNos) {
				if (!maps.containsKey(vo.getOrderNo())) {
					if (vo.getCommandStatus().equals(PayEnum.PAY1.getCode())) {
						vo.setCommandStatus(PayEnum.PAY4.getCode());
						// 账户余额增加
						//补单
					}
					vo.setReconStatus(Integer.valueOf(PayEnum.RECONCILIATION3.getCode()));
					vo.setUpdateTime(time);
					payLists.add(vo);
				}
			}

			for (PaymentVo vo : payLists) {
				for (OrderVo orderVo : orderNos) {
					if (orderVo.getOrderNo().trim().equals(vo.getOrderNo().trim())) {
						orderVo.setStatus(vo.getCommandStatus());
						orderVo.setReconStatus(vo.getReconStatus());
						orderVo.setUpdateTime(time);
						break;
					}
				}
			}

			comOrderDao.save(orderNos);
			paymentDao.save(payLists);
			bankHistoryDao.save(reconNos);
		}
	}
	
}