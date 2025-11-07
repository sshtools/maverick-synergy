package com.sshtools.synergy.ssh;

/*-
 * #%L
 * Common API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
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

import java.io.IOException;

import com.sshtools.common.forwarding.ForwardingHandle;
import com.sshtools.common.forwarding.ForwardingRequest;
import com.sshtools.synergy.ssh.SocketListeningForwardingChannelFactoryImpl.ActiveTunnelManager;

/**
 * This interface defines the behaviour for remote forwarding requests. When an SSH client requests
 * a remote forwarding we typically open a server socket, accept connections and
 * open remote forwarding channels on the client.
 */
public interface ForwardingChannelFactory<T extends SshContext> {

    /**
     * A client has requested that the server start listening and forward
     * any subsequent connections to the client.
     *
     * @param addressToBind address to bind
     * @param portToBind port to bind
     * @param connection connection
     * @return port
     * @throws IOException on IO error
     */
	@Deprecated(forRemoval = true, since = "3.2.0")
    default int bindInterface(String addressToBind, int portToBind, ConnectionProtocol<T> connection) throws IOException {
		return bindInterface(addressToBind, portToBind, connection, getChannelType());
	}

    /**
     * A client has requested that the server start listening and forward
     * any subsequent connections to the client.
     * 
     * @param addressToBind address to bind
     * @param portToBind port to bind
     * @param connection connection
     * @param channelType channel type
     * @return port
     * @throws IOException on IO error
     */
	@Deprecated(forRemoval = true, since = "3.2.0")
    int bindInterface(String addressToBind, int portToBind, ConnectionProtocol<?> connection, String channelType) throws IOException;

    /**
     * A client has requested that the server start listening and forward
     * any subsequent connections to the client.
     * @param request request
     * @param channelType channel type
     * @return handle
     * 
     * @throws IOException on IO error
     */
    default ForwardingHandle bindInterface(ForwardingRequest request, ConnectionProtocol<?> connection) throws IOException {
    	return bindInterface(request, connection, getChannelType());
    }
    
    /**
     * A client has requested that the server start listening and forward
     * any subsequent connections to the client.
     * @param request request
     * @param connection connection
     * @param channelType channel type
     * @return handle
     * 
     * @throws IOException on IO error
     */
    ForwardingHandle bindInterface(ForwardingRequest request, ConnectionProtocol<?> connection, String channelType) throws IOException;
    
    /**
     * Does this factory belong to the connection provided?
     * @param connection ConnectionProtocol
     * @return boolean
     */
    boolean belongsTo(ConnectionProtocol<T> connection);

    /**
     * Stop listening on active interfaces.
     * @param dropActiveTunnels boolean
     */
    void stopListening(boolean dropActiveTunnels);
    
    
    /**
     * Get the underlying channel type for this forwarding factory.
     */
    String getChannelType();

	int getStartedEventCode();

	int getStoppedEventCode();

    ActiveTunnelManager<T> getActiveTunnelManager();

}
