/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.common.ssh;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.sshtools.common.logger.Log;

public abstract class ExecutorOperationSupport<T extends ExecutorServiceProvider> {

	public static final Integer MESSAGES_INCOMING = ExecutorOperationQueues.generateUniqueQueue("ExecutorOperationSupport.in");
	public static final Integer MESSAGES_OUTGOING = ExecutorOperationQueues.generateUniqueQueue("ExecutorOperationSupport.out");
	public static final Integer EVENTS = ExecutorOperationQueues.generateUniqueQueue("ExecutorOperationSupport.events");
	public static final Integer CALLBACKS = ExecutorOperationQueues.generateUniqueQueue("ExecutorOperationSupport.callbacks");
	
	boolean shutdown = false;
	String queueName;
	
	ConcurrentHashMap<Integer,OperationTask> operationQueues = new ConcurrentHashMap<Integer,OperationTask>();
	
	protected ExecutorOperationSupport(String queueName) {
		this.queueName = queueName;
	}
	
	public abstract T getContext();

	public void addOutgoingTask(ConnectionAwareTask r) {
		addTask(MESSAGES_OUTGOING, r);
	}
	
	public void addIncomingTask(ConnectionAwareTask r) {
		addTask(MESSAGES_INCOMING, r);
	}
	
	public void addTask(Integer queue, ConnectionAwareTask r) {
		if(!operationQueues.containsKey(queue)) {
			operationQueues.put(queue, new OperationTask());
		}
		operationQueues.get(queue).addTask(r);
	}
	
	public void cleanupOperations(ConnectionAwareTask doCleanup) {
		for(OperationTask task : operationQueues.values()) {
			if(task.running) {
				task.cleanupOperations();
			}
		}
		addTask(ExecutorOperationSupport.EVENTS, doCleanup);
	}

	class OperationTask implements Runnable {

		boolean running = false;
		Future<?> operationFuture = null;
		LinkedList<Runnable> subsystemOperations = new LinkedList<Runnable>();

		public void run() {

			if(Log.isTraceEnabled()) {
				Log.trace("{}: Operation task is starting", queueName);
			}

			do {

				executeAllTasks();

				if(Log.isTraceEnabled()) {
					Log.trace("{}: No more tasks, will wait for a few more seconds before completing task", queueName);
				}

				synchronized (this) {
					running = !subsystemOperations.isEmpty();
				}

			} while (running);

			if(Log.isTraceEnabled()) {
				Log.trace("{}: Operation task has ended");
			}
		}

		public synchronized void addTask(Runnable r) {

			subsystemOperations.addLast(r);
			
			if (!running) {
				running = true;
				
				if(Log.isTraceEnabled()) {
					Log.trace("{}: Starting new subsystem task", queueName);
				}
				operationFuture = getContext().getExecutorService().submit(this);
			} else {
				notifyAll();
			}
		}

		private void executeAllTasks() {
			while (!subsystemOperations.isEmpty()) {
				try {
					Runnable r = null;

					synchronized (this) {
						r = subsystemOperations.removeFirst();
					}
					if (r != null) {
						try {
							r.run();
						} catch (Throwable t) {
							t.printStackTrace();
							Log.error("{}: Caught exception in operation remainingTasks={}", queueName, subsystemOperations.size(), t);
						} 
					} else {
						if(Log.isWarnEnabled()) {
							Log.warn("{}: Unexpected null task in operation queue", queueName);
						}
					}
				} catch (Throwable t) {
					t.printStackTrace();
					Log.error("{}: Caught exception in operation remainingTasks={}", queueName, subsystemOperations.size(), t);
				}
			}

		}
		
		protected synchronized void cleanupOperations() {

			if (!shutdown) {

				shutdown = true;

				if(Log.isTraceEnabled()) {
					Log.trace("{}: Submitting clean up operation to executor service", queueName);
				}

				ExecutorService executorService = getContext().getExecutorService();
				if(!executorService.isShutdown()) {
					executorService.submit(new Runnable() {
						public void run() {
							if (operationFuture != null) {
					
								if(Log.isTraceEnabled()) {
									Log.trace("{}: Cleaning up operations", queueName);
								}
								
								try {
									if(Log.isTraceEnabled()) {
										Log.trace("{}: Waiting for operations to complete", queueName);
									}
									operationFuture.get();
									if(Log.isTraceEnabled()) {
										Log.trace("{}: All operations have completed", queueName);
									}
	
								} catch (InterruptedException e) {
								} catch (ExecutionException e) {
								}
							}
						}
					});
				}
			}
		}
	}

}
