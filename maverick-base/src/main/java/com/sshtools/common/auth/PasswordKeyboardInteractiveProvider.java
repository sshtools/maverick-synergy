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
package com.sshtools.common.auth;

import java.io.IOException;
import java.util.Collection;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.TransportProtocolSpecification;
import com.sshtools.common.ssh2.KBIPrompt;

public class PasswordKeyboardInteractiveProvider implements
		KeyboardInteractiveProvider {

	final static int REQUESTED_PASSWORD = 1;
	final static int CHANGING_PASSWORD = 2;
	final static int FINISHED = 2;
	
	String username;
	String password;
	boolean success = false;
	String name = "password";
	String instruction = "";
	
	SshConnection con;
	PasswordAuthenticationProvider[] providers;
	int state = REQUESTED_PASSWORD;
	int maxAttempts = 2;
	PasswordAuthenticationProvider selectedProvider = null;
	
	public PasswordKeyboardInteractiveProvider() {
		
	}
	public PasswordKeyboardInteractiveProvider(
			PasswordAuthenticationProvider[] providers, SshConnection con) {
		this.providers = providers;
		this.con = con;
	}

	public boolean hasAuthenticated() {
		return success;
	}

	public boolean setResponse(String[] answers, Collection<KBIPrompt> additionalPrompts) {

		if(answers.length == 0) {
			throw new RuntimeException("Not enough answers!");
		}
		
		maxAttempts--;
		
		if(maxAttempts < 0) {
			state = FINISHED;
			return false;
		}
		
		switch(state) {
		case REQUESTED_PASSWORD:
			
			password = answers[0];
			
			try {

				for(PasswordAuthenticationProvider passwordProvider : providers) {
					selectedProvider = passwordProvider;
					success = passwordProvider.verifyPassword(con, username, password);
					if(success) {
						state = FINISHED;
						return true;
					}
				}
				
				instruction = "Sorry, try again";
				additionalPrompts.add(new KBIPrompt(getPasswordPrompt(), false));
				
				return false;
			} catch (PasswordChangeException e) {
				state = CHANGING_PASSWORD;
				maxAttempts = 2;
				
				additionalPrompts.add(new KBIPrompt(getNewPasswordPrompt(), false));
				additionalPrompts.add(new KBIPrompt(getConfirmPasswordPrompt(), false));
				
				if(e.getMessage()==null)
					instruction = getChangePasswordInstructions(username);
				else
					instruction = e.getMessage();
				
				return true;
			} catch(IOException ex) {
				con.disconnect(TransportProtocolSpecification.BY_APPLICATION, ex.getMessage());
			}
		case CHANGING_PASSWORD:
			if(answers.length < 2) {
				throw new RuntimeException("Not enough answers!");
			}
			
			String password1 = answers[0];
			String password2 = answers[1];
			
			if (password1.equals(password2)) {

				try {
					success = selectedProvider.changePassword(con,
							username, password, password1);
					if(success) {
						state = FINISHED;
						return true;
					}
				} catch (PasswordChangeException e) {	
				} catch (IOException e) {
				}

				state = CHANGING_PASSWORD;

				additionalPrompts.add(new KBIPrompt(getNewPasswordPrompt(), false));
				additionalPrompts.add(new KBIPrompt(getConfirmPasswordPrompt(), false));
				instruction = getChangePasswordFailed(username);

				return true;
		} else {
			instruction = getChangePasswordMismatch(username);
			additionalPrompts.add(new KBIPrompt(getNewPasswordPrompt(), false));
			additionalPrompts.add(new KBIPrompt(getConfirmPasswordPrompt(), false));

			return true;
		}
			
		default:
			throw new RuntimeException("We shouldn't be here");
		}
		
	}

	public KBIPrompt[] init(SshConnection con) {
		this.username = con.getUsername();
		this.con = con;
		KBIPrompt[] prompts = new KBIPrompt[1];
		prompts[0] = new KBIPrompt(getPasswordPrompt(), false);
		instruction = getInstructions(username);
		return prompts;
	}

	public String getInstruction() {
		return instruction;
	}

	public String getName() {
		return name;
	}
	
	protected String getPasswordPrompt() {
		return "Password:";
	}
	
	protected String getConfirmPasswordPrompt() {
		return "Confirm Password:";
	}
	
	protected String getNewPasswordPrompt() {
		return "New Password:";
	}
	
	protected String getInstructions(String username) {
		return "Enter password for " + username;
	}
	
	protected String getChangePasswordInstructions(String username) {
		return "Enter new password for " + username;
	}
	
	protected String getChangePasswordFailed(String username) {
		return "Password change failed! Enter new password for " + username;
	}
	
	protected String getChangePasswordMismatch(String username) {
		return "Passwords do not match! Enter new password for " + username;
	}

}
