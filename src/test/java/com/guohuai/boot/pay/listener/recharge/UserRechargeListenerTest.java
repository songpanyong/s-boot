package com.guohuai.boot.pay.listener.recharge;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;

/**   
 * @Description: 用户充值监听测试 
 * @author ZJ   
 * @date 2018年1月17日 下午2:29:53 
 * @version V1.0   
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class UserRechargeListenerTest {
	@Autowired
	private ApplicationEventPublisher publisher;
	
	/**
	 * 充值
	 */
	@Test
	@Ignore
	public void testRecharge() {
		this.publisher.publishEvent(null);
	}
}