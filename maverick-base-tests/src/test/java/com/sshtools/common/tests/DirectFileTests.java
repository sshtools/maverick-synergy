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
 * Copyright (C) 2002-2025 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.common.tests;

/*-
 * #%L
 * Base API Tests
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
import com.sshtools.common.files.direct.NioFileFactory;
import com.sshtools.common.files.direct.NioFileFactory.NioFileFactoryBuilder;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.FileUtils;
import com.sshtools.common.util.IOUtils;

public class DirectFileTests extends AbstractFileTest {

	File baseFolder;
	NioFileFactory factory;
	
	protected File getBaseFolder() throws IOException {
		if(Objects.isNull(baseFolder)) {
			baseFolder = Files.createTempDirectory("direct-file").toFile();
			factory =  NioFileFactoryBuilder.create().withHome(baseFolder).build(); 
		}
		return baseFolder;
 	}
	
	protected void setup() throws IOException {
		baseFolder = Files.createTempDirectory("direct-file").toFile();
		factory = NioFileFactoryBuilder.create().withHome(baseFolder).build();
	}
	
	protected void clean() throws IOException {
		FileUtils.deleteFolder(baseFolder);
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
		try(OutputStream out = new FileOutputStream(file);
				RandomInputStream in = new RandomInputStream(65535, size, false)) {
			IOUtils.copy(in, out);
			return in.digest();
		}
	}

	@Override
	protected byte[] hashContent(String path, long size) throws IOException, NoSuchAlgorithmException {
		
		File file = new File(getBaseFolder(), path);
		System.out.println("hashContent " + file.getAbsolutePath());
		try(DigestInputStream in = new DigestInputStream(
				new FileInputStream(file),
						MessageDigest.getInstance("MD5"))) {
			IOUtils.copy(in, new NullOutputStream());
			return in.getMessageDigest().digest();
		}
	}

}
