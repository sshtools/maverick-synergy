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
import java.util.Objects;
import java.util.StringTokenizer;

import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

/**
 * Represents an SFTP file object.
 */
public final class SftpFile {
  private final String filename;
  private final SftpChannel sftp;
  private final String absolutePath;
  private final String longname;
  private final Map<String,Object> properties = new HashMap<>();
  
  SftpFile(String path, SftpChannel sftp, String longname) {
      this.sftp = sftp;
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

  
  @Override
  public int hashCode() {
	  return Objects.hash(absolutePath);
  }

  @Override
  public boolean equals(Object obj) {
	  if (this == obj)
		  return true;
	  if (obj == null)
		  return false;
	  if (getClass() != obj.getClass())
		  return false;
	  SftpFile other = (SftpFile) obj;
	  return Objects.equals(absolutePath, other.absolutePath);
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
   * Set the files attributes.
   *
   * @param attributes attributes
   * @return SftpFileAttributes
   * @throws SshException
   * @throws SftpStatusException
   */
  @SuppressWarnings("deprecation")
  public void setAttributes(SftpFileAttributes attributes) throws SftpStatusException, SshException {
	  sftp.setAttributes(absolutePath, attributes);
  }

  /**
   * Get the files attributes.
   *
   * @return SftpFileAttributes
   * @throws SshException
   * @throws SftpStatusException
   */
  public SftpFileAttributes getAttributes() throws SftpStatusException, SshException {
    SftpFileAttributes attrs = sftp.getAttributes(getAbsolutePath());

	// Work out username/group from long name
	if (longname != null && sftp.version <= 3) {
		try {
			StringTokenizer t = new StringTokenizer(longname);
			t.nextToken();
			t.nextToken();
			String username = t.nextToken();
			String group = t.nextToken();

			attrs.setUsername(username);
			attrs.setGroup(group);

		} catch (Exception e) {
		}
	}
	
	return attrs;
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

  /** 
   * Set an arbitrary property in this file object.
   * <p> 
   * Deprecated, no replacement.
   * 
   * @param key key
   * @param value vlaue
   * @deprecated
   */
  @Deprecated(since = "3.1.0", forRemoval = true)
  public void setProperty(String key, Object value) {
	  properties.put(key, value);
  }

  /** 
   * Get an arbitrary property stored in this file object.
   * <p> 
   * Deprecated, no replacement.
   * 
   * @param key key
   * @return value
   * @deprecated
   */
  @Deprecated(since = "3.1.0", forRemoval = true)
  public Object getProperty(String key) {
	  return properties.get(key);
  }
  
  SftpHandle openFile(int flags) throws SftpStatusException, SshException {
		return sftp.openFile(absolutePath, flags);
  }
}
