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
package com.sshtools.agent.client;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import com.sshtools.agent.AgentMessage;
import com.sshtools.agent.KeyConstraints;
import com.sshtools.agent.exceptions.AgentNotAvailableException;
import com.sshtools.agent.exceptions.InvalidMessageException;
import com.sshtools.agent.openssh.OpenSSHAgentMessages;
import com.sshtools.agent.openssh.OpenSshSignRequest;
import com.sshtools.agent.rfc.RFCAgentMessages;
import com.sshtools.agent.rfc.SshAgentAddKey;
import com.sshtools.agent.rfc.SshAgentAlive;
import com.sshtools.agent.rfc.SshAgentDeleteKey;
import com.sshtools.agent.rfc.SshAgentFailure;
import com.sshtools.agent.rfc.SshAgentForwardingNotice;
import com.sshtools.agent.rfc.SshAgentKeyList;
import com.sshtools.agent.rfc.SshAgentLock;
import com.sshtools.agent.rfc.SshAgentOperationComplete;
import com.sshtools.agent.rfc.SshAgentPing;
import com.sshtools.agent.rfc.SshAgentPrivateKeyOp;
import com.sshtools.agent.rfc.SshAgentRandom;
import com.sshtools.agent.rfc.SshAgentRandomData;
import com.sshtools.agent.rfc.SshAgentRequestVersion;
import com.sshtools.agent.rfc.SshAgentSuccess;
import com.sshtools.agent.rfc.SshAgentUnlock;
import com.sshtools.agent.rfc.SshAgentVersionResponse;
import com.sshtools.agent.win32.NamedPipeClient;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SignatureGenerator;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

/** Maintain the public keys for SshConnection
 * @author Aruna Abesekara
 * @date  21/07/2014
 */
public class SshAgentClient implements SignatureGenerator {

	public static final String HASH_AND_SIGN = "hash-and-sign";
	InputStream in;
	OutputStream out;
	boolean isForwarded = false;
	HashMap<Integer,Class<? extends AgentMessage>> messages = new HashMap<Integer,Class<? extends AgentMessage>>();
	Closeable socket;
	boolean isRFCAgent = false;
	
	public static String WINDOWS_SSH_AGENT_SERVICE = "openssh-ssh-agent";
	
//	http://www.opensource.apple.com/source/OpenSSH/OpenSSH-142/openssh/PROTOCOL.agent
	
	public SshAgentClient(boolean isForwarded, String application, Socket socket) throws IOException {
		this(isForwarded, application, socket, socket.getInputStream(), socket.getOutputStream(), false);
	}
	
	public SshAgentClient(boolean isForwarded, String application, Closeable socket, InputStream in, OutputStream out, boolean isRFC)
			throws IOException {
		Log.info("New SshAgentClient instance created");
		this.socket = socket;
		this.in = in;
		this.out = out;
		this.isForwarded = isForwarded;
		
		registerMessages();

		if (isForwarded && isRFC) {
			sendForwardingNotice();
		} else if(isRFC) {
			sendVersionRequest(application);
		} else {
			listKeys();
		}
	}

	public boolean isRFCAgent() {
		return isRFCAgent;
	}
	
	
	public static SshAgentClient connectOpenSSHAgent(String application) throws AgentNotAvailableException, IOException {
		
		String location = System.getenv("SSH_AUTH_SOCK");
		if(location==null && System.getProperty("os.name").toLowerCase().startsWith("windows")) {
			location = SshAgentClient.WINDOWS_SSH_AGENT_SERVICE;
		} else if(location==null) {
			throw new AgentNotAvailableException("SSH_AUTH_SOCK is undefined");
		}
		
		return connectOpenSSHAgent(application, location);
	}
	/**
	 * Determines operating system type (windows or not) and connects using the appropriate
	 * socket type for the platform.
	 * 
	 * @param application
	 * @param location
	 * @return
	 * @throws AgentNotAvailableException
	 * @throws IOException
	 */
	public static SshAgentClient connectOpenSSHAgent(String application,
			String location) throws AgentNotAvailableException, IOException {
	
		if(System.getProperty("os.name").toLowerCase().startsWith("windows")) {
			return connectLocalAgent(application, location, AgentSocketType.WINDOWS_NAMED_PIPE, false);
		} else {
			return connectLocalAgent(application, location, AgentSocketType.UNIX_DOMAIN, false);
		}
	}
	
