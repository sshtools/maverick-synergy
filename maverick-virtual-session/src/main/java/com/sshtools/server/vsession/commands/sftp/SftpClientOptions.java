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
package com.sshtools.server.vsession.commands.sftp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.cli.Option;

public class SftpClientOptions {

	private static Map<String, SftpClientOption> optionsMap = new LinkedHashMap<>();
	private static List<Option> options;
	
	static {
		assembleOptions();
	}
	
	private static void assembleOptions() {
		
		optionsMap.put(Port.PORT_OPTION, Port.instance);
		
		options = optionsMap.values().stream().map(o -> o.option()).collect(Collectors.toList());
	}

	public static Option[] getOptions() {
		return options.toArray(new Option[0]);
	}

	static abstract class SftpClientOption {
		abstract Option option();
	}
	
	public static class Port extends SftpClientOption {
		
		public static final Port instance = new Port();
		
		public static String PORT_OPTION = "P";
		
		private static String description = "Specifies the port to connect to on the remote host.";
		
		private Port() {}
		
		@Override
		Option option() {
			return Option
					.builder(PORT_OPTION)
					.hasArg()
					.desc(description)
					.argName("port")
					.build();
		}
	}
}
