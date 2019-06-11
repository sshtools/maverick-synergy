package com.maverick.agent.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.sshtools.common.logger.Log;

import com.maverick.agent.KeyStore;
import com.maverick.agent.exceptions.AgentNotAvailableException;
import com.maverick.agent.rfc.RFCAgentConnection;


/**
 * Implements a listener to run an agent over a socket.
 */
public class SshAgentSocketListener {
    private 
     boolean start=false;
    KeyStore keystore;
    ServerSocket server;
    int port;
    Thread thread;
    String location;

    /**
     * Creates a new SshAgentSocketListener object.
     *
     * @param location the location of the listening agent. This should be a
     *        random port on the localhost such as localhost:15342
     * @param keystore the keystore for agent operation
     *
     * @throws AgentNotAvailableException if the location specifies an invalid
     *         location
     */
    public SshAgentSocketListener(String location, KeyStore keystore)
        throws AgentNotAvailableException {
        Log.info("New SshAgent instance created");

        // Verify the agent location
        this.location = location;

        if (location == null) {
            throw new AgentNotAvailableException();
        }

        int idx = location.indexOf(":");

        if (idx == -1) {
            throw new AgentNotAvailableException();
        }

        String host = location.substring(0, idx);
        port = Integer.parseInt(location.substring(idx + 1));
        this.keystore = keystore;

        try {
            server = new ServerSocket(port, 5, InetAddress.getByName(host));
            port = server.getLocalPort();
            this.location = host + ":" + port;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the agent listeners state
     *
     * @return the current state of the listener
     */
    public boolean getState() {
        return start;
    }

    /**
     * Starts the agent listener thread
     */
    public void start() {
    	Log.info("Staring..");
        thread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            Socket socket;
                            System.setProperty("sshtools.agent", location);
                            start=true;

                            while ((socket = server.accept()) != null) {
                            	Log.info("waiting for client");
                                new RFCAgentConnection(keystore,
                                        socket.getInputStream(),
                                        socket.getOutputStream(),
                                        socket);
                            }

                            thread = null;
                        } catch (IOException ex) {
                            Log.info("The agent listener closed: " +
                                ex.getMessage());
                        } finally {
                            start=false;
                        }
                    }
                });
        thread.start();
    }

    /**
     * The current port of the agent listener
     *
     * @return the integer port
     */
    public int getPort() {
        return port;
    }

    /**
     * Stops the agent listener
     */
    public void stop() {
        try {
            server.close();
        } catch (IOException ex) {
        }
    }

    /**
     * Gets the underlying keystore for this agent listener.
     *
     * @return the keystore
     */
    protected KeyStore getKeystore() {
        return keystore;
    }

	public String getLocation() {
		return location;
	}

}
