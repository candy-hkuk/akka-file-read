package com.m800.constant;

public enum FileType {
	TXT ("txt");
	
	String ext;
	FileType(String ext){
		this.ext = ext;
	}
	
	public String ext(){
		return ext;
	}
	
	public static FileType fromExt(String ext){
		for(FileType f : values()){
			if(f.ext().equals(ext)){
				return f;
			}
		}
		return null;
	}
}
