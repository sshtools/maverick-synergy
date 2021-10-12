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
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.client.sftp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

public class RegExpMatching implements RegularExpressionMatching {

	@Override
	public SftpFile[] matchFilesWithPattern(SftpFile[] files, String pattern)
			throws SftpStatusException, SshException {
		
		Pattern p = Pattern.compile(pattern);

		List<SftpFile> matched = new ArrayList<>();
		
		for(SftpFile file : files) {
			if(p.matcher(file.getFilename()).matches()) {
				matched.add(file);
			}
		}
		return matched.toArray(new SftpFile[0]);
	}

	@Override
	public String[] matchFileNamesWithPattern(AbstractFile[] files, String pattern)
			throws SftpStatusException, SshException, IOException, PermissionDeniedException {
		
		Pattern p = Pattern.compile(pattern);
		
		List<String> matched = new ArrayList<>();
		
		for(AbstractFile file : files) {
			if(p.matcher(file.getName()).matches()) {
				matched.add(file.getAbsolutePath());
			}
		}
		
		return matched.toArray(new String[0]);
	}

}
