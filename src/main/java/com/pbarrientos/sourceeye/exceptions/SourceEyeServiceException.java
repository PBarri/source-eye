package com.pbarrientos.sourceeye.exceptions;

public class SourceEyeServiceException extends Exception {

	private static final long serialVersionUID = 1L;

	public SourceEyeServiceException() {
	}

	public SourceEyeServiceException(String arg0) {
		super(arg0);
	}

	public SourceEyeServiceException(Throwable arg0) {
		super(arg0);
	}

	public SourceEyeServiceException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public SourceEyeServiceException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

}
