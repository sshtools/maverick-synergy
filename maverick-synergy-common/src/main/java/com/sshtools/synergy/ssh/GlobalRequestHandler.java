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

import com.sshtools.common.ssh.GlobalRequest;

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
	byte[] processGlobalRequest(GlobalRequest request, ConnectionProtocol<T> connection, boolean wantsReply) throws GlobalRequestHandlerException, IOException;
	
	/**
     * Process a global request.
     *
     * @param request GlobalRequest
     * @param sessionid byte[]
     * @return boolean
     * @see #processGlobalRequest(GlobalRequest, ConnectionProtocol, boolean)
     */
	@Deprecated
    default boolean processGlobalRequest(GlobalRequest request, ConnectionProtocol<T> connection) {
    	try {
    		processGlobalRequest(request, connection, false);
    		return true;
    	} catch(GlobalRequestHandlerException | IOException grhe) {
    		return false;
    	}
    }

 

    /**
     * Returns an array of strings containing the supported global requests.
     * @return String[]
     */
	String[] supportedRequests();

}
