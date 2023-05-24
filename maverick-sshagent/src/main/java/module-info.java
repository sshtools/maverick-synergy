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
import com.sshtools.agent.AgentProvider;
import com.sshtools.agent.provider.tcp.TCPAgentProvider;

open module com.sshtools.agent {
	requires transitive com.sshtools.maverick.base;
	requires com.sshtools.common.util;
	requires com.sshtools.common.logger;
	exports com.sshtools.agent;
	exports com.sshtools.agent.client;
	exports com.sshtools.agent.exceptions;
	exports com.sshtools.agent.rfc;
	exports com.sshtools.agent.server;
	exports com.sshtools.agent.openssh;
	uses AgentProvider;
	provides AgentProvider with TCPAgentProvider;
}