package com.sshtools.client.sftp;

/*-
 * #%L
 * Client API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
