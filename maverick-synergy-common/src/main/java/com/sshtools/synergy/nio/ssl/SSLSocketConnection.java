package com.sshtools.synergy.nio.ssl;

/*-
 * #%L
 * Common API
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import com.sshtools.common.logger.Log;
import com.sshtools.synergy.nio.SelectorThread;
import com.sshtools.synergy.nio.SocketConnection;
import com.sshtools.synergy.nio.SocketWriteCallback;

/**
 * This class implements an SSL socket connection for the custom server.
 * Using the Java 1.5 SSLEngine class this implementation wraps all protocol
 * traffic into SSL records.
 */
public class SSLSocketConnection extends SocketConnection {

    SSLContext sslContext = null;

    private static String[] protocols;
    private static String[] cipherSuites;
    
    /**
     * SSL variables
     */
    @SuppressWarnings("unused")
	private SSLSession session;
    private SSLEngine engine;
    private ByteBuffer dummy;
    private SSLEngineResult.HandshakeStatus hsStatus;
    private SSLEngineResult.Status status;
    private boolean initialHandshake;
    private boolean wantsWrite = false;
    ByteBuffer sourceBuffer;
    ByteBuffer destinationBuffer;

    

    LinkedList<SocketWriteCallback> socketWriteCallbacks = new LinkedList<SocketWriteCallback>();
    boolean requireClientCertificate;
    boolean allowClientCertificate; 
    
    /**
     * Default constructor. We need one of these so we can dynamically create
     * a SocketConnection on demand.
     */
    public SSLSocketConnection(SSLContext context) {
    	this(context, true, false);
    }
    
    public SSLSocketConnection(SSLContext context, boolean allowClientCertificate, boolean requireClientCertificate) {
    	this.sslContext = context;
    	this.allowClientCertificate = allowClientCertificate;
    	this.requireClientCertificate = requireClientCertificate;
    }

    /**
     * This method is called once the socket is registered with a SelectorThread. At
     * this point we're ready to start transfering data.
     *
     * @param channel SelectableChannel
     * @param key SelectionKey
     * @param selectorThread SelectorThread
     */
    public void registrationCompleted(SelectableChannel channel,
                                      SelectionKey key,
                                      SelectorThread selectorThread) {
          this.socketChannel = (SocketChannel)channel;
          this.selectorThread = selectorThread;
          this.key = key;

          // Initialize SSL
          try {
              // Create an SSLEngine to use
              engine = sslContext.createSSLEngine();

              if (protocols != null)
              {
                  engine.setEnabledProtocols(protocols);
              }

              if (cipherSuites != null)
              {
                  engine.setEnabledCipherSuites(cipherSuites);
              }
              
              // Duh! we're the server
              engine.setUseClientMode(false);

              engine.setWantClientAuth(allowClientCertificate);
              engine.setNeedClientAuth(requireClientCertificate);
              
              // Get the session and begin the handshake
              session = engine.getSession();
              engine.beginHandshake();
              hsStatus = engine.getHandshakeStatus();
              initialHandshake = true;

              // This is a dummy byte buffer which is used during initial
              // handshake negotiation
              dummy = ByteBuffer.allocate(0);

              // We're going to want to write something
              wantsWrite = true;

          } catch(Exception ex) {
              closeConnection();
          }
    }

    /**
     * Is the socket still connected? During the initial handshake check the raw
     * socket status, otherwise check the protocol status.
     * @return boolean
     */
    public boolean isConnected() {
        if(initialHandshake) {
            return socketChannel.isOpen();
        }
		return super.isConnected();
    }

    /**
     * Shutdown the SSL socket.
     */
    private void shutdown() throws SSLException {
        engine.closeInbound();
        engine.closeOutbound();
        closeConnection();
    }

