package com.sshtools.synergy.nio;

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

import java.nio.ByteBuffer;

import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.synergy.ssh.Connection;
import com.sshtools.synergy.ssh.SshContext;

/**
 * This interface defines the contract for a protocol implementation. An
 * instance of this interface is created for each socket connection.
 */
public interface ProtocolEngine {
	  
	/** Disconnect reason: The application disconnected */
	  public final static int BY_APPLICATION = 11;
        
	  	/**
         * The socket is connected and the protocol can now start.
         *
         * @param connection SocketConnection
         */
        public void onSocketConnect(SocketConnection connection);

        /**
         * The socket has closed.
         */
        public void onSocketClose();

        /**
         * Data has arrived on the socket.
         *
         * @param applicationData ByteBuffer
         * @return boolean to determine if protocol wants to write to the socket
         */
        public boolean onSocketRead(ByteBuffer applicationData);

        /**
         * The socket is ready for writing.
         *
         * @param applicationData ByteBuffer
         */
        public SocketWriteCallback onSocketWrite(ByteBuffer applicationData);

        /**
         * Determines whether the protocol wants to write to the socket. The
         * value of this method determines the write state of the socket. Only
         * return a true value when the protocol needs to write data to the
         * socket.
         *
         * @return boolean
         */
        public boolean wantsToWrite();

        /**
         * Is the protocol connected.
         *
         * @return boolean
         */
        public boolean isConnected();

        /**
         * Get the {@link SocketConnection} for this connection.
         *
         * @return SocketConnection
         */
        public SocketConnection getSocketConnection();
        
        /**
         * Disconnect the Engine
         * @param reason
         * @param description
         */
        public void disconnect(int reason, String description);

    	public ConnectRequestFuture getConnectFuture();
    	
		public DisconnectRequestFuture getDisconnectFuture();
        
        public ExecutorOperationSupport<?> getExecutor();

		public String getName();

		public SshContext getContext();

		public Connection<? extends SshContext> getConnection();
}
