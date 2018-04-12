package com.guohuai.boot.account.controller;

import java.math.BigDecimal;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.guohuai.SettlementBoot;
import com.guohuai.account.api.request.AccountTransRequest;
import com.guohuai.account.api.response.AccountTransResponse;

import lombok.extern.slf4j.Slf4j;

/**   
 * @Description: 交易控制器测试 
 * @author ZJ   
 * @date 2018年1月23日 下午2:29:28 
 * @version V1.0   
 */
@SuppressWarnings("deprecation")
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SettlementBoot.class)
@WebIntegrationTest("server.port:8883")
public class TransControllerTest {
	@Autowired
	private TransController transController;

	/**
	 * 产品户放款、收款测试
	 */
	@Test
	@Ignore
	public void testProductAccountTrans() {
		AccountTransRequest req = new AccountTransRequest();
		req.setUserOid("7c74cab2d43042d8a0a17582ecb22f74");
		req.setBalance(new BigDecimal("10"));
		req.setUserType("T2");
		req.setOrderType("58");
		req.setOrderNo("12345678903");
		AccountTransResponse result = this.transController.productAccountTrans(req);
		log.info("产品户放款、收款测试结果：" + result.getErrorMessage());
	}
}