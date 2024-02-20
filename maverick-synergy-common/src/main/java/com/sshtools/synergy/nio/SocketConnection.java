package com.sshtools.synergy.nio;

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
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;

import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.IdleStateManager;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.ExecutorOperationQueues;
import com.sshtools.synergy.ssh.Connection;
import com.sshtools.synergy.ssh.SshContext;

/**
 * This class implements a {@link SocketHandler} using a standard socket.
 */
public class SocketConnection implements SocketHandler {
	
    private static final Integer SOCKET_QUEUE = ExecutorOperationQueues.generateUniqueQueue("SocketConnection.in");
    
	protected SocketChannel socketChannel;
    protected ProtocolEngine protocolEngine;
    protected SshEngineContext daemonContext;
    protected SelectorThread selectorThread;
    protected SelectionKey key;
    protected SshEngine daemon;

    protected ByteBuffer socketDataIn;
    protected ByteBuffer socketDataOut;

    protected boolean closed;

    boolean hasInterestedOps = false;

    int currentInterestedOps = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    int pendingRemoveOps = 0;
    int pendingIncludeOps = 0;

    Object opsLock = new Object();
    LinkedList<SocketWriteCallback> socketWriteCallbacks = new LinkedList<SocketWriteCallback>();

	private SocketAddress remoteAddress;

	private int remotePort;

	private int localPort;

	private SocketAddress localAddress;

    /**
     * Construct the socket.
     */
    public SocketConnection() {
    }

    /**
     * Initialize the socket with the protocol engine and the daemon process.
     *
     * @param protocolEngine ProtocolEngine
     * @param daemon Daemon
     * @throws IOException 
     */
    public void initialize(ProtocolEngine protocolEngine, SshEngine daemon, SelectableChannel channel) throws IOException {
        this.protocolEngine = protocolEngine;
        this.daemon = daemon;
        this.daemonContext = daemon.getContext();
        this.socketChannel = (SocketChannel)channel;
        this.localAddress = socketChannel.getLocalAddress();
        this.localPort = socketChannel.socket().getLocalPort();
        this.remoteAddress = socketChannel.getRemoteAddress();
        this.remotePort = socketChannel.socket().getPort();
        
    }

    /**
     * The sockets channel has completed registration.
     *
     * @param channel SelectableChannel
     * @param key SelectionKey
     * @param selectorThread SelectorThread
     * @throws IOException 
     */
    public void registrationCompleted(SelectableChannel channel,
                                      SelectionKey key,
                                      SelectorThread selectorThread) throws IOException {
          
          this.selectorThread = selectorThread;
          this.key = key;
          protocolEngine.onSocketConnect(this);
    }
    
    public void setSelectionKey(SelectionKey key) {
    	this.key = key;
    	hasInterestedOps = false;
    }
    
    /**
     * Set a new ProtocolEngine to handle this sockets data.
     * @param protocolEngine
     */
    public void setProtocolEngine(ProtocolEngine protocolEngine) {
    	this.protocolEngine = protocolEngine;
    }

    /**
     * Close this socket connection.
     */
    public void closeConnection() {
    	 closeConnection(true);
    }
    
    public void closeConnection(boolean closeProtocol) {

        if(!closed) {
            if (socketChannel != null && socketChannel.isOpen()) {
                try {
                    if(Log.isTraceEnabled()) {
                    	Log.trace("Closing socket");
                    }
                    socketChannel.close();
                } catch (IOException ex) {
                }
            }

            if(closeProtocol) {
	            if(Log.isTraceEnabled()) {
	            	Log.trace("Closing protocol engine");
	            }
	            protocolEngine.onSocketClose();
            }
            closed = true;
        }
    }

    /**
     * Get the protocol engine for this socket.
     * @return ProtocolEngine
     */
    public ProtocolEngine getProtocolEngine() {
        return protocolEngine;
    }

    /**
     * Get the daemon process for this socket.
     * @return DaemonContext
     */
    public SshEngineContext getDaemonContext() {
        return daemonContext;
    }

    /**
     * Returns the local address to which the remote socket is connected.
     * @return
     */
    public SocketAddress getLocalAddress(){
      return localAddress;
    }

    /**
     * Returns the local port to which the remote socket is connected.
     * @return
     */
    public int getLocalPort() {
      return localPort;
    }

    /**
     * Returns the local port to which the remote socket is connected.
     * @return
     */
    public int getPort() {
    	return remotePort;
    }
    
    /**
     * Returns the address of the remote socket.
     * @return
     */
    public SocketAddress getRemoteAddress() {
      return remoteAddress;
    }

    /**
     * Get the SocketChannel for this socket.
     * @return SocketChannel
     */
    public SocketChannel getSocketChannel() {
            return socketChannel;
    }

    /**
     * Get the idle state manager.
     * @return IdleStateManager
     */
    public IdleStateManager getIdleStates() {
        return selectorThread.getIdleStates();
    }

    /**
     * Is the current thread this sockets {@link SelectorThread}?
     * @return boolean
     */
    public boolean isSelectorThread() {
        return Thread.currentThread().equals(selectorThread);
    }

  /**
   * Is the socket still connected?
   * @return boolean
   */
  protected boolean isConnected() {
        return socketChannel!=null && socketChannel.isOpen() && protocolEngine.isConnected();
    }

    /**
    * Get the selector thread for this connection
    * @return SelectorThread
    */
    public SelectorThread getThread() {
	    return selectorThread;
    }
    
    /**
     * Get the initial interested ops for this socket.
     * @return int
     */
    public int getInitialOps() {
        return SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    }

