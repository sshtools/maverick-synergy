package com.sshtools.server.vsession.commands.sftp;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.sshtools.client.sftp.SftpFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.Utils;
import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Ls extends SftpCommand {

	public Ls() {
		//super("ls", "SFTP", "ls", "Moves the working directory to a new directory");
		super("ls", "SFTP", UsageHelper.build("ls [options] path...",
				"-l, --long						        Show details for each individual file/folder",
				"-a, --all                              Show all files",
				"-d, --directory                        List directories themeselves, not their contents",
				"-x, --extended                         Show extended attributes"), 
				"List the contents of a directory.");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {

		try {
			
			if (args.length == 1) {
				SftpFile[] sftpFiles = this.sftp.ls();
				
					processSftpFilesForPrinting(args, console, sftpFiles);
				
			} else {
				List<String> paths = new ArrayList<>();
				for(int i=1; i<args.length;i++) {
					if(!CliHelper.isOption(args[i], "ladx")) {
						paths.add(args[i]);
					}
				}
				if(paths.isEmpty()) {
					SftpFile[] sftpFiles = this.sftp.ls();
					processSftpFilesForPrinting(args, console, sftpFiles);
				} else {
					for (String path : paths) {
						SftpFile[] sftpFiles = this.sftp.ls(path);
						processSftpFilesForPrinting(args, console, sftpFiles);
					}
				}
			}
		} catch (SftpStatusException | SshException | IOException | PermissionDeniedException e) {
			throw new IllegalStateException(String.format("Problem in processing ls command with args %s", Arrays.toString(args)), e);
		}
	}

	private void processSftpFilesForPrinting(String[] args, VirtualConsole console, SftpFile[] sftpFiles)
			throws SftpStatusException, SshException, IOException, PermissionDeniedException {
		for (SftpFile sftpFile : sftpFiles) {
			if (sftpFile.isFile() && CliHelper.hasShortOption(args, 'd')) {
				continue;
			}
			
			printFile(args, console, sftpFile);
		}
	}

	protected void printFile(String[] args, VirtualConsole console, SftpFile file) throws IOException, 
		PermissionDeniedException, SftpStatusException, SshException {
		
		SftpFileAttributes fileAttributes = file.getAttributes();
		
		if (!(isHidden(file)) || CliHelper.hasOption(args,'a', "all")) {
			
			if (CliHelper.hasOption(args,'l', "all")) {
				
				String lastModifiedTime = "";
				long size = 0;
				if (file.isFile()) {
					size = fileAttributes.getSize().longValue();
				} else if (file.isDirectory()) {
					size = 0;
				}
				SimpleDateFormat df;
		        long mt = (fileAttributes.getModifiedTime().longValue() * 1000L);
		        long now = System.currentTimeMillis();

		        if ((now - mt) > (6 * 30 * 24 * 60 * 60 * 1000L)) {
		            df = new SimpleDateFormat("MMM dd  yyyy", console.getConnection().getLocale());
		        } else {
		            df = new SimpleDateFormat("MMM dd HH:mm", console.getConnection().getLocale());
		        }

		        lastModifiedTime = df.format(new Date(mt));
				int linkCount = 0;
				console.println(String.format("%s %-3d %-8s %-8s %10d %-14s %-30s", 
						fileAttributes.getPermissionsString(), 
						linkCount, 
						Utils.defaultString(fileAttributes.getUID(), "nouser"),
						Utils.defaultString(fileAttributes.getGID(), "nogroup"),
						size, 
						lastModifiedTime, 
						file.getFilename()));
			} else {
				console.println(file.getFilename());
			}
			if(CliHelper.hasOption(args,'x', "extended")) {
				for(Object name : fileAttributes.getExtendedAttributes().keySet()) {
					Object val = fileAttributes.getExtendedAttributes().get(name);
					console.println(String.format("%" + (CliHelper.hasShortOption(args,'l') ? 64 : 4)+ "s%s", "", name.toString() + "=" + (val == null ? "" : val.toString())));
				}
			}
		}
	}
	
	private boolean isHidden(SftpFile sftpFile) throws SftpStatusException, SshException {
		SftpFileAttributes fileAttributes = sftpFile.getAttributes();
		if (fileAttributes.hasAttributeBits() && fileAttributes.isHidden()) {
			return true;
		}
		
		if (sftpFile.getFilename().startsWith(".")) {
			return true;
		}
		
		return false;
	}
}
