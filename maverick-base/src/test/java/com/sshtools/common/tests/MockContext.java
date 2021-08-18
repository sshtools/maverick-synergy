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
	public void setPolicy(Class<?> clz, Object p) {
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
