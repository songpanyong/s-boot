package com.guohuai.seetlement.listener.event;

import java.io.Serializable;

import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.settlement.api.request.OrderRequest;
import com.guohuai.settlement.api.response.OrderResponse;

import lombok.Data;

@Data
public class GatewayOrderEvent implements Serializable{
	private static final long serialVersionUID = -3686023843181816793L;
	
	private OrderVo orderVo;
	private OrderResponse orderResponse;
	private OrderRequest orderRequest;

}
