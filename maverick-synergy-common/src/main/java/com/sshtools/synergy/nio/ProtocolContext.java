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

import java.io.IOException;
import java.util.Map;

import com.sshtools.common.net.ProxyType;

/**
 * A protocol context defines the behavior for a listening interface.
 */
public abstract class ProtocolContext {

    protected boolean keepAlive = false;
    protected boolean tcpNoDelay = false;
    protected boolean reuseAddress = true;
    protected int receiveBufferSize = 0;
    protected int sendBufferSize = 0;
    
	private String proxyHostname;
	private int proxyPort;
	private String proxyUsername;
	private String proxyPassword;
	private boolean resolveLocally;
	private String userAgent;
	private Map<String,String> optionalHeaders;
	private ProxyType proxyType = ProxyType.NONE;
	
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
	

	public void enableSocks4Proxy(String proxyHostname, int proxyPort, String proxyUsername) {
		this.proxyType = ProxyType.SOCKS4;
		this.proxyHostname = proxyHostname;
		this.proxyPort = proxyPort;
		this.proxyUsername = proxyUsername;
	}
	
	public void enableSocks5Proxy(String proxyHostname, int proxyPort, 
			String proxyUsername, String proxyPassword, boolean localLookup) {

		this.proxyType = ProxyType.SOCKS5;
		this.proxyHostname = proxyHostname;
		this.proxyPort = proxyPort;
		this.proxyUsername = proxyUsername;
		this.proxyPassword = proxyPassword;
		this.resolveLocally = localLookup;
	}
	
	public void enableHTTPProxy(String proxyHostname, int proxyPort) {
		enableHTTPProxy(proxyHostname, proxyPort, null, null, null, null);
	}
	
	public void enableHTTPProxy(String proxyHostname, int proxyPort,
			String proxyUsername, String proxyPassword) {
		enableHTTPProxy(proxyHostname, proxyPort, proxyUsername, proxyPassword, null, null);
	}
	
	public void enableHTTPProxy(String proxyHostname, int proxyPort,
			String proxyUsername, String proxyPassword, String userAgent) {
		enableHTTPProxy(proxyHostname, proxyPort, proxyUsername, proxyPassword, userAgent, null);
	}
	
	public void enableHTTPProxy(String proxyHostname, int proxyPort,
	        String proxyUsername, String proxyPassword, 
	        String userAgent, Map<String,String> optionalHeaders ) {
			
			this.proxyType = ProxyType.HTTP;
			this.proxyHostname = proxyHostname;
			this.proxyPort = proxyPort;
			this.proxyUsername = proxyUsername;
			this.proxyPassword = proxyPassword;
			this.userAgent = userAgent;
			this.optionalHeaders = optionalHeaders;
			
		}
		
		public boolean isProxyEnabled() {
			return proxyType != ProxyType.NONE;
		}

		public String getProxyHostname() {
			return proxyHostname;
		}

		public int getProxyPort() {
			return proxyPort;
		}

		public String getProxyUsername() {
			return proxyUsername;
		}

		public String getProxyPassword() {
			return proxyPassword;
		}

		public boolean isResolveLocally() {
			return resolveLocally;
		}
		
		public String getUserAgent() {
			return userAgent;
		}

		public Map<String, String> getOptionalHeaders() {
			return optionalHeaders;
		}

		public ProxyType getProxyType() {
			return proxyType;
		}

}
