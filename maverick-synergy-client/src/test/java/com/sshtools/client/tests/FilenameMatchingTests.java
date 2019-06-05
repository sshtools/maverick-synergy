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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.client.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.client.sftp.GlobRegExpMatching;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

import junit.framework.TestCase;

public class FilenameMatchingTests extends TestCase {

	List<File> files;
	
	@Override
	protected void setUp() throws Exception {
		files = new ArrayList<File>();
		
		files.add(new File("1a.txt"));
		files.add(new File("2a.txt"));
		files.add(new File("3b.txt"));
		files.add(new File("4b.txt"));
		files.add(new File("5c.exe"));
		files.add(new File("6c.exe"));
		files.add(new File("7d.exe"));
		files.add(new File("8d.exe"));
		
	}

	public void testWildcardWithExtensionGlobExpressions() throws SftpStatusException, SshException {
		
		GlobRegExpMatching m = new GlobRegExpMatching();
		assertEquals(4, m.matchFileNamesWithPattern(files.toArray(new File[0]), "*.txt").length);
		assertEquals(4, m.matchFileNamesWithPattern(files.toArray(new File[0]), "*.exe").length);
	}
	
	public void testSingleCharacterGlobExpressions() throws SftpStatusException, SshException {
		
		GlobRegExpMatching m = new GlobRegExpMatching();
		assertEquals(2, m.matchFileNamesWithPattern(files.toArray(new File[0]), "?a.txt").length);
		assertEquals(2, m.matchFileNamesWithPattern(files.toArray(new File[0]), "?b.txt").length);
		assertEquals(2, m.matchFileNamesWithPattern(files.toArray(new File[0]), "?c.exe").length);
		assertEquals(2, m.matchFileNamesWithPattern(files.toArray(new File[0]), "?d.exe").length);
	}
	
	public void testWildcardGlobExpressions() throws SftpStatusException, SshException {
		
		GlobRegExpMatching m = new GlobRegExpMatching();
		assertEquals(8, m.matchFileNamesWithPattern(files.toArray(new File[0]), "*.*").length);
		assertEquals(8, m.matchFileNamesWithPattern(files.toArray(new File[0]), "*").length);
		
	}	
	
	public void testNoMatchGlobExpressions() throws SftpStatusException, SshException {
		
		GlobRegExpMatching m = new GlobRegExpMatching();
		assertEquals(0, m.matchFileNamesWithPattern(files.toArray(new File[0]), "*.dat").length);
		
	}	

}
