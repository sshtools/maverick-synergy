
package com.sshtools.synergy.nio;

import java.nio.channels.SelectionKey;

/**
 * Each {@link SelectorThread} requires an implementation of this
 * interface to pass selection events.
 */
public interface SelectorThreadImpl {

        /**
         * Process a selection key.
         *
         * @param key SelectionKey
         */
        public void processSelectionKey(SelectionKey key, SelectorThread thread);
        
        /**
         * Get the name of the implementation.
         * @return String
         */
        public String getName();
}
