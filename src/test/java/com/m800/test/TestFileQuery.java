package com.m800.test;

import static com.m800.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.m800.actor.Aggregator;
import com.m800.actor.FileParser;
import com.m800.actor.FileQuery;
import com.m800.actor.FileScanner;
import com.m800.constant.FileActions;
import com.m800.utils.dataHandler.CountText;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import scala.concurrent.duration.FiniteDuration;

public class TestFileQuery {
	static ActorSystem system;
	static Path testFile = null;
	static List<String> strList = null;
	static Optional<Map<String, Integer>> wordCount = null;
	static Optional<Integer> total = null;
	
	private static void prepareStrList(){
		strList = new ArrayList<>();
        strList.add("It is recommended to create a hierarchy of children, grand-children and so on such that it fits the logical failure-handling structure of the application, see Actor Systems.");
        strList.add("");
        strList.add("The call to actorOf returns an instance of ActorRef. This is a handle to the actor instance and the only way to interact with it. The ActorRef is immutable and has a one to one relationship with the Actor it represents. The ActorRef is also serializable and network-aware. This means that you can serialize it, send it over the wire and use it on a remote host and it will still be representing the same Actor on the original node, across the network.");
        strList.add("");
        strList.add("The name parameter is optional, but you should preferably name your actors, since that is used in log messages and for identifying actors. The name must not be empty or start with $, but it may contain URL encoded characters (eg. %20 for a blank space). If the given name is already in use by another child to the same parent an InvalidActorNameException is thrown.");
        strList.add("");
        strList.add("Actors are automatically started asynchronously when created.");
	}
	
	private static void prepareWordCount(){
		wordCount = Optional.of(CountText.countText(strList));
		total = Optional.of(CountText.getTotalText(strList));
	}
	
	@BeforeClass
    public static void setup() {
        system = ActorSystem.create("sample");
        testFile = Paths.get("D:\\m800\\data\\sample1.txt");
        prepareStrList();
        prepareWordCount();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
        testFile = null;
        strList = null;
        total = null;
    }
    
    @Test
    public void testReturnResultForActiveActions(){
    	TestKit requester = new TestKit(system);
    	
    	TestKit action1 = new TestKit(system);
    	TestKit action2 = new TestKit(system);
    	
    	Map<ActorRef, FileActions> actorToAction = new HashMap<>();
    	
    	actorToAction.put(action1.getRef(), FileActions.GET_TOTAL_WORDS);
    	actorToAction.put(action2.getRef(), FileActions.GET_WORD_COUNT);
    	
    	ActorRef queryActor = system.actorOf(FileQuery.props(
    			actorToAction, 
    			1L,
    			requester.getRef(), 
    			new FiniteDuration(3, TimeUnit.SECONDS)), "testReturnResultForActiveActions");    	
    	
    	assertEquals(1L, action1.expectMsgClass(Aggregator.GetTotalWords.class).requestId);
    	assertEquals(1L, action2.expectMsgClass(Aggregator.GetWordCount.class).requestId);
    	
    	queryActor.tell(new Aggregator.ReturnTotalWords(1L, testFile, total), action1.getRef());
    	queryActor.tell(new Aggregator.ReturnWordCount(1L, testFile, wordCount), action2.getRef());
    	
    	FileParser.ReturnAllResults response = requester.expectMsgClass(FileParser.ReturnAllResults.class);
    	assertEquals(1L, response.requestId);
    	
    	Map<FileActions, FileParser.FileAction> expectedResults = new HashMap<>();
    	expectedResults.put(FileActions.GET_TOTAL_WORDS, new FileParser.TotalWords(testFile, CountText.getTotalText(strList)));
    	expectedResults.put(FileActions.GET_WORD_COUNT, new FileParser.WordCount(testFile, CountText.countText(strList)));
    	
//    	assertEqualFileActionResults(expectedResults, response.actions);
    }
    
