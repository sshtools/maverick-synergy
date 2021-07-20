
package com.sshtools.common.ssh;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.sshtools.common.logger.Log;

public abstract class ExecutorOperationSupport<T extends ExecutorServiceProvider> {

	public static final Integer MESSAGES_INCOMING = ExecutorOperationQueues.generateUniqueQueue("ExecutorOperationSupport.in");
	public static final Integer MESSAGES_OUTGOING = ExecutorOperationQueues.generateUniqueQueue("ExecutorOperationSupport.out");
	public static final Integer EVENTS = ExecutorOperationQueues.generateUniqueQueue("ExecutorOperationSupport.events");
	public static final Integer CALLBACKS = ExecutorOperationQueues.generateUniqueQueue("ExecutorOperationSupport.callbacks");
	
	boolean shutdown = false;
	String queueName;
	
	Map<Integer,OperationTask> operationQueues = new HashMap<Integer,OperationTask>();
	
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

				if(Log.isTraceEnabled()) {
					Log.trace("{}: Submitting clean up operation to executor service", queueName);
				}

				getContext().getExecutorService().submit(new Runnable() {
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

				shutdown = true;
			}
		}
	}

}
