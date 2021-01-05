/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
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