    @Test
    public void testReturnResultNotAvailableForActionWithNoResults(){
    	TestKit requester = new TestKit(system);
    	
    	TestKit action1 = new TestKit(system);
    	TestKit action2 = new TestKit(system);
    	
    	Map<ActorRef, FileActions> actorToAction = new HashMap<>();
    	actorToAction.put(action1.getRef(), FileActions.GET_TOTAL_WORDS);
    	actorToAction.put(action2.getRef(), FileActions.GET_WORD_COUNT);
    	
    	ActorRef queryActor = system.actorOf(FileQuery.props(
    			actorToAction,
    			1L, 
    			requester.getRef(), 
    			new FiniteDuration(3, TimeUnit.SECONDS)), "testReturnResultNotAvailableForActionWithNoResults");
    	
    	assertEquals(1L, action1.expectMsgClass(Aggregator.GetTotalWords.class).requestId);
    	assertEquals(1L, action2.expectMsgClass(Aggregator.GetWordCount.class).requestId);
    	
    	queryActor.tell(new Aggregator.ReturnTotalWords(1L, testFile, Optional.empty()), action1.getRef());
    	queryActor.tell(new Aggregator.ReturnWordCount(1L, testFile, wordCount), action2.getRef());
    	
    	FileParser.ReturnAllResults response = requester.expectMsgClass(FileParser.ReturnAllResults.class);
    	assertEquals(1L, response.requestId);
    	
    	Map<FileActions, FileParser.FileAction> expectedResults = new HashMap<>();
    	expectedResults.put(FileActions.GET_TOTAL_WORDS, new FileParser.ResultNotAvailable());
    	expectedResults.put(FileActions.GET_WORD_COUNT, new FileParser.WordCount(testFile, CountText.countText(strList)));
    	
//    	assertEqualFileActionResults(expectedResults, response.actions);
    }
    
    @Test
    public void testReturnActionNotAvailableIfActionStopsBeforeAnswering(){
    	TestKit requester = new TestKit(system);
    	
    	TestKit action1 = new TestKit(system);
    	TestKit action2 = new TestKit(system);
    	
    	Map<ActorRef, FileActions> actorToAction = new HashMap<>();
    	actorToAction.put(action1.getRef(), FileActions.GET_TOTAL_WORDS);
    	actorToAction.put(action2.getRef(), FileActions.GET_WORD_COUNT);
    	
    	ActorRef queryActor = system.actorOf(FileQuery.props(
    			actorToAction, 
    			1L, 
    			requester.getRef(), 
    			new FiniteDuration(3, TimeUnit.SECONDS)), "testReturnActionNotAvailableIfActionStopsBeforeAnswering");
    	
    	assertEquals(1L, action1.expectMsgClass(Aggregator.GetTotalWords.class).requestId);
    	assertEquals(1L, action2.expectMsgClass(Aggregator.GetWordCount.class).requestId);
    	
    	queryActor.tell(new Aggregator.ReturnTotalWords(1L, testFile, total), action1.getRef());
    	action2.getRef().tell(PoisonPill.getInstance(), ActorRef.noSender());
    	
    	FileParser.ReturnAllResults response = requester.expectMsgClass(FileParser.ReturnAllResults.class);
    	assertEquals(1L, response.requestId);
    	
    	Map<FileActions, FileParser.FileAction> expectedResults = new HashMap<>();
    	expectedResults.put(FileActions.GET_TOTAL_WORDS, new FileParser.TotalWords(testFile, CountText.getTotalText(strList)));
    	expectedResults.put(FileActions.GET_WORD_COUNT, new FileParser.ActionNotAvailable());
    	
//    	assertEqualFileActionResults(expectedResults, response.actions);
    }
    
    @Test
    public void testReturnResultsEvenIfActorStopsAfterAnswering(){
    	TestKit requester = new TestKit(system);
    	
    	TestKit action1 = new TestKit(system);
    	TestKit action2 = new TestKit(system);
    	
    	Map<ActorRef, FileActions> actorToAction = new HashMap<>();
    	actorToAction.put(action1.getRef(), FileActions.GET_TOTAL_WORDS);
    	actorToAction.put(action2.getRef(), FileActions.GET_WORD_COUNT);
    	
    	ActorRef queryActor = system.actorOf(FileQuery.props(
    			actorToAction, 
    			1L, 
    			requester.getRef(), 
    			new FiniteDuration(3, TimeUnit.SECONDS)), "testReturnResultsEvenIfActorStopsAfterAnswering");
    	
    	assertEquals(1L, action1.expectMsgClass(Aggregator.GetTotalWords.class).requestId);
    	assertEquals(1L, action2.expectMsgClass(Aggregator.GetWordCount.class).requestId);
    	
    	queryActor.tell(new Aggregator.ReturnTotalWords(1L, testFile, total), action1.getRef());
    	queryActor.tell(new Aggregator.ReturnWordCount(1L, testFile, wordCount), action2.getRef());
    	action2.getRef().tell(PoisonPill.getInstance(), ActorRef.noSender());
    	
    	FileParser.ReturnAllResults response = requester.expectMsgClass(FileParser.ReturnAllResults.class);
    	assertEquals(1L, response.requestId);
    	
    	Map<FileActions, FileParser.FileAction> expectedResults = new HashMap<>();
    	expectedResults.put(FileActions.GET_TOTAL_WORDS, new FileParser.TotalWords(testFile, CountText.getTotalText(strList)));
    	expectedResults.put(FileActions.GET_WORD_COUNT, new FileParser.WordCount(testFile, CountText.countText(strList)));
    	
//    	assertEqualFileActionResults(expectedResults, response.actions);
    }
    
