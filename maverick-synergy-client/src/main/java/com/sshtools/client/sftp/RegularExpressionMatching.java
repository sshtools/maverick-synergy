/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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