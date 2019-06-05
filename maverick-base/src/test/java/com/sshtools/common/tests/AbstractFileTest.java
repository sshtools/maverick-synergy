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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.AbstractFileRandomAccess;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

public abstract class AbstractFileTest {
	
	/**
	 * Provide instance of Abstract file, which will tested against. DirectFile, InMemory ......
	 * 
	 * @param path
	 * @param con
	 * @return
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public abstract AbstractFile getAbstractFile(String path, SshConnection con) throws PermissionDeniedException, IOException;
	
	/**
	 * SshConnection to be used for the test.
	 * 
	 * @return
	 */
	public abstract SshConnection getSshConnection();
	
	/**
	 * Provide a single test directory, all test files will be created under this directory, for easy cleanup.
	 * 
	 * @return
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public abstract AbstractFile getMainTestDirectory() throws PermissionDeniedException, IOException;
	
	/**
	 * Unassuming any underlying OS or Protocol.
	 * Easy for test setups.
	 * 
	 * @return
	 */
	public abstract String getPathSeperator();
	
	/**
	 * Unassuming any underlying OS or Protocol.
	 * Easy for test setups.
	 * 
	 * @return
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public abstract  AbstractFile getHomeDirectory() throws PermissionDeniedException, IOException;
	
	/**
	 * Unassuming about getChildren call, some API lists recursive by default some not.
	 * In memory lists all.
	 * Java File lists first level only.
	 * 
	 * @param folder
	 * @return
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public abstract List<AbstractFile> getListOfFilesRecursive(AbstractFile folder) throws PermissionDeniedException, IOException;
	
	
	/**
	 * Different instance of Abstract File can have different semantics for hidden file, hence explicitly asking for one.
	 * 
	 * @return
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public abstract  AbstractFile getHiddenFile() throws PermissionDeniedException, IOException;
	
	
	/**
	 * Different instance of Abstract File can have different semantics for hidden file, hence explicitly asking for one.
	 * 
	 * @return
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public abstract  AbstractFile getNonHiddenFile() throws PermissionDeniedException, IOException;
	
	
	/**
	 * Different instance of Abstract File can have different semantics for readable file, hence explicitly asking for one.
	 * 
	 * @return
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public abstract  AbstractFile getReadableFile() throws PermissionDeniedException, IOException;
	
	
	/**
	 * Different instance of Abstract File can have different semantics for readable file, hence explicitly asking for one.
	 * 
	 * @return
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public abstract  AbstractFile getNonReadableFile() throws PermissionDeniedException, IOException;
	
	
	/**
	 * Different instance of Abstract File can have different semantics for writable file, hence explicitly asking for one.
	 * 
	 * @return
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public abstract  AbstractFile getWritableFile() throws PermissionDeniedException, IOException;
	
	
	/**
	 * Different instance of Abstract File can have different semantics for writable file, hence explicitly asking for one.
	 * 
	 * @return
	 * @throws PermissionDeniedException
	 * @throws IOException
	 */
	public abstract  AbstractFile getNonWritableFile() throws PermissionDeniedException, IOException;
	

