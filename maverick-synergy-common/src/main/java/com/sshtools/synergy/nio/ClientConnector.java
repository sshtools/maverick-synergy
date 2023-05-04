package com.sshtools.synergy.nio;

import java.nio.channels.SelectionKey;

/**
 * An interface for the NIO framework to connect outgoing sockets.
 */
public interface ClientConnector extends SelectorRegistrationListener {

        /**
         * Complete the connect operation.
         * @param key SelectionKey
         * @return boolean
         */
        public boolean finishConnect(SelectionKey key);

}
