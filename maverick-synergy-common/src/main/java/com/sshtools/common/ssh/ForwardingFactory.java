package com.sshtools.common.ssh;

import java.io.IOException;

/**
 * This interface defines the behaviour for remote forwarding requests. When an SSH client requests
 * a remote forwarding we typically open a server socket, accept connections and
 * open remote forwarding channels on the client.
 */
public interface ForwardingFactory<T extends SshContext> {

        /**
         * A client has requested that the server start listening and forward
         * any subsequent connections to the client.
         *
         * @param addressToBind String
         * @param portToBind int
         * @param connection ConnectionProtocol
         * @throws IOException
         */
        public int bindInterface(String addressToBind, int portToBind, ConnectionProtocol<T> connection) throws IOException;

        /**
         * 
         * @param addressToBind
         * @param portToBind
         * @param connection
         * @param channelType
         * @throws IOException
         */
        public int bindInterface(String addressToBind, int portToBind, ConnectionProtocol<?> connection, String channelType) throws IOException;
        
        /**
         * Does this factory belong to the connection provided?
         * @param connection ConnectionProtocol
         * @return boolean
         */
        public boolean belongsTo(ConnectionProtocol<T> connection);

        /**
         * Stop listening on active interfaces.
         * @param dropActiveTunnels boolean
         */
        public void stopListening(boolean dropActiveTunnels);
        
        
        /**
         * Get the underlying channel type for this forwarding factory.
         */
        public String getChannelType();

		public int getStartedEventCode();

		public int getStoppedEventCode();
        

}
