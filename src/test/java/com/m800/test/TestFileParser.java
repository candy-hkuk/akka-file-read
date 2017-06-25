package com.m800.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.m800.actor.Aggregator;
import com.m800.actor.FileParser;
import com.m800.actor.FileScanner;
import com.m800.constant.FileActions;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;

public class TestFileParser {
	static ActorSystem system;
	static Path testFile = null;
	static Set<FileActions> actions = null; 
	
	@BeforeClass
    public static void setup() {
        system = ActorSystem.create("sample");
        testFile = Paths.get("D:\\m800\\data\\sample1.txt");
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
        testFile = null;
    }
    
    @Test
    public void testReplyToRegistration(){
    	TestKit probe = new TestKit(system);
    	ActorRef fileParserActor = system.actorOf(Aggregator.props(FileActions.GET_WORD_COUNT, testFile), "testReplyToRegistration");
  		
    	fileParserActor.tell(new FileScanner.RequestActionOnFile(testFile, FileActions.GET_WORD_COUNT), probe.getRef());
    	probe.expectMsgClass(FileScanner.ActionRegistered.class);
    	assertEquals(fileParserActor, probe.getLastSender());
    }
	
	@Test
	public void testIgnoreDuplicatedActionRegistration(){
		TestKit probe = new TestKit(system);
		ActorRef fileParserActor = system.actorOf(FileParser.props(testFile, probe.getRef()), "testIgnoreDuplicatedActionRegistration");
		
		fileParserActor.tell(new FileScanner.RequestActionOnFile(testFile, FileActions.GET_TOTAL_WORDS), probe.getRef());
		probe.expectMsgClass(FileScanner.ActionRegistered.class);
		
		fileParserActor.tell(new FileScanner.RequestActionOnFile(testFile, FileActions.GET_TOTAL_WORDS), probe.getRef());
		probe.expectNoMsg();
	}
	
	@Test
	public void testIgnoreFileMismatchActionRegistration(){
		TestKit probe = new TestKit(system);
		ActorRef fileParserActor = system.actorOf(FileParser.props(testFile, probe.getRef()), "testIgnoreFileMismatchedActionRegistration");
		
		fileParserActor.tell(new FileScanner.RequestActionOnFile(testFile, FileActions.GET_TOTAL_WORDS), probe.getRef());
		probe.expectMsgClass(FileScanner.ActionRegistered.class);
		
		Path tempFile = null;
		
		try {
			tempFile = Files.createTempFile("test", "exe");
		} catch (IOException e){
			e.printStackTrace();
		}
		
		fileParserActor.tell(new FileScanner.RequestActionOnFile(tempFile, FileActions.GET_WORD_COUNT), probe.getRef());
		probe.expectNoMsg();	
	}
    
    @Test
    public void testRegisterAction(){
    	TestKit probe = new TestKit(system);
    	ActorRef fileParserActor = system.actorOf(FileParser.props(testFile, probe.getRef()), "testRegisterAction");
    	
    	fileParserActor.tell(new FileScanner.RequestActionOnFile(testFile, FileActions.GET_TOTAL_WORDS), probe.getRef());
    	probe.expectMsgClass(FileScanner.ActionRegistered.class);
    	ActorRef action1 = probe.getLastSender();
    	
    	fileParserActor.tell(new FileScanner.RequestActionOnFile(testFile, FileActions.GET_WORD_COUNT), probe.getRef());
    	probe.expectMsgClass(FileScanner.ActionRegistered.class);
    	ActorRef action2 = probe.getLastSender();
    	
    	assertNotEquals(action1, action2);
    	
    	// Check that the action actors are working
		action1.tell(new Aggregator.GetTotalWords(0L), probe.getRef());
		assertEquals(0L, probe.expectMsgClass(Aggregator.ReturnTotalWords.class).requestId);
		action2.tell(new Aggregator.GetWordCount(1L), probe.getRef());
		assertEquals(1L, probe.expectMsgClass(Aggregator.ReturnWordCount.class).requestId);
    }
    
    @Test
    public void testGetActionList(){
    	TestKit probe = new TestKit(system);
    	ActorRef fileParserActor = system.actorOf(FileParser.props(testFile, probe.getRef()), "testGetActionList");
    		
    	fileParserActor.tell(new FileScanner.RequestActionOnFile(testFile, FileActions.GET_TOTAL_WORDS), probe.getRef());
    	probe.expectMsgClass(FileScanner.ActionRegistered.class);
    	
    	fileParserActor.tell(new FileScanner.RequestActionOnFile(testFile, FileActions.GET_WORD_COUNT), probe.getRef());
    	probe.expectMsgClass(FileScanner.ActionRegistered.class);
    	    	
    	fileParserActor.tell(new FileParser.GetActionsList(0L), probe.getRef());
    	FileParser.ReturnActionsList reply = probe.expectMsgClass(FileParser.ReturnActionsList.class); 
    	assertEquals(0L, reply.requestId);
  	  	assertEquals(Stream.of(FileActions.GET_TOTAL_WORDS, FileActions.GET_WORD_COUNT).collect(Collectors.toSet()), reply.actions);
    }

	@Test
	public void testListActiveActionsAfterOneCompletes() {
		TestKit probe = new TestKit(system);
		ActorRef fileParserActor = system.actorOf(FileParser.props(testFile, probe.getRef()));

		fileParserActor.tell(new FileScanner.RequestActionOnFile(testFile, FileActions.GET_TOTAL_WORDS), probe.getRef());
		probe.expectMsgClass(FileScanner.ActionRegistered.class);
		ActorRef toComplete = probe.getLastSender();
	  
		fileParserActor.tell(new FileScanner.RequestActionOnFile(testFile, FileActions.GET_WORD_COUNT), probe.getRef());
		probe.expectMsgClass(FileScanner.ActionRegistered.class);

		fileParserActor.tell(new FileParser.GetActionsList(0L), probe.getRef());
		FileParser.ReturnActionsList reply = probe.expectMsgClass(FileParser.ReturnActionsList.class);
		assertEquals(0L, reply.requestId);
		assertEquals(Stream.of(FileActions.GET_WORD_COUNT, FileActions.GET_TOTAL_WORDS).collect(Collectors.toSet()), reply.actions);

		probe.watch(toComplete);
		toComplete.tell(PoisonPill.getInstance(), ActorRef.noSender());
		probe.expectTerminated(toComplete);

		probe.awaitAssert(() -> {
			fileParserActor.tell(new FileParser.GetActionsList(1L), probe.getRef());
			FileParser.ReturnActionsList r = 
					probe.expectMsgClass(FileParser.ReturnActionsList.class);
			assertEquals(1L, r.requestId);
			assertEquals(Stream.of(FileActions.GET_WORD_COUNT).collect(Collectors.toSet()), r.actions);
			return null;
		});
	}
}
