/*-
 * #%L
 * Virtual Sessions
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

module com.sshtools.server.vsession {
	requires transitive org.jline;
	requires transitive com.sshtools.maverick.base;
	requires transitive commons.cli;
	requires transitive com.sshtools.synergy.client;
	requires transitive com.sshtools.synergy.server;
	requires org.apache.commons.lang3;
	requires pty4j;
	exports com.sshtools.server.vsession;
	exports com.sshtools.server.vsession.commands;
	exports com.sshtools.server.vsession.commands.admin;
	exports com.sshtools.server.vsession.commands.fs;
	exports com.sshtools.server.vsession.commands.os;
	exports com.sshtools.server.vsession.commands.sftp;
	exports com.sshtools.server.vsession.jvm;
	exports com.sshtools.vsession.commands.ssh;
}
