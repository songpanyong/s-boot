package com.guohuai.boot.account.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import com.guohuai.account.api.request.AccountTransRequest;
import org.springframework.stereotype.Component;

/**
 * 适配转发
 *
 */
@Component
public class AdapterService {

	@Autowired
	private ApplicationEventPublisher event;

	public AccountTransRequest adapter(AccountTransRequest req) {
		event.publishEvent(req);
		return req;
	}
}