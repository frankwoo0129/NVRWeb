package com.foxconn.util;

public class HttpServletException extends Exception {
	
	private final int code;
	
	public HttpServletException(int code) {
		super();
		this.code = code;
	}

	public HttpServletException(int code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public HttpServletException(int code, String message) {
		super(message);
		this.code = code;
	}

	public HttpServletException(int code, Throwable cause) {
		super(cause);
		this.code = code;
	}
	
	public int getStatus() {
		return this.code;
	}
}
