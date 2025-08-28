package com.sshtools.client.sftp;

/*-
 * #%L
 * Client API
 * %%
 * Copyright (C) 2002 - 2025 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.Objects;

import com.sshtools.common.sftp.SftpFileAttributes;

/**
 * Represents an SFTP file object.
 */
public final class SftpFile {
	
  private final String filename;
  private final String absolutePath;
  private final String longname;
  private final SftpFileAttributes attrs;
  
  SftpFile(String path, SftpFileAttributes attrs, String longname) {
	  if(path == null || attrs == null)
		  throw new NullPointerException();
	  
      this.attrs = attrs;
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
   * Get the attributes for this file as they were when this file object was obtained. To
   * get the latest attributes, call {@link #refresh()} to obtain a new {@link SftpFile} instance.
   * 
   * @return attributes
   */
  public SftpFileAttributes attributes() {
	  return attrs;
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
}
