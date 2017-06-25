package com.m800.utils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.m800.actor.Aggregator;
import com.m800.constant.FileActions;
import com.m800.constant.FileType;
import com.m800.utils.fileHandler.read.ReadTxtFile;

import akka.actor.Props;

public class FileUtils {
	public static FileType getFileType(Path p){
		return FileType.fromExt(FilenameUtils.getExtension(p.getFileName().toString()));
	}
	
	public static Props getPropForAction(Path path, FileActions fa){
		switch(fa){
		case GET_WORD_COUNT:
		case GET_TOTAL_WORDS:
			return Aggregator.props(fa, path);
		default:
			return null;
		}
	}
	
	public static Object getActionMsg(FileActions fa, long requestId){
		switch(fa){
		case GET_WORD_COUNT:
			return new Aggregator.GetWordCount(requestId);
		case GET_TOTAL_WORDS:
			return new Aggregator.GetTotalWords(requestId);
		default:
			return null;
		}
	}
	
	public static List<String> getDataFromFile(Path path){		
		List<String> lines = new ArrayList<>();
		
		switch(getFileType(path)){
		case TXT:
			lines = new ReadTxtFile(path).getTextFromFile();
			break;
		}
		return lines;
	}
}
