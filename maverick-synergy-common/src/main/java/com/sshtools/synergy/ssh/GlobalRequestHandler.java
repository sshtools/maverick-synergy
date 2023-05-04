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
package com.sshtools.synergy.ssh;

import java.io.IOException;

import com.sshtools.common.ssh.GlobalRequest;
import com.sshtools.common.util.ByteArrayWriter;

/**
 * The SSH protocol allows for the sending of requests independently of
 * any communication channel. One of the main uses of this mechanism is to
 * request remote forwardings, however it can be used for any purpose.
 */
public interface GlobalRequestHandler<T extends SshContext> {
	
	@SuppressWarnings("serial")
	public class GlobalRequestHandlerException extends Exception {
	}

    /**
     * Process a global request.
     *
     * @param request GlobalRequest
     * @param sessionid byte[]
     * @return response if wantsReply is true. If no response is return but wantsReply is true, this is treated as an error.
     * @throws GlobalRequestHandlerException if there is an error handling this request 
     * @throws IOException 
     */
	boolean processGlobalRequest(GlobalRequest request, ConnectionProtocol<T> connection, boolean wantsReply,
			ByteArrayWriter response) throws GlobalRequestHandlerException, IOException;
	

    /**
     * Returns an array of strings containing the supported global requests.
     * @return String[]
     */
	String[] supportedRequests();

}
