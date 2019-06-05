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

package com.sshtools.client.tasks;

/**
 * <p>Interface for monitoring the state of a file transfer</p>
 *
 * <p>It should be noted that the total bytes to transfer passed to the
 * started method is an indication of file length and may not be exact for
 * some types of file transfer, for example ASCII text mode transfers may add
 * or remove newline characters from the stream and therefore the total bytes
 * transfered may not equal the number expected.
 *
 * 
 */
public interface FileTransferProgress {
  /**
   * The transfer has started
   *
   * @param bytesTotal
   * @param remoteFile
   */
  public void started(long bytesTotal, String remoteFile);

  /**
   * The transfer is cancelled. Implementations should return true if the
   * user wants to cancel the transfer. The transfer will then be stopped
   * at the next evaluation stage.
   *
   * @return boolean
   */
  public boolean isCancelled();

  /**
   * The transfer has progressed
   *
   * @param bytesSoFar
   */
  public void progressed(long bytesSoFar);

  /**
   * The transfer has completed.
   */
  public void completed();
}
