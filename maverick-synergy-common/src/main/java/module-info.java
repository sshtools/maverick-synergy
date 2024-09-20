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

import com.sshtools.common.ssh.compression.SshCompressionFactory;

@SuppressWarnings("rawtypes")
module com.sshtools.synergy.common {
	requires transitive com.sshtools.maverick.base;
	requires com.sshtools.common.logger;
	requires com.sshtools.common.util;
	exports com.sshtools.synergy.nio;
	exports com.sshtools.synergy.nio.ssl;
	exports com.sshtools.synergy.ssh;
	exports com.sshtools.synergy.ssh.components;
	exports com.sshtools.synergy.ssh.components.jce;
	exports com.sshtools.synergy.util;
	opens com.sshtools.synergy.ssh;
	
	uses SshCompressionFactory;
}
