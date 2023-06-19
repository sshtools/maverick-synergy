package com.sshtools.synergy.niofs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.junit.Test;

import com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder;
import com.sshtools.synergy.niofs.SftpFileAttributeViews.ExtendedSftpFileAttributeView;
import com.sshtools.synergy.niofs.SftpFileAttributeViews.ExtendedSftpFileAttributes;

public class SftpFileAttributeViewsTest extends AbstractNioFsTest {
	public interface FakeBasicFileAttributes extends BasicFileAttributes { }
	
	@Test(expected = NullPointerException.class)
	public void testGetNullAttributeByName() throws Exception {
		testWithFilesystem(fs -> {
			var path = fs.getPath("testfile0");
			Files.createFile(path);
			Files.getAttribute(path, (String) null);
		});
	}
	
	@Test(expected = NullPointerException.class)
	public void testGetNullAttributes() throws Exception {
		testWithFilesystem(fs -> {
			var path = fs.getPath("testfile0");
			Files.createFile(path);
			Files.readAttributes(path, (String) null);
		});
	}

	@Test(expected = NullPointerException.class)
	public void testGetNullAttributesByClass() throws Exception {
		testWithFilesystem(fs -> {
			var path = fs.getPath("testfile0");
			Files.createFile(path);
			Files.readAttributes(path, (Class<BasicFileAttributes>) null);
		});
	}

