package com.maverick.agent.client;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.maverick.ssh2.AuthenticationProtocol;
import com.maverick.ssh2.AuthenticationResult;
import com.maverick.ssh2.Ssh2PublicKeyAuthentication;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayWriter;
/**
 * @desc   Authenticate a user from public key agent client  
 * @author Aruna Abesekara
 *
 */
public class Ssh2AgentAuthentication extends Ssh2PublicKeyAuthentication {
	
	final static int SSH_MSG_USERAUTH_PK_OK = 60;
	
	String username;
	SshAgentClient agent=null;
	
	public Ssh2AgentAuthentication(SshAgentClient agent) {
		super();
		this.agent = agent;
		setSignatureGenerator(agent);
	}
	
	public String getMethod() {
		return "agent";
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username=username;
	}

	public SshAgentClient getSshAgentClient() {
		return agent;
	}

	public void authenticate(AuthenticationProtocol authentication, String servicename)
			throws SshException, AuthenticationResult {
		
		try{
			if(getSshAgentClient()==null){
				throw new SshException("Agent not set!",
						SshException.BAD_API_USAGE);
			}
			if (getUsername() == null) {
				throw new SshException("Username not set!",
						SshException.BAD_API_USAGE);
			}
			 Map<SshPublicKey,String> keys = getSshAgentClient().listKeys();
			 Iterator<Map.Entry<SshPublicKey,String>> it = keys.entrySet().iterator();
		     boolean acceptable = false;
		     SshPublicKey key = null;
		     Map.Entry<SshPublicKey,String> entry;
		     
		     while (it.hasNext() && !acceptable) {
		    	 	 entry = it.next();
		    		 key = (SshPublicKey) entry.getKey();
		    		 entry.getValue();
		    		 try {
		    			setPublicKey(key);
		    			setAuthenticating(false);
						super.authenticate(authentication, servicename);
					} catch (AuthenticationResult e) {
						if(e.getResult()==PUBLIC_KEY_ACCEPTABLE) {
							acceptable = true;
							break;
						}
					}
		      }
		     
		     // Authenticate 
		     if(acceptable){
		    	 setPublicKey(key);
		    	 setSignatureGenerator(agent);
		    	 setAuthenticating(true);
		    	 super.authenticate(authentication, servicename);
		     } else {
		    	 throw new AuthenticationResult(FAILED);
		     }
		} catch (IOException ex) {
			Log.error("Authentication error",ex);
			throw new SshException(ex, SshException.INTERNAL_ERROR);
		} finally {
			
		}

	}
	
	protected byte[] encodeSignature(byte[] signature) throws IOException {
		if(agent.isRFCAgent()) {
			return super.encodeSignature(signature);
		} else {
			return signature;
		}
	}
	
	@SuppressWarnings("unused")
	private boolean acceptKey(AuthenticationProtocol authentication,
			String username, String servicename, SshPublicKey key)
	                throws SshException,IOException,AuthenticationResult {
		
		ByteArrayWriter baw = new ByteArrayWriter();
		baw.writeBinaryString(authentication.getSessionIdentifier());
		baw.write(AuthenticationProtocol.SSH_MSG_USERAUTH_REQUEST);
		baw.writeString(getUsername());
		baw.writeString(servicename);
		baw.writeString("publickey");
		baw.writeBoolean(false);
		
		byte[] encoded = key.getEncoded();

		ByteArrayWriter baw2 = new ByteArrayWriter();

		try {
			// Generate the authentication request
			baw2.writeBoolean(isAuthenticating());
			baw2.writeString(key.getAlgorithm());
			baw2.writeBinaryString(encoded);

			if (isAuthenticating()) {

				byte[] signature;
				signature =  agent.hashAndSign(key, key.getSigningAlgorithm(), baw.toByteArray());
				
				// Format the signature correctly
				ByteArrayWriter sig = new ByteArrayWriter();

				try {
					sig.writeString(key.getSigningAlgorithm());
					sig.writeBinaryString(signature);
					baw2.writeBinaryString(sig.toByteArray());
				} finally {
					sig.close();
				}
			}

			authentication.sendRequest(getUsername(), servicename,	"publickey", baw2.toByteArray());

			byte[] response = authentication.readMessage();

			if (response[0] == SSH_MSG_USERAUTH_PK_OK) {
				return true;
			}
			
		} finally {
			baw2.close();
			baw.close();
		}

		return false;
	}

}
