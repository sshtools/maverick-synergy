/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */


package com.sshtools.synergy.nio;

import java.io.IOException;
import java.nio.channels.SelectableChannel;

import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.synergy.ssh.Connection;
import com.sshtools.synergy.ssh.SshContext;

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
