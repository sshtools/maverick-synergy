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
 * Interface for treating a filename as a regular expression and returning the
 * list of files that match.
 */
public interface RegularExpressionMatching {

    /**
     * returns each of the SftpFiles that match the pattern fileNameRegExp
     * 
     * @param files
     * @param fileNameRegExp
     * @return SftpFile[]
     * @throws SftpStatusException
     * @throws SshException
     */
    public SftpFile[] matchFilesWithPattern(SftpFile[] files, String fileNameRegExp) throws SftpStatusException, SshException;

    /**
     * returns each of the files that match the pattern fileNameRegExp
     * 
     * @param files
     * @param fileNameRegExp
     * @return String[]
     * @throws SftpStatusException
     * @throws SshException
     * @throws PermissionDeniedException 
     * @throws IOException 
     */
    public String[] matchFileNamesWithPattern(AbstractFile[] files, String fileNameRegExp) throws SftpStatusException, SshException, IOException, PermissionDeniedException;

}