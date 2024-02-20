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
	static ThreadLocal<SshConnection> currentConnection = new ThreadLocal<>();
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
	
	public static SshConnection getCurrentConnection() {
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
    	
    	if(Log.isDebugEnabled()) {
    		Log.debug("Connection {} is now authenticated", connection.getSessionIdentifier());
    	}
    	
    	Connection<T> con = activeConnections.get(connection.getSessionIdentifier());
    	if(Objects.isNull(con)) {
    		throw new IllegalArgumentException("Cannot set connection instance on non-existent transport!");
    	}

    	con.connection = connection;
    	
    	if(Log.isDebugEnabled()) {
    		Log.debug("Notifying future that authentication is complete");
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
    	if(Objects.nonNull(con)) {
	    	con.close();
	    	ctx.close(con);
    	}
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
