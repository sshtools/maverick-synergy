package com.sshtools.common.ssh;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.sshtools.common.logger.Log;
import com.sshtools.common.logger.Log.Level;

/**
 * Holds and manages Connection objects.
 */
public class ConnectionManager<T extends SshContext> implements SshConnectionManager {

    private HashMap<String, Connection<T>> activeConnections = new HashMap<String, Connection<T>>();
    
    private static List<ConnectionManager<?>> instances = new ArrayList<ConnectionManager<?>>();
    
    public static final String DEFAULT_NAME = "default";
    
    final String name;
	ThreadLocal<SshConnection> currentConnection = new ThreadLocal<>();
	ConnectionLoggingContext ctx;
	
	ConnectionManager() {
		this(DEFAULT_NAME);
	}
	
    public ConnectionManager(String name) {
    	this(name, Level.valueOf(
    			Log.getDefaultContext().getLoggingProperties().getProperty("maverick.log.connection.defaultLevel", "NONE")));
    }
    
    public ConnectionManager(String name, Level level) {
    	this.name = name;
    	instances.add(this);
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
    	for(ConnectionManager<?> instance : instances) {
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
