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

package com.sshtools.client;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.sshtools.common.logger.Log;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.synergy.ssh.ByteArrayMessage;
import com.sshtools.synergy.ssh.ConnectionTaskWrapper;
import com.sshtools.synergy.ssh.TransportProtocol;

/**
 * Implements the keyboard-interactive authentication method.
 */
public class KeyboardInteractiveAuthenticator extends SimpleClientAuthenticator implements ClientAuthenticator {

	
	final static int SSH_MSG_USERAUTH_INFO_REQUEST = 60;
	final static int SSH_MSG_USERAUTH_INFO_RESPONSE = 61;
	
	KeyboardInteractiveCallback callback;
	TransportProtocolClient transport;
	String username;
	boolean completeFutureOnFailure = true;
	
	public KeyboardInteractiveAuthenticator(KeyboardInteractiveCallback callback) {
		this.callback = callback;
	}
	
	@Override
	public void authenticate(TransportProtocolClient transport, String username) {
		
		this.transport = transport;
		this.username = username;
		
		callback.init(transport.getConnection());
		
		transport.postMessage(new AuthenticationMessage(username, "ssh-connection", "keyboard-interactive") {

			@Override
			public boolean writeMessageIntoBuffer(ByteBuffer buf) {
				super.writeMessageIntoBuffer(buf);
				buf.putInt(0);
				buf.putInt(0);
				
				return true;
			}
			
		});
	}
	
	@Override
	public boolean processMessage(ByteArrayReader msg) throws IOException {
		
		switch(msg.read()) {
		case SSH_MSG_USERAUTH_INFO_REQUEST:
			
			if(Log.isDebugEnabled()) {
				Log.debug("SSH_MSG_USERAUTH_INFO_REQUEST received");
			}
			
			final String name = msg.readString();
			final String instruction = msg.readString();
			@SuppressWarnings("unused")
			String langtag = msg.readString();

			int num = (int) msg.readInt();
			String prompt;
			boolean echo;
			final KeyboardInteractivePrompt[] prompts = new KeyboardInteractivePrompt[num];
			for (int i = 0; i < num; i++) {
				prompt = msg.readString();
				echo = (msg.read() == 1);
				prompts[i] = new KeyboardInteractivePrompt(prompt, echo);
			}

			transport.addTask(TransportProtocol.CALLBACKS, new ConnectionTaskWrapper(transport.getConnection(), new Runnable() {
				public void run() {
					callback.showPrompts(name, instruction, prompts, new KeyboardInteractivePromptCompletor() {
						@Override
						public void complete() {
							
							ByteArrayWriter baw = new ByteArrayWriter();
							
							try {
								
								baw.write(SSH_MSG_USERAUTH_INFO_RESPONSE);
								baw.writeInt(prompts.length);

								for (int i = 0; i < prompts.length; i++) {
									baw.writeString(prompts[i].getResponse());
								}
								
								transport.postMessage(new ByteArrayMessage(baw.toByteArray()) {
									@Override
									public void messageSent(Long sequenceNo) {
										if(Log.isDebugEnabled()) {
											Log.debug("SSH_MSG_USERAUTH_INFO_RESPONSE sent");
										}
									}
									
								});
							} catch (IOException e) {
								Log.error("Error during showPrompts", e);
								transport.disconnect(TransportProtocol.AUTH_CANCELLED_BY_USER, "User cancelled auth.");
							} finally {
								try {
									baw.close();
								} catch (IOException e) {
								}
							}
						}
						
						@Override
						public void cancel() {
							KeyboardInteractiveAuthenticator.this.cancel();
							transport.disconnect(TransportProtocol.AUTH_CANCELLED_BY_USER, "User cancelled auth.");
						}
						
					});
				}
			}));
			

			return true;
		}
		return false;
	}

	@Override
	public String getName() {
		return "keyboard-interactive";
	}

}
