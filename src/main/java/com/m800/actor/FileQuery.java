package com.m800.actor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.m800.constant.FileActions;
import com.m800.utils.FileUtils;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.FiniteDuration;

public class FileQuery extends AbstractActor {
	public static final class CollectionTimeout {
		
	}
	
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	
	final Map<ActorRef, FileActions> actorToAction;
	final long requestId;
	final ActorRef requester;
	
	Cancellable queryTimeoutTimer;
	
	public FileQuery(
			Map<ActorRef, FileActions> actorToAction, 
			long requestId, 
			ActorRef requester, 
			FiniteDuration timeout){
		this.actorToAction = actorToAction;
		this.requestId = requestId;
		this.requester = requester;
		
		queryTimeoutTimer = getContext().getSystem().scheduler().scheduleOnce(
				timeout, getSelf(), new CollectionTimeout(), getContext().dispatcher(), getSelf());
	}
	
	public static Props props(
			Map<ActorRef, FileActions> actorToAction, 
			long requestId, 
			ActorRef requester, 
			FiniteDuration timeout){
		return Props.create(FileQuery.class, actorToAction, requestId, requester, timeout);
	}
	
	public void receivedResponse(
			ActorRef actionActor,
			FileParser.FileAction result,
			Set<ActorRef> stillWaiting,
			Map<FileActions, FileParser.FileAction> actionsSoFar){
		getContext().unwatch(actionActor);
		FileActions action = actorToAction.get(actionActor);
		
		Set<ActorRef> newStillWaiting = new HashSet<>(stillWaiting);
		newStillWaiting.remove(actionActor);
		
		Map<FileActions, FileParser.FileAction> newRepliesSoFar = new HashMap<>(actionsSoFar);
		newRepliesSoFar.put(action, result);
		if(newStillWaiting.isEmpty()) {
			requester.tell(new FileParser.ReturnAllResults(requestId, newRepliesSoFar), getSelf());
			getContext().stop(getSelf());
		} else {
			getContext().become(waitingForReplies(newRepliesSoFar, newStillWaiting));
		}
	}
	
	public Receive waitingForReplies(
			Map<FileActions, FileParser.FileAction> repliesSoFar,
			Set<ActorRef> stillWaiting){
		
		return receiveBuilder()
				.match(Aggregator.ReturnWordCount.class, r -> {
					ActorRef actionActor = getSender();
					FileParser.FileAction result = new FileParser.ResultNotAvailable();
					if(r.wordCount.isPresent()){
						result = new FileParser.WordCount(r.path, r.wordCount.get());;
					}
					receivedResponse(actionActor, result, stillWaiting, repliesSoFar);
				})
				.match(Aggregator.ReturnTotalWords.class, r -> {
					ActorRef actionActor = getSender();
					FileParser.FileAction result = new FileParser.ResultNotAvailable();
					if(r.totalWords.isPresent()){
						result = new FileParser.TotalWords(r.path, r.totalWords.get());
					}
					receivedResponse(actionActor, result, stillWaiting, repliesSoFar);
				})
				.match(Terminated.class, t -> {
					receivedResponse(t.getActor(), new FileParser.ResultNotAvailable(), stillWaiting, repliesSoFar);
				})
				.match(CollectionTimeout.class, t -> {
					Map<FileActions, FileParser.FileAction> replies = new HashMap<>(repliesSoFar);
					for (ActorRef actionActor : stillWaiting){
						FileActions action = actorToAction.get(actionActor);
						replies.put(action, new FileParser.ActionTimeOut());
					}
					requester.tell(new FileParser.ReturnAllResults(requestId, replies), getSelf());
					getContext().stop(getSelf());
				})
				.build();
	}
	
	@Override
	public void preStart(){
		for(ActorRef actionActor : actorToAction.keySet()){
			getContext().watch(actionActor);
			actionActor.tell(FileUtils.getActionMsg(actorToAction.get(actionActor), this.requestId), getSelf());
		}
	}
	
	@Override
	public void postStop(){
		queryTimeoutTimer.cancel();
	}
	
	@Override
	public Receive createReceive() {
		return waitingForReplies(new HashMap<>(), actorToAction.keySet());
	}

}
