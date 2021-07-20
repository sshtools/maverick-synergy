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

package com.sshtools.synergy.nio;

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
