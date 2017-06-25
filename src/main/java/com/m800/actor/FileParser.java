package com.m800.actor;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.m800.constant.FileActions;
import com.m800.utils.FileUtils;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.FiniteDuration;

public class FileParser extends AbstractActor {	
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	final ActorRef parent;
	final Path path;
	final Map<FileActions, ActorRef> actionToActor = new HashMap<>();
	final Map<ActorRef, FileActions> actorToAction = new HashMap<>();
	
	public FileParser(Path path, ActorRef parent){
		this.parent = parent;
		this.path = path;
	}
	
	public static Props props(Path path, ActorRef parent) {
		return Props.create(FileParser.class, path, parent);
	}
	
	public static final class GetActionsList {
		public long requestId;
		
		public GetActionsList(long requestId){
			this.requestId = requestId;
		}
	}
	
	public static final class ReturnActionsList {
		public long requestId;
		public Set<FileActions> actions;
		
		public ReturnActionsList(long requestId, Set<FileActions> actions){
			this.requestId = requestId;
			this.actions = actions;
		}
	}
	
	public static final class RequestAllResults {
		public long requestId;
		
		public RequestAllResults(long requestId){
			this.requestId = requestId;
		}
	}
	
	public static final class ReturnAllResults {
		public long requestId;
		public Map<FileActions, FileAction> actions;
		
		public ReturnAllResults(long requestId, Map<FileActions, FileAction> actions){
			this.requestId = requestId;
			this.actions = actions;
		}
	}
	
	public static interface FileAction {
		
	}
	
	public static final class TotalWords implements FileAction {
		public Path path;
		public Integer total;
		
		public TotalWords(Path path, Integer total){
			this.path = path;
			this.total = total;
		}
	}
	
	public static final class WordCount implements FileAction {
		public Path path;
		public Map<String, Integer> wordCount;
		
		public WordCount(Path path, Map<String, Integer> value){
			this.path = path;
			this.wordCount = value;
		}
	}
	
	public static final class ResultNotAvailable implements FileAction {
		
	}
	
	public static final class ActionNotAvailable implements FileAction {
		
	}
	
	public static final class ActionTimeOut implements FileAction {
		
	}
	
	private void registerActionOnFile(FileScanner.RequestActionOnFile rF){
		if(this.path.equals(rF.path)){
			ActorRef ref = actionToActor.get(rF.action);
			if(ref != null){
//				ref.forward(rF, getContext());
//				log.warning("Ignoring action request for {} as it already exists for {}.", rF.action.name(), this.path);
			} else {
//				log.info("Creating action actor for {}", rF.action);
				ActorRef actionActor = getContext().actorOf(FileUtils.getPropForAction(path, rF.action), rF.action.lbl());
				getContext().watch(actionActor);
				actorToAction.put(actionActor, rF.action);
				actionToActor.put(rF.action, actionActor);
				actionActor.forward(rF, getContext());
			}
		} else {
//			log.warning("Ignoring action request for {}. This actor is "
//					+ "responsible for {}.", rF.path, this.path);
		}
	}
		
	private void getActions(RequestAllResults rar){
		getContext().actorOf(FileQuery.props(
				actorToAction, 
				rar.requestId, 
				getSender(), 
				new FiniteDuration(3, TimeUnit.SECONDS)));
	}
	
	private void getActionsList(GetActionsList gwc){
//		log.info(actionToActor.toString());
		getSender().tell(new ReturnActionsList(gwc.requestId, actionToActor.keySet()), getSelf());
	}
	
	private void onTerminated(Terminated t){
		ActorRef actionActor = t.getActor();
		FileActions action = actorToAction.get(actionActor);
//		log.info("Action actor for {} has been terminated", action.lbl());
		actorToAction.remove(actionActor);
		actionToActor.remove(action);
	}

	@Override
	public void preStart() {
//		log.info("FileParser started");
	}

	@Override
	public void postStop() {
//		log.info("FileParser stopped");
	}
  
	// No need to handle any messages
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(FileScanner.RequestActionOnFile.class, this::registerActionOnFile)
				.match(GetActionsList.class, this::getActionsList)
				.match(RequestAllResults.class, this::getActions)
				.match(Terminated.class, this::onTerminated)
				.build();
	}
}
