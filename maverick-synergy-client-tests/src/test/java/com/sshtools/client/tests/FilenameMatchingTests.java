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

package com.sshtools.client.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.client.sftp.GlobRegExpMatching;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.direct.DirectFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

import junit.framework.TestCase;

public class FilenameMatchingTests extends TestCase {

	List<AbstractFile> files;
	
	@Override
	protected void setUp() throws Exception {
		
		AbstractFileFactory<?> fileFactory = new DirectFileFactory(new File("."));
		files = new ArrayList<>();
		
		files.add(fileFactory.getFile("1a.txt"));
		files.add(fileFactory.getFile("2a.txt"));
		files.add(fileFactory.getFile("3b.txt"));
		files.add(fileFactory.getFile("4b.txt"));
		files.add(fileFactory.getFile("5c.exe"));
		files.add(fileFactory.getFile("6c.exe"));
		files.add(fileFactory.getFile("7d.exe"));
		files.add(fileFactory.getFile("8d.exe"));
		
	}

	public void testWildcardWithExtensionGlobExpressions() throws SftpStatusException, SshException, IOException, PermissionDeniedException {
		
		GlobRegExpMatching m = new GlobRegExpMatching();
		assertEquals(4, m.matchFileNamesWithPattern(files.toArray(new AbstractFile[0]), "*.txt").length);
		assertEquals(4, m.matchFileNamesWithPattern(files.toArray(new AbstractFile[0]), "*.exe").length);
	}
	
	public void testSingleCharacterGlobExpressions() throws SftpStatusException, SshException, IOException, PermissionDeniedException {
		
		GlobRegExpMatching m = new GlobRegExpMatching();
		assertEquals(2, m.matchFileNamesWithPattern(files.toArray(new AbstractFile[0]), "?a.txt").length);
		assertEquals(2, m.matchFileNamesWithPattern(files.toArray(new AbstractFile[0]), "?b.txt").length);
		assertEquals(2, m.matchFileNamesWithPattern(files.toArray(new AbstractFile[0]), "?c.exe").length);
		assertEquals(2, m.matchFileNamesWithPattern(files.toArray(new AbstractFile[0]), "?d.exe").length);
	}
	
	public void testWildcardGlobExpressions() throws SftpStatusException, SshException, IOException, PermissionDeniedException {
		
		GlobRegExpMatching m = new GlobRegExpMatching();
		assertEquals(8, m.matchFileNamesWithPattern(files.toArray(new AbstractFile[0]), "*.*").length);
		assertEquals(8, m.matchFileNamesWithPattern(files.toArray(new AbstractFile[0]), "*").length);
		
	}	
	
	public void testNoMatchGlobExpressions() throws SftpStatusException, SshException, IOException, PermissionDeniedException {
		
		GlobRegExpMatching m = new GlobRegExpMatching();
		assertEquals(0, m.matchFileNamesWithPattern(files.toArray(new AbstractFile[0]), "*.dat").length);
		
	}	

}
