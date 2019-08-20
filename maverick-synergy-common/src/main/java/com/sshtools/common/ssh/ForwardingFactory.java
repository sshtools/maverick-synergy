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
