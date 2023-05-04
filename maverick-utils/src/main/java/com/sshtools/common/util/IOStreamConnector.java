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

package com.sshtools.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Vector;

/**
 *  Connects an input stream to an outputstream.
 *  Reads from in stream and writes to out stream.
 *
 * @author Lee David Painter
 */
public class IOStreamConnector {

  private InputStream in = null;
  private OutputStream out = null;
  private Thread thread;
  private long bytes;
  private boolean closeInput = true;
  private boolean closeOutput = true;
  boolean running = false;
  boolean closed = false;
  Throwable lastError;
  public static final int DEFAULT_BUFFER_SIZE = 32768;
  int BUFFER_SIZE = DEFAULT_BUFFER_SIZE;

  /**  */
  protected Vector<IOStreamConnectorListener> listenerList = new Vector<IOStreamConnectorListener>();

  /**
   * Creates a new IOStreamConnector object.
   */
  public IOStreamConnector() {
  }

  /**
   * Creates a new IOStreamConnector object.
   *
   * @param in
   * @param out
   */
  public IOStreamConnector(InputStream in, OutputStream out) {
    connect(in, out);
  }

  /**
   *
   *
   * @return
   */
  /* public IOStreamConnectorState getState() {
     return state;
   }*/

  public void close() {
    if(thread==null) {
    	closed=true;
    }
    
	running = false;

    if (thread != null) {
      thread.interrupt();
    }
  }

  /**
   * 
   * @return IOException
   */
  public Throwable getLastError() {
    return lastError;
  }

  /**
   *
   *
   * @param closeInput
   */
  public void setCloseInput(boolean closeInput) {
    this.closeInput = closeInput;
  }

  /**
   *
   *
   * @param closeOutput
   */
  public void setCloseOutput(boolean closeOutput) {
    this.closeOutput = closeOutput;
  }

  public void setBufferSize(int numbytes) {
    if (numbytes <= 0) {
      throw new IllegalArgumentException(
          "Buffer size must be greater than zero!");
    }

    BUFFER_SIZE = numbytes;
  }

  /**
   *
   *
   * @param in
   * @param out
   */
  public void connect(InputStream in, OutputStream out) {
    this.in = in;
    this.out = out;

    thread = new Thread(new IOStreamConnectorThread());
    thread.setDaemon(true);
    thread.setName("IOStreamConnector " + in.toString() + ">>" + out.toString());
    thread.start();
  }

  /**
   *
   *
   * @return long
   */
  public long getBytes() {
    return bytes;
  }

  public boolean isClosed() {
    return closed;
  }

  /**
   *
   *
   * @param l
   */
  public void addListener(IOStreamConnectorListener l) {
    listenerList.addElement(l);
  }

  /**
   *
   *
   * @param l
   */
  public void removeListener(IOStreamConnectorListener l) {
    listenerList.removeElement(l);
  }

  class IOStreamConnectorThread
      implements Runnable {

    public void run() {
      byte[] buffer = new byte[BUFFER_SIZE];
      int read = 0;
      running = true;

      
      while (running) {
        try {
          // Block
          read = in.read(buffer, 0, buffer.length);

          if (read > 0) {

            // Write it
            out.write(buffer, 0, read);

            // Record it
            bytes += read;

            // Flush it
            out.flush();

            // Inform all of the listeners
            for (int i = 0; i < listenerList.size(); i++) {
              ( (IOStreamConnectorListener) listenerList.elementAt(i)).
                  dataTransfered(buffer, read);
            }
          }
          else {
            if (read < 0) {
              running = false;
            }
          }
        }
        catch(InterruptedIOException ex) {
            for (int i = 0; i < listenerList.size(); i++) {
                ( (IOStreamConnectorListener) listenerList.elementAt(i)).
                    connectorTimeout(IOStreamConnector.this);
              }
        } catch (Throwable ioe) {
          // only log the error if were supposed to be connected
          if (running) {
            lastError = ioe;
            running = false;
          }

        }
      }

      if (closeInput) {
        try {
          in.close();
        }
        catch (IOException ex) {}
      }

      if (closeOutput) {
        try {
          out.close();
        }
        catch (IOException ex) {}
      }

      closed = true;

      for (int i = 0; i < listenerList.size(); i++) {
        ( (IOStreamConnectorListener) listenerList.elementAt(i)).
            connectorClosed(IOStreamConnector.this);
      }

      thread = null;

    }
  }

  public interface IOStreamConnectorListener {
    public void connectorClosed(IOStreamConnector connector);
    
    public void connectorTimeout(IOStreamConnector connector);

    public void dataTransfered(byte[] data, int count);
  }

}