    /**
     * This method is called when new network data arrives on the socket. We
     * have to unwrap any SSL traffic into raw application data
     *
     * @param applicationData ByteBuffer
     */
    public boolean processReadEvent() {

        /* DEBUG */////Log.trace("Processing socket READ event");

                try {

                  if (!isConnected()) {
                    return true;
                  }

                  // Get a byte buffer if needed
                  if(socketDataIn==null)
                      socketDataIn = daemonContext.getBufferPool().get();

                  /* DEBUG */////Log.trace("Socket buffer has " + socketDataIn.remaining() + " bytes remaining");

                  int numBytesRead;

                 // do {
                      numBytesRead = socketChannel.read(socketDataIn);
                      socketDataIn.flip();

                      /* DEBUG */////Log.trace("Read " + numBytesRead + " bytes from socket");

                      // Check for EOF from the socket
                      if (numBytesRead == -1) {
                          /* DEBUG */////Log.trace("Received EOF from remote host");
                          shutdown();
                          return true;
                      } else if (numBytesRead > 0) {

                          /* DEBUG */////Log.trace("Unwrapping SSL");

                          // Get a spare buffer
                          if (destinationBuffer == null)
                              destinationBuffer = daemon.getContext().getBufferPool().get();

                          // Record the current position in the buffer
                          int currentDestinationPos = destinationBuffer.position();
                          int remaining = socketDataIn.remaining();
                          int noUnwrap = 0;
                          
                          do {

                              SSLEngineResult res;

                              /**
                               * Unwrap data from the socket
                               */
                              do {
                                  res = engine.unwrap(socketDataIn, destinationBuffer);

                                  if(remaining == socketDataIn.remaining()) {
                                	  noUnwrap++;
                                	  if(noUnwrap > 50) {
                                		  shutdown();
                                		  return true;
                                	  }
                                  } else {
                                	  noUnwrap = 0;
                                	  remaining = socketDataIn.remaining();
                                  }
                                  
                                  destinationBuffer.flip();

                                  if (destinationBuffer.hasRemaining() && !initialHandshake)
                                      protocolEngine.onSocketRead(destinationBuffer);

                                  destinationBuffer.compact();

                              } while (res.getStatus() == SSLEngineResult.Status.OK
                                       &&
                                       res.getHandshakeStatus() ==
                                       SSLEngineResult.HandshakeStatus.NEED_UNWRAP
                                       && res.bytesProduced() == 0);

                              /**
                               * Check for a completed SSL handshake
                               */
                              /*
                               REMOVED: This could cause multiple finished events to be fired!
                               if (res.getHandshakeStatus() ==
                                  SSLEngineResult.HandshakeStatus.FINISHED)
                                  finishInitialHandshake();
                               */

                              /**
                               * We might have some outstanding socket data so process it
                               */
                              if (destinationBuffer.position() == currentDestinationPos
                                  && socketDataIn.hasRemaining()) {
                                  res = engine.unwrap(socketDataIn, destinationBuffer);

                                  destinationBuffer.flip();

                                  if (destinationBuffer.hasRemaining() && !initialHandshake)
                                      protocolEngine.onSocketRead(destinationBuffer);

                                  destinationBuffer.compact();
                              }

                              /**
                               * Record the SSL status
                               */
                              status = res.getStatus();
                              hsStatus = res.getHandshakeStatus();

                              /**
                               * Check that the engine hasn't closed
                               */
                              if (status == SSLEngineResult.Status.CLOSED) {
                                  shutdown();
                                  return true;
                              }

                              /**
                               * If we have a handshake status lets process it
                               */
                              if (hsStatus == SSLEngineResult.HandshakeStatus.NEED_TASK
                                  || hsStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP
                                  || hsStatus == SSLEngineResult.HandshakeStatus.FINISHED)
                                  doHandshake();

                          } while (socketDataIn.hasRemaining()
                                   && status != SSLEngineResult.Status.BUFFER_UNDERFLOW);

                      }

                      socketDataIn.compact();

                // } while(numBytesRead > 0);

                  /* DEBUG */ if (!isConnected()) {
                  /* DEBUG */////Log.trace("Connection is closed, cancelling selectors");
                  /* DEBUG */}

                  return!isConnected();
                }
                catch (Throwable ex) {
                  Log.error("An error occured whilst trying to read from the socket", ex);
                  closeConnection();
                  return true;
                } finally {

                    /**
                     * Make sure the buffers are put back to the pool
                     */
                    if (destinationBuffer != null) {
                        if (destinationBuffer.remaining()==destinationBuffer.capacity()) {
                            daemonContext.getBufferPool().add(destinationBuffer);
                            destinationBuffer = null;
                        }
                    }
                    if (socketDataIn != null) {
                        if (socketDataIn.remaining()==socketDataIn.capacity()) {
                            daemonContext.getBufferPool().add(socketDataIn);
                            socketDataIn = null;
                        }
                    }
        }


    }

    /**
     * Called when the initial handshake is complete. Here we can tell the
     * protocol engine that its connected.
     */
    private void finishInitialHandshake() {
        initialHandshake = false;
        /* DEBUG */Log.debug("Completed handshake");
        protocolEngine.onSocketConnect(this);
    }

