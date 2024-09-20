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
