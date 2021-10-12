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

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

/**
 * Implements the RegularExpressionMatching Interface.<br>
 * Performs no regular expression matching so:<br>
 * matchFilesWithPattern() simply returns the files parameter it is passed as an
 * argument<br>
 * matchFileNamesWithPattern() simply returns a 1 element array containing the
 * filename on the first element of the file[] argument passed to it.
 */
public class NoRegExpMatching implements RegularExpressionMatching {

    /**
     * opens and returns the requested filename string
     * 
     * @throws SftpStatusException
     * @throws PermissionDeniedException 
     * @throws IOException 
     */
    public String[] matchFileNamesWithPattern(AbstractFile[] files, String fileNameRegExp) throws SshException, SftpStatusException, IOException, PermissionDeniedException {
        String[] thefile = new String[1];
        for(int i=0;i<files.length;i++) {
        	thefile[i] = files[i].getAbsolutePath();
        }
        return thefile;
    }

    /**
     * returns files
     */
    public SftpFile[] matchFilesWithPattern(SftpFile[] files, String fileNameRegExp) throws SftpStatusException, SshException {
        return files;
    }

}
