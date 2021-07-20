/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
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