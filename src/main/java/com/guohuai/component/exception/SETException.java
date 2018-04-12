package com.guohuai.component.exception;

import com.guohuai.basic.config.ErrorDefineConfig;


public class SETException extends RuntimeException {

	/**
	* @Fields serialVersionUID 
	*/
	private static final long serialVersionUID = 7242771522912099091L;
	
	private int code;

	public SETException(int code, Object... args) {
		super(String.format(ErrorDefineConfig.define.get(code), args));
		this.code = code;
	}

	public SETException(String message) {
		super(message);
		this.code = -1;
	}

	public SETException(Throwable cause) {
		super(cause);
		this.code = -1;
	}

	public SETException(String message, Throwable cause) {
		super(message, cause);
		this.code = -1;
	}

	public int getCode() {
		return this.code;
	}

	public static SETException getException(int errorCode) {
		return getException(errorCode, new Object[0]);
	}

	public static SETException getException(int errorCode, Object... args) {
		if (ErrorDefineConfig.define.containsKey(errorCode)) {
			return new SETException(errorCode, args);
		}
		return new SETException(String.valueOf(errorCode));
	}

	public static SETException getException(String errorMessage) {
		return new SETException(errorMessage);
	}

	public static SETException getException(Throwable error) {
		if (error instanceof SETException) {
			return (SETException) error;
		}
		return new SETException(error);
	}

}
