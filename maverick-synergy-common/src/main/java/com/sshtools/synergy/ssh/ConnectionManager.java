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

package com.sshtools.synergy.ssh;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

import com.sshtools.common.logger.Log;
import com.sshtools.common.logger.Log.Level;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshConnectionManager;

/**
 * Holds and manages Connection objects.
 */
public class ConnectionManager<T extends SshContext> implements SshConnectionManager {

    private HashMap<String, Connection<T>> activeConnections = new HashMap<>();
    
    private static Map<String,ConnectionManager<?>> instances = new HashMap<>();
    
    public static final String DEFAULT_NAME = "default";
    
    final String name;
	ThreadLocal<SshConnection> currentConnection = new ThreadLocal<>();
	ConnectionLoggingContext ctx;
		
    public ConnectionManager(String name) {
    	this(name, Level.valueOf(
    			Log.getDefaultContext().getProperty("maverick.log.connection.level", "NONE")));
    }
    
    public ConnectionManager(String name, Level level) {
    	
    	if(instances.containsKey(name)) {
    		throw new IllegalArgumentException(String.format("There is already a connection manager registered named %s", name));
    	}
    	this.name = name;
    	instances.put(name, this);
    	ctx = new ConnectionLoggingContext(level, this);
    }

    public static SshConnection searchConnectionsById(String uuid) {
    	for(ConnectionManager<?> mgr : instances.values()) {
    		SshConnection con = mgr.getConnectionById(uuid);
    		if(!Objects.isNull(con)) {
    			return con;
    		}
    	}
    	return null;
    }
    
    public String getName() {
    	return name;
    }
    
    public void startLogging(SshConnection con, Level level) throws IOException {
    	ctx.startLogging(con, level);
    }
    
	public void startLogging(SshConnection con) throws IOException {
		ctx.startLogging(con);
	}
    
    public void setupConnection(SshConnection con) {
		currentConnection.set(con);
		Log.setupCurrentContext(ctx);
	}
	
	public void clearConnection() {
		currentConnection.remove();
		Log.clearCurrentContext();
	}
	
	public SshConnection getCurrentConnection() {
		return currentConnection.get();
	}
	
    public static Connection<?> getConnection(String id) {
    	for(ConnectionManager<?> instance : instances.values()) {
    		Connection<?> c = instance.getConnectionById(id);
    		if(c!=null) {
    			return c;
    		}
    	}
    	return null;
    }
    
    public synchronized Connection<T> registerConnection(ConnectionProtocol<T> connection) {
    	Connection<T> con = activeConnections.get(connection.getSessionIdentifier());
    	if(con!=null) {
    		con.connection = connection;
    	}
    	else {
    		throw new IllegalArgumentException("Cannot set connection instance on non-existent transport!");
    	}
    	con.getAuthenticatedFuture().done(true);
    	return con;
    }

    public Connection<T> getConnectionById(String sessionid) {
    	if(sessionid != null && activeConnections.containsKey(sessionid)) {
        	return activeConnections.get(sessionid);
        }
        return null;
    }

	public synchronized Collection<SshConnection> getAllConnections() {
        return Collections.unmodifiableCollection(activeConnections.values());
    }

    public synchronized Connection<T> registerTransport(TransportProtocol<T> transport, T sshContext) {
    	Connection<T> con = new Connection<T>(transport.getContext());
    	con.transport = transport;
    	con.remoteAddress = (InetSocketAddress)transport.getRemoteAddress();
    	con.localAddress = (InetSocketAddress)transport.getLocalAddress();
        activeConnections.put(con.getSessionId(), con);
       
		if(Log.isDebugEnabled()) {
				Log.debug("There {} now {} active connections on {} connection manager",
						(activeConnections.size() > 1 ? "are" : "is"),
						activeConnections.size(),
						getName());
		}
		
        try {
			ctx.open(con);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        return con;
    }
    
    public synchronized void unregisterTransport(TransportProtocol<T> transport) {
    	Connection<T> con = activeConnections.remove(transport.getUUID());
    	con.close();
    	ctx.close(con);
    }
    
    /**
     * Get a list of currently logged on users. 
       * 
       * @return String[]
     */
    public String[] getLoggedOnUsers() {

      HashSet<String> users = new HashSet<String>();
      
      for(Connection<T> c : activeConnections.values()) {
    	  
    	  if(c.isAuthenticated())
    		  users.add(c.getUsername());
      }

      String[] result = new String[users.size()];
      users.toArray(result);

      return result;
    }


	public Integer getNumberOfConnections() {
		return activeConnections.size();
	}

}
