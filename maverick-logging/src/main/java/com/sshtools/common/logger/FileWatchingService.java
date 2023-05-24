package com.sshtools.common.logger;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sshtools.common.util.IOUtils;

public class FileWatchingService extends Thread {
	
	private static FileWatchingService instance = null;
	
	private AtomicBoolean stop = new AtomicBoolean(false);

	Map<Path,FileWatchingCallback> paths = new HashMap<>();
	WatchService service;
	
    public FileWatchingService() throws IOException {
        setName("FileWatchingService");
        setDaemon(true);
        service = FileSystems.getDefault().newWatchService();
        Runtime.getRuntime().addShutdownHook(new Thread() {
        	public void run() {
        		stopThread();
        	}
        });
    }
    
    public static FileWatchingService getInstance() throws IOException {
    	if(Objects.isNull(instance)) {
    		instance = new FileWatchingService();
    	}
    	return instance;
    }

    public boolean isStopped() { return stop.get(); }
    public void stopThread() { 
    	IOUtils.closeStream(service);
    	stop.set(true); 
    }
    
    public void register(Path path, FileWatchingCallback callback) throws IOException {
    	paths.put(path.getParent(), callback);
    	path.register(service, java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY);
    }

    public void doOnChange(Path path) {
        FileWatchingCallback callback = paths.get(path);
        if(Objects.nonNull(callback)) {
        	callback.changed(path);
        }
    }

    @Override
    public void run() {
  
         try {
            while (!isStopped()) {
                WatchKey key;
                try { key = service.poll(25, TimeUnit.MILLISECONDS); }
                catch (InterruptedException e) { return; }
                if (key == null) { Thread.yield(); continue; }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path path = ev.context();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        Thread.yield();
                        continue;
                    } else if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY) {
                        doOnChange(path);
                    }
                    boolean valid = key.reset();
                    if (!valid) { break; }
                }
                Thread.yield();
            }
        } catch (Throwable e) {
            // Log or rethrow the error
        }
    }
}