	@Test
	public void testRelativePathCreation() throws PermissionDeniedException, IOException {
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			file =  getAbstractFile(".ssh", sshConnection);
			file.createNewFile();
			
			assertEquals(pathInDirectory(getHomeDirectory(), ".ssh"), file.getCanonicalPath());
		} finally {
			cleanUp(file);
		}
	}
	
	@Test
	public void testAbsolutePathCreation() throws PermissionDeniedException, IOException {
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			file =  getAbstractFile("/Application", sshConnection);
			file.createNewFile();
			
			assertEquals("/Application", file.getCanonicalPath());
		} finally {
			cleanUp(file);
		}
	}
	
	@Test
	public void testInputStream() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "input_stream_read_test_directory"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			file =  getAbstractFile(pathInDirectory(fileDirectory, "test.txt") , sshConnection);
			file.createNewFile();
			
			byte[] data = new byte[] {1,2,3,4,5,6,7,8};
			addContentToFile(file, data);
			
			
			byte[] result = getFileContent(file);
			
			assertArrayEquals(data, result);
		} finally {
			cleanUp(file);
			cleanUp(fileDirectory);
		}
		
	}
	
	
	@Test
	public void testCreateAndExists() throws PermissionDeniedException, IOException{
		AbstractFile fileDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "exist_test_directory"), sshConnection);
			fileDirectory.createFolder();
			file =  getAbstractFile(pathInDirectory(fileDirectory, "exist_file.txt"), sshConnection);
			file.createNewFile();
			
			assertTrue(file.exists());
		} finally {
			cleanUp(file);
			cleanUp(fileDirectory);
		}
	}
	
	
	@Test
	public void testListChildren() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile file1 = null;
		AbstractFile file2 = null;
		
		try {
		
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "child_list_test_directory"), sshConnection);
			fileDirectory.createFolder();
			
			file1 =  getAbstractFile(pathInDirectory(fileDirectory, "file1.txt"), sshConnection);
			file1.createNewFile();
			file2 =  getAbstractFile(pathInDirectory(fileDirectory, "file2.txt"), sshConnection);
			file2.createNewFile();
			
			assertTrue(fileDirectory.exists());
			assertTrue(file1.exists());
			assertTrue(file2.exists());
			
			List<AbstractFile> files = this.getListOfFilesRecursive(fileDirectory);
			assertEquals(2, files.size());
			
			assertEquals(1, files.stream().filter((af) -> af.getName().equals("file1.txt")).count());
			assertEquals(1, files.stream().filter((af) -> af.getName().equals("file2.txt")).count());
		} finally {
			cleanUp(file1);
			cleanUp(file2);
			cleanUp(fileDirectory);
		}
	}
	
	@Test
	public void new_file_should_have_absolute_path() throws PermissionDeniedException, IOException {
		AbstractFile file = null;
		try {
			SshConnection sshConnection = getSshConnection();
			file =  getAbstractFile("/Application", sshConnection);
			file.createNewFile();
			
			assertEquals("/Application", file.getAbsolutePath());
		} finally {
			cleanUp(file);
		}
	}
	
	@Test
	public void testIsDirectoryWhenDirectory() throws PermissionDeniedException, IOException {
		AbstractFile parentDirectory = null;
		AbstractFile directory = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			parentDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "directory_is_directory"), sshConnection);
			parentDirectory.createFolder();
			directory =  getAbstractFile(pathInDirectory(parentDirectory, "directory"), sshConnection);
			directory.createFolder();
			
			assertTrue(directory.isDirectory());
		} finally {
			cleanUp(directory);
			cleanUp(parentDirectory);
		}
	}
	
	@Test
	public void testIsDirectoryWhenFile() throws PermissionDeniedException, IOException {
		AbstractFile parentDirectory = null;
		AbstractFile file = null;
		
		try {
		
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			parentDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "directory_is_directory"), sshConnection);
			parentDirectory.createFolder();
			file =  getAbstractFile(pathInDirectory(parentDirectory, "file.txt"), sshConnection);
			file.createNewFile();
			
			assertFalse(file.isDirectory());
		} finally {
			cleanUp(file);
			cleanUp(parentDirectory);
		}
	}
	
	@Test
	public void testIsFileWhenFile() throws PermissionDeniedException, IOException {
		AbstractFile parentDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			parentDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "file_is_file"), sshConnection);
			parentDirectory.createFolder();
			file =  getAbstractFile(pathInDirectory(parentDirectory, "file.txt"), sshConnection);
			file.createNewFile();
			
			assertTrue(file.isFile());
		} finally {
			cleanUp(file);
			cleanUp(parentDirectory);
		}
	}
	
	@Test
	public void testIsFileWhenDirectory() throws PermissionDeniedException, IOException {
		AbstractFile parentDirectory = null;
		AbstractFile directory = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			parentDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "file_is_file"), sshConnection);
			parentDirectory.createFolder();
			directory =  getAbstractFile(pathInDirectory(parentDirectory, "directory"), sshConnection);
			directory.createFolder();
			
			assertFalse(directory.isFile());
		} finally {
			cleanUp(directory);
			cleanUp(parentDirectory);
		}
	}
	
	@Test
	public void testOutputStream() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "output_stream_write_test_directory"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			file =  getAbstractFile(pathInDirectory(fileDirectory, "test.txt") , sshConnection);
			file.createNewFile();
			
			byte[] data = new byte[] {1,2,3,4,5,6,7,8};
			addContentToFile(file, data);
			
			byte[] result = getFileContent(file);
			
			assertArrayEquals(data, result);
		} finally {
			cleanUp(file);
			cleanUp(fileDirectory);
		}
		
	}
	
	@Test
	public void testIsHiddenWhenHidden() throws PermissionDeniedException, IOException {
		AbstractFile  hidden = null;
		try {
			hidden = getHiddenFile();
			
			if (hidden.getName().contains("_ignore_")) {
				return;
			}
			
			assertTrue(hidden.isHidden());
			
		} finally {
			cleanUp(hidden);
		}
	}
	
	@Test
	public void testIsHiddenWhenNotHidden() throws PermissionDeniedException, IOException {
		AbstractFile  nonHidden = null;
		try {
			nonHidden = getNonHiddenFile();
			
			if (nonHidden.getName().contains("_ignore_")) {
				return;
			}
			
			assertFalse(nonHidden.isHidden());
			
		} finally {
			cleanUp(nonHidden);
		}
	}
	
	@Test
	public void testIsReadableWhenReadable() throws PermissionDeniedException, IOException {
		AbstractFile  readable = null;
		try {
			readable = getReadableFile();
			
			if (readable.getName().contains("_ignore_")) {
				return;
			}
			
			assertTrue(readable.isReadable());
			
		} finally {
			cleanUp(readable);
		}
	}
	
	@Test
	public void testIsReadableWhenNotReadable() throws PermissionDeniedException, IOException {
		AbstractFile  nonReadable = null;
		try {
			nonReadable = getNonReadableFile();
			
			if (nonReadable.getName().contains("_ignore_")) {
				return;
			}
			
			assertFalse(nonReadable.isReadable());
			
		} finally {
			cleanUp(nonReadable);
		}
	}
	
	@Test
	public void testIsWritableWhenWritable() throws PermissionDeniedException, IOException {
		AbstractFile  writable = null;
		try {
			writable = getWritableFile();
			
			if (writable.getName().contains("_ignore_")) {
				return;
			}
			
			assertTrue(writable.isWritable());
			
		} finally {
			cleanUp(writable);
		}
	}
	
	@Test
	public void testIsWritableWhenNotWritable() throws PermissionDeniedException, IOException {
		AbstractFile  nonWritable = null;
		try {
			nonWritable = getNonWritableFile();
			
			if (nonWritable.getName().contains("_ignore_")) {
				return;
			}
			
			assertFalse(nonWritable.isWritable());
			
		} finally {
			cleanUp(nonWritable);
		}
	}
	
	
	@Test
	public void testCreateFolder() throws PermissionDeniedException, IOException {
		AbstractFile directory = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			directory =  getAbstractFile(pathInDirectory(testDirectory, "create_new_directory"), sshConnection);
			directory.createFolder();
			
			assertTrue(directory.exists());
		} finally {
			cleanUp(directory);
		}
	}
	
	@Test
	public void testCopyFrom() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile fileSrc = null;
		AbstractFile fileDst = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "copy_from_file_test_directory"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			fileSrc =  getAbstractFile(pathInDirectory(fileDirectory, "testSrc.txt") , sshConnection);
			fileSrc.createNewFile();
			
			byte[] data = new byte[] {1,2,3,4,5,6,7,8};
			addContentToFile(fileSrc, data);
			
			fileDst =  getAbstractFile(pathInDirectory(fileDirectory, "testDst.txt") , sshConnection);
			fileDst.createNewFile();
			
			fileDst.copyFrom(fileSrc);
			
			byte[] result = getFileContent(fileDst);
			
			assertArrayEquals(data, result);
		} finally {
			cleanUp(fileSrc);
			cleanUp(fileDst);
			cleanUp(fileDirectory);
		}
	}
	
	
	@Test
	public void testCopyFromDirectoryToDirectory() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectorySrc = null;
		AbstractFile file1Src = null;
		AbstractFile file2Src = null;
		
		AbstractFile fileDirectoryDst = null;
		AbstractFile file1Dst = null;
		AbstractFile file2Dst = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectorySrc =  getAbstractFile(pathInDirectory(testDirectory,  "copy_from_another_directory_test_directory_src"), sshConnection);
			fileDirectorySrc.createFolder();
			assertTrue(fileDirectorySrc.exists());
			
			file1Src =  getAbstractFile(pathInDirectory(fileDirectorySrc, "test1Src.txt") , sshConnection);
			file1Src.createNewFile();
			byte[] dataSrc1 = new byte[] {1,2,3,4,5,6,7,8};
			addContentToFile(file1Src, dataSrc1);
			
			file2Src =  getAbstractFile(pathInDirectory(fileDirectorySrc, "test2Src.txt") , sshConnection);
			file2Src.createNewFile();
			byte[] dataSrc2 = new byte[] {9,10,11,12,13,14,15,16};
			addContentToFile(file2Src, dataSrc2);
			
			fileDirectoryDst =  getAbstractFile(pathInDirectory(testDirectory,  "copy_from_another_directory_test_directory_dst"), sshConnection);
			fileDirectoryDst.createFolder();
			assertTrue(fileDirectoryDst.exists());
			
			file1Dst =  getAbstractFile(pathInDirectory(fileDirectoryDst, "test1Dst.txt") , sshConnection);
			file1Dst.createNewFile();
			byte[] dataDst1 = new byte[] {20};
			addContentToFile(file1Dst, dataDst1);
			
			file2Dst =  getAbstractFile(pathInDirectory(fileDirectoryDst, "test2Dst.txt") , sshConnection);
			file2Dst.createNewFile();
			byte[] dataDst2 = new byte[] {30};
			addContentToFile(file2Dst, dataDst2);
			
			List<AbstractFile> childrenDst = getListOfFilesRecursive(fileDirectoryDst);
			
			assertEquals(2, childrenDst.size());
			
			fileDirectoryDst.copyFrom(fileDirectorySrc);
			
			childrenDst = getListOfFilesRecursive(fileDirectoryDst);
			List<AbstractFile> childrenSrc = getListOfFilesRecursive(fileDirectorySrc);
			
			assertEquals(4, childrenDst.size());
			
			assertEquals(2, childrenSrc.size());
			
			Set<String> fileNamesDst = childrenDst.stream().map((fo) ->fo.getName() ).collect(Collectors.toSet());
			Set<String> fileNamesSrc = childrenSrc.stream().map((fo) ->fo.getName() ).collect(Collectors.toSet());
			
			assertTrue(fileNamesDst.contains("test1Src.txt"));
			assertTrue(fileNamesDst.contains("test2Src.txt"));
			assertTrue(fileNamesDst.contains("test1Dst.txt"));
			assertTrue(fileNamesDst.contains("test2Dst.txt"));
			
			assertTrue(fileNamesSrc.contains("test1Src.txt"));
			assertTrue(fileNamesSrc.contains("test2Src.txt"));
			
			AbstractFile copied1 = childrenDst.stream().filter((fo) -> "test1Src.txt".equals(fo.getName())).findFirst().get();
			assertEquals(file1Src.length(), copied1.length());
			assertArrayEquals(getFileContent(file1Src), getFileContent(copied1));
			
			AbstractFile copied2 = childrenDst.stream().filter((fo) -> "test2Src.txt".equals(fo.getName())).findFirst().get();
			assertEquals(file2Src.length(), copied2.length());
			assertArrayEquals(getFileContent(file2Src), getFileContent(copied2));
			
		} finally {
			cleanUp(file1Src);
			cleanUp(file2Src);
			cleanUp(fileDirectorySrc);
			
			cleanUp(file1Dst);
			cleanUp(file2Dst);
			cleanUp(fileDirectoryDst);
		}
	}
	
	@Test
	public void testCopyFromFileToDirectory() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectorySrc = null;
		AbstractFile file1Src = null;
		
		AbstractFile fileDirectoryDst = null;
		AbstractFile file1Dst = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectorySrc =  getAbstractFile(pathInDirectory(testDirectory,  "copy_from_another_file_test_directory_src"), sshConnection);
			fileDirectorySrc.createFolder();
			assertTrue(fileDirectorySrc.exists());
			
			file1Src =  getAbstractFile(pathInDirectory(fileDirectorySrc, "test1Src.txt") , sshConnection);
			file1Src.createNewFile();
			byte[] dataSrc1 = new byte[] {90};
			addContentToFile(file1Src, dataSrc1);
			
			
			fileDirectoryDst =  getAbstractFile(pathInDirectory(testDirectory,  "copy_from_another_file_test_directory_dst"), sshConnection);
			fileDirectoryDst.createFolder();
			assertTrue(fileDirectoryDst.exists());
			
			file1Dst =  getAbstractFile(pathInDirectory(fileDirectoryDst, "test1Dst.txt") , sshConnection);
			file1Dst.createNewFile();
			byte[] dataDst1 = new byte[] {80};
			addContentToFile(file1Dst, dataDst1);
			
			List<AbstractFile> childrenDst = getListOfFilesRecursive(fileDirectoryDst);
			
			assertEquals(1, childrenDst.size());
			
			fileDirectoryDst.copyFrom(file1Src);
			
			childrenDst = getListOfFilesRecursive(fileDirectoryDst);
			List<AbstractFile> childrenSrc = getListOfFilesRecursive(fileDirectorySrc);
			
			assertEquals(2, childrenDst.size());
			
			assertEquals(1, childrenSrc.size());
			
			Set<String> fileNamesDst = childrenDst.stream().map((fo) ->fo.getName() ).collect(Collectors.toSet());
			Set<String> fileNamesSrc = childrenSrc.stream().map((fo) ->fo.getName() ).collect(Collectors.toSet());
			
			assertTrue(fileNamesDst.contains("test1Src.txt"));
			assertTrue(fileNamesDst.contains("test1Dst.txt"));
			
			assertTrue(fileNamesSrc.contains("test1Src.txt"));
			
			AbstractFile copied = childrenDst.stream().filter((fo) -> "test1Src.txt".equals(fo.getName())).findFirst().get();
			
			assertEquals(file1Src.length(), copied.length());
			
			assertArrayEquals(getFileContent(file1Src), getFileContent(copied));
		} finally {
			cleanUp(file1Src);
			cleanUp(fileDirectorySrc);
			
			cleanUp(file1Dst);
			cleanUp(fileDirectoryDst);
		}
	}
	
	@Test(expected=IOException.class)
	public void testCopyFromDirectoryToFile() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile directorySrc = null;
		AbstractFile fileDst = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "copy_from_file_in_file_from_directory_error"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			directorySrc =  getAbstractFile(pathInDirectory(fileDirectory, "directorySrc") , sshConnection);
			directorySrc.createFolder();
			
			fileDst =  getAbstractFile(pathInDirectory(fileDirectory, "file.txt") , sshConnection);
			fileDst.createNewFile();
			
			fileDst.copyFrom(directorySrc);
			
		} finally {
			cleanUp(directorySrc);
			cleanUp(fileDst);
			cleanUp(fileDirectory);
		}
	}
	
	@Test
	public void testMoveFile() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile fileSrc = null;
		AbstractFile fileDst = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "move_from_file_test_directory"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			fileSrc =  getAbstractFile(pathInDirectory(fileDirectory, "testSrc.txt") , sshConnection);
			fileSrc.createNewFile();
			
			byte[] dataSrc = new byte[] {1,2,3,4,5,6,7,8};
			addContentToFile(fileSrc, dataSrc);
			
			fileDst =  getAbstractFile(pathInDirectory(fileDirectory, "testDst.txt") , sshConnection);
			fileDst.createNewFile();
			
			byte[] dataDst = new byte[] {9,10,11,12,13,14,15,16};
			addContentToFile(fileDst, dataDst);
			
			fileSrc.moveTo(fileDst);
			
			byte[] result = getFileContent(fileDst);
			
			assertFalse(fileSrc.exists());
			assertTrue(fileDst.exists());
			assertArrayEquals(dataSrc, result);
		} finally {
			cleanUp(fileSrc);
			cleanUp(fileDst);
			cleanUp(fileDirectory);
		}
	}
	
	
	@Test
	public void testMoveDirectoryToDirectory() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectorySrc = null;
		AbstractFile file1Src = null;
		AbstractFile file2Src = null;
		
		AbstractFile fileDirectoryDst = null;
		AbstractFile file1Dst = null;
		AbstractFile file2Dst = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectorySrc =  getAbstractFile(pathInDirectory(testDirectory,  "move_from_another_directory_test_directory_src"), sshConnection);
			fileDirectorySrc.createFolder();
			assertTrue(fileDirectorySrc.exists());
			
			file1Src =  getAbstractFile(pathInDirectory(fileDirectorySrc, "test1Src.txt") , sshConnection);
			file1Src.createNewFile();
			byte[] dataSrc1 = new byte[] {1,2,3,4,5,6,7,8};
			addContentToFile(file1Src, dataSrc1);
			
			file2Src =  getAbstractFile(pathInDirectory(fileDirectorySrc, "test2Src.txt") , sshConnection);
			file2Src.createNewFile();
			byte[] dataSrc2 = new byte[] {9,10,11,12,13,14,15,16};
			addContentToFile(file2Src, dataSrc2);
			
			fileDirectoryDst =  getAbstractFile(pathInDirectory(testDirectory,  "move_from_another_directory_test_directory_dst"), sshConnection);
			fileDirectoryDst.createFolder();
			assertTrue(fileDirectoryDst.exists());
			
			file1Dst =  getAbstractFile(pathInDirectory(fileDirectoryDst, "test1Dst.txt") , sshConnection);
			file1Dst.createNewFile();
			byte[] dataDst1 = new byte[] {20};
			addContentToFile(file1Dst, dataDst1);
			
			file2Dst =  getAbstractFile(pathInDirectory(fileDirectoryDst, "test2Dst.txt") , sshConnection);
			file2Dst.createNewFile();
			byte[] dataDst2 = new byte[] {30};
			addContentToFile(file2Dst, dataDst2);
			
			List<AbstractFile> childrenDst = getListOfFilesRecursive(fileDirectoryDst);
			
			assertEquals(2, childrenDst.size());
			
			fileDirectorySrc.moveTo(fileDirectoryDst);
			
			childrenDst = getListOfFilesRecursive(fileDirectoryDst);
			
			assertFalse(fileDirectorySrc.exists());
			assertEquals(5, childrenDst.size()); // including folder entry
			
			Set<String> fileNamesDst = childrenDst.stream().map((fo) ->fo.getName() ).collect(Collectors.toSet());
			
			assertTrue(fileNamesDst.contains("test1Dst.txt"));
			assertTrue(fileNamesDst.contains("test2Dst.txt"));
			assertTrue(fileNamesDst.contains("test1Src.txt"));
			assertTrue(fileNamesDst.contains("test2Src.txt"));
			assertTrue(fileNamesDst.contains("move_from_another_directory_test_directory_src"));
			
			AbstractFile copied1 = childrenDst.stream().filter((fo) -> "test1Src.txt".equals(fo.getName())).findFirst().get();
			assertArrayEquals(dataSrc1, getFileContent(copied1));
			
			AbstractFile copied2 = childrenDst.stream().filter((fo) -> "test2Src.txt".equals(fo.getName())).findFirst().get();
			assertArrayEquals(dataSrc2, getFileContent(copied2));
			
		} finally {
			cleanUp(file1Dst);
			cleanUp(file2Dst);
			cleanUp(fileDirectoryDst);
		}
	}
	
	@Test(expected=IOException.class)
	public void testMoveDirectoryToFile() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile directorySrc = null;
		AbstractFile fileDst = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "move_from_file_in_file_from_directory_error"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			directorySrc =  getAbstractFile(pathInDirectory(fileDirectory, "directorySrc") , sshConnection);
			directorySrc.createFolder();
			
			fileDst =  getAbstractFile(pathInDirectory(fileDirectory, "file.txt") , sshConnection);
			fileDst.createNewFile();
			
			directorySrc.moveTo(fileDst);
			
		} finally {
			cleanUp(directorySrc);
			cleanUp(fileDst);
			cleanUp(fileDirectory);
		}
	}
	
	@Test
	public void testDeleteDirectory() throws PermissionDeniedException, IOException {
		SshConnection sshConnection = getSshConnection();
		AbstractFile testDirectory = getMainTestDirectory();
		
		AbstractFile directory =  getAbstractFile(pathInDirectory(testDirectory, "delete_test_directory"), sshConnection);
		directory.createFolder();
		assertTrue(directory.exists());
		
		directory.delete(true);
		assertFalse(directory.exists());
	}
	
	@Test
	public void testDeleteFile() throws PermissionDeniedException, IOException {
		SshConnection sshConnection = getSshConnection();
		AbstractFile testDirectory = getMainTestDirectory();
		
		AbstractFile file =  getAbstractFile(pathInDirectory(testDirectory, "delete_test_file.txt"), sshConnection);
		file.createNewFile();
		assertTrue(file.exists());
		
		file.delete(true);
		assertFalse(file.exists());
	}
	
	// TODO refresh
	
	@Test
	public void testLastModified() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "file_last_modified_test"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			file =  getAbstractFile(pathInDirectory(fileDirectory, "file.txt") , sshConnection);
			file.createNewFile();
			
			long beforeUpdateLastModified = file.lastModified();
			
			// Needed some delay, else before and after update both values come out similar
			try {
				Thread.sleep(5);
			} catch (Exception e) {
				// ignore
			}
			addContentToFile(file, new byte[] {'A'});
			
			long afterUpdateLastModified = file.lastModified();
			
			assertNotEquals(beforeUpdateLastModified, afterUpdateLastModified);
			
		} finally {
			cleanUp(file);
			cleanUp(fileDirectory);
		}
	}
	
	@Test
	public void testLengthOfFile() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "file_length_test"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			file =  getAbstractFile(pathInDirectory(fileDirectory, "file.txt") , sshConnection);
			file.createNewFile();
			
			addContentToFile(file, new byte[] {'A', 'B', 'C'});
			
			assertEquals(3, file.length());
			
		} finally {
			cleanUp(file);
			cleanUp(fileDirectory);
		}
	}
	
	@Test
	public void testTruncate() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "truncate_file_test"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			file =  getAbstractFile(pathInDirectory(fileDirectory, "file.txt") , sshConnection);
			file.createNewFile();
			
			addContentToFile(file, new byte[] {'A', 'B', 'C', 'D', 'E', 'F'});
			
			assertEquals(6, file.length());
			
			file.truncate();
			
			assertEquals(0, file.length());
			
		} finally {
			cleanUp(file);
			cleanUp(fileDirectory);
		}
	}
	
	@Test
	public void it_should_seek_file_to_a_point_before_file_end_and_write_data_less_than_file_length_in_random_access_mode() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "seek_file_test_seek_after"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			file =  getAbstractFile(pathInDirectory(fileDirectory, "file.txt") , sshConnection);
			file.createNewFile();
			
			addContentToFile(file, new byte[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'});
			
			byte[] data = new byte[] {1,2};
			
			assertEquals(10, file.length());
			
			AbstractFileRandomAccess randomAccess = null;
			try {
				randomAccess = file.openFile(true);
				randomAccess.seek(2);
				randomAccess.write(data, 0, data.length);
			} finally {
				if (randomAccess != null) {
					randomAccess.close();
				}
			}
			assertEquals(10, file.length());
			assertArrayEquals(new byte[] {'A', 'B', 1, 2, 'E', 'F', 'G', 'H', 'I', 'J'}, getFileContent(file));
		} finally {
			cleanUp(file);
			cleanUp(fileDirectory);
		}
	}
	
	@Test
	public void it_should_seek_file_to_a_point_after_file_end_and_write_data_in_random_access_mode() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "seek_file_test_seek_after"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			file =  getAbstractFile(pathInDirectory(fileDirectory, "file.txt") , sshConnection);
			file.createNewFile();
			
			addContentToFile(file, new byte[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'});
			
			byte[] data = new byte[] {1,2,3,4,5,6,7,8,9,10};
			
			assertEquals(10, file.length());
			
			AbstractFileRandomAccess randomAccess = null;
			try {
				randomAccess = file.openFile(true);
				randomAccess.seek(11);
				randomAccess.write(data, 0, data.length);
			} finally {
				if (randomAccess != null) {
					randomAccess.close();
				}
			}
			
			assertEquals(21, file.length());
			assertArrayEquals(new byte[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, getFileContent(file));
		} finally {
			cleanUp(file);
			cleanUp(fileDirectory);
		}
	}
	
	@Test
	public void it_should_seek_file_to_a_point_before_file_end_and_write_data_in_random_access_mode() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "seek_file_test_seek_before"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			file =  getAbstractFile(pathInDirectory(fileDirectory, "file.txt") , sshConnection);
			file.createNewFile();
			
			addContentToFile(file, new byte[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'});
			
			byte[] data = new byte[] {1,2,3,4,5,6,7,8,9,10};
			
			assertEquals(10, file.length());
			
			AbstractFileRandomAccess randomAccess = null;
			try {
				randomAccess = file.openFile(true);
				randomAccess.seek(9);
				randomAccess.write(data, 0, data.length);
			} finally {
				if (randomAccess != null) {
					randomAccess.close();
				}
			}
			
			assertEquals(19, file.length());
			assertArrayEquals(new byte[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, getFileContent(file));
		} finally {
			cleanUp(file);
			cleanUp(fileDirectory);
		}
	}
	
	@Test
	public void it_should_seek_file_to_a_point_to_file_end_and_write_data_in_random_access_mode() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "seek_file_test_seek_equal"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			file =  getAbstractFile(pathInDirectory(fileDirectory, "file.txt") , sshConnection);
			file.createNewFile();
			
			addContentToFile(file, new byte[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'});
			
			byte[] data = new byte[] {1,2,3,4,5,6,7,8,9,10};
			
			assertEquals(10, file.length());
			
			AbstractFileRandomAccess randomAccess = null;
			try {
				randomAccess = file.openFile(true);
				randomAccess.seek(10);
				randomAccess.write(data, 0, data.length);
			} finally {
				if (randomAccess != null) {
					randomAccess.close();
				}
			}
			
			assertEquals(20, file.length());
			assertArrayEquals(new byte[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, getFileContent(file));
		} finally {
			cleanUp(file);
			cleanUp(fileDirectory);
		}
	}
	
	public void it_should_write_data_in_random_access_mode() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "seek_file_test_seek_no"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			file =  getAbstractFile(pathInDirectory(fileDirectory, "file.txt") , sshConnection);
			file.createNewFile();
			
			addContentToFile(file, new byte[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'});
			
			byte[] data = new byte[] {1,2,3,4,5,6,7,8,9,10};
			
			assertEquals(10, file.length());
			
			AbstractFileRandomAccess randomAccess = null;
			try {
				randomAccess = file.openFile(true);
				randomAccess.write(data, 0, data.length);
			} finally {
				if (randomAccess != null) {
					randomAccess.close();
				}
			}
			
			assertEquals(10, file.length());
			assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, getFileContent(file));
			
		} finally {
			cleanUp(file);
			cleanUp(fileDirectory);
		}
	}
	
	@Test(expected=IOException.class)
	public void testWriteInReadOnlyMode() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "file_write_in_read_mode"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			file =  getAbstractFile(pathInDirectory(fileDirectory, "file.txt") , sshConnection);
			file.createNewFile();
			
			addContentToFile(file, new byte[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'});
			
			byte[] data = new byte[] {1,2,3,4,5,6,7,8,9,10};
			
			assertEquals(10, file.length());
			
			AbstractFileRandomAccess randomAccess = null;
			try {
				randomAccess = file.openFile(false);
				randomAccess.write(data, 0, data.length);
			} finally {
				if (randomAccess != null) {
					randomAccess.close();
				}
			}
			
		} finally {
			cleanUp(file);
			cleanUp(fileDirectory);
		}
	}
	
	@Test
	public void it_should_read_data_in_random_access_mode_seek_after_end_of_file() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "read_seek_file_test_seek_no"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			file =  getAbstractFile(pathInDirectory(fileDirectory, "file.txt") , sshConnection);
			file.createNewFile();
			
			addContentToFile(file, new byte[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'});
			
			byte[] data = new byte[] {0};
			
			assertEquals(10, file.length());
			
			AbstractFileRandomAccess randomAccess = null;
			try {
				randomAccess = file.openFile(false);
				randomAccess.seek(11);
				randomAccess.read(data, 0, data.length);
			} finally {
				if (randomAccess != null) {
					randomAccess.close();
				}
			}
			
			assertArrayEquals(new byte[] {0}, data);
			
		} finally {
			cleanUp(file);
			cleanUp(fileDirectory);
		}
	}
	
	@Test
	public void it_should_read_data_in_random_access_mode_seek_before_end_of_file() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "read_seek_file_test_seek_before"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			file =  getAbstractFile(pathInDirectory(fileDirectory, "file.txt") , sshConnection);
			file.createNewFile();
			
			addContentToFile(file, new byte[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'});
			
			byte[] data = new byte[] {0};
			
			assertEquals(10, file.length());
			
			AbstractFileRandomAccess randomAccess = null;
			try {
				randomAccess = file.openFile(false);
				randomAccess.seek(9);
				randomAccess.read(data, 0, data.length);
			} finally {
				if (randomAccess != null) {
					randomAccess.close();
				}
			}
			
			assertArrayEquals(new byte[] {'J'}, data);
			
		} finally {
			cleanUp(file);
			cleanUp(fileDirectory);
		}
	}
		
	@Test
	public void it_should_read_data_in_random_access_mode_no_seek() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "read_seek_file_test_seek_no"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			file =  getAbstractFile(pathInDirectory(fileDirectory, "file.txt") , sshConnection);
			file.createNewFile();
			
			addContentToFile(file, new byte[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'});
			
			byte[] data = new byte[10];
			
			assertEquals(10, file.length());
			
			AbstractFileRandomAccess randomAccess = null;
			try {
				randomAccess = file.openFile(false);
				randomAccess.read(data, 0, data.length);
			} finally {
				if (randomAccess != null) {
					randomAccess.close();
				}
			}
			
			assertArrayEquals(new byte[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'}, data);
			
		} finally {
			cleanUp(file);
			cleanUp(fileDirectory);
		}
	}
	
	@Test
	public void testAppendOutputStream() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile file = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "file_append_test"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			file =  getAbstractFile(pathInDirectory(fileDirectory, "file.txt") , sshConnection);
			file.createNewFile();
			
			addContentToFile(file, new byte[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'});
			
			byte[] data = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
			
			assertEquals(10, file.length());
			
			OutputStream outputStream = null;
			try {
				outputStream = file.getOutputStream(true);
				outputStream.write(data);
			} finally {
				if (outputStream != null) {
					outputStream.close();
				}
			}
			
			assertArrayEquals(new byte[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, getFileContent(file));
			
		} finally {
			cleanUp(file);
			cleanUp(fileDirectory);
		}
	}
	
	@Test
	public void it_should_resolve_specified_non_absolute_path_to_full_path_concatenated_to_parent() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile parentDirectory = null;
		AbstractFile child = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "resolve_path"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			parentDirectory =  getAbstractFile(pathInDirectory(fileDirectory, "parent") , sshConnection);
			
			child = parentDirectory.resolveFile("child");
			
			assertEquals(testDirectory.getAbsolutePath() + getPathSeperator() + "resolve_path" + getPathSeperator() + "parent" + getPathSeperator() + "child", child.getAbsolutePath());
			
		} finally {
			cleanUp(child);
			cleanUp(parentDirectory);
			cleanUp(fileDirectory);
		}
	}
	
	@Test
	public void it_should_resolve_specified_child_absolute_path_back_to_specified_path() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile parentDirectory = null;
		AbstractFile child = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "resolve_path"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			parentDirectory =  getAbstractFile(pathInDirectory(fileDirectory, "parent") , sshConnection);
			
			child = parentDirectory.resolveFile("/child");
			
			assertEquals(getPathSeperator() +  "child", child.getAbsolutePath());
			
		} finally {
			cleanUp(child);
			cleanUp(parentDirectory);
			cleanUp(fileDirectory);
		}
	}
	
	@Test
	public void it_should_resolve_empty_path_to_parent_path() throws PermissionDeniedException, IOException {
		AbstractFile fileDirectory = null;
		AbstractFile parentDirectory = null;
		AbstractFile child = null;
		
		try {
			SshConnection sshConnection = getSshConnection();
			AbstractFile testDirectory = getMainTestDirectory();
			
			fileDirectory =  getAbstractFile(pathInDirectory(testDirectory,  "resolve_path"), sshConnection);
			fileDirectory.createFolder();
			assertTrue(fileDirectory.exists());
			
			parentDirectory =  getAbstractFile(pathInDirectory(fileDirectory, "parent") , sshConnection);
			
			child = parentDirectory.resolveFile("");
			
			assertEquals(testDirectory.getAbsolutePath() + getPathSeperator() + "resolve_path" + getPathSeperator() + "parent" , child.getAbsolutePath());
			
		} finally {
			cleanUp(child);
			cleanUp(parentDirectory);
			cleanUp(fileDirectory);
		}
	}
	
	private String pathInDirectory(AbstractFile directory, String child) throws IOException, PermissionDeniedException {
		return String.format("%s%s%s", directory.getCanonicalPath(), getPathSeperator(), child);
	}
	
	private void cleanUp(AbstractFile abstractFile) throws IOException, PermissionDeniedException {
		if (abstractFile != null) {
			abstractFile.delete(false);
		}
	}
	
	private byte[] getFileContent(AbstractFile abstractFile) throws IOException {
		InputStream inputStream = null; 
		try {
			inputStream = abstractFile.getInputStream();
			byte[] data = new byte[inputStream.available()];
			inputStream.read(data);
			return data;
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
		
	}
	
	private void addContentToFile(AbstractFile abstractFile, byte[] data) throws IOException {
		OutputStream outputStream = null;
		try {
			outputStream = abstractFile.getOutputStream();
			outputStream.write(data);
		} finally {
			if (outputStream != null) {
				outputStream.close();
			}
		}
	}
}
