package com.m800.utils;

import java.io.IOException;
import java.io.InputStream;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.m800.exception.ConversionException;

public class EncodingUtil {

	public static String detectEncoding(InputStream in) throws IOException, ConversionException {
		CharsetDetector detector = new CharsetDetector();
		detector.setText(in);
		CharsetMatch charsetMatch = detector.detect();
		if (charsetMatch == null) {
		    throw new ConversionException("Cannot detect source charset.");
		}
		return charsetMatch.getName();
	}
}
