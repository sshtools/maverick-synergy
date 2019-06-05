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
