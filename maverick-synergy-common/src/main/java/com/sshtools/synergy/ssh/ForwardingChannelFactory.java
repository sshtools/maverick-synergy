package com.sshtools.synergy.ssh;

import java.io.IOException;

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
