# Maverick Synergy NIO File System

This module adds the ability to access remote files on an SFTP server using the standard Java NIO VFS APIs, `Path`, `Files` and friends.

It is similar in function to our `vfs-sftp` module which makes use of [Commons VFS](https://commons.apache.org/proper/commons-vfs/), but has the added advantages of using facilities built-in to Java (and so fewer dependencies), and uses modern syntax and APIs. 

## Features

 * Re-use existing connections.
 * Quick access using URI strings.
 * Supports all basic file operations and attributes.
 * Makes use of SFTP extensions such as remote copying when available.

### TODO

 * WatchService not implemented.
 * UserPrincipalLookupService not implemented.

## Quick Start

There are multiple ways to access SFTP via the Java NIO VFS. 

### Using a URI

The quickest way to access a remote file over SFTP instead of a local one is to use a `URI` instead of a `String` local path.
The URI must point to a remote folder.

```java
	var localPath = Paths.get("/home/me/test-file-1.txt");
	var remotePath = Paths.get(URI.create("sftp://testuser:password@some.host/remote-test-file-1.txt");

	try(var in = Files.newInputStream(localPath)) {
		try(var out = Files.newOutputStream(remotePath)) {
			in.transferTo(out);
		}
	}	
```

This example will copy the local file `test-file-1.txt` to the remote host `some.host`, authenticating as `testuser` with a password of `password`.
The remote file will be placed in the *Default Directory* for the user, usually the user's home directory.

Some other things to note about the URI.

 * If you do not provide a username, the current username will be used.
 * The password is optional, if not present, other mechanisms will be used (e.g. keys or agent).
 * Providing a password using a URI requires that special characters be encoded.
 * When referring to the root of the remote file system, the URI path should start with a double slash.
 * When referring to the default directory (the users home), the URI path start with a single slash. 

### Using Environment Map To Pass Configuration

```
try (var fs = FileSystems.newFileSystem(URI.create("sftp:///"), Map.of(
      SftpFileSystemProvider.USERNAME, "testuser",
      SftpFileSystemProvider.PASSWORD, "password",
      SftpFileSystemProvider.HOSTNAME, "some.host",
      SftpFileSystemProvider.PORT, 22
    ))) {
        
    var remotePath = fs.getPath("remote-test-file-1.txt");
        
    // ...
}
```

### Using an existing SftpClient

If you already have an instance of a `SftpClient`, you may re-use this and so avoid having to connect and authenticate. 
This will return a `FileSystem` that should have `close()` called when it is finished with.

You can optionally provide a remote folder path that will be the root of all further paths. This path may be either relative to the default directory (users home), or absolute on the file system.

When created this way, the underlying `SftpClient` *will not* be closed when the `FileSystem` is closed. 

To create a new file system, you can use the `SftpFileSystems` helper.

```java
	var sftpClient = new SftpClient(......);
	
	try(var fs = SftpFileSystems.newFileSystem(sftpClient)) {
		var remotePath = fs.getPath("remote-test-file-1.txt");
		
		// ...
	}

``` 

Or use the standard Java NIO API and pass the client into the `environment` map.

```
	try (var fs = FileSystems.newFileSystem(URI.create("sftp://"), Map.of(
			SftpFileSystemProvider.SFTP_CLIENT, sftp))) {
		
		var remotePath = fs.getPath("remote-test-file-1.txt");
		
		// ...
	}
```

### Using an existing SshClient

Much like re-using an `SftpClient`, you may also re-use an `SshClient`. This method will internally create a new dedicated `SftpClient`. 

You can optionally provide a remote folder path that will be the root of all further paths. This path may be either relative to the default directory (users home), or absolute on the file system.

When created this way, the underlying `SftpClient` *will* be closed when the `FileSystem` is closed. 

To create a new file system, you can use the `SftpFileSystems` helper.

```java
	var sftpClient = new SshClient(......);
	
	try(var fs = SftpFileSystems.newFileSystem(sshClient)) {
		var remotePath = fs.getPath("remote-test-file-1.txt");
		
		// ...
	}

``` 

Or use the standard Java NIO API and pass the client into the `environment` map.

```
	try (var fs = FileSystems.newFileSystem(URI.create("sftp://"), Map.of(
			SftpFileSystemProvider.SSH_CLIENT, sftp))) {
		
		var remotePath = fs.getPath("remote-test-file-1.txt");
		
		// ...
	}
```