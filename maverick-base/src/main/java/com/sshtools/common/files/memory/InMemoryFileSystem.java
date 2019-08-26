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
package com.sshtools.common.files.memory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InMemoryFileSystem {
	
	public static final String PATH_SEPARATOR = "/";

	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
	private final Lock readLock = readWriteLock.readLock();
	private final Lock writeLock = readWriteLock.writeLock();
	
	private final Map<String, InMemoryFile> fileSystem = new HashMap<>();
	private final InMemoryFile root = new InMemoryFile(null, this, "root", true);
	
	private final Map<String, Long> fileLocks = Collections.synchronizedMap(new WeakHashMap<>());
	
	{
		fileSystem.put(PATH_SEPARATOR, root);
	}
	
	public InMemoryFile createFolder(InMemoryFile parent, String name) throws IOException {
		InMemoryFile parentRef = parent == null  ? root : parent;
		return createFileObject(parentRef, name, true);
	}

	public InMemoryFile createFile(InMemoryFile parent, String name) throws IOException {
		InMemoryFile parentRef = parent == null  ? root : parent;
		if (parentRef.isFile()) {
			String parentPath = parentRef.getPath();
			throw new IOException(String.format("Parent at path %s is not a folder instance.", parentPath));
		}
		
		return createFileObject(parentRef, name, false);
	}
	
	public void delete(InMemoryFile fileObject) throws IOException {
		String path = fileObject.getPath();
		try {
			acquireLock(path);
			if (fileObject.isFile()) {
				write(() -> {
					InMemoryFileSystem.this.fileSystem.remove(path);
					return null;
				});
			} else {
				write(() -> {
					List<Entry<String, InMemoryFile>> clone = new ArrayList<>(InMemoryFileSystem.this.fileSystem.entrySet());
					
					for (Entry<String, InMemoryFile> entry : clone) {
						if (entry.getKey().startsWith(path)) {
							InMemoryFileSystem.this.fileSystem.remove(path);
						}
					}
					
					return null;
				});
			}
		} finally {
			releaseLock(path);
		}
	}
	
	public void copyFrom(InMemoryFile fileObjectSrc, InMemoryFile fileObjectDest) throws IOException {
		if (fileObjectDest.isFile() && fileObjectSrc.isFolder()) {
			throw new IOException("Folder cannot be moved into a file.");
		}
		
		String destPath = fileObjectDest.getPath();
		String srcPath = fileObjectSrc.getPath();
		
		try {
			acquireLock(destPath);
			acquireLock(srcPath);
		
			if (fileObjectDest.isFile() && fileObjectSrc.isFile()) {
				write(() -> {
					fileObjectDest.setData(Arrays.copyOf(fileObjectSrc.getData(), fileObjectSrc.getData().length));
					return null;
				});
			} else if (fileObjectDest.isFolder() && fileObjectSrc.isFolder()){
				write(() -> {
					fileObjectSrc.getChildren().stream().filter((fo) -> fo.isFile()).forEach((fo) -> {
						try {
							String childPath = fo.getPath();
							String newPath = childPath.replaceAll(fileObjectSrc.getPath(), fileObjectDest.getPath());
							InMemoryFile fileObject = createFileWithParents(newPath);
							
							fileObject.setData(Arrays.copyOf(fo.getData(), fo.getData().length));
						} catch (Exception e) {
							throw new RuntimeException(e.getMessage(), e);
						}
					});
					return null;
				});
			} else if (fileObjectDest.isFolder() && fileObjectSrc.isFile()) {
				write(() -> {
					String childPath = fileObjectSrc.getPath();
					String childPathParent = computeParentPath(childPath);
					String newPath = childPath.replaceAll(childPathParent, fileObjectDest.getPath());
					InMemoryFile fileObject = createFileWithParents(newPath);
					
					fileObject.setData(Arrays.copyOf(fileObjectSrc.getData(), fileObjectSrc.getData().length));
					return null;
				});
			}
			
		} finally {
			releaseLock(destPath);
			releaseLock(srcPath);
		}
	}

	public void moveTo(InMemoryFile fileObjectDest, InMemoryFile fileObjectSrc) throws IOException {
		if (fileObjectDest.isFile() && fileObjectSrc.isFolder()) {
			throw new IOException("Folder cannot be moved into a file.");
		}
		String destPath = fileObjectDest.getPath();
		String srcPath = fileObjectSrc.getPath();
		try {
			acquireLock(destPath);
			acquireLock(srcPath);
			if (fileObjectDest.isFile() && fileObjectSrc.isFile()) {
				write(() -> {
					fileObjectDest.setData(fileObjectSrc.getData());
					fileObjectSrc.delete();
					return null;
				});
			} else if (fileObjectDest.isFolder() && fileObjectSrc.isFolder()){
				write(() -> {
					InMemoryFile newMovedFolder = fileObjectDest.createFolder(fileObjectSrc.getName());
					
					fileObjectSrc.getChildren().forEach((fo) -> {
						String childPath = fo.getPath();
						InMemoryFileSystem.this.fileSystem.remove(childPath);
						if (fo.getParent().equals(fileObjectSrc)) {
							fo.setParent(newMovedFolder);
						}
						String newChildPath = childPath.replaceAll(fileObjectSrc.getPath(), newMovedFolder.getPath());
						InMemoryFileSystem.this.fileSystem.put(newChildPath, fo);
					});
					
					// delete source
					fileObjectSrc.delete();
					return null;
				});
			} else if (fileObjectDest.isFolder() && fileObjectSrc.isFile()) {
				write(() -> {
					InMemoryFile newFile = InMemoryFileSystem.this.createFile(fileObjectDest, fileObjectSrc.getName());
					newFile.setData(fileObjectSrc.getData());
					fileObjectSrc.delete();
					return null;
				});
			}
		} finally {
			releaseLock(destPath);
			releaseLock(srcPath);
		}
		
	}
	
	public void rename(InMemoryFile fileObject, String name) throws IOException {
		String path = fileObject.getPath();
		try {
			acquireLock(path);
			if (fileObject.isFile()) {
				write(() -> {
					renameFileObjectEntry(fileObject, name);
					return null;
				});
			} else {
				write(() -> {
					String oldPath = fileObject.getPath();
					
					renameFileObjectEntry(fileObject, name);
					
					List<Entry<String, InMemoryFile>> clone = new ArrayList<>(InMemoryFileSystem.this.fileSystem.entrySet());
					
					for (Entry<String, InMemoryFile> entry : clone) {
						if (entry.getKey().startsWith(oldPath)) {
							InMemoryFileSystem.this.fileSystem.remove(oldPath);
							InMemoryFileSystem.this.fileSystem.put(entry.getValue().getPath(), entry.getValue());
						}
					}
					
					return null;
				});
			}
		} finally {
			releaseLock(path);
		}
	}

	public List<InMemoryFile> getChildren(InMemoryFile fileObject) throws IOException {
		if (fileObject.isFile()) {
			throw new IOException("File instance does not have any children..");
		}
		return read(() ->  {
			return fileObject.getChildren();
		});
	}
	
	public boolean exists(String path) throws IOException {
		return read(() -> {
			return InMemoryFileSystem.this.fileSystem.containsKey(path);
		});  
	}
	
	public InMemoryFile root() {
		return this.root;
	}
	
	public InMemoryFile getFile(String path) throws IOException {
		return read(() -> {
			if (!InMemoryFileSystem.this.exists(path)) {
				throw new FileNotFoundException(String.format("File by path %s does not exists.", path));
			}
			return InMemoryFileSystem.this.fileSystem.get(path);
		});
	}
	
	public InMemoryFile createFileWithParents(String path) throws IOException {
		String parentPath = computeParentPath(path);
		String fileName = computeFileName(path);
		createParentPaths(parentPath);
		
		InMemoryFile fileObjectParent = getFile(parentPath);
		InMemoryFile fileObject = createFile(fileObjectParent, fileName);
		InMemoryFileSystem.this.fileSystem.put(fileObject.getPath(), fileObject);
		return fileObject;
	}

	public void createParentPaths(String path) throws IOException {
		if (root.getPath().equals(path)) {
			return;
		}
		//we need substring as on split first root's / will add entry as empty string in parts.
		// /home/ user => "", home, user
		// home/user => home, user
		String[] parts = path.substring(1, path.length()).split(PATH_SEPARATOR);
		
		Stack<String> stack = new Stack<>();
		InMemoryFile fileObjectParent = root();
		InMemoryFile fileObject = null;
		for (String part : parts) {
			stack.push(part);
			String joinedPath = joinPrepend(stack);
			
			fileObject = getFile(joinedPath);
			if (fileObject == null) {
				fileObject = createFolder(fileObjectParent, part);
				InMemoryFileSystem.this.fileSystem.put(fileObject.getPath(), fileObject);
			}
			
			fileObjectParent = fileObject;
		}
	}
	
	public void acquireLock(String path) {
		// With thread id locks kind of becomes reentrant
		// Single thread execution is sequential, file locks acquire release will be sequential
		Long lock = Thread.currentThread().getId();
		Long addedLock = this.fileLocks.putIfAbsent(path, lock);
		if (addedLock != null && addedLock != lock) {
			throw new RuntimeException("File is locked!");
		}
	}
	
	public void releaseLock(String path) throws IOException {
		this.fileLocks.remove(path);
	}
	
	public static String computeParentPath(String path) {
		if (path.lastIndexOf(PATH_SEPARATOR) == 0) {
			return PATH_SEPARATOR;
		}
		return path.substring(0, path.lastIndexOf(PATH_SEPARATOR));
	}
	
	public static String computeFileName(String path) {
		return path.substring(path.lastIndexOf(PATH_SEPARATOR) + 1, path.length());
	}
	
	private String joinPrepend(Stack<String> stack) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String string : stack) {
			stringBuilder.append(PATH_SEPARATOR).append(string);
		}
		
		return stringBuilder.toString();
	}
	
	private void renameFileObjectEntry(InMemoryFile fileObject, String name) {
		// remove old entry
		String path = fileObject.getPath();
		InMemoryFileSystem.this.fileSystem.remove(path);
		
		// update and add new entry
		fileObject.rename(name);
		path = fileObject.getPath();
		InMemoryFileSystem.this.fileSystem.put(path, fileObject);
	}
	
	@SuppressWarnings("unused")
	private void checkPathExists(InMemoryFile parent, String name) throws IOException {
		String path = InMemoryFile.computePathFrom(parent, name); 
		if (fileSystem.containsKey(path)) {
			throw new IOException(String.format("Folder path `%s` already exists. ", path));
		}
	}
	
	private InMemoryFile createFileObject(InMemoryFile parent, String name, boolean folder) throws IOException {
		return this.write(() -> {
			if (name.contains("/")) {
				throw new IOException(String.format("File name %s cannot contain `/`.", name));
			}
			
			String parentPath = parent.getPath();
			
			if (!exists(parentPath)) {
				throw new IOException(String.format("Parent path %s does not exists.", parentPath));
			}
			
			String path = InMemoryFile.computePathFrom(parent, name);
			
			if (exists(path)) {
				throw new IOException(String.format("File with path %s already exists, use getFile API to get the instance..", path));
			}
			 
			InMemoryFile fileObject = new InMemoryFile(parent, this, name, folder);
			fileSystem.put(path, fileObject);
			return fileObject;
		});
		
	}
	
	private <V>  V write(Callable<V> logic) throws IOException {
		try {
			writeLock.lock();
			return logic.call();
		} catch (IOException e) {
			throw e;
		} 	catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			writeLock.unlock();
		}
	}
	
	private <V>  V read(Callable<V> logic) throws IOException {
		try {
			readLock.lock();
			return logic.call();
		} catch (IOException e) {
			throw e;
		} 	catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			readLock.unlock();
		}
	}
	
}
