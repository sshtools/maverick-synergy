
package com.sshtools.common.ssh;

import java.util.HashMap;
import java.util.Map;

public class ExecutorOperationQueues {

	static int nextQueueId = 0;
	static Map<String,Integer> queueNames = new HashMap<>();
	
	public static int generateUniqueQueue(String name) {
		if(queueNames.containsKey(name)) {
			throw new IllegalStateException(String.format("There is already a queue named %s", name));
		}
		queueNames.put(name, nextQueueId++);
		return queueNames.get(name);
	}
}
