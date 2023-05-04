/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
