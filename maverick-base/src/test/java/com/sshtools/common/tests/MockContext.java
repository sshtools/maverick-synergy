package com.sshtools.common.tests;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.util.ByteBufferPool;

public class MockContext implements Context {

	Map<Class<?>, Object> policy = new HashMap<>();
	ByteBufferPool bbp = new ByteBufferPool();
	ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        }
    });
	
	@SuppressWarnings("unchecked")
	@Override
	public <P> P getPolicy(Class<P> clz) {
		try {
			if(!policy.containsKey(clz)) {
				policy.put(clz, clz.newInstance());
			}
			return (P) policy.get(clz);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public <P> void setPolicy(Class<P> clz, P p) {
		policy.put(clz, policy);
	}

	@Override
	public boolean hasPolicy(Class<?> clz) {
		return policy.containsKey(clz);
	}

	@Override
	public ExecutorService getExecutorService() {
		return executor;
	}

	@Override
	public int getMaximumPacketLength() {
		return 65536;
	}

	@Override
	public ByteBufferPool getByteBufferPool() {
		return bbp;
	}

	@Override
	public ComponentManager getComponentManager() {
		return JCEComponentManager.getDefaultInstance();
	}

}
