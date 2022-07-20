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

package com.sshtools.synergy.ssh;

import java.io.IOException;

import com.sshtools.synergy.ssh.SocketListeningForwardingFactoryImpl.ActiveTunnelManager;

/**
 * This interface defines the behaviour for remote forwarding requests. When an SSH client requests
 * a remote forwarding we typically open a server socket, accept connections and
 * open remote forwarding channels on the client.
 */
public interface ForwardingFactory<T extends SshContext> {

    /**
     * A client has requested that the server start listening and forward
     * any subsequent connections to the client.
     *
     * @param addressToBind String
     * @param portToBind int
     * @param connection ConnectionProtocol
     * @throws IOException
     */
    int bindInterface(String addressToBind, int portToBind, ConnectionProtocol<T> connection) throws IOException;

    /**
     * 
     * @param addressToBind
     * @param portToBind
     * @param connection
     * @param channelType
     * @throws IOException
     */
    int bindInterface(String addressToBind, int portToBind, ConnectionProtocol<?> connection, String channelType) throws IOException;
    
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
