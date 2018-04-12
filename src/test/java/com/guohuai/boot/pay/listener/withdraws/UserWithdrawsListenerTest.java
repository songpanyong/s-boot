package com.guohuai.boot.pay.listener.withdraws;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;

/**   
 * @Description: 用户提现监听测试 
 * @author ZJ   
 * @date 2018年1月17日 下午2:30:31 
 * @version V1.0   
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class UserWithdrawsListenerTest {
	@Autowired
	private ApplicationEventPublisher publisher;
	
	/**
	 * 提现
	 */
	@Test
	@Ignore
	public void testWithdraws() {
		this.publisher.publishEvent(null);
	}
}