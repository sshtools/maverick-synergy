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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sshtools.client.sftp;

import java.io.File;

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
     */
    public String[] matchFileNamesWithPattern(File[] files, String fileNameRegExp) throws SshException, SftpStatusException {
        String[] thefile = new String[1];
        thefile[0] = files[0].getName();
        return thefile;
    }

    /**
     * returns files
     */
    public SftpFile[] matchFilesWithPattern(SftpFile[] files, String fileNameRegExp) throws SftpStatusException, SshException {
        return files;
    }

}
