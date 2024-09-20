package com.sshtools.common.ssh;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
			while (true) {
				try {
					Runnable r = null;

					synchronized (this) {
						if(subsystemOperations.isEmpty())
							return;
						r = subsystemOperations.removeFirst();
					}
					if (r != null) {
						try {
							r.run();
						} catch (Throwable t) {
							Log.error("{}: Caught exception in operation remainingTasks={}", queueName, subsystemOperations.size(), t);
						} 
					} else {
						if(Log.isWarnEnabled()) {
							Log.warn("{}: Unexpected null task in operation queue", queueName);
						}
					}
				} catch (Throwable t) {
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
