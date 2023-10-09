package com.sshtools.common.auth;

import java.io.IOException;
import java.util.Iterator;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.publickey.SshPublicKeyFile;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

/**
 * 
 * <p>
 * This interface allows you to define a custom public key store for the
 * publickey authentication mechanism. The actual key verification is performed
 * by the mechanism itself and your only requirement is to return a value which
 * indicates whether the key has been authorized by the user for public key
 * access.
 * </p>
 * <p>
 * The implementation may optionally support addition, removal and listing of
 * keys. When supported, {@link PublicKeySubsystemServer} may be used. If not required
 * the methods should thrown {@link UnsupportedOperationException}.
 * </p>
 * 
 * @author Lee David Painter
 */
public interface PublicKeyAuthenticationProvider extends Authenticator {

	/**
	 * <p>
	 * Check the supplied public key against the users authorized keys. The
	 * actual verification of the key is performed by the server, you only need
	 * to return a value to indicate whether the key is authorized or not. You
	 * can obtain the username, home directory, group or remote socket address
	 * from the {@link com.sshtools.common.auth.server.PasswordAuthenticationProvider}
	 * instance.
	 * </p>
	 * 
	 * <p>
	 * If your authorized key database is kept on the native file system you can
	 * obtain and initialize an instance as follows:<br>
	 * <blockquote>
	 * 
	 * <pre>
	 * NativeFileSystemProvider nfs = (NativeFileSystemProvider) authenticationProvider
	 * 		.getContext().getFileSystemProvider().newInstance();
	 * 
	 * nfs.init(sessionid, null, authenticationProvider.getContext());
	 * </pre>
	 * 
	 * </blockquote> Don't forget to close any file handles and the file system
	 * once you've done accessing files.
	 * 
	 * @param key
	 *            SshPublicKey
	 * @param con
	 *            connection
	 * @param authenticationProvider
	 *            AuthenticationProvider
	 * @return boolean <tt>true</tt> if the key is an authorized key, otherwise
	 *         <tt>false</tt>
	 */
	boolean isAuthorizedKey(SshPublicKey key, SshConnection con) throws IOException;

	boolean checkKey(SshPublicKey key, SshConnection con) throws IOException;
	
	Iterator<SshPublicKeyFile> getKeys(SshConnection con)
			throws PermissionDeniedException, IOException;

	void remove(SshPublicKey key, SshConnection con) throws IOException,
			PermissionDeniedException, SshException;

	void add(SshPublicKey key, String comment, SshConnection con)
			throws IOException, PermissionDeniedException, SshException;

}
