package com.sshtools.common.ssh;

import java.util.concurrent.ExecutorService;

import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.util.ByteBufferPool;

public interface Context {

	<P> P getPolicy(Class<P> clz);
	
	boolean hasPolicy(Class<?> clz);
	
	ExecutorService getExecutorService();

	int getMaximumPacketLength();

	ByteBufferPool getByteBufferPool();
	
	ComponentManager getComponentManager();

	void setPolicy(Class<?> clz, Object policy);

}