    @Test
    public void testReturnMsgTimedOutIfActorDoesNotAnswerInTime(){
    	TestKit requester = new TestKit(system);
    	
    	TestKit action1 = new TestKit(system);
    	TestKit action2 = new TestKit(system);
    	
    	Map<ActorRef, FileActions> actorToAction = new HashMap<>();
    	actorToAction.put(action1.getRef(), FileActions.GET_TOTAL_WORDS);
    	actorToAction.put(action2.getRef(), FileActions.GET_WORD_COUNT);
    	
    	ActorRef queryActor = system.actorOf(FileQuery.props(
    			actorToAction, 
    			1L, 
    			requester.getRef(), 
    			new FiniteDuration(3, TimeUnit.SECONDS)), "testReturnMsgTimedOutIfActorDoesNotAnswerInTime");
    	
    	assertEquals(1L, action1.expectMsgClass(Aggregator.GetTotalWords.class).requestId);
    	assertEquals(1L, action2.expectMsgClass(Aggregator.GetWordCount.class).requestId);
    	
    	queryActor.tell(new Aggregator.ReturnTotalWords(1L, testFile, total), action1.getRef());
    	
    	FileParser.ReturnAllResults response = requester.expectMsgClass(
    			FiniteDuration.create(5, TimeUnit.SECONDS),
    			FileParser.ReturnAllResults.class);
    	assertEquals(1L, response.requestId);
    	
    	Map<FileActions, FileParser.FileAction> expectedResults = new HashMap<>();
    	expectedResults.put(FileActions.GET_TOTAL_WORDS, new FileParser.TotalWords(testFile, CountText.getTotalText(strList)));
    	expectedResults.put(FileActions.GET_WORD_COUNT, new FileParser.ActionTimeOut());
    	
//    	assertEqualFileActionResults(expectedResults, response.actions);
    }
    
    @Test
    public void testCollectDataFromAllActiveActions(){
    	TestKit probe = new TestKit(system);
    	ActorRef fileParserActor = system.actorOf(FileParser.props(testFile, probe.getRef()), "testCollectDataFromAllActiveActions");
    	
    	fileParserActor.tell(new FileScanner.RequestActionOnFile(testFile, FileActions.GET_TOTAL_WORDS), probe.getRef());
    	probe.expectMsgClass(FileScanner.ActionRegistered.class);
    	ActorRef actionActor1 = probe.getLastSender();
    	
    	fileParserActor.tell(new FileScanner.RequestActionOnFile(testFile, FileActions.GET_WORD_COUNT), probe.getRef());
    	probe.expectMsgClass(FileScanner.ActionRegistered.class);
    	ActorRef actionActor2 = probe.getLastSender();
    	
    	// Check that the action actors are working
    	actionActor1.tell(new Aggregator.GetTotalWords(0L), probe.getRef());
    	assertEquals(0L, probe.expectMsgClass(Aggregator.ReturnTotalWords.class).requestId);
    	
    	fileParserActor.tell(new FileParser.RequestAllResults(0L), probe.getRef());
    	FileParser.ReturnAllResults response = probe.expectMsgClass(FileParser.ReturnAllResults.class);
    	assertEquals(0L, response.requestId);
    	
    	Map<FileActions, FileParser.FileAction> expectedResults = new HashMap<>();
    	expectedResults.put(FileActions.GET_TOTAL_WORDS, new FileParser.TotalWords(testFile, CountText.getTotalText(strList)));
    	expectedResults.put(FileActions.GET_WORD_COUNT, new FileParser.ResultNotAvailable());
    	
//    	assertEqualFileActionResults(expectedResults, response.actions);
    }
}
