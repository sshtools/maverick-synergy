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
package com.sshtools.vsession.commands.ssh;

import static org.junit.Assert.*;

import org.junit.Test;


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