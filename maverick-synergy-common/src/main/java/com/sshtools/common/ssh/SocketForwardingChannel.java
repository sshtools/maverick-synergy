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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sshtools.common.ssh;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.ProtocolEngine;
import com.sshtools.common.nio.SelectorThread;
import com.sshtools.common.nio.SocketHandler;
import com.sshtools.common.nio.SshEngine;
import com.sshtools.common.nio.WriteOperationRequest;

/**
 * An abstract forwarding channel implementation for use with forwarding both
 * local and remote sockets.
 *
 * @see com.sshtools.common.ssh.ForwardingChannel
 * 
 */
public abstract class SocketForwardingChannel<T extends SshContext> extends ForwardingChannel<T>
		implements SocketHandler {

	public static final String LOCAL_FORWARDING_CHANNEL_TYPE = "direct-tcpip";
	public static final String REMOTE_FORWARDING_CHANNEL_TYPE = "forwarded-tcpip";
	public static final String X11_FORWARDING_CHANNEL_TYPE = "x11";
	
	private static final int SOCKET_QUEUE = 0xF0F00000;
	
	protected SocketChannel socketChannel;
	protected SelectorThread selectorThread;
	protected SelectionKey key;

	/** flag indicating that the channel is being closed */
	boolean closePending = false;

	ForwardingDataWindow toSocket;
	ByteBuffer toChannel;
	
	long totalIn;
	long totalOut;
	AtomicBoolean socketEOF = new AtomicBoolean(false);
	
	/**
	 * Construct the forwarding channel.
	 *
	 * @param channeltype the type of channel i.e. "forwarded-tcpip"
	 */
	public SocketForwardingChannel(String channeltype, T context, SshConnection con) {
		super(channeltype, con, 
				context.getForwardingPolicy().getForwardingMaxPacketSize(),
				context.getForwardingPolicy().getForwardingMaxWindowSize(),
				context.getForwardingPolicy().getForwardingMaxWindowSize(), 
				context.getForwardingPolicy().getForwardingMinWindowSize());
	}

	@Override
	protected ChannelDataWindow createLocalWindow(int initialWindowSize, int maximumWindowSpace, int minimumWindowSpace,
			int maximumPacketSize) {
		toChannel = ByteBuffer.allocate(1024000);
		toChannel.flip();
		return toSocket = new ForwardingDataWindow(initialWindowSize, maximumWindowSpace, minimumWindowSpace,
				maximumPacketSize);
	}

	public void setSelectionKey(SelectionKey key) {
		this.key = key;
	}

	/**
	 * does nothing
	 */
	protected void onChannelOpen() {
	}

	/**
	 * No initialisation is necessary so this method does nothing.
	 */
	public void initialize(ProtocolEngine engine, SshEngine daemon) {

	}

	protected abstract byte[] createChannel() throws java.io.IOException;

	/**
	 * does nothing
	 */
	protected void onExtendedData(ByteBuffer data, int type) {
		throw new IllegalStateException("Extended data is not supported on forwarding channels");
	}

	/**
	 * Called by the subsystem when the selector registration has been completed.
	 */
	protected abstract void onRegistrationComplete();

	public void registrationCompleted(SelectableChannel channel, SelectionKey key, SelectorThread selectorThread) {

		if (Log.isTraceEnabled())
			log("Forwarding channel selector thread registration completed");

		this.selectorThread = selectorThread;
		this.key = key;

		onRegistrationComplete();

	}

	/**
	 * data has arrived from the end of the tunnel so add to the queue of data
	 * heading towards the start of the tunnel
	 * 
	 */
	protected void onChannelData(ByteBuffer data) {
		
		toSocket.put(data);
		changeInterestedOps();
		
		if(socketEOF.get() && canClose()) {
			close();
		}
	}

	protected void onChannelRequest(String parm1, boolean parm2, byte[] parm3) {
		// Forwarding channels do not support any requests
		sendRequestResponse(false);
	}

	private void changeInterestedOps() {
		selectorThread.addSelectorOperation(new Runnable() {
			public void run() {
				if (key.isValid()) {
					int ops = 0;
					boolean wantsWrite = wantsWrite();
					boolean wantsRead =  wantsRead();
					if(wantsWrite) {
						ops |= SelectionKey.OP_WRITE;
					}
					if(wantsRead) {
						ops |= SelectionKey.OP_READ;
					}
					if(Log.isTraceEnabled()) {
						Log.trace(String.format("%s channel=%d ops=%d has state %s",
								getName(),
								getLocalId(),
								ops,
								wantsWrite && wantsRead ? "READ/WRITE" : wantsWrite ? "WRITE" : wantsRead ? "READ" : "NONE"));
					}
					key.interestOps(ops); 
				}
			}
		});
	}


	/**
	 * does nothing
	 */
	protected void onChannelFree() {

	}

	/**
	 * does nothing
	 */
	protected void onChannelClosing() {

	}

	protected synchronized void cleanupSocket() {
		
		if (socketChannel != null) {
			if (socketChannel.isOpen()) {
				if (Log.isTraceEnabled())
					log("Closing SocketChannel");
				try {
					socketChannel.close();
					socketEOF.set(true);
				} catch (IOException ex) {
					if (Log.isTraceEnabled())
						log("Closing SocketChannel caused Exception", ex);
				} finally {
					if (Log.isTraceEnabled())
						Log.trace("Socket is closed channel=%d remote=%d", getLocalId(), getRemoteId());
				}
			}
			socketChannel = null;
		}
	}

	protected synchronized boolean canClose() {
		
		if (!socketEOF.get() && toSocket.hasRemaining()) {
			if (Log.isTraceEnabled()) {
				log("Not closing due to socket cache");
			}
			return false;
		}
		
		synchronized(toChannel) {
			if (toChannel.hasRemaining() && isOpen() && !isLocalEOF()) {
				if (Log.isTraceEnabled()) {
					log("Not closing due to channel cache");
				}
				return false;
			}
		}

		return super.canClose();

	}

	/**
	 * set the closePending flag to true and attempt to close the channel
	 */
	protected synchronized void evaluateClosure() {
		closePending = true;
		if (canClose() && isRemoteEOF()) {
			close();
		}
	}

	protected void shutdownSocket() {

		if (selectorThread != null && socketChannel != null) {

			if (Log.isTraceEnabled())
				log("Adding Socket close operation to selector");
			// Close the socket channel but only when the thread is ready
			selectorThread.addSelectorOperation(new Runnable() {
				public void run() {

					cleanupSocket();

					if (key != null && key.isValid()) {
						if (Log.isTraceEnabled()) {
							log("Cancelling selection key because its still valid");
						}
						key.cancel();
						key = null;
					}
				}
			});

			if (Log.isTraceEnabled())
				log("Waking up selector");
			selectorThread.wakeup();
		} else if (socketChannel != null) {
			if (Log.isTraceEnabled())
				log("Socket is not attached to selector so closing now");
			cleanupSocket();
		}
	}

	protected void onChannelClosed() {
		shutdownSocket();
	}

	/**
	 * The start of the tunnel has gone EOF , if the end of the tunnel has already
	 * gone EOF then close the tunnel.
	 */
	@Override
	protected void onLocalEOF() {
		evaluateClosure();
	}

	/**
	 * We override this to make sure that all data from the socket has been sent
	 * before we close our side of the channel
	 */
	@Override
	protected void onRemoteClose() {
		evaluateClosure();
	}

	/**
	 * The end of the tunnel has gone EOF , if the start of the tunnel has already
	 * gone EOF then close the tunnel.
	 */
	protected void onRemoteEOF() {
		evaluateClosure();
	}

	protected abstract void onChannelOpenConfirmation();

	protected void evaluateWindowSpace(int remaining) {
		/**
		 * Handle window space after we have written to the outgoing socket.
		 */
	}

	protected abstract byte[] openChannel(byte[] parm1) throws WriteOperationRequest, ChannelOpenException;

	/**
	 * read data from the start/end of tunnel and write it into the ssh tunnel.
	 */
	public boolean processReadEvent() {

		if(Log.isTraceEnabled()) {
			log("Processing FORWARDING READ");
		}
		
		if (socketChannel == null || !socketChannel.isConnected() || !isOpen()) {
			if(Log.isTraceEnabled()) {
				log("Forwarding socket is closed");
			}
			return true;
		}

		try {

			int numBytesRead;
			
			synchronized(toChannel) {

				toChannel.compact();
				
				try {
					numBytesRead = socketChannel.read(toChannel);
				} finally {
					toChannel.flip();
				}
			}

			if(Log.isTraceEnabled()) {
				log(String.format("Processed FORWARDING READ read=%d", numBytesRead));
			}
			
			if (numBytesRead <= 0) {

				if (numBytesRead == -1) {

					socketEOF.set(true);
					if(Log.isDebugEnabled()) {
						log("Received EOF from forwarding socket");
					}
					getConnectionProtocol().addOutgoingTask(new ConnectionAwareTask(con) {
						protected void doTask() {
							sendEOF();
							evaluateClosure();
						}
					});
					
					return true;
				}

			} else if (numBytesRead > 0) {

				totalIn += numBytesRead;
				
				if(Log.isTraceEnabled())
					log("Processing FORWARDING READ read=" + numBytesRead);

				getConnectionProtocol().addOutgoingTask(new QueueChannelDataTask(con, numBytesRead));
			}

		} catch (Throwable ex) {
			if (Log.isTraceEnabled())
				log("processReadEvent() failed to read from socket", ex);

			socketEOF.set(true);
			
			getConnectionProtocol().addOutgoingTask(new ConnectionAwareTask(con) {
				protected void doTask() {
					sendEOF();
					evaluateClosure();
				}
			});
			
			return true;
		} 

		return !isOpen() && (socketChannel==null || !socketChannel.isConnected());

	}

	/**
	 * read data from the ssh tunnel and write it to the start/end point.
	 */
	public boolean processWriteEvent() {
		
		if(Log.isTraceEnabled()) {
			log("Processing FORWARDING WRITE");
		}
		
		if (socketChannel == null || !socketChannel.isConnected()) {
			if(Log.isTraceEnabled()) {
				log("Forwarding socket is closed");
			}
			return true;
		}

		int written = 0;
		try {
			synchronized (toSocket) {
				if (toSocket.hasRemaining()) {
					written = toSocket.write(socketChannel);

					if(Log.isTraceEnabled()) {
						log(String.format("Processed FORWARDING WRITE written=%d", written));
					}
					
					totalOut += written;
				}

				if(Log.isTraceEnabled()) {
					log("Completed FORWARDING WRITE");
				}
				
				if(toSocket.isAdjustRequired()) {
					sendWindowAdjust();
				}
			}
			
			if (closePending && canClose()) {
				close();
			}
			
		} catch (Throwable t) {
			
			socketEOF.set(true);
			
			if (Log.isTraceEnabled()) {
				log("processWriteEvent() failed to write to socket", t);
			}
			
			evaluateClosure();
			return true;
		} 
		
		return !isOpen() && (socketChannel==null || !socketChannel.isConnected());
	}

	@Override
	public boolean wantsWrite() {
		return toSocket.hasRemaining();
	}
	
	@Override
	public boolean wantsRead() {
		return true;
	}

	public int getInitialOps() {
		return SelectionKey.OP_READ;
	}

	/**
	 * Sets the selector thread for this connection
	 *
	 * @param thread SelectorThread
	 */
	public void setThread(SelectorThread thread) {
		this.selectorThread = thread;
	}

	class QueueChannelDataTask extends ConnectionAwareTask {

		int count;
		QueueChannelDataTask(SshConnection con, int count) {
			super(con);
			this.count = count;
		}

		protected void doTask() {
			
			try {

				byte[] tmp = new byte[getRemotePacket()];
				int c;
				while(count > 0) {

					synchronized (toChannel) {
						c = Math.min(Math.min(count, tmp.length), toChannel.remaining());
						toChannel.get(tmp, 0, c);
						count -= c;
					}
					
					sendData(tmp, 0, c);
				}
				
				changeInterestedOps();
				
				if(closePending && canClose()) {
					close();
				}
				
			} catch (IOException e) {
				log("Channel I/O error", e);
				close(true);
			} 
		}
	}

	void log() {
		super.log();
		if(Log.isInfoEnabled()) {
			Log.info(String.format("socketCache=%d channelCache=%d closePending=%s connected=%s in=%d out=%d", 
					toSocket.remaining(), toChannel.remaining(),
					closePending, socketChannel != null && socketChannel.isConnected(), 
					totalIn,
					totalOut));
		}
	}

	public void addTask(ConnectionAwareTask task) {
		getConnectionProtocol().addTask(SOCKET_QUEUE & getLocalId(), task);;
	}

	public SelectorThread getSelectorThread() {
		return selectorThread;
	}

	public String getName() {
		return getChannelType();
	}

}