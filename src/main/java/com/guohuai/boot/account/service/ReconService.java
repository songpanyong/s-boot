package com.guohuai.boot.account.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.guohuai.boot.account.dao.ReconDao;

@Service
@SuppressWarnings("unused")
public class ReconService {
	private final static Logger log = LoggerFactory.getLogger(ReconService.class);
	@Autowired
	private ReconDao reconDao;
}