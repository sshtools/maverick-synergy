/* HEADER */
package com.sshtools.common.publickey;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Pattern;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.Base64;

public class OpenSSHPublicKeyFile implements SshPublicKeyFile {

	byte[] formattedkey;
	String comment;
	String options;

	OpenSSHPublicKeyFile(byte[] formattedkey) throws IOException {
		this.formattedkey = formattedkey;
		toPublicKey(); // To validate
	}

	OpenSSHPublicKeyFile(SshPublicKey key, String comment) throws IOException {
		this(key, comment, null);
	}

	OpenSSHPublicKeyFile(SshPublicKey key, String comment, String options)
			throws IOException {
		try {
			String formatted = options == null ? "" : (options + " ");

			formatted += key.getAlgorithm() + " "
					+ Base64.encodeBytes(key.getEncoded(), true);

			if (comment != null && comment.trim().length() > 0) {
				formatted += (" " + comment);
			}

			formattedkey = formatted.getBytes();
		} catch (SshException ex) {
			throw new IOException("Failed to encode public key");
		}
	}

	public String toString() {
		return new String(formattedkey);
	}

	public byte[] getFormattedKey() {
		return formattedkey;
	}

	public SshPublicKey toPublicKey() throws IOException {

		String temp = new String(formattedkey);

		BufferedReader r = new BufferedReader(new StringReader(temp));

		String line;
		temp = "";
		while((line = r.readLine())!=null) {
			temp += line;
		}
	    
	    int i = 0;
	    
	    while(i > -1) {
	    
	    	int f = i;
	    	i = temp.indexOf(" ", i);
	    	
	    	if(i > -1) {
		    	String algorithm = temp.substring(f, i);
		    	i++;
		    	if(!ComponentManager.getInstance().supportedPublicKeys().contains(algorithm)) {
		    		continue;
		    	}
		      
		        // Get the keyblob end index
		        int i2 = temp.indexOf(" ", i);
	
		        String encoded;
		        if(i2 !=-1) {
		          encoded = temp.substring(i, i2);
	
		          if(temp.length() > i2) {
		            comment = temp.substring(i2).trim();
		          }
	
		          if(!encoded.matches("(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{0,2}==|[A-Za-z0-9+/]{0,3}=)?")) {
		        	  throw new IOException("Public key blob does not appear to be base64 encoded data");
		          }
		          return SshPublicKeyFileFactory.decodeSSH2PublicKey(algorithm, Base64.decode(encoded));
	
		        }
				encoded = temp.substring(i);
		        if(!encoded.matches("(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{0,2}==|[A-Za-z0-9+/]{0,3}=)?")) {
		        	throw new IOException("Public key blob does not appear to be base64 encoded data");
		        }
				return SshPublicKeyFileFactory.decodeSSH2PublicKey(algorithm, Base64.decode(encoded));
	    	}
	    }

	    throw new IOException("The format does not appear to be an OpenSSH public key file in the format <algorithm> <base64_blob>");
	}

	public String getComment() {
		return comment;
	}

	public String getOptions() {
		return options;
	}

	public static boolean isFormatted(byte[] formattedkey) {
		
		try {
			String temp = new String(formattedkey);

			BufferedReader r = new BufferedReader(new StringReader(temp));

			String line;
			temp = "";
			while((line = r.readLine())!=null) {
				temp += line;
			}
			
			int i = 0;
			
			while(i > -1) {
			
				int f = i;
				i = temp.indexOf(" ", i);
				
				if(i > -1) {
			    	String algorithm = temp.substring(f, i);
			    	i++;
			    	if(!ComponentManager.getInstance().supportedPublicKeys().contains(algorithm)) {
			    		continue;
			    	}
			      
			        // Get the keyblob end index
			        int i2 = temp.indexOf(" ", i);

			        String encoded;
			        if(i2 !=-1) {
			          encoded = temp.substring(i, i2);

			          Pattern p = Pattern.compile("(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{0,2}==|[A-Za-z0-9+/]{0,3}=)?");
			          if(!p.matcher(encoded).matches()) {
			        	  return false;
			          }
			          return true;

			        }
					encoded = temp.substring(i);
			        if(!encoded.matches("(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{0,2}==|[A-Za-z0-9+/]{0,3}=)?")) {
			          return false;
			        }
					return true;
				}
			}
			
			return false;
		} catch (Throwable e) {
			return false;
		}
	}
}
