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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.direct.DirectFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.IOUtils;

public class DirectFileTests extends AbstractFileTest {

	File baseFolder;
	DirectFileFactory factory;
	
	protected File getBaseFolder() throws IOException {
		if(Objects.isNull(baseFolder)) {
			baseFolder = Files.createTempDirectory("direct-file").toFile();
			factory =  new DirectFileFactory(baseFolder); 
		}
		return baseFolder;
 	}
	

	@Override
	protected String getBasePath() throws IOException {
		return baseFolder.getAbsolutePath();
	}
	
	@Override
	protected String getCanonicalPath() throws IOException {
		return baseFolder.getCanonicalPath();
	}
	
	@Override
	protected AbstractFile getFile(String path) throws PermissionDeniedException, IOException {
		return factory.getFile(path);
	}

	@Override
	protected void createFile(String path) throws IOException {
		
		File file = new File(getBaseFolder(), path);
		System.out.println("createFile " + file.getAbsolutePath());
		assertTrue(file.createNewFile());
		assertTrue(file.exists());
	}

	@Override
	protected void createFolder(String path) throws IOException {
		File file = new File(getBaseFolder(), path);
		System.out.println("createFolder " + file.getAbsolutePath());
		assertTrue(file.mkdir());
		assertTrue(file.exists());
	}

	@Override
	protected void assertExists(String path, boolean exists) throws IOException {
				
		File file = new File(getBaseFolder(), path);
		System.out.println("assertExists " + file.getAbsolutePath());
		assertEquals(file.exists(), exists);

	}

	@Override
	protected void deleteFile(String path) throws IOException {
		
		File file = new File(getBaseFolder(), path);
		System.out.println("deleteFile " + file.getAbsolutePath());
		assertTrue(file.delete());
		
	}

	@Override
	protected void deleteFolder(String path) throws IOException {
		
		File file = new File(getBaseFolder(), path);
		System.out.println("deleteFolder " + file.getAbsolutePath());
		assertTrue(file.delete());
		
	}

	@Override
	protected byte[] createContent(String path, long size) throws IOException, NoSuchAlgorithmException {
		
		File file = new File(getBaseFolder(), path);
		System.out.println("createContent " + file.getAbsolutePath());
		OutputStream out = new FileOutputStream(file);
		RandomInputStream in = new RandomInputStream(65535, size, false);
		IOUtils.copy(in, out);
		return in.digest();
	}

	@Override
	protected byte[] hashContent(String path, long size) throws IOException, NoSuchAlgorithmException {
		
		File file = new File(getBaseFolder(), path);
		System.out.println("hashContent " + file.getAbsolutePath());
		DigestInputStream in = new DigestInputStream(
				new FileInputStream(file),
						MessageDigest.getInstance("MD5"));
				
		IOUtils.copy(in, new NullOutputStream());
		
		return in.getMessageDigest().digest();
	}

}
