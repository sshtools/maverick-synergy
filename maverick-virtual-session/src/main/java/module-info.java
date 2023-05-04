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
module com.sshtools.server.vsession {
	requires transitive org.jline;
	requires transitive com.sshtools.maverick.base;
	requires commons.cli;
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