package com.m800.master;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.m800.actor.FileScanner;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class Core {
	
	public static void main (String[] args) throws IOException {
		final ActorSystem system = ActorSystem.create("text-miner");
//		Path path = Paths.get("D:\\m800\\data");
		Path path = Paths.get(args[0]);
		
	    try {
	    	//#create-actors
	        final ActorRef senderActor = 
	          system.actorOf(FileScanner.props(path), "senderActor");
	        final ActorRef receiverActor = 
	        		system.actorOf(FileScanner.props(path), "receiverActor");
	        //#create-actors

	        //#main-send-messages
//	        receiverActor.tell(new FileScanner.GetChildFileList(0L), senderActor);
	        receiverActor.tell(new FileScanner.ProcessDirectory(1L), senderActor);
	        receiverActor.tell(new FileScanner.GetAllDataOfChildFiles(2), senderActor);
	        
	        //#main-send-message
	    } finally {
//	      system.terminate();
	    }
	}
}
