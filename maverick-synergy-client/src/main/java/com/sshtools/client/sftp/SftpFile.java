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

package com.sshtools.client.sftp;

import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.UnsignedInteger64;

/**
 * Represents an SFTP file object.
 */
public class SftpFile {
  private final String filename;
  private final Optional<SftpFileAttributes> attrs;
  private final SftpChannel sftp;
  private final String absolutePath;
  private final String longname;
  private final Map<String,Object> properties = new HashMap<>();
  
  private byte[] handle;
  
  /**
   * Creates a new SftpFile object.
   *
   * @param path
   * @param attrs
   */
  @Deprecated(since = "3.1.0", forRemoval = true)
  public SftpFile(String path, SftpFileAttributes attrs) {
	  this(path, attrs, null, null, null);
  }

  SftpFile(String path, SftpFileAttributes attrs, SftpChannel sftp, byte[] handle, String longname) {
      this.attrs = Optional.ofNullable(attrs);
      this.sftp = sftp;
      this.handle = handle;
      this.longname = longname;

      //set filename
      var absolutePath = path;
      if(absolutePath.equals("/")) {
          this.filename = "/";
      } else {

          //Remove trailing forward slash
          if (absolutePath.endsWith("/")) {
              absolutePath = absolutePath.substring(0, absolutePath.length() - 1);
          }

          int i = absolutePath.lastIndexOf('/');

          //if absolutePath contains / then filename= name following last /, else filename=absolutePath
          if (i > -1) {
              this.filename = absolutePath.substring(i + 1);
          } else {
              this.filename = absolutePath;
          }
      }
      this.absolutePath = absolutePath;
  }

  /**
   * Get the parent of the current file. This method determines the correct
   * path of the parent file; if no parent exists (i.e. the current file is
   * the root of the filesystem) then this method returns a null value.
   *
   * @return SftpFile
   * @throws SshException
   * @throws SftpStatusException
   */
  public SftpFile getParent() throws SshException, SftpStatusException {

    if(absolutePath.lastIndexOf('/') == -1) {
      // This is simply a filename so the parent is the users default directory
      String dir = sftp.getDefaultDirectory();
      return sftp.getFile(dir);

    }
	// Extract the filename from the absolute path and return the parent
	String path = sftp.getAbsolutePath(absolutePath);

	if(path.equals("/"))
	  return null;

	// If we have . or .. then strip the path and let getParent start over
	// again with the correct canonical path
	if(filename.equals(".") || filename.equals("..")) {
	  return sftp.getFile(path).getParent();
	}
	int idx = path.lastIndexOf('/');

	  String parent = path.substring(0, idx);

	  // Check if we at the root if so we will have to add /
	  if(parent.equals(""))
	    parent = "/";

	  return sftp.getFile(parent);

  }

  public String toString() {
      return absolutePath;
  }

  public int hashCode() {
    return absolutePath.hashCode();
  }
  
  /**
   * The longname supplied by the server. Note this will not be present if SFTP version is
   * > 3.
   * @return
   */
  public String getLongname() {
	  return longname;
  }

  /**
   * Compares the Object to this instance and returns true if they point to the
   * same file. If they point to the same file but have open file handles, the
   * handles are also used to determine the equality. Therefore two separate
   * instances both pointing to the same file will return true, unless one or both
   * have an open file handle in which case it will only return true if the
   * file handles also match.
   *
   * @param obj
   * @return boolean
   */
  public boolean equals(Object obj) {
    if(obj instanceof SftpFile) {
      boolean match = ((SftpFile)obj).getAbsolutePath().equals(absolutePath);
      if(handle==null && (((SftpFile)obj).handle == null)) {
        return match;
      }
	if(handle!=null && ((SftpFile)obj).handle!=null) {
	  for (int i = 0; i < handle.length; i++) {
	    if ( ( (SftpFile) obj).handle[i] != handle[i])
	      return false;
	  }
	}
	return match;
    }

    return false;
  }

  /**
   * Creates a new SftpFile object.
   *
   * @param absolutePath
   */
/*  public SftpFile(String absolutePath) {
    this(absolutePath, new SftpFileAttributes());
  }*/

  /**
   * Delete this file/directory from the remote server.
   *
   * @throws SshException
   * @throws SftpStatusException
   */
  public void delete() throws SftpStatusException, SshException {
    if (sftp == null) {
      throw new SshException("Instance not connected to SFTP subsystem",
                             SshException.BAD_API_USAGE);
    }

    if (isDirectory()) {
      sftp.removeDirectory(getAbsolutePath());
    }
    else {
      sftp.removeFile(getAbsolutePath());
    }
  }

