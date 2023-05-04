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
package com.sshtools.agent.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.sshtools.agent.KeyStore;
import com.sshtools.agent.exceptions.AgentNotAvailableException;
import com.sshtools.agent.rfc.RFCAgentConnection;
import com.sshtools.common.logger.Log;


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
