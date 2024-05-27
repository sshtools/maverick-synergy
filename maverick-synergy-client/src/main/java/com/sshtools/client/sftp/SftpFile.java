package com.sshtools.client.sftp;

import java.util.Objects;

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
  private SftpFileAttributes attrs;
  
  SftpFile(String path, SftpFileAttributes attrs, SftpChannel sftp, String longname) {
	  if(path == null || attrs == null || sftp == null)
		  throw new NullPointerException();
	  
      this.attrs = attrs;
      this.sftp = sftp;
      this.longname = longname;

      //set filename
      var absolutePath = path;
      if(absolutePath.equals("/")) {
          this.filename = "/";
      } else {

          //Remove trailing forward slash
          if (!Boolean.getBoolean("maverick.disableSlashRemoval") && absolutePath.endsWith("/")) {
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
   * Create a new handle for this file given the handle data.
   * 
   * @param handle
   */
  public SftpHandle handle(byte[] handle) {
	  return new SftpHandle(handle, sftp, this);
  }
  
  /**
   * Get the attributes for this file as they were when this file object was obtained. To
   * get the latest attributes, call {@link #refresh()} to obtain a new {@link SftpFile} instance.
   * 
   * @return attributes
   */
  public SftpFileAttributes attributes() {
	  return attrs;
  }
  
 /**
  * Set the given attributes on the remote file represented by this {@link SftpFile}.
  * 
  * @param attributes
  * @return this
  * @throws SftpStatusException
  * @throws SshException
  */
  public SftpFile attributes(SftpFileAttributes attributes) throws SftpStatusException, SshException {
	  	sftp.setAttributes(absolutePath, attributes);
	  	this.attrs = attributes;
		return this;
  }
 
	/**
	 * Refresh the {@link SftpFileAttributes} from the the remote file. 
	 * 
	 * @return file new file instance
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public SftpFile refresh() throws SftpStatusException, SshException {
		attrs = sftp.getAttributes(absolutePath);
		return this;
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

    if (attrs.isDirectory()) {
      sftp.removeDirectory(getAbsolutePath());
    }
    else {
      sftp.removeFile(getAbsolutePath());
    }
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
   * Get the absolute path
   *
   * @return String
   */
  public String getAbsolutePath() {
    return absolutePath;
  }
  
  SftpHandle openFile(int flags) throws SftpStatusException, SshException {
		return sftp.openFile(absolutePath, flags);
  }
}
