package com.guohuai.component.common;

import java.io.Serializable;

import com.guohuai.basic.config.ErrorDefineConfig;

import lombok.Data;

/**
 * @ClassName: AbsEvent
 * @Description: 请求参数抽象.
 * @author xueyunlong
 * @date 2016年11月8日 下午12:29:05
 */
@Data
public abstract class AbsEvent implements Serializable {
	private static final long serialVersionUID = 2526919534077453038L;

	/** 交易类型 */
	private String transType;
	/** 事件类型 */
	private String eventType;
	/** 结果码 */
	private String returnCode;
	/** 结果描述 */
	private String errorMessage;
	
	public void setError(String errorCode) {
		this.returnCode = errorCode;
		this.errorMessage = ErrorDefineConfig.define.get(Integer.valueOf(errorCode));
	}
}