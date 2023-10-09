package com.sshtools.synergy.ssh;

/**
 * An event handler for SSH Transport protocol events.
 */
public interface TransportProtocolListener {

        /**
         * The transport was disconnected.
         *
         * @param transport TransportProtocol
         */
        public void onDisconnect(TransportProtocol<?> transport);
}
