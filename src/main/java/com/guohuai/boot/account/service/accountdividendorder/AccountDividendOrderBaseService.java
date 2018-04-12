package com.guohuai.boot.account.service.accountdividendorder;

import java.sql.Timestamp;
import java.util.Date;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.account.api.request.AccountTransRequest;
import com.guohuai.account.api.response.AccountTransResponse;
import com.guohuai.basic.common.DateUtil;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.account.dao.AccountDividendOrderDao;
import com.guohuai.boot.account.entity.AccountDividendOrderEntity;
import com.guohuai.boot.account.service.TransService;
import com.guohuai.component.util.AccountTypeEnum;
import com.guohuai.component.util.Constant;

import lombok.extern.slf4j.Slf4j;

/**   
 * @Description: TODO 
 * @author ZJ   
 * @date 2018年1月19日 下午6:16:01 
 * @version V1.0   
 */
@Slf4j
@Component
public class AccountDividendOrderBaseService {
	@Autowired
	private AccountDividendOrderDao orderDao;
	@Autowired
	private TransService transService;
	@Autowired
	private AccountDividendOrderDao dividendOrderDao;

	/**
	 * 新增派息收单的数据
	 */
	public AccountTransResponse addOrder(AccountTransRequest req) {
		log.info("派息收单交易:[" + JSONObject.toJSONString(req) + "]");
		AccountTransResponse resp = new AccountTransResponse();
		resp.setReturnCode(Constant.SUCCESS);
		resp.setErrorMessage("成功");
		try {
			AccountDividendOrderEntity orderEntity = new AccountDividendOrderEntity();
			BeanUtils.copyProperties(req, orderEntity);

			orderEntity.setDividendStatus(AccountDividendOrderEntity.ORDERSTATUS_INIT);
			// YYYY-MM-DD HH:mm:ss
			String orderCreateTime = req.getOrderCreatTime();
			if (orderCreateTime != null) {
				Date submitTime = DateUtil.parseDate(orderCreateTime, Constant.fomat);
				orderEntity.setSubmitTime(new Timestamp(submitTime.getTime()));
			}
			Timestamp time = new Timestamp(System.currentTimeMillis());
			orderEntity.setReceiveTime(time);
			orderEntity.setUpdateTime(time);
			orderEntity.setCreateTime(time);

			log.info("保存派息收单开始：{}", JSONObject.toJSON(orderEntity));
			orderDao.save(orderEntity);
			log.info("保存派息收单结束");

		} catch (Exception e) {
			log.error("派息收单保存失败", e);
			resp.setReturnCode(Constant.FAIL);
			resp.setErrorMessage("派息收单保存失败");
		}
		return resp;
	}


	/**
	 * 派息交易处理
	 */
	public AccountTransResponse addDividendOrder(AccountTransRequest req) {

		AccountTransResponse resp = new AccountTransResponse();
		BeanUtils.copyProperties(req, resp);

		String orderNo = req.getOrderNo();
		String accountType = "";

		// 将数据保存到派息收单表中
		AccountTransResponse orderResp = addOrder(req);
		resp.setReturnCode(orderResp.getReturnCode());
		resp.setErrorMessage(orderResp.getErrorMessage());
		if (!resp.getReturnCode().equals(Constant.SUCCESS)) {
			return resp;
		}

		// 判断用户，类型是否存在
		resp = transService.checkAccountTransRequest(req);
		if (!resp.getReturnCode().equals(Constant.SUCCESS)) {
			// 更新派息收单表的数据状态
			updateDividendOrderFailStatus(resp, orderNo);
			return resp;
		}

		/*
		 * 根据定单类别，获取用户账户类别 如：定单类别为申购活期， 账户类别应该是活期户
		 */
		if (StringUtil.isEmpty(req.getProductType())) {
			resp.setErrorMessage("产品类别不能为空");
			resp.setReturnCode(Constant.PRODUCTTYPEISNULL);
			updateDividendOrderFailStatus(resp, orderNo);
			return resp;

		}
		if (StringUtil.isEmpty(req.getRelationProductNo())) {
			resp.setErrorMessage("关联产品不能为空");
			resp.setReturnCode(Constant.RelationProductNotNULL);
			updateDividendOrderFailStatus(resp, orderNo);
			return resp;
		}
		// 如果订单类型为派息,判断是定期还是活期派息
		if (AccountTypeEnum.CURRENT.getCode().equals(req.getProductType())) {
			accountType = AccountTypeEnum.CURRENTINTEREST.getCode();
		} else if (AccountTypeEnum.REGULAR.getCode().equals(req.getProductType())) {
			accountType = AccountTypeEnum.REGULARINTEREST.getCode();
		} else {
			resp.setErrorMessage("产品类别不支持");
			resp.setReturnCode(Constant.PRODUCTTYPEISNULL);
			updateDividendOrderFailStatus(resp, orderNo);
			return resp;
		}
		// 异步处理派息
		transService.tradeDividend(req, accountType);
		return resp;
	}

	public void updateDividendOrderFailStatus(AccountTransResponse resp, String orderNo) {
		AccountDividendOrderEntity orderEntity = dividendOrderDao.findByOrderNo(orderNo);
		if (null != orderEntity) {
			orderEntity.setOrderDesc(resp.getErrorMessage());
				orderEntity.setDividendStatus(AccountDividendOrderEntity.ORDERSTATUS_SAVE_FAIL);
			dividendOrderDao.saveAndFlush(orderEntity);
		}

	}
}