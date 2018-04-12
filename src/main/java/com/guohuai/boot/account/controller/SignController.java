package com.guohuai.boot.account.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.guohuai.account.api.request.CardQueryRequest;
import com.guohuai.account.api.response.CardListResponse;
import com.guohuai.boot.account.service.SignService;
import com.guohuai.boot.pay.form.ProtocolForm;
import com.guohuai.boot.pay.vo.ProtocolVo;

@RestController
@RequestMapping(value = "/account/card")
public class SignController {

	@Autowired
	private SignService signService;

//	@RequestMapping(value = "/tiedcard",method = RequestMethod.POST)
//	public TiedCardResponse tiedCard(@RequestBody TiedCardRequest req){
//		log.info("绑卡request:{}",JSONObject.toJSON(req));
//		TiedCardResponse resp = signService.tiedCard(req);
//		log.info("绑卡resp:{}",JSONObject.toJSON(resp));
//		return resp;
//	}
	
	@RequestMapping(value = "/cardlist",method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<CardListResponse> cardQueryList(CardQueryRequest req){
		CardListResponse resp = signService.cardQueryList(req);
		return new ResponseEntity<CardListResponse>(resp, HttpStatus.OK);
	}
	
//	@RequestMapping(value = "/unLockCard",method = RequestMethod.POST)
//	public @ResponseBody ResponseEntity<TiedCardResponse> unLockCard(@RequestBody TiedCardRequest req){
//		log.info("解绑request:{}",JSONObject.toJSON(req));
//		TiedCardResponse resp = signService.unLockCard(req);
//		log.info("解绑resp:{}",JSONObject.toJSON(resp));
//		return new ResponseEntity<TiedCardResponse>(resp, HttpStatus.OK);
//	}
	
	/**
	 * 获取已绑定的银行卡信息
	 * @param req 用户userOid
	 * @return 绑卡信息
	 */
	@RequestMapping(value = "/getBindCardByUserOid", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<ProtocolVo> getBindCardByUserOid(ProtocolForm req) {
		ProtocolVo vo = signService.getBindCardByUserOid(req.getUserOid());
		return new ResponseEntity<ProtocolVo>(vo, HttpStatus.OK);
	}

}
