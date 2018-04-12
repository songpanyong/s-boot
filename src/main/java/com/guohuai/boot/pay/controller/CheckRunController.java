package com.guohuai.boot.pay.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/settlement")
public class CheckRunController {
	@RequestMapping(value = "/check",method = { RequestMethod.POST, RequestMethod.GET })
	public String  check() {
		return "success";
	}
}
