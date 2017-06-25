package com.m800.utils.fileHandler.read;

import java.nio.file.Path;
import java.util.List;

public abstract class ReadFileTemplate {
	private Path p;
	
	public ReadFileTemplate(Path path){
		setPath(path);
	}
	
	private void setPath(Path path){
		this.p = path;
	}
	
	public Path getPath(){
		return p;
	}
	
	public abstract List<String> getTextFromFile();
}
