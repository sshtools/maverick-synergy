/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.ssh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.sshtools.common.logger.Log;

public abstract class ExecutorOperationSupport<T extends ExecutorServiceProvider> {

	

	public static final Integer MESSAGES_INCOMING = 0x01;
	public static final Integer MESSAGES_OUTGOING = 0x02;
	public static final Integer EVENTS = 0x04;
	public static final Integer CALLBACKS = 0x08;
	
	boolean shutdown = false;
	String queueName;
	
	Map<Integer,OperationTask> operationQueues = new HashMap<Integer,OperationTask>();
	List<ExecutorOperationListener> listeners = new ArrayList<ExecutorOperationListener>();
	
	protected ExecutorOperationSupport(String queueName) {
		this.queueName = queueName;
	}
	
	public abstract T getContext();

	public synchronized void addOperationListener(ExecutorOperationListener listener) {
		listeners.add(listener);
	}
	
	public synchronized void removeOperationListener(ExecutorOperationListener listener) {
		listeners.remove(listener);
	}
	
	public void addOutgoingTask(Runnable r) {
		addTask(MESSAGES_OUTGOING, r);
	}
	
	public void addIncomingTask(Runnable r) {
		addTask(MESSAGES_INCOMING, r);
	}
	
	public void addTask(Integer queue, Runnable r) {
		if(!operationQueues.containsKey(queue)) {
			operationQueues.put(queue, new OperationTask());
		}
		operationQueues.get(queue).addTask(r);
	}
	
	public void cleanupOperations(Runnable doCleanup) {
		for(OperationTask task : operationQueues.values()) {
			if(task.running) {
				task.cleanupOperations();
			}
		}
		addTask(ExecutorOperationSupport.EVENTS, doCleanup);
	}

	public int getOperationsCount() {
		int count = 0;
		for(OperationTask task : operationQueues.values()) {
			count += task.subsystemOperations.size();
		}
		return count;
	}
	
	class OperationTask implements Runnable {

		boolean running = false;
		Future<?> operationFuture = null;
		LinkedList<Runnable> subsystemOperations = new LinkedList<Runnable>();

		public void run() {

			if(Log.isTraceEnabled()) {
				Log.trace(queueName + ": Operation task is starting");
			}

			do {

				executeAllTasks();

				if(Log.isTraceEnabled()) {
					Log.trace(queueName + ": No more tasks, will wait for a few more seconds before completing task");
				}

				synchronized (this) {
					try {
						wait(1000);
					} catch (InterruptedException e) {
					}
					running = !subsystemOperations.isEmpty();
				}

			} while (running);

			if(Log.isTraceEnabled()) {
				Log.trace(queueName + ": Operation task has ended");
			}
		}

		public synchronized void addTask(Runnable r) {

			subsystemOperations.addLast(r);

			addedTask(r);
			
			if (!running) {
				running = true;
				
				if(Log.isTraceEnabled()) {
					Log.trace(queueName + ": Starting new subsystem task");
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
							startTask(r);
							r.run();
						} catch (Throwable t) {
							t.printStackTrace();
							Log.error("Caught exception in operation remainingTasks=" + subsystemOperations.size(), t);
						} finally {
							completedTask(r);
						}
						
					} else {
						if(Log.isWarnEnabled()) {
							Log.warn(queueName + ": Unexpected null task in operation queue");
						}
					}
				} catch (Throwable t) {
					t.printStackTrace();
					Log.error(queueName + ": Caught exception in operation remainingTasks=" + subsystemOperations.size(), t);
				}
			}

		}
		
		protected synchronized void cleanupOperations() {

			if (!shutdown) {

				if(Log.isTraceEnabled()) {
					Log.trace(queueName + ": Submitting clean up operation to executor service");
				}

				getContext().getExecutorService().submit(new Runnable() {
					public void run() {
						if (operationFuture != null) {
				
							if(Log.isTraceEnabled()) {
								Log.trace(queueName + ": Cleaning up operations");
							}
							
							try {
								if(Log.isTraceEnabled()) {
									Log.trace(queueName + ": Waiting for operations to complete");
								}
								operationFuture.get();
								if(Log.isTraceEnabled()) {
									Log.trace(queueName + ": All operations have completed");
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
	
	protected synchronized void addedTask(Runnable r) {
		for(ExecutorOperationListener l : getContext().getExecutorListeners()) {
			try {
				l.addedTask(r);
			} catch (Throwable t) {
			}
		}
		for(ExecutorOperationListener l : listeners) {
			try {
				l.addedTask(r);
			} catch (Throwable t) {
			}
		}
	}
	
	protected synchronized void startTask(Runnable r) {
		
		if(Log.isTraceEnabled()) {
			Log.trace(String.format("Executing task on thread %s", Thread.currentThread().getName()));
		}
		
		for(ExecutorOperationListener l : getContext().getExecutorListeners()) {
			try {
				l.startedTask(r);
			} catch (Throwable t) {
			}
		}
		for(ExecutorOperationListener l : listeners) {
			try {
				l.startedTask(r);
			} catch (Throwable t) {
			}
		}
	}
	
	protected synchronized void completedTask(Runnable r) {
		for(ExecutorOperationListener l : getContext().getExecutorListeners()) {
			try {
				l.completedTask(r);
			} catch (Throwable t) {
			}
		}
		for(ExecutorOperationListener l : listeners) {
			try {
				l.completedTask(r);
			} catch (Throwable t) {
			}
		}
	}
}
