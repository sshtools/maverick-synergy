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
package com.sshtools.common.ssh;

public interface TransportProtocolSpecification {

	static final int SSH_MSG_DISCONNECT = 1;
	static final int SSH_MSG_IGNORE = 2;
	static final int SSH_MSG_UNIMPLEMENTED = 3;
	static final int SSH_MSG_DEBUG = 4;
	static final int SSH_MSG_SERVICE_REQUEST = 5;
	static final int SSH_MSG_SERVICE_ACCEPT = 6;

	static final int SSH_MSG_KEX_INIT = 20;
	static final int SSH_MSG_NEWKEYS = 21;
	
	/**
	 * Protocol state: Negotation of the protocol version
	 */
	public final static int NEGOTIATING_PROTOCOL = 1;

	/**
	 * Protocol state: The protocol is performing key exchange
	 */
	public final static int PERFORMING_KEYEXCHANGE = 2;

	/**
	 * Protocol state: The transport protocol is connected and services can be
	 * started or may already be active.
	 */
	public final static int CONNECTED = 3;

	/**
	 * Protocol state: The transport protocol has disconnected.
	 * 
	 * @see #getLastError()
	 */
	public final static int DISCONNECTED = 4;
	
	/** Disconnect reason: The host is not allowed */
	public final static int HOST_NOT_ALLOWED = 1;

	/** Disconnect reason: A protocol error occurred */
	public final static int PROTOCOL_ERROR = 2;

	/** Disconnect reason: Key exchange failed */
	public final static int KEY_EXCHANGE_FAILED = 3;

	/** Disconnect reason: Reserved */
	public final static int RESERVED = 4;

	/** Disconnect reason: An error occurred verifying the MAC */
	public final static int MAC_ERROR = 5;

	/** Disconnect reason: A compression error occurred */
	public final static int COMPRESSION_ERROR = 6;

	/** Disconnect reason: The requested service is not available */
	public final static int SERVICE_NOT_AVAILABLE = 7;

	/** Disconnect reason: The protocol version is not supported */
	public final static int PROTOCOL_VERSION_NOT_SUPPORTED = 8;

	/** Disconnect reason: The host key supplied could not be verified */
	public final static int HOST_KEY_NOT_VERIFIABLE = 9;

	/** Disconnect reason: The connection was lost */
	public final static int CONNECTION_LOST = 10;

	/** Disconnect reason: The application disconnected */
	public final static int BY_APPLICATION = 11;

	/** Disconnect reason: Too many connections, try later */
	public final static int TOO_MANY_CONNECTIONS = 12;

	/** Disconnect reason: Authentication was cancelled */
	public final static int AUTH_CANCELLED_BY_USER = 13;

	/** Disconnect reason: No more authentication methods are available */
	public final static int NO_MORE_AUTH_METHODS_AVAILABLE = 14;

	/** Disconnect reason: The user's name is illegal */
	public final static int ILLEGAL_USER_NAME = 15;
}
