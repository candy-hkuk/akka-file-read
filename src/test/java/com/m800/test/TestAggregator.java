package com.m800.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.m800.actor.Aggregator;
import com.m800.constant.FileActions;
import com.m800.utils.FileUtils;
import com.m800.utils.dataHandler.CountText;

import static com.m800.junit.Assert.assertEquals;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

public class TestAggregator {
	static ActorSystem system;
	static Path path = null;
	static Optional<Map<String, Integer>> wordCount = null;
	static Optional<Integer> total = null;
	
	private static void prepareWordCount(){
		wordCount = Optional.of(CountText.countText(FileUtils.getDataFromFile(path)));
		total = Optional.of(CountText.getTotalText(FileUtils.getDataFromFile(path)));
	}
	
	@BeforeClass
    public static void setup() {
        system = ActorSystem.create("sample");        
        path = Paths.get("D:\\m800\\data\\sample1.txt");
        prepareWordCount();        
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }
    
    @Test
    public void testGetWordCount(){
    	TestKit probe = new TestKit(system);
    	ActorRef aggregatorActor = system.actorOf(Aggregator.props(FileActions.GET_WORD_COUNT, path), "testGetWordCount");
    		
    	aggregatorActor.tell(new Aggregator.GetWordCount(1L), probe.getRef());
    	Aggregator.ReturnWordCount response = probe.expectMsgClass(Aggregator.ReturnWordCount.class);
    	assertEquals(1L, response.requestId);
    	assertEquals(wordCount, response.wordCount);
    	System.out.println(response.wordCount.toString());
    }
    
    @Test
    public void testGetTotalWords(){
    	TestKit probe = new TestKit(system);
    	ActorRef aggregatorActor = system.actorOf(Aggregator.props(FileActions.GET_TOTAL_WORDS, path), "testGetTotalWords");
    		
    	aggregatorActor.tell(new Aggregator.GetTotalWords(1L), probe.getRef());
    	Aggregator.ReturnTotalWords response = probe.expectMsgClass(Aggregator.ReturnTotalWords.class);
    	assertEquals(1L, response.requestId);
    	assertEquals(total, response.totalWords);
    	System.out.println(response.totalWords.toString());
    	
    }
}
