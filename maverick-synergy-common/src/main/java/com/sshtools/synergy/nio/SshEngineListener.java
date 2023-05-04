/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.synergy.nio;

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
