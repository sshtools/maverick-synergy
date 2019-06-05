
package com.sshtools.common.nio;

import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.SshContext;

/**
 * An interface used by the {@link SelectorThread} to notify an object
 * of when a read or write event has occurred.
 */
public interface SocketHandler extends SelectorRegistrationListener, SelectionKeyAware {

        /**
         * Initialise this socket with a {@link ProtocolEngine} and the current
         * {@link SshEngine}.
         *
         * @param engine ProtocolEngine
         * @param daemon Daemon
         */
        public void initialize(ProtocolEngine engine, SshEngine daemon);

        /**
         * The selector is ready to be read.
         * @return boolean indicating ??
         */
        public boolean processReadEvent();

        /**
         * The selector is ready to be written to.
         * @return boolean
         */
        public boolean processWriteEvent();

        /**
         * Returns the current operations the handler is interested in
         * @return int
         */
        public int getInitialOps();
        
        /**
         * Sets the selector thread this handler runs upon
         * @param thread
         */
        public void setThread(SelectorThread thread);
        
        /**
         * Add a task to the executor
         * @return
         */
        public void addTask(Runnable task);

        /**
         * Tell the selector that the handler wants to write.
         * @return
         */
		public boolean wantsWrite();

		
		/**
		 * Get the selector thread this handler is connected to.
		 * @return
		 */
		SelectorThread getSelectorThread();

		/**
		 * A name for this Socket
		 * @return
		 */
		public String getName();
		
		/**
		 * Get the current context
		 * @return
		 */
		public SshContext getContext();

		/**
		 * Get the Connection for this handler
		 * @return
		 */
		public Connection<? extends SshContext> getConnection();

		/**
		 * Tell the selector that the handler wants to read.
		 * @return
		 */
		boolean wantsRead();
}
