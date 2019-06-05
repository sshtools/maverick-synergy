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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sshtools.client.sftp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.util.UnsignedInteger32;

/**
 * An OutputStream to write data to a remote file.
 */
public class SftpFileOutputStream
    extends OutputStream {
  SftpFile file;
  AbstractSftpTask sftp;
  long position;
  Vector<UnsignedInteger32> outstandingRequests = new Vector<UnsignedInteger32>();
  boolean error = false;
  
  /**
   * Creates a new SftpFileOutputStream object.
   *
   * @param file
   *
   * @throws SftpStatusException
   * @throws SshException
   */
  public SftpFileOutputStream(SftpFile file) throws SftpStatusException, SshException {
    if (file.getHandle() == null) {
      throw new SftpStatusException(SftpStatusException.INVALID_HANDLE,
                                    "The file does not have a valid handle!");
    }

    if (file.getSFTPChannel() == null) {
      throw new SshException(
          "The file is not attached to an SFTP subsystem!",
          SshException.BAD_API_USAGE);
    }

    this.file = file;
    this.sftp = file.getSFTPChannel();
  }

  /**
   *
   */
  public void write(byte[] buffer, int offset, int len) throws IOException {
    try {

        int count;
        while(len > 0) {

            count = Math.min(32768, len);

            // Post a request
            outstandingRequests.addElement(sftp.postWriteRequest(file.getHandle(),
                    position,
                    buffer, offset, count));

            processNextResponse(100);

            // Update our positions
            offset += count;
            len -= count;
            position += count;
        }

    }
    catch(SshException ex) {
      throw new SshIOException(ex);
    }
    catch(SftpStatusException ex) {
      throw new IOException(ex.getMessage());
    }

  }

  /**
   *
   */
  public void write(int b) throws IOException {
      try {


          byte[] array = new byte[] { (byte)b };

          // Post a request
          outstandingRequests.addElement(sftp.postWriteRequest(file.getHandle(),
                  position,
                  array, 0, 1));

          processNextResponse(100);

          // Update our positions
          position += 1;


      }
      catch(SshException ex) {
        throw new SshIOException(ex);
      }
      catch(SftpStatusException ex) {
        throw new IOException(ex.getMessage());
    }
  }

  private boolean processNextResponse(int numOutstandingRequests) throws SftpStatusException, SshException {
      try {
		// Maybe look for a response
		  if (outstandingRequests.size() > numOutstandingRequests) {
		      UnsignedInteger32 requestid = (UnsignedInteger32)
		                                    outstandingRequests.
		                                    elementAt(0);
		      sftp.getOKRequestStatus(requestid);
		      outstandingRequests.removeElementAt(0);
		  }

		  return outstandingRequests.size() > 0;
	} catch (SshException e) {
		error = true;
		throw e;
	} catch(SftpStatusException e) {
		error = true;
		throw e;
	}
  }

  /**
   * Closes the file's handle
   */
  public void close() throws IOException {
    try {
      while(!error && processNextResponse(0));
      file.close();
    }
    catch(SshException ex) {
      throw new SshIOException(ex);
    }
    catch(SftpStatusException ex) {
      throw new IOException(ex.getMessage());
    }
  }

  /**
   * This method will only be available in J2SE builds
   */
  // J2SE protected void finalize() throws IOException {
  // J2SE  if (file.getHandle() != null) {
  // J2SE   close();
  // J2SE  }
  // J2SE }
}
