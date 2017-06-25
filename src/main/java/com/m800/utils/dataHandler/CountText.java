package com.m800.utils.dataHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CountText {
	
	public static Integer getTotalText(List<String> lines){
		int totalText = 0;
		for(String l : lines){
			String[] words = l.split("\\W");
			totalText = totalText + words.length;
		}
		return (Integer) totalText;
	}
	
	public static Map<String, Integer> countText(List<String> lines){
		Map<String, Integer> wordCount = new HashMap<>();
		
		lines.forEach(l -> {
			for(String word : l.split("\\W")){
				if(wordCount.get(word) != null){
					wordCount.put(word, ((int) wordCount.get(word)) + 1);
				} else {
					wordCount.put(word, 1);
				}
			}
		});
		
		return wordCount;
	}
}
