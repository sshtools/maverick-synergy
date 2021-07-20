
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