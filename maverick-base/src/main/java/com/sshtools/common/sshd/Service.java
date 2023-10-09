package com.sshtools.common.sshd;

import java.io.IOException;

import com.sshtools.common.ssh.Context;

/**
 * A service is a protocol that operates on top of the {@link TransportProtocol}.
 * There is only one active service at anyone time and the current SSH protocol
 * defines the {@link AuthenticationProtocol} and the {@link ConnectionProtocol}.
 * 
 * @author Lee David Painter
 */
public interface Service<C extends Context> {

    /**
     * Initialize the service.
     * 
     * @param transport
     * @throws IOException
     */
    public void init(AbstractServerTransport<C> transport) throws IOException;

    /**
     * Process a transport message. When a message is received by the
     * {@link TransportProtocol} that is not a transport level message the
     * message is passed onto the active service using this method. The service
     * processes the message and returns a value to indicate whether the message
     * was used.
     * 
     * @param msg
     * @return <tt>true</tt> if the message was processed, otherwise
     *         <tt>false</tt>
     * @throws IOException
     */
    public boolean processMessage(byte[] msg) throws IOException;

    /**
     * Start the service.
     */
    public void start();

    /**
     * Stop the service
     */
    public void stop();

    /**
     * How long does the service allow idle for?
     * @return
     */
	public int getIdleTimeoutSeconds();

	/**
	 * Transport level idle
	 */
	public void idle();
}