    /**
     * The selector is ready to be read.
     *
     * @return boolean
     */
    public synchronized boolean processReadEvent() {

    	if(Log.isTraceEnabled()) {
        	Log.trace("Processing socket READ event");
        }

        try {

          if (!isConnected()) {
            return true;
          }

          // Get a byte buffer if needed
          if(socketDataIn==null) {
              socketDataIn = daemonContext.getBufferPool().get();
          }

          int  numBytesRead = socketChannel.read(socketDataIn);
          //flip buffer so that any remaining data can be read
          socketDataIn.flip();

          if(Log.isTraceEnabled()) {
        	  Log.trace("Read " + numBytesRead + " bytes from socket");
          }
         

          if (numBytesRead == -1) {
              if(Log.isTraceEnabled()) {
            	  Log.trace("Received EOF from remote host");
              }
              closeConnection();
              return true;
          } 
              
          if(socketDataIn.hasRemaining()) {
          	protocolEngine.onSocketRead(socketDataIn);
          }
          
          if(socketDataIn!= null && Log.isTraceEnabled()) {
        	  Log.trace("There is " +  socketDataIn.remaining() + " bytes left to process on socket");
          }

           if (!isConnected()) {
        	   if(Log.isTraceEnabled()) {
        		   Log.trace("Connection is closed, cancelling selectors");
        	   }
          }

          return !isConnected();
        } catch (Throwable ex) {
        	if(Log.isDebugEnabled()) {
        		Log.debug("Connection closed on socket read: " + ex.getMessage());
        	}
        	if(Log.isTraceEnabled()) {
        		Log.trace("Trace: ", ex);
        	}
        	closeConnection();
        	return true;
        } finally {
        	
            if(socketDataIn!=null) {
                if (!socketDataIn.hasRemaining()) {
                	daemonContext.getBufferPool().add(socketDataIn);
                    socketDataIn = null;
                } else {
                	socketDataIn.compact();
                }
            }
        }
    }

    /**
     * The selector is ready to be written to.
     *
     * @return boolean
     */
    public synchronized boolean processWriteEvent() {

        if(Log.isTraceEnabled()) {
        	Log.trace("Processing socket WRITE event");
        }
        if(socketChannel==null || !socketChannel.isOpen()) {
            return true;
        }

        if (socketDataOut == null) {
            socketDataOut = daemonContext.getBufferPool().get();
        }

        try {
        	
        	// Make sure we have sent all buffered data before getting more from protocol engine
             if(socketDataOut.remaining() == socketDataOut.capacity()
            		 && protocolEngine.isConnected()) {
        		SocketWriteCallback c = protocolEngine.onSocketWrite(socketDataOut);
        		if(c!=null)
            	 socketWriteCallbacks.addLast(c);
             }

            socketDataOut.flip();

            // Check before we send that the connection hasn't been closed
            if(!socketChannel.isOpen())
                return true;

            if(socketDataOut.hasRemaining()) {
            	int written = socketChannel.write(socketDataOut);
            	if(Log.isTraceEnabled()) {
            		Log.trace("Written " + written + " bytes to socket");
            	}
            }
            
            // Make sure any unprocessed read data is processed
            if(socketDataIn!=null) {
            	socketDataIn.flip();
            	if(socketDataIn.hasRemaining())
            		protocolEngine.onSocketRead(socketDataIn);
            }
            
            return !isConnected();

        } catch (Throwable ex) {
        	ex.printStackTrace();
            if(Log.isTraceEnabled()) {
            	Log.trace("Connection closed on socket write");
            }
            if(Log.isTraceEnabled()) {
            	Log.trace("Connection error", ex);
            }
            
            closeConnection();
            return true;
        } finally {
        	
            if(socketDataOut!=null) {
                if (!socketDataOut.hasRemaining()) {
                    daemonContext.getBufferPool().add(socketDataOut);
                    socketDataOut = null;
                    
                    for(Iterator<SocketWriteCallback> it = socketWriteCallbacks.iterator(); it.hasNext() ;) {
                    	it.next().completedWrite();
                    }
                    socketWriteCallbacks.clear();
                } else
                    socketDataOut.compact();
            }
            
            if(socketDataIn!=null) {
                if (!socketDataIn.hasRemaining()) {
                	daemonContext.getBufferPool().add(socketDataIn);
                    socketDataIn = null;
                } else {
                	socketDataIn.compact();
                }
            }
        }
    }

    /**
     * Set the selector thread for this connection
     * 
     * @param thread SelectorThread
     */
	public void setThread(SelectorThread thread) {
		this.selectorThread = thread;
	}
	
	
	public void addTask(ConnectionAwareTask task) {
		protocolEngine.getExecutor().addTask(SOCKET_QUEUE, task);
	}

	@Override
	public synchronized boolean wantsWrite() {
		return (socketDataOut!=null && socketDataOut.hasRemaining()) || (protocolEngine!=null && protocolEngine.wantsToWrite());
	}

	@Override
	public SelectorThread getSelectorThread() {
		return selectorThread;
	}

	public void flagWrite() {
		selectorThread.addSelectorOperation(new Runnable() {
			public void run() {
				if(key.isValid()) {
					if(Log.isTraceEnabled()) {
						Log.trace("Flag selector as READ/WRITE");
					}
					key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
				}
			}
		});
	}
	
	public String getName() {
		return protocolEngine.getName();
	}

	@Override
	public SshContext getContext() {
		return protocolEngine.getContext();
	}

	@Override
	public Connection<? extends SshContext> getConnection() {
		return protocolEngine.getConnection();
	}

	@Override
	public boolean wantsRead() {
		return true;
	}
}