	/**
	 * <p>
	 * Read bytes directly from this file. This is a low-level operation,
	 * you may only need to use {@link SftpClientTask#get(String)} methods instead if you just want
	 * to download files.
	 * </p>
	 * 
	 * @param offset offset in remote file to read from
	 * @param output output buffer to place read bytes in
	 * @param outputOffset offset in output buffer to write bytes to
	 * @param len number of bytes to read
	 * @return int number of bytes read
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public int read(long offset, byte[] output, int outputOffset, int len) throws SftpStatusException, SshException {
		if(handle == null)
			throw new SftpStatusException(SftpStatusException.SSH_FX_FAILURE);
		return sftp.readFile(handle, new UnsignedInteger64(offset), output, outputOffset, len);
	}
	
	/**
	 * <p>
	 * Write bytes directly to this file. This is a low-level operation,
	 * you may only need to use {@link SftpClientTask#put(String)} methods instead if you just want
	 * to upload files.
	 * </p>
	 * 
	 * @param offset offset in remote file to write to
	 * @param input input buffer to retrieve bytes from to write
	 * @param inputOffset offset in output buffer to write bytes to
	 * @param len number of bytes to write
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void write(long offset, byte[] input, int inputOffset, int len) throws SftpStatusException, SshException {
		if(handle == null)
			throw new SftpStatusException(SftpStatusException.SSH_FX_FAILURE);
		sftp.writeFile(handle, new UnsignedInteger64(offset), input, inputOffset, len);
	}

  /**
   * Determine whether the user has write access to the file. This
   * checks the S_IWUSR flag is set in permissions.
   *
   * @return boolean
   * @throws SftpStatusException
   * @throws SshException
   */
  @Deprecated(since = "3.1.0", forRemoval = true)
  public boolean canWrite() throws SftpStatusException, SshException {
    //  This is long hand because gcj chokes when it is not? Investigate why
	if(getAttributes().getPosixPermissions().has(PosixFilePermission.OWNER_WRITE)) {
      return true;
    }
	return false;
  }

  /**
   * Determine whether the user has read access to the file. This
   * checks the S_IRUSR flag is set in permissions. 
   *
   * @return boolean
   * @throws SftpStatusException
   * @throws SshException
   */
  @Deprecated(since = "3.1.0", forRemoval = true)
  public boolean canRead() throws SftpStatusException, SshException {
    if(getAttributes().getPosixPermissions().has(PosixFilePermission.OWNER_READ)) {
      return true;
    }
	return false;
  }

  /**
   * Determine whether the file is open.
   *
   * @return boolean
   */
  public boolean isOpen() {
    if (sftp == null) {
      return false;
    }

    return sftp.isValidHandle(handle);
  }

  /**
   * Get the open file handle
   *
   * @return byte[]
   */
  public byte[] getHandle() {
    return handle;
  }

  /**
   * Get the SFTP subsystem channel that created this file object.
   *
   * @return SftpSubsystemChannel
   */
  public SftpChannel getSFTPChannel() {
    return sftp;
  }

  /**
   * Get the filename.
   *
   * @return String
   */
  public String getFilename() {
    return filename;
  }

  /**
   * Get the files attributes.
   *
   * @return SftpFileAttributes
   * @throws SshException
   * @throws SftpStatusException
*/
  public SftpFileAttributes getAttributes() throws SftpStatusException, SshException {
    return attrs.isPresent() ? attrs.get() : sftp.getAttributes(getAbsolutePath());
  }

  /**
   * Get the absolute path
   *
   * @return String
   */
  public String getAbsolutePath() {
    return absolutePath;
  }

  /**
   * Close the file.
   *
   * @throws SshException
   * @throws SftpStatusException
   */
  public void close() throws SftpStatusException, SshException {
	if (handle != null) {
		sftp.closeHandle(handle);
		EventServiceImplementation.getInstance().fireEvent(
				(new Event(this, EventCodes.EVENT_SFTP_FILE_CLOSED,
						true)).addAttribute(
						EventCodes.ATTRIBUTE_FILE_NAME,
						getAbsolutePath()));
		handle = null;
	}
  }

  /**
   * Determine whether the file object is pointing to a directory. Note,
   * if the file is a symbolic link pointing to a directory then <code>false</code>
   * will be returned. Use {@link com.sshtools.sftp.SftpClient#isDirectoryOrLinkedDirectory(SftpFile)} instead if you
   * wish to follow links.
   *
   * @return is directory
   * @throws SshException on SSH error
   * @throws SftpStatusException on SFTP error
   */
  public boolean isDirectory() throws SftpStatusException, SshException {
    return getAttributes().isDirectory();
  }

  /**
   * Determine whether the file object is pointing to a file.
   *
   * @return is file
   * @throws SshException on SSH error
   * @throws SftpStatusException on SFTP error
  */
  public boolean isFile() throws SftpStatusException, SshException {
    return getAttributes().isFile();
  }

  /**
   * Determine whether the file object is a symbolic link.
   *
   * @return is link
   * @throws SshException on SSH error
   * @throws SftpStatusException on SFTP error
   */
  public boolean isLink() throws SftpStatusException, SshException {
    return getAttributes().isLink();
  }

  /**
   * Determine whether the file is pointing to a pipe.
   *
   * @return is fifo
   * @throws SshException on SSH error
   * @throws SftpStatusException on SFTP error
   */
  public boolean isFifo() throws SftpStatusException, SshException {
    return getAttributes().isFifo();
  }

  /**
   * Determine whether the file is pointing to a block special file.
   *
   * @return is block special file
   * @throws SshException on SSH error
   * @throws SftpStatusException on SFTP error
   */
  public boolean isBlock() throws SftpStatusException, SshException {
    return getAttributes().isBlock();
  }

  /**
   * Determine whether the file is pointing to a character mode device.
   *
   * @return is character mode device
   * @throws SshException on SSH error
   * @throws SftpStatusException on SFTP error
   */
  public boolean isCharacter() throws SftpStatusException, SshException {
    return getAttributes().isCharacter();
  }

  /**
   * Determine whether the file is pointing to a socket.
   *
   * @return is socket file
   * @throws SshException on SSH error
   * @throws SftpStatusException on SFTP error
   */
  public boolean isSocket() throws SftpStatusException, SshException {
	  return getAttributes().isSocket();
  }

  public void setProperty(String key, Object value) {
	  properties.put(key, value);
  }
  
  public Object getProperty(String key) {
	  return properties.get(key);
  }
}
