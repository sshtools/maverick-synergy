package com.sshtools.common.auth;

/*-
 * #%L
 * Base API
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
import java.nio.ByteBuffer;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.TransportProtocolSpecification;
import com.sshtools.common.sshd.AbstractServerTransport;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;

/**
 * Provides an {@link AuthenticationMechanism} that enables the standard SSH
 * password authentication.
 * 
 * @author Lee David Painter
 */
public class PasswordAuthentication<C extends Context> implements AuthenticationMechanism {

    static final int SSH_MSG_PASSWORD_CHANGE_REQ = 60;
    AbstractServerTransport<C> transport;
    SshConnection con;
    PasswordAuthenticationProvider[] providers;
    AbstractAuthenticationProtocol<C> authentication;
    String username;
    String service;

    public static final String AUTHENTICATION_METHOD = "password";
    /**
     * Construct an instance.
     */
    public PasswordAuthentication() {
    }
    
    public PasswordAuthentication(AbstractServerTransport<C> transport,
			AbstractAuthenticationProtocol<C> authentication, SshConnection con,
			PasswordAuthenticationProvider[] providers) {
		this.transport = transport;
		this.authentication = authentication;
		this.con = con;
		this.providers = providers;
	}
    
    public boolean hasProviders() {
    	return providers!=null && providers.length > 0;
    }

    public String getMethod() {
        return "password";
    }

    public boolean startRequest(String username, byte[] msg) throws IOException {
    	transport.addTask(ExecutorOperationSupport.EVENTS, new PasswordAuthenticationTask(con, username, msg));
        return true;
    }

    public boolean processMessage(byte[] msg) throws IOException {
        // We dont support additional messages
        return false;
    }
    
    class PasswordAuthenticationTask extends ConnectionAwareTask {
    	String username;
    	byte[] msg;
    	
    	PasswordAuthenticationTask(SshConnection con, String username, byte[] msg) {
    		super(con);
    		this.username = username;
    		this.msg = msg;
    	}
    	protected void doTask() {
    		
    		if(!hasProviders()) {
    			if(Log.isDebugEnabled()) {
    				Log.debug("Remote requested password authentication but its not currently supported by this configuration.");
    			}
    			authentication.failedAuthentication();
    			return;
    		}
    	        
    		// Create ByteArrayReader so can read msg as if it was stream.
            ByteArrayReader bar = new ByteArrayReader(msg);

            // try to logon using the username and password obtains from msg
            try {

                // if the first byte is 0 then password doesnt need changing, else it
                // does
                boolean passwordChange = bar.read() == 0 ? false : true;
                String password = bar.readString();
                
                boolean success = false;


	            for (PasswordAuthenticationProvider passwordProvider : providers) {
					if (passwordChange) {
						success = passwordProvider.changePassword(con,
								username, password, bar.readString());
					} else {
						success = passwordProvider.verifyPassword(con,
								username, password);
					}
					if (success)
						break;
				}

                
                if (success) {
                    authentication.completedAuthentication();
                } else {
                    authentication.failedAuthentication();
                }
            } catch (PasswordChangeException ex) {
                // Send the password change message
                transport.postMessage(new SshMessage() {
                    public boolean writeMessageIntoBuffer(ByteBuffer buf) {
                        buf.put((byte) SSH_MSG_PASSWORD_CHANGE_REQ);
                        String desc = "Password change required.";
                        buf.putInt(desc.length());
                        buf.put(desc.getBytes());
                        buf.putInt(0);
                        return true;
                    }
                    
                    public void messageSent(Long sequenceNo) {
                        if(Log.isDebugEnabled()) Log.debug("Sent SSH_MSG_PASSWORD_CHANGE_REQ");
                    }
                });

                authentication.discardAuthentication();
            } catch(IOException ex) { 
            	transport.disconnect(TransportProtocolSpecification.BY_APPLICATION, ex.getMessage());
            }finally {
            	bar.close();
            }
    	}
    }

}
