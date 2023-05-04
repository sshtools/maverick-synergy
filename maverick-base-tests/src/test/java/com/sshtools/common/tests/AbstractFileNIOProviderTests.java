/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.direct.DirectFileFactory;
import com.sshtools.common.files.nio.AbstractFileURI;
import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.Arrays;
import com.sshtools.common.util.IOUtils;

public class AbstractFileNIOProviderTests {

	FileSystem fs;
	
	@Before
	public void setupTest() throws IOException {
		Map<String,Object> env = new HashMap<>();
		SshConnection con = new MockConnection("lee", 
				UUID.randomUUID().toString(),
				new InetSocketAddress(InetAddress.getLocalHost(), 22),
				new InetSocketAddress(InetAddress.getLocalHost(), 22),
				new MockContext());
		
		con.getContext().getPolicy(FileSystemPolicy.class).setFileFactory((c) -> createTestFileSystem());
		env.put("connection", con);
		fs = FileSystems.newFileSystem(AbstractFileURI.create(con, "/"), env);
	}
	
	private AbstractFileFactory<?> createTestFileSystem() throws IOException {
		
		File tmp = Files.createTempDirectory("synergy").toFile();
		File f = new File(tmp, "file.txt");
		IOUtils.writeUTF8StringToFile(f, "This is a test file");
		f.createNewFile();
		File folder = new File(tmp, "folder");
		folder.mkdirs();
		File child = new File(folder, "child.txt");
		child.createNewFile();
		
		return new DirectFileFactory(tmp);

	}
	
	@Test
	public void testDefaultPath() throws IOException {

		Path path = fs.getPath("");
		Assert.assertTrue(Files.exists(path));
		Assert.assertEquals("/", path.toAbsolutePath().toString());
	}
	
	@Test
	public void testCurrentPath() throws IOException {

		Path path = fs.getPath(".");
		Assert.assertTrue(Files.exists(path));
		Assert.assertEquals("/", path.normalize().toAbsolutePath().toString());
	}
	
	@Test
	public void testPathExists() throws IOException {

		Path path = fs.getPath("file.txt");
		Assert.assertTrue(Files.exists(path));

	}

	@Test
	public void testPathSize() throws IOException {

		Path path = fs.getPath("file.txt");
		
		long count = Files.size(path);
		
		Assert.assertEquals(19, count);

	}
	
	
	@Test
	public void testPathInputStream() throws IOException {

		Path path = fs.getPath("file.txt");
		
		long count = Files.size(path);
		
		InputStream in = Files.newInputStream(path);
		while(in.read() > -1) count--;
		
		Assert.assertEquals(0, count);
	}
	
	@Test
	public void testPathOutputStreamValidateContent() throws IOException {

		Path path = fs.getPath("file.txt");
		
		byte[] tmp = new byte[65536];
		new SecureRandom().nextBytes(tmp);
		
		OutputStream out = Files.newOutputStream(path);
		out.write(tmp);
		out.flush();
		out.close();
		
		Assert.assertEquals(65536, Files.size(path));
		
		InputStream in = Files.newInputStream(path);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		IOUtils.copy(in, bout);
		
		Assert.assertTrue(Arrays.areEqual(tmp, bout.toByteArray()));
	}
	
	@Test
	public void testNewPathOutputStreamValidateContent() throws IOException {

		Path path = fs.getPath("file2.txt");
		
		byte[] tmp = new byte[65536];
		new SecureRandom().nextBytes(tmp);
		
		OutputStream out = Files.newOutputStream(path);
		out.write(tmp);
		out.flush();
		out.close();
		
		Assert.assertEquals(65536, Files.size(path));
		
		InputStream in = Files.newInputStream(path);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		IOUtils.copy(in, bout);
		
		Assert.assertTrue(Arrays.areEqual(tmp, bout.toByteArray()));
	}
	
	
	@Test
	public void testPathDelete() throws IOException {

		Path path = fs.getPath("file.txt");
		Files.delete(path);
		
		Assert.assertFalse(Files.exists(path));

	}
	
	@Test
	public void testFolderExists() throws IOException {

		Path path = fs.getPath("folder");
		Assert.assertTrue(Files.exists(path));

	}
	
	@Test
	public void testFolderChildPathExists() throws IOException {

		Path path = fs.getPath("folder/child.txt");
		Assert.assertTrue(Files.exists(path));

	}
	
	@Test
	public void testNonExistentPath() throws IOException {

		Path path = fs.getPath("this_does_not_exist");
		Assert.assertFalse(Files.exists(path));
	}
	
	@Test
	public void testFolderCreation() throws IOException {
		
		Path path = fs.getPath("newfolder");
		Files.createDirectory(path);
		
		Assert.assertTrue(Files.exists(path));
		
	}
	
	@Test
	public void testFolderListing() {
		List<String> fileNames = new ArrayList<>();
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(fs.getPath("."))) {
            for (Path path : directoryStream) {
                fileNames.add(path.toString());
            }
        } catch (IOException ex) {}
		
		Assert.assertEquals(2, fileNames.size());
	}
}
