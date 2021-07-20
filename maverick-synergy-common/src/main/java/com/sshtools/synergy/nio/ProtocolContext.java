

package com.sshtools.synergy.nio;

import java.io.IOException;

/**
 * A protocol context defines the behavior for a listening interface.
 */
public abstract class ProtocolContext {

    protected boolean keepAlive = false;
    protected boolean tcpNoDelay = false;
    protected boolean reuseAddress = true;
    protected int receiveBufferSize = 0;
    protected int sendBufferSize = 0;
    private SocketConnectionFactory socketConnectionFactory = new DefaultSocketConnectionFactory();
    


    /**
     * Create a protocol engine.
     *
     * @return ProtocolEngine
     * @throws IOException
     */
    protected abstract ProtocolEngine createEngine(ConnectRequestFuture connectFuture) throws IOException;

    /**
     * Indicates whether the SO_KEEPALIVE socket option is set on connected
     * sockets.
     *
     * @return boolean
     */
    public boolean getSocketOptionKeepAlive() {
        return keepAlive;
    }


    /**
     * Indicates whether the SO_REUSEADDR socket option will be set on a server
     * socket.
     *
     * @return boolean
     */
    public boolean getSocketOptionReuseAddress() {
        return reuseAddress;
    }

    /**
     * Indicates whether the SO_REUSEADDR socket option will be set on a server
     * socket.
     *
     * @param reuseAddress boolean
     */
    public void setSocketOptionReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
  }

    /**
     * Set the SO_KEEPALIVE socket option on connected sockets.
     *
     * @param keepAlive boolean
     */
    public void setSocketOptionKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    /**
     * Indicates whether the TCP_NODELAY socket option is set on connected sockets.
     * @return boolean
     */
    public boolean getSocketOptionTcpNoDelay() {
        return tcpNoDelay;
    }

    /**
     * Set the TCP_NODELAY socket option on connected sockets.
     * @param tcpNoDelay boolean
     */
    public void setSocketOptionTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    /**
     * Set the receive buffer size for sockets.
     * @param receiveBufferSize int
     */
    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    /**
     * Set the send buffer size for sockets.
     * @param sendBufferSize int
     */
    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    /**
     * Get the socket receive buffer size.
     * @return int
     */
    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    /**
     * Get the socket send buffer size.
     * @return int
     */
    public int getSendBufferSize() {
        return sendBufferSize;
    }

	public SocketConnectionFactory getSocketConnectionFactory() {
		return socketConnectionFactory;
	}

	public void setSocketConnectionFactory(
			SocketConnectionFactory socketConnectionFactory) {
		this.socketConnectionFactory = socketConnectionFactory;
	}

	public abstract void shutdown();

}
