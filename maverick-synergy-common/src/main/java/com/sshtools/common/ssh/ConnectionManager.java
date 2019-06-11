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
package com.sshtools.common.ssh;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.sshtools.common.logger.Log;
import com.sshtools.common.logger.Log.Level;

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
    			Log.getDefaultContext().getLoggingProperties().getProperty("maverick.log.connection.defaultLevel", "NONE")));
    }
    
    public ConnectionManager(String name, Level level) {
    	
    	if(instances.containsKey(name)) {
    		throw new IllegalArgumentException(String.format("There is already a connection manager registered named %s", name));
    	}
    	this.name = name;
    	instances.put(name, this);
    	ctx = new ConnectionLoggingContext(level, this);
    }

    public String getName() {
    	return name;
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

    public synchronized void registerTransport(TransportProtocol<T> transport, T sshContext) {
    	Connection<T> con = new Connection<T>(transport.getContext());
    	con.transport = transport;
    	con.remoteAddress = (InetSocketAddress)transport.getRemoteAddress();
    	con.localAddress = (InetSocketAddress)transport.getLocalAddress();
        activeConnections.put(con.getSessionId(), con);
       
		if(Log.isDebugEnabled()) {
				Log.debug("There %s now %d active connections on %s connection manager",
						(activeConnections.size() > 1 ? "are" : "is"),
						activeConnections.size(),
						getName());
		}
		
        try {
			ctx.open(con);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
