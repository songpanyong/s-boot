package com.guohuai.boot.account.controller;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.guohuai.SettlementBoot;
import com.guohuai.account.api.request.CreateAccountRequest;
import com.guohuai.account.api.request.ProductAccountRequest;
import com.guohuai.account.api.response.CreateAccountResponse;
import com.guohuai.account.api.response.ProductAccountListResponse;

import lombok.extern.slf4j.Slf4j;

/**   
 * @Description: 账户信息控制器测试 
 * @author ZJ   
 * @date 2018年1月22日 下午5:20:49 
 * @version V1.0   
 */
@SuppressWarnings("deprecation")
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SettlementBoot.class)
@WebIntegrationTest("server.port:8883")
public class AccountInfoControllerTest {
	@Autowired
	private AccountInfoController accountInfoController;

	/**
	 * 创建产品户测试
	 */
	@Test
	@Ignore
	public void testCreateAccount() {
		CreateAccountRequest req = new CreateAccountRequest();
		req.setUserOid("7c74cab2d43042d8a0a17582ecb22f74");
		req.setUserType("T2");
		req.setAccountType("07");
		CreateAccountResponse result = this.accountInfoController.createProductAccount(req);
		log.info("创建产品户测试结果：" + result.getErrorMessage());
	}

	/**
	 * 查询产品户可用余额接口测试
	 */
	@Test
	@Ignore
	public void testQueryProductAccountBalance() {
		ProductAccountRequest req = new ProductAccountRequest();
		req.setUserOid("7c74cab2d43042d8a0a17582ecb22f74");
		ResponseEntity<ProductAccountListResponse> result = this.accountInfoController.queryProductAccountBalance(req);
		log.info("查询产品户可用余额接口测试结果：" + result.getBody().getErrorMessage());
	}
}