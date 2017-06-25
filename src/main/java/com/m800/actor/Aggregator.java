package com.m800.actor;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import com.m800.constant.FileActions;
import com.m800.utils.FileUtils;
import com.m800.utils.dataHandler.CountText;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Aggregator extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	final FileActions fa;
	final Path path;
	
	Optional<Map<String, Integer>> wordCount = Optional.empty();
	Optional<Integer> totalWords = Optional.empty();
	
	public Aggregator(FileActions fa, Path path){
		this.fa = fa;
		this.path = path;
	}
	
	public static Props props(FileActions fa, Path path){
		return Props.create(Aggregator.class, fa, path);
	}
	
	public static final class GetWordCount {
		public long requestId;
		
		public GetWordCount(long requestId){
			this.requestId = requestId;
		}
	}
	
	public static final class ReturnWordCount {
		public long requestId;
		public Path path;
		public Optional<Map<String, Integer>> wordCount;
		
		public ReturnWordCount(long requestId, Path path, Optional<Map<String, Integer>> wordCount){
			this.requestId = requestId;
			this.path = path;
			this.wordCount = wordCount;
		}
	}
	
	public static final class GetTotalWords {
		public long requestId;
		
		public GetTotalWords(long requestId){
			this.requestId = requestId;
		}
	}
	
	public static final class ReturnTotalWords {
		public long requestId;
		public Path path;
		public Optional<Integer> totalWords;
		
		public ReturnTotalWords(long requestId, Path path, Optional<Integer> totalWords){
			this.requestId = requestId;
			this.path = path;
			this.totalWords = totalWords;
		}
	}
	
	private void countText(FileScanner.RequestActionOnFile raf){
		getSender().tell(new FileScanner.ActionRegistered(), getSelf());
	}
	
	private void getTotalWords(GetTotalWords gtw){
		this.totalWords = Optional.of(CountText.getTotalText(FileUtils.getDataFromFile(path)));
		getSender().tell(new ReturnTotalWords(gtw.requestId, path, totalWords), getSelf());
	}
	
	private void getWordCount(GetWordCount rWordCnt){
		this.wordCount = Optional.of(CountText.countText(FileUtils.getDataFromFile(path)));
		getSender().tell(new ReturnWordCount(rWordCnt.requestId, path, wordCount), getSelf());
	}

	@Override
	public void preStart() {
//		log.info("Aggregator started");
	}

	@Override
	public void postStop() {
//		log.info("Aggregator stopped");
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(FileScanner.RequestActionOnFile.class, this::countText)
				.match(GetWordCount.class, this::getWordCount)
				.match(GetTotalWords.class, this::getTotalWords)
				.build();
	}
}
