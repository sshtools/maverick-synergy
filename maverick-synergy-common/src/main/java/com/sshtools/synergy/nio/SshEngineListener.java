package com.sshtools.synergy.nio;

/*-
 * #%L
 * Common API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;

public interface SshEngineListener {

	default void interfaceStarted(SshEngine engine, ListeningInterface li) { };

	default void interfaceStopped(SshEngine engine, ListeningInterface li) { };

	default void interfaceCannotStart(SshEngine engine, ListeningInterface li, IOException ex) { };

	default void interfaceCannotStop(SshEngine engine, ListeningInterface li, IOException e) { } ;

	default void starting(SshEngine engine) { };

	default void started(SshEngine engine) { };

	default void shuttingDown(SshEngine engine) { };

	default void shutdown(SshEngine engine) { };
}
