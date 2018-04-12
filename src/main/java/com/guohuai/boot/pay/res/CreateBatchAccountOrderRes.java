package com.guohuai.boot.pay.res;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.guohuai.boot.account.entity.AccOrderEntity;
import com.guohuai.settlement.api.response.BaseResponse;

/**
 * 创建账户请求参数
* @ClassName: NewUserRequest 
* @Description: 
* @author longyunbo
* @date 2016年11月8日 上午10:10:41 
*
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class CreateBatchAccountOrderRes extends BaseResponse implements Serializable{
	
	/**
	* @Fields serialVersionUID : 
	*/
	private static final long serialVersionUID = -5247704351104399272L;
	
	private List<AccOrderEntity> accOrderEntityList;
}
