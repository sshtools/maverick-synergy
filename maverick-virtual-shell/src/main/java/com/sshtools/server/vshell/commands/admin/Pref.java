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
package com.sshtools.server.vshell.commands.admin;

import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;
import com.sshtools.server.vshell.terminal.Cell;
import com.sshtools.server.vshell.terminal.Row;
import com.sshtools.server.vshell.terminal.Table;

public class Pref extends ShellCommand {
	public Pref() {
		super("pref", SUBSYSTEM_SYSTEM);
		setDescription("View and manipulate Java Preferences.");
		setBuiltIn(false);
		getOptions().addOption("s", false,
				"Use system root instead of user root.");
		getOptions().addOption("r", false, "Recursively list keys and values.");
		getOptions().addOption("k", false, "List keys only.");
		getOptions().addOption("n", false, "List nodes only.");
	}

	public void run(CommandLine cli, VirtualProcess process)
			throws IOException, PermissionDeniedException {

		String[] args = cli.getArgs();
		Preferences root = Preferences.userRoot();
		if (cli.hasOption("-s")) {
			root = Preferences.systemRoot();
		}
		if (args.length == 2) {
			root = root.node(args[1]);
		}
		Table table = new Table(process.getTerminal());
		Row header = new Row(new Cell<String>("Name"),
				new Cell<String>("Value"));
		header.setStrong(true);
		table.setHeader(header);
		try {
			addNode(0, table, root, cli.hasOption("r"), cli.hasOption("n")
					|| !cli.hasOption("k"),
					cli.hasOption("k") || !cli.hasOption("n"));
		} catch (BackingStoreException e) {
			IOException ioe = new IOException("Failed to list preferences.");
			ioe.initCause(e);
			throw ioe;
		}
		table.render(process.getConsole());
	}

	protected void addNode(int indent, Table table, Preferences node,
			boolean recurse, boolean showNodes, boolean showKeys)
			throws BackingStoreException {
		StringBuilder bui = new StringBuilder();
		for (int i = 0; i < indent * 2; i++) {
			bui.append(' ');
		}
		if (showNodes) {
			table.add(new Row(new Cell<String>(bui.toString() + "*"
					+ node.name()), new Cell<String>("")));
		}
		if (showKeys) {
			for (String k : node.keys()) {
				table.add(new Row(new Cell<String>(bui.toString() + "  " + k),
						new Cell<String>(trimToSize(node, k))));
			}
		}
		if (recurse) {
			for (String c : node.childrenNames()) {
				addNode(indent + 1, table, node.node(c), recurse, showNodes,
						showKeys);
			}
		}
	}

	private String trimToSize(Preferences node, String k) {
		String val = node.get(k, "");
		if(val.length() > 40) {
			val = val.substring(0, 40) + "...";
		}
		return val;
	}
}
