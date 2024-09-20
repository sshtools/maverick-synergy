package com.sshtools.client.tasks;

/*-
 * #%L
 * Client API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
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
   * @param file
   */
  default void started(long bytesTotal, String file) { } ;

  /**
   * The transfer is cancelled. Implementations should return true if the
   * user wants to cancel the transfer. The transfer will then be stopped
   * at the next evaluation stage.
   *
   * @return boolean
   */
  default boolean isCancelled() { return false; };

  /**
   * The transfer has progressed
   *
   * @param bytesSoFar
   */
  default void progressed(long bytesSoFar) { };

  /**
   * The transfer has completed.
   */
  default void completed() { };
}
