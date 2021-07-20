
package com.sshtools.synergy.ssh;

import com.sshtools.common.ssh.GlobalRequest;

/**
 * The SSH protocol allows for the sending of requests independently of
 * any communication channel. One of the main uses of this mechanism is to
 * request remote forwardings, however it can be used for any purpose.
 */
public interface GlobalRequestHandler<T extends SshContext> {

        /**
         * Process a global request.
         *
         * @param request GlobalRequest
         * @param sessionid byte[]
         * @return boolean
         */
        public boolean processGlobalRequest(GlobalRequest request, ConnectionProtocol<T> connection);

        /**
         * Returns an array of strings containing the supported global requests.
         * @return String[]
         */
        public String[] supportedRequests();

}
