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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.Arrays;
import com.sshtools.common.util.IOUtils;

public abstract class AbstractFileTest {
	
	
	protected abstract AbstractFile getFile(String path) throws PermissionDeniedException, IOException;
	
	protected abstract void createFile(String path) throws IOException;
	
	protected abstract void createFolder(String path) throws IOException;
	
	protected abstract void assertExists(String path, boolean exists) throws IOException;
	
	protected abstract void deleteFile(String path) throws IOException;
	
	protected abstract void deleteFolder(String path) throws IOException;
	
	protected abstract byte[] createContent(String path, long size) throws IOException, NoSuchAlgorithmException;
	
	protected abstract byte[] hashContent(String path, long size) throws IOException, NoSuchAlgorithmException;
	
	@Test
	public void testFileExists() throws IOException, PermissionDeniedException {
		
		System.out.println("testFileExists");
		
		String path = "exists.txt";
		assertExists(path, false);
		createFile(path);
		assertExists(path, true);
		
		AbstractFile file = getFile(path);
		assertTrue("The object is not a file", file.isFile());
		assertTrue("The file should exist", file.exists());
		
		deleteFile(path);
	}
	
	@Test
	public void testFileDoesNotExists() throws IOException, PermissionDeniedException {
		
		System.out.println("testFileDoesNotExists");
		
		String path = "does-not-exists.txt";
		assertExists(path, false);
		
		AbstractFile file = getFile(path);
		assertFalse("The file should not exist", file.exists());

	}
	
	@Test
	public void testCreateNewFile() throws IOException, PermissionDeniedException {
		
		System.out.println("testCreateNewFile");
		
		String path = "does-not-exist.txt";
		assertExists(path, false);
		
		AbstractFile file = getFile(path);
		assertFalse("The file should not exist", file.exists());
		assertTrue("createNewFile should return true", file.createNewFile());
		assertTrue("The file should exist", file.exists());
		assertTrue("The object should be a file", file.isFile());
		
		assertExists(path, true);
	}
	
	@Test
	public void testCreateFileAlreadyExists() throws IOException, PermissionDeniedException {
		
		System.out.println("testCreateFileAlreadyExists");
		
		String path = "exists.txt";
		assertExists(path, false);
		createFile(path);
		assertExists(path, true);
		
		AbstractFile file = getFile(path);
		assertTrue("The object is not a file", file.isFile());
		assertTrue("The file should exist", file.exists());
		assertFalse("createNewFile should return false if the file exists", file.createNewFile());
		
		deleteFile(path);
	}
	
	@Test
	public void testCreateFolder() throws IOException, PermissionDeniedException {
		System.out.println("testCreateFolder");
		testCreateFolder("folder-to-be-created");
	}
	
	@Test
	public void testCreateMultipleFolders() throws IOException, PermissionDeniedException {
		System.out.println("testCreateMultipleFolders");
		testCreateFolder("parent/folder-to-be-created");
	}
	
	protected void testCreateFolder(String path) throws IOException, PermissionDeniedException {
		
		assertExists(path, false);
		
		AbstractFile file = getFile(path);
		assertFalse("The file should not exist", file.exists());
		assertTrue("createFolder should reutrn true", file.createFolder());
		assertTrue("The file should exist", file.exists());
		assertTrue("The object is not a directory", file.isDirectory());
		System.out.println(file.getAbsolutePath());
		assertExists(path, true);
	}
	
	@Test
	public void testCreateFolderAlreadyExists() throws IOException, PermissionDeniedException {
		
		System.out.println("testCreateFolderAlreadyExists");
		
		String path = "folder-to-be-created";
		assertExists(path, false);
		createFolder(path);
		assertExists(path, true);
		
		AbstractFile file = getFile(path);
		assertTrue("The file should exist", file.exists());
		assertTrue("The object is not a directory", file.isDirectory());
		assertFalse("createFolder should return false", file.createFolder());
		
		deleteFolder(path);
	}
	
