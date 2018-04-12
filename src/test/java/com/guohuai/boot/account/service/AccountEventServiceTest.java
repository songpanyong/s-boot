package com.guohuai.boot.account.service;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.guohuai.SettlementBoot;
import com.guohuai.boot.account.dto.AccountEventReqDTO;
import com.guohuai.boot.account.dto.AccountEventResDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 登账事件测试
 * @author ZJ
 * @date 2018年1月18日 下午2:24:08
 * @version V1.0
 */
@SuppressWarnings("deprecation")
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SettlementBoot.class)
@WebIntegrationTest("server.port:8883")
public class AccountEventServiceTest {
	@Autowired
	private AccountEventService accountEventService;

	/**
	 * 查询登账事件信息测试
	 */
	@Test
	@Ignore
	public void testQueryAccountEventInfo() {
		AccountEventReqDTO req = new AccountEventReqDTO();
		req.setUserOid("platform201800000001");
		req.setTransType("56");
		req.setEventType("03");
		AccountEventResDTO result = this.accountEventService.queryAccountEventInfo(req);
		log.info("查询登账事件信息测试结果：" + result.getErrorMessage());
	}
}