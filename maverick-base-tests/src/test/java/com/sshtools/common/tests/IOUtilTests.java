package com.sshtools.common.tests;

import java.io.File;
import java.io.IOException;

import com.sshtools.common.util.IOUtils;

import junit.framework.TestCase;

public class IOUtilTests extends TestCase {

	
	public void testGetFilenameExtension() {
		
		assertEquals("exe", IOUtils.getFilenameExtension("run.exe"));
		assertEquals("exe", IOUtils.getFilenameExtension("/usr/bin/run.exe"));
		assertEquals(null, IOUtils.getFilenameExtension("/usr/bin/run"));
		
	}
	
	public void testGetFilenameWithoutExtension() {
		
		assertEquals("run", IOUtils.getFilenameWithoutExtension("run.exe"));
		assertEquals("/usr/bin/run", IOUtils.getFilenameWithoutExtension("/usr/bin/run"));
		assertEquals("/usr/bin/run", IOUtils.getFilenameWithoutExtension("/usr/bin/run"));
	}
	
	public void testRolloverPartial() throws IOException {
		File parent = new File("tmp");
		IOUtils.silentRecursiveDelete(parent);
		
		parent.mkdirs();
		
		File f = new File("tmp/test.log");
		IOUtils.writeUTF8StringToFile(f,  "0000000000");
		IOUtils.writeUTF8StringToFile(new File("tmp/test.1.log"), "1111111111");
		IOUtils.writeUTF8StringToFile(new File("tmp/test.2.log"), "2222222222");
		IOUtils.writeUTF8StringToFile(new File("tmp/test.3.log"), "3333333333");
		IOUtils.writeUTF8StringToFile(new File("tmp/test.4.log"), "4444444444");
		IOUtils.writeUTF8StringToFile(new File("tmp/test.5.log"), "5555555555");
		IOUtils.writeUTF8StringToFile(new File("tmp/test.6.log"), "6666666666");
		
		
		IOUtils.rollover(f, 10);
		
		assertTrue(!f.exists());
		assertExistsAndHasContent(new File("tmp/test.1.log"), "0000000000");
		assertExistsAndHasContent(new File("tmp/test.2.log"), "1111111111");
		assertExistsAndHasContent(new File("tmp/test.3.log"), "2222222222");
		assertExistsAndHasContent(new File("tmp/test.4.log"), "3333333333");
		assertExistsAndHasContent(new File("tmp/test.5.log"), "4444444444");
		assertExistsAndHasContent(new File("tmp/test.6.log"), "5555555555");
		assertExistsAndHasContent(new File("tmp/test.7.log"), "6666666666");

		
	}
	
	private void assertExistsAndHasContent(File file, String content) throws IOException {
		assertTrue(file.exists());
		assertEquals(content, IOUtils.readUTF8StringFromFile(file));
	}
	
	public void testRolloverFull() throws IOException {
		File parent = new File("tmp");
		IOUtils.silentRecursiveDelete(parent);
		
		parent.mkdirs();
		
		File f = new File("tmp/test.log");
		IOUtils.writeUTF8StringToFile(f,  "0000000000");
		IOUtils.writeUTF8StringToFile(new File("tmp/test.1.log"), "1111111111");
		IOUtils.writeUTF8StringToFile(new File("tmp/test.2.log"), "2222222222");
		IOUtils.writeUTF8StringToFile(new File("tmp/test.3.log"), "3333333333");
		IOUtils.writeUTF8StringToFile(new File("tmp/test.4.log"), "4444444444");
		IOUtils.writeUTF8StringToFile(new File("tmp/test.5.log"), "5555555555");
		IOUtils.writeUTF8StringToFile(new File("tmp/test.6.log"), "6666666666");
		
		
		IOUtils.rollover(f, 6);
		
		assertTrue(!f.exists());
		assertExistsAndHasContent(new File("tmp/test.1.log"), "0000000000");
		assertExistsAndHasContent(new File("tmp/test.2.log"), "1111111111");
		assertExistsAndHasContent(new File("tmp/test.3.log"), "2222222222");
		assertExistsAndHasContent(new File("tmp/test.4.log"), "3333333333");
		assertExistsAndHasContent(new File("tmp/test.5.log"), "4444444444");
		assertExistsAndHasContent(new File("tmp/test.6.log"), "5555555555");
		assertFalse(new File("tmp/test.7.log").exists());
		
	}
}
