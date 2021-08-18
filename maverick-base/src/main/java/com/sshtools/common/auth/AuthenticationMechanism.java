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

import com.sshtools.common.sshd.SshMessage;

/**
 * <p>
 * Each authentication mechanism the server supports should implement this
 * interface. When an authentication request is received from the client the
 * server looks up the authentication method name, for example "password" from
 * the {@link com.maverick.sshd.ConfigurationContext}. To support a new type of
 * SSH authentication mechanism, or to overide an existing implementation you
 * should add its Class object to the ConfigurationContext. This can be acheived
 * by adding the following code to your {@link com.maverick.sshd.SshDaemon} code
 * implementation of the
 * {@link com.maverick.sshd.SshDaemon#configure(ConfigurationContext)} method.
 * <blockquote>
 * 
 * <pre>
 * protected void configure(ConfigurationContext context) {
 * 	context.supportedAuthenticationMechanisms().add(&quot;kerberos@sshtools.com&quot;,
 * 			Class.forName(&quot;com.sshtools.kerberos.SSHKerberos&quot;));
 * }
 * </pre>
 * 
 * </blockquote>
 * </p>
 * 
 * <p>
 * <em>The SSH protocol recommends that method names are in the name@domain.com
 * syntax.</em>
 * </p>
 * 
 * <p>
 * The server will initialize your authentication object first by calling the
 * {@link #init(com.maverick.sshd.TransportProtocol, com.maverick.sshd.AuthenticationProtocol, byte[]) }
 * method, you should save the variables provided as these will be required to
 * communicate back to the client. Once initialized the transaction will be
 * started by the server by calling the {@link #startRequest} method. Here you
 * will be provided with the users' name and the request specific data. How you
 * proceed from here depends upon the authentication mechanism, in the standard
 * password authentication mechanism, the password is provided in the request
 * data and a native login takes place. If the authentication is successful your
 * implementation should call the
 * {@link com.maverick.sshd.AuthenticationProtocol#completedAuthentication()}
 * method, if it fails call
 * {@link com.maverick.sshd.AuthenticationProtocol#failedAuthentication()}
 * instead.
 * </p>
 * 
 * <p>
 * If your mechanism require further SSH messages to be sent you send them using
 * {@link com.maverick.sshd.TransportProtocol#sendMessage(SshMessage)} and
 * messages sent by the client will be received by your
 * {@link com.sshtools.common.auth.AuthenticationMechanism#processMessage(byte[])}
 * implementation.
 * </p>
 * 
 * @author Lee David Painter
 */
public interface AuthenticationMechanism {


	/**
	 * Start an authentication transaction. If the authentication mechanism is
	 * simple and you can determine the result from all information received in
	 * the SSH_MSG_USERAUTH_REQUEST message, you should call the approriate
	 * completion methods on the
	 * {@link com.maverick.sshd.AuthenticationProtocol} instance that was passed
	 * in the initialization process. The request data varies according to the
	 * authentication method. <blockquote>
	 * 
	 * <pre>
	 * if (success)
	 * 	authentication.completedAuthentication(method, username, service);
	 * else
	 * 	authentication.failedAuthentication(method);
	 * </pre>
	 * 
	 * </blockquote>
	 * @param username
	 * @param msg  the request data from the SSH_MSG_USERAUTH_REQUEST message
	 * @return <tt>true</tt> if the message was processed, otherwise
	 *         <tt>false</tt>
	 * @throws IOException
	 */
	public boolean startRequest(String username, byte[] msg) throws IOException;

	/**
	 * If the SSH protocol authentication method defines additional messages
	 * which are sent from the client, they will be passed into your
	 * implementation here when received.
	 * 
	 * @param msg
	 * @return boolean
	 * @throws IOException
	 */
	public boolean processMessage(byte[] msg) throws IOException;

	/**
	 * Return the SSH method name for this authentication. e.g "password"
	 * 
	 * @return String
	 */
	public String getMethod();

}
