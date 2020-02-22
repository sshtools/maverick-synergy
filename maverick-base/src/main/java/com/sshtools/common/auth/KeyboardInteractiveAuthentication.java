/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.auth;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.TransportProtocolSpecification;
import com.sshtools.common.ssh2.KBIPrompt;
import com.sshtools.common.sshd.AbstractServerTransport;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

public class KeyboardInteractiveAuthentication<C extends Context> implements
		AuthenticationMechanism {

	public static final int SSH_MSG_USERAUTH_INFO_REQUEST = 60;
	public static final int SSH_MSG_USERAUTH_INFO_RESPONSE = 61;

	AbstractServerTransport<C> transport;
	AbstractAuthenticationProtocol<C> authentication;
	SshConnection con;
	KeyboardInteractiveAuthenticationProvider[] providers;
	
	public static final String AUTHENTICATION_METHOD = "keyboard-interactive";
	
	public KeyboardInteractiveAuthentication(
			AbstractServerTransport<C> transport,
			AbstractAuthenticationProtocol<C> authentication, SshConnection con,
			KeyboardInteractiveAuthenticationProvider[] providers) {
		this.transport = transport;
		this.authentication = authentication;
		this.con = con;
		this.providers = providers;
	}

	String username;
	String service;

	KeyboardInteractiveProvider selectedProvider;

	public String getMethod() {
		return "keyboard-interactive";
	}

	public boolean processMessage(byte[] msg) throws IOException {

		if (msg[0] != SSH_MSG_USERAUTH_INFO_RESPONSE)
			return false;
		
		if(Log.isDebugEnabled()) {
			Log.debug("Received SSH_MSG_USERAUTH_INFO_RESPONSE");
		}
		con.addTask(ExecutorOperationSupport.EVENTS, new ProcessMessageTask(con, msg));
		
		return true;
	}

	public boolean startRequest(String username, byte[] msg) throws IOException {
		
		con.addTask(ExecutorOperationSupport.EVENTS, new StartAuthenticationTask(con, username, msg));
		return false;
	}

	void sendInfoRequest(KBIPrompt[] prompts, String name, String instructions)
			throws IOException {

		/**
		 * byte SSH_MSG_USERAUTH_INFO_REQUEST string name (ISO-10646 UTF-8)
		 * string instruction (ISO-10646 UTF-8) string language tag (as defined
		 * in [RFC-3066]) int num-prompts string prompt[1] (ISO-10646 UTF-8)
		 * boolean echo[1] ... string prompt[num-prompts] (ISO-10646 UTF-8)
		 * boolean echo[num-prompts]
		 **/
		ByteArrayWriter request = new ByteArrayWriter();

		try {
			request.write(SSH_MSG_USERAUTH_INFO_REQUEST);
			request.writeString(name);
			request.writeString(instructions);
			request.writeString("");
			request.writeInt(prompts.length);
			for (int i = 0; i < prompts.length; i++) {
				request.writeString(prompts[i].getPrompt());
				request.writeBoolean(prompts[i].echo());
			}

			transport.postMessage(new InfoRequest(request.toByteArray()));
		} finally {
			request.close();
		}
	}

	class InfoRequest implements SshMessage {
		byte[] msg;

		InfoRequest(byte[] msg) {
			this.msg = msg;
		}

		public void messageSent(Long sequenceNo) {
			if(Log.isDebugEnabled())
				Log.debug("Sent SSH_MSG_USERAUTH_INFO_REQUEST");
		}

		public boolean writeMessageIntoBuffer(ByteBuffer buf) {
			buf.put(msg);
			return true;
		}

	}
	
	class StartAuthenticationTask extends ConnectionAwareTask {
    	String username;
    	byte[] msg;
    	
    	StartAuthenticationTask(SshConnection con, String username, byte[] msg) {
    		super(con);
    		this.username = username;
    		this.msg = msg;
    	}
    	
    	public void doTask() {
    		// Create ByteArrayReader so can read msg as if it was stream.
    		ByteArrayReader bar = new ByteArrayReader(msg);

    		try {
    			String languageTag = bar.readString();
    			if(Log.isDebugEnabled()) {
    				Log.debug("Language: " + languageTag);
    			}
    			String submethods = bar.readString();
    			if(Log.isDebugEnabled()) {
    				Log.debug("Submethods: " + submethods);
    			}
    			// Ignore submethods for now, we simply support one configured method
    			if(selectedProvider==null) {
    				for (KeyboardInteractiveAuthenticationProvider k : providers) {
    					KeyboardInteractiveProvider kp = k.createInstance(con);
    					KBIPrompt[] prompts = kp.init(con);
    					if (prompts != null) {
    						sendInfoRequest(prompts, kp.getName(), kp.getInstruction());
    						selectedProvider = kp;
    						break;
    					} else {
    						if (kp.hasAuthenticated()) {
    							selectedProvider = kp;
    							authentication.completedAuthentication();
    						}
    					}
    				}
    				
    				if(selectedProvider==null) {
    					authentication.failedAuthentication();
    				}
    			} else {
    				KBIPrompt[] prompts = selectedProvider.init(con);
    				if(prompts!=null) {
    					sendInfoRequest(prompts, selectedProvider.getName(), selectedProvider.getInstruction());
    				} else {
    					if (selectedProvider.hasAuthenticated()) {
    						authentication.completedAuthentication();
    					} else {
    						authentication.failedAuthentication();
    					}
    				}
    			}		
    		} catch(IOException ex) { 
    			Log.error("Error starting keyboard-interactive authentication", ex);
    			con.disconnect(TransportProtocolSpecification.PROTOCOL_ERROR, ex.getMessage());
    		} finally {
    			bar.close();
    		}
    	}
	}
	
	class ProcessMessageTask extends ConnectionAwareTask {
		
		byte[] msg;
		
		ProcessMessageTask(SshConnection con, byte[] msg) {
			super(con);
			this.msg = msg;
		}
		
		public void doTask() {
			ByteArrayReader response = new ByteArrayReader(msg);

			try {
				response.read();

				ArrayList<String> answers = new ArrayList<String>();
				int count = (int) response.readInt();
				for (int i = 0; i < count; i++) {
					answers.add(response.readString());
				}

				List<KBIPrompt> additionalPrompts = new ArrayList<>();

				boolean success = selectedProvider.setResponse((String[]) answers
						.toArray(new String[0]), additionalPrompts);

				if(!additionalPrompts.isEmpty()) {

					if(authentication.canContinue()) {
						if(!success) {
							authentication.markFailed();
						}
						sendInfoRequest(additionalPrompts.toArray(new KBIPrompt[0]), 
								selectedProvider.getName(), selectedProvider.getInstruction());
					} else {
						authentication.failedAuthentication();
					}
				} else {
					if (selectedProvider.hasAuthenticated()) {
						authentication.completedAuthentication();
					} else {
						authentication.failedAuthentication();
					}

				}
			} catch(IOException ex) { 
				Log.error("Error processing USER_AUTH_INFO_RESPONSE", ex);
				con.disconnect(TransportProtocolSpecification.PROTOCOL_ERROR, ex.getMessage());
			} finally {
				response.close();
			}
		}
	}

	public KeyboardInteractiveProvider getSelectedProvider() {
		return selectedProvider;
	}
}
