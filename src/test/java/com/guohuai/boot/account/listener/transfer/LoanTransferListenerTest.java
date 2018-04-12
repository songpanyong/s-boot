package com.guohuai.boot.account.listener.transfer;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;

/**   
 * @Description: 放款转账监听测试 
 * @author ZJ   
 * @date 2018年1月17日 下午5:40:49 
 * @version V1.0   
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class LoanTransferListenerTest {
	@Autowired
	private ApplicationEventPublisher publisher;
	
	/**
	 * 转账
	 */
	@Test
	@Ignore
	public void testTransfer() {
		this.publisher.publishEvent(null);
	}
}