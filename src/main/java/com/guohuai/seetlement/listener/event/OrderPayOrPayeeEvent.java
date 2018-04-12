package com.guohuai.seetlement.listener.event;

import java.io.Serializable;

import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.boot.pay.vo.ProtocolVo;
import com.guohuai.settlement.api.request.OrderRequest;
import com.guohuai.settlement.api.response.OrderResponse;

import lombok.Data;

@Data
public class OrderPayOrPayeeEvent implements Serializable{
	private static final long serialVersionUID = -3686023843181816795L;
	
	private OrderVo orderVo;
	private ProtocolVo protocolVo;
	private OrderResponse orderResponse;
	private OrderRequest orderRequest;
	private boolean isReqTwo=false;

}