	@Test
	public void testDeleteFile() throws IOException, PermissionDeniedException {
		
		System.out.println("testDeleteFile");
		
		String path = "to-be-deleted.txt";
		assertExists(path, false);
		createFile(path);
		assertExists(path, true);
		
		AbstractFile file = getFile(path);
		assertTrue("The file should exist", file.exists());
		assertTrue("delete should return true", file.delete(false));
		assertFalse("The file should not exist", file.exists());
		
		assertExists(path, false);
	}
	
	@Test
	public void testDeleteNonExistentFile() throws IOException, PermissionDeniedException {
		
		System.out.println("testDeleteNonExistentFile");
		
		String path = "to-be-deleted-but-does-not-exist.txt";
		assertExists(path, false);
		
		AbstractFile file = getFile(path);
		assertFalse("The file should not exist", file.exists());
		assertFalse("delete should return false", file.delete(false));
		assertFalse("The file should not exist", file.exists());
		
		assertExists(path, false);
	}

	@Test
	public void testSmallInputStream() throws NoSuchAlgorithmException, IOException, PermissionDeniedException {
		System.out.println("testSmallInputStream");
		testInputStream("small-file.dat", "1kb");
	}
	
	@Test
	public void testLargeInputStream() throws NoSuchAlgorithmException, IOException, PermissionDeniedException {
		System.out.println("testLargeInputStream");
		testInputStream("large-file.dat", "100mb");
	}
	
	@Test
	public void testHugeInputStream() throws NoSuchAlgorithmException, IOException, PermissionDeniedException {
		System.out.println("testHugeInputStream");
		testInputStream("huge-file.dat", "2gb");
	}
	
	@Test
	public void testSmallOutputStream() throws NoSuchAlgorithmException, IOException, PermissionDeniedException {
		System.out.println("testSmallOutputStream");
		testOutputStream("small-output.dat", "1kb");
	}
	
	@Test
	public void testLargeOutputStream() throws NoSuchAlgorithmException, IOException, PermissionDeniedException {
		System.out.println("testLargeOutputStream");
		testOutputStream("large-output.dat", "100mb");
	}

	@Test
	public void testHugeOutputStream() throws NoSuchAlgorithmException, IOException, PermissionDeniedException {
		System.out.println("testHugeOutputStream");
		testOutputStream("huge-output.dat", "2gb");
	}
	
	protected void testInputStream(String path, String size) throws IOException, PermissionDeniedException, NoSuchAlgorithmException {
		
		assertExists(path, false);
		createFile(path);
		long length = IOUtils.fromByteSize(size);
		byte[] contentHash = createContent(path, length);
		assertExists(path, true);
		
		AbstractFile file = getFile(path);
		assertTrue("The file must exist", file.exists());
		assertEquals("The file is not the expected length", length, file.length());
		
		DigestInputStream din = new DigestInputStream(
				file.getInputStream(),
				MessageDigest.getInstance("MD5"));
		
		IOUtils.copy(din, new NullOutputStream());
		IOUtils.closeStream(din);
		file.refresh();
		
		assertTrue("The content digests must match", Arrays.areEqual(din.getMessageDigest().digest(), contentHash));
		
		deleteFile(path);
	}
	
	protected void testOutputStream(String path, String size) throws IOException, PermissionDeniedException, NoSuchAlgorithmException {
		
		assertExists(path, false);

		long length = IOUtils.fromByteSize(size);
		
		AbstractFile file = getFile(path);

		RandomInputStream in = new RandomInputStream(65535, length, false);
		OutputStream out = file.getOutputStream();
		
		IOUtils.copy(in, out);
		out.flush();
		IOUtils.closeStream(out);
		file.refresh();
		
		byte[] contentHash = hashContent(path, length);
		
		assertTrue("The file must exist", file.exists());
		assertEquals("The file is not the expected length", length, file.length());
		assertTrue("The content digests must match", Arrays.areEqual(in.digest(), contentHash));
		
		deleteFile(path);
	}
	
