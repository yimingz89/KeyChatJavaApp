package org.keychat.common.exceptions;

public class UnhandledException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public UnhandledException() {
		super();
	}

	public UnhandledException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public UnhandledException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnhandledException(String message) {
		super(message);
	}

	public UnhandledException(Throwable cause) {
		super(cause);
	}


}
