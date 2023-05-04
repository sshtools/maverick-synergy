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
