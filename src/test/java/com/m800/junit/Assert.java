package com.m800.junit;

import java.util.Map;

import com.m800.actor.FileParser.FileAction;
import com.m800.constant.FileActions;
public class Assert extends org.junit.Assert {
	
	public static void assertEqualFileActionResults(
			Map<FileActions, FileAction> expected, 
			Map<FileActions, FileAction> actual){
		for(FileActions action: actual.keySet()){
			if(!expected.get(action).equals(actual.get(action))){
				throw new AssertionError();
			}
		}
	}
}
