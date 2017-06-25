package com.m800.exception;

import java.io.IOException;

public class ConversionException extends IOException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ConversionException(){
		super();
	}

	public ConversionException(String msg){
		super(msg);
	}
}
