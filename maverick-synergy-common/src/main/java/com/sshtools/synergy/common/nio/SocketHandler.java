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

package com.sshtools.synergy.common.nio;

import java.io.IOException;
import java.nio.channels.SelectableChannel;

import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.synergy.common.ssh.Connection;
import com.sshtools.synergy.common.ssh.SshContext;

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
         * @throws IOException 
         */
        public void initialize(ProtocolEngine engine, SshEngine daemon, SelectableChannel channel) throws IOException;

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
        public void addTask(ConnectionAwareTask task);

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
