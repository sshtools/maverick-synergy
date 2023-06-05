package com.sshtools.synergy.niofs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.util.regex.PatternSyntaxException;

import org.junit.Test;

public class SftpFileSystemTest extends AbstractNioFsTest {
	
	@Test
	public void testGetPathMultiple() throws Exception {
		testWithFilesystem((fs) -> {
			var p = fs.getPath("dir", "subfolder", "file/");
			Files.createDirectories(p);
			assertTrue("The folder should exist", Files.exists(p));
			assertEquals("dir/subfolder/file", p.toString());
		});
	}

	@Test
	public void testStores() throws Exception {
		testWithFilesystem((fs) -> {
			var it = fs.getFileStores().iterator();
			assertTrue("There should be one store", it.hasNext());
			it.next();
			assertFalse("There should be no more than one stores", it.hasNext());
		});
	}
	
	@Test
	public void testRoots() throws Exception {
		testWithFilesystem(fs -> {
			var it = fs.getRootDirectories().iterator();
			assertTrue("There should be one root", it.hasNext());
			assertEquals("/", it.next().toString());
			assertFalse("There should be no more than root", it.hasNext());
			assertEquals("/", fs.getSeparator());
			var remotePath = fs.getPath(tmpDir.toString());
			assertTrue("Path must be the SftpPath", remotePath instanceof SftpPath);
			assertEquals(tmpDir.toString(), remotePath.toString());
			assertTrue("The filesystem should be open", fs.isOpen());
			assertFalse("The filesystem should not be read only", fs.isReadOnly());
			assertTrue("The filesystem should have 3 attribute views", fs.supportedFileAttributeViews().size() == 3);
			assertTrue("The filesystem should have basic view", fs.supportedFileAttributeViews().contains("basic"));
			assertTrue("The filesystem should have sftp view", fs.supportedFileAttributeViews().contains("sftp"));
			assertTrue("The filesystem should have posix view", fs.supportedFileAttributeViews().contains("posix"));
		});
	}
	
	@Test
	public void testState() throws Exception {
		testWithFilesystem(fs -> {
			assertTrue("File system should be open", fs.isOpen());
			fs.close();
			assertFalse("File system should be closed", fs.isOpen());
		});
	}
	
	@Test
	public void testPathMatch() throws Exception {
		testWithFilesystem(fs -> {
			for(int i = 0 ; i < 10 ; i++) {
				Files.createFile(fs.getPath("testfile" + i + ".jar"));
			}
			for(int i = 0 ; i < 10 ; i++) {
				Files.createFile(fs.getPath("testfile" + i + ".txt"));
			}
			
			var pm = fs.getPathMatcher("glob:*.txt");
			int texts = 0;
			try(var str = Files.newDirectoryStream(fs.getPath("."))) {
				for(var f : str)
					if(pm.matches(f)) {
						texts++;
					}
			}
			assertEquals("Should be 10 .txt files", 10, texts);
		});
	}

	@Test(expected = PatternSyntaxException.class)
	public void testBadGlobPattern() throws Exception {
		testWithFilesystem(fs -> {
			Files.createFile(fs.getPath("123"));
			countMatches(fs, fs.getPathMatcher("glob:123\\"));
		});
	}
	
	@Test
	public void testMoreGlobPatterns() throws Exception {
		testWithFilesystem(fs -> {
			Files.createFile(fs.getPath("abc123.txt"));
			Files.createFile(fs.getPath("zzzzzz.jar"));
			Files.createFile(fs.getPath("123"));
			Files.createFile(fs.getPath("abacus.doc"));
			Files.createFile(fs.getPath("banana.doc"));
			Files.createFile(fs.getPath("cherry.doc"));
			Files.createFile(fs.getPath("dancing.doc"));
			Files.createFile(fs.getPath("europe.doc"));
			Files.createFile(fs.getPath("fanta.doc"));
			Files.createFile(fs.getPath("gravy.doc"));
			Files.createFile(fs.getPath("gravyZdoc"));
			Files.createFile(fs.getPath("helium.doc"));
			Files.createFile(fs.getPath("india.dog"));
			Files.createFile(fs.getPath("b!ack.cat"));
			Files.createFile(fs.getPath("BBB,CCC"));
			Files.createFile(fs.getPath("DEF,GHI"));
			Files.createFile(fs.getPath("?aaaa.doc"));
			Files.createFile(fs.getPath("xxxxbxxxx.doc"));
			

			assertEquals(1, countMatches(fs, fs.getPathMatcher("glob:*[^+F]*")));
			assertEquals(11, countMatches(fs, fs.getPathMatcher("glob:*.do?")));
			assertEquals(6, countMatches(fs, fs.getPathMatcher("glob:[abcd]*")));
			assertEquals(1, countMatches(fs, fs.getPathMatcher("glob:[*?]*.doc")));
			assertEquals(16, countMatches(fs, fs.getPathMatcher("glob:[!a]*")));
			assertEquals(4, countMatches(fs, fs.getPathMatcher("glob:{f,g,h}*")));
			assertEquals(2, countMatches(fs, fs.getPathMatcher("glob:???,???")));
			assertEquals(1, countMatches(fs, fs.getPathMatcher("glob:?\\!*")));
			assertEquals(1, countMatches(fs, fs.getPathMatcher("glob:[a!]*.doc")));
			assertEquals(2, countMatches(fs, fs.getPathMatcher("glob:*\\,*")));
			assertEquals(1, countMatches(fs, fs.getPathMatcher("glob:\\Qgravy.doc\\E")));
		});
	}

	protected int countMatches(SftpFileSystem fs, PathMatcher pm) throws IOException {
		int texts = 0;
		try(var str = Files.newDirectoryStream(fs.getPath("."))) {
			for(var f : str)
				if(pm.matches(f.getFileName())) {
					texts++;
				}
		}
		return texts;
	}
	
	@Test
	public void testRegExpPathMatch() throws Exception {
		testWithFilesystem(fs -> {
			for(int i = 0 ; i < 10 ; i++) {
				Files.createFile(fs.getPath("testfile" + i + ".jar"));
			}
			for(int i = 0 ; i < 10 ; i++) {
				Files.createFile(fs.getPath("testfile" + i + ".txt"));
			}
			
			var pm = fs.getPathMatcher("regex:.*\\.txt");
			int texts = 0;
			try(var str = Files.newDirectoryStream(fs.getPath("."))) {
				for(var f : str)
					if(pm.matches(f)) {
						texts++;
					}
			}
			assertEquals("Should be 10 .txt files", 10, texts);
		});
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testBadPathMatch() throws Exception {
		testWithFilesystem(fs -> {
			fs.getPathMatcher("XXXXX");
		});
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testUnknownPathMatch() throws Exception {
		testWithFilesystem(fs -> {
			fs.getPathMatcher("xxxxx:XXXXX");
		});
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testUserLookup() throws Exception {
		testWithFilesystem((fs) -> {
			fs.getUserPrincipalLookupService();
		});
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testWatchService() throws Exception {
		testWithFilesystem((fs) -> {
			fs.newWatchService();
		});
	}
}