	public void testResolveFile() {
		
	}
	
	public void testTruncate() {
		
	}
	
	public void testRefresh() {
		
	}
	
	public void testWritableFile() {
		
	}
	
	public void testNonWritableFile() {
		
	}
	
	public void testReadableFile() {
		
	}
	
	public void testNonReadableFile() {
		
	}
	
	public void testHiddenFile() {
		
	}
	
	public void testNonHiddenFile() {
		
	}
	
	public void testLength() {
		
	}
	
	public void testFolderLength() {
		
	}
	
	public void testDeleteEmptyFolder() {
		
	}
	
	public void testDeleteNonEmptyFolder() {
		
	}
	
	public void testDeleteRecursive() {
		
	}
	
	public void testAppendOutputStream() {
		
	}
	
	public void testName() {
		
	}
	
	public void testChildren() {
		
	}
	
	@Test
	public void testEquals() throws IOException, PermissionDeniedException {
		
		String path = "equals.txt";
		createFile(path);
		assertExists(path, true);
		
		AbstractFile file1 = getFile(path);
		AbstractFile file2 = getFile(path);
		
		assertFalse("Objects should not be the same instance", file1 == file2);
		assertTrue("equals should return true", file1.equals(file2));
		
		deleteFile(path);
	}
	
	@Test
	public void testNotEquals() throws IOException, PermissionDeniedException {
		
		String path1 = "not-equals1.txt";
		createFile(path1);
		assertExists(path1, true);
		
		String path2 = "not-equals2.txt";
		createFile(path2);
		assertExists(path2, true);
		
		AbstractFile file1 = getFile(path1);
		AbstractFile file2 = getFile(path2);
		
		assertFalse("Objects should not be the same instance", file1 == file2);
		assertFalse("equals should return false", file1.equals(file2));
		
		deleteFile(path1);
		deleteFile(path2);
	}
	
	@Test
	public void testMatchingHashCode() throws IOException, PermissionDeniedException {
		
		String path = "hashcode.txt";
		createFile(path);
		assertExists(path, true);
		
		AbstractFile file1 = getFile(path);
		AbstractFile file2 = getFile(path);
		
		assertFalse("Objects should not be the same instance", file1 == file2);
		assertTrue("hashCode return values should match", file1.hashCode() == file2.hashCode());
		
		deleteFile(path);
	}
	
	@Test
	public void testNonMatchingHashCode() throws IOException, PermissionDeniedException {
		
		String path1 = "hashcode1.txt";
		createFile(path1);
		assertExists(path1, true);
		
		String path2 = "hashcode2.txt";
		createFile(path2);
		assertExists(path2, true);
		
		AbstractFile file1 = getFile(path1);
		AbstractFile file2 = getFile(path2);
		
		assertFalse("Objects should not be the same instance", file1 == file2);
		assertFalse("hashCode return values should not match", file1.hashCode() == file2.hashCode());
		
		deleteFile(path1);
		deleteFile(path2);
	}
	
	public void testAbsolutePath() {
		
	}
	
	public void testCanonicalPath() {
		
	}
	
	public void testPermissions() {
		
	}
	
	public void testGetAttributes() {
		
	}
	
	public void testSetAttributes() {
		
	}
	

	public void testCopyFromFileToFile() {
		
	}
	
	public void testCopyFromFileToFolder() {
		
	}
	
	public void testCopyFromFolderToFolder() {
		
	}
	
	public void testCopyFromFolderToFile() {
		
	}
	
	public void testMoveFileToFile() {
		
	}
	
	public void testMoveFileToFolder() {
		
	}
	
	public void testMoveFolderToFolder() {
		
	}
	
	public void testMoveFolderToFile() {
		
	}
	
	public void testLastModified() {
		
	}
	
	public void testRandomAccess() {
		
	}
}
