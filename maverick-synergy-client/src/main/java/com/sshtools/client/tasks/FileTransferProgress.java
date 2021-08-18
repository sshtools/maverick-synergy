/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
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