    /**
     * Perform initial handshake tasks.
     * @throws SSLException
     */
    private void doHandshake() throws SSLException {

            /**
             * Perform the initial handshake operation
             */
            while(true) {

            switch (hsStatus) {
            case FINISHED:
                /**
                 * We have completed the handshake so lets start doing the real stuff
                 */
                /* DEBUG */////Log.trace("SSL Handshake finished");
                finishInitialHandshake();
                return;

            case NEED_TASK:

                /**
                 * The SSL engine needs to perform a task... do it!
                 */
                /* DEBUG */////Log.trace("Performing SSLEngine task");

                // Execute the tasks
                Runnable task;
                while ((task = engine.getDelegatedTask()) != null) {
                    task.run();
                }
                hsStatus = engine.getHandshakeStatus();
                break;

            case NEED_UNWRAP:
            case NEED_UNWRAP_AGAIN:

                /**
                 * SSL engine wants more data to make sure we're reading from
                 * the socket
                 */
            	wantsWrite = false;
                return;

            case NEED_WRAP:

                /**
                 * SSL engine wants to write data to the socket so make sure
                 * we can write to it
                 */
            	wantsWrite = true;
                return;

            case NOT_HANDSHAKING:
                /**
                 * This state should never be caught here
                 */
                Log.error("doHandshake has caught a NOT_HANDSHAKING state.. This is impossible!");
                return;
            }
        }
    }

    /**
     * Flush any data remaining in internal buffers to the socket
     * @throws IOException
     */
    private void flush() throws IOException {

        if(socketDataOut!=null) {
            try {
                    socketChannel.write(socketDataOut);
                } catch(IOException ex) {
                    closeConnection();
                }
        }
    }

    /**
     * Write any application data to the socket by wrapping it up into the
     * SSL protocol.
     *
     * @param applicationData ByteBuffer
     * @todo Implement this com.maverick.nio.SSLProtocolConnection2 method
     */
    public boolean processWriteEvent() {


        /* DEBUG *///Log.trace("Processing socket WRITE event");

        if(socketChannel==null || !socketChannel.isOpen())
            return true;

        if (socketDataOut == null) {
            socketDataOut = daemonContext.getBufferPool().get();
        }

        try {

            // Check before we send that the connection hasn't been closed
            if(!socketChannel.isOpen())
                return true;

            if(initialHandshake) {

                    /**
                     * We're in the initial handshake operation so lets write
                     * any data and see what doHandshake needs to do.
                     */
                    if(hsStatus==SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                            SSLEngineResult res = engine.wrap(dummy, socketDataOut);
                            hsStatus = res.getHandshakeStatus();
                            socketDataOut.flip();
                            flush();
                        }

                        doHandshake();

                        if(hsStatus!=SSLEngineResult.HandshakeStatus.NEED_WRAP
                           && hsStatus!=SSLEngineResult.HandshakeStatus.FINISHED)
                            wantsWrite = false;
                    } else {

                        /**
                         * We're in application mode, read data from the
                         * protocol engine and wrap in SSL
                         */
                        if (sourceBuffer == null)
                            sourceBuffer = daemon.getContext().getBufferPool().
                                           get();

                        // If we don't have any data left in the buffer get
                        // some more from the protocol engine.
                        if (sourceBuffer.remaining() == sourceBuffer.limit()
                            && protocolEngine.isConnected()) {

                    		SocketWriteCallback c = protocolEngine.onSocketWrite(sourceBuffer);
                    		if(c!=null)
                        	 socketWriteCallbacks.addLast(c);
                        }

                        sourceBuffer.flip();

                        SSLEngineResult res = engine.wrap(sourceBuffer, socketDataOut);

                        status = res.getStatus();
                        hsStatus = res.getHandshakeStatus();

                        // Prepare the buffer for reading
                        socketDataOut.flip();

                        flush();

                        if(protocolEngine.wantsToWrite() || sourceBuffer.hasRemaining() || socketDataOut.hasRemaining()) {
                        	wantsWrite = true;
                        } else {
                        	wantsWrite = false;
                        }

                    }


                    return!isConnected();

        } catch (Throwable ex) {
            Log.error("An error occured whilst trying to write to the socket", ex);
            closeConnection();
            return true;
        } finally {

            /**
             * Make sure that the buffers are put back to the pool
             */
            if (sourceBuffer != null) {
                if (!sourceBuffer.hasRemaining() || (sourceBuffer.remaining()==sourceBuffer.capacity())) {
                    daemonContext.getBufferPool().add(sourceBuffer);
                    sourceBuffer = null;
                } else
                    sourceBuffer.compact();
            }

            if (socketDataOut != null) {
                if (!socketDataOut.hasRemaining() || (socketDataOut.remaining()==socketDataOut.capacity())) {
                    daemonContext.getBufferPool().add(socketDataOut);
                    socketDataOut = null;
                    
                    for(Iterator<SocketWriteCallback> it = socketWriteCallbacks.iterator(); it.hasNext() ;) {
                    	it.next().completedWrite();
                    }
                    socketWriteCallbacks.clear();
                } else
                    socketDataOut.compact();
            }
        }
    }

	@Override
	public boolean wantsWrite() {
		return wantsWrite;
	}
    
    public static void setEnabledProtocols(String[] aProtocols)
    {
        protocols = aProtocols;
    }

    public static void setEnabledCipherSuites(String[] aCipherSuites)
    {
        cipherSuites = aCipherSuites;
    }
}
