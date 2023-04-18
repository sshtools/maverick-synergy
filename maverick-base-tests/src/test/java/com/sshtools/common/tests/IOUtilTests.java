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
