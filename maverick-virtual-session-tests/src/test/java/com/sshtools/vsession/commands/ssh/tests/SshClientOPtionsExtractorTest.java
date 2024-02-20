package com.sshtools.vsession.commands.ssh.tests;

/*-
 * #%L
 * Virtual Sessions Tests
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

import static org.junit.Assert.*;

import org.junit.Test;

import com.sshtools.vsession.commands.ssh.SshClientOptionsExtractor;


public class SshClientOPtionsExtractorTest {

	@Test
	public void itShouldParseSSHWithoutExecuteCommand() {
		String[] values = new String[] {"ssh", "localhost"};
		int result = SshClientOptionsExtractor.extractSshCommandLineFromExecuteCommand(values);
		assertEquals(1, result);
	}
	
	@Test
	public void itShouldParseSSHWithExecuteCommand() {
		String[] values = new String[] {"ssh", "localhost", "ls", "-al"};
		int result = SshClientOptionsExtractor.extractSshCommandLineFromExecuteCommand(values);
		assertEquals(1, result);
	}
	
	@Test
	public void itShouldParseSSHWithOptionButWithoutExecuteCommand() {
		String[] values = new String[] {"ssh", "-p", "2222", "-l", "admin", "localhost"};
		int result = SshClientOptionsExtractor.extractSshCommandLineFromExecuteCommand(values);
		assertEquals(5, result);
	}
	
	@Test
	public void itShouldParseSSHWithOptionButWithExecuteCommand() {
		String[] values = new String[] {"ssh", "-p", "2222", "-l", "admin", "localhost", "ls", "-al"};
		int result = SshClientOptionsExtractor.extractSshCommandLineFromExecuteCommand(values);
		assertEquals(5, result);
	}

	@Test
	public void itShouldParseSSHWithSingleOptionButWithoutExecuteCommand() {
		String[] values = new String[] {"ssh", "-4", "localhost"};
		int result = SshClientOptionsExtractor.extractSshCommandLineFromExecuteCommand(values);
		assertEquals(2, result);
	}
	
	@Test
	public void itShouldParseSSHWithCustomOptionButWithExecuteCommand() {
		String[] values = new String[] {"ssh", "-JS", "PARANOID", "-l", "admin", "localhost", "ls", "-al"};
		int result = SshClientOptionsExtractor.extractSshCommandLineFromExecuteCommand(values);
		assertEquals(5, result);
	}
	
	@Test
	public void itShouldParseSSHWithCustomOptionButWithoutExecuteCommand() {
		String[] values = new String[] {"ssh", "-JS", "PARANOID", "-l", "admin", "localhost"};
		int result = SshClientOptionsExtractor.extractSshCommandLineFromExecuteCommand(values);
		assertEquals(5, result);
	}
	
	@Test
	public void itShouldParseSSHWithSingleOptionButWithExecuteCommand() {
		String[] values = new String[] {"ssh", "-4", "localhost", "ls", "-al"};
		int result = SshClientOptionsExtractor.extractSshCommandLineFromExecuteCommand(values);
		assertEquals(2, result);
	}
	
	@Test(expected = IllegalStateException.class)
	public void itShouldNotParseSSHWithOptionsNotRecognizedInSSH() {
		String[] values = new String[] {"ssh", "-h", "asdasd"};
		SshClientOptionsExtractor.extractSshCommandLineFromExecuteCommand(values);
		fail("Should not parse.");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void itShouldNotParseNullArguments() {
		String[] values = null;
		SshClientOptionsExtractor.extractSshCommandLineFromExecuteCommand(values);
		fail("Should not parse.");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void itShouldNotParseEmptyArguments() {
		String[] values = new String[] {};
		SshClientOptionsExtractor.extractSshCommandLineFromExecuteCommand(values);
		fail("Should not parse.");
	}
}
