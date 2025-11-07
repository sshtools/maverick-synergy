package com.sshtools.synergy.ssh;

/*-
 * #%L
 * Common API
 * %%
 * Copyright (C) 2002 - 2025 JADAPTIVE Limited
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

import com.sshtools.common.forwarding.ForwardingRequest;
import com.sshtools.common.forwarding.ForwardingRequest.Protocol;

public interface ForwardingFactory<C extends SshContext, F extends ForwardingChannelFactory<C>> {

	@Deprecated(forRemoval = true, since = "3.2.0")
	F createChannelFactory(String hostToConnect, int portToConnect);

	default F createChannelFactory(ForwardingRequest request) {
		return createChannelFactory(request.destinationAddress(), request.destinationPort());
	}
	
	default boolean isHandled(ForwardingRequest request) {
		return request.protocol() == Protocol.TCP;
	}
}
