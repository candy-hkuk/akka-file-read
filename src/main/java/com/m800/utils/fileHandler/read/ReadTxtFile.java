package com.m800.utils.fileHandler.read;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.m800.exception.ConversionException;
import com.m800.utils.EncodingUtil;

public class ReadTxtFile extends ReadFileTemplate {
	
	public ReadTxtFile(Path path){
		super(path);
	}
	
	@Override
	public List<String> getTextFromFile(){
		List<String> lines = null;
		
		try (InputStream is = new BufferedInputStream(Files.newInputStream(getPath()))){
    		String charset = EncodingUtil.detectEncoding(is);
    		lines = Files.readAllLines(getPath(), Charset.forName(charset));
    		
		} catch (ConversionException e){ 
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return lines;
	}
}
