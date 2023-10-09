package com.sshtools.client.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.client.sftp.GlobRegExpMatching;
import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.direct.NioFileFactory.NioFileFactoryBuilder;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

import junit.framework.TestCase;

public class FilenameMatchingTests extends TestCase {

	List<AbstractFile> files;
	
	@Override
	protected void setUp() throws Exception {
		
		AbstractFileFactory<?> fileFactory = NioFileFactoryBuilder.create().withHome(new File(".")).build();
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
