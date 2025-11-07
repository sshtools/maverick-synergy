package com.sshtools.synergy.ssh;

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
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sshtools.common.forwarding.ForwardingHandle;
import com.sshtools.common.forwarding.ForwardingRequest;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelEventListener;
import com.sshtools.common.ssh.RequestFuture;
import com.sshtools.common.ssh.RequestFutureListener;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.IOUtils;
import com.sshtools.synergy.nio.ClientAcceptor;
import com.sshtools.synergy.nio.ListeningInterface;

/**
 * This class implements the standard socket based forwarding for the SSHD.
 */
public abstract class SocketListeningForwardingChannelFactoryImpl<T extends SshContext, ADDR extends SocketAddress>
      extends ClientAcceptor implements ForwardingChannelFactory<T> {

    protected ServerSocketChannel socketChannel;
    protected ConnectionProtocol<T> connection;
    protected ADDR addr;
    protected String channelType;
    protected ActiveTunnelManager<T> activeRemoteForwardings = new ActiveTunnelManager<T>();
    protected ForwardingRequest request;

    public SocketListeningForwardingChannelFactoryImpl() {
      super(null);
    }
    
    public ActiveTunnelManager<T> getActiveTunnelManager() {
    	return activeRemoteForwardings;
    }

    public boolean belongsTo(ConnectionProtocol<T> connection) {
        return this.connection!=null && this.connection.equals(connection);
    }
    
    protected abstract ADDR createAddress(ForwardingRequest request);
    
    @SuppressWarnings("unchecked")
	@Override
	public ForwardingHandle bindInterface(ForwardingRequest request, ConnectionProtocol<?> connection,
			String channelType) throws IOException {
    	this.request = request;;
        this.connection = (ConnectionProtocol<T>) connection;
        this.channelType = channelType;
        
        addr = createAddress(request);

        this.socketChannel = createSocketChannel();
        
        try {
	        socketChannel.configureBlocking(false);
	        socketChannel.bind(addr, connection.getContext().getMaximumSocketsBacklogPerRemotelyForwardedConnection());
	
	        connection.getContext().getEngine().registerAcceptor(this, socketChannel);
	        
	        return createHandle();
        
        } catch(IOException e) {
			IOUtils.closeStream(socketChannel);
			throw e;
        }
	}

    protected abstract ServerSocketChannel createSocketChannel() throws IOException;

	protected abstract ForwardingHandle createHandle();

	public boolean finishAccept(SelectionKey key, ListeningInterface li) {
      try {
        final SocketChannel sc = socketChannel.accept();
        
        if(sc!=null) {

           
            sc.configureBlocking(false);
            onAccept(sc);

            ForwardingChannel<T> channel = createChannel(channelType,
            		connection.getTransport().getConnection(),
                    request,
                    sc,
                    connection.getContext());

            channel.addEventListener(activeRemoteForwardings);

            channel.getOpenFuture().addFutureListener(new RequestFutureListener() {
            	public void complete(RequestFuture future) {
            		
            		if(!future.isSuccess()) {
	            		if(Log.isDebugEnabled()) {
	                    	Log.debug("Channel could not be opened");
	                    }
	                    try {
	                        sc.close();
	                    } catch(IOException ex) { }
            		}
            	}
            });
            
            connection.openChannel(channel);

        } else {
            if(Log.isDebugEnabled()) {
            	Log.debug("FORWARDING accept event fired but no socket was accepted");
            }
        }
      }
      catch (IOException ex) {
        if(Log.isDebugEnabled()) {
        	Log.debug("Accept operation failed on " + request.bindName(), ex);
        }
      }

      return !socketChannel.isOpen();
    }

	protected abstract void onAccept(final SocketChannel sc) throws SocketException;

    protected abstract ForwardingChannel<T> createChannel(String channelType,
    		SshConnection con, 	ForwardingRequest request, SocketChannel sc, T context);

	public void stopListening(boolean dropActiveTunnels) {
     
	  stopAccepting();

      if(dropActiveTunnels) {
          activeRemoteForwardings.killAllTunnels();
      }

    }

	public void stopAccepting() {
		try {
			socketChannel.close();
		} catch (Throwable e) {
			Log.error("Error closing listening socket", e);
		}
	}

	public static class ActiveTunnelManager<K extends SshContext> implements ChannelEventListener {

		public interface TunnelListener<K extends SshContext> {

			void tunnelOpened(ForwardingChannel<K> channel);
		}

		List<Channel> activeTunnels = Collections.synchronizedList(new ArrayList<>());
		List<TunnelListener<K>> listeners = Collections.synchronizedList(new ArrayList<>());
		boolean killingTunnels = false;
		
		public void addListener(TunnelListener<K> listener) {
			listeners.add(listener);
		}
		
		public void removeListener(TunnelListener<K> listener) {
			listeners.remove(listener);
		}

		public List<Channel> getTunnels() {
			return activeTunnels;
		}

		public void killAllTunnels() {

			synchronized (activeTunnels) {
				killingTunnels = true;
				for (Channel channel : activeTunnels) {
					try {
						channel.close();
					} catch (Throwable t) {
					}
				}
				activeTunnels.clear();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onChannelOpen(Channel channel) {
			synchronized (activeTunnels) {
				if (!killingTunnels)
					activeTunnels.add(channel);
				for(int i = listeners.size() - 1 ; i >= 0 ; i--) 
					listeners.get(i).tunnelOpened((ForwardingChannel<K>) channel);
			}
		}

		@Override
		public void onChannelClose(Channel channel) {
			synchronized (activeTunnels) {
				if (!killingTunnels)
					activeTunnels.remove(channel);
			}
		}
	}

	protected abstract ForwardingRequest.ForwardingType type();

}
