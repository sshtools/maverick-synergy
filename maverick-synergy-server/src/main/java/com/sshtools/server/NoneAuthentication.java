package com.sshtools.server;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.sshtools.common.auth.AuthenticationMechanism;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.synergy.ssh.TransportProtocol;

/**
 * <p>This authentication mechanism can be used to send custom banner messages
 * to the client. When an SSH client connects it sends a 'none' authentication
 * request with the username of the connecting user, normally the Maverick SSHD
 * will send the default banner message configured in the ConfigurationContext.</p>
 *
 * <p>However you can extend this class and override the getBannerForUser method to return
 * a banner message for a specific user. To configure the SSHD to use your class instead
 * of the default behaviour add it to the supported authentication mechanisms in your
 * SshDaemon.configure method. Also make sure that no banner is configured
 * in the SshContext.</p>
 *
 * <code>
 * context.supportedAuthenticationMechanisms().add("none", MyNoneAuthentication.class);
 * </code>
 * 
 */
public class NoneAuthentication implements AuthenticationMechanism {

    TransportProtocol<SshServerContext> transport;
    AuthenticationProtocolServer auth;

    public NoneAuthentication() {
    }
    
    public NoneAuthentication(TransportProtocol<SshServerContext> transport, AuthenticationProtocolServer auth) {
    	this.transport = transport;
    	this.auth = auth;
    }

    /**
     * Return the SSH method name for this authentication.
     *
     * @return String
     * @todo Implement this com.maverick.sshd.AuthenticationMechanism method
     */
    final public String getMethod() {
        return "none";
    }

    /**
     * Initializes the mechanism with variables.
     *
     * @param transport the transport protocol
     * @param authentication the authentication protocol
     * @param sessionid the id of the current session.
     * @throws IOException
     * @todo Implement this com.maverick.sshd.AuthenticationMechanism method
     */
    final public void init(TransportProtocol<SshServerContext> transport,
                     AuthenticationProtocolServer authentication) throws
            IOException {
        this.transport = transport;
        this.auth = authentication;
    }

    /**
     * If the SSH protocol authentication method defines additional messages
     * which are sent from the client, they will be passed into your
     * implementation here when received.
     *
     * @param msg byte[]
     * @return boolean
     * @throws IOException
     * @todo Implement this com.maverick.sshd.AuthenticationMechanism method
     */
    final public boolean processMessage(byte[] msg) throws IOException {
        return false;

    }

    /**Override this method to send user customized banners, if this method is overridden then com.maverick.sshd.SshContext.setBannerMessage(null) should be set to null.
     * @param username
     * @return
     */
    public String getBannerForUser(String username) {
        return null;
    }

    /**
     * Start an authentication transaction.
     *
     * @param username String
     * @param msg the request data from the SSH_MSG_USERAUTH_REQUEST message
     * @return <tt>true</tt> if the message was processed, otherwise
     *   <tt>false</tt>
     * @throws IOException
     * @todo Implement this com.maverick.sshd.AuthenticationMechanism method
     */
    final public boolean startRequest(String username, byte[] msg) throws IOException {

    	//If this class has been overridden then there may be a banner to send for this user. 
        final String banner = getBannerForUser(username);

        if(banner!=null) {
            transport.postMessage(new SshMessage() {
                public boolean writeMessageIntoBuffer(ByteBuffer buf) {
                    buf.put((byte) AuthenticationProtocolServer.SSH_MSG_USERAUTH_BANNER);
                    buf.putInt(banner.getBytes().length);
                    buf.put(banner.getBytes());
                    buf.putInt(0);
                    return true;
                }

                public void messageSent(Long sequenceNo) {
                }
            });
        }

     auth.failedAuthentication();
     return true;

    }
}
