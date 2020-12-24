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

public interface SshEngineListener {

	void interfaceStarted(SshEngine engine, ListeningInterface li);

	void interfaceStopped(SshEngine engine, ListeningInterface li);

	void interfaceCannotStart(SshEngine engine, ListeningInterface li, IOException ex);

	void interfaceCannotStop(SshEngine engine, ListeningInterface li, IOException e);

	void starting(SshEngine engine);

	void started(SshEngine engine);

	void shuttingDown(SshEngine engine);

	void shutdown(SshEngine engine);
}
