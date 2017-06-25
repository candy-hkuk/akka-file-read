package com.m800.actor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.m800.actor.FileParser;
import com.m800.constant.FileActions;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class FileScanner extends AbstractActor {	
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	final Path path;
	final Set<Path> fileList;
	final Map<Path, ActorRef> pathToActor = new HashMap<>();
	final Map<ActorRef, Path> actorToPath = new HashMap<>();
	
	public FileScanner(Path path){
		this.path = path;
		this.fileList = getAllFiles(path);
	}
	
	private Set<Path> getAllFiles(Path p){
		Set<Path> fileList = new HashSet<>();
		
		if(Files.isDirectory(p)){
			try (Stream<Path> paths = Files.walk(p)) {
			    paths
			        .filter(Files::isRegularFile)
			        .forEach(f -> {
			        	fileList.add(f);
			        });
			} catch(Exception e){
				e.printStackTrace();
			}
		} else {
			fileList.add(p);
		}
		return fileList;
	}
	
	public static Props props(Path path) {
		return Props.create(FileScanner.class, path);
	}
	
	public static final class ScanPath {
		public long requestId;
		
		public ScanPath(long requestId){
			this.requestId = requestId;
		}
	}
	
	public static final class PathScanned {
		public long requestId;
		
		public PathScanned(long requestId){
			this.requestId = requestId;
		}
	}
	
	public static final class GetChildFileList {
		public long requestId;
		
		public GetChildFileList(long requestId){
			this.requestId = requestId;
		}
	}
	
	public static final class ReturnChildFileList {
		public long requestId;
		public Set<Path> fileList;
		
		public ReturnChildFileList(long requestId, Set<Path> fileList){
			this.requestId = requestId;
			this.fileList = fileList;
		}
	}
	
	public static final class ProcessDirectory {
		public long requestId;
		
		public ProcessDirectory(long requestId){
			this.requestId = requestId;
		}
	}
	
	public static final class GetAllDataOfChildFiles {
		public long requestId;
		
		public GetAllDataOfChildFiles(long requestId){
			this.requestId = requestId;
		}
	}
	
	public static final class RequestActionOnFile {
		public Path path;
		public FileActions action;
		
		public RequestActionOnFile(Path path, FileActions action){
			this.path = path;
			this.action = action;
		}
	}
	
	public static final class ActionRegistered {
	}
	
	private void scanPath(ScanPath sp){
		getSender().tell(new PathScanned(sp.requestId), getSelf());
	}
	
	private void getChildFileList(GetChildFileList gcfl){
		getSender().tell(new ReturnChildFileList(gcfl.requestId, fileList), getSelf());
	}
	
	private void processAllChildFiles(ProcessDirectory pd){
		this.fileList.forEach(s -> {
			addActionToProcessFile(new RequestActionOnFile(s, FileActions.GET_TOTAL_WORDS));
		});
	}
	
	private void returnAllDataFromFiles(GetAllDataOfChildFiles gdcf){
		this.fileList.forEach(p -> {
			pathToActor.get(p).tell(new FileParser.RequestAllResults(gdcf.requestId), getSelf());
		});
	}
	
	private void addActionToProcessFile(RequestActionOnFile raf){		
		Path p = raf.path;
		ActorRef ref = pathToActor.get(p);
		if(ref != null){
			ref.forward(raf, getContext());
		} else {
//			log.info("Creating path actor for {}", p);
			ActorRef fileActor = getContext().actorOf(FileParser.props(p, getSelf()), "file-" + p.getFileName().toString());
			getContext().watch(fileActor);
			fileActor.forward(raf, getContext());
			pathToActor.put(p, fileActor);
			actorToPath.put(fileActor, p);
		}
	}
	
	private void onTerminated(Terminated t){
		ActorRef pathActor = t.getActor();
		Path path = actorToPath.get(pathActor);
//		log.info("Path actor for {} has been terminated", path.getFileName().toString());
		actorToPath.remove(pathActor);
		pathToActor.remove(path);
	}

	@Override
	public void preStart() {
//		log.info("FileScanner started");
	}

	@Override
	public void postStop() {
//		log.info("FileScanner stopped");
	}
  
	// No need to handle any messages
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(ScanPath.class, this::scanPath)
				.match(GetChildFileList.class, this::getChildFileList)
				.match(ReturnChildFileList.class, s -> {
//					log.info(s.fileList.toString());
				})
				.match(ProcessDirectory.class, this::processAllChildFiles)
				.match(GetAllDataOfChildFiles.class, this::returnAllDataFromFiles)
				.match(FileParser.ReturnAllResults.class, s -> {
					FileParser.TotalWords tw = (FileParser.TotalWords) s.actions.get(FileActions.GET_TOTAL_WORDS);
					String logStr = tw.path + " " +	tw.total.toString();
					log.info(logStr);
				})
				.match(RequestActionOnFile.class, this::addActionToProcessFile)
				.match(Terminated.class, this::onTerminated)
				.build();
	}
}