	/**
	 * Connect to the local agent.
	 * 
	 * @param application
	 *            the application connecting
	 * @param location
	 *            the location of the agent, in the form "localhost:port"
	 * 
	 * @return a connected agent client
	 * 
	 * @throws AgentNotAvailableException
	 *             if the agent is not available at the location specified
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public static SshAgentClient connectLocalAgent(String application,
			String location, AgentSocketType type, boolean RFCAgent) throws AgentNotAvailableException, IOException {
		try {

			switch(type) {
			case WINDOWS_NAMED_PIPE:
				NamedPipeClient namedPipe = new NamedPipeClient(location);
				return new SshAgentClient(false, application, namedPipe, namedPipe.getInputStream(), namedPipe.getOutputStream(), false);
			default:
				Socket socket = connectAgentSocket(location, type);
				return new SshAgentClient(false, application, socket, socket.getInputStream(), socket.getOutputStream(), false);
			}
		} catch (IOException ex) {
			Log.error("Agent socket error :",ex);
			throw new AgentNotAvailableException();
		}
	}

	/**
	 * Connect a socket to the agent at the location specified.
	 * 
	 * @param location
	 *            the location of the agent, in the form "localhost:port"
	 * 
	 * @return the connected socket
	 * 
	 * @throws AgentNotAvailableException
	 *             if an agent is not available at the location specified
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public static Socket connectAgentSocket(String location, AgentSocketType type)
			throws AgentNotAvailableException, IOException {
		try {
			if (location == null) {
				throw new AgentNotAvailableException();
			}
			Socket socket = null;
			switch(type) {
			case TCPIP:
				int idx = location.indexOf(":");

				if (idx == -1) {
					throw new AgentNotAvailableException();
				}

				String host = location.substring(0, idx);
				int port = Integer.parseInt(location.substring(idx + 1));
			    socket = new Socket(host, port);
				break;
			case UNIX_DOMAIN:
				File socketFile = new File(location);
				socket = AFUNIXSocket.newInstance();
				socket.connect(new AFUNIXSocketAddress(socketFile));
				break;
			default:
				throw new AgentNotAvailableException("Invalid socket type!");
			}
			

			return socket;
		} catch (IOException ex) {
			Log.error("Agent socket error :",ex);
			throw new AgentNotAvailableException();
		}
	}

	/**
	 * Close the agent
	 */
	public void close() {
		Log.info("Closing agent client");

		try {
			in.close();
		} catch (IOException ex) {
			Log.error("Error in closing inputstream", ex);
		}

		try {
			out.close();
		} catch (IOException ex1) {
			Log.error("Error in closing outputstream", ex1);
		}

		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException ex2) {
			Log.error("Error in closing socket", ex2);
		}
	}

	/**
	 * Ping the remote side with some random padding data
	 * 
	 * @param padding
	 *            the padding data
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public void ping(byte[] padding) throws IOException {
		AgentMessage msg = new SshAgentPing(padding);
		sendMessage(msg);
		try {
			msg = readMessage();
		} catch (InvalidMessageException e) {
			throw new IOException("Error in reading agent response");
		}

		if (msg instanceof SshAgentAlive) {
			if (!Arrays.equals(padding, ((SshAgentAlive) msg).getPadding())) {
				throw new IOException(
						"Agent failed to reply with expected data");
			}
		} else {
			throw new IOException(
					"Agent failed to provide the request random data");
		}
	}

	protected void registerMessages() {
		
		messages.put(Integer.valueOf(OpenSSHAgentMessages.SSH_AGENT_SUCCESS),
				SshAgentSuccess.class);
		messages.put(Integer.valueOf(OpenSSHAgentMessages.SSH_AGENT_FAILURE),
				SshAgentFailure.class);
		messages.put(Integer.valueOf(OpenSSHAgentMessages.SSH2_AGENT_IDENTITIES_ANSWER),
				SshAgentKeyList.class);
		
		messages.put(Integer.valueOf(RFCAgentMessages.SSH_AGENT_VERSION_RESPONSE),
				SshAgentVersionResponse.class);
		messages.put(Integer.valueOf(RFCAgentMessages.SSH_AGENT_SUCCESS),
				SshAgentSuccess.class);
		messages.put(Integer.valueOf(RFCAgentMessages.SSH_AGENT_FAILURE),
				SshAgentFailure.class);
		messages.put(Integer.valueOf(RFCAgentMessages.SSH_AGENT_KEY_LIST),
				SshAgentKeyList.class);
		messages.put(Integer.valueOf(RFCAgentMessages.SSH_AGENT_RANDOM_DATA),
				SshAgentRandomData.class);
		messages.put(Integer.valueOf(RFCAgentMessages.SSH_AGENT_ALIVE),
				SshAgentAlive.class);
		messages.put(Integer.valueOf(RFCAgentMessages.SSH_AGENT_OPERATION_COMPLETE),
				SshAgentOperationComplete.class);
		
		messages.put(Integer.valueOf(OpenSSHAgentMessages.SSH2_AGENT_SIGN_RESPONSE),
				SshAgentOperationComplete.class);
	}

	/**
	 * Send a forwarding notice.
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void sendForwardingNotice() throws IOException {
		InetAddress addr = InetAddress.getLocalHost();
		SshAgentForwardingNotice msg = new SshAgentForwardingNotice(
				addr.getHostName(), addr.getHostAddress(), 22);
		sendMessage(msg);
	}

	/**
	 * Send a subsystem message
	 * 
	 * @param msg
	 *            the message to send
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void sendMessage(AgentMessage msg) throws IOException {
		Log.info("Sending message " + msg.getMessageName());

		byte[] msgdata;
		try {
			msgdata = msg.toByteArray();
			out.write(ByteArrayWriter.encodeInt(msgdata.length));
			out.write(msgdata);
			out.flush();
		} catch (InvalidMessageException e) {
			Log.error("Message sending error :",e);
			throw new IOException(e.getMessage(), e);
		}
	}

	/**
	 * Request the agent version.
	 * 
	 * @param application
	 *            the application connecting
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	protected void sendVersionRequest(String application) throws IOException {
		try {
			AgentMessage msg = new SshAgentRequestVersion(application);
			sendMessage(msg);

			msg = readMessage();

			if (msg instanceof SshAgentVersionResponse) {
				/**
				 * This is the RFC version
				 */
				isRFCAgent = true;
				SshAgentVersionResponse reply = (SshAgentVersionResponse) msg;
				System.out.println("Agent Version :" + reply.getVersion());
				if (reply.getVersion() != 2) {
					throw new IOException(
							"The agent verison is not compatible with verison 2");
				}
			} else if(msg instanceof SshAgentSuccess) {
				
				/**
				 * This is the OpenSSH version
				 */
				isRFCAgent = false;
				
			} else if(msg instanceof SshAgentFailure) {
				
				/**
				 * This is the OpenSSH version
				 */
				isRFCAgent = false;
				
			} else {
				throw new IOException(
						"The agent did not respond with the appropriate version");
			}
		} catch (InvalidMessageException e) {
			Log.error("Version sending error :",e);
			throw new IOException(e.getMessage(), e);
		}
	}

	/**
	 * Delete all the keys held by the agent.
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public void deleteAllKeys() throws IOException {
		try {
			AgentMessage msg = new AgentMessage(
					RFCAgentMessages.SSH_AGENT_DELETE_ALL_KEYS);
			sendMessage(msg);
			msg = readMessage();

			if (!(msg instanceof SshAgentSuccess)) {
				throw new IOException("The agent failed to delete all keys");
			}
		} catch (Exception ex) {
			Log.error("delete All key error :",ex);
			throw new IOException(ex.getMessage(), ex);
		}
	}

	/**
	 * Read a single message from the inputstream and convert into a valid
	 * subsystem message
	 * 
	 * @return the next available subsystem message
	 * 
	 * @throws InvalidMessageException
	 *             if the message received is invalid
	 */
	protected AgentMessage readMessage() throws InvalidMessageException {
		try {
			byte[] lendata = new byte[4];
			byte[] msgdata;
			int len;

			// Read the first 4 bytes to determine the length of the message
			len = 0;

			while (len < 3) {
				len += in.read(lendata, len, lendata.length - len);
			}

			len = (int) ByteArrayReader.readInt(lendata, 0);
			msgdata = new byte[len];
			len = 0;

			while (len < msgdata.length) {
				len += in.read(msgdata, len, msgdata.length - len);
			}

			Integer id = Integer.valueOf((int) msgdata[0] & 0xFF);

			if (messages.containsKey(id)) {
				Class<? extends AgentMessage> cls = messages.get(id);
				AgentMessage msg = cls.getConstructor().newInstance();
				msg.fromByteArray(msgdata);
				Log.info("Received message " + msg.getMessageName());

				return msg;
			} else {
				throw new InvalidMessageException("Unrecognised message id "
						+ id.toString(), SshException.AGENT_ERROR);
			}
		} catch (Exception ex) {
			throw new InvalidMessageException(ex.getMessage(),
					SshException.AGENT_ERROR);
		}
	}

	/**
	 * Add a key to the agent
	 * 
	 * @param prvkey
	 *            the private key to add
	 * @param pubkey
	 *            the private keys public key
	 * @param description
	 *            a description of the key
	 * @param constraints
	 *            a set of contraints for key use
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public void addKey(SshPrivateKey prvkey, SshPublicKey pubkey,
			String description, KeyConstraints constraints) throws IOException {
		AgentMessage msg = new SshAgentAddKey(prvkey, pubkey, description,
				constraints);
		sendMessage(msg);
		try {
			msg = readMessage();
			if (!(msg instanceof SshAgentSuccess)) {
				throw new IOException("The key could not be added");
			}
		} catch (InvalidMessageException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
	
	/**
	 * List all the keys on the agent.
	 * 
	 * @return a map of public keys and descriptions
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public Map<SshPublicKey,String> listKeys() throws IOException {
		try{
			AgentMessage msg = new AgentMessage(isRFCAgent ? 
					RFCAgentMessages.SSH_AGENT_LIST_KEYS
					: OpenSSHAgentMessages.SSH2_AGENTC_REQUEST_IDENTITIES);
			sendMessage(msg);
			msg = readMessage();

			if (msg instanceof SshAgentKeyList) {
				return ((SshAgentKeyList) msg).getKeys();
			} else {
				throw new IOException("The agent responsed with an invalid message :"+msg.getMessageName());
			}
		} catch (InvalidMessageException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
	
	/**
	 * Lock the agent
	 * 
	 * @param password
	 *            password that will be required to unlock
	 * 
	 * @return true if the agent was locked, otherwise false
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public boolean lockAgent(String password) throws IOException {
		try{
			AgentMessage msg = new SshAgentLock(isRFCAgent, password);
			sendMessage(msg);
			msg = readMessage();

			return (msg instanceof SshAgentSuccess);
		} catch (InvalidMessageException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
	
	/**
	 * Unlock the agent
	 * 
	 * @param password
	 *            the password to unlock
	 * 
	 * @return true if the agent was unlocked, otherwise false
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public boolean unlockAgent(String password) throws IOException {
		try{
			AgentMessage msg = new SshAgentUnlock(isRFCAgent, password);
			sendMessage(msg);
			msg = readMessage();
			return (msg instanceof SshAgentSuccess);
		} catch (InvalidMessageException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
	
	/**
	 * Request some random data from the remote side
	 * 
	 * @param count
	 *            the number of bytes needed
	 * 
	 * @return the random data received
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public byte[] getRandomData(int count) throws IOException {
		try {
			AgentMessage msg = new SshAgentRandom(count);
			sendMessage(msg);
			msg = readMessage();
			if (msg instanceof SshAgentRandomData) {
				return ((SshAgentRandomData) msg).getRandomData();
			} else {
				throw new IOException("Agent failed to provide the request random data");
			}
		} catch (InvalidMessageException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
	
	/**
	 * Delete a key held by the agent
	 * 
	 * @param key
	 *            the public key of the private key to delete
	 * @param description
	 *            the description of the key
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public void deleteKey(SshPublicKey key, String description)
			throws IOException {
		try {
			AgentMessage msg = new SshAgentDeleteKey(key, description);
			sendMessage(msg);
			msg = readMessage();

			if (!(msg instanceof SshAgentSuccess)) {
				throw new IOException("The agent failed to delete the key");
			}
		} catch (InvalidMessageException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
	
	
	/**
	 * Request a hash and sign operation be performed for a given public key.
	 * 
	 * @param key
	 *            the public key of the required private key
	 * @param data
	 *            the data to has and sign
	 * 
	 * @return the hashed and signed data
	 * 
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public byte[] hashAndSign(SshPublicKey key, String signingAlgorithm, byte[] data) throws IOException {
		try {
			AgentMessage msg;
			
			if(isRFCAgent()) {
				msg = new SshAgentPrivateKeyOp(key, HASH_AND_SIGN, data);
			} else {
				msg = new OpenSshSignRequest(key, data);
			}
				
			sendMessage(msg);
			msg = readMessage();

			if (msg instanceof SshAgentOperationComplete) {
				return ((SshAgentOperationComplete) msg).getData();
			} else {
				throw new IOException("The operation failed");
			}
		} catch (InvalidMessageException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public byte[] sign(SshPublicKey key, String signingAlgorithm, byte[] data) throws SshException {
		try {
			return hashAndSign(key, signingAlgorithm, data);
		} catch (IOException e) {
			throw new SshException(e);
		}
	}

	@Override
	public Collection<SshPublicKey> getPublicKeys() throws IOException {
		return listKeys().keySet();
	}

}
