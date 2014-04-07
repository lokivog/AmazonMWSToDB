package com.lokivog.mws.products;

public class InsertException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InsertException(String pMesssage) {
		super(pMesssage);
	}

	public InsertException(String message, Throwable cause) {
		super(message, cause);
	}

}
