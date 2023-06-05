package com.sshtools.synergy.niofs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.util.NoSuchElementException;

import org.junit.Test;

public class SftpDirectoryStreamTest extends AbstractNioFsTest {

	@Test
	public void testIterate() throws Exception {

		testWithFilesystem(fs -> {
			for(int i = 0 ; i < 100 ; i++) {
				Files.createFile(fs.getPath("testfile" + i + ".jar"));
			}
			var found = 0;
			try(var str = Files.newDirectoryStream(fs.getPath("."))) {
				for(var f : str) {
					if(f.getFileName().toString().startsWith("testfile"))
						found++;
				}
			}
			assertEquals("Should be 100 .txt files", 100, found);
		});
	}
	@Test(expected = ClosedDirectoryStreamException.class)
	public void testFailIterateWhenClosed() throws Exception {

		testWithFilesystem(fs -> {
			for(int i = 0 ; i < 100 ; i++) {
				Files.createFile(fs.getPath("testfile" + i + ".jar"));
			}
			try(var str = Files.newDirectoryStream(fs.getPath("."))) {
				str.close();
				str.iterator();
			}
		});
	}
	
	@Test(expected = UncheckedIOException.class)
	public void testIOExceptionOnFilteredIterate() throws Exception {

		testWithFilesystem(fs -> {
			for(int i = 0 ; i < 100 ; i++) {
				Files.createFile(fs.getPath("testfile" + i + ".jar"));
			}
			try(var str = Files.newDirectoryStream(fs.getPath("."), (f) -> {
				throw new IOException("Bang!");
			} )) {
				str.iterator().next();
			}
		});
	}
	@Test(expected = NoSuchElementException.class)
	public void testFailEmptyIterate() throws Exception {

		testWithFilesystem(fs -> {
			try(var str = Files.newDirectoryStream(fs.getPath("."))) {
				str.iterator().next();
			}
		});
	}
	@Test(expected = UncheckedIOException.class)
	public void testFailIterateMissingAfterStream() throws Exception {

		testWithFilesystem(fs -> {
			var dir = fs.getPath("dir");
			Files.createDirectories(dir);
			try(var str = Files.newDirectoryStream(dir)) {
				Files.delete(dir);
				str.iterator().next();
			}
		});
	}
	@Test(expected = NoSuchFileException.class)
	public void testFailIterateMissing() throws Exception {

		testWithFilesystem(fs -> {
			try(var str = Files.newDirectoryStream(fs.getPath("dir"))) {
			}
		});
	}
	
	@Test(expected = IllegalStateException.class)
	public void testFailIterateTwice() throws Exception {

		testWithFilesystem(fs -> {
			for(int i = 0 ; i < 100 ; i++) {
				Files.createFile(fs.getPath("testfile" + i + ".jar"));
			}
			try(var str = Files.newDirectoryStream(fs.getPath("."))) {
				str.iterator();
				str.iterator();
			}
		});
	}
	
	@Test(expected = NotDirectoryException.class)
	public void testFailIterateFile() throws Exception {
		testWithFilesystem(fs -> {
			var f = fs.getPath("testfile1.jar");
			Files.createFile(f);
			try(var str = Files.newDirectoryStream(f)) {
			}
		});
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void testFailRemove() throws Exception {
		testWithFilesystem(fs -> {
			for(int i = 0 ; i < 100 ; i++) {
				Files.createFile(fs.getPath("testfile" + i + ".jar"));
			}
			try(var str = Files.newDirectoryStream(fs.getPath("."))) {
				var it = str.iterator();
				it.next();
				it.remove();
			}
		});
	}
	
	@Test(expected = UncheckedIOException.class)
	public void testFailClosedClient() throws Exception {
		testWithFilesystem(fs -> {
			for(int i = 0 ; i < 100 ; i++) {
				Files.createFile(fs.getPath("testfile" + i + ".jar"));
			}
			try(var str = Files.newDirectoryStream(fs.getPath("."))) {
				fs.close();
				str.iterator();
			}
		});
	}
	
	@Test
	public void testClosedStreamReturnsNotHasNext() throws Exception {
		testWithFilesystem(fs -> {
			for(int i = 0 ; i < 100 ; i++) {
				Files.createFile(fs.getPath("testfile" + i + ".jar"));
			}
			try(var str = Files.newDirectoryStream(fs.getPath("."))) {
				var it = str.iterator();
				str.close();
				assertFalse("Iterator should return false for a closed stream.", it.hasNext());
			}
		});
	}
	
	@Test(expected = NoSuchElementException.class)
	public void testFailNoSuchElementOnClosedStreamNext() throws Exception {
		testWithFilesystem(fs -> {
			for(int i = 0 ; i < 100 ; i++) {
				Files.createFile(fs.getPath("testfile" + i + ".jar"));
			}
			try(var str = Files.newDirectoryStream(fs.getPath("."))) {
				var it = str.iterator();
				str.close();
				it.next();
			}
		});
	}
	
	
}
