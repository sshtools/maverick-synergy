package com.sshtools.common.ssh;

import java.util.concurrent.ExecutorService;

import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.util.ByteBufferPool;

public interface Context {
	
	AbstractFileFactory<?> getFileFactory();

	<P> P getPolicy(Class<P> clz);
	
	<P> void setPolicy(Class<P> clz, P policy);
	
	boolean hasPolicy(Class<?> clz);
	
	ExecutorService getExecutorService();

	int getMaximumPacketLength();

	ByteBufferPool getByteBufferPool();
	
	ComponentManager getComponentManager();
}
