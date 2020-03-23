package com.sshtools.vsession.commands.ssh;

import static org.junit.Assert.*;

import org.junit.Test;


public class CommandUtilTest {

	@Test
	public void itShouldParseSSHWithoutExecuteCommand() {
		String[] values = new String[] {"ssh", "localhost"};
		int result = CommandUtil.extractSshCommandLineFromExecuteCommand(values);
		assertEquals(1, result);
	}
	
	@Test
	public void itShouldParseSSHWithExecuteCommand() {
		String[] values = new String[] {"ssh", "localhost", "ls", "-al"};
		int result = CommandUtil.extractSshCommandLineFromExecuteCommand(values);
		assertEquals(1, result);
	}
	
	@Test
	public void itShouldParseSSHWithOptionButWithoutExecuteCommand() {
		String[] values = new String[] {"ssh", "-p", "2222", "-l", "admin", "localhost"};
		int result = CommandUtil.extractSshCommandLineFromExecuteCommand(values);
		assertEquals(5, result);
	}
	
	@Test
	public void itShouldParseSSHWithOptionButWithExecuteCommand() {
		String[] values = new String[] {"ssh", "-p", "2222", "-l", "admin", "localhost", "ls", "-al"};
		int result = CommandUtil.extractSshCommandLineFromExecuteCommand(values);
		assertEquals(5, result);
	}

	@Test
	public void itShouldParseSSHWithSingleOptionButWithoutExecuteCommand() {
		String[] values = new String[] {"ssh", "-4", "localhost"};
		int result = CommandUtil.extractSshCommandLineFromExecuteCommand(values);
		assertEquals(2, result);
	}
	
	@Test
	public void itShouldParseSSHWithSingleOptionButWithExecuteCommand() {
		String[] values = new String[] {"ssh", "-4", "localhost", "ls", "-al"};
		int result = CommandUtil.extractSshCommandLineFromExecuteCommand(values);
		assertEquals(2, result);
	}
	
	@Test(expected = IllegalStateException.class)
	public void itShouldNotParseSSHWithOptionsNotRecognizedInSSH() {
		String[] values = new String[] {"ssh", "-h", "asdasd"};
		CommandUtil.extractSshCommandLineFromExecuteCommand(values);
		fail("Should not parse.");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void itShouldNotParseNullArguments() {
		String[] values = null;
		CommandUtil.extractSshCommandLineFromExecuteCommand(values);
		fail("Should not parse.");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void itShouldNotParseEmptyArguments() {
		String[] values = new String[] {};
		CommandUtil.extractSshCommandLineFromExecuteCommand(values);
		fail("Should not parse.");
	}
}