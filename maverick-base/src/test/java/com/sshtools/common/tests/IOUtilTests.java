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
package com.sshtools.common.tests;

import java.io.File;
import java.io.IOException;

import com.sshtools.common.util.IOUtil;

import junit.framework.TestCase;

public class IOUtilTests extends TestCase {

	
	public void testGetFilenameExtension() {
		
		assertEquals("exe", IOUtil.getFilenameExtension("run.exe"));
		assertEquals("exe", IOUtil.getFilenameExtension("/usr/bin/run.exe"));
		assertEquals(null, IOUtil.getFilenameExtension("/usr/bin/run"));
		
	}
	
	public void testGetFilenameWithoutExtension() {
		
		assertEquals("run", IOUtil.getFilenameWithoutExtension("run.exe"));
		assertEquals("/usr/bin/run", IOUtil.getFilenameWithoutExtension("/usr/bin/run"));
		assertEquals("/usr/bin/run", IOUtil.getFilenameWithoutExtension("/usr/bin/run"));
	}
	
	public void testRolloverPartial() throws IOException {
		File parent = new File("tmp");
		IOUtil.delTree(parent);
		
		parent.mkdirs();
		
		File f = new File("tmp/test.log");
		IOUtil.writeUTF8StringToFile(f,  "0000000000");
		IOUtil.writeUTF8StringToFile(new File("tmp/test.1.log"), "1111111111");
		IOUtil.writeUTF8StringToFile(new File("tmp/test.2.log"), "2222222222");
		IOUtil.writeUTF8StringToFile(new File("tmp/test.3.log"), "3333333333");
		IOUtil.writeUTF8StringToFile(new File("tmp/test.4.log"), "4444444444");
		IOUtil.writeUTF8StringToFile(new File("tmp/test.5.log"), "5555555555");
		IOUtil.writeUTF8StringToFile(new File("tmp/test.6.log"), "6666666666");
		
		
		IOUtil.rollover(f, 10);
		
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
		assertEquals(content, IOUtil.readUTF8StringFromFile(file));
	}
	
	public void testRolloverFull() throws IOException {
		File parent = new File("tmp");
		IOUtil.delTree(parent);
		
		parent.mkdirs();
		
		File f = new File("tmp/test.log");
		IOUtil.writeUTF8StringToFile(f,  "0000000000");
		IOUtil.writeUTF8StringToFile(new File("tmp/test.1.log"), "1111111111");
		IOUtil.writeUTF8StringToFile(new File("tmp/test.2.log"), "2222222222");
		IOUtil.writeUTF8StringToFile(new File("tmp/test.3.log"), "3333333333");
		IOUtil.writeUTF8StringToFile(new File("tmp/test.4.log"), "4444444444");
		IOUtil.writeUTF8StringToFile(new File("tmp/test.5.log"), "5555555555");
		IOUtil.writeUTF8StringToFile(new File("tmp/test.6.log"), "6666666666");
		
		
		IOUtil.rollover(f, 6);
		
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
