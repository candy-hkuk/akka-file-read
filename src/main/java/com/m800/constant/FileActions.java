package com.m800.constant;

public enum FileActions {
	GET_WORD_COUNT ("count_words"),
	GET_TOTAL_WORDS ("total_words");
	
	String lbl;
	FileActions(String lbl){
		this.lbl = lbl;
	}
	
	public String lbl(){
		return lbl;
	}
	
	public static FileActions fromExt(String lbl){
		for(FileActions f : values()){
			if(f.lbl() == lbl){
				return f;
			}
		}
		return null;
	}
}
