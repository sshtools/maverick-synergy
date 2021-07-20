
package com.sshtools.common.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.util.Arrays;
import com.sshtools.common.util.FileUtils;
import com.sshtools.common.util.IOUtils;

public abstract class AbstractFileTest {
	
	
	protected abstract AbstractFile getFile(String path) throws PermissionDeniedException, IOException;
	
	protected abstract String getBasePath() throws IOException;
	
	protected abstract String getCanonicalPath() throws IOException;
	
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
		createFile(path);
		
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
		createFile(path);
		
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
		createFile(path);
		
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
		
		createFile(path);
		long length = IOUtils.fromByteSize(size);
		byte[] contentHash = createContent(path, length);
		
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
	
	@Test
	public void testName() throws PermissionDeniedException, IOException {
		
		String path = "name.txt";
		createFile(path);
		
		try {
			AbstractFile file = getFile(path);
			assertTrue("The name is incorrect", path.equals(file.getName()));
		} finally {
			deleteFile(path);
		}
	}
	
	@Test
	public void testChildren() throws IOException, PermissionDeniedException {
		
		createFolder("tree");
		createFile("tree/1");
		createFile("tree/2");
		createFile("tree/3");
		createFolder("tree/leaf");
		
		try {
			Set<String> names = new HashSet<>(java.util.Arrays.asList("1", "2", "3", "leaf"));
			AbstractFile dir = getFile("tree");
			for(AbstractFile file : dir.getChildren()) {
				assertTrue("Child is not valid", names.contains(file.getName()));
			}
			
			assertEquals("There must be 4 children", 4, dir.getChildren().size());
		} finally {
			deleteFile("tree/1");
			deleteFile("tree/2");
			deleteFile("tree/3");
			deleteFolder("tree/leaf");
			deleteFolder("tree");
		}
		
	}
	
	@Test
	public void testEquals() throws IOException, PermissionDeniedException {
		
		String path = "equals.txt";
		createFile(path);
		
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
		
		String path2 = "not-equals2.txt";
		createFile(path2);
		
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
		
		String path2 = "hashcode2.txt";
		createFile(path2);
		
		AbstractFile file1 = getFile(path1);
		AbstractFile file2 = getFile(path2);
		
		assertFalse("Objects should not be the same instance", file1 == file2);
		assertFalse("hashCode return values should not match", file1.hashCode() == file2.hashCode());
		
		deleteFile(path1);
		deleteFile(path2);
	}
	
	@Test
	public void testAbsolutePath() throws IOException, PermissionDeniedException {
		
		String path = "relative.txt";
		createFile(path);
		
		AbstractFile file = getFile(path);
		String absolutePath = file.getAbsolutePath();
		String nativePath = FileUtils.checkEndsWithSlash(getBasePath()) + file.getName();
		
		assertTrue("Absolute path must equal the native absolute path", absolutePath.equals(nativePath));
		deleteFile(path);
		
	}
	
	@Test
	public void testCanonicalPath() throws IOException, PermissionDeniedException {
		
		createFolder("child");
		String path = "child/.././relative.txt";
		createFile(path);
		
		AbstractFile file = getFile(path);
		String canoncial = file.getCanonicalPath();
		String nativePath =  FileUtils.checkEndsWithSlash(getCanonicalPath())  + file.getName();
		
		assertTrue("Absolute path must equal the native absolute path", canoncial.equals(nativePath));
		deleteFile(path);
		deleteFolder("child");
	}
	
	public void testPermissions() {
		
	}
	
	public void testGetAttributes() {
		
	}
	
	public void testSetAttributes() {
		
	}
	
	@Test
	public void testCopyFromFile() throws NoSuchAlgorithmException, IOException, PermissionDeniedException {
		
		String path1 = "file-to-copy.dat";
		createFile(path1);
		long length = IOUtils.fromByteSize("1mb");
		byte[] sourceHash = createContent(path1, length);
		
		String path2 = "copied-file.dat";
		
		AbstractFile file1 = getFile(path1);
		assertTrue("The source file should exist", file1.exists());
		assertEquals("The source length should be 1mb", length, file1.length());
		
		AbstractFile file2 = getFile(path2);
		assertFalse("The desination file should not exist", file2.exists());
		
		file2.copyFrom(file1);
		file2.refresh();

		
		assertTrue("The destination file should exist", file2.exists());
		assertEquals("The destination length should be 1mb", length, file2.length());
		
		byte[] destinationHash = hashContent(path2, length);
		assertTrue("The source and destination hash should match", Arrays.areEqual(sourceHash, destinationHash));
		
		deleteFile(path1);
		deleteFile(path2);
		
	}
	
	public void testCopyFromFolder() throws PermissionDeniedException, IOException, NoSuchAlgorithmException {
		
		
	}
	
	@Test
	public void testMoveFileToFile() throws PermissionDeniedException, IOException, NoSuchAlgorithmException {
		
		String path1 = "file-to-move.dat";
		createFile(path1);
		long length = IOUtils.fromByteSize("1mb");
		byte[] sourceHash = createContent(path1, length);
		
		String path2 = "moved-file.dat";
		
		AbstractFile file1 = getFile(path1);
		assertTrue("The source file should exist", file1.exists());
		assertEquals("The source length should be 1mb", length, file1.length());
		
		AbstractFile file2 = getFile(path2);
		assertFalse("The desination file should not exist", file2.exists());
		
		file1.moveTo(file2);
		file1.refresh();
		file2.refresh();

		assertFalse("The source file should not exist", file1.exists());
		assertTrue("The destination file should exist", file2.exists());
		assertEquals("The destination length should be 1mb", length, file2.length());
		
		byte[] destinationHash = hashContent(path2, length);
		assertTrue("The source and destination hash should match", Arrays.areEqual(sourceHash, destinationHash));
		
		deleteFile(path2);
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