	@Test
	public void testGetBadAttributesByClass() throws Exception {
		testWithFilesystem(fs -> {
			var path = fs.getPath("testfile0");
			Files.createFile(path);
			assertNull(Files.readAttributes(path, FakeBasicFileAttributes.class));
		});
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testGetBadAttributes() throws Exception {
		testWithFilesystem(fs -> {
			var path = fs.getPath("testfile0");
			Files.createFile(path);
			assertNull(Files.readAttributes(path, "XXXXXXXX:ZZZZZZZZ"));
		});
	}

	@Test(expected = NullPointerException.class)
	public void testGetNullView() throws Exception {
		testWithFilesystem(fs -> {
			var path = fs.getPath("testfile0");
			Files.createFile(path);
			Files.getFileAttributeView(path, null);
		});
	}

	@Test
	public void testGetBadView() throws Exception {
		testWithFilesystem(fs -> {
			var path = fs.getPath("testfile0");
			Files.createFile(path);
			assertNull(Files.getFileAttributeView(path, FileAttributeView.class));
		});
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testGetBadViewAttribute() throws Exception {
		testWithFilesystem(fs -> {
			var path = fs.getPath("testfile0");
			Files.createFile(path);
			Files.getAttribute(path, "xxxxxx:XXXXXXXXXXXXXXXXXXXXXXX");
		});
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testSetBadViewAttribute() throws Exception {
		testWithFilesystem(fs -> {
			var path = fs.getPath("testfile0");
			Files.createFile(path);
			Files.setAttribute(path, "xxxxxx:XXXXXXXXXXXXXXXXXXXXXXX", "Some Val");
		});
	}

	//
	// Basic
	//

	@Test(expected = IOException.class)
	public void testBasicAttributesOnClosed() throws Exception {
		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile1");
			Files.createFile(test);
			var view = Files.getFileAttributeView(test, BasicFileAttributeView.class);
			fs.close();
			view.readAttributes();
		});
	}

	@Test
	public void testBasicAttributesOnFile() throws Exception {
		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile1");
			Files.createFile(test);
			var view = Files.getFileAttributeView(test, BasicFileAttributeView.class);
			assertEquals("basic", view.name());
			var attr = view.readAttributes();
			assertTrue("Should be regular file", attr.isRegularFile());
			assertFalse("Should not be a directory", attr.isDirectory());
			assertFalse("Should not be a link", attr.isSymbolicLink());
			assertFalse("Should not be an other", attr.isOther());
			assertNull(attr.fileKey());
		});
	}

	@Test(expected = NoSuchFileException.class)
	public void testBasicAttributesOnMissingFile() throws Exception {
		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile1");
			var view = Files.getFileAttributeView(test, BasicFileAttributeView.class);
			view.readAttributes();
		});
	}

	@Test
	public void testBasicAttributesOnDirectory() throws Exception {
		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile1");
			Files.createDirectory(test);
			var view = Files.getFileAttributeView(test, BasicFileAttributeView.class);
			assertEquals("basic", view.name());
			var attr = view.readAttributes();
			assertFalse("Should not be regular file", attr.isRegularFile());
			assertTrue("Should be a directory", attr.isDirectory());
			assertFalse("Should not be a synbolic link", attr.isSymbolicLink());
			assertFalse("Should not be an other", attr.isOther());
		});
	}

	@Test
	public void testBasicAttributesSymlink() throws Exception {
		testWithFilesystem(fs -> {
			var test0 = fs.getPath("testfile0");
			Files.createFile(test0);
			var test = fs.getPath("testfile1");
			Files.createSymbolicLink(test, test0);
			var view = Files.getFileAttributeView(test, BasicFileAttributeView.class);
			assertEquals("basic", view.name());
			var attr = view.readAttributes();
			assertFalse("Should not be regular file", attr.isRegularFile());
			assertFalse("Should not be a directory", attr.isDirectory());
			assertTrue("Should be a synbolic link", attr.isSymbolicLink());
			assertFalse("Should not be an other", attr.isOther());
		});
	}

	@Test
	public void testBasicAttributesHardlink() throws Exception {
		testWithFilesystem(fs -> {
			var test0 = fs.getPath("testfile0");
			Files.createFile(test0);
			var test = fs.getPath("testfile1");
			Files.createLink(test, test0);
			var view = Files.getFileAttributeView(test, BasicFileAttributeView.class);
			assertEquals("basic", view.name());
			var attr = view.readAttributes();
			assertTrue("Should be regular file", attr.isRegularFile());
			assertFalse("Should not be a directory", attr.isDirectory());
			assertFalse("Should not be a synbolic link", attr.isSymbolicLink());
			assertFalse("Should not be an other", attr.isOther());
		});
	}

	@Test(expected = IOException.class)
	public void testBasicAttributesSetTimesWhenClose() throws Exception {
		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile0");
			var view = Files.getFileAttributeView(test, BasicFileAttributeView.class);
			fs.close();
			view.setTimes(FileTime.from(Instant.now()), FileTime.from(Instant.now()), FileTime.from(Instant.now()));
		});
	}

	@Test
	public void testBasicAttributesSetTimesDifferentNullsForCoverage() throws Exception {
		testWithFilesystem(fs -> {
			var path = fs.getPath("testfile0");
			Files.createFile(path);
			var view = Files.getFileAttributeView(path, BasicFileAttributeView.class);
			view.setTimes(FileTime.from(Instant.now()), null, FileTime.from(Instant.now()));
			view.setTimes(FileTime.from(Instant.now()), FileTime.from(Instant.now()), null);
			view.setTimes(null, FileTime.from(Instant.now()), FileTime.from(Instant.now()));
		});
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testBasicAttributesSetBadAttribute() throws Exception {
		testWithFilesystem(fs -> {
			var path = fs.getPath("testfile0");
			Files.createFile(path);
			Files.setAttribute(path, "basic:XXXXXXXXXXXXXXXXXXXXXXX", "Some Val");
		});
	}

	@Test
	public void testBasicAttributesSetAttributeWithoutPrefix() throws Exception {
		var now = FileTime.from(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		testWithFilesystem(fs -> {
			var path = fs.getPath("testfile0");
			Files.createFile(path);
			Files.setAttribute(path, "lastModifiedTime", now);
			assertEquals(now, Files.getAttribute(path, "lastModifiedTime"));
		});
	}

	@Test
	public void testBasicAttributesSetTimes() throws Exception {
		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile0");
			var now = Calendar.getInstance();
			Files.createFile(test);
			var view = Files.getFileAttributeView(test, BasicFileAttributeView.class);

			now.add(Calendar.DAY_OF_YEAR, -1);
			now.set(Calendar.MILLISECOND, 0);

			now.setTimeInMillis(0);
			now.add(Calendar.SECOND, 5);
			var created = now.toInstant();

			now.add(Calendar.SECOND, 10);
			var modified = now.toInstant();

			now.add(Calendar.SECOND, 70);
			var accessed = now.toInstant();

			/*
			 * TODO: Creation time doesn't seem to work (on Linux host at least). It is
			 * being passed correctly all the way through to NioFile using Files.setTimes(),
			 * but the underyling file does not get a creation time. This may be a security
			 * issue. The time can created OK
			 */
			view.setTimes(FileTime.from(modified), FileTime.from(accessed), FileTime.from(created));

			var viewAttr = view.readAttributes();
			assertEquals(FileTime.from(accessed), viewAttr.lastAccessTime());
			assertEquals(FileTime.from(modified), Files.getLastModifiedTime(test));

			/* See above */
			assertTrue("Creation time must be close to call of createFile()",
					viewAttr.creationTime().toMillis() - now.getTimeInMillis() < 2000);
		});
	}

	@Test
	public void testBasicAttributesSetTimesUsingAttributes() throws Exception {
		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile0");
			var now = Calendar.getInstance();
			Files.createFile(test);

			now.add(Calendar.DAY_OF_YEAR, -1);
			now.set(Calendar.MILLISECOND, 0);

			now.setTimeInMillis(0);
			now.add(Calendar.SECOND, 5);
			var created = now.toInstant();

			now.add(Calendar.SECOND, 10);
			var modified = now.toInstant();

			now.add(Calendar.SECOND, 70);
			var accessed = now.toInstant();

			/*
			 * Creation time doesn't seem to work (on Linux host at least). It is being
			 * passed correctly all the way through to NioFile using Files.setTimes(), but
			 * the underyling file does not get a creation time. This may be a security
			 * issue. The time can created OK
			 */
			Files.setAttribute(test, "basic:lastModifiedTime", FileTime.from(modified));
			Files.setAttribute(test, "basic:lastAccessTime", FileTime.from(accessed));
			Files.setAttribute(test, "basic:creationTime", FileTime.from(created));

			assertEquals(FileTime.from(accessed), Files.getAttribute(test, "basic:lastAccessTime"));
			assertEquals(FileTime.from(modified), Files.getAttribute(test, "basic:lastModifiedTime"));

			/* See above */
			assertTrue("Creation time must be close to call of createFile()",
					((FileTime) Files.getAttribute(test, "basic:creationTime")).toMillis()
							- now.getTimeInMillis() < 2000);
		});
	}

	@Test
	public void testBasicAttributes() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var attr = Files.readAttributes(src, BasicFileAttributes.class);
			assertTrue(attr.isRegularFile());
		});
	}

	@Test
	public void testBasicAttributesOfBrokenSymbolicLink() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			Files.createFile(src);
			var link = fs.getPath("testlink");
			Files.createSymbolicLink(link, src);
			Files.delete(src);
			var attr = Files.readAttributes(link, BasicFileAttributes.class);
			assertTrue("Should be a symbolic link", attr.isSymbolicLink());
			assertFalse("Should not be a regular file", attr.isRegularFile());
		});
	}

	@Test(expected = NoSuchFileException.class)
	public void testBasicAttributesOfMissingFile() throws Exception {
		testWithFilesystem(fs -> {
			Files.readAttributes(fs.getPath("testfile"), BasicFileAttributes.class);
		});
	}

	@Test(expected = IOException.class)
	public void testBasicAttributesWhenClosed() throws Exception {
		testWithFilesystem(fs -> {
			fs.close();
			Files.readAttributes(fs.getPath("testfile"), BasicFileAttributes.class);
		});
	}

	@Test
	public void testBasicAttributesMap() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var attr = Files.readAttributes(src, "*");
			assertNull(attr.get("fileKey"));
			assertNotNull(attr.get("creationTime"));
			assertNotNull(attr.get("lastAccessTime"));
			assertNotNull(attr.get("lastModifiedTime"));
			assertFalse((Boolean) attr.get("isDirectory"));
			assertFalse((Boolean) attr.get("isOther"));
			assertTrue((Boolean) attr.get("isRegularFile"));
			assertFalse((Boolean) attr.get("isSymbolicLink"));
			assertEquals(Long.valueOf(Files.size(src)), (Long) attr.get("size"));
		});
	}

	@Test
	public void testBasicAttributesMapIndividual() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var attr = Files.readAttributes(src, "ZZZZZZZ,fileKey,lastAccessTime,isDirectory,size");
			assertNull(attr.get("fileKey"));
			assertNull(attr.get("creationTime"));
			assertNotNull(attr.get("lastAccessTime"));
			assertNull(attr.get("lastModifiedTime"));
			assertFalse((Boolean) attr.get("isDirectory"));
			assertNull((Boolean) attr.get("isOther"));
			assertNull((Boolean) attr.get("isRegularFile"));
			assertNull((Boolean) attr.get("isSymbolicLink"));
			assertEquals(Long.valueOf(Files.size(src)), (Long) attr.get("size"));
		});
	}

	@Test
	public void testBasicSetAttribute() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			Files.setAttribute(src, "basic:lastModifiedTime", FileTime.fromMillis(0));
			assertEquals(0, Files.getLastModifiedTime(src).toMillis());
		});
	}

	//
	// POSIX
	//

	@Test(expected = IOException.class)
	public void testPosixAttributesOnClosed() throws Exception {
		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile1");
			Files.createFile(test);
			var view = Files.getFileAttributeView(test, PosixFileAttributeView.class);
			fs.close();
			view.readAttributes();
		});
	}

	@Test
	public void testPosixAttributesOnFile() throws Exception {

		var cfile = Files.createTempFile("testxxx", ".test");
		var oview = Files.getFileAttributeView(cfile, PosixFileAttributeView.class);
		var oowner = oview.getOwner();
		var oattr = oview.readAttributes();
		var ogroup = oattr.group();

		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile1");
			Files.createFile(test);
			var view = Files.getFileAttributeView(test, PosixFileAttributeView.class);
			assertEquals("posix", view.name());
			var attr = view.readAttributes();
			assertEquals(oowner.getName(), view.getOwner().getName());
			assertEquals(oowner.getName(), attr.owner().getName());
			assertEquals(ogroup.getName(), attr.group().getName());
			assertEquals(new LinkedHashSet<>(Arrays.asList(PosixFilePermission.OWNER_READ,
					PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE,
					PosixFilePermission.OTHERS_READ)), attr.permissions());
		});
	}

	@Test(expected = NoSuchFileException.class)
	public void testPosixAttributesOnMissingFile() throws Exception {
		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile1");
			var view = Files.getFileAttributeView(test, PosixFileAttributeView.class);
			view.readAttributes();
		});
	}

	@Test
	public void testPosixAttributesOnDirectory() throws Exception {
		var cfile = Files.createTempDirectory("testxxx");
		var oview = Files.getFileAttributeView(cfile, PosixFileAttributeView.class);
		var oowner = oview.getOwner();
		var oattr = oview.readAttributes();
		var ogroup = oattr.group();

		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile1");
			Files.createDirectory(test);
			var view = Files.getFileAttributeView(test, PosixFileAttributeView.class);
			assertEquals("posix", view.name());
			var attr = view.readAttributes();
			assertEquals(oowner.getName(), view.getOwner().getName());
			assertEquals(oowner.getName(), attr.owner().getName());
			assertEquals(ogroup.getName(), attr.group().getName());
			assertEquals(new LinkedHashSet<>(Arrays.asList(PosixFilePermission.OWNER_READ,
					PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ,
					PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ,
					PosixFilePermission.OTHERS_EXECUTE)), attr.permissions());
		});
	}

	@Test
	public void testPosixAttributesOwnershipUsingAttributes() throws Exception {

		var cfile = Files.createTempFile("test", ".test");
		var oview = Files.getFileAttributeView(cfile, PosixFileAttributeView.class);
		var oowner = oview.getOwner();
		var oattr = oview.readAttributes();
		var ogroup = oattr.group();
		var opermissions = oattr.permissions();

		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile0");
			Files.createFile(test);

			/*
			 * All we can really do (and get out-of-the-box working tests) is set the owner
			 * to the local owner, as we know it exists.
			 */
			Files.setAttribute(test, "posix:owner", oowner);
			Files.setAttribute(test, "posix:group", ogroup);
			Files.setAttribute(test, "posix:permissions", PosixPermissionsBuilder.create().fromPermissions(opermissions)
					.withoutWritePermissions().build().asPermissions());

			assertEquals(oowner.getName(), ((UserPrincipal) Files.getAttribute(test, "posix:owner")).getName());
			assertEquals(ogroup.getName(), ((UserPrincipal) Files.getAttribute(test, "posix:group")).getName());

			var withoutWrite = new LinkedHashSet<>(opermissions);
			withoutWrite.removeAll(Arrays.asList(PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_WRITE,
					PosixFilePermission.OTHERS_WRITE));
			assertEquals(withoutWrite, Files.getAttribute(test, "posix:permissions"));
		});
	}

	@Test(expected = IOException.class)
	public void testPosixAttributesOwnerWhenClosed() throws Exception {
		var cfile = Files.createTempFile("testxxx", ".test");
		var oview = Files.getFileAttributeView(cfile, PosixFileAttributeView.class);
		var oowner = oview.getOwner();
		testWithFilesystem(fs -> {
			fs.close();
			Files.setAttribute(fs.getPath("testfile"), "posix:owner", oowner);
		});
	}

	@Test(expected = IOException.class)
	public void testPosixAttributesGroupWhenClosed() throws Exception {
		var cfile = Files.createTempFile("testxxx", ".test");
		var oview = Files.getFileAttributeView(cfile, PosixFileAttributeView.class);
		var ogroup = oview.readAttributes().group();
		testWithFilesystem(fs -> {
			fs.close();
			Files.setAttribute(fs.getPath("testfile"), "posix:group", ogroup);
		});
	}

	@Test(expected = IOException.class)
	public void testPosixAttributesPermissionsWhenClosed() throws Exception {
		testWithFilesystem(fs -> {
			fs.close();
			Files.setAttribute(fs.getPath("testfile"), "posix:permissions", Collections.emptySet());
		});
	}

	@Test
	public void testPosixAttributesMap() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var attr = Files.readAttributes(src, "posix:*");
			assertNotNull(attr.get("owner"));
			assertNotNull(attr.get("group"));
			assertNotNull(attr.get("permissions"));
		});
	}

	@Test
	public void testPosixAttributesMapIndividual() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var attr = Files.readAttributes(src, "posix:ZZZZZZZ,owner,group,permissions");
			assertNotNull(attr.get("owner"));
			assertNotNull(attr.get("group"));
			assertNotNull(attr.get("permissions"));
		});
	}

	@Test(expected = NoSuchFileException.class)
	public void testPosixAttributesOfMissingFile() throws Exception {
		testWithFilesystem(fs -> {
			Files.readAttributes(fs.getPath("testfile"), PosixFileAttributes.class);
		});
	}

	@Test(expected = IOException.class)
	public void testPosixAttributesWhenClosed() throws Exception {
		testWithFilesystem(fs -> {
			fs.close();
			Files.readAttributes(fs.getPath("testfile"), PosixFileAttributes.class);
		});
	}

	@Test
	public void testPosixAttributesOfBrokenSymbolicLink() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			Files.createFile(src);
			var link = fs.getPath("testlink");
			Files.createSymbolicLink(link, src);
			Files.delete(src);
			var attr = Files.readAttributes(link, PosixFileAttributes.class);
			assertTrue("Should be a symbolic link", attr.isSymbolicLink());
			assertFalse("Should not be a regular file", attr.isRegularFile());
		});
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testPosixAttributesSetBadAttribute() throws Exception {
		testWithFilesystem(fs -> {
			var path = fs.getPath("testfile0");
			Files.createFile(path);
			Files.setAttribute(path, "posix:XXXXXXXXXXXXXXXXXXXXXXX", "Some Val");
		});
	}

	@Test
	public void testPosixAttributesSetBasicAttribute() throws Exception {
		testWithFilesystem(fs -> {
			var path = fs.getPath("testfile0");
			Files.createFile(path);
			var ft = FileTime.from(Instant.now());
			Files.setAttribute(path, "posix:lastModifiedTime", ft);
			ft = FileTime.from(ft.toInstant().truncatedTo(ChronoUnit.SECONDS));
			assertEquals(ft, Files.getAttribute(path, "posix:lastModifiedTime"));
		});
	}


	//
	// Extended
	//

	@Test(expected = IOException.class)
	public void testExtendedAttributesOnClosed() throws Exception {
		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile1");
			Files.createFile(test);
			var view = Files.getFileAttributeView(test, ExtendedSftpFileAttributeView.class);
			fs.close();
			view.readAttributes();
		});
	}

	@Test
	public void testExtendedAttributesOnFile() throws Exception {

		var cfile = Files.createTempFile("testxxx", ".test");
		var oview = Files.getFileAttributeView(cfile, PosixFileAttributeView.class);
		var oowner = oview.getOwner();
		var oattr = oview.readAttributes();
		var ogroup = oattr.group();

		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile1");
			Files.createFile(test);
			var view = Files.getFileAttributeView(test, ExtendedSftpFileAttributeView.class);
			assertEquals("sftp", view.name());
			var attr = view.readAttributes();
			assertEquals(oowner.getName(), attr.uid());
			assertEquals(ogroup.getName(), attr.gid());
			assertEquals(0, attr.linkCount());
			assertEquals("application/octet-stream", attr.mimeType());
			assertEquals(1, attr.type());
			assertEquals( 
					PosixPermissionsBuilder.create().fromPermissions(PosixFilePermission.OWNER_READ,
					PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE,
					PosixFilePermission.OTHERS_READ).build(), attr.permissions());
		});
	}

	@Test(expected = NoSuchFileException.class)
	public void testExtendedAttributesOnMissingFile() throws Exception {
		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile1");
			var view = Files.getFileAttributeView(test, ExtendedSftpFileAttributeView.class);
			view.readAttributes();
		});
	}

	@Test
	public void testExtendedAttributesOnDirectory() throws Exception {
		var cfile = Files.createTempDirectory("testxxx");
		var oview = Files.getFileAttributeView(cfile, PosixFileAttributeView.class);
		var oowner = oview.getOwner();
		var oattr = oview.readAttributes();
		var ogroup = oattr.group();

		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile1");
			Files.createDirectory(test);
			var view = Files.getFileAttributeView(test, ExtendedSftpFileAttributeView.class);
			assertEquals("sftp", view.name());
			var attr = view.readAttributes();
			assertEquals(oowner.getName(), attr.uid());
			assertEquals(ogroup.getName(), attr.gid());
			assertEquals(0, attr.linkCount());
			assertEquals("application/octet-stream", attr.mimeType());
			assertEquals(2, attr.type());
			assertEquals(PosixPermissionsBuilder.create().fromPermissions(PosixFilePermission.OWNER_READ,
					PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ,
					PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ,
					PosixFilePermission.OTHERS_EXECUTE).build(), attr.permissions());
		});
	}

	@Test
	public void testExtendedAttributesOwnershipUsingAttributes() throws Exception {

		var cfile = Files.createTempFile("test", ".test");
		var oview = Files.getFileAttributeView(cfile, PosixFileAttributeView.class);
		var oowner = oview.getOwner();
		var oattr = oview.readAttributes();
		var ogroup = oattr.group();
		var opermissions = oattr.permissions();

		testWithFilesystem(fs -> {
			var test = fs.getPath("testfile0");
			Files.createFile(test);

			/*
			 * All we can really do (and get out-of-the-box working tests) is set the owner
			 * to the local owner, as we know it exists.
			 */
			Files.setAttribute(test, "sftp:uid", oowner.getName());
			Files.setAttribute(test, "sftp:gid", ogroup.getName());
			Files.setAttribute(test, "sftp:permissions", PosixPermissionsBuilder.create().fromPermissions(opermissions)
					.withoutWritePermissions().build());

			assertEquals(oowner.getName(), Files.getAttribute(test, "sftp:uid"));
			assertEquals(ogroup.getName(), Files.getAttribute(test, "sftp:gid"));

			var withoutWrite = new LinkedHashSet<>(opermissions);
			withoutWrite.removeAll(Arrays.asList(PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_WRITE,
					PosixFilePermission.OTHERS_WRITE));
			assertEquals(PosixPermissionsBuilder.create().withPermissions(withoutWrite).build(), Files.getAttribute(test, "sftp:permissions"));
		});
	}

	@Test(expected = IOException.class)
	public void testExtendedAttributesOwnerWhenClosed() throws Exception {
		var cfile = Files.createTempFile("testxxx", ".test");
		var oview = Files.getFileAttributeView(cfile, PosixFileAttributeView.class);
		var oowner = oview.getOwner();
		testWithFilesystem(fs -> {
			fs.close();
			Files.setAttribute(fs.getPath("testfile"), "sftp:uid", oowner.getName());
		});
	}

	@Test(expected = IOException.class)
	public void testExtendedAttributesGroupWhenClosed() throws Exception {
		var cfile = Files.createTempFile("testxxx", ".test");
		var oview = Files.getFileAttributeView(cfile, PosixFileAttributeView.class);
		var ogroup = oview.readAttributes().group();
		testWithFilesystem(fs -> {
			fs.close();
			Files.setAttribute(fs.getPath("testfile"), "sftp:gid", ogroup.getName());
		});
	}

	@Test(expected = IOException.class)
	public void testExtendedAttributesPermissionsWhenClosed() throws Exception {
		testWithFilesystem(fs -> {
			fs.close();
			Files.setAttribute(fs.getPath("testfile"), "sftp:uid", Collections.emptySet());
		});
	}

	@Test
	public void testExtendedAttributesMap() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var attr = Files.readAttributes(src, "sftp:*");
			assertNotNull(attr.get("uid"));
			assertNotNull(attr.get("gid"));
			assertNotNull(attr.get("permissions"));
		});
	}

	@Test
	public void testExtendedAttributesMapIndividual() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var attr = Files.readAttributes(src, "sftp:ZZZZZZZ,uid,gid,permissions");
			assertNotNull(attr.get("uid"));
			assertNotNull(attr.get("gid"));
			assertNotNull(attr.get("permissions"));
		});
	}

	@Test(expected = NoSuchFileException.class)
	public void testExtendedAttributesOfMissingFile() throws Exception {
		testWithFilesystem(fs -> {
			Files.readAttributes(fs.getPath("testfile"), ExtendedSftpFileAttributes.class);
		});
	}

	@Test(expected = IOException.class)
	public void testExtendedAttributesWhenClosed() throws Exception {
		testWithFilesystem(fs -> {
			fs.close();
			Files.readAttributes(fs.getPath("testfile"), ExtendedSftpFileAttributes.class);
		});
	}

	@Test
	public void testExtendedAttributesOfBrokenSymbolicLink() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			Files.createFile(src);
			var link = fs.getPath("testlink");
			Files.createSymbolicLink(link, src);
			Files.delete(src);
			var attr = Files.readAttributes(link, ExtendedSftpFileAttributes.class);
			assertTrue("Should be a symbolic link", attr.isSymbolicLink());
			assertFalse("Should not be a regular file", attr.isRegularFile());
		});
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testExtendedAttributesSetBadAttribute() throws Exception {
		testWithFilesystem(fs -> {
			var path = fs.getPath("testfile0");
			Files.createFile(path);
			Files.setAttribute(path, "sftp:XXXXXXXXXXXXXXXXXXXXXXX", "Some Val");
		});
	}

	@Test
	public void testExtendedAttributesSetBasicAttribute() throws Exception {
		testWithFilesystem(fs -> {
			var path = fs.getPath("testfile0");
			Files.createFile(path);
			var ft = FileTime.from(Instant.now().truncatedTo(ChronoUnit.SECONDS));
			Files.setAttribute(path, "sftp:lastModifiedTime", ft);
			assertEquals(ft, Files.getAttribute(path, "posix:lastModifiedTime"));
		});
	}
}
